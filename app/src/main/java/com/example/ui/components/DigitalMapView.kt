package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LatLngPoint
import com.example.ui.theme.*
import kotlin.math.max
import kotlin.math.min

@Composable
fun DigitalMapView(
    points: List<LatLngPoint>,
    territoryPolygons: List<List<LatLngPoint>> = emptyList(),
    modifier: Modifier = Modifier,
    liveLocation: LatLngPoint? = null,
    emptyText: String = "Waiting for GPS signal..."
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Combine all coordinate points to find the geographic viewport center and span
    val allPoints = remember(points, territoryPolygons, liveLocation) {
        val list = mutableListOf<LatLngPoint>()
        list.addAll(points)
        territoryPolygons.forEach { list.addAll(it) }
        liveLocation?.let { list.add(it) }
        list
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(RunBackground)
            .pointerInput(allPoints) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.5f, 5.0f)
                    offset += pan
                }
            }
    ) {
        if (allPoints.isEmpty()) {
            // Empty Map placeholder state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = RunPrimary,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = emptyText,
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }
        } else {
            // Find bounding box to fit the route on Screen Scale
            val bounds = remember(allPoints) {
                var minLat = Double.MAX_VALUE
                var maxLat = -Double.MAX_VALUE
                var minLng = Double.MAX_VALUE
                var maxLng = -Double.MAX_VALUE

                for (p in allPoints) {
                    minLat = min(minLat, p.latitude)
                    maxLat = max(maxLat, p.latitude)
                    minLng = min(minLng, p.longitude)
                    maxLng = max(maxLng, p.longitude)
                }

                // Add small default padding buffer
                val latBuffer = max((maxLat - minLat) * 0.15, 0.001)
                val lngBuffer = max((maxLng - minLng) * 0.15, 0.001)

                object {
                    val minLatitude = minLat - latBuffer
                    val maxLatitude = maxLat + latBuffer
                    val minLongitude = minLng - lngBuffer
                    val maxLongitude = maxLng + lngBuffer
                    val latSpan = (maxLatitude - minLatitude)
                    val lngSpan = (maxLongitude - minLongitude)
                }
            }

            val gridColor = MapGridColor()
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                // Draw minimal gridlines in background
                val gridSize = 60.dp.toPx()
                val gridStroke = 1.dp.toPx()
                
                // Draw horizontal grids
                var yGrid = 0f
                while (yGrid < canvasHeight) {
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, yGrid),
                        end = Offset(canvasWidth, yGrid),
                        strokeWidth = gridStroke
                    )
                    yGrid += gridSize
                }

                // Draw vertical grids
                var xGrid = 0f
                while (xGrid < canvasWidth) {
                    drawLine(
                        color = gridColor,
                        start = Offset(xGrid, 0f),
                        end = Offset(xGrid, canvasHeight),
                        strokeWidth = gridStroke
                    )
                    xGrid += gridSize
                }

                // Project coordinate to canvas coordinates
                fun project(latLng: LatLngPoint): Offset {
                    // Standard linear equirectangular project:
                    // x is proportional to longitude
                    // y is proportional to latitude (negative because Canvas y goes DOWN)
                    val x = ((latLng.longitude - bounds.minLongitude) / bounds.lngSpan) * canvasWidth
                    val y = (1.0 - ((latLng.latitude - bounds.minLatitude) / bounds.latSpan)) * canvasHeight

                    // Apply dynamic pan offset & touch zoom around canvas center
                    val centerX = canvasWidth / 2f
                    val centerY = canvasHeight / 2f
                    
                    val zoomedX = (x.toFloat() - centerX) * scale + centerX + offset.x
                    val zoomedY = (y.toFloat() - centerY) * scale + centerY + offset.y
                    return Offset(zoomedX, zoomedY)
                }

                // 1. Draw Saved Territories Polygons
                for (poly in territoryPolygons) {
                    if (poly.size >= 3) {
                        val path = Path()
                        val firstProjected = project(poly.first())
                        path.moveTo(firstProjected.x, firstProjected.y)
                        
                        for (i in 1 until poly.size) {
                            val nextProjected = project(poly[i])
                            path.lineTo(nextProjected.x, nextProjected.y)
                        }
                        path.close()

                        // Fill Region (Semi-transparent secondary Emerald)
                        drawPath(
                            path = path,
                            color = RunSecondary.copy(alpha = 0.2f)
                        )

                        // Outline (Dashed Emerald)
                        drawPath(
                            path = path,
                            color = RunSecondary,
                            style = Stroke(
                                width = 3.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                            )
                        )
                    }
                }

                // 2. Draw Active Route Line (Glowing Neon Blue)
                if (points.size >= 2) {
                    val routePath = Path()
                    val startOffset = project(points.first())
                    routePath.moveTo(startOffset.x, startOffset.y)

                    for (i in 1 until points.size) {
                        val pt = project(points[i])
                        routePath.lineTo(pt.x, pt.y)
                    }

                    // Stroke route path
                    drawPath(
                        path = routePath,
                        color = MapPathColor,
                        style = Stroke(
                            width = 5.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }

                // 3. Draw Start Indicator (Vibrant Blue glow circle)
                if (points.isNotEmpty()) {
                    val startOffset = project(points.first())
                    drawCircle(
                        color = RunPrimary,
                        radius = 8.dp.toPx(),
                        center = startOffset
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 3.dp.toPx(),
                        center = startOffset
                    )
                }

                // 4. Draw Current Live Location Runner Pin
                val livePinLoc = liveLocation ?: points.lastOrNull()
                if (livePinLoc != null) {
                    val currentOffset = project(livePinLoc)
                    // Pulse ring
                    drawCircle(
                        color = RunPrimary.copy(alpha = 0.4f),
                        radius = 16.dp.toPx(),
                        center = currentOffset
                    )
                    // Solid marker
                    drawCircle(
                        color = Color.White,
                        radius = 7.dp.toPx(),
                        center = currentOffset
                    )
                    drawCircle(
                        color = RunPrimary,
                        radius = 4.dp.toPx(),
                        center = currentOffset
                    )
                }
            }

            // Inline instructions for Zooming & Panning
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .align(Alignment.BottomStart)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "Zoom: Pinch to scale • Drag to pan",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            // Floating Map Zoom controllers (Material 3 Touch Ergonomics)
            Card(
                modifier = Modifier
                    .padding(12.dp)
                    .align(Alignment.TopEnd),
                colors = CardDefaults.cardColors(containerColor = RunSurface.copy(alpha = 0.9f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(4.dp)) {
                    IconButton(
                        onClick = { scale = (scale + 0.3f).coerceAtMost(5.0f) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Zoom In", tint = Color.White)
                    }
                    Divider(color = RunSurfaceVariant, thickness = 1.dp)
                    IconButton(
                        onClick = { scale = (scale - 0.3f).coerceAtLeast(0.5f) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = "Zoom Out", tint = Color.White)
                    }
                }
            }
        }
    }
}

// Simple color helper for clean UI matching
@Composable
fun MapGridColor(): Color = RunSurfaceVariant.copy(alpha = 0.5f)
