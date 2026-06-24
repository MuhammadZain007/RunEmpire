package com.example.ui.components

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.LatLngPoint
import com.example.ui.theme.*
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import android.os.Looper
import androidx.compose.material.icons.filled.MyLocation
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import java.io.File

@Composable
fun DigitalMapView(
    points: List<LatLngPoint>,
    territoryPolygons: List<List<LatLngPoint>> = emptyList(),
    modifier: Modifier = Modifier,
    liveLocation: LatLngPoint? = null,
    emptyText: String = "Waiting for GPS signal..."
) {
    val context = LocalContext.current
    
    // Configure User-Agent and Tile Cache for Offline/Robust Map Handling
    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = "RunEmpireSovereignJogger/1.0 (Android; malikzainawan672@gmail.com)"
        val cacheDir = File(context.cacheDir, "osmdroid")
        Configuration.getInstance().osmdroidTileCache = cacheDir
    }

    // Fused Location Provider
    val fusedLocationProviderClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var defaultCenter by remember { mutableStateOf<GeoPoint?>(null) }
    var currentUserLocation by remember { mutableStateOf<LatLngPoint?>(null) }
    var hasCenteredOnLiveLocation by remember { mutableStateOf(false) }
    var triggerRecenter by remember { mutableStateOf(0) }
    var lastTriggeredRecenter by remember { mutableStateOf(0) }

    // Start a continuous location listener to show live location always
    LaunchedEffect(Unit) {
        // Query last location first as an initial fallback
        try {
            fusedLocationProviderClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    val geo = GeoPoint(loc.latitude, loc.longitude)
                    defaultCenter = geo
                    currentUserLocation = LatLngPoint(loc.latitude, loc.longitude)
                }
            }
        } catch (e: SecurityException) {
            // Permission restricted
        }

        // Setup continuous GPS tracking
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 4000)
            .setMinUpdateDistanceMeters(2f)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                super.onLocationResult(result)
                val loc = result.lastLocation
                if (loc != null) {
                    currentUserLocation = LatLngPoint(loc.latitude, loc.longitude)
                }
            }
        }

        try {
            fusedLocationProviderClient.requestLocationUpdates(
                request,
                callback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            // Permission restricted
        }
    }

    var mapScale by remember { mutableStateOf(17.5) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(RunBackground)
    ) {
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    isTilesScaledToDpi = true
                    zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
                    
                    // Apply Sleek Futuristic Slate Night Theme via Color Filter
                    val darkMatrix = ColorMatrix().apply { setSaturation(0.25f) }
                    val invertMatrix = ColorMatrix(floatArrayOf(
                        -0.85f, 0f, 0f, 0f, 230f,
                        0f, -0.85f, 0f, 0f, 230f,
                        0f, 0f, -0.85f, 0f, 230f,
                        0f, 0f, 0f, 1f, 0f
                    ))
                    invertMatrix.postConcat(darkMatrix)
                    val filter = ColorMatrixColorFilter(invertMatrix)
                    overlayManager.tilesOverlay.setColorFilter(filter)

                    // Add dynamic rotation support
                    val rotationOverlay = RotationGestureOverlay(this)
                    rotationOverlay.isEnabled = true
                    overlays.add(rotationOverlay)

                    controller.setZoom(mapScale)
                    
                    // Try to center on some point if available initially
                    val initialCenter = points.firstOrNull()?.let { GeoPoint(it.latitude, it.longitude) }
                        ?: currentUserLocation?.let { GeoPoint(it.latitude, it.longitude) }
                        ?: defaultCenter
                        ?: GeoPoint(37.77490, -122.41940) // Scenic SF center fallback
                    controller.setCenter(initialCenter)
                }
            },
            update = { mapView ->
                // Clear and recreate overlays to avoid duplicates
                mapView.overlays.clear()

                // Re-add rotation overlay
                val rotationOverlay = RotationGestureOverlay(mapView)
                rotationOverlay.isEnabled = true
                mapView.overlays.add(rotationOverlay)

                // 1. Draw territories (Polygons)
                territoryPolygons.forEach { polyPoints ->
                    if (polyPoints.size >= 3) {
                        val polygon = Polygon(mapView).apply {
                            setPoints(polyPoints.map { GeoPoint(it.latitude, it.longitude) })
                            // Emerald Theme colors matching RunEmpire design
                            fillColor = android.graphics.Color.parseColor("#4010B981") // 25% alpha Emerald
                            strokeColor = android.graphics.Color.parseColor("#FF10B981") // Solid Emerald
                            strokeWidth = 5f
                        }
                        mapView.overlays.add(polygon)
                    }
                }

                // 2. Draw active running route (Polyline)
                if (points.size >= 2) {
                    val polyline = Polyline(mapView).apply {
                        setPoints(points.map { GeoPoint(it.latitude, it.longitude) })
                        // Radiant Neon Blue
                        color = android.graphics.Color.parseColor("#FF3B82F6")
                        width = 8f
                    }
                    mapView.overlays.add(polyline)
                }

                // 3. Draw Start Marker pin
                if (points.isNotEmpty()) {
                    val startPt = points.first()
                    val startMarker = Marker(mapView).apply {
                        position = GeoPoint(startPt.latitude, startPt.longitude)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        title = "Start Node"
                        // Subtly colored dot as a start node indicator
                        val drawable = android.graphics.drawable.GradientDrawable().apply {
                            shape = android.graphics.drawable.GradientDrawable.OVAL
                            setSize(24, 24)
                            setColor(android.graphics.Color.WHITE)
                            setStroke(4, android.graphics.Color.parseColor("#FF3B82F6"))
                        }
                        icon = drawable
                    }
                    mapView.overlays.add(startMarker)
                }

                // 4. Draw Current Live runner pin
                val livePinLoc = liveLocation ?: points.lastOrNull() ?: currentUserLocation
                if (livePinLoc != null) {
                    val liveGeo = GeoPoint(livePinLoc.latitude, livePinLoc.longitude)
                    val liveMarker = Marker(mapView).apply {
                        position = liveGeo
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        title = "Active Location"
                        val drawable = android.graphics.drawable.GradientDrawable().apply {
                            shape = android.graphics.drawable.GradientDrawable.OVAL
                            setSize(32, 32)
                            setColor(android.graphics.Color.parseColor("#FF3B82F6")) // Primary neon blue
                            setStroke(3, android.graphics.Color.WHITE)
                        }
                        icon = drawable
                    }
                    mapView.overlays.add(liveMarker)
                    
                    // Auto-focus logic:
                    if (liveLocation != null || points.isNotEmpty()) {
                        // Focus and animate map viewpoint around active runner position during run
                        mapView.controller.animateTo(liveGeo)
                    } else if (!hasCenteredOnLiveLocation) {
                        // Centering on initial acquired GPS position
                        mapView.controller.setCenter(liveGeo)
                        hasCenteredOnLiveLocation = true
                    }
                } else if (points.isNotEmpty()) {
                    val lastGeo = GeoPoint(points.last().latitude, points.last().longitude)
                    mapView.controller.animateTo(lastGeo)
                } else {
                    defaultCenter?.let {
                        if (!hasCenteredOnLiveLocation) {
                            mapView.controller.setCenter(it)
                            hasCenteredOnLiveLocation = true
                        }
                    }
                }

                // Handle manual recenter trigger
                if (triggerRecenter > lastTriggeredRecenter) {
                    lastTriggeredRecenter = triggerRecenter
                    val target = liveLocation ?: points.lastOrNull() ?: currentUserLocation
                    if (target != null) {
                        mapView.controller.animateTo(GeoPoint(target.latitude, target.longitude))
                    } else {
                        defaultCenter?.let { mapView.controller.animateTo(it) }
                    }
                }

                mapView.invalidate()
            },
            modifier = Modifier.fillMaxSize()
        )

        // Floating Map Zoom & Recenter controllers Overlay
        Card(
            modifier = Modifier
                .padding(12.dp)
                .align(Alignment.TopEnd),
            colors = CardDefaults.cardColors(containerColor = RunSurface.copy(alpha = 0.9f)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(4.dp)) {
                IconButton(
                    onClick = {
                        mapScale = (mapScale + 0.5).coerceAtMost(21.0)
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Zoom In", tint = Color.White)
                }
                Divider(color = RunSurfaceVariant, thickness = 1.dp)
                IconButton(
                    onClick = {
                        mapScale = (mapScale - 0.5).coerceAtLeast(4.0)
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(imageVector = Icons.Default.Remove, contentDescription = "Zoom Out", tint = Color.White)
                }
                Divider(color = RunSurfaceVariant, thickness = 1.dp)
                IconButton(
                    onClick = {
                        triggerRecenter += 1
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(imageVector = Icons.Default.MyLocation, contentDescription = "Center on Me", tint = Color.White)
                }
            }
        }

        // Inline HUD guidelines
        Box(
            modifier = Modifier
                .padding(8.dp)
                .align(Alignment.BottomStart)
                .background(Color.Black.copy(alpha = 0.62f), RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "OpenStreetMap Active • Pinch to rotate/scale",
                color = TextSecondary,
                fontSize = 10.sp
            )
        }
    }
}
