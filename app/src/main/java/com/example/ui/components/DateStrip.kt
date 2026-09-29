package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.util.PlantUtils

@Composable
fun DateStrip(
    selectedDate: String,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val weekDays = remember { PlantUtils.getCurrentWeekDays() }
    val todayString = remember { PlantUtils.getTodayDateString() }
    val listState = rememberLazyListState()

    val selectedIndex = remember(weekDays, selectedDate) {
        weekDays.indexOfFirst { it.dateString == selectedDate }.coerceAtLeast(0)
    }

    LaunchedEffect(selectedIndex) {
        if (selectedIndex >= 0 && selectedIndex < weekDays.size) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val totalAvailableWidth = maxWidth
        val minItemWidth = 44.dp
        val defaultSpacing = 8.dp
        val totalCount = weekDays.size

        val canFitAll = totalAvailableWidth > 0.dp &&
                (totalAvailableWidth >= (minItemWidth * totalCount + defaultSpacing * (totalCount - 1) + 4.dp))

        val (itemWidth, spacing) = if (canFitAll) {
            val dynamicSpacing = (totalAvailableWidth * 0.02f).coerceIn(4.dp, 8.dp)
            val totalSpacing = dynamicSpacing * (totalCount - 1) + 4.dp
            val calculatedWidth = (totalAvailableWidth - totalSpacing) / totalCount
            calculatedWidth.coerceIn(44.dp, 56.dp) to dynamicSpacing
        } else {
            48.dp to 8.dp
        }

        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
        ) {
            items(weekDays, key = { it.dateString }) { day ->
                val isSelected = day.dateString == selectedDate
                val isToday = day.dateString == todayString

                val containerModifier = if (isSelected) {
                    Modifier
                        .width(itemWidth)
                        .defaultMinSize(minHeight = 70.dp)
                        .shadow(
                            elevation = 6.dp,
                            shape = RoundedCornerShape(20.dp),
                            spotColor = PlantPrimaryDark,
                            ambientColor = PlantPrimary
                        )
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    PlantPrimary,
                                    PlantPrimaryDark
                                )
                            )
                        )
                        .border(
                            1.dp,
                            Color.White.copy(alpha = 0.5f),
                            RoundedCornerShape(20.dp)
                        )
                } else {
                    Modifier
                        .width(itemWidth)
                        .defaultMinSize(minHeight = 70.dp)
                        .shadow(
                            elevation = 2.dp,
                            shape = RoundedCornerShape(20.dp),
                            spotColor = Color(0x0F000000)
                        )
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isToday) PlantTheme.colors.accentSoft else PlantTheme.colors.surfaceElevated
                        )
                        .border(
                            1.dp,
                            if (isToday) PlantTheme.colors.accentSoftBorder else PlantTheme.colors.border,
                            RoundedCornerShape(20.dp)
                        )
                }

                Column(
                    modifier = containerModifier
                        .clickable { onDateSelected(day.dateString) }
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = day.dayName,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 12.sp,
                            fontWeight = if (isSelected || isToday) FontWeight.SemiBold else FontWeight.Medium,
                            color = when {
                                isSelected -> Color.White.copy(alpha = 0.9f)
                                isToday -> PlantTheme.colors.accent
                                else -> PlantTextSecondary
                            }
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = day.dayNumber.toString(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isSelected -> Color.White
                                isToday -> PlantTheme.colors.accent
                                else -> PlantText
                            }
                        )
                    )
                }
            }
        }
    }
}
