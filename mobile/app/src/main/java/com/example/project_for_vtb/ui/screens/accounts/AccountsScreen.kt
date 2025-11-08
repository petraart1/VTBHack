package com.example.project_for_vtb.ui.screens.accounts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.project_for_vtb.domain.model.Account
import com.example.project_for_vtb.ui.components.AccountCard as AccountCardComponent
import java.math.BigDecimal

/**
 * Экран списка счетов с улучшенным дизайном
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddBank: () -> Unit = {}
) {
    // TODO: Получить из ViewModel/UseCase
    val accounts = remember {
        mutableStateListOf<Account>(
            Account(
                id = "1",
                bankId = "vtb",
                accountNumber = "40817810099910004312",
                accountType = com.example.project_for_vtb.domain.model.AccountType.DEBIT_CARD,
                balance = BigDecimal("125000.50"),
                currency = "₽",
                name = "Основная карта",
                isActive = true
            ),
            Account(
                id = "2",
                bankId = "sber",
                accountNumber = "40817810099910004313",
                accountType = com.example.project_for_vtb.domain.model.AccountType.CHECKING,
                balance = BigDecimal("50000.00"),
                currency = "₽",
                name = "Расчетный счет",
                isActive = true
            )
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Счета и карты") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToAddBank) {
                        Icon(Icons.Default.Add, contentDescription = "Добавить банк")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddBank,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить счет")
            }
        }
    ) { paddingValues ->
        if (accounts.isEmpty()) {
            EmptyAccountsState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                onAddBank = onNavigateToAddBank
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "Подключенные счета (${accounts.size})",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                items(accounts) { account ->
                    AccountCardComponent(
                        account = account,
                        onClick = {
                            // TODO: Навигация к деталям счета
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyAccountsState(
    modifier: Modifier = Modifier,
    onAddBank: () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🏦",
            style = MaterialTheme.typography.displayLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Нет подключенных счетов",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Подключите банк, чтобы начать управлять финансами",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onAddBank) {
            Text("Подключить банк")
        }
    }
}

