package com.billreconciler.network

import com.billreconciler.data.ParsedTransaction
import com.billreconciler.data.ParseResult
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object BillDeepSeekClient {

    private const val API_URL = "https://api.deepseek.com/v1/chat/completions"
    private val gson = Gson()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun parseBillText(apiKey: String, ocrText: String): ParseResult =
        withContext(Dispatchers.IO) {

            val systemPrompt = """
                你是一位专业的账单分析助手，擅长从各种平台的账单截图文字中提取交易信息。

                支持的账单平台：支付宝、微信支付、信用卡账单、花呗、京东白条、银行App等。

                请从 OCR 识别的账单文字中提取每笔交易，包括：
                1. 交易日期（格式：YYYY-MM-DD，如果只有月日则推断为当年）
                2. 商户/交易名称
                3. 金额（支出为正数，收入/退款为负数）
                4. 分类（从以下分类中选择最合适的：餐饮美食、交通出行、购物消费、休闲娱乐、居住缴费、医疗健康、教育培训、通讯网络、金融理财、收入、其他）

                注意：
                - 跳过非交易行（如标题、总计、余额等）
                - 金额要统一为数字，去掉 ¥、$ 等符号
                - 如果无法确定分类，使用"其他"
                - 日期格式统一为 YYYY-MM-DD
            """.trimIndent()

            val userPrompt = """
                以下是通过 OCR 识别的账单截图内容，请提取所有交易记录：

                ---
                $ocrText
                ---

                请严格按照以下 JSON 格式返回（不要包含其他文字）：

                {
                  "platform": "账单平台名称（如：支付宝/微信/信用卡）",
                  "transactions": [
                    {
                      "date": "YYYY-MM-DD",
                      "merchant": "商户名称",
                      "amount": 金额数字,
                      "category": "分类",
                      "note": "备注（可选）"
                    }
                  ]
                }

                如果无法识别任何交易，返回：{"platform":"未知","transactions":[]}
            """.trimIndent()

            val requestBody = mapOf(
                "model" to "deepseek-chat",
                "messages" to listOf(
                    mapOf("role" to "system", "content" to systemPrompt),
                    mapOf("role" to "user", "content" to userPrompt)
                ),
                "temperature" to 0.2,
                "max_tokens" to 4000,
                "stream" to false
            )

            val jsonBody = gson.toJson(requestBody)
            val request = Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
                ?: throw Exception("API 返回空响应")

            if (!response.isSuccessful) {
                throw Exception("API 请求失败: ${response.code}")
            }

            val jsonResponse = gson.fromJson(responseBody, Map::class.java)
            val content = (jsonResponse["choices"] as? List<*>)?.firstOrNull()
                ?.let { (it as Map<*, *>)["message"] as? Map<*, *> }
                ?.get("content") as? String
                ?: throw Exception("API 返回内容为空")

            parseResult(content)
        }

    private fun parseResult(content: String): ParseResult {
        val jsonText = when {
            content.contains("```json") -> {
                val start = content.indexOf("```json") + 7
                val end = content.indexOf("```", start)
                content.substring(start, end).trim()
            }
            content.contains("```") -> {
                val start = content.indexOf("```") + 3
                val end = content.indexOf("```", start)
                content.substring(start, end).trim()
            }
            else -> {
                val firstBrace = content.indexOf('{')
                val lastBrace = content.lastIndexOf('}')
                if (firstBrace >= 0 && lastBrace > firstBrace) {
                    content.substring(firstBrace, lastBrace + 1)
                } else throw Exception("无法解析 AI 响应")
            }
        }

        val map = gson.fromJson(jsonText, Map::class.java)
        val platform = map["platform"] as? String ?: "未知"
        val transactionsRaw = map["transactions"] as? List<*> ?: emptyList()

        val transactions = transactionsRaw.mapNotNull { item ->
            val t = item as? Map<*, *> ?: return@mapNotNull null
            ParsedTransaction(
                date = t["date"] as? String ?: "",
                merchant = t["merchant"] as? String ?: "",
                amount = (t["amount"] as? Number)?.toDouble() ?: 0.0,
                category = t["category"] as? String ?: "其他",
                note = t["note"] as? String
            )
        }

        return ParseResult(platform, transactions)
    }
}
