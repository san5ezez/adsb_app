package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SettingsRepository
import com.example.data.WebSocketManager
import com.example.model.Aircraft
import com.example.model.ConnectionStatus
import com.example.model.RawMessage
import com.example.model.SortColumn
import com.example.model.SortDirection
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

data class PlanesUiState(
    val connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val serverAddress: String = SettingsRepository.DEFAULT_SERVER,
    val aircraftList: List<Aircraft> = emptyList(),
    val totalPlanesCount: Int = 0,
    val selectedAircraft: Aircraft? = null,
    val panToAircraft: Aircraft? = null,
    val fitAllPlanesTrigger: Long = 0L,
    val sortColumn: SortColumn = SortColumn.CALLSIGN,
    val sortDirection: SortDirection = SortDirection.ASCENDING,
    val rawMessages: List<RawMessage> = emptyList(),
    val isSettingsOpen: Boolean = false,
    val isDebugOpen: Boolean = false,
    val currentTime: Long = System.currentTimeMillis()
)

class PlanesViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepository = SettingsRepository(application)
    private val webSocketManager = WebSocketManager()

    private val aircraftMap = ConcurrentHashMap<String, Aircraft>()

    private val _uiState = MutableStateFlow(PlanesUiState())
    val uiState: StateFlow<PlanesUiState> = _uiState.asStateFlow()

    init {
        // Collect server address from DataStore and start WebSocket
        viewModelScope.launch {
            settingsRepository.serverAddressFlow.collectLatest { address ->
                _uiState.value = _uiState.value.copy(serverAddress = address)
                webSocketManager.start(address)
            }
        }

        // Collect connection status
        viewModelScope.launch {
            webSocketManager.connectionStatus.collectLatest { status ->
                _uiState.value = _uiState.value.copy(connectionStatus = status)
            }
        }

        // Collect raw messages for debug view
        viewModelScope.launch {
            webSocketManager.rawMessages.collectLatest { messages ->
                _uiState.value = _uiState.value.copy(rawMessages = messages)
            }
        }

        // Collect incoming aircraft updates
        viewModelScope.launch {
            webSocketManager.aircraftUpdates.collect { incomingList ->
                mergeAircrafts(incomingList)
            }
        }

        // Purge loop: runs every 1 second to remove planes older than 60s and refresh timestamps
        viewModelScope.launch {
            while (isActive) {
                delay(1000)
                val now = System.currentTimeMillis()
                val cutoff = now - 60_000L

                var removedAny = false
                val iterator = aircraftMap.entries.iterator()
                while (iterator.hasNext()) {
                    val entry = iterator.next()
                    if (entry.value.lastSeenTimestamp < cutoff) {
                        iterator.remove()
                        removedAny = true
                    }
                }

                // If selected aircraft was removed, clear selection
                val currentSelected = _uiState.value.selectedAircraft
                val updatedSelected = if (currentSelected != null && !aircraftMap.containsKey(currentSelected.icao)) {
                    null
                } else {
                    currentSelected?.let { aircraftMap[it.icao] }
                }

                updateUiAircraftList(now, updatedSelected)
            }
        }
    }

    private fun mergeAircrafts(incomingList: List<Aircraft>) {
        val now = System.currentTimeMillis()
        for (newPlane in incomingList) {
            val existing = aircraftMap[newPlane.icao]
            val merged = if (existing != null) {
                newPlane.copy(
                    callsign = newPlane.callsign?.takeIf { it.isNotBlank() } ?: existing.callsign,
                    altitude = newPlane.altitude ?: existing.altitude,
                    speed = newPlane.speed ?: existing.speed,
                    course = if (newPlane.course != 0f) newPlane.course else existing.course,
                    lastSeenTimestamp = newPlane.lastSeenTimestamp
                )
            } else {
                newPlane
            }
            aircraftMap[newPlane.icao] = merged
        }

        val currentSelected = _uiState.value.selectedAircraft
        val updatedSelected = currentSelected?.let { aircraftMap[it.icao] } ?: currentSelected
        updateUiAircraftList(now, updatedSelected)
    }

    private fun updateUiAircraftList(now: Long, updatedSelected: Aircraft?) {
        val rawList = aircraftMap.values.toList()
        val sortedList = sortList(rawList, _uiState.value.sortColumn, _uiState.value.sortDirection, now)
        _uiState.value = _uiState.value.copy(
            aircraftList = sortedList,
            totalPlanesCount = sortedList.size,
            selectedAircraft = updatedSelected,
            currentTime = now
        )
    }

    private fun sortList(
        list: List<Aircraft>,
        column: SortColumn,
        direction: SortDirection,
        now: Long
    ): List<Aircraft> {
        val comparator: Comparator<Aircraft> = when (column) {
            SortColumn.CALLSIGN -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.displayCallsign }
            SortColumn.ICAO -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.icao }
            SortColumn.ALTITUDE -> compareBy { it.altitude ?: -1 }
            SortColumn.SPEED -> compareBy { it.speed ?: -1 }
            SortColumn.COURSE -> compareBy { it.course }
            SortColumn.AGE -> compareBy { it.secondsAgo(now) }
        }
        return if (direction == SortDirection.ASCENDING) {
            list.sortedWith(comparator)
        } else {
            list.sortedWith(comparator.reversed())
        }
    }

    fun onSortHeaderClicked(column: SortColumn) {
        val currentColumn = _uiState.value.sortColumn
        val currentDirection = _uiState.value.sortDirection
        val newDirection = if (currentColumn == column) {
            currentDirection.toggle()
        } else {
            SortDirection.ASCENDING
        }
        _uiState.value = _uiState.value.copy(
            sortColumn = column,
            sortDirection = newDirection
        )
        updateUiAircraftList(_uiState.value.currentTime, _uiState.value.selectedAircraft)
    }

    fun selectAircraft(aircraft: Aircraft?) {
        _uiState.value = _uiState.value.copy(selectedAircraft = aircraft)
    }

    fun centerOnAircraft(aircraft: Aircraft) {
        _uiState.value = _uiState.value.copy(
            selectedAircraft = aircraft,
            panToAircraft = aircraft
        )
    }

    fun centerOnAllPlanes() {
        _uiState.value = _uiState.value.copy(
            fitAllPlanesTrigger = System.currentTimeMillis()
        )
    }

    fun resetPanTarget() {
        _uiState.value = _uiState.value.copy(panToAircraft = null)
    }

    fun openSettings() {
        _uiState.value = _uiState.value.copy(isSettingsOpen = true)
    }

    fun closeSettings() {
        _uiState.value = _uiState.value.copy(isSettingsOpen = false)
    }

    fun saveServerAddress(newAddress: String) {
        viewModelScope.launch {
            settingsRepository.saveServerAddress(newAddress)
            closeSettings()
        }
    }

    fun reconnect() {
        webSocketManager.reconnect()
    }

    fun openDebug() {
        _uiState.value = _uiState.value.copy(isDebugOpen = true)
    }

    fun closeDebug() {
        _uiState.value = _uiState.value.copy(isDebugOpen = false)
    }

    fun clearRawMessages() {
        webSocketManager.clearRawMessages()
    }

    override fun onCleared() {
        super.onCleared()
        webSocketManager.stop()
    }
}
