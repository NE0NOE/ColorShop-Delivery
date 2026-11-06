package com.rapiflete.delivery.ui.screens

import android.app.TimePickerDialog
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.view.MotionEvent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import com.rapiflete.delivery.model.CourierDriver
import com.rapiflete.delivery.model.DEFAULT_CUSTOMER_MESSAGE_TEMPLATE
import com.rapiflete.delivery.model.DEFAULT_DRIVER_DISPATCH_TEMPLATE
import com.rapiflete.delivery.model.DistanceCalculationMode
import com.rapiflete.delivery.model.FrozenDestination
import com.rapiflete.delivery.model.GeoCoordinate
import com.rapiflete.delivery.model.OrderStatus
import com.rapiflete.delivery.model.PaymentMethodType
import com.rapiflete.delivery.model.QuoteBreakdown
import com.rapiflete.delivery.model.RoundingMode
import com.rapiflete.delivery.model.ShareOutputFormat
import com.rapiflete.delivery.model.SpecialZoneRule
import com.rapiflete.delivery.model.StoreAndPricingConfig
import com.rapiflete.delivery.model.SurchargeType
import com.rapiflete.delivery.model.TransferBankType
import com.rapiflete.delivery.ui.components.ColorShopTopBar
import com.rapiflete.delivery.ui.theme.ColorShopDeepPlum
import com.rapiflete.delivery.ui.theme.ColorShopMagenta
import com.rapiflete.delivery.ui.theme.ColorShopMutedPlum
import com.rapiflete.delivery.ui.theme.ColorShopPinkPill
import com.rapiflete.delivery.ui.theme.ColorShopPrimary
import com.rapiflete.delivery.ui.theme.ColorShopSurface
import com.rapiflete.delivery.ui.theme.ColorShopSurfaceHigh
import com.rapiflete.delivery.ui.theme.ColorShopSurfaceLow
import com.rapiflete.delivery.ui.theme.ColorShopTertiaryBlue
import com.rapiflete.delivery.ui.theme.ColorShopTertiaryFixed
import com.rapiflete.delivery.util.ReceiptImageGenerator
import com.rapiflete.delivery.util.RoutingAndFareCalculator
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    currentConfig: StoreAndPricingConfig,
    couriers: List<CourierDriver>,
    zones: List<SpecialZoneRule>,
    onSaveConfig: (StoreAndPricingConfig) -> Unit,
    onUpdateConfig: (StoreAndPricingConfig) -> Unit = {},
    onOpenCouriers: () -> Unit,
    onAddSpecialZone: (String, Double, Double, Double, Double, Boolean, Double) -> Unit,
    onUpdateSpecialZone: (SpecialZoneRule) -> Unit = {},
    onDeleteSpecialZone: (String) -> Unit,
    onApplyManualSurchargeAmount: (Double) -> Unit = {}
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var storeName by remember(currentConfig) { mutableStateOf(currentConfig.storeName) }
    var storeLatText by remember(currentConfig) { mutableStateOf(currentConfig.storeLat.toString()) }
    var storeLngText by remember(currentConfig) { mutableStateOf(currentConfig.storeLng.toString()) }
    var storePlusCode by remember(currentConfig) { mutableStateOf(currentConfig.storePlusCode) }
    var isStoreDataExpanded by remember { mutableStateOf(false) }

    var currencySymbol by remember(currentConfig) { mutableStateOf(currentConfig.currencySymbol) }
    var minimumFareText by remember(currentConfig) { mutableStateOf(currentConfig.minimumFare.toString()) }
    var includedKmText by remember(currentConfig) { mutableStateOf(currentConfig.includedKmInMinimum.toString()) }
    var costPerExtraKmText by remember(currentConfig) { mutableStateOf(currentConfig.costPerExtraKm.toString()) }

    var weekdayOpen by remember(currentConfig) { mutableStateOf(currentConfig.weekdayOpenText) }
    var weekdayClose by remember(currentConfig) { mutableStateOf(currentConfig.weekdayCloseText) }
    var saturdayEnabled by remember(currentConfig) { mutableStateOf(currentConfig.saturdayEnabled) }
    var satOpen by remember(currentConfig) { mutableStateOf(currentConfig.saturdayOpenText) }
    var satClose by remember(currentConfig) { mutableStateOf(currentConfig.saturdayCloseText) }
    var sundayEnabled by remember(currentConfig) { mutableStateOf(currentConfig.sundayEnabled) }
    var sunOpen by remember(currentConfig) { mutableStateOf(currentConfig.sundayOpenText) }
    var sunClose by remember(currentConfig) { mutableStateOf(currentConfig.sundayCloseText) }

    var autoDetectOffHours by remember(currentConfig) { mutableStateOf(currentConfig.autoDetectOffHours) }

    var offHoursType by remember(currentConfig) { mutableStateOf(currentConfig.offHoursSurchargeType) }
    var offHoursValText by remember(currentConfig) { mutableStateOf(currentConfig.offHoursSurchargeValue.toString()) }

    var holidayType by remember(currentConfig) { mutableStateOf(currentConfig.holidaySurchargeType) }
    var holidayValText by remember(currentConfig) { mutableStateOf(currentConfig.holidaySurchargeValue.toString()) }

    var rainType by remember(currentConfig) { mutableStateOf(currentConfig.rainOrHighDemandSurchargeType) }
    var rainValText by remember(currentConfig) { mutableStateOf(currentConfig.rainOrHighDemandSurchargeValue.toString()) }

    var editingZoneId by remember { mutableStateOf<String?>(null) }
    var newZoneName by remember { mutableStateOf("") }
    var newZonePriceText by remember { mutableStateOf("100.0") }
    var newZoneRadiusText by remember { mutableStateOf("2.5") }
    var newZoneCenterLat by remember { mutableDoubleStateOf(12.1564) }
    var newZoneCenterLng by remember { mutableDoubleStateOf(-86.3489) }
    var hasSelectedZonePinInMap by remember { mutableStateOf(false) }
    var newZoneAppliesExtraKm by remember { mutableStateOf(true) }
    var newZoneExtraKmPriceText by remember(currentConfig) { mutableStateOf(currentConfig.costPerExtraKm.toString()) }
    var showZoneMapPickerDialog by remember { mutableStateOf(false) }

    var roundingMode by remember(currentConfig) { mutableStateOf(currentConfig.roundingMode) }
    var defaultShareFormat by remember(currentConfig) { mutableStateOf(currentConfig.defaultShareFormat) }
    var preferentialAutoPrompt by remember(currentConfig) { mutableStateOf(currentConfig.preferentialAutoPrompt) }
    var preferentialThresholdText by remember(currentConfig) { mutableStateOf(currentConfig.preferentialThreshold.toString()) }
    var preferentialDiscountText by remember(currentConfig) { mutableStateOf(currentConfig.preferentialDiscountPercent.toString()) }

    var isTemplatesExpanded by remember { mutableStateOf(false) }
    var showLivePreview by rememberSaveable { mutableStateOf(false) }
    var previewFormatMode by rememberSaveable { mutableIntStateOf(0) } // 0 = Texto WhatsApp, 1 = Recibo en Imagen
    var selectedTemplateTab by remember { mutableIntStateOf(0) } // 0 = Cliente, 1 = Motorizado
    var customerTemplateValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(currentConfig.customerMessageTemplate))
    }
    var driverTemplateValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(currentConfig.driverDispatchTemplate))
    }

    fun buildCurrentConfig(): StoreAndPricingConfig = currentConfig.copy(
        storeName = storeName.trim().ifBlank { currentConfig.storeName },
        storeLat = storeLatText.toDoubleOrNull() ?: currentConfig.storeLat,
        storeLng = storeLngText.toDoubleOrNull() ?: currentConfig.storeLng,
        storePlusCode = storePlusCode.trim().ifBlank { currentConfig.storePlusCode },
        currencySymbol = currencySymbol,
        minimumFare = minimumFareText.toDoubleOrNull() ?: currentConfig.minimumFare,
        includedKmInMinimum = includedKmText.toDoubleOrNull() ?: currentConfig.includedKmInMinimum,
        costPerExtraKm = costPerExtraKmText.toDoubleOrNull() ?: currentConfig.costPerExtraKm,
        weekdayOpenText = weekdayOpen,
        weekdayCloseText = weekdayClose,
        saturdayEnabled = saturdayEnabled,
        saturdayOpenText = satOpen,
        saturdayCloseText = satClose,
        sundayEnabled = sundayEnabled,
        sundayOpenText = sunOpen,
        sundayCloseText = sunClose,
        autoDetectOffHours = autoDetectOffHours,
        offHoursSurchargeType = offHoursType,
        offHoursSurchargeValue = offHoursValText.toDoubleOrNull() ?: currentConfig.offHoursSurchargeValue,
        holidaySurchargeType = holidayType,
        holidaySurchargeValue = holidayValText.toDoubleOrNull() ?: currentConfig.holidaySurchargeValue,
        rainOrHighDemandSurchargeType = rainType,
        rainOrHighDemandSurchargeValue = rainValText.toDoubleOrNull() ?: currentConfig.rainOrHighDemandSurchargeValue,
        roundingMode = roundingMode,
        defaultShareFormat = defaultShareFormat,
        preferentialAutoPrompt = preferentialAutoPrompt,
        preferentialThreshold = preferentialThresholdText.toDoubleOrNull() ?: currentConfig.preferentialThreshold,
        preferentialDiscountPercent = preferentialDiscountText.toDoubleOrNull() ?: currentConfig.preferentialDiscountPercent,
        customerMessageTemplate = customerTemplateValue.text,
        driverDispatchTemplate = driverTemplateValue.text
    )

    DisposableEffect(Unit) {
        onDispose {
            onSaveConfig(buildCurrentConfig())
        }
    }

    val sampleVariables = remember(
        selectedTemplateTab,
        currencySymbol,
        storeName,
        weekdayOpen,
        weekdayClose,
        satOpen,
        satClose
    ) {
        if (selectedTemplateTab == 0) {
            mapOf(
                "cliente" to "María Pérez",
                "telefono_cliente" to "+505 8888-1234",
                "direccion" to "Colonia Los Robles, Casa #42, de la Vicky 2c al lago",
                "ubicacion_maps" to "https://maps.google.com/?q=12.1364,-86.2513",
                "distancia" to "4.20 km",
                "hora_entrega" to "02:30 PM",
                "comentarios" to "Tocar timbre blanco, portón negro",
                "mercaderia" to "$currencySymbol 350.00",
                "tarifa_envio" to "• Tarifa mínima envío (incluye 3.0 km): $currencySymbol 100.00\n• Tarifa de distancia (1.20 km × $currencySymbol 20.00/km): +$currencySymbol 24.00",
                "total" to "GRAN TOTAL A PAGAR: $currencySymbol 474.00",
                "metodo_pago" to "Efectivo al recibir",
                "banco" to "BAC Credomatic",
                "repartidor" to "Walter Martínez",
                "telefono_repartidor" to "+505 7777-4321",
                "horarios_atencion" to "Lun - Vie: $weekdayOpen - $weekdayClose | Sáb: $satOpen - $satClose",
                "nombre_tienda" to storeName.ifBlank { "ColorShop Nicaragua" },
                "codigo_recibo" to "#20261105011"
            )
        } else {
            mapOf(
                "cliente" to "María Pérez",
                "telefono_cliente" to "+505 8888-1234",
                "hora_entrega" to "02:30 PM",
                "comentarios" to "Tocar timbre blanco, portón negro",
                "direccion" to "Colonia Los Robles, Casa #42, de la Vicky 2c al lago",
                "ubicacion_maps" to "https://maps.google.com/?q=12.1364,-86.2513",
                "distancia" to "4.20 km",
                "mercaderia" to "$currencySymbol 350.00",
                "tarifa_envio" to "$currencySymbol 124.00",
                "total" to "$currencySymbol 474.00 (Efectivo al cliente)",
                "metodo_pago" to "Efectivo",
                "banco" to "BAC Credomatic",
                "liquidacion_motorizado" to "Cobro Efectivo al cliente: $currencySymbol 474.00. Te descuentas tu envío ($currencySymbol 124.00). Depositar/liquidar a tienda por mercadería: $currencySymbol 350.00.",
                "repartidor" to "Walter Martínez",
                "placa_repartidor" to "M-142857",
                "vehiculo_repartidor" to "Moto Bajaj Pulsar 150 Roja",
                "horarios_atencion" to "Lun - Vie: $weekdayOpen - $weekdayClose",
                "nombre_tienda" to storeName.ifBlank { "ColorShop Nicaragua" },
                "codigo_recibo" to "#20261105011"
            )
        }
    }

    val activeTemplateText = if (selectedTemplateTab == 0) customerTemplateValue.text else driverTemplateValue.text
    val livePreviewResolved = remember(activeTemplateText, sampleVariables) {
        RoutingAndFareCalculator.resolveTemplate(activeTemplateText, sampleVariables)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorShopSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
        ColorShopTopBar(showQuickSearchButton = false)

        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Título de pantalla
            Column {
                Text(
                    text = "Configuración y Tarifas",
                    fontSize = 23.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ColorShopDeepPlum
                )
                Text(
                    text = "Ajustes de tienda matriz, cálculo por km y recargos",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = ColorShopMutedPlum
                )
            }

            // SECCIÓN 1: Tarjeta de Gestión de Repartidores (Simplificada)
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(26.dp),
                shadowElevation = 5.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ColorShopSurfaceHigh, RoundedCornerShape(26.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(ColorShopSurfaceLow.copy(alpha = 0.7f), Color.White)
                            )
                        )
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(ColorShopPrimary, ColorShopMagenta)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.DirectionsBike,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Gestión de Repartidores",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ColorShopDeepPlum,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                CategoryBadge(
                                    text = "Flota",
                                    containerColor = ColorShopTertiaryFixed,
                                    contentColor = ColorShopTertiaryBlue
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${couriers.size} motorizados registrados",
                                fontSize = 11.sp,
                                color = ColorShopMutedPlum,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Button(
                        onClick = onOpenCouriers,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ColorShopPrimary),
                        modifier = Modifier.height(42.dp)
                    ) {
                        Text("Administrar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            // SECCIÓN: 📄 Formato de Recibo / Compartir (Texto vs. Imagen Digital)
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(26.dp),
                shadowElevation = 5.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ColorShopSurfaceHigh, RoundedCornerShape(26.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ColorShopSurfaceLow),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📄", fontSize = 18.sp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Formato de Recibo / Compartir",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ColorShopDeepPlum,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                CategoryBadge(
                                    text = "Recibo Digital",
                                    containerColor = ColorShopPinkPill.copy(alpha = 0.5f),
                                    contentColor = ColorShopPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Elige si enviar cotizaciones y comprobantes como texto o como imagen",
                                fontSize = 11.sp,
                                color = ColorShopMutedPlum
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(ColorShopSurfaceLow.copy(alpha = 0.5f))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ShareOutputFormat.entries.forEach { fmt ->
                            val isSelected = defaultShareFormat == fmt
                            Surface(
                                color = if (isSelected) ColorShopPrimary else Color.Transparent,
                                shape = RoundedCornerShape(12.dp),
                                shadowElevation = if (isSelected) 3.dp else 0.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { defaultShareFormat = fmt }
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = if (fmt == ShareOutputFormat.WHATSAPP_TEXT) "💬 Texto WhatsApp" else "🖼️ Imagen Recibo",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else ColorShopDeepPlum,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = if (defaultShareFormat == ShareOutputFormat.IMAGE_RECEIPT) {
                            "🖼️ Generará un recibo gráfico de alta calidad con el logo ColorShop, datos del cliente, estado del pedido y liquidación del motorizado para compartir directamente por WhatsApp."
                        } else {
                            "💬 Enviará un mensaje formateado con texto y emojis listo para copiar o enviar directamente en el chat de WhatsApp."
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = ColorShopMutedPlum
                    )
                }
            }

            // SECCIÓN: 🏪 Datos del Negocio / Tienda Matriz y Origen GPS (Colapsable)
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(26.dp),
                shadowElevation = 5.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ColorShopSurfaceHigh, RoundedCornerShape(26.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { isStoreDataExpanded = !isStoreDataExpanded },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ColorShopSurfaceLow),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🏪", fontSize = 18.sp)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "🏪 Datos del Negocio / Tienda Matriz y Origen GPS",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = ColorShopDeepPlum,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    CategoryBadge(
                                        text = "Matriz",
                                        containerColor = ColorShopSurfaceHigh,
                                        contentColor = ColorShopPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isStoreDataExpanded) {
                                        "Punto base y datos del negocio para cálculo de rutas"
                                    } else {
                                        "Negocio: ${storeName.ifBlank { "ColorShop" }} • Plus Code: ${storePlusCode.ifBlank { "Configurado" }}"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = ColorShopMutedPlum,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        IconButton(
                            onClick = { isStoreDataExpanded = !isStoreDataExpanded },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = if (isStoreDataExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isStoreDataExpanded) "Colapsar datos del negocio" else "Expandir datos del negocio",
                                tint = ColorShopPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    AnimatedVisibility(visible = isStoreDataExpanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            HorizontalDivider(color = ColorShopSurfaceHigh.copy(alpha = 0.6f))

                            OutlinedTextField(
                                value = storeName,
                                onValueChange = { storeName = it },
                                label = { Text("Nombre de Tienda / Negocio Matriz") },
                                placeholder = { Text("Ej. Tienda Matriz ColorShop") },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ColorShopPrimary,
                                    unfocusedBorderColor = ColorShopSurfaceHigh,
                                    focusedContainerColor = ColorShopSurfaceLow.copy(alpha = 0.35f),
                                    unfocusedContainerColor = ColorShopSurfaceLow.copy(alpha = 0.35f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = storeLatText,
                                    onValueChange = { storeLatText = it },
                                    label = { Text("Latitud GPS") },
                                    placeholder = { Text("12.158438") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ColorShopPrimary,
                                        unfocusedBorderColor = ColorShopSurfaceHigh,
                                        focusedContainerColor = ColorShopSurfaceLow.copy(alpha = 0.35f),
                                        unfocusedContainerColor = ColorShopSurfaceLow.copy(alpha = 0.35f)
                                    ),
                                    modifier = Modifier.weight(1f)
                                )

                                OutlinedTextField(
                                    value = storeLngText,
                                    onValueChange = { storeLngText = it },
                                    label = { Text("Longitud GPS") },
                                    placeholder = { Text("-86.305688") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ColorShopPrimary,
                                        unfocusedBorderColor = ColorShopSurfaceHigh,
                                        focusedContainerColor = ColorShopSurfaceLow.copy(alpha = 0.35f),
                                        unfocusedContainerColor = ColorShopSurfaceLow.copy(alpha = 0.35f)
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            OutlinedTextField(
                                value = storePlusCode,
                                onValueChange = { storePlusCode = it },
                                label = { Text("Plus Code Google Maps (Origen)") },
                                placeholder = { Text("5M5V+9P Managua") },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ColorShopPrimary,
                                    unfocusedBorderColor = ColorShopSurfaceHigh,
                                    focusedContainerColor = ColorShopSurfaceLow.copy(alpha = 0.35f),
                                    unfocusedContainerColor = ColorShopSurfaceLow.copy(alpha = 0.35f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Surface(
                                color = ColorShopSurfaceLow.copy(alpha = 0.65f),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "ℹ️ Este punto geográfico se utiliza como origen de partida para el cálculo de kilómetros y tarifas de entrega hacia el cliente.",
                                    fontSize = 11.sp,
                                    color = ColorShopMutedPlum,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }
            }

            // SECCIÓN 2: ⚡ Tarifas y Costo Mínimo (Grid 2x2 de Stitch)
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(26.dp),
                shadowElevation = 5.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ColorShopSurfaceHigh, RoundedCornerShape(26.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ColorShopSurfaceLow),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("⚡", fontSize = 18.sp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Tarifas Base y Costo Mínimo",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ColorShopDeepPlum,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                CategoryBadge(
                                    text = "Tarifas Base",
                                    containerColor = Color(0xFFFEF3C7),
                                    contentColor = Color(0xFFB45309)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Reglas base de cobro por distancia",
                                fontSize = 11.sp,
                                color = ColorShopMutedPlum,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Grid 2x2
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Moneda selector
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "MONEDA",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ColorShopMutedPlum
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(ColorShopSurfaceLow.copy(alpha = 0.45f))
                                    .border(1.dp, ColorShopSurfaceHigh, RoundedCornerShape(16.dp))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf("C$", "$").forEach { sym ->
                                    val sel = currencySymbol == sym
                                    Surface(
                                        color = if (sel) ColorShopPrimary else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                            .clickable { currencySymbol = sym }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = if (sym == "C$") "C$ (NIO)" else "$ (USD)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (sel) Color.White else ColorShopDeepPlum
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Costo Mínimo / Base
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "COSTO MÍNIMO / BASE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ColorShopMutedPlum
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = minimumFareText,
                                onValueChange = { minimumFareText = it },
                                prefix = { Text("$currencySymbol ", fontWeight = FontWeight.Bold, color = ColorShopMutedPlum) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ColorShopPrimary,
                                    unfocusedBorderColor = ColorShopSurfaceHigh,
                                    focusedContainerColor = ColorShopSurfaceLow.copy(alpha = 0.35f),
                                    unfocusedContainerColor = ColorShopSurfaceLow.copy(alpha = 0.35f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Km en Mínimo
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "KM EN MÍNIMO",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ColorShopMutedPlum
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = includedKmText,
                                onValueChange = { includedKmText = it },
                                suffix = { Text("km", fontWeight = FontWeight.Bold, color = ColorShopMutedPlum) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ColorShopPrimary,
                                    unfocusedBorderColor = ColorShopSurfaceHigh,
                                    focusedContainerColor = ColorShopSurfaceLow.copy(alpha = 0.35f),
                                    unfocusedContainerColor = ColorShopSurfaceLow.copy(alpha = 0.35f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Km Adicional
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "KM ADICIONAL",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ColorShopMutedPlum
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = costPerExtraKmText,
                                onValueChange = { costPerExtraKmText = it },
                                prefix = { Text("$currencySymbol ", fontWeight = FontWeight.Bold, color = ColorShopMutedPlum) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ColorShopPrimary,
                                    unfocusedBorderColor = ColorShopSurfaceHigh,
                                    focusedContainerColor = ColorShopSurfaceLow.copy(alpha = 0.35f),
                                    unfocusedContainerColor = ColorShopSurfaceLow.copy(alpha = 0.35f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Surface(
                        color = ColorShopSurfaceLow.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "ℹ️ Si deseas cobrar desde el primer kilómetro además de la tarifa base, coloca 0 en 'Km en Mínimo'.",
                            fontSize = 11.sp,
                            color = ColorShopMutedPlum,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }

            // SECCIÓN: ⭐ Tarifa Preferencial por Distancia (Umbral Máximo y Descuento)
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(26.dp),
                shadowElevation = 5.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ColorShopSurfaceHigh, RoundedCornerShape(26.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ColorShopSurfaceLow),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("⭐", fontSize = 18.sp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Tarifa Preferencial por Distancia",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ColorShopDeepPlum,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                CategoryBadge(
                                    text = "Descuento Inteligente",
                                    containerColor = Color(0xFFE0E7FF),
                                    contentColor = Color(0xFF4338CA)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Aviso emergente sugerido cuando el precio del envío excede lo razonable",
                                fontSize = 11.sp,
                                color = ColorShopMutedPlum,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Switch para activar/desactivar pop-up automático
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(ColorShopSurfaceLow.copy(alpha = 0.45f))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Aviso emergente automático (Pop-up)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorShopDeepPlum
                            )
                            Text(
                                text = "Muestra una alerta con el descuento sugerido al cotizar rutas lejanas",
                                fontSize = 11.sp,
                                color = ColorShopMutedPlum
                            )
                        }
                        Switch(
                            checked = preferentialAutoPrompt,
                            onCheckedChange = { preferentialAutoPrompt = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = ColorShopPrimary
                            )
                        )
                    }

                    // Grid 2 columnas: Umbral y Descuento %
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Umbral
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "UMBRAL MÁXIMO",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ColorShopMutedPlum
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = preferentialThresholdText,
                                onValueChange = { preferentialThresholdText = it },
                                prefix = { Text("$currencySymbol ", fontWeight = FontWeight.Bold, color = ColorShopMutedPlum) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ColorShopPrimary,
                                    unfocusedBorderColor = ColorShopSurfaceHigh,
                                    focusedContainerColor = ColorShopSurfaceLow.copy(alpha = 0.35f),
                                    unfocusedContainerColor = ColorShopSurfaceLow.copy(alpha = 0.35f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Porcentaje de reducción
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "REDUCCIÓN SUGERIDA",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ColorShopMutedPlum
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = preferentialDiscountText,
                                onValueChange = { preferentialDiscountText = it },
                                suffix = { Text("%", fontWeight = FontWeight.Bold, color = ColorShopMutedPlum) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ColorShopPrimary,
                                    unfocusedBorderColor = ColorShopSurfaceHigh,
                                    focusedContainerColor = ColorShopSurfaceLow.copy(alpha = 0.35f),
                                    unfocusedContainerColor = ColorShopSurfaceLow.copy(alpha = 0.35f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Tarjeta explicativa con ejemplo en vivo
                    val previewThresh = preferentialThresholdText.toDoubleOrNull() ?: 150.0
                    val previewDisc = preferentialDiscountText.toDoubleOrNull() ?: 15.0
                    val sample175Result = com.rapiflete.delivery.util.RoutingAndFareCalculator.calculatePreferentialFare(
                        calculatedTotal = 175.0,
                        threshold = previewThresh,
                        discountPercent = previewDisc,
                        roundingMode = roundingMode
                    )
                    val sampleSaving = (175.0 - sample175Result).toInt()

                    Surface(
                        color = ColorShopPrimary.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "💡 Ejemplo con tu configuración actual:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorShopPrimary
                            )
                            Text(
                                text = "Si un envío calcula $currencySymbol 175 (mayor al umbral de $currencySymbol ${previewThresh.toInt()}), el pop-up sugerirá dejarlo en $currencySymbol ${sample175Result.toInt()} (ahorro de $currencySymbol $sampleSaving para el cliente).",
                                fontSize = 11.sp,
                                color = ColorShopDeepPlum
                            )
                        }
                    }
                }
            }

            // SECCIÓN 3: 🕒 Horarios de Atención y Recargos Automáticos
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(26.dp),
                shadowElevation = 5.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ColorShopSurfaceHigh, RoundedCornerShape(26.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ColorShopSurfaceLow),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🕒", fontSize = 18.sp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Horarios de Atención y Recargos",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ColorShopDeepPlum,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                CategoryBadge(
                                    text = "Horarios",
                                    containerColor = Color(0xFFEDE9FE),
                                    contentColor = Color(0xFF6D28D9)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Tarificación por nocturnidad y eventos",
                                fontSize = 11.sp,
                                color = ColorShopMutedPlum,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Bloques de Horario: Lunes a Viernes, Sábado, Domingo
                    ScheduleDayBox(
                        title = "🗓️ Lunes a Viernes",
                        badge = "Lun - Vie",
                        openValue = weekdayOpen,
                        onOpenChange = { weekdayOpen = it },
                        closeValue = weekdayClose,
                        onCloseChange = { weekdayClose = it }
                    )

                    ScheduleDayBox(
                        title = "📅 Sábado",
                        badge = if (saturdayEnabled) "Medio día" else "Cerrado",
                        openValue = satOpen,
                        onOpenChange = { satOpen = it },
                        closeValue = satClose,
                        onCloseChange = { satClose = it },
                        showToggle = true,
                        toggleChecked = saturdayEnabled,
                        onToggleChange = {
                            saturdayEnabled = it
                            onUpdateConfig(buildCurrentConfig().copy(saturdayEnabled = it))
                        }
                    )

                    ScheduleDayBox(
                        title = "✨ Domingo",
                        badge = if (sundayEnabled) "Activo" else "Cerrado / Inactivo",
                        openValue = sunOpen,
                        onOpenChange = { sunOpen = it },
                        closeValue = sunClose,
                        onCloseChange = { sunClose = it },
                        showToggle = true,
                        toggleChecked = sundayEnabled,
                        onToggleChange = {
                            sundayEnabled = it
                            onUpdateConfig(buildCurrentConfig().copy(sundayEnabled = it))
                        }
                    )

                    // Switch de Recargo Nocturno Automático
                    Surface(
                        color = ColorShopSurfaceLow.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, ColorShopSurfaceHigh, RoundedCornerShape(18.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Recargo Nocturno Automático",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorShopDeepPlum
                                )
                                Text(
                                    text = "Aplicar fuera de horario según el reloj del dispositivo",
                                    fontSize = 11.sp,
                                    color = ColorShopMutedPlum
                                )
                            }
                            Switch(
                                checked = autoDetectOffHours,
                                onCheckedChange = { autoDetectOffHours = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = ColorShopPrimary
                                )
                            )
                        }
                    }

                    // Lista de Recargos (Nocturno, Festivo, Lluvia)
                    StitchSurchargeItem(
                        title = "🌙 Recargo Nocturno / Fuera Horario",
                        badgeText = "Activo",
                        currencySymbol = currencySymbol,
                        selectedType = offHoursType,
                        onTypeSelected = { offHoursType = it },
                        valueText = offHoursValText,
                        onValueChanged = { offHoursValText = it }
                    )

                    StitchSurchargeItem(
                        title = "🎉 Día Festivo / Domingo",
                        badgeText = "Activo",
                        currencySymbol = currencySymbol,
                        selectedType = holidayType,
                        onTypeSelected = { holidayType = it },
                        valueText = holidayValText,
                        onValueChanged = { holidayValText = it }
                    )

                    StitchSurchargeItem(
                        title = "🌧️ Lluvia / Alta Demanda",
                        badgeText = "Manual",
                        currencySymbol = currencySymbol,
                        selectedType = rainType,
                        onTypeSelected = { rainType = it },
                        valueText = rainValText,
                        onValueChanged = { rainValText = it }
                    )
                }
            }

            // SECCIÓN 4: 📍 Zonas de Recargo / Aledañas (Municipios y Distantes)
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(26.dp),
                shadowElevation = 5.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ColorShopSurfaceHigh, RoundedCornerShape(26.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cabecera limpia y despejada (sin badges apiñados a la derecha)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ColorShopSurfaceLow),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📍", fontSize = 18.sp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Zonas de Recargo / Aledañas",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ColorShopDeepPlum,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Tarifas base, radio en mapa y km extra fuera de cobertura",
                                fontSize = 11.5.sp,
                                color = ColorShopMutedPlum,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Lista de Zonas
                    if (zones.isEmpty()) {
                        Surface(
                            color = ColorShopSurfaceLow.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "No hay zonas especiales registradas. Añade una con el formulario abajo.",
                                fontSize = 11.sp,
                                color = ColorShopMutedPlum,
                                modifier = Modifier.padding(12.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            zones.forEach { zone ->
                                val isBeingEdited = editingZoneId == zone.id
                                Surface(
                                    color = if (isBeingEdited) ColorShopSurfaceLow.copy(alpha = 0.45f) else Color.White,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            width = if (isBeingEdited) 1.5.dp else 1.dp,
                                            color = if (isBeingEdited) ColorShopPrimary else ColorShopSurfaceHigh,
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .clickable {
                                            editingZoneId = zone.id
                                            newZoneName = zone.name
                                            newZonePriceText = String.format(Locale.US, "%.0f", zone.basePrice)
                                            newZoneRadiusText = String.format(Locale.US, "%.1f", zone.radiusKm)
                                            newZoneCenterLat = zone.centerLat
                                            newZoneCenterLng = zone.centerLng
                                            hasSelectedZonePinInMap = true
                                            newZoneAppliesExtraKm = zone.appliesExtraKm
                                            newZoneExtraKmPriceText = String.format(Locale.US, "%.0f", zone.extraKmPrice)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(30.dp)
                                                    .clip(CircleShape)
                                                    .background(ColorShopSurfaceLow),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(zone.iconEmoji, fontSize = 14.sp)
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = zone.name,
                                                        fontSize = 13.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = ColorShopDeepPlum,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Surface(
                                                        color = ColorShopSurfaceLow,
                                                        shape = RoundedCornerShape(50)
                                                    ) {
                                                        Text(
                                                            text = "Radio ${String.format(Locale.US, "%.1f", zone.radiusKm)} km",
                                                            fontSize = 9.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = ColorShopPrimary,
                                                            maxLines = 1,
                                                            softWrap = false,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                                val extraLabel = if (zone.appliesExtraKm) {
                                                    "+$currencySymbol${zone.extraKmPrice.toInt()}/km fuera del círculo"
                                                } else {
                                                    "Tarifa fija sin km extra"
                                                }
                                                Text(
                                                    text = "Base: $currencySymbol ${zone.basePrice.toInt()} · $extraLabel",
                                                    fontSize = 11.sp,
                                                    color = ColorShopMutedPlum,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = {
                                                    editingZoneId = zone.id
                                                    newZoneName = zone.name
                                                    newZonePriceText = String.format(Locale.US, "%.0f", zone.basePrice)
                                                    newZoneRadiusText = String.format(Locale.US, "%.1f", zone.radiusKm)
                                                    newZoneCenterLat = zone.centerLat
                                                    newZoneCenterLng = zone.centerLng
                                                    hasSelectedZonePinInMap = true
                                                    newZoneAppliesExtraKm = zone.appliesExtraKm
                                                    newZoneExtraKmPriceText = String.format(Locale.US, "%.0f", zone.extraKmPrice)
                                                    showZoneMapPickerDialog = true
                                                },
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Map,
                                                    contentDescription = "Ver o editar círculo en mapa",
                                                    tint = ColorShopPrimary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    if (editingZoneId == zone.id) {
                                                        editingZoneId = null
                                                        newZoneName = ""
                                                        hasSelectedZonePinInMap = false
                                                    }
                                                    onDeleteSpecialZone(zone.id)
                                                    Toast.makeText(context, "Zona eliminada", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteOutline,
                                                    contentDescription = "Eliminar zona",
                                                    tint = ColorShopMutedPlum,
                                                    modifier = Modifier.size(19.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Formulario Añadir / Editar Zona con Esfera / Círculo en Mapa
                    Surface(
                        color = ColorShopSurfaceLow.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, ColorShopSurfaceHigh, RoundedCornerShape(18.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (editingZoneId != null) "EDITAR ZONA SELECCIONADA" else "AÑADIR NUEVA ZONA",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ColorShopDeepPlum
                                )
                                if (editingZoneId != null) {
                                    TextButton(
                                        onClick = {
                                            editingZoneId = null
                                            newZoneName = ""
                                            newZonePriceText = "100"
                                            newZoneRadiusText = "2.5"
                                            hasSelectedZonePinInMap = false
                                            newZoneAppliesExtraKm = true
                                            newZoneExtraKmPriceText = currentConfig.costPerExtraKm.toString()
                                        }
                                    ) {
                                        Text("Cancelar edición", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ColorShopPrimary)
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = newZoneName,
                                onValueChange = { newZoneName = it },
                                label = { Text("Nombre o etiqueta de la zona", fontSize = 11.sp) },
                                placeholder = { Text("Ej. Ciudad Sandino, Tipitapa, Veracruz...", fontSize = 12.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ColorShopPrimary,
                                    unfocusedBorderColor = ColorShopSurfaceHigh,
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Botón interactivo para ubicar el Punto Central y el Radio de Cobertura en el Mapa
                            Surface(
                                color = Color.White,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.5.dp, ColorShopPrimary.copy(alpha = 0.55f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { showZoneMapPickerDialog = true }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(ColorShopSurfaceLow),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Map,
                                                contentDescription = null,
                                                tint = ColorShopPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (hasSelectedZonePinInMap || editingZoneId != null) {
                                                    "Centro y círculo configurados en mapa"
                                                } else {
                                                    "Ubicar centro y círculo de tarifa en mapa"
                                                },
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = ColorShopDeepPlum
                                            )
                                            Text(
                                                text = "Coord: ${String.format(Locale.US, "%.4f, %.4f", newZoneCenterLat, newZoneCenterLng)} · Radio: ${newZoneRadiusText.ifBlank { "2.5" }} km",
                                                fontSize = 10.5.sp,
                                                color = ColorShopMutedPlum,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Surface(
                                        color = ColorShopPrimary,
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text(
                                            text = "Abrir Mapa",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }

                            // Fila: Costo Normal (Precio Base) + Radio de Cobertura (km)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newZonePriceText,
                                    onValueChange = { newZonePriceText = it },
                                    label = { Text("Precio Base Zona", fontSize = 10.5.sp) },
                                    prefix = { Text("$currencySymbol ", fontWeight = FontWeight.Bold) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    textStyle = TextStyle(fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ColorShopPrimary,
                                        unfocusedBorderColor = ColorShopSurfaceHigh,
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    ),
                                    modifier = Modifier.weight(1f)
                                )

                                OutlinedTextField(
                                    value = newZoneRadiusText,
                                    onValueChange = { newZoneRadiusText = it },
                                    label = { Text("Radio Círculo", fontSize = 10.5.sp) },
                                    suffix = { Text("km", fontWeight = FontWeight.Bold, color = ColorShopMutedPlum) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    textStyle = TextStyle(fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ColorShopPrimary,
                                        unfocusedBorderColor = ColorShopSurfaceHigh,
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Fila: Checkbox Aplica Km extra al salir del círculo + Costo por Km extra
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .weight(1.15f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color.White)
                                        .border(1.dp, ColorShopSurfaceHigh, RoundedCornerShape(14.dp))
                                        .clickable { newZoneAppliesExtraKm = !newZoneAppliesExtraKm }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Checkbox(
                                        checked = newZoneAppliesExtraKm,
                                        onCheckedChange = { newZoneAppliesExtraKm = it },
                                        colors = CheckboxDefaults.colors(checkedColor = ColorShopPrimary)
                                    )
                                    Column {
                                        Text(
                                            text = "Km extra fuera",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ColorShopDeepPlum,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "Al salir del radio",
                                            fontSize = 9.5.sp,
                                            color = ColorShopMutedPlum,
                                            maxLines = 1
                                        )
                                    }
                                }

                                if (newZoneAppliesExtraKm) {
                                    OutlinedTextField(
                                        value = newZoneExtraKmPriceText,
                                        onValueChange = { newZoneExtraKmPriceText = it },
                                        label = { Text("Costo Km Extra", fontSize = 10.5.sp) },
                                        prefix = { Text("$currencySymbol ", fontWeight = FontWeight.Bold) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        textStyle = TextStyle(fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = ColorShopPrimary,
                                            unfocusedBorderColor = ColorShopSurfaceHigh,
                                            focusedContainerColor = Color.White,
                                            unfocusedContainerColor = Color.White
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    val cleanName = newZoneName.trim()
                                    if (cleanName.isBlank()) {
                                        Toast.makeText(context, "Escribe el nombre de la zona", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    val parsedBase = newZonePriceText.replace(",", ".").toDoubleOrNull() ?: 100.0
                                    val parsedRadius = (newZoneRadiusText.replace(",", ".").toDoubleOrNull() ?: 2.5).coerceAtLeast(0.2)
                                    val parsedExtraKm = newZoneExtraKmPriceText.replace(",", ".").toDoubleOrNull()
                                        ?: currentConfig.costPerExtraKm

                                    val currentEditId = editingZoneId
                                    if (currentEditId != null) {
                                        val existing = zones.find { it.id == currentEditId }
                                        onUpdateSpecialZone(
                                            SpecialZoneRule(
                                                id = currentEditId,
                                                name = cleanName,
                                                iconEmoji = existing?.iconEmoji ?: "📍",
                                                basePrice = parsedBase,
                                                centerLat = newZoneCenterLat,
                                                centerLng = newZoneCenterLng,
                                                radiusKm = parsedRadius,
                                                appliesExtraKm = newZoneAppliesExtraKm,
                                                extraKmPrice = parsedExtraKm
                                            )
                                        )
                                        editingZoneId = null
                                        Toast.makeText(context, "Zona actualizada y guardada", Toast.LENGTH_SHORT).show()
                                    } else {
                                        onAddSpecialZone(
                                            cleanName,
                                            parsedBase,
                                            newZoneCenterLat,
                                            newZoneCenterLng,
                                            parsedRadius,
                                            newZoneAppliesExtraKm,
                                            parsedExtraKm
                                        )
                                        Toast.makeText(context, "Nueva zona guardada correctamente", Toast.LENGTH_SHORT).show()
                                    }
                                    newZoneName = ""
                                    hasSelectedZonePinInMap = false
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ColorShopPrimary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (editingZoneId != null) "Guardar Cambios de Zona" else "Guardar Zona",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // SECCIÓN 5: 🎯 Redondeo Automático del Precio Final
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(26.dp),
                shadowElevation = 5.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ColorShopSurfaceHigh, RoundedCornerShape(26.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ColorShopSurfaceLow),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🎯", fontSize = 18.sp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Redondeo Automático",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ColorShopDeepPlum,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                CategoryBadge(
                                    text = "Precio Final",
                                    containerColor = Color(0xFFFEF3C7),
                                    contentColor = Color(0xFFB45309)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Regla de ajuste matemático aplicada al calcular cada cotización",
                                fontSize = 11.sp,
                                color = ColorShopMutedPlum,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    RoundingMode.entries.forEach { mode ->
                        val isSel = roundingMode == mode
                        Surface(
                            color = if (isSel) ColorShopSurfaceLow.copy(alpha = 0.5f) else Color.White,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (isSel) 2.dp else 1.dp,
                                    color = if (isSel) ColorShopPrimary else ColorShopSurfaceHigh,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { roundingMode = mode }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = mode.displayName,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSel) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    color = ColorShopDeepPlum
                                )
                                Icon(
                                    imageVector = if (isSel) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (isSel) ColorShopPrimary else ColorShopMutedPlum,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // SECCIÓN 6: 💬 Plantillas de Mensajes (Cliente y Motorizado)
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(26.dp),
                shadowElevation = 5.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ColorShopSurfaceHigh, RoundedCornerShape(26.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isTemplatesExpanded = !isTemplatesExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ColorShopSurfaceLow),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("💬", fontSize = 18.sp)
                            }
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Plantillas de Mensajes",
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = ColorShopDeepPlum,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    CategoryBadge(
                                        text = "WhatsApp",
                                        containerColor = Color(0xFFDCFCE7),
                                        contentColor = Color(0xFF15803D)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Personaliza los textos generados con variables dinámicas",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = ColorShopMutedPlum,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        IconButton(onClick = { isTemplatesExpanded = !isTemplatesExpanded }) {
                            Icon(
                                imageVector = if (isTemplatesExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isTemplatesExpanded) "Colapsar" else "Expandir",
                                tint = ColorShopPrimary
                            )
                        }
                    }

                    AnimatedVisibility(visible = isTemplatesExpanded) {
                        Column(
                            modifier = Modifier.padding(top = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Selector tipo pestañas/chips entre Cliente y Motorizado + Botón interactivo Vista Previa en Vivo
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val tabs = listOf("👤 Cliente", "🛵 Motorizado")
                                    tabs.forEachIndexed { index, label ->
                                        val isSel = selectedTemplateTab == index
                                        Surface(
                                            color = if (isSel) ColorShopPrimary else ColorShopSurfaceLow,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { selectedTemplateTab = index }
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSel) Color.White else ColorShopDeepPlum,
                                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                                textAlign = TextAlign.Center,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Toggle interactivo: 👁️ Vista Previa en Vivo
                                Surface(
                                    color = if (showLivePreview) Color(0xFFDCFCE7) else ColorShopSurfaceLow,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .border(
                                            1.dp,
                                            if (showLivePreview) Color(0xFF16A34A) else ColorShopSurfaceHigh,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { showLivePreview = !showLivePreview }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = if (showLivePreview) "👁️ Ocultar" else "👁️ Ver Previa",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (showLivePreview) Color(0xFF15803D) else ColorShopPrimary,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }

                            // Vista Previa en Vivo con estilo WhatsApp o Imagen Digital
                            AnimatedVisibility(
                                visible = showLivePreview,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    // Selector interactivo de Formato de Previsualización: Texto vs Imagen
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            color = if (previewFormatMode == 0) ColorShopPrimary else ColorShopSurfaceLow,
                                            shape = RoundedCornerShape(50),
                                            border = BorderStroke(1.dp, if (previewFormatMode == 0) ColorShopPrimary else ColorShopSurfaceHigh),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { previewFormatMode = 0 }
                                        ) {
                                            Text(
                                                text = "💬 Texto WhatsApp",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (previewFormatMode == 0) Color.White else ColorShopDeepPlum,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(vertical = 6.dp)
                                            )
                                        }

                                        Surface(
                                            color = if (previewFormatMode == 1) ColorShopPrimary else ColorShopSurfaceLow,
                                            shape = RoundedCornerShape(50),
                                            border = BorderStroke(1.dp, if (previewFormatMode == 1) ColorShopPrimary else ColorShopSurfaceHigh),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { previewFormatMode = 1 }
                                        ) {
                                            Text(
                                                text = "🖼️ Recibo en Imagen",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (previewFormatMode == 1) Color.White else ColorShopDeepPlum,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(vertical = 6.dp)
                                            )
                                        }
                                    }

                                    if (previewFormatMode == 0) {
                                        WhatsAppPreviewBubble(
                                            isCustomer = selectedTemplateTab == 0,
                                            resolvedText = livePreviewResolved
                                        )
                                    } else {
                                        val config = buildCurrentConfig()
                                        val sampleReceiptBitmap = remember(
                                            config,
                                            selectedTemplateTab,
                                            customerTemplateValue.text,
                                            driverTemplateValue.text
                                        ) {
                                            runCatching {
                                                val sampleDest = FrozenDestination(
                                                    coordinate = GeoCoordinate(config.storeLat + 0.025, config.storeLng + 0.025),
                                                    label = "Colonia Los Robles, Managua",
                                                    sourceRawText = "Los Robles",
                                                    capturedAtTime = "02:30 PM",
                                                    isFrozen = true
                                                )
                                                val sampleQuote = QuoteBreakdown(
                                                    effectiveDistanceKm = 4.2,
                                                    straightLineKm = 3.2,
                                                    adjustedStraightLineKm = 4.2,
                                                    osrmDrivingKm = 4.2,
                                                    durationMinutes = 14.0,
                                                    usedMode = DistanceCalculationMode.OSRM_DRIVING,
                                                    baseMinimumFare = config.minimumFare,
                                                    includedKm = config.includedKmInMinimum,
                                                    extraKm = 2.2,
                                                    costPerExtraKm = config.costPerExtraKm,
                                                    extraDistanceCharge = 22.0,
                                                    subtotalBeforeSurcharges = config.minimumFare + 22.0,
                                                    offHoursApplied = false,
                                                    offHoursCharge = 0.0,
                                                    holidayApplied = false,
                                                    holidayCharge = 0.0,
                                                    rainApplied = false,
                                                    rainCharge = 0.0,
                                                    manualExtraAdjustment = 0.0,
                                                    rawTotal = config.minimumFare + 22.0,
                                                    finalRoundedTotal = config.minimumFare + 22.0,
                                                    roundingDifference = 0.0
                                                )
                                                if (selectedTemplateTab == 0) {
                                                    ReceiptImageGenerator.generateCustomerReceiptBitmap(
                                                        config = config,
                                                        destination = sampleDest,
                                                        quote = sampleQuote,
                                                        customerNote = "María Fernanda Pérez",
                                                        customerPhone = "8899-1122",
                                                        orderComments = "Portón negro, llamar al llegar",
                                                        approximateDeliveryTime = "En 30 min (~03:00 PM)",
                                                        courier = null,
                                                        paymentMethod = PaymentMethodType.CASH,
                                                        transferBank = TransferBankType.BAC,
                                                        productAmount = 450.0,
                                                        orderStatus = OrderStatus.EN_RUTA,
                                                        customTemplate = customerTemplateValue.text,
                                                        receiptNumber = "#20261105011"
                                                    )
                                                } else {
                                                    val sampleCourier = CourierDriver(
                                                        id = "sample_c",
                                                        name = "Walter López",
                                                        initials = "W",
                                                        plate = "M 123456",
                                                        phone = "8888-0000"
                                                    )
                                                    ReceiptImageGenerator.generateDriverDispatchBitmap(
                                                        config = config,
                                                        destination = sampleDest,
                                                        quote = sampleQuote,
                                                        customerNote = "María Fernanda Pérez",
                                                        customerPhone = "8899-1122",
                                                        orderComments = "Portón negro, llamar al llegar",
                                                        approximateDeliveryTime = "En 30 min (~03:00 PM)",
                                                        courier = sampleCourier,
                                                        paymentMethod = PaymentMethodType.CASH,
                                                        transferBank = TransferBankType.BAC,
                                                        productAmount = 450.0,
                                                        orderStatus = OrderStatus.EN_RUTA,
                                                        customTemplate = driverTemplateValue.text,
                                                        dispatchNumber = "#20261105011"
                                                    )
                                                }
                                            }.getOrNull()
                                        }

                                        Surface(
                                            color = Color(0xFFF8FAFC),
                                            shape = RoundedCornerShape(16.dp),
                                            border = BorderStroke(1.dp, ColorShopSurfaceHigh),
                                            shadowElevation = 2.dp,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = if (selectedTemplateTab == 0) "🖼️ Vista Previa: Recibo Digital del Cliente" else "📦 Vista Previa: Hoja de Despacho del Motorizado",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = ColorShopDeepPlum
                                                )
                                                if (sampleReceiptBitmap != null) {
                                                    Image(
                                                        bitmap = sampleReceiptBitmap.asImageBitmap(),
                                                        contentDescription = "Previsualización del comprobante en imagen",
                                                        contentScale = ContentScale.FillWidth,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(12.dp))
                                                            .border(1.dp, ColorShopPrimary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                                    )
                                                } else {
                                                    Text("Generando previsualización...", fontSize = 11.sp, color = ColorShopMutedPlum)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Editor de texto (OutlinedTextField) con TextFieldValue para conservar el cursor
                            val activeValue = if (selectedTemplateTab == 0) customerTemplateValue else driverTemplateValue
                            OutlinedTextField(
                                value = activeValue,
                                onValueChange = { newVal ->
                                    if (selectedTemplateTab == 0) {
                                        customerTemplateValue = newVal
                                    } else {
                                        driverTemplateValue = newVal
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ColorShopPrimary,
                                    unfocusedBorderColor = ColorShopSurfaceHigh,
                                    focusedContainerColor = ColorShopSurfaceLow.copy(alpha = 0.25f),
                                    unfocusedContainerColor = ColorShopSurfaceLow.copy(alpha = 0.15f),
                                    focusedTextColor = ColorShopDeepPlum,
                                    unfocusedTextColor = ColorShopDeepPlum
                                ),
                                textStyle = TextStyle(fontSize = 12.sp, lineHeight = 18.sp),
                                placeholder = {
                                    Text("Escribe aquí la plantilla usando {variable}...", fontSize = 12.sp, color = ColorShopMutedPlum)
                                }
                            )

                            // Subtítulo de etiquetas dinámicas
                            Text(
                                text = "ETIQUETAS DINÁMICAS (Toca una etiqueta para insertarla en la posición del cursor):",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ColorShopMutedPlum
                            )

                            // FlowRow con botones modo etiqueta
                            val customerTags = listOf(
                                "{cliente}",
                                "{telefono_cliente}",
                                "{direccion}",
                                "{ubicacion_maps}",
                                "{distancia}",
                                "{hora_entrega}",
                                "{comentarios}",
                                "{mercaderia}",
                                "{tarifa_envio}",
                                "{total}",
                                "{metodo_pago}",
                                "{banco}",
                                "{repartidor}",
                                "{telefono_repartidor}",
                                "{horarios_atencion}",
                                "{nombre_tienda}",
                                "{codigo_recibo}"
                            )

                            val driverTags = listOf(
                                "{cliente}",
                                "{telefono_cliente}",
                                "{hora_entrega}",
                                "{comentarios}",
                                "{direccion}",
                                "{ubicacion_maps}",
                                "{distancia}",
                                "{mercaderia}",
                                "{tarifa_envio}",
                                "{total}",
                                "{metodo_pago}",
                                "{banco}",
                                "{liquidacion_motorizado}",
                                "{repartidor}",
                                "{placa_repartidor}",
                                "{vehiculo_repartidor}",
                                "{codigo_recibo}"
                            )

                            val activeTags = if (selectedTemplateTab == 0) customerTags else driverTags
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                activeTags.forEach { tag ->
                                    Surface(
                                        color = ColorShopSurfaceLow,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .border(1.dp, ColorShopPrimary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                            .clickable {
                                                val curr = if (selectedTemplateTab == 0) customerTemplateValue else driverTemplateValue
                                                val text = curr.text
                                                val sel = curr.selection
                                                val start = sel.min.coerceIn(0, text.length)
                                                val end = sel.max.coerceIn(0, text.length)
                                                val newText = text.replaceRange(start, end, tag)
                                                val nextPos = start + tag.length
                                                val updated = TextFieldValue(
                                                    text = newText,
                                                    selection = TextRange(nextPos)
                                                )
                                                if (selectedTemplateTab == 0) {
                                                    customerTemplateValue = updated
                                                } else {
                                                    driverTemplateValue = updated
                                                }
                                            }
                                    ) {
                                        Text(
                                            text = "+ $tag",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = ColorShopPrimary,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }

                            // Botón secundario "↺ Restaurar por defecto"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Surface(
                                    color = Color.Transparent,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .border(1.dp, ColorShopMutedPlum.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                        .clickable {
                                            if (selectedTemplateTab == 0) {
                                                customerTemplateValue = TextFieldValue(
                                                    text = DEFAULT_CUSTOMER_MESSAGE_TEMPLATE,
                                                    selection = TextRange(DEFAULT_CUSTOMER_MESSAGE_TEMPLATE.length)
                                                )
                                            } else {
                                                driverTemplateValue = TextFieldValue(
                                                    text = DEFAULT_DRIVER_DISPATCH_TEMPLATE,
                                                    selection = TextRange(DEFAULT_DRIVER_DISPATCH_TEMPLATE.length)
                                                )
                                            }
                                            Toast.makeText(context, "Plantilla restaurada a los valores por defecto", Toast.LENGTH_SHORT).show()
                                        }
                                ) {
                                    Text(
                                        text = "↺ Restaurar por defecto",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ColorShopMutedPlum,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Botón Principal Guardar Configuración y Tarifas
            Button(
                onClick = {
                    val updated = buildCurrentConfig()
                    onSaveConfig(updated)
                    Toast.makeText(context, "Configuración guardada correctamente", Toast.LENGTH_SHORT).show()
                },
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ColorShopPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Guardar Configuración y Tarifas",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }

    if (showZoneMapPickerDialog) {
        val currentRadiusKm = (newZoneRadiusText.replace(",", ".").toDoubleOrNull() ?: 2.5).coerceIn(0.2, 25.0)
        ZoneCircleMapPickerDialog(
            zoneName = newZoneName.ifBlank { "Nueva Zona" },
            storeCoord = currentConfig.storeCoordinate,
            initialCenterLat = newZoneCenterLat,
            initialCenterLng = newZoneCenterLng,
            initialRadiusKm = currentRadiusKm,
            onDismiss = { showZoneMapPickerDialog = false },
            onConfirm = { pickedLat, pickedLng, pickedRadiusKm ->
                newZoneCenterLat = pickedLat
                newZoneCenterLng = pickedLng
                newZoneRadiusText = String.format(Locale.US, "%.1f", pickedRadiusKm)
                hasSelectedZonePinInMap = true
                showZoneMapPickerDialog = false
            }
        )
    }
}
}

@Composable
private fun ZoneCircleMapPickerDialog(
    zoneName: String,
    storeCoord: GeoCoordinate,
    initialCenterLat: Double,
    initialCenterLng: Double,
    initialRadiusKm: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double, Double, Double) -> Unit
) {
    val context = LocalContext.current
    var centerLat by remember { mutableDoubleStateOf(initialCenterLat) }
    var centerLng by remember { mutableDoubleStateOf(initialCenterLng) }
    var radiusKm by remember { mutableDoubleStateOf(initialRadiusKm.coerceIn(0.3, 15.0)) }

    val mapView = remember {
        Configuration.getInstance().userAgentValue = context.packageName
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(13.2)
            controller.setCenter(GeoPoint(centerLat, centerLng))
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

    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(28.dp),
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 20.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cabecera del selector de mapa
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(ColorShopSurfaceLow),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🎯", fontSize = 18.sp)
                        }
                        Column {
                            Text(
                                text = "Delimitar Zona en Mapa",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ColorShopDeepPlum
                            )
                            Text(
                                text = "Toca el mapa para fijar el centro de '$zoneName'",
                                fontSize = 11.5.sp,
                                color = ColorShopMutedPlum
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = ColorShopMutedPlum
                        )
                    }
                }

                // Accesos rápidos de municipios de referencia por si desea centrar rápido
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val quickPoints = listOf(
                        "C. Sandino" to GeoPoint(12.1564, -86.3489),
                        "Ticuantepe" to GeoPoint(12.0228, -86.2044),
                        "Tipitapa" to GeoPoint(12.1972, -86.0969),
                        "Managua" to GeoPoint(storeCoord.latitude, storeCoord.longitude)
                    )
                    quickPoints.forEach { (label, pt) ->
                        Surface(
                            color = ColorShopSurfaceLow,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(50))
                                .clickable {
                                    centerLat = pt.latitude
                                    centerLng = pt.longitude
                                    mapView.controller.animateTo(pt)
                                }
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorShopPrimary,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp)
                            )
                        }
                    }
                }

                // Contenedor del Mapa con Círculo (Esfera) en tiempo real
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.5.dp, ColorShopSurfaceHigh, RoundedCornerShape(20.dp))
                ) {
                    AndroidView(
                        factory = { mapView },
                        modifier = Modifier.fillMaxSize(),
                        update = { map ->
                            map.overlays.clear()
                            val receiver = object : MapEventsReceiver {
                                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                                    if (p != null) {
                                        centerLat = p.latitude
                                        centerLng = p.longitude
                                    }
                                    return true
                                }

                                override fun longPressHelper(p: GeoPoint?): Boolean {
                                    if (p != null) {
                                        centerLat = p.latitude
                                        centerLng = p.longitude
                                    }
                                    return true
                                }
                            }
                            map.overlays.add(MapEventsOverlay(receiver))

                            val centerGeo = GeoPoint(centerLat, centerLng)
                            val radiusMeters = (radiusKm * 1000.0).coerceAtLeast(200.0)
                            val circlePts = Polygon.pointsAsCircle(centerGeo, radiusMeters)

                            val circleOverlay = Polygon(map).apply {
                                points = circlePts
                                fillPaint.color = android.graphics.Color.argb(55, 164, 28, 137)
                                outlinePaint.color = android.graphics.Color.parseColor("#A41C89")
                                outlinePaint.strokeWidth = 5f
                                outlinePaint.isAntiAlias = true
                            }
                            map.overlays.add(circleOverlay)

                            val centerMarker = Marker(map).apply {
                                position = centerGeo
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                title = "Centro: $zoneName"
                                icon = createZoneCenterDrawable(context)
                            }
                            map.overlays.add(centerMarker)

                            map.invalidate()
                        }
                    )

                    // Botones de Zoom flotantes en la esquina derecha
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = Color.White.copy(alpha = 0.92f),
                            shape = CircleShape,
                            shadowElevation = 4.dp,
                            modifier = Modifier.size(36.dp)
                        ) {
                            IconButton(onClick = { mapView.controller.zoomIn() }) {
                                Icon(Icons.Default.Add, contentDescription = "Acercar", tint = ColorShopDeepPlum, modifier = Modifier.size(18.dp))
                            }
                        }
                        Surface(
                            color = Color.White.copy(alpha = 0.92f),
                            shape = CircleShape,
                            shadowElevation = 4.dp,
                            modifier = Modifier.size(36.dp)
                        ) {
                            IconButton(onClick = { mapView.controller.zoomOut() }) {
                                Icon(Icons.Default.Remove, contentDescription = "Alejar", tint = ColorShopDeepPlum, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    // Pill inferior con coordenadas y diámetro
                    Surface(
                        color = Color.White.copy(alpha = 0.94f),
                        shape = RoundedCornerShape(50),
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 10.dp)
                    ) {
                        Text(
                            text = "📍 ${String.format(Locale.US, "%.4f, %.4f", centerLat, centerLng)} • Radio: ${String.format(Locale.US, "%.1f", radiusKm)} km (Diámetro ${String.format(Locale.US, "%.1f", radiusKm * 2)} km)",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorShopDeepPlum,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                // Control deslizante de Radio (Esfera de cobertura de la tarifa base)
                Surface(
                    color = ColorShopSurfaceLow.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Radio de cobertura de tarifa base:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorShopDeepPlum
                            )
                            Text(
                                text = "${String.format(Locale.US, "%.1f", radiusKm)} km",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ColorShopPrimary
                            )
                        }
                        Slider(
                            value = radiusKm.toFloat(),
                            onValueChange = { radiusKm = (it * 10).toInt() / 10.0 },
                            valueRange = 0.5f..10.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = ColorShopPrimary,
                                activeTrackColor = ColorShopPrimary,
                                inactiveTrackColor = ColorShopSurfaceHigh
                            )
                        )
                        Text(
                            text = "Dentro de este círculo aplica el Precio Base de la zona. Al salir del círculo se suman los kilómetros adicionales.",
                            fontSize = 10.5.sp,
                            color = ColorShopMutedPlum
                        )
                    }
                }

                // Botones Cancelar y Confirmar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar", fontWeight = FontWeight.Bold, color = ColorShopMutedPlum)
                    }
                    Button(
                        onClick = { onConfirm(centerLat, centerLng, radiusKm) },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = ColorShopPrimary),
                        modifier = Modifier.weight(1.4f)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Confirmar Zona", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    }
                }
            }
        }
    }
}

private fun createZoneCenterDrawable(context: android.content.Context): BitmapDrawable {
    val sizePx = 80
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = sizePx / 2f

    val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(110, 164, 28, 137)
        style = Paint.Style.FILL
    }
    canvas.drawCircle(center, center, (sizePx / 2f) - 2f, haloPaint)

    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#A41C89")
        style = Paint.Style.FILL
    }
    canvas.drawCircle(center, center, 22f, fillPaint)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }
    canvas.drawCircle(center, center, 20f, borderPaint)

    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(center, center, 6f, dotPaint)

    return BitmapDrawable(context.resources, bitmap)
}

@Composable
private fun CategoryBadge(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = ColorShopSurfaceHigh,
    contentColor: Color = ColorShopPrimary
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(50),
        modifier = modifier
    ) {
        Text(
            text = text,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            color = contentColor,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun WhatsAppPreviewBubble(
    isCustomer: Boolean,
    resolvedText: String
) {
    Surface(
        color = Color(0xFFECE5DD), // Fondo realista de chat WhatsApp
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFD1D7DB), RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Cabecera del chat simulado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF25D366)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("💬", fontSize = 13.sp)
                    }
                    Column {
                        Text(
                            text = if (isCustomer) "WhatsApp Cliente (Chat)" else "WhatsApp Motorizado (Despacho)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF075E54)
                        )
                        Text(
                            text = "🟢 Simulación en vivo con datos realistas",
                            fontSize = 10.sp,
                            color = Color(0xFF54656F)
                        )
                    }
                }

                Surface(
                    color = Color(0xFFD9FDD3),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "EN VIVO",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF075E54),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            // Burbuja de mensaje de WhatsApp (Mensaje Enviado)
            Surface(
                color = Color(0xFFE7FFDB), // Verde oficial WhatsApp #E7FFDB
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 4.dp,
                    bottomStart = 16.dp,
                    bottomEnd = 16.dp
                ),
                shadowElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = Color(0xFFBCE7B0),
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 4.dp,
                            bottomStart = 16.dp,
                            bottomEnd = 16.dp
                        )
                    )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = resolvedText.ifBlank { "(La plantilla está vacía)" },
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = Color(0xFF111B21),
                        fontFamily = FontFamily.Default
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "14:32",
                            fontSize = 10.sp,
                            color = Color(0xFF667781)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "✓✓",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF53BDEB) // Doble check azul WhatsApp
                        )
                    }
                }
            }

            Text(
                text = "ℹ️ Al editar la plantilla abajo, esta burbuja se actualiza al instante con los datos de prueba.",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ScheduleDayBox(
    title: String,
    badge: String,
    openValue: String,
    onOpenChange: (String) -> Unit,
    closeValue: String,
    onCloseChange: (String) -> Unit,
    showToggle: Boolean = false,
    toggleChecked: Boolean = true,
    onToggleChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val isDayActive = !showToggle || toggleChecked

    val (openHour, openMin) = remember(openValue) { parseHourMinute(openValue) }
    val (closeHour, closeMin) = remember(closeValue) { parseHourMinute(closeValue) }

    Surface(
        color = if (isDayActive) ColorShopSurfaceLow.copy(alpha = 0.35f) else ColorShopSurfaceLow.copy(alpha = 0.15f),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ColorShopSurfaceHigh.copy(alpha = 0.7f), RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDayActive) ColorShopDeepPlum else ColorShopMutedPlum,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Surface(
                        color = if (isDayActive) ColorShopSurfaceHigh else Color.LightGray.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = badge,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDayActive) ColorShopPrimary else ColorShopMutedPlum,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                if (showToggle) {
                    Switch(
                        checked = toggleChecked,
                        onCheckedChange = onToggleChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = ColorShopPrimary
                        )
                    )
                }
            }

            AnimatedVisibility(visible = isDayActive) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Campo Apertura con TimePickerDialog
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = openValue,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Apertura") },
                            textStyle = TextStyle(fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold),
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = "Seleccionar hora apertura",
                                    tint = ColorShopPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ColorShopPrimary,
                                unfocusedBorderColor = ColorShopSurfaceHigh,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    TimePickerDialog(
                                        context,
                                        { _, h, m ->
                                            onOpenChange(formatTime12(h, m))
                                        },
                                        openHour,
                                        openMin,
                                        false
                                    ).show()
                                }
                        )
                    }

                    // Campo Cierre con TimePickerDialog
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = closeValue,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Cierre") },
                            textStyle = TextStyle(fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold),
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = "Seleccionar hora cierre",
                                    tint = ColorShopPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ColorShopPrimary,
                                unfocusedBorderColor = ColorShopSurfaceHigh,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    TimePickerDialog(
                                        context,
                                        { _, h, m ->
                                            onCloseChange(formatTime12(h, m))
                                        },
                                        closeHour,
                                        closeMin,
                                        false
                                    ).show()
                                }
                        )
                    }
                }
            }
        }
    }
}

private fun parseHourMinute(timeStr: String): Pair<Int, Int> {
    val clean = timeStr.trim().uppercase(Locale.US)
    val isPm = clean.endsWith("PM")
    val isAm = clean.endsWith("AM")
    val digitsOnly = clean.replace("[^0-9:]".toRegex(), "")
    val parts = digitsOnly.split(":")
    if (parts.size >= 2) {
        var h = parts[0].toIntOrNull() ?: 9
        val m = parts[1].toIntOrNull() ?: 0
        if (isPm && h < 12) h += 12
        if (isAm && h == 12) h = 0
        return Pair(h.coerceIn(0, 23), m.coerceIn(0, 59))
    } else if (parts.size == 1 && parts[0].isNotBlank()) {
        var h = parts[0].toIntOrNull() ?: 9
        if (isPm && h < 12) h += 12
        if (isAm && h == 12) h = 0
        return Pair(h.coerceIn(0, 23), 0)
    }
    return Pair(9, 0)
}

private fun formatTime12(hourOfDay: Int, minute: Int): String {
    val h12 = if (hourOfDay % 12 == 0) 12 else hourOfDay % 12
    val amPm = if (hourOfDay >= 12) "PM" else "AM"
    return String.format(Locale.US, "%02d:%02d %s", h12, minute, amPm)
}

@Composable
private fun StitchSurchargeItem(
    title: String,
    badgeText: String,
    currencySymbol: String,
    selectedType: SurchargeType,
    onTypeSelected: (SurchargeType) -> Unit,
    valueText: String,
    onValueChanged: (String) -> Unit
) {
    Surface(
        color = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorShopDeepPlum
                )
                Surface(
                    color = ColorShopSurfaceLow,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ColorShopPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            StitchSurchargeSegmentedRow(
                currencySymbol = currencySymbol,
                selectedType = selectedType,
                onTypeSelected = onTypeSelected,
                valueText = valueText,
                onValueChanged = onValueChanged
            )
        }
    }
}

@Composable
private fun StitchSurchargeSegmentedRow(
    currencySymbol: String,
    selectedType: SurchargeType,
    onTypeSelected: (SurchargeType) -> Unit,
    valueText: String,
    onValueChanged: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = Color(0xFFE2E8F0),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .weight(1.35f)
                .height(42.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                SurchargeType.entries.forEach { t ->
                    val sel = selectedType == t
                    Surface(
                        color = if (sel) Color.White else Color.Transparent,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clickable { onTypeSelected(t) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (t == SurchargeType.PERCENTAGE) "% Porc." else "Fijo",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (sel) ColorShopPrimary else ColorShopMutedPlum,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        OutlinedTextField(
            value = valueText,
            onValueChange = onValueChanged,
            prefix = {
                Text(
                    text = if (selectedType == SurchargeType.FIXED) "$currencySymbol " else "% ",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorShopMutedPlum
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            textStyle = TextStyle(fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ColorShopPrimary,
                unfocusedBorderColor = Color(0xFFCBD5E1),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            modifier = Modifier.weight(0.9f)
        )
    }
}
