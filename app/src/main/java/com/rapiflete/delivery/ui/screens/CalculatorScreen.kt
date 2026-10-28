package com.rapiflete.delivery.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.SendToMobile
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rapiflete.delivery.R
import com.rapiflete.delivery.model.CourierDriver
import com.rapiflete.delivery.model.DistanceCalculationMode
import com.rapiflete.delivery.model.PaymentMethodType
import com.rapiflete.delivery.model.SpecialZoneRule
import com.rapiflete.delivery.model.TransferBankType
import com.rapiflete.delivery.ui.components.ColorShopTopBar
import com.rapiflete.delivery.ui.components.DeliveryOsmMapView
import com.rapiflete.delivery.ui.theme.ColorShopDeepPlum
import com.rapiflete.delivery.ui.theme.ColorShopMagenta
import com.rapiflete.delivery.ui.theme.ColorShopMutedPlum
import com.rapiflete.delivery.ui.theme.ColorShopPinkPill
import com.rapiflete.delivery.ui.theme.ColorShopPrimary
import com.rapiflete.delivery.ui.theme.ColorShopSoftButtonBg
import com.rapiflete.delivery.ui.theme.ColorShopSoftButtonText
import com.rapiflete.delivery.ui.theme.ColorShopSurface
import com.rapiflete.delivery.ui.theme.ColorShopSurfaceHigh
import com.rapiflete.delivery.ui.theme.ColorShopSurfaceLow
import com.rapiflete.delivery.util.RoutingAndFareCalculator
import com.rapiflete.delivery.viewmodel.DeliveryUiState
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CalculatorScreen(
    uiState: DeliveryUiState,
    onInputChanged: (String) -> Unit,
    onCustomerNoteChanged: (String) -> Unit,
    onCustomerPhoneChanged: (String) -> Unit,
    onProductAmountChanged: (String) -> Unit = {},
    onOrderCommentsChanged: (String) -> Unit,
    onApproximateDeliveryTimeChanged: (String) -> Unit,
    onReceiptPhotoSelected: (String) -> Unit,
    onFreezeInputLocation: (String) -> Unit,
    onMapTapLocation: (Double, Double) -> Unit,
    onMapLongPressFreeze: (Double, Double) -> Unit,
    onToggleFreezeLock: () -> Unit,
    onSelectCalculationMode: (DistanceCalculationMode) -> Unit,
    onToggleOffHours: (Boolean) -> Unit,
    onToggleHoliday: (Boolean) -> Unit,
    onToggleRain: (Boolean) -> Unit,
    onAdjustManualExtra: (Double) -> Unit,
    onResetManualExtra: () -> Unit,
    onSetCustomTotalOverride: (Double?) -> Unit,
    onSelectZone: (SpecialZoneRule?) -> Unit,
    onSelectCourier: (CourierDriver) -> Unit,
    onSelectPaymentMethod: (PaymentMethodType) -> Unit,
    onSelectTransferBank: (TransferBankType) -> Unit,
    onSaveToHistory: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var showSearchBarOverlay by remember { mutableStateOf(false) }
    var showAjustesBottomSheet by remember { mutableStateOf(false) }
    var showEditPriceDialog by remember { mutableStateOf(false) }
    var showOrderDetailsExpanded by remember { mutableStateOf(true) }
    var zoomTrigger by remember { mutableIntStateOf(0) }
    var resetNorthTrigger by remember { mutableIntStateOf(0) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { pickedUri: Uri? ->
        if (pickedUri != null) {
            val savedPath = copyUriToInternalReceiptFile(context, pickedUri)
            if (savedPath != null) {
                onReceiptPhotoSelected(savedPath)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorShopSurface)
            .verticalScroll(scrollState)
    ) {
        // 1. Barra Superior Oficial de ColorShop Delivery (con botón de lupa para buscar dirección en Managua o pegar link)
        ColorShopTopBar(
            showQuickSearchButton = true,
            onQuickSearchClick = { showSearchBarOverlay = !showSearchBarOverlay }
        )

        // Buscador limpio al tocar la lupa (priorizando el departamento de Managua)
        AnimatedVisibility(visible = showSearchBarOverlay) {
            Surface(
                color = Color.White,
                shadowElevation = 6.dp,
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = ColorShopPrimary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "📍 Managua (Departamento)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ColorShopPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "Búsqueda predefinida en Managua, Nicaragua",
                            fontSize = 11.sp,
                            color = ColorShopMutedPlum
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.inputLinkOrText,
                            onValueChange = onInputChanged,
                            placeholder = {
                                Text(
                                    "Barrio, calle o lugar en Managua...",
                                    fontSize = 13.sp
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(50),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    if (uiState.inputLinkOrText.isNotBlank()) {
                                        onFreezeInputLocation(uiState.inputLinkOrText)
                                        showSearchBarOverlay = false
                                    }
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ColorShopPrimary,
                                unfocusedBorderColor = ColorShopSurfaceHigh,
                                focusedContainerColor = ColorShopSurfaceLow.copy(alpha = 0.35f),
                                unfocusedContainerColor = ColorShopSurfaceLow.copy(alpha = 0.35f)
                            ),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = ColorShopPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                if (uiState.isLoadingRouteOrLink) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = ColorShopPrimary
                                    )
                                } else if (uiState.inputLinkOrText.isNotBlank()) {
                                    IconButton(
                                        onClick = { onInputChanged("") },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Limpiar",
                                            tint = ColorShopMutedPlum,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )

                        Button(
                            onClick = {
                                if (uiState.inputLinkOrText.isNotBlank()) {
                                    onFreezeInputLocation(uiState.inputLinkOrText)
                                    showSearchBarOverlay = false
                                }
                            },
                            enabled = uiState.inputLinkOrText.isNotBlank(),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = ColorShopPrimary)
                        ) {
                            Text("Buscar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 2. Lienzo del Mapa Interactivo (con rotación habilitada a 2 dedos) y Controles Flotantes
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
                .shadow(
                    elevation = 10.dp,
                    shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
                    ambientColor = ColorShopPrimary.copy(alpha = 0.25f)
                )
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
        ) {
            DeliveryOsmMapView(
                storeCoord = uiState.config.storeCoordinate,
                storeName = uiState.config.storeName,
                destination = uiState.frozenDestination,
                routePoints = uiState.routeResult?.routeGeometry ?: emptyList(),
                calculationMode = uiState.selectedCalculationMode,
                zoomDeltaTrigger = zoomTrigger,
                resetNorthTrigger = resetNorthTrigger,
                onMapTap = onMapTapLocation,
                onMapLongPressFreeze = onMapLongPressFreeze,
                modifier = Modifier.fillMaxSize()
            )

            // Controles derechos del mapa (Pill vertical + / - / Norte y Botón "Ajustes" magenta de Stitch)
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
                    .height(290.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                // Pill vertical Zoom + / - y Enderezar Norte
                Surface(
                    color = Color.White.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(50),
                    shadowElevation = 5.dp,
                    modifier = Modifier.width(44.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        IconButton(
                            onClick = { zoomTrigger = if (zoomTrigger <= 0) 1 else zoomTrigger + 1 },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Acercar mapa",
                                tint = ColorShopDeepPlum,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        HorizontalDivider(
                            modifier = Modifier.width(24.dp),
                            color = ColorShopSurfaceHigh
                        )
                        IconButton(
                            onClick = { zoomTrigger = if (zoomTrigger >= 0) -1 else zoomTrigger - 1 },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Alejar mapa",
                                tint = ColorShopDeepPlum,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        HorizontalDivider(
                            modifier = Modifier.width(24.dp),
                            color = ColorShopSurfaceHigh
                        )
                        IconButton(
                            onClick = { resetNorthTrigger++ },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CompassCalibration,
                                contentDescription = "Alinear mapa al Norte",
                                tint = ColorShopPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Botón flotante "Ajustes" (Opciones y recargos de tarifa)
                Surface(
                    color = ColorShopPrimary,
                    shape = RoundedCornerShape(50),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .height(44.dp)
                        .clickable { showAjustesBottomSheet = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Ajustes y recargos",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Ajustes",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 3. Tarjeta Flotante Inferior de Cotización, Datos del Cliente/Respaldo y Despacho
        val quote = uiState.quoteBreakdown
        val dest = uiState.frozenDestination
        val cfg = uiState.config
        val cur = cfg.currencySymbol

        Surface(
            color = Color.White,
            shape = RoundedCornerShape(28.dp),
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .offset(y = (-24).dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Indicador superior tipo handle
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(ColorShopSurfaceHigh)
                        .align(Alignment.CenterHorizontally)
                )

                if (quote != null && dest != null) {
                    val displayedTotal = quote.effectiveTotal
                    val totalFormatted = String.format(Locale.US, "%s %.2f", cur, displayedTotal)
                    val kmFormatted = String.format(Locale.US, "%.1f km", quote.effectiveDistanceKm)
                    val minsFormatted = "~${(quote.durationMinutes?.roundToInt() ?: 14).coerceAtLeast(1)} min"

                    val extraChargesSum = quote.extraDistanceCharge +
                        quote.offHoursCharge +
                        quote.holidayCharge +
                        quote.rainCharge +
                        quote.manualExtraAdjustment

                    val breakdownSubtext = when {
                        quote.customTotalOverride != null -> {
                            "Estimado: $cur ${quote.finalRoundedTotal.toInt()} (Editado)"
                        }
                        quote.selectedZoneName != null -> {
                            "Zona ${quote.selectedZoneName}: $cur${quote.baseMinimumFare.toInt()}"
                        }
                        else -> {
                            "Mín. $cur${quote.baseMinimumFare.toInt()} + $cur${extraChargesSum.roundToInt()} dist."
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Columna Izquierda: TARIFA CALCULADA C$ 62.00 neto (Tocable para editar monto acordado)
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { showEditPriceDialog = true }
                                .padding(end = 6.dp, top = 2.dp, bottom = 2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = if (quote.customTotalOverride != null) "TARIFA ACORDADA" else "TARIFA CALCULADA",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF893B92),
                                    letterSpacing = 0.8.sp
                                )
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Editar monto del envío",
                                    tint = ColorShopPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = totalFormatted,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ColorShopPrimary,
                                    letterSpacing = (-0.5).sp
                                )
                                Text(
                                    text = "neto",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF893B92),
                                    modifier = Modifier.padding(bottom = 5.dp)
                                )
                            }
                        }

                        // Columna Derecha: Pill 4.2 km • ~14 min + Mín. C$40 + C$22 dist.
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                color = ColorShopPinkPill,
                                shape = RoundedCornerShape(50)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NearMe,
                                        contentDescription = null,
                                        tint = ColorShopPrimary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = kmFormatted,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF40000D)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(ColorShopPrimary)
                                    )
                                    Text(
                                        text = minsFormatted,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF40000D)
                                    )
                                }
                            }

                            Text(
                                text = breakdownSubtext,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF893B92)
                            )
                        }
                    }

                    // Chips activos si hay algún recargo o zona seleccionada
                    if (uiState.applyOffHours || uiState.applyHoliday || uiState.applyRainOrDemand || uiState.selectedZone != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (uiState.applyOffHours) {
                                ActiveMiniBadge("🌙 Nocturno")
                            }
                            if (uiState.applyHoliday) {
                                ActiveMiniBadge("🎉 Festivo")
                            }
                            if (uiState.applyRainOrDemand) {
                                ActiveMiniBadge("🌧️ Lluvia")
                            }
                            uiState.selectedZone?.let { z ->
                                ActiveMiniBadge("📍 ${z.name}")
                            }
                        }
                    }

                    // Desglose de Productos y Envío por separado + Gran Total
                    if (uiState.productAmount > 0.0) {
                        Surface(
                            color = ColorShopSurfaceLow.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("🛍️ Mercadería / Productos:", fontSize = 11.sp, color = ColorShopMutedPlum)
                                    Text(
                                        "$cur ${String.format(Locale.US, "%.2f", uiState.productAmount)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ColorShopDeepPlum
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("🛵 Tarifa de Envío:", fontSize = 11.sp, color = ColorShopMutedPlum)
                                    Text(
                                        "$cur ${String.format(Locale.US, "%.2f", quote.effectiveTotal)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ColorShopPrimary
                                    )
                                }
                                HorizontalDivider(color = ColorShopSurfaceHigh)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("💰 GRAN TOTAL A PAGAR:", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = ColorShopDeepPlum)
                                    Text(
                                        "$cur ${String.format(Locale.US, "%.2f", uiState.productAmount + quote.effectiveTotal)}",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = ColorShopPrimary
                                    )
                                }
                            }
                        }
                    }

                    // Resumen de liquidación para el conductor según método de pago
                    val settlementPreview = RoutingAndFareCalculator.buildDriverSettlementNote(
                        paymentMethod = uiState.selectedPaymentMethod,
                        transferBank = uiState.selectedTransferBank,
                        productAmount = uiState.productAmount,
                        deliveryFee = quote.effectiveTotal,
                        currencySymbol = cur
                    )
                    Surface(
                        color = ColorShopSurfaceLow.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, ColorShopPrimary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    ) {
                        Text(
                            text = settlementPreview,
                            fontSize = 10.sp,
                            color = ColorShopDeepPlum,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                        )
                    }
                } else {
                    // Estado inicial limpio (sin cliente de destino predefinido)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "TARIFA CALCULADA",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF893B92),
                                letterSpacing = 0.8.sp
                            )
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "$cur 0.00",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ColorShopMutedPlum.copy(alpha = 0.45f),
                                    letterSpacing = (-0.5).sp
                                )
                                Text(
                                    text = "neto",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ColorShopMutedPlum.copy(alpha = 0.45f),
                                    modifier = Modifier.padding(bottom = 5.dp)
                                )
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                color = ColorShopPinkPill,
                                shape = RoundedCornerShape(50)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NearMe,
                                        contentDescription = null,
                                        tint = ColorShopPrimary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "Sin destino",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF40000D)
                                    )
                                }
                            }

                            Text(
                                text = "Comparte ubicación o toca el mapa",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF893B92)
                            )
                        }
                    }
                }

                // SECCIÓN DESPLEGABLE / DIRECTA: Datos del Cliente, Hora Aprox, Comentarios y Foto de Respaldo
                Surface(
                    color = ColorShopSurfaceLow.copy(alpha = 0.45f),
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showOrderDetailsExpanded = !showOrderDetailsExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = ColorShopPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Datos del Cliente, Entrega y Foto",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorShopDeepPlum
                                )
                            }
                            Text(
                                text = if (showOrderDetailsExpanded) "Ocultar ▲" else "Completar ▼",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorShopPrimary
                            )
                        }

                        AnimatedVisibility(visible = showOrderDetailsExpanded) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                // Nombre del cliente o persona a recibir
                                OutlinedTextField(
                                    value = uiState.customerNote,
                                    onValueChange = onCustomerNoteChanged,
                                    label = { Text("Cliente / Persona que recibe", fontSize = 11.sp) },
                                    placeholder = { Text("Ej. María Fernanda Pérez", fontSize = 12.sp) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = ColorShopPrimary, modifier = Modifier.size(17.dp))
                                    },
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

                                // Fila: Número del cliente + Hora aproximada de entrega
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = uiState.customerPhone,
                                        onValueChange = onCustomerPhoneChanged,
                                        label = { Text("Teléfono cliente", fontSize = 11.sp) },
                                        placeholder = { Text("8899-1122", fontSize = 12.sp) },
                                        leadingIcon = {
                                            Icon(Icons.Default.Phone, contentDescription = null, tint = ColorShopPrimary, modifier = Modifier.size(16.dp))
                                        },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                        singleLine = true,
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
                                        value = uiState.approximateDeliveryTime,
                                        onValueChange = onApproximateDeliveryTimeChanged,
                                        label = { Text("Hora aprox.", fontSize = 11.sp) },
                                        placeholder = { Text("Ej. 3:30 PM", fontSize = 12.sp) },
                                        leadingIcon = {
                                            Icon(Icons.Default.AccessTime, contentDescription = null, tint = ColorShopPrimary, modifier = Modifier.size(16.dp))
                                        },
                                        singleLine = true,
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

                                // Total de Productos / Mercadería por separado
                                OutlinedTextField(
                                    value = uiState.productAmountInput,
                                    onValueChange = onProductAmountChanged,
                                    label = { Text("Total Productos / Mercadería ($cur)", fontSize = 11.sp) },
                                    placeholder = { Text("Ej. 450", fontSize = 12.sp) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Inventory2, contentDescription = null, tint = ColorShopPrimary, modifier = Modifier.size(17.dp))
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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

                                // Comentarios del pedido
                                OutlinedTextField(
                                    value = uiState.orderComments,
                                    onValueChange = onOrderCommentsChanged,
                                    label = { Text("Comentarios del pedido / referencias", fontSize = 11.sp) },
                                    placeholder = { Text("Ej. Portón negro, llamar al llegar, frágil...", fontSize = 12.sp) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Notes, contentDescription = null, tint = ColorShopPrimary, modifier = Modifier.size(17.dp))
                                    },
                                    maxLines = 2,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ColorShopPrimary,
                                        unfocusedBorderColor = ColorShopSurfaceHigh,
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Botón para añadir foto de recibo / respaldo
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { photoPickerLauncher.launch("image/*") },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.White,
                                            contentColor = ColorShopPrimary
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .border(1.dp, ColorShopPrimary.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AddAPhoto,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (uiState.receiptPhotoUri.isBlank()) "Añadir Foto / Recibo" else "Cambiar Foto Adjunta",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    if (uiState.receiptPhotoUri.isNotBlank()) {
                                        val bmp = remember(uiState.receiptPhotoUri) {
                                            runCatching { BitmapFactory.decodeFile(uiState.receiptPhotoUri) }.getOrNull()
                                        }
                                        if (bmp != null) {
                                            Image(
                                                bitmap = bmp.asImageBitmap(),
                                                contentDescription = "Foto recibo",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .border(1.dp, ColorShopPrimary, RoundedCornerShape(10.dp))
                                            )
                                        }
                                        IconButton(
                                            onClick = { onReceiptPhotoSelected("") },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Quitar foto",
                                                tint = ColorShopPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Selector Rápido de Motorizado sobre los botones de acción
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(18.dp),
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ColorShopSurfaceHigh, RoundedCornerShape(18.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.DirectionsBike,
                                    contentDescription = null,
                                    tint = ColorShopPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Motorizado asignado:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorShopDeepPlum
                                )
                            }
                            Text(
                                text = uiState.selectedCourier?.name ?: "Sin asignar",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ColorShopPrimary
                            )
                        }

                        // Lista horizontal de chips con los repartidores registrados
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.couriers.forEach { courier ->
                                val isSelected = uiState.selectedCourier?.id == courier.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onSelectCourier(courier) },
                                    label = { Text("🛵 ${courier.name}") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ColorShopPrimary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }

                // Botones Gemelos de Acción (Cliente | Delivery)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val hasDestination = quote != null && dest != null
                    // Botón 1: Cliente (#FFEBF3 bg, #8C1D74 text con icono de Mariposa)
                    Button(
                        onClick = {
                            if (quote != null && dest != null) {
                                val summary = RoutingAndFareCalculator.buildCustomerWhatsAppSummary(
                                    config = cfg,
                                    destination = dest,
                                    quote = quote,
                                    customerNote = uiState.customerNote,
                                    customerPhone = uiState.customerPhone,
                                    orderComments = uiState.orderComments,
                                    approximateDeliveryTime = uiState.approximateDeliveryTime,
                                    courier = uiState.selectedCourier,
                                    paymentMethod = uiState.selectedPaymentMethod,
                                    transferBank = uiState.selectedTransferBank,
                                    productAmount = uiState.productAmount
                                )
                                copyToClipboard(context, "Cotización ColorShop", summary)
                                shareTextToWhatsAppOrChooser(context, summary)
                            }
                        },
                        enabled = hasDestination,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ColorShopSoftButtonBg,
                            contentColor = ColorShopSoftButtonText
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .border(
                                width = 1.dp,
                                color = ColorShopPrimary.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(50)
                            )
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_butterfly),
                            contentDescription = "Cliente",
                            modifier = Modifier.size(20.dp),
                            tint = Color.Unspecified
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Cliente",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    // Botón 2: Delivery (#A51D89 bg, White text)
                    Button(
                        onClick = {
                            if (quote != null && dest != null) {
                                onSaveToHistory()
                                Toast.makeText(context, "✅ Pedido guardado en historial (En Ruta)", Toast.LENGTH_SHORT).show()
                                val dispatchText = RoutingAndFareCalculator.buildDriverWhatsAppDispatch(
                                    config = cfg,
                                    destination = dest,
                                    quote = quote,
                                    customerNote = uiState.customerNote,
                                    customerPhone = uiState.customerPhone,
                                    orderComments = uiState.orderComments,
                                    approximateDeliveryTime = uiState.approximateDeliveryTime,
                                    productAmount = uiState.productAmount,
                                    paymentMethod = uiState.selectedPaymentMethod,
                                    transferBank = uiState.selectedTransferBank
                                )
                                copyToClipboard(context, "Despacho ColorShop", dispatchText)
                                shareTextToWhatsAppOrChooser(context, dispatchText)
                            }
                        },
                        enabled = hasDestination,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ColorShopPrimary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.DirectionsBike,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Delivery",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }

    // Diálogo para editar manualmente el monto del envío si llegan a otro acuerdo
    if (showEditPriceDialog && uiState.quoteBreakdown != null) {
        val currentQuote = uiState.quoteBreakdown
        var editedAmountText by remember(currentQuote) {
            mutableStateOf(
                String.format(
                    Locale.US,
                    "%.0f",
                    currentQuote.effectiveTotal
                )
            )
        }

        AlertDialog(
            onDismissRequest = { showEditPriceDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "Editar Precio del Envío",
                    fontWeight = FontWeight.ExtraBold,
                    color = ColorShopDeepPlum,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Estimado por distancia: ${uiState.config.currencySymbol} ${String.format(Locale.US, "%.2f", currentQuote.finalRoundedTotal)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ColorShopMutedPlum
                    )
                    OutlinedTextField(
                        value = editedAmountText,
                        onValueChange = { editedAmountText = it },
                        label = { Text("Monto acordado (${uiState.config.currencySymbol})") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ColorShopPrimary,
                            unfocusedBorderColor = ColorShopSurfaceHigh
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = editedAmountText.replace(",", ".").trim().toDoubleOrNull()
                        if (parsed != null && parsed >= 0.0) {
                            onSetCustomTotalOverride(parsed)
                        }
                        showEditPriceDialog = false
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = ColorShopPrimary)
                ) {
                    Text("Aplicar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (currentQuote.customTotalOverride != null) {
                        TextButton(
                            onClick = {
                                onSetCustomTotalOverride(null)
                                showEditPriceDialog = false
                            }
                        ) {
                            Text("Restaurar estimado", color = ColorShopMutedPlum)
                        }
                    }
                    TextButton(onClick = { showEditPriceDialog = false }) {
                        Text("Cancelar", color = ColorShopPrimary)
                    }
                }
            }
        )
    }

    // Bottom Sheet de "Ajustes" (Opciones rápidas de recargos, zonas, repartidor, método de pago y bancos)
    if (showAjustesBottomSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showAjustesBottomSheet = false },
            sheetState = sheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Ajustes Rápidos del Envío",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = ColorShopDeepPlum
                )

                // Recargos rápidos
                Text(
                    text = "Recargos aplicables a este pedido:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorShopMutedPlum
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = uiState.applyOffHours,
                        onClick = { onToggleOffHours(!uiState.applyOffHours) },
                        label = { Text("🌙 Nocturno / Fuera Hora (+${uiState.config.currencySymbol}${uiState.config.offHoursSurchargeValue.toInt()})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ColorShopPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = uiState.applyHoliday,
                        onClick = { onToggleHoliday(!uiState.applyHoliday) },
                        label = { Text("🎉 Festivo / Dom (+${uiState.config.currencySymbol}${uiState.config.holidaySurchargeValue.toInt()})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ColorShopPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = uiState.applyRainOrDemand,
                        onClick = { onToggleRain(!uiState.applyRainOrDemand) },
                        label = { Text("🌧️ Lluvia / Demanda (+${uiState.config.currencySymbol}${uiState.config.rainOrHighDemandSurchargeValue.toInt()})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ColorShopPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }

                // Zonas Aledañas (Ciudad Sandino, Ticuantepe, etc.)
                if (uiState.zones.isNotEmpty()) {
                    Text(
                        text = "Zona Aledaña / Municipio:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorShopMutedPlum
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = uiState.selectedZone == null,
                            onClick = { onSelectZone(null) },
                            label = { Text("Managua Estándar") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ColorShopPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                        uiState.zones.forEach { zone ->
                            FilterChip(
                                selected = uiState.selectedZone?.id == zone.id,
                                onClick = {
                                    onSelectZone(if (uiState.selectedZone?.id == zone.id) null else zone)
                                },
                                label = { Text("${zone.iconEmoji} ${zone.name} (${uiState.config.currencySymbol}${zone.basePrice.toInt()})") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ColorShopPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                // Selección de Repartidor y Método de Pago
                Text(
                    text = "Asignar Repartidor y Forma de Cobro:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorShopMutedPlum
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    uiState.couriers.forEach { courier ->
                        FilterChip(
                            selected = uiState.selectedCourier?.id == courier.id,
                            onClick = { onSelectCourier(courier) },
                            label = { Text("🛵 ${courier.name}") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ColorShopPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PaymentMethodType.entries.forEach { method ->
                        FilterChip(
                            selected = uiState.selectedPaymentMethod == method,
                            onClick = { onSelectPaymentMethod(method) },
                            label = { Text(method.badgeLabel) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ColorShopMagenta,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                // Selector de Banco si es Transferencia Bancaria (BAC, Lafise, Banpro, Ficohsa)
                if (uiState.selectedPaymentMethod == PaymentMethodType.TRANSFER) {
                    Text(
                        text = "Banco de Transferencia:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorShopMutedPlum
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TransferBankType.entries.forEach { bank ->
                            FilterChip(
                                selected = uiState.selectedTransferBank == bank,
                                onClick = { onSelectTransferBank(bank) },
                                label = { Text("🏦 ${bank.displayName}") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ColorShopPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                // Modo de cálculo (OSRM vs Línea Recta)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DistanceCalculationMode.entries.forEach { mode ->
                        FilterChip(
                            selected = uiState.selectedCalculationMode == mode,
                            onClick = { onSelectCalculationMode(mode) },
                            label = { Text(mode.shortLabel) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ColorShopPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Button(
                    onClick = { showAjustesBottomSheet = false },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = ColorShopPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Listo", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ActiveMiniBadge(text: String) {
    Surface(
        color = ColorShopSurfaceLow,
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = ColorShopPrimary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

private fun copyUriToInternalReceiptFile(context: Context, uri: Uri): String? {
    return try {
        val dir = File(context.filesDir, "receipt_photos").apply { mkdirs() }
        val targetFile = File(dir, "recibo_${UUID.randomUUID()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(targetFile).use { output ->
                input.copyTo(output)
            }
        }
        targetFile.absolutePath
    } catch (_: Exception) {
        null
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}

private fun shareTextToWhatsAppOrChooser(context: Context, text: String) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        // Permite que la app externa (WhatsApp) abra en su propia tarea separada
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
    }
    val chooser = Intent.createChooser(sendIntent, "Compartir con ColorShop Delivery").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooser)
}
