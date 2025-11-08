package com.example.project_for_vtb.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import com.example.project_for_vtb.R
import com.example.project_for_vtb.ui.theme.CardGradientEnd
import com.example.project_for_vtb.ui.theme.CardGradientStart
import com.example.project_for_vtb.ui.theme.CardTextWhite
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.*

/**
 * Компонент банковской карты для главного экрана
 * 
 * Соответствует ТЗ:
 * - Размер: 350dp x 208dp
 * - Градиент от светло-голубого к сиреневому
 * - Скруглённые углы 12-16dp, тень
 * - Элементы: чип, номер карты, логотип VISA, сумма с иконкой глаза, NFC-значок
 * - Возможность показывать/скрывать сумму с анимацией
 * - Кликабельность всей карты
 */
@Composable
fun BankCard(
    balance: BigDecimal,
    cardNumber: String = "**** 9749",
    currency: String = "₽",
    isBalanceVisible: Boolean = false,
    onBalanceVisibilityToggle: () -> Unit = {},
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Логирование для отладки
    android.util.Log.d("BankCard", "💳 BankCard рендерится: баланс=$balance, валюта=$currency, видим=$isBalanceVisible, номер=$cardNumber")
    
    // Пропорции карты: 350dp x 208dp (примерно 1.68:1)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(350f / 208f)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color.Black.copy(alpha = 0.2f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        CardGradientStart,
                        CardGradientEnd
                    )
                )
            )
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        // Верхняя часть карты - Чип (слева-вверху) и NFC-значок (справа-вверху)
        // Чип в левом верхнем углу
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(top = 0.dp, start = 0.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_chip),
                    contentDescription = "Чип карты",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        
        // NFC-значок в правом верхнем углу
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 0.dp, end = 0.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_nfc),
                contentDescription = "NFC",
                modifier = Modifier
                    .width(32.dp)
                    .height(24.dp)
            )
        }
        
        // Центральная часть - Иконка глаза и Сумма (справа по центру)
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Иконка глаза (перед суммой)
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clickable(
                        onClick = onBalanceVisibilityToggle,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isBalanceVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = if (isBalanceVisible) "Скрыть баланс" else "Показать баланс",
                    tint = CardTextWhite.copy(alpha = 0.95f),
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Анимированное отображение суммы (текст всегда занимает одинаковое место)
            Box(
                modifier = Modifier.widthIn(min = 120.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Crossfade(
                    targetState = isBalanceVisible,
                    animationSpec = tween(durationMillis = 300),
                    label = "balance_crossfade"
                ) { visible ->
                    if (visible) {
                        Column(
                            horizontalAlignment = Alignment.End
                        ) {
                            val balanceText = formatBalance(balance)
                            android.util.Log.d("BankCard", "💰 Отображаем баланс: $balanceText $currency")
                            Text(
                                text = balanceText,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = CardTextWhite.copy(alpha = 1f),
                                lineHeight = 32.sp
                            )
                            Text(
                                text = currency,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = CardTextWhite.copy(alpha = 0.9f)
                            )
                        }
                    } else {
                        Text(
                            text = "****** $currency",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = CardTextWhite.copy(alpha = 1f),
                            lineHeight = 32.sp
                        )
                    }
                }
            }
        }
        
        // Нижняя часть карты
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            // Номер карты (внизу слева)
            Text(
                text = cardNumber,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = CardTextWhite.copy(alpha = 0.95f),
                letterSpacing = 2.sp
            )
            
            // Логотип VISA (справа-внизу)
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                // Простой текст VISA (в реальном приложении можно использовать изображение)
                Text(
                    text = "VISA",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = CardTextWhite.copy(alpha = 0.95f),
                    letterSpacing = 1.sp
                )
            }
        }
    }
}


@Composable
private fun formatBalance(balance: BigDecimal): String {
    val formatter = NumberFormat.getNumberInstance(Locale.forLanguageTag("ru-RU"))
    formatter.maximumFractionDigits = 2
    formatter.minimumFractionDigits = 2
    return formatter.format(balance)
}

