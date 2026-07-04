@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.bank.salestracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bank.salestracker.data.model.ProductSetting
import com.bank.salestracker.data.model.SaleBatchItem
import com.bank.salestracker.di.ServiceLocator
import kotlinx.coroutines.launch

class AddSaleVm : ViewModel() {
    var clientLast4 by mutableStateOf("")
    var products by mutableStateOf<List<ProductSetting>>(emptyList())
    var selectedProducts = mutableStateListOf<ProductSetting>()
    var amounts = mutableStateMapOf<String, String>()
    var comment by mutableStateOf("")
    var loading by mutableStateOf(false)
    var loadingProducts by mutableStateOf(true)
    var message by mutableStateOf<String?>(null)

    init {
        loadProducts()
    }

    fun loadProducts() = viewModelScope.launch {
        loadingProducts = true
        runCatching { products = ServiceLocator.salesRepo.activeProducts() }
            .onFailure { message = "Не удалось загрузить продукты офиса" }
        loadingProducts = false
    }

    fun toggleProduct(product: ProductSetting) {
        if (selectedProducts.any { it.productId == product.productId }) {
            selectedProducts.removeAll { it.productId == product.productId }
            amounts.remove(product.productId)
        } else {
            selectedProducts.add(product)
        }
    }

    fun save(onSaved: () -> Unit) {
        val last4 = clientLast4.filter(Char::isDigit).take(4)
        if (last4.length != 4) {
            message = "Введите последние 4 цифры телефона клиента"
            return
        }
        if (selectedProducts.isEmpty()) {
            message = "Выберите хотя бы один продукт"
            return
        }

        val items = mutableListOf<SaleBatchItem>()
        for (product in selectedProducts) {
            val amount = amounts[product.productId]?.replace(",", ".")?.toDoubleOrNull()
            if (product.requiresAmount && (amount == null || amount <= 0)) {
                message = "Укажите сумму для ${product.title}"
                return
            }
            items.add(
                SaleBatchItem(
                    productId = product.productId,
                    amount = amount,
                    quantity = 1,
                    comment = comment.ifBlank { null }
                )
            )
        }

        viewModelScope.launch {
            loading = true
            message = null
            val sentOnline = ServiceLocator.salesRepo.addSalesBatch(last4, items)
            loading = false
            message = if (sentOnline) "Продажи записаны" else "Сохранено офлайн, отправится при появлении сети"
            onSaved()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddSaleScreen(onSaved: () -> Unit, vm: AddSaleVm = viewModel()) {
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(vm.message) {
        vm.message?.let {
            snackbar.showSnackbar(it)
            vm.message = null
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Новая продажа") }) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { pad ->
        Column(
            Modifier.padding(pad).padding(16.dp).fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = vm.clientLast4,
                onValueChange = { vm.clientLast4 = it.filter(Char::isDigit).take(4) },
                label = { Text("Последние 4 цифры телефона") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Продукты", style = MaterialTheme.typography.titleSmall)
            when {
                vm.loadingProducts -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    CircularProgressIndicator()
                }
                vm.products.isEmpty() -> {
                    Text("Для вашего офиса пока нет активных продуктов", color = MaterialTheme.colorScheme.outline)
                    OutlinedButton(onClick = vm::loadProducts) { Text("Обновить") }
                }
                else -> FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    vm.products.sortedBy { it.sortOrder }.forEach { product ->
                        FilterChip(
                            selected = vm.selectedProducts.any { it.productId == product.productId },
                            onClick = { vm.toggleProduct(product) },
                            label = { Text(product.title) }
                        )
                    }
                }
            }

            vm.selectedProducts.filter { it.requiresAmount }.forEach { product ->
                OutlinedTextField(
                    value = vm.amounts[product.productId].orEmpty(),
                    onValueChange = { vm.amounts[product.productId] = it.filter { ch -> ch.isDigit() || ch == ',' || ch == '.' } },
                    label = { Text("${product.title}: сумма, ₽") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            OutlinedTextField(
                value = vm.comment,
                onValueChange = { vm.comment = it.take(200) },
                label = { Text("Комментарий ко всему набору") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Button(
                onClick = { vm.save(onSaved) },
                enabled = !vm.loading && !vm.loadingProducts && vm.products.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                val count = vm.selectedProducts.size
                Text(if (vm.loading) "Сохранение..." else "Записать продажи ($count)")
            }
        }
    }
}
