@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.bank.salestracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bank.salestracker.data.model.ProductSetting
import com.bank.salestracker.data.model.SaleBatchItem
import com.bank.salestracker.di.ServiceLocator
import com.bank.salestracker.ui.theme.AppBackground
import com.bank.salestracker.ui.theme.GlassSurface
import com.bank.salestracker.ui.theme.GlassTopBar
import com.bank.salestracker.ui.theme.GradientActionButton
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
            message = if (sentOnline) {
                "Продажи записаны"
            } else {
                "Сохранено офлайн, отправится при появлении сети"
            }
            onSaved()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddSaleScreen(onSaved: () -> Unit, vm: AddSaleVm = viewModel()) {
    val snackbar = remember { SnackbarHostState() }
    val selectedPoints = vm.selectedProducts.sumOf { it.points }
    val amountProducts = vm.selectedProducts.filter { it.requiresAmount }

    LaunchedEffect(vm.message) {
        vm.message?.let {
            snackbar.showSnackbar(it)
            vm.message = null
        }
    }

    AppBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Box(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    GlassTopBar(
                        title = "Новая продажа",
                        subtitle = "Последние 4 цифры и все продукты клиента"
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbar) }
        ) { pad ->
            Column(
                Modifier
                    .padding(pad)
                    .padding(16.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                GlassSurface(Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Клиент", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            value = vm.clientLast4,
                            onValueChange = { vm.clientLast4 = it.filter(Char::isDigit).take(4) },
                            label = { Text("Последние 4 цифры телефона") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                GlassSurface(Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Продукты", style = MaterialTheme.typography.titleMedium)
                            Text("${vm.selectedProducts.size} выбрано", color = MaterialTheme.colorScheme.primary)
                        }
                        when {
                            vm.loadingProducts -> Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }

                            vm.products.isEmpty() -> {
                                Text("Для вашего офиса пока нет активных продуктов", color = MaterialTheme.colorScheme.outline)
                                OutlinedButton(onClick = vm::loadProducts) { Text("Обновить") }
                            }

                            else -> FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                vm.products.sortedBy { it.sortOrder }.forEach { product ->
                                    FilterChip(
                                        selected = vm.selectedProducts.any { it.productId == product.productId },
                                        onClick = { vm.toggleProduct(product) },
                                        label = { Text(product.title) },
                                        shape = MaterialTheme.shapes.medium,
                                        colors = FilterChipDefaults.filterChipColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
                                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                if (vm.selectedProducts.isNotEmpty()) {
                    GlassSurface(Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Итог", style = MaterialTheme.typography.titleMedium)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                SummaryPill("${vm.selectedProducts.size}", "продуктов", Modifier.weight(1f))
                                SummaryPill(formatSalePoints(selectedPoints), "баллов", Modifier.weight(1f))
                            }
                            Text(
                                vm.selectedProducts.joinToString { it.title },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (amountProducts.isNotEmpty()) {
                    GlassSurface(Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Суммы по продуктам", style = MaterialTheme.typography.titleMedium)
                            amountProducts.forEach { product ->
                                OutlinedTextField(
                                    value = vm.amounts[product.productId].orEmpty(),
                                    onValueChange = {
                                        vm.amounts[product.productId] = it.filter { ch -> ch.isDigit() || ch == ',' || ch == '.' }
                                    },
                                    label = { Text("${product.title}: сумма, ₽") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                GlassSurface(Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
                    OutlinedTextField(
                        value = vm.comment,
                        onValueChange = { vm.comment = it.take(200) },
                        label = { Text("Комментарий ко всему набору") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }

                GradientActionButton(
                    text = if (vm.loading) "Сохранение..." else "Записать продажи (${vm.selectedProducts.size})",
                    onClick = { vm.save(onSaved) },
                    enabled = !vm.loading && !vm.loadingProducts && vm.products.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                )

                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun SummaryPill(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.68f)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatSalePoints(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)
