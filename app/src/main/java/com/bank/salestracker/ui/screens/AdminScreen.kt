@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.bank.salestracker.ui.screens

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bank.salestracker.data.model.AdminReport
import com.bank.salestracker.data.model.EmployeeSummary
import com.bank.salestracker.data.model.RegistrationStatus
import com.bank.salestracker.data.model.Sale
import com.bank.salestracker.data.model.User
import com.bank.salestracker.di.ServiceLocator
import kotlinx.coroutines.launch

class AdminVm : ViewModel() {
    var report by mutableStateOf<AdminReport?>(null)
    var period by mutableStateOf("month")
    var loading by mutableStateOf(true)
    var employeeQuery by mutableStateOf("")
    var foundUser by mutableStateOf<User?>(null)
    var pendingUsers by mutableStateOf<List<User>>(emptyList())
    var actionMessage by mutableStateOf<String?>(null)
    var lookupLoading by mutableStateOf(false)

    init {
        refresh()
        loadPending()
    }

    fun refresh() = viewModelScope.launch {
        loading = true
        runCatching { report = ServiceLocator.salesRepo.adminReport(period) }
        loading = false
    }

    fun selectPeriod(value: String) {
        period = value
        refresh()
    }

    fun loadPending() = viewModelScope.launch {
        runCatching { pendingUsers = ServiceLocator.salesRepo.pendingUsers() }
    }

    fun lookupEmployee() = viewModelScope.launch {
        val employeeId = employeeQuery.trim().lowercase()
        if (!employeeId.matches(Regex("^vtb\\d+$"))) {
            actionMessage = "Введите табельный в формате vtb70336144"
            return@launch
        }
        lookupLoading = true
        foundUser = null
        actionMessage = null
        runCatching { ServiceLocator.salesRepo.managementUser(employeeId) }
            .onSuccess { foundUser = it }
            .onFailure { actionMessage = "Сотрудник не найден" }
        lookupLoading = false
    }

    fun assignFoundUser() = viewModelScope.launch {
        val user = foundUser ?: return@launch
        lookupLoading = true
        runCatching { ServiceLocator.salesRepo.assignUser(user.employeeId) }
            .onSuccess {
                foundUser = it
                actionMessage = "Сотрудник добавлен в вашу структуру"
                loadPending()
                refresh()
            }
            .onFailure { actionMessage = "Не удалось привязать сотрудника" }
        lookupLoading = false
    }

    fun setGoal(employeeId: String, goal: Int) = viewModelScope.launch {
        runCatching { ServiceLocator.salesRepo.setGoal(employeeId, goal) }
        refresh()
    }

    suspend fun exportCsv(): String? =
        runCatching { ServiceLocator.salesRepo.exportCsv(period) }.getOrNull()
}

@Composable
fun AdminScreen(onEmployeeClick: (String) -> Unit, vm: AdminVm = viewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var goalDialogFor by remember { mutableStateOf<EmployeeSummary?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Команда") },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            vm.exportCsv()?.let { csv ->
                                context.startActivity(
                                    Intent.createChooser(
                                        Intent(Intent.ACTION_SEND).apply {
                                            type = "text/csv"
                                            putExtra(Intent.EXTRA_TEXT, csv)
                                        },
                                        "Экспорт CSV"
                                    )
                                )
                            }
                        }
                    }) { Icon(Icons.Default.FileDownload, "Экспорт CSV") }
                }
            )
        }
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item { AddEmployeePanel(vm) }

            item {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf("today" to "Сегодня", "week" to "Неделя", "month" to "Месяц").forEachIndexed { index, item ->
                        SegmentedButton(
                            selected = vm.period == item.first,
                            onClick = { vm.selectPeriod(item.first) },
                            shape = SegmentedButtonDefaults.itemShape(index, 3)
                        ) { Text(item.second) }
                    }
                }
            }

            if (vm.loading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                return@LazyColumn
            }

            vm.report?.let { report ->
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                            MetricColumn("${report.branchTodayCount}", "сегодня", Modifier.weight(1f))
                            MetricColumn("${report.branchMonthCount}", "за месяц", Modifier.weight(1f))
                            MetricColumn("%,.0f".format(report.branchMonthAmount), "₽ за месяц", Modifier.weight(1f))
                        }
                    }
                }

                item { Text("Рейтинг сотрудников", style = MaterialTheme.typography.titleMedium) }

                val sorted = report.employees.sortedByDescending { it.monthCount }
                items(sorted) { employee ->
                    val place = sorted.indexOf(employee) + 1
                    Card(Modifier.fillMaxWidth().clickable { onEmployeeClick(employee.user.id) }) {
                        ListItem(
                            leadingContent = {
                                if (place <= 3) {
                                    Icon(
                                        Icons.Default.EmojiEvents,
                                        null,
                                        tint = when (place) {
                                            1 -> MaterialTheme.colorScheme.tertiary
                                            2 -> MaterialTheme.colorScheme.outline
                                            else -> MaterialTheme.colorScheme.secondary
                                        }
                                    )
                                } else {
                                    Text("$place", style = MaterialTheme.typography.titleMedium)
                                }
                            },
                            headlineContent = { Text(employee.user.fullName) },
                            supportingContent = {
                                Column {
                                    Text("Сегодня: ${employee.todayCount} · Период: ${employee.monthCount} · %,.0f ₽".format(employee.monthAmount))
                                    employee.goalProgress?.let { progress ->
                                        Spacer(Modifier.height(4.dp))
                                        LinearProgressIndicator(
                                            progress = { progress.coerceIn(0f, 1f) },
                                            modifier = Modifier.fillMaxWidth().height(6.dp),
                                            strokeCap = StrokeCap.Round
                                        )
                                    }
                                }
                            },
                            trailingContent = {
                                TextButton(onClick = { goalDialogFor = employee }) { Text("План") }
                            }
                        )
                    }
                }
            }
        }
    }

    goalDialogFor?.let { employee ->
        var goalText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { goalDialogFor = null },
            title = { Text("План для ${employee.user.fullName}") },
            text = {
                OutlinedTextField(
                    value = goalText,
                    onValueChange = { goalText = it.filter(Char::isDigit) },
                    label = { Text("Продаж в месяц") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    goalText.toIntOrNull()?.let { vm.setGoal(employee.user.id, it) }
                    goalDialogFor = null
                }) { Text("Сохранить") }
            },
            dismissButton = { TextButton(onClick = { goalDialogFor = null }) { Text("Отмена") } }
        )
    }
}

@Composable
private fun AddEmployeePanel(vm: AdminVm) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Добавить сотрудника", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = vm.employeeQuery,
                onValueChange = { vm.employeeQuery = it },
                label = { Text("Табельный номер") },
                placeholder = { Text("vtb70336144") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = vm::lookupEmployee, enabled = !vm.lookupLoading) {
                        Icon(Icons.Default.Search, "Найти")
                    }
                }
            )

            vm.pendingUsers.takeIf { it.isNotEmpty() }?.let { pending ->
                Text("Ожидают привязки", style = MaterialTheme.typography.labelLarge)
                pending.take(3).forEach { user ->
                    AssistChip(
                        onClick = {
                            vm.employeeQuery = user.employeeId
                            vm.foundUser = user
                        },
                        label = { Text("${user.fullName} · ${user.employeeId}") }
                    )
                }
            }

            vm.foundUser?.let { user ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(user.fullName, style = MaterialTheme.typography.titleSmall)
                        Text(user.employeeId)
                        Text("Статус: ${statusTitle(user.registrationStatus)}")
                        Button(
                            onClick = vm::assignFoundUser,
                            enabled = !vm.lookupLoading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PersonAdd, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Добавить в мою структуру")
                        }
                    }
                }
            }

            vm.actionMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        }
    }
}

@Composable
private fun MetricColumn(value: String, label: String, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(value, style = MaterialTheme.typography.headlineSmall)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

private fun statusTitle(status: RegistrationStatus): String = when (status) {
    RegistrationStatus.PENDING_ASSIGNMENT -> "ожидает привязки"
    RegistrationStatus.ACTIVE -> "активен"
    RegistrationStatus.BLOCKED -> "заблокирован"
    RegistrationStatus.DEACTIVATED -> "деактивирован"
}

class EmployeeDetailVm : ViewModel() {
    var sales by mutableStateOf<List<Sale>>(emptyList())
    var loading by mutableStateOf(true)

    fun load(id: String) = viewModelScope.launch {
        loading = true
        runCatching { sales = ServiceLocator.salesRepo.employeeSales(id) }
        loading = false
    }
}

@Composable
fun EmployeeDetailScreen(employeeId: String, vm: EmployeeDetailVm = viewModel()) {
    LaunchedEffect(employeeId) { vm.load(employeeId) }

    Scaffold(topBar = { TopAppBar(title = { Text("Продажи сотрудника") }) }) { pad ->
        if (vm.loading) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        LazyColumn(Modifier.padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (vm.sales.isEmpty()) item { Text("Продаж за период нет") }
            items(vm.sales) { sale ->
                Card {
                    ListItem(
                        headlineContent = { Text(sale.category.title) },
                        supportingContent = {
                            Column {
                                Text("Клиент: ${sale.clientLast4}")
                                sale.createdAt?.let { Text(it.take(16).replace("T", " ")) }
                                sale.comment?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                            }
                        },
                        trailingContent = { sale.amount?.let { Text("%,.0f ₽".format(it)) } }
                    )
                }
            }
        }
    }
}
