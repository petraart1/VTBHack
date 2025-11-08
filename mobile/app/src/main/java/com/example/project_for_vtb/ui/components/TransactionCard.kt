package com.example.project_for_vtb.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.project_for_vtb.domain.model.Transaction
import com.example.project_for_vtb.domain.model.TransactionType
import com.example.project_for_vtb.ui.theme.ExpenseColor
import com.example.project_for_vtb.ui.theme.IncomeColor
import com.example.project_for_vtb.ui.theme.NeutralColor
import java.math.BigDecimal
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Карточка транзакции
 */
@Composable
fun TransactionCard(
    transaction: Transaction,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Иконка категории
            Surface(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                color = getTransactionColor(transaction.type).copy(alpha = 0.1f)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = getTransactionIcon(transaction.type),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
            
            // Информация о транзакции
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = transaction.description,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatDate(transaction.date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    transaction.category?.let { category ->
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = category,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Сумма транзакции
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = formatTransactionAmount(transaction.amount, transaction.type, transaction.currency),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = getTransactionColor(transaction.type)
                )
            }
        }
    }
}

@Composable
private fun getTransactionColor(type: TransactionType): androidx.compose.ui.graphics.Color {
    return when (type) {
        TransactionType.INCOME -> IncomeColor
        TransactionType.EXPENSE -> ExpenseColor
        TransactionType.TRANSFER -> NeutralColor
    }
}

@Composable
private fun getTransactionIcon(type: TransactionType): String {
    return when (type) {
        TransactionType.INCOME -> "↑"
        TransactionType.EXPENSE -> "↓"
        TransactionType.TRANSFER -> "⇄"
    }
}

@Composable
private fun formatTransactionAmount(
    amount: BigDecimal,
    type: TransactionType,
    currency: String
): String {
    val prefix = when (type) {
        TransactionType.INCOME -> "+"
        TransactionType.EXPENSE -> "-"
        TransactionType.TRANSFER -> "±"
    }
    val formatter = NumberFormat.getNumberInstance(Locale("ru", "RU"))
    formatter.maximumFractionDigits = 2
    formatter.minimumFractionDigits = 2
    return "$prefix${formatter.format(amount)} $currency"
}

@Composable
private fun formatDate(date: Date): String {
    val today = Calendar.getInstance()
    val transactionDate = Calendar.getInstance().apply { time = date }
    
    return when {
        today.get(Calendar.DAY_OF_YEAR) == transactionDate.get(Calendar.DAY_OF_YEAR) &&
        today.get(Calendar.YEAR) == transactionDate.get(Calendar.YEAR) -> "Сегодня"
        today.get(Calendar.DAY_OF_YEAR) - 1 == transactionDate.get(Calendar.DAY_OF_YEAR) &&
        today.get(Calendar.YEAR) == transactionDate.get(Calendar.YEAR) -> "Вчера"
        else -> {
            val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale("ru", "RU"))
            dateFormat.format(date)
        }
    }
}

