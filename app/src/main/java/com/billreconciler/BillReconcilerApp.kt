package com.billreconciler

import android.app.Application
import com.billreconciler.data.AppDatabase

class BillReconcilerApp : Application() {
    val database by lazy { AppDatabase.getInstance(this) }
}
