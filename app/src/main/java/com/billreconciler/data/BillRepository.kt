package com.billreconciler.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BillRepository(
    private val billDao: BillDao,
    private val transactionDao: TransactionDao
) {

    val allBills = billDao.getAllBills()
    val allTransactions = transactionDao.getAllTransactions()

    suspend fun insertBill(bill: BillEntity) = withContext(Dispatchers.IO) {
        billDao.insertBill(bill)
    }

    suspend fun updateBill(bill: BillEntity) = withContext(Dispatchers.IO) {
        billDao.updateBill(bill)
    }

    suspend fun getBillById(id: String) = withContext(Dispatchers.IO) {
        billDao.getBillById(id)
    }

    suspend fun deleteBill(id: String) = withContext(Dispatchers.IO) {
        transactionDao.deleteByBillId(id)
        billDao.deleteBill(id)
    }

    suspend fun insertTransactions(transactions: List<TransactionEntity>) = withContext(Dispatchers.IO) {
        transactionDao.insertAll(transactions)
    }

    fun getTransactionsByBill(billId: String) = transactionDao.getTransactionsByBill(billId)

    fun getTransactionsByMonth(month: String) = transactionDao.getTransactionsByMonth(month)

    fun getCategoryBreakdown(month: String) = transactionDao.getCategoryBreakdown(month)

    suspend fun updateTransaction(transaction: TransactionEntity) = withContext(Dispatchers.IO) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(id: String) = withContext(Dispatchers.IO) {
        transactionDao.deleteById(id)
    }

    suspend fun getTransactionById(id: String) = withContext(Dispatchers.IO) {
        transactionDao.getById(id)
    }

    companion object {
        fun fromContext(context: Context): BillRepository {
            val db = AppDatabase.getInstance(context)
            return BillRepository(db.billDao(), db.transactionDao())
        }
    }
}
