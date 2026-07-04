@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.bank.salestracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bank.salestracker.data.model.ProductCreateRequest
import com.bank.salestracker.data.model.ProductSetting
import com.bank.salestracker.data.model.ProductSettingPatch
import com.bank.salestracker.data.model.Role
import com.bank.salestracker.di.ServiceLocator
import kotlinx.coroutines.launch

class ProductManagementVm : ViewModel() {
    var products by mutableStateOf<List<ProductSetting>>(emptyList())
    var query by mutableStateOf("")
    var loading by mutableStateOf(true)
    var savingProductId by mutableStateOf<String?>(null)
    var message by mutableStateOf<String?>(null)
    private val orgUnitId: String? get() = ServiceLocator.authRepo.currentUser()?.orgUnitId

    init {
        refresh()
    }

    fun refresh() = viewModelScope.launch {
        val org = orgUnitId
        if (org == null) {
            message = "Не найден офис пользователя"
            loading = false
            return@launch
        }
        loading = true
        runCatching { products = ServiceLocator.salesRepo.officeProducts(org) }
            .onFailure { message = "Не удалось загрузить продукты" }
        loading = false
    }

    fun updateProduct(
        product: ProductSetting,
        points: Double,
        active: Boolean,
        requiresAmount: Boolean,
        countsTowardPlan: Boolean
    ) = viewModelScope.launch {
        val org = orgUnitId ?: return@launch
        savingProductId = product.productId
        runCatching {
            ServiceLocator.salesRepo.updateOfficeProduct(
                org,
                product.productId,
                ProductSettingPatch(
                    active = active,
                    points = points,
                    requiresAmount = requiresAmount,
                    countsTowardPlan = countsTowardPlan
                )
            )
        }.onSuccess { updated ->
            products = products.map { if (it.productId == updated.productId) updated else it }
            message = "Продукт обновлен"
        }.onFailure {
            message = "Не удалось сохранить продукт"
        }
        savingProductId = null
    }

    fun createProduct(code: String, title: String, groupName: String) = viewModelScope.launch {
        if (code.isBlank() || title.isBlank()) {
            message = "Заполните код и название"
            return@launch
        }
        loading = true
        runCatching {
            ServiceLocator.salesRepo.createProduct(
                ProductCreateRequest(
                    code = code.trim().uppercase(),
                    title = title.trim(),
                    groupName = groupName.trim()
                )
            )
        }.onSuccess {
            message = "Продукт создан. Включите его для офиса"
            refresh()
        }.onFailure {
            message = "Не удалось создать продукт"
        }
        loading = false
    }
}

@Composable
fun ProductManagementScreen(vm: ProductManagementVm = viewModel()) {
    val snackbar = remember { SnackbarHostState() }
    val user = ServiceLocator.authRepo.currentUser()
    var createDialog by remember { mutableStateOf(false) }
    val filtered = vm.products.filter {
        val q = vm.query.trim()
        q.isEmpty() || it.title.contains(q, ignoreCase = true) || it.code.contains(q, ignoreCase = true)
    }

    LaunchedEffect(vm.message) {
        vm.message?.let {
            snackbar.showSnackbar(it)
            vm.message = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Продукты офиса") },
                actions = {
                    if (user?.role == Role.ADMIN) {
                        IconButton(onClick = { createDialog = true }) {
                            Icon(Icons.Default.Add, "Добавить продукт")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = vm.query,
                    onValueChange = { vm.query = it },
                    label = { Text("Поиск продукта") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (vm.loading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (filtered.isEmpty()) {
                item { Text("Продукты не найдены", color = MaterialTheme.colorScheme.outline) }
            } else {
                items(filtered, key = { it.productId }) { product ->
                    ProductSettingCard(
                        product = product,
                        saving = vm.savingProductId == product.productId,
                        onSave = vm::updateProduct
                    )
                }
            }
        }
    }

    if (createDialog) {
        CreateProductDialog(
            onDismiss = { createDialog = false },
            onCreate = { code, title, group ->
                createDialog = false
                vm.createProduct(code, title, group)
            }
        )
    }
}

@Composable
private fun ProductSettingCard(
    product: ProductSetting,
    saving: Boolean,
    onSave: (ProductSetting, Double, Boolean, Boolean, Boolean) -> Unit
) {
    var active by remember(product.productId, product.active) { mutableStateOf(product.active) }
    var requiresAmount by remember(product.productId, product.requiresAmount) { mutableStateOf(product.requiresAmount) }
    var countsTowardPlan by remember(product.productId, product.countsTowardPlan) { mutableStateOf(product.countsTowardPlan) }
    var pointsText by remember(product.productId, product.points) { mutableStateOf(formatPoints(product.points)) }
    val points = pointsText.replace(",", ".").toDoubleOrNull()

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(product.title, style = MaterialTheme.typography.titleMedium)
                    Text("${product.groupName.ifBlank { "Без группы" }} · ${product.code}", style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = active, onCheckedChange = { active = it })
            }

            OutlinedTextField(
                value = pointsText,
                onValueChange = { pointsText = it.filter { ch -> ch.isDigit() || ch == ',' || ch == '.' }.take(8) },
                label = { Text("Баллы за продажу") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                isError = points == null,
                modifier = Modifier.fillMaxWidth()
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Требует сумму")
                Switch(checked = requiresAmount, onCheckedChange = { requiresAmount = it })
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Учитывать в плане")
                Switch(checked = countsTowardPlan, onCheckedChange = { countsTowardPlan = it })
            }

            Button(
                onClick = { points?.let { onSave(product, it, active, requiresAmount, countsTowardPlan) } },
                enabled = !saving && points != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Save, null)
                Spacer(Modifier.width(8.dp))
                Text(if (saving) "Сохранение..." else "Сохранить")
            }
        }
    }
}

@Composable
private fun CreateProductDialog(onDismiss: () -> Unit, onCreate: (String, String, String) -> Unit) {
    var code by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var group by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый продукт") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase().filter { ch -> ch.isLetterOrDigit() || ch == '_' }.take(40) },
                    label = { Text("Код") },
                    placeholder = { Text("NEW_PRODUCT") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(80) },
                    label = { Text("Название") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = group,
                    onValueChange = { group = it.take(60) },
                    label = { Text("Группа") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onCreate(code, title, group) }) { Text("Создать") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

private fun formatPoints(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
