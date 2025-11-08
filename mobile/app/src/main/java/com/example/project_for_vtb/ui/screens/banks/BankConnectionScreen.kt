package com.example.project_for_vtb.ui.screens.banks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.project_for_vtb.domain.model.Bank

/**
 * Экран подключения банков через OAuth2
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankConnectionScreen(
    onNavigateBack: () -> Unit,
    onConnectBank: (Bank) -> Unit = {}
) {
    // TODO: Получить из ViewModel/UseCase
    val availableBanks = remember {
        listOf(
            Bank(
                id = "vtb",
                name = "ВТБ",
                logoUrl = null,
                authUrl = "https://api.vtb.ru/oauth/authorize",
                tokenUrl = "https://api.vtb.ru/oauth/token",
                clientId = "vtb_client_id",
                isConnected = true,
                connectedAt = System.currentTimeMillis()
            ),
            Bank(
                id = "sber",
                name = "Сбербанк",
                logoUrl = null,
                authUrl = "https://api.sberbank.ru/oauth/authorize",
                tokenUrl = "https://api.sberbank.ru/oauth/token",
                clientId = "sber_client_id",
                isConnected = false,
                connectedAt = null
            ),
            Bank(
                id = "tinkoff",
                name = "Тинькофф",
                logoUrl = null,
                authUrl = "https://api.tinkoff.ru/oauth/authorize",
                tokenUrl = "https://api.tinkoff.ru/oauth/token",
                clientId = "tinkoff_client_id",
                isConnected = false,
                connectedAt = null
            )
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Подключение банков") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Подключите банки",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Подключите свои банковские счета через безопасное OAuth2 подключение",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            
            item {
                Text(
                    text = "Доступные банки",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            items(availableBanks) { bank ->
                BankCard(
                    bank = bank,
                    onConnect = { onConnectBank(bank) }
                )
            }
        }
    }
}

@Composable
private fun BankCard(
    bank: Bank,
    onConnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Логотип банка (заглушка)
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = bank.name.take(1),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Column {
                    Text(
                        text = bank.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (bank.isConnected) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Подключено",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
            
            if (bank.isConnected) {
                TextButton(onClick = { /* TODO: Отключить банк */ }) {
                    Text("Управление")
                }
            } else {
                Button(
                    onClick = onConnect,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Подключить")
                }
            }
        }
    }
}

