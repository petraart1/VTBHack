package com.example.project_for_vtb.ui.screens.analytics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.project_for_vtb.ui.theme.ExpenseColor
import com.example.project_for_vtb.ui.theme.IncomeColor
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.*

/**
 * Экран аналитики с графиками трат и категорий
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onNavigateBack: () -> Unit
) {
    // TODO: Получить данные из ViewModel/UseCase
    val totalIncome = remember { BigDecimal("150000.00") }
    val totalExpense = remember { BigDecimal("75000.00") }
    val categories = remember {
        listOf(
            CategoryData("Продукты", BigDecimal("25000.00"), ExpenseColor),
            CategoryData("Транспорт", BigDecimal("15000.00"), ExpenseColor),
            CategoryData("Развлечения", BigDecimal("10000.00"), ExpenseColor),
            CategoryData("Зарплата", BigDecimal("150000.00"), IncomeColor)
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Аналитика") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Сводка доходов и расходов
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaryCard(
                        title = "Доходы",
                        amount = totalIncome,
                        color = IncomeColor,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        title = "Расходы",
                        amount = totalExpense,
                        color = ExpenseColor,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            // График трат (заглушка)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "График трат",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        // TODO: Добавить график с использованием Vico Charts
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "График будет здесь",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            // Категории трат
            item {
                Text(
                    text = "По категориям",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            items(
                count = categories.size,
                key = { index -> categories[index].name }
            ) { index ->
                CategoryCard(category = categories[index])
            }
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    amount: BigDecimal,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatAmount(amount),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun CategoryCard(category: CategoryData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Прогресс бар (заглушка)
                LinearProgressIndicator(
                    progress = 0.5f,
                    modifier = Modifier.fillMaxWidth(),
                    color = category.color
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = formatAmount(category.amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = category.color
            )
        }
    }
}

@Composable
private fun formatAmount(amount: BigDecimal): String {
    val formatter = NumberFormat.getNumberInstance(Locale("ru", "RU"))
    formatter.maximumFractionDigits = 2
    formatter.minimumFractionDigits = 2
    return "${formatter.format(amount)} ₽"
}

private data class CategoryData(
    val name: String,
    val amount: BigDecimal,
    val color: androidx.compose.ui.graphics.Color
)
