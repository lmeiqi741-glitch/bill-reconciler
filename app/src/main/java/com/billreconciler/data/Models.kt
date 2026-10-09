package com.billreconciler.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * 账单记录（一次截图识别的结果）
 */
@Entity(tableName = "bills")
data class BillEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val createdAt: Long = System.currentTimeMillis(),
    val platform: String,        // 平台名称：支付宝/微信/信用卡/花呗等
    val imagePath: String?,      // 截图路径（可选）
    val rawText: String,         // OCR 原始文字
    val status: String = "pending"  // pending / processing / done / error
)

/**
 * 单笔交易
 */
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val billId: String,          // 关联的账单ID
    val date: String,            // 交易日期 YYYY-MM-DD
    val merchant: String,        // 商户名称
    val amount: Double,          // 金额（正数=支出，负数=收入）
    val category: String,        // 分类
    val note: String? = null     // 备注
)

// ===== 分类枚举 =====
object Category {
    const val FOOD = "餐饮美食"
    const val TRANSPORT = "交通出行"
    const val SHOPPING = "购物消费"
    const val ENTERTAINMENT = "休闲娱乐"
    const val HOUSING = "居住缴费"
    const val MEDICAL = "医疗健康"
    const val EDUCATION = "教育培训"
    const val COMMUNICATION = "通讯网络"
    const val FINANCE = "金融理财"
    const val INCOME = "收入"
    const val OTHER = "其他"

    val ALL = listOf(
        FOOD, TRANSPORT, SHOPPING, ENTERTAINMENT,
        HOUSING, MEDICAL, EDUCATION, COMMUNICATION,
        FINANCE, INCOME, OTHER
    )

    // 分类图标
    val ICONS = mapOf(
        FOOD to "🍜",
        TRANSPORT to "🚗",
        SHOPPING to "🛒",
        ENTERTAINMENT to "🎮",
        HOUSING to "🏠",
        MEDICAL to "🏥",
        EDUCATION to "📚",
        COMMUNICATION to "📱",
        FINANCE to "💰",
        INCOME to "💵",
        OTHER to "📦"
    )
}

// ===== AI 解析结果模型 =====
data class ParsedTransaction(
    val date: String,
    val merchant: String,
    val amount: Double,
    val category: String,
    val note: String? = null
)

data class ParseResult(
    val platform: String,
    val transactions: List<ParsedTransaction>
)

// ===== 月度统计 =====
data class MonthlyStats(
    val month: String,           // YYYY-MM
    val totalExpense: Double,
    val totalIncome: Double,
    val categoryBreakdown: Map<String, Double>,
    val transactionCount: Int
)
