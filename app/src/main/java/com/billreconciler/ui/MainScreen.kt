package com.billreconciler.ui

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.billreconciler.data.BillEntity
import com.billreconciler.data.BillRepository
import com.billreconciler.data.Category
import com.billreconciler.data.TransactionEntity
import com.billreconciler.util.ApiKeyManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ===== 主题色 =====
val PrimaryColor = Color(0xFF6366F1)
val PrimaryLight = Color(0xFFE0E7FF)
val SuccessColor = Color(0xFF16A34A)
val WarningColor = Color(0xFFD97706)
val DangerColor = Color(0xFFDC2626)
val DangerLight = Color(0xFFFEF2F2)
val WarningLight = Color(0xFFFFFBEB)
val SuccessLight = Color(0xFFF0FDF4)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                BillReconcilerApp()
            }
        }
    }
}

@Composable
fun BillReconcilerApp() {
    val viewModel: BillViewModel = viewModel()
    val context = LocalContext.current
    val currentScreen by viewModel.currentScreen.collectAsState()
    val repository = remember { BillRepository.fromContext(context) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentScreen is Screen.Home,
                    onClick = { viewModel.navigateTo(Screen.Home) },
                    icon = { Icon(Icons.Default.Add, "导入") },
                    label = { Text("导入") }
                )
                NavigationBarItem(
                    selected = currentScreen is Screen.Settings,
                    onClick = { viewModel.navigateTo(Screen.Settings) },
                    icon = { Icon(Icons.Default.Settings, "设置") },
                    label = { Text("设置") }
                )
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (val screen = currentScreen) {
                is Screen.Home -> HomeScreen(viewModel, repository)
                is Screen.Settings -> SettingsScreenContent()
                is Screen.BillDetail -> BillDetailScreen(
                    billId = screen.billId,
                    repository = repository,
                    onBack = { viewModel.navigateTo(Screen.Home) },
                    onDeleteTransaction = { viewModel.deleteTransaction(it) },
                    onUpdateCategory = { id, cat -> viewModel.updateTransactionCategory(id, cat) }
                )
            }
        }
    }
}

// ===== 首页 =====
@Composable
fun HomeScreen(viewModel: BillViewModel, repository: BillRepository) {
    val context = LocalContext.current
    val processState by viewModel.processState.collectAsState()
    val bills by repository.allBills.collectAsState(initial = emptyList())
    val transactions by repository.allTransactions.collectAsState(initial = emptyList())

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.processBill(context, it) }
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("💰 账单对账助手", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = PrimaryColor,
                titleContentColor = Color.White
            )
        )

        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // 统计卡片
            StatsCards(transactions)

            Spacer(Modifier.height(16.dp))

            // 导入按钮
            Button(
                onClick = { galleryLauncher.launch("image/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.size(8.dp))
                Text("导入账单截图")
            }

            // API Key 提示
            if (!ApiKeyManager.hasApiKey(context)) {
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = WarningLight)
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⚠️ 请先设置 API Key", color = WarningColor, fontSize = 13.sp)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // 处理状态
            when (val state = processState) {
                is ProcessState.OCRProcessing -> {
                    LoadingCard("正在识别账单文字...", "ML Kit OCR 中文识别")
                }
                is ProcessState.Parsing -> {
                    LoadingCard("AI 正在解析交易...", "DeepSeek 智能分析中")
                }
                is ProcessState.Success -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SuccessLight)
                    ) {
                        Row(
                            Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, null, SuccessColor, Modifier.size(20.dp))
                            Spacer(Modifier.size(8.dp))
                            Column {
                                Text("✅ 解析成功", color = SuccessColor, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "平台: ${state.platform}，共 ${state.count} 笔交易",
                                    fontSize = 13.sp,
                                    color = SuccessColor.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { viewModel.reset() }, modifier = Modifier.fillMaxWidth()) {
                        Text("继续导入")
                    }
                }
                is ProcessState.Error -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DangerLight)
                    ) {
                        Row(
                            Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("❌ ${state.message}", color = DangerColor, fontSize = 13.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { viewModel.reset() }, modifier = Modifier.fillMaxWidth()) {
                        Text("重试")
                    }
                }
                else -> {}
            }

            Spacer(Modifier.height(16.dp))

            // 账单列表
            Text(
                "📋 账单记录 (${bills.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))

            if (bills.isEmpty()) {
                Column(
                    Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("📭", fontSize = 40.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "还没有账单记录",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        "点击上方按钮导入截图",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(bills, key = { it.id }) { bill ->
                        BillCard(
                            bill = bill,
                            onClick = { viewModel.navigateTo(Screen.BillDetail(bill.id)) },
                            onDelete = { viewModel.deleteBill(bill.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsCards(transactions: List<TransactionEntity>) {
    val totalExpense = transactions.filter { it.amount > 0 }.sumOf { it.amount }
    val totalIncome = transactions.filter { it.amount < 0 }.sumOf { -it.amount }
    val count = transactions.size

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard(
            "总支出",
            "¥${String.format("%.0f", totalExpense)}",
            DangerColor,
            DangerLight,
            Modifier.weight(1f)
        )
        StatCard(
            "总收入",
            "¥${String.format("%.0f", totalIncome)}",
            SuccessColor,
            SuccessLight,
            Modifier.weight(1f)
        )
        StatCard(
            "交易数",
            "$count 笔",
            PrimaryColor,
            PrimaryLight,
            Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(label: String, value: String, color: Color, bgColor: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = bgColor)) {
        Column(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 12.sp, color = color.copy(alpha = 0.8f))
        }
    }
}

@Composable
private fun LoadingCard(title: String, subtitle: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(Modifier.size(24.dp), color = PrimaryColor)
            Spacer(Modifier.size(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodyMedium)
                Text(
                    subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun BillCard(bill: BillEntity, onClick: () -> Unit, onDelete: () -> Unit) {
    val dateFormat = SimpleDateFormat("MM月dd日 HH:mm", Locale.CHINA)
    val dateStr = dateFormat.format(Date(bill.createdAt))

    val statusColor = when (bill.status) {
        "done" -> SuccessColor
        "error" -> DangerColor
        "processing" -> WarningColor
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    }
    val statusText = when (bill.status) {
        "done" -> "✅ 已完成"
        "error" -> "❌ 失败"
        "processing" -> "⏳ 处理中"
        else -> "待处理"
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    bill.platform,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(2.dp))
                Text(dateStr, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                Spacer(Modifier.height(2.dp))
                Text(statusText, fontSize = 12.sp, color = statusColor)
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    "删除",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ===== 设置页 =====
@Composable
fun SettingsScreenContent() {
    val context = LocalContext.current
    var apiKey by remember { mutableStateOf(ApiKeyManager.getApiKey(context)) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("⚙️ 设置", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = PrimaryColor,
                titleContentColor = Color.White
            )
        )

        Column(
            Modifier.fillMaxSize().padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "DeepSeek API Key",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "用于 AI 解析账单。新用户有免费额度。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.height(12.dp))
                    androidx.compose.material3.OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("API Key") },
                        placeholder = { Text("sk-xxxxxxxx") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { ApiKeyManager.saveApiKey(context, apiKey) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("保存")
                    }
                }
            }
        }
    }
}

// ===== 账单详情页 =====
@Composable
fun BillDetailScreen(
    billId: String,
    repository: BillRepository,
    onBack: () -> Unit,
    onDeleteTransaction: (String) -> Unit,
    onUpdateCategory: (String, String) -> Unit
) {
    val transactions by repository.getTransactionsByBill(billId).collectAsState(initial = emptyList())

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("📋 交易明细", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.History, "返回", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = PrimaryColor,
                titleContentColor = Color.White
            )
        )

        if (transactions.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("📭", fontSize = 40.sp)
                Spacer(Modifier.height(8.dp))
                Text("暂无交易记录", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(transactions, key = { it.id }) { transaction ->
                    TransactionCard(
                        transaction = transaction,
                        onDelete = { onDeleteTransaction(transaction.id) },
                        onUpdateCategory = { onUpdateCategory(transaction.id, it) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionCard(
    transaction: TransactionEntity,
    onDelete: () -> Unit,
    onUpdateCategory: (String) -> Unit
) {
    val isExpense = transaction.amount > 0
    val amountColor = if (isExpense) DangerColor else SuccessColor
    val amountPrefix = if (isExpense) "-" else "+"
    val icon = Category.ICONS[transaction.category] ?: "📦"

    var showCategoryMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 分类图标
            Text(icon, fontSize = 24.sp)

            Spacer(Modifier.size(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    transaction.merchant,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Row {
                    Text(
                        transaction.date,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        transaction.category,
                        fontSize = 12.sp,
                        color = PrimaryColor,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(PrimaryLight)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .clickable { showCategoryMenu = true }
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "$amountPrefix¥${String.format("%.2f", kotlin.math.abs(transaction.amount))}",
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        "删除",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // 分类选择下拉
        if (showCategoryMenu) {
            androidx.compose.material3.DropdownMenu(
                expanded = showCategoryMenu,
                onDismissRequest = { showCategoryMenu = false }
            ) {
                Category.ALL.forEach { category ->
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("${Category.ICONS[category]} $category") },
                        onClick = {
                            onUpdateCategory(category)
                            showCategoryMenu = false
                        }
                    )
                }
            }
        }
    }
}
