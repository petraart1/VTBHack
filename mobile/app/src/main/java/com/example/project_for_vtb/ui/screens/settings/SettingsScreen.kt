package com.example.project_for_vtb.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Экран настроек приложения
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit = {}
) {
    var darkTheme by remember { mutableStateOf(false) }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var biometricEnabled by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Профиль
            item {
                SettingsSectionTitle("Профиль")
            }
            
            item {
                SettingsItem(
                    title = "Имя пользователя",
                    subtitle = "Иван Иванов",
                    icon = Icons.Default.Person,
                    onClick = { /* TODO: Редактировать профиль */ }
                )
            }
            
            item {
                SettingsItem(
                    title = "Email",
                    subtitle = "ivan@example.com",
                    icon = Icons.Default.Email,
                    onClick = { /* TODO: Редактировать email */ }
                )
            }
            
            // Внешний вид
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SettingsSectionTitle("Внешний вид")
            }
            
            item {
                SettingsItem(
                    title = "Темная тема",
                    icon = Icons.Default.DarkMode,
                    trailing = {
                        Switch(
                            checked = darkTheme,
                            onCheckedChange = { darkTheme = it }
                        )
                    }
                )
            }
            
            item {
                SettingsItem(
                    title = "Язык",
                    subtitle = "Русский",
                    icon = Icons.Default.Language,
                    onClick = { /* TODO: Выбор языка */ }
                )
            }
            
            // Безопасность
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SettingsSectionTitle("Безопасность")
            }
            
            item {
                SettingsItem(
                    title = "Биометрическая аутентификация",
                    icon = Icons.Default.Fingerprint,
                    trailing = {
                        Switch(
                            checked = biometricEnabled,
                            onCheckedChange = { biometricEnabled = it }
                        )
                    }
                )
            }
            
            item {
                SettingsItem(
                    title = "Изменить PIN",
                    icon = Icons.Default.Lock,
                    onClick = { /* TODO: Изменить PIN */ }
                )
            }
            
            // Уведомления
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SettingsSectionTitle("Уведомления")
            }
            
            item {
                SettingsItem(
                    title = "Push-уведомления",
                    icon = Icons.Default.Notifications,
                    trailing = {
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { notificationsEnabled = it }
                        )
                    }
                )
            }
            
            // О приложении
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SettingsSectionTitle("О приложении")
            }
            
            item {
                SettingsItem(
                    title = "Версия",
                    subtitle = "1.0.0",
                    icon = Icons.Default.Info,
                    onClick = { /* TODO: О приложении */ }
                )
            }
            
            // Выход
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Выйти")
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Surface(
        onClick = onClick ?: {},
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary
            )
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (trailing != null) {
                trailing()
            } else if (onClick != null) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

