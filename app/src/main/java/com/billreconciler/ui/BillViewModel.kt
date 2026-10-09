package com.billreconciler.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billreconciler.data.BillEntity
import com.billreconciler.data.BillRepository
import com.billreconciler.data.Category
import com.billreconciler.data.MonthlyStats
import com.billreconciler.data.TransactionEntity
import com.billreconciler.network.BillDeepSeekClient
import com.billreconciler.util.ApiKeyManager
import com.billreconciler.util.BillOCRProcessor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * 处理状态
 */
sealed class ProcessState {
    data object Idle : ProcessState()
    data object OCRProcessing : ProcessState()
    data class OCRSuccess(val text: String) : ProcessState()
    data class Parsing(val text: String) : ProcessState()
    data class Success(val platform: String, val count: Int) : ProcessState()
    data class Error(val message: String) : ProcessState()
}

/**
 * 页面
 */
sealed class Screen {
    data object Home : Screen()
    data object Settings : Screen()
    data class BillDetail(val billId: String) : Screen()
}

class BillViewModel : ViewModel() {

    private val _processState = MutableStateFlow<ProcessState>(ProcessState.Idle)
    val processState: StateFlow<ProcessState> = _processState.asStateFlow()

    private val _currentScreen = MutableStateFlow<Screen>(Screen.Home)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _selectedMonth = MutableStateFlow<String?>(null)
    val selectedMonth: StateFlow<String?> = _selectedMonth.asStateFlow()

    private var repository: BillRepository? = null

    fun initRepository(context: Context) {
        if (repository == null) {
            repository = BillRepository.fromContext(context)
        }
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun selectMonth(month: String) {
        _selectedMonth.value = month
    }

    /**
     * 处理账单截图
     */
    fun processBill(context: Context, uri: Uri) {
        initRepository(context)
        val apiKey = ApiKeyManager.getApiKey(context)

        if (apiKey.isBlank()) {
            _processState.value = ProcessState.Error("请先设置 DeepSeek API Key")
            return
        }

        viewModelScope.launch {
            _processState.value = ProcessState.OCRProcessing

            try {
                // Step 1: OCR
                val rawText = BillOCRProcessor.recognizeFromUri(context, uri)

                if (rawText.isBlank()) {
                    _processState.value = ProcessState.Error("未识别到文字，请尝试更清晰的截图")
                    return@launch
                }

                // Save bill record
                val bill = BillEntity(
                    platform = "识别中...",
                    imagePath = uri.toString(),
                    rawText = rawText,
                    status = "processing"
                )
                repository?.insertBill(bill)

                _processState.value = ProcessState.OCRSuccess(rawText)

                // Step 2: AI 解析
                _processState.value = ProcessState.Parsing(rawText)

                val parseResult = BillDeepSeekClient.parseBillText(apiKey, rawText)

                // Save transactions
                val transactions = parseResult.transactions.map { t ->
                    TransactionEntity(
                        billId = bill.id,
                        date = t.date,
                        merchant = t.merchant,
                        amount = t.amount,
                        category = t.category,
                        note = t.note
                    )
                }
                repository?.insertTransactions(transactions)

                // Update bill
                repository?.updateBill(
                    bill.copy(
                        platform = parseResult.platform,
                        status = "done"
                    )
                )

                _processState.value = ProcessState.Success(
                    platform = parseResult.platform,
                    count = transactions.size
                )

            } catch (e: Exception) {
                Log.e("BillViewModel", "Process failed", e)
                _processState.value = ProcessState.Error(e.message ?: "处理失败")
            }
        }
    }

    fun deleteBill(billId: String) {
        viewModelScope.launch {
            repository?.deleteBill(billId)
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            repository?.deleteTransaction(id)
        }
    }

    fun updateTransactionCategory(id: String, category: String) {
        viewModelScope.launch {
            repository?.getTransactionById(id)?.let { transaction ->
                repository?.updateTransaction(transaction.copy(category = category))
            }
        }
    }

    fun reset() {
        _processState.value = ProcessState.Idle
    }
}
