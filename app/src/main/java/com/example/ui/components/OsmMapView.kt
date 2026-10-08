package com.example.ui.components

import android.content.Context
import android.graphics.ColorFilter
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterHdr
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.R
import com.example.model.Aircraft
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.EmeraldGreen
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun OsmMapView(
    aircraftList: List<Aircraft>,
    selectedAircraft: Aircraft?,
    panToAircraft: Aircraft?,
    fitAllPlanesTrigger: Long,
    onAircraftSelected: (Aircraft?) -> Unit,
    onCenterOnAllPlanes: () -> Unit,
    onPanTargetHandled: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isTacticalDarkMap by remember { mutableStateOf(true) }

    // Dark radar color filter matrix for OSM map tiles
    val darkRadarColorFilter = remember {
        val inverseMatrix = ColorMatrix(
            floatArrayOf(
                -0.70f, 0f, 0f, 0f, 210f,
                0f, -0.70f, 0f, 0f, 220f,
                0f, -0.60f, -0.20f, 0f, 240f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        ColorMatrixColorFilter(inverseMatrix)
    }

    val mapView = remember {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid_prefs", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = context.packageName

        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(7.0)
            controller.setCenter(GeoPoint(55.751244, 37.618423)) // Default center
            minZoomLevel = 3.0
            maxZoomLevel = 19.0
        }
    }

    // Toggle dark radar color filter on map tiles
    LaunchedEffect(isTacticalDarkMap) {
        if (isTacticalDarkMap) {
            mapView.overlayManager.tilesOverlay.setColorFilter(darkRadarColorFilter)
        } else {
            mapView.overlayManager.tilesOverlay.setColorFilter(null)
        }
        mapView.invalidate()
    }

    // Update aircraft markers on the map
    LaunchedEffect(aircraftList, selectedAircraft) {
        val normalIcon = ContextCompat.getDrawable(context, R.drawable.ic_plane_marker)
        val selectedIcon = ContextCompat.getDrawable(context, R.drawable.ic_plane_marker_selected)

        // Keep non-marker overlays (like tiles), clear previous plane markers
        val existingMarkers = mapView.overlays.filterIsInstance<Marker>()
        mapView.overlays.removeAll(existingMarkers)

        for (aircraft in aircraftList) {
            val isSelected = aircraft.icao == selectedAircraft?.icao
            val marker = Marker(mapView).apply {
                position = GeoPoint(aircraft.latitude, aircraft.longitude)
                rotation = aircraft.course
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = if (isSelected) selectedIcon else normalIcon
                title = aircraft.displayCallsign
                snippet = "ICAO: ${aircraft.icao}\n" +
                        "Высота: ${aircraft.altitude?.let { "$it ft" } ?: "---"}\n" +
                        "Скорость: ${aircraft.speed?.let { "$it kts" } ?: "---"}\n" +
                        "Курс: ${aircraft.course.toInt()}°"
                id = aircraft.icao

                setOnMarkerClickListener { clickedMarker, _ ->
                    val clickedAircraft = aircraftList.firstOrNull { it.icao == clickedMarker.id }
                    onAircraftSelected(clickedAircraft)
                    true
                }
            }
            mapView.overlays.add(marker)
        }
        mapView.invalidate()
    }

    // Center on specific aircraft when requested
    LaunchedEffect(panToAircraft) {
        panToAircraft?.let { target ->
            val point = GeoPoint(target.latitude, target.longitude)
            mapView.controller.animateTo(point)
            if (mapView.zoomLevelDouble < 8.0) {
                mapView.controller.setZoom(10.0)
            }
            onPanTargetHandled()
        }
    }

    // Fit all planes into view
    LaunchedEffect(fitAllPlanesTrigger) {
        if (fitAllPlanesTrigger > 0L && aircraftList.isNotEmpty()) {
            var minLat = Double.MAX_VALUE
            var maxLat = -Double.MAX_VALUE
            var minLon = Double.MAX_VALUE
            var maxLon = -Double.MAX_VALUE

            for (a in aircraftList) {
                minLat = minOf(minLat, a.latitude)
                maxLat = maxOf(maxLat, a.latitude)
                minLon = minOf(minLon, a.longitude)
                maxLon = maxOf(maxLon, a.longitude)
            }

            // Ensure bounding box has minimal dimension so zoom is reasonable
            val latDiff = maxLat - minLat
            val lonDiff = maxLon - minLon
            if (latDiff < 0.1) {
                maxLat += 0.05
                minLat -= 0.05
            }
            if (lonDiff < 0.1) {
                maxLon += 0.05
                minLon -= 0.05
            }

            val boundingBox = BoundingBox(maxLat, maxLon, minLat, minLon)
            mapView.zoomToBoundingBox(boundingBox, true, 100)
        }
    }

    // Lifecycle handling for osmdroid MapView
    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier
                .fillMaxSize()
                .testTag("osmdroid_map_view")
        )

        // Selected aircraft popup overlay card
        if (selectedAircraft != null) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("selected_aircraft_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = DarkSurface.copy(alpha = 0.94f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AmberAccent.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flight,
                            contentDescription = null,
                            tint = AmberAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = selectedAircraft.displayCallsign,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = AmberAccent
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ICAO: ${selectedAircraft.icao}",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = "Высота: ${selectedAircraft.altitude?.let { "$it ft" } ?: "---"}  •  " +
                                    "Скор: ${selectedAircraft.speed?.let { "$it kts" } ?: "---"}  •  " +
                                    "Курс: ${selectedAircraft.course.toInt()}°",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = { onAircraftSelected(null) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрыть",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Map controls: Center on all planes, Tactical theme toggle, Zoom in/out
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Tactical Dark Map mode toggle
            SmallFloatingActionButton(
                onClick = { isTacticalDarkMap = !isTacticalDarkMap },
                containerColor = DarkSurface.copy(alpha = 0.9f),
                contentColor = if (isTacticalDarkMap) CyanNeon else Color.Gray,
                shape = CircleShape,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("toggle_map_theme_button")
            ) {
                Icon(
                    imageVector = Icons.Default.FilterHdr,
                    contentDescription = "Режим карты",
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Zoom in
            SmallFloatingActionButton(
                onClick = { mapView.controller.zoomIn() },
                containerColor = DarkSurface.copy(alpha = 0.9f),
                contentColor = CyanNeon,
                shape = CircleShape,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("zoom_in_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Приблизить",
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Zoom out
            SmallFloatingActionButton(
                onClick = { mapView.controller.zoomOut() },
                containerColor = DarkSurface.copy(alpha = 0.9f),
                contentColor = CyanNeon,
                shape = CircleShape,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("zoom_out_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Отдалить",
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Center on all aircraft FAB
            FloatingActionButton(
                onClick = onCenterOnAllPlanes,
                containerColor = CyanNeon,
                contentColor = Color(0xFF00363D),
                shape = CircleShape,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("center_on_planes_button")
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Центрировать на самолётах",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
