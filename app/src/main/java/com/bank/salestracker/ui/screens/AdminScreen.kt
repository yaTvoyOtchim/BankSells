@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.bank.salestracker.ui.screens

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import com.bank.salestracker.data.model.displayTitle
import com.bank.salestracker.di.ServiceLocator
import com.bank.salestracker.ui.theme.AppBackground
import com.bank.salestracker.ui.theme.GlassSurface
import com.bank.salestracker.ui.theme.GlassTopBar
import com.bank.salestracker.ui.theme.MetricGlassCard
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
fun AdminScreen(
    onEmployeeClick: (String) -> Unit,
    onProductsClick: () -> Unit,
    vm: AdminVm = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var goalDialogFor by remember { mutableStateOf<EmployeeSummary?>(null) }

    AppBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Box(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    GlassTopBar(
                        title = "Команда",
                        subtitle = "Офис, сотрудники и результаты",
                        trailing = {
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
                            }) {
                                Icon(Icons.Default.FileDownload, "Экспорт CSV")
                            }
                        }
                    )
                }
            }
        ) { pad ->
            LazyColumn(
                Modifier.padding(pad).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                item {
                    GlassSurface(
                        Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onProductsClick),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Продукты офиса", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Баллы, активность и суммы по продуктам",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

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
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                MetricGlassCard("${report.branchTodayCount}", "сегодня", Modifier.weight(1f))
                                MetricGlassCard("${report.branchMonthCount}", "месяц", Modifier.weight(1f))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                MetricGlassCard("%,.0f ₽".format(report.branchMonthAmount), "сумма", Modifier.weight(1f))
                                MetricGlassCard(formatAdminPoints(report.branchMonthPoints), "баллы", Modifier.weight(1f))
                            }
                        }
                    }

                    item { Text("Рейтинг сотрудников", style = MaterialTheme.typography.titleMedium) }

                    val sorted = report.employees.sortedByDescending { it.monthCount }
                    itemsIndexed(sorted, key = { _, employee -> employee.user.id }) { index, employee ->
                        EmployeeRankRow(
                            place = index + 1,
                            employee = employee,
                            onClick = { onEmployeeClick(employee.user.id) },
                            onGoalClick = { goalDialogFor = employee }
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
    GlassSurface(Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                GlassSurface(contentPadding = PaddingValues(12.dp), elevation = 0.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(user.fullName, style = MaterialTheme.typography.titleSmall)
                        Text(user.employeeId, style = MaterialTheme.typography.bodySmall)
                        Text("Статус: ${statusTitle(user.registrationStatus)}", style = MaterialTheme.typography.bodySmall)
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
private fun EmployeeRankRow(
    place: Int,
    employee: EmployeeSummary,
    onClick: () -> Unit,
    onGoalClick: () -> Unit
) {
    GlassSurface(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(34.dp), contentAlignment = Alignment.Center) {
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
                    Text("$place", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(employee.user.fullName, style = MaterialTheme.typography.titleMedium)
                Text(
                    "Сегодня: ${employee.todayCount} · Период: ${employee.monthCount} · ${formatAdminPoints(employee.monthPoints)} б. · %,.0f ₽".format(employee.monthAmount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                employee.goalProgress?.let { progress ->
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        strokeCap = StrokeCap.Round
                    )
                }
            }
            TextButton(onClick = onGoalClick) { Text("План") }
        }
    }
}

private fun statusTitle(status: RegistrationStatus): String = when (status) {
    RegistrationStatus.PENDING_ASSIGNMENT -> "ожидает привязки"
    RegistrationStatus.ACTIVE -> "активен"
    RegistrationStatus.BLOCKED -> "заблокирован"
    RegistrationStatus.DEACTIVATED -> "деактивирован"
}

private fun formatAdminPoints(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)

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

    AppBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Box(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    GlassTopBar(title = "Карточка сотрудника", subtitle = "Продажи и история за период")
                }
            }
        ) { pad ->
            if (vm.loading) {
                Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                return@Scaffold
            }
            LazyColumn(
                Modifier.padding(pad).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                if (vm.sales.isEmpty()) {
                    item {
                        GlassSurface(Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
                            Text("Продаж за период нет")
                        }
                    }
                }
                items(vm.sales) { sale ->
                    GlassSurface(Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(sale.displayTitle(), style = MaterialTheme.typography.titleSmall)
                                sale.amount?.let { Text("%,.0f ₽".format(it), color = MaterialTheme.colorScheme.primary) }
                            }
                            Text("Клиент: ${sale.clientLast4}", style = MaterialTheme.typography.bodySmall)
                            sale.createdAt?.let { Text(it.take(16).replace("T", " "), style = MaterialTheme.typography.bodySmall) }
                            sale.comment?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    }
                }
            }
        }
    }
}
