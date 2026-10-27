package com.rapiflete.delivery.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.rapiflete.delivery.model.DistanceCalculationMode
import com.rapiflete.delivery.model.FrozenDestination
import com.rapiflete.delivery.model.GeoCoordinate
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay

@Composable
fun DeliveryOsmMapView(
    storeCoord: GeoCoordinate,
    storeName: String,
    destination: FrozenDestination?,
    routePoints: List<GeoCoordinate>,
    calculationMode: DistanceCalculationMode,
    zoomDeltaTrigger: Int = 0,
    resetNorthTrigger: Int = 0,
    onMapTap: (Double, Double) -> Unit,
    onMapLongPressFreeze: (Double, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val rotationOverlay = remember {
        mutableListOf<RotationGestureOverlay>()
    }

    val mapView = remember {
        Configuration.getInstance().userAgentValue = context.packageName
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            // Ocultamos los botones nativos porque usamos el pill vertical de zoom del diseño Stitch
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(14.5)
            controller.setCenter(GeoPoint(storeCoord.latitude, storeCoord.longitude))

            // Habilitar rotación del mapa con dos dedos
            val rot = RotationGestureOverlay(this).apply {
                isEnabled = true
            }
            rotationOverlay.clear()
            rotationOverlay.add(rot)
            overlays.add(rot)

            setOnTouchListener { v, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_MOVE -> {
                        v.parent?.requestDisallowInterceptTouchEvent(true)
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        v.parent?.requestDisallowInterceptTouchEvent(false)
                    }
                }
                false
            }
        }
    }

    LaunchedEffect(zoomDeltaTrigger) {
        if (zoomDeltaTrigger > 0) {
            mapView.controller.zoomIn()
        } else if (zoomDeltaTrigger < 0) {
            mapView.controller.zoomOut()
        }
    }

    LaunchedEffect(resetNorthTrigger) {
        if (resetNorthTrigger > 0) {
            mapView.mapOrientation = 0f
            mapView.invalidate()
        }
    }

    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { map ->
            map.overlays.clear()
            rotationOverlay.firstOrNull()?.let { map.overlays.add(it) }

            val receiver = object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                    if (p != null) {
                        onMapTap(p.latitude, p.longitude)
                    }
                    return true
                }

                override fun longPressHelper(p: GeoPoint?): Boolean {
                    if (p != null) {
                        onMapLongPressFreeze(p.latitude, p.longitude)
                    }
                    return true
                }
            }
            map.overlays.add(MapEventsOverlay(receiver))

            val storeGeoPoint = GeoPoint(storeCoord.latitude, storeCoord.longitude)

            // Polilínea doble con efecto halo rosa/magenta estilo ColorShop Stitch
            if (destination != null) {
                val destGeoPoint = GeoPoint(destination.coordinate.latitude, destination.coordinate.longitude)
                val pointsToDraw = if (calculationMode == DistanceCalculationMode.OSRM_DRIVING && routePoints.size > 1) {
                    routePoints.map { GeoPoint(it.latitude, it.longitude) }
                } else {
                    listOf(storeGeoPoint, destGeoPoint)
                }

                // Halo exterior rosa suave (#FFC1D5)
                val glowLine = Polyline(map).apply {
                    setPoints(pointsToDraw)
                    outlinePaint.color = Color.parseColor("#FFC1D5")
                    outlinePaint.strokeWidth = 20f
                    outlinePaint.strokeCap = Paint.Cap.ROUND
                    outlinePaint.isAntiAlias = true
                }
                map.overlays.add(glowLine)

                // Línea principal magenta/carmín (#B90538 / #A41C89)
                val mainLine = Polyline(map).apply {
                    setPoints(pointsToDraw)
                    outlinePaint.color = Color.parseColor("#B90538")
                    outlinePaint.strokeWidth = 10f
                    outlinePaint.strokeCap = Paint.Cap.ROUND
                    outlinePaint.isAntiAlias = true
                }
                map.overlays.add(mainLine)
            }

            // Marcador Tienda Matriz (blanco con borde y centro magenta)
            val storeMarker = Marker(map).apply {
                position = storeGeoPoint
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                title = "🏪 Tienda Matriz"
                snippet = "ColorShop (${storeCoord.format(4)})"
                icon = createColorShopPinDrawable(
                    context = context,
                    haloColor = Color.argb(120, 255, 193, 213),
                    bgColor = Color.WHITE,
                    borderColor = Color.parseColor("#A41C89"),
                    textColor = Color.parseColor("#A41C89"),
                    innerText = "T"
                )
            }
            map.overlays.add(storeMarker)

            // Marcador Cliente (Destino)
            if (destination != null) {
                val destGeoPoint = GeoPoint(destination.coordinate.latitude, destination.coordinate.longitude)
                val destMarker = Marker(map).apply {
                    position = destGeoPoint
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    title = "📍 Cliente (Destino)"
                    snippet = "Congelado ${destination.capturedAtTime} • ${destination.coordinate.format(5)}"
                    icon = createColorShopPinDrawable(
                        context = context,
                        haloColor = Color.argb(135, 255, 193, 213),
                        bgColor = Color.parseColor("#A41C89"),
                        borderColor = Color.WHITE,
                        textColor = Color.WHITE,
                        innerText = "📍"
                    )
                }
                map.overlays.add(destMarker)

                val lastDestTag = map.tag as? String
                val currentDestTag = "${storeCoord.format(5)}_${destination.coordinate.format(5)}"
                if (lastDestTag != currentDestTag) {
                    map.tag = currentDestTag
                    map.post {
                        try {
                            val minLat = minOf(storeGeoPoint.latitude, destGeoPoint.latitude) - 0.005
                            val maxLat = maxOf(storeGeoPoint.latitude, destGeoPoint.latitude) + 0.005
                            val minLon = minOf(storeGeoPoint.longitude, destGeoPoint.longitude) - 0.005
                            val maxLon = maxOf(storeGeoPoint.longitude, destGeoPoint.longitude) + 0.005
                            val box = BoundingBox(maxLat, maxLon, minLat, minLon)
                            map.zoomToBoundingBox(box, true, 96)
                        } catch (_: Exception) {
                            map.controller.animateTo(destGeoPoint)
                        }
                    }
                }
            }

            map.invalidate()
        }
    )
}

private fun createColorShopPinDrawable(
    context: android.content.Context,
    haloColor: Int,
    bgColor: Int,
    borderColor: Int,
    textColor: Int,
    innerText: String
): BitmapDrawable {
    val sizePx = 92
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = sizePx / 2f

    // Halo exterior suave
    val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = haloColor
        style = Paint.Style.FILL
    }
    canvas.drawCircle(center, center, (sizePx / 2f) - 2f, haloPaint)

    val innerRadius = (sizePx / 2f) - 14f

    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawCircle(center, center, innerRadius, fillPaint)

    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = borderColor
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }
    canvas.drawCircle(center, center, innerRadius - 2.5f, strokePaint)

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor
        textSize = 28f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    val textY = center - ((textPaint.descent() + textPaint.ascent()) / 2f)
    canvas.drawText(innerText, center, textY, textPaint)

    return BitmapDrawable(context.resources, bitmap)
}
