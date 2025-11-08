package com.example.project_for_vtb.ui.screens.transactions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.project_for_vtb.domain.model.Transaction
import com.example.project_for_vtb.domain.model.TransactionType
import com.example.project_for_vtb.ui.components.TransactionCard as TransactionCardComponent
import java.math.BigDecimal
import java.util.Date

/**
 * Экран списка транзакций с фильтрами и поиском
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    onNavigateBack: () -> Unit
) {
    // TODO: Получить из ViewModel/UseCase
    val transactions = remember {
        mutableStateListOf<Transaction>(
            Transaction(
                id = "1",
                accountId = "1",
                bankId = "vtb",
                amount = BigDecimal("1500.00"),
                currency = "₽",
                type = TransactionType.EXPENSE,
                category = "Продукты",
                description = "Покупка в магазине",
                date = Date(),
                merchantName = "Магазин",
                reference = "REF123"
            ),
            Transaction(
                id = "2",
                accountId = "1",
                bankId = "vtb",
                amount = BigDecimal("50000.00"),
                currency = "₽",
                type = TransactionType.INCOME,
                category = "Зарплата",
                description = "Зарплата",
                date = Date(System.currentTimeMillis() - 86400000),
                merchantName = "Работодатель",
                reference = "REF124"
            )
        )
    }
    
    var searchQuery by remember { mutableStateOf("") }
    var showFilters by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Транзакции") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Фильтры")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Поиск
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Поиск транзакций...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Поиск")
                },
                singleLine = true
            )
            
            // Фильтры (если показаны)
            if (showFilters) {
                // TODO: Добавить фильтры по дате, типу, категории
            }
            
            // Список транзакций
            if (transactions.isEmpty()) {
                EmptyTransactionsState(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                )
            } else {
                val filteredTransactions = transactions.filter {
                    it.description.contains(searchQuery, ignoreCase = true) ||
                    it.category?.contains(searchQuery, ignoreCase = true) == true
                }
                
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredTransactions) { transaction ->
                        TransactionCardComponent(
                            transaction = transaction,
                            onClick = {
                                // TODO: Навигация к деталям транзакции
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyTransactionsState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "📋",
                style = MaterialTheme.typography.displayLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Нет транзакций",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Транзакции появятся здесь после подключения банков",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

