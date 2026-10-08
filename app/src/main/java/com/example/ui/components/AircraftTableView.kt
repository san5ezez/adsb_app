package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Aircraft
import com.example.model.SortColumn
import com.example.model.SortDirection
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AircraftTableView(
    aircraftList: List<Aircraft>,
    selectedAircraft: Aircraft?,
    sortColumn: SortColumn,
    sortDirection: SortDirection,
    currentTime: Long,
    onHeaderClicked: (SortColumn) -> Unit,
    onRowClicked: (Aircraft) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkSurface)
    ) {
        // Table Header with sort buttons
        TableHeaderRow(
            currentSort = sortColumn,
            direction = sortDirection,
            onHeaderClicked = onHeaderClicked
        )

        HorizontalDivider(
            thickness = 1.dp,
            color = BorderSubtle
        )

        if (aircraftList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Flight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Нет активных бортов ADSB",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Ожидание пакетов type=\"update\" с сервера...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("aircraft_table_list")
            ) {
                items(
                    items = aircraftList,
                    key = { it.icao }
                ) { aircraft ->
                    val isSelected = aircraft.icao == selectedAircraft?.icao
                    AircraftTableRow(
                        aircraft = aircraft,
                        isSelected = isSelected,
                        currentTime = currentTime,
                        onClick = { onRowClicked(aircraft) }
                    )
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = BorderSubtle.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TableHeaderRow(
    currentSort: SortColumn,
    direction: SortDirection,
    onHeaderClicked: (SortColumn) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurfaceVariant)
            .padding(vertical = 8.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HeaderCell(
            title = "Позывной",
            column = SortColumn.CALLSIGN,
            currentSort = currentSort,
            direction = direction,
            weight = 1.15f,
            alignment = TextAlign.Start,
            onClick = onHeaderClicked
        )
        HeaderCell(
            title = "ICAO",
            column = SortColumn.ICAO,
            currentSort = currentSort,
            direction = direction,
            weight = 0.95f,
            alignment = TextAlign.Start,
            onClick = onHeaderClicked
        )
        HeaderCell(
            title = "Высота",
            column = SortColumn.ALTITUDE,
            currentSort = currentSort,
            direction = direction,
            weight = 0.95f,
            alignment = TextAlign.End,
            onClick = onHeaderClicked
        )
        HeaderCell(
            title = "Скор.",
            column = SortColumn.SPEED,
            currentSort = currentSort,
            direction = direction,
            weight = 0.85f,
            alignment = TextAlign.End,
            onClick = onHeaderClicked
        )
        HeaderCell(
            title = "Курс",
            column = SortColumn.COURSE,
            currentSort = currentSort,
            direction = direction,
            weight = 0.80f,
            alignment = TextAlign.End,
            onClick = onHeaderClicked
        )
        HeaderCell(
            title = "Время",
            column = SortColumn.AGE,
            currentSort = currentSort,
            direction = direction,
            weight = 0.95f,
            alignment = TextAlign.End,
            onClick = onHeaderClicked
        )
    }
}

@Composable
private fun RowScope.HeaderCell(
    title: String,
    column: SortColumn,
    currentSort: SortColumn,
    direction: SortDirection,
    weight: Float,
    alignment: TextAlign,
    onClick: (SortColumn) -> Unit
) {
    val isSorted = currentSort == column

    Row(
        modifier = Modifier
            .weight(weight)
            .clip(RoundedCornerShape(4.dp))
            .clickable { onClick(column) }
            .padding(vertical = 4.dp, horizontal = 2.dp)
            .testTag("sort_header_${column.name.lowercase()}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = when (alignment) {
            TextAlign.End -> Arrangement.End
            TextAlign.Center -> Arrangement.Center
            else -> Arrangement.Start
        }
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSorted) FontWeight.Bold else FontWeight.Medium,
            color = if (isSorted) CyanNeon else TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (isSorted) {
            Icon(
                imageVector = if (direction == SortDirection.ASCENDING) {
                    Icons.Default.ArrowUpward
                } else {
                    Icons.Default.ArrowDownward
                },
                contentDescription = null,
                tint = CyanNeon,
                modifier = Modifier
                    .padding(start = 2.dp)
                    .size(12.dp)
            )
        }
    }
}

@Composable
private fun AircraftTableRow(
    aircraft: Aircraft,
    isSelected: Boolean,
    currentTime: Long,
    onClick: () -> Unit
) {
    val secondsAgo = aircraft.secondsAgo(currentTime)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (isSelected) AmberAccent.copy(alpha = 0.15f) else Color.Transparent
            )
            .then(
                if (isSelected) Modifier.border(1.dp, AmberAccent.copy(alpha = 0.6f)) else Modifier
            )
            .padding(vertical = 10.dp, horizontal = 6.dp)
            .testTag("aircraft_row_${aircraft.icao}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Callsign
        Text(
            text = aircraft.displayCallsign,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = if (isSelected) AmberAccent else CyanBright,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.15f)
        )

        // ICAO
        Text(
            text = aircraft.icao,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = TextSecondary,
            maxLines = 1,
            modifier = Modifier.weight(0.95f)
        )

        // Altitude (ft)
        Text(
            text = aircraft.altitude?.let { "${it}ft" } ?: "---",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.End,
            color = TextPrimary,
            maxLines = 1,
            modifier = Modifier.weight(0.95f)
        )

        // Speed (kts)
        Text(
            text = aircraft.speed?.let { "${it}kt" } ?: "---",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.End,
            color = TextPrimary,
            maxLines = 1,
            modifier = Modifier.weight(0.85f)
        )

        // Course (°)
        Text(
            text = "${aircraft.course.toInt()}°",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.End,
            color = TextPrimary,
            maxLines = 1,
            modifier = Modifier.weight(0.80f)
        )

        // Seconds ago
        Text(
            text = "${secondsAgo}с",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.End,
            color = if (secondsAgo > 30) Color(0xFFFF8A80) else TextSecondary,
            maxLines = 1,
            modifier = Modifier.weight(0.95f)
        )
    }
}
