package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConnectionStatus
import com.example.ui.components.AircraftTableView
import com.example.ui.components.DebugDialog
import com.example.ui.components.OsmMapView
import com.example.ui.components.SettingsDialog
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CoralRed
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun PlanesScreen(
    viewModel: PlanesViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground),
        topBar = {
            TopStatusBar(
                connectionStatus = uiState.connectionStatus,
                aircraftCount = uiState.totalPlanesCount,
                serverAddress = uiState.serverAddress,
                onReconnect = { viewModel.reconnect() },
                onOpenDebug = { viewModel.openDebug() },
                onOpenSettings = { viewModel.openSettings() }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Upper half: osmdroid MapView
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                OsmMapView(
                    aircraftList = uiState.aircraftList,
                    selectedAircraft = uiState.selectedAircraft,
                    panToAircraft = uiState.panToAircraft,
                    fitAllPlanesTrigger = uiState.fitAllPlanesTrigger,
                    onAircraftSelected = { viewModel.selectAircraft(it) },
                    onCenterOnAllPlanes = { viewModel.centerOnAllPlanes() },
                    onPanTargetHandled = { viewModel.resetPanTarget() }
                )
            }

            // Divider between Map and Table with tactical style
            HorizontalDivider(
                thickness = 2.dp,
                color = CyanNeon.copy(alpha = 0.35f)
            )

            // Lower half: Aircraft table
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                AircraftTableView(
                    aircraftList = uiState.aircraftList,
                    selectedAircraft = uiState.selectedAircraft,
                    sortColumn = uiState.sortColumn,
                    sortDirection = uiState.sortDirection,
                    currentTime = uiState.currentTime,
                    onHeaderClicked = { viewModel.onSortHeaderClicked(it) },
                    onRowClicked = { aircraft ->
                        viewModel.centerOnAircraft(aircraft)
                    }
                )
            }
        }
    }

    // Settings Dialog
    if (uiState.isSettingsOpen) {
        SettingsDialog(
            currentAddress = uiState.serverAddress,
            onSave = { viewModel.saveServerAddress(it) },
            onDismiss = { viewModel.closeSettings() }
        )
    }

    // Debug Dialog
    if (uiState.isDebugOpen) {
        DebugDialog(
            messages = uiState.rawMessages,
            serverAddress = uiState.serverAddress,
            connectionStatus = uiState.connectionStatus,
            onClear = { viewModel.clearRawMessages() },
            onDismiss = { viewModel.closeDebug() }
        )
    }
}

@Composable
private fun TopStatusBar(
    connectionStatus: ConnectionStatus,
    aircraftCount: Int,
    serverAddress: String,
    onReconnect: () -> Unit,
    onOpenDebug: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = DarkSurface,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App Logo & Title
                Icon(
                    imageVector = Icons.Default.Flight,
                    contentDescription = null,
                    tint = CyanNeon,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = "Planes ADS-B",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "OpenWebRX+ • $serverAddress",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Debug Screen Button
                IconButton(
                    onClick = onOpenDebug,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("open_debug_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = "Отладка",
                        tint = CyanNeon,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Settings Button
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("open_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Настройки",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Sub-status Row: Status indicator & Plane Count
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Connection Status Chip
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceVariant)
                        .clickable(enabled = connectionStatus == ConnectionStatus.DISCONNECTED) {
                            onReconnect()
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("connection_status_badge"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val (statusColor, statusText) = when (connectionStatus) {
                        ConnectionStatus.CONNECTED -> EmeraldGreen to "Подключено"
                        ConnectionStatus.CONNECTING -> AmberAccent to "Подключение..."
                        ConnectionStatus.DISCONNECTED -> CoralRed to "Отключено (повтор 5с)"
                    }

                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor
                    )

                    if (connectionStatus == ConnectionStatus.DISCONNECTED) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Переподключить",
                            tint = CoralRed,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // Aircraft Count Chip
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyanNeon.copy(alpha = 0.12f))
                        .border(1.dp, CyanNeon.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("aircraft_count_badge"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Самолётов: ",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = "$aircraftCount",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CyanNeon
                    )
                }
            }
        }
    }
}
