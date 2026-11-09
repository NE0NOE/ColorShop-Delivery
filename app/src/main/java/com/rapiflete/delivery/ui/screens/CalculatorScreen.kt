package com.rapiflete.delivery.ui.screens

import android.app.TimePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.SendToMobile
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rapiflete.delivery.R
import com.rapiflete.delivery.model.CourierDriver
import com.rapiflete.delivery.model.DistanceCalculationMode
import com.rapiflete.delivery.model.GeoCoordinate
import com.rapiflete.delivery.model.OrderStatus
import com.rapiflete.delivery.model.PaymentMethodType
import com.rapiflete.delivery.model.ShareOutputFormat
import com.rapiflete.delivery.model.SpecialZoneRule
import com.rapiflete.delivery.model.TransferBankType
import com.rapiflete.delivery.model.calculateMixedPaymentSettlement
import com.rapiflete.delivery.ui.components.ColorShopButterflyLogo
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
import com.rapiflete.delivery.util.ReceiptImageGenerator
import com.rapiflete.delivery.util.RoutingAndFareCalculator
import com.rapiflete.delivery.viewmodel.DeliveryUiState
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
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
    onPartialTransferAmountChanged: (String) -> Unit = {},
    onPartialCashAmountChanged: (String) -> Unit = {},
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
    onSetPaymentPendingOnDelivery: (Boolean) -> Unit = {},
    onSaveToHistory: () -> Unit,
    onClearQuoteAndInputs: () -> Unit = {},
    onDismissErrorMessage: () -> Unit = {},
    onOpenSettings: () -> Unit,
    onApplyPreferentialRate: (Double?) -> Unit = {},
    onDismissPreferentialDialog: () -> Unit = {},
    onOpenPreferentialDialog: () -> Unit = {},
    onConfirmSuggestedZone: () -> Unit = {},
    onDismissZoneConfirmDialog: () -> Unit = {},
    onOpenZoneConfirmDialog: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var showAjustesBottomSheet by remember { mutableStateOf(false) }
    var showEditPriceDialog by remember { mutableStateOf(false) }
    var showDuplicateConfirmDialog by remember { mutableStateOf(false) }
    var zoomTrigger by remember { mutableIntStateOf(0) }
    var resetNorthTrigger by remember { mutableIntStateOf(0) }
    var isTopSearchActive by rememberSaveable { mutableStateOf(false) }
    var isSheetExpanded by rememberSaveable { mutableStateOf(false) }

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

    val cfg = uiState.config
    val quote = uiState.quoteBreakdown
    val dest = uiState.frozenDestination
    val cur = cfg.currencySymbol

    val grandTotal = if (quote != null) {
        if (uiState.productAmount > 0.0) {
            uiState.productAmount + quote.effectiveTotal
        } else {
            quote.effectiveTotal
        }
    } else {
        0.0
    }
    val grandTotalFormatted = String.format(Locale.US, "%s %.2f", cur, grandTotal)

    val executeDispatch: (Boolean) -> Unit = { saveToHistory ->
        if (quote != null && dest != null) {
            val receiptCode = "#${uiState.currentReceiptCode}"
            if (saveToHistory) {
                onSaveToHistory()
                Toast.makeText(context, "✅ Pedido guardado en historial (En Ruta)", Toast.LENGTH_SHORT).show()
            }
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
                transferBank = uiState.selectedTransferBank,
                courier = uiState.selectedCourier,
                isPaymentPendingOnDelivery = uiState.isPaymentPendingOnDelivery,
                receiptCode = receiptCode,
                partialTransferAmount = uiState.partialTransferAmount
            )
            copyToClipboard(context, "Despacho ColorShop", dispatchText)
            if (cfg.defaultShareFormat == ShareOutputFormat.IMAGE_RECEIPT) {
                val bitmap = ReceiptImageGenerator.generateDriverDispatchBitmap(
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
                    productAmount = uiState.productAmount,
                    orderStatus = OrderStatus.EN_RUTA,
                    isPaymentPendingOnDelivery = uiState.isPaymentPendingOnDelivery,
                    customTemplate = cfg.driverDispatchTemplate,
                    dispatchNumber = receiptCode,
                    partialTransferAmount = uiState.partialTransferAmount
                )
                val imageUri = ReceiptImageGenerator.saveBitmapToShareCache(
                    context,
                    bitmap,
                    "despacho_delivery"
                )
                ReceiptImageGenerator.shareReceiptImage(context, imageUri, dispatchText)
            } else {
                shareTextToWhatsAppOrChooser(context, dispatchText)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorShopSurface)
    ) {
        // 1. Lienzo del Mapa Interactivo (DeliveryOsmMapView) ocupando la pantalla completa
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 64.dp)
        ) {
            DeliveryOsmMapView(
                storeCoord = uiState.config.storeCoordinate,
                storeName = uiState.config.storeName,
                destination = uiState.frozenDestination,
                routePoints = uiState.routeResult?.routeGeometry ?: emptyList(),
                calculationMode = uiState.selectedCalculationMode,
                activeZone = uiState.selectedZone ?: uiState.suggestedZone,
                zoomDeltaTrigger = zoomTrigger,
                resetNorthTrigger = resetNorthTrigger,
                onMapTap = onMapTapLocation,
                onMapLongPressFreeze = onMapLongPressFreeze,
                modifier = Modifier.fillMaxSize()
            )

            // Pill flotante de bloqueo/desbloqueo de pin sobre el mapa
            if (uiState.frozenDestination != null) {
                val isFrozen = uiState.frozenDestination.isFrozen
                Surface(
                    color = Color.White.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(50),
                    shadowElevation = 5.dp,
                    border = BorderStroke(
                        1.dp,
                        if (isFrozen) ColorShopPrimary else Color(0xFFFF9800)
                    ),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 14.dp, top = 14.dp)
                        .defaultMinSize(minHeight = 40.dp)
                        .clip(RoundedCornerShape(50))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleFreezeLock()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (isFrozen) "🔒 Destino Congelado" else "🔓 Desbloqueado",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isFrozen) ColorShopPrimary else Color(0xFFE65100)
                        )
                    }
                }
            }

            // Controles flotantes derechos del mapa (Pill zoom/norte y Botón "Ajustes")
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 14.dp, top = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.End
            ) {
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

                    Surface(
                        color = ColorShopPrimary,
                        shape = RoundedCornerShape(50),
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .height(44.dp)
                            .clickable { showAjustesBottomSheet = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Ajustes y recargos",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
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

        // 2. Barra Superior Unificada con Búsqueda In-Place y Banner de Error
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .zIndex(2f)
        ) {
        // 1. Barra Superior Unificada con Búsqueda In-Place (Logo ColorShop ↔ Buscador)
        Surface(
            color = Color.White,
            shadowElevation = 3.dp,
            shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            AnimatedContent(
                targetState = isTopSearchActive,
                transitionSpec = {
                    fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(150))
                },
                label = "TopBarSearchTransition"
            ) { searching ->
                if (!searching) {
                    // Modo Normal: Logo mariposa + "ColorShop Delivery" a la izquierda, botón 🔍 a la derecha
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ColorShopButterflyLogo(size = 32.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(
                                        SpanStyle(
                                            color = ColorShopDeepPlum,
                                            fontWeight = FontWeight.Bold
                                        )
                                    ) {
                                        append("ColorShop ")
                                    }
                                    withStyle(
                                        SpanStyle(
                                            color = ColorShopPrimary,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    ) {
                                        append("Delivery")
                                    }
                                },
                                fontSize = 19.sp
                            )
                        }

                        // Botón de búsqueda a la derecha que activa el buscador in-place
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                isTopSearchActive = true
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(ColorShopSurfaceLow)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Buscar o pegar dirección",
                                tint = ColorShopPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                } else {
                    // Modo Búsqueda Activo: Reemplaza las letras por el buscador en el mismo espacio
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Botón cerrar/volver
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                isTopSearchActive = false
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar buscador",
                                tint = ColorShopDeepPlum,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Campo de texto de búsqueda
                        OutlinedTextField(
                            value = uiState.inputLinkOrText,
                            onValueChange = onInputChanged,
                            placeholder = {
                                Text(
                                    "Barrio, calle, Plus o enlace...",
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(50),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    if (uiState.inputLinkOrText.isNotBlank()) {
                                        onFreezeInputLocation(uiState.inputLinkOrText)
                                        isTopSearchActive = false
                                    }
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ColorShopPrimary,
                                unfocusedBorderColor = ColorShopSurfaceHigh,
                                focusedContainerColor = ColorShopSurfaceLow.copy(alpha = 0.5f),
                                unfocusedContainerColor = ColorShopSurfaceLow.copy(alpha = 0.5f)
                            ),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = ColorShopPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            trailingIcon = {
                                if (uiState.isLoadingRouteOrLink) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = ColorShopPrimary
                                    )
                                } else if (uiState.inputLinkOrText.isNotBlank()) {
                                    IconButton(
                                        onClick = { onInputChanged("") },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Limpiar texto",
                                            tint = ColorShopMutedPlum,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )

                        // Botón Buscar
                        Button(
                            onClick = {
                                if (uiState.inputLinkOrText.isNotBlank()) {
                                    onFreezeInputLocation(uiState.inputLinkOrText)
                                    isTopSearchActive = false
                                }
                            },
                            enabled = uiState.inputLinkOrText.isNotBlank(),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = ColorShopPrimary),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Text("Buscar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Botón Pegar
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                val clipData = clipboard?.primaryClip
                                val clipText = if (clipData != null && clipData.itemCount > 0) {
                                    clipData.getItemAt(0)?.text?.toString()?.trim()
                                } else null

                                if (!clipText.isNullOrBlank()) {
                                    onInputChanged(clipText)
                                    onFreezeInputLocation(clipText)
                                    isTopSearchActive = false
                                    Toast.makeText(context, "📋 Ubicación pegada del portapapeles", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "El portapapeles no tiene texto o enlace copiado", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ColorShopPinkPill,
                                contentColor = ColorShopPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier
                                .height(40.dp)
                                .border(1.dp, ColorShopPrimary.copy(alpha = 0.3f), RoundedCornerShape(50))
                        ) {
                            Text("📋 Pegar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Banner de error claro y visible cuando uiState.errorMessage != null
        if (uiState.errorMessage != null) {
            Surface(
                color = Color(0xFFFFEBEE),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFEF9A9A)),
                shadowElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("⚠️", fontSize = 18.sp)
                        Text(
                            text = uiState.errorMessage ?: "",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFC62828),
                            lineHeight = 16.sp
                        )
                    }
                    IconButton(
                        onClick = onDismissErrorMessage,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar advertencia",
                            tint = Color(0xFFC62828),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
        }

        // 3. Panel Unificado de Cotización y Dock de Acciones (Bottom Sheet Integrado)
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            shadowElevation = 16.dp,
            border = BorderStroke(1.dp, ColorShopSurfaceHigh.copy(alpha = 0.8f)),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .animateContentSize()
                .imePadding()
                .zIndex(3f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Pestañita desplegable tipo Handle
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            isSheetExpanded = !isSheetExpanded
                        }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(44.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(ColorShopPrimary.copy(alpha = 0.35f))
                    )
                    Text(
                        text = if (isSheetExpanded) "▼ Ocultar opciones de cotización" else "▲ Ver opciones de cotización y cliente",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorShopPrimary
                    )
                }

                // 2. Contenido desplegable (Cotización, Desglose, Cliente, Cobro, Formato)
                if (isSheetExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 460.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                // Fila de encabezado: Título "Cotización" y Botón "Nueva Cotización"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cotización",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ColorShopDeepPlum
                    )

                    // Botón 'Limpiar / Nueva Cotización'
                    Surface(
                        color = ColorShopSurfaceLow,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, ColorShopPrimary.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 40.dp)
                            .clip(RoundedCornerShape(50))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onClearQuoteAndInputs()
                                Toast.makeText(context, "✨ Cotización restablecida para nuevo despacho", Toast.LENGTH_SHORT).show()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = ColorShopPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Nueva Cotización",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorShopPrimary
                            )
                        }
                    }
                }

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
                            if (quote.extraDistanceCharge > 0.01) {
                                "Zona ${quote.selectedZoneName}: $cur${quote.baseMinimumFare.toInt()} + $cur${quote.extraDistanceCharge.roundToInt()} km extra"
                            } else {
                                "Zona ${quote.selectedZoneName}: $cur${quote.baseMinimumFare.toInt()} (en radio)"
                            }
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

                    // Banner de sugerencia de Zona Aledaña si se detectó una y aún no ha sido activada
                    if (uiState.suggestedZone != null && uiState.selectedZone == null) {
                        val sZone = uiState.suggestedZone
                        val distFromZoneCenter = RoutingAndFareCalculator.calculateHaversineKm(
                            GeoCoordinate(sZone.centerLat, sZone.centerLng),
                            dest.coordinate
                        )
                        val extraOutRadius = kotlin.math.max(0.0, distFromZoneCenter - sZone.radiusKm)
                        val extraZoneCost = if (sZone.appliesExtraKm) extraOutRadius * sZone.extraKmPrice else 0.0
                        val previewZoneTotal = RoutingAndFareCalculator.applyRounding(
                            sZone.basePrice + extraZoneCost,
                            uiState.config.roundingMode
                        )

                        Surface(
                            color = ColorShopSurfaceLow.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, ColorShopPrimary.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onOpenZoneConfirmDialog() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("📍", fontSize = 16.sp)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Zona detectada: ${sZone.name}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = ColorShopDeepPlum
                                    )
                                    val subInfo = if (extraOutRadius > 0.05 && sZone.appliesExtraKm) {
                                        "Base $cur${sZone.basePrice.toInt()} + ${String.format(Locale.US, "%.1f", extraOutRadius)} km fuera del círculo → $cur${previewZoneTotal.toInt()}"
                                    } else {
                                        "Dentro del círculo (${String.format(Locale.US, "%.1f", sZone.radiusKm)} km) → Tarifa: $cur${previewZoneTotal.toInt()}"
                                    }
                                    Text(
                                        text = subInfo,
                                        fontSize = 11.sp,
                                        color = ColorShopMutedPlum
                                    )
                                }
                                Surface(
                                    color = ColorShopPrimary,
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.clickable { onConfirmSuggestedZone() }
                                ) {
                                    Text(
                                        text = "Aplicar",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Aviso y acceso directo a Tarifa Preferencial si supera el umbral
                    if (quote.finalRoundedTotal > uiState.config.preferentialThreshold) {
                        val isApplied = quote.customTotalOverride != null && quote.customTotalOverride < quote.finalRoundedTotal
                        if (isApplied) {
                            Surface(
                                color = ColorShopPrimary.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable { onOpenPreferentialDialog() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("⭐", fontSize = 12.sp)
                                    Text(
                                        text = "Tarifa Preferencial Activa: $cur ${quote.effectiveTotal.toInt()}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ColorShopPrimary
                                    )
                                    val diff = (quote.finalRoundedTotal - quote.effectiveTotal).toInt()
                                    if (diff > 0) {
                                        Text(
                                            text = "(-$cur $diff ahorro)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2E7D32)
                                        )
                                    }
                                }
                            }
                        } else {
                            Surface(
                                color = Color(0xFFFFF3E0),
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB74D)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onOpenPreferentialDialog() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("⭐", fontSize = 16.sp)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Tarifa alta detectada (> $cur ${uiState.config.preferentialThreshold.toInt()})",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color(0xFFE65100)
                                        )
                                        Text(
                                            text = "Toca para aplicar tarifa preferencial sugerida de $cur ${uiState.suggestedPreferentialRate.toInt()}.",
                                            fontSize = 11.sp,
                                            color = Color(0xFF6D4C41)
                                        )
                                    }
                                    Surface(
                                        color = ColorShopPrimary,
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text(
                                            text = "Aplicar",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
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
                                val autoLabel = if (uiState.isZoneAutoDetected) "📍 Zona auto: ${z.name} ($cur${z.basePrice.toInt()})" else "📍 ${z.name} ($cur${z.basePrice.toInt()})"
                                ActiveMiniBadge(autoLabel)
                            }
                        }
                    }

                    // Desglose de Productos y Envío por separado + Gran Total
                    if (uiState.productAmount > 0.0 || uiState.selectedPaymentMethod == PaymentMethodType.MIXED) {
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
                                if (uiState.selectedPaymentMethod == PaymentMethodType.MIXED) {
                                    val mixed = calculateMixedPaymentSettlement(
                                        uiState.productAmount,
                                        quote.effectiveTotal,
                                        uiState.partialTransferAmount
                                    )
                                    HorizontalDivider(color = ColorShopSurfaceHigh.copy(alpha = 0.6f))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "🏦 Abono por Transferencia (${uiState.selectedTransferBank.displayName}):",
                                            fontSize = 11.sp,
                                            color = ColorShopMutedPlum
                                        )
                                        Text(
                                            "- $cur ${String.format(Locale.US, "%.2f", mixed.transferAmount)}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2E7D32)
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "💵 Efectivo a Cobrar en Entrega:",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ColorShopDeepPlum
                                        )
                                        Text(
                                            "$cur ${String.format(Locale.US, "%.2f", mixed.cashToCollect)}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = ColorShopPrimary
                                        )
                                    }
                                }
                            }
                        }
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

                // 👤 Tarjeta Principal: Datos del Cliente y Pedido (Acordeón y Menú Rápido)
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.5.dp, ColorShopPrimary.copy(alpha = 0.25f)),
                    shadowElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Título y distintivo visual
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
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(ColorShopPrimary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = ColorShopPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Datos del Cliente y Pedido",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = ColorShopDeepPlum
                                    )
                                    Text(
                                        text = if (uiState.customerNote.isNotBlank()) "Cliente: " + uiState.customerNote else "Información para el comprobante y despacho",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = ColorShopMutedPlum,
                                        maxLines = 1
                                    )
                                }
                            }

                            if (uiState.customerNote.isNotBlank()) {
                                Surface(
                                    color = ColorShopPinkPill,
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = "✓ Listo",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ColorShopPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(
                            color = ColorShopSurfaceHigh.copy(alpha = 0.6f),
                            thickness = 1.dp
                        )

                        CustomerFieldsForm(
                            uiState = uiState,
                            cur = cur,
                            context = context,
                            haptic = haptic,
                            photoPickerLauncher = photoPickerLauncher,
                            onCustomerNoteChanged = onCustomerNoteChanged,
                            onCustomerPhoneChanged = onCustomerPhoneChanged,
                            onApproximateDeliveryTimeChanged = onApproximateDeliveryTimeChanged,
                            onProductAmountChanged = onProductAmountChanged,
                            onOrderCommentsChanged = onOrderCommentsChanged,
                            onReceiptPhotoSelected = onReceiptPhotoSelected
                        )
                    }
                }

                // 💳 Tarjeta: Cobro y Repartidor (Accesible)
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, ColorShopPrimary.copy(alpha = 0.15f)),
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(ColorShopPrimary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("💳", fontSize = 16.sp)
                            }
                            Column {
                                Text(
                                    text = "Cobro y Repartidor",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ColorShopDeepPlum
                                )
                                val courierLabel = uiState.selectedCourier?.name ?: "Sin asignar"
                                Text(
                                    text = "${uiState.selectedPaymentMethod.badgeLabel} • $courierLabel",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ColorShopPrimary
                                )
                            }
                        }

                        // Selector de Forma de Pago y Banco
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(14.dp),
                            shadowElevation = 1.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, ColorShopSurfaceHigh, RoundedCornerShape(14.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Forma de Pago del Pedido:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ColorShopDeepPlum
                                    )
                                    Text(
                                        text = uiState.selectedPaymentMethod.badgeLabel,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = ColorShopPrimary
                                    )
                                }

                                // Chips: Efectivo, Transferencia, POS Tarjeta, Mixto / Parcial en 2 filas limpias
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val entries = PaymentMethodType.entries
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        entries.take(2).forEach { method ->
                                            val isSelected = uiState.selectedPaymentMethod == method
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { onSelectPaymentMethod(method) },
                                                label = { Text(method.badgeLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                                modifier = Modifier.weight(1f),
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = ColorShopMagenta,
                                                    selectedLabelColor = Color.White
                                                )
                                            )
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        entries.drop(2).forEach { method ->
                                            val isSelected = uiState.selectedPaymentMethod == method
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { onSelectPaymentMethod(method) },
                                                label = { Text(method.badgeLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                                modifier = Modifier.weight(1f),
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = ColorShopMagenta,
                                                    selectedLabelColor = Color.White
                                                )
                                            )
                                        }
                                    }
                                }

                                // Si es Transferencia o Pago Mixto, mostrar los bancos (BAC, Lafise, Banpro, Ficohsa)
                                if (uiState.selectedPaymentMethod == PaymentMethodType.TRANSFER || uiState.selectedPaymentMethod == PaymentMethodType.MIXED) {
                                    HorizontalDivider(color = ColorShopSurfaceHigh.copy(alpha = 0.6f))
                                    Text(
                                        text = "Banco receptor para la transferencia:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ColorShopMutedPlum
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        TransferBankType.entries.forEach { bank ->
                                            val isSelected = uiState.selectedTransferBank == bank
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { onSelectTransferBank(bank) },
                                                label = { Text("🏦 ${bank.displayName}", fontSize = 11.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = ColorShopPrimary,
                                                    selectedLabelColor = Color.White
                                                )
                                            )
                                        }
                                    }

                                    HorizontalDivider(color = ColorShopSurfaceHigh.copy(alpha = 0.6f))
                                    Text(
                                        text = "Momento del pago / Depósito:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ColorShopMutedPlum
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        FilterChip(
                                            selected = !uiState.isPaymentPendingOnDelivery,
                                            onClick = { onSetPaymentPendingOnDelivery(false) },
                                            label = { Text("✅ Transf. Acreditada", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFF2E7D32),
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                        FilterChip(
                                            selected = uiState.isPaymentPendingOnDelivery,
                                            onClick = { onSetPaymentPendingOnDelivery(true) },
                                            label = { Text("⏳ Transf. al Recibir", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFFE65100),
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                    }
                                }

                                // Si es Pago Mixto, mostrar desglose dedicado (Transferencia + Efectivo)
                                if (uiState.selectedPaymentMethod == PaymentMethodType.MIXED) {
                                    HorizontalDivider(color = ColorShopSurfaceHigh.copy(alpha = 0.6f))
                                    Text(
                                        text = "Desglose de Pago Mixto (Transferencia + Efectivo):",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ColorShopDeepPlum
                                    )

                                    // Quick preset chips (cuando grandTotal > 0)
                                    if (grandTotal > 0.0) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            if (uiState.productAmount > 0.0 && quote != null) {
                                                FilterChip(
                                                    selected = kotlin.math.abs(uiState.partialTransferAmount - uiState.productAmount) < 0.01,
                                                    onClick = { onPartialTransferAmountChanged(uiState.productAmount.toInt().toString()) },
                                                    label = {
                                                        Text(
                                                            "🛍️ Mercadería Transf. (${cur}${uiState.productAmount.toInt()}) + Envío Efectivo (${cur}${quote.effectiveTotal.toInt()})",
                                                            fontSize = 10.sp
                                                        )
                                                    },
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = ColorShopPrimary,
                                                        selectedLabelColor = Color.White
                                                    )
                                                )
                                                FilterChip(
                                                    selected = kotlin.math.abs(uiState.partialTransferAmount - quote.effectiveTotal) < 0.01,
                                                    onClick = { onPartialTransferAmountChanged(quote.effectiveTotal.toInt().toString()) },
                                                    label = {
                                                        Text(
                                                            "🛵 Envío Transf. (${cur}${quote.effectiveTotal.toInt()}) + Mercadería Efectivo (${cur}${uiState.productAmount.toInt()})",
                                                            fontSize = 10.sp
                                                        )
                                                    },
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = ColorShopPrimary,
                                                        selectedLabelColor = Color.White
                                                    )
                                                )
                                            }
                                            FilterChip(
                                                selected = kotlin.math.abs(uiState.partialTransferAmount - (grandTotal / 2)) < 0.01,
                                                onClick = { onPartialTransferAmountChanged((grandTotal / 2).toInt().toString()) },
                                                label = {
                                                    Text(
                                                        "Mitad / 50% (${cur}${(grandTotal / 2).toInt()})",
                                                        fontSize = 10.sp
                                                    )
                                                },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = ColorShopPrimary,
                                                    selectedLabelColor = Color.White
                                                )
                                            )
                                        }
                                    }

                                    // Dos textfields lado a lado
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = uiState.partialTransferAmountInput,
                                            onValueChange = onPartialTransferAmountChanged,
                                            label = { Text("Transf. / Depósito ($cur)", fontSize = 10.sp) },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = ColorShopPrimary,
                                                unfocusedBorderColor = ColorShopSurfaceHigh
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = uiState.partialCashAmountInput,
                                            onValueChange = onPartialCashAmountChanged,
                                            label = { Text("Efectivo a Cobrar ($cur)", fontSize = 10.sp) },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = ColorShopPrimary,
                                                unfocusedBorderColor = ColorShopSurfaceHigh
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    // Resumen visual si quote != null
                                    if (quote != null) {
                                        val mixed = calculateMixedPaymentSettlement(
                                            uiState.productAmount,
                                            quote.effectiveTotal,
                                            uiState.partialTransferAmount
                                        )
                                        Surface(
                                            color = ColorShopSurfaceLow.copy(alpha = 0.7f),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, ColorShopPrimary.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = "💵 Motorizado cobra en efectivo: $cur ${String.format(Locale.US, "%.2f", mixed.cashToCollect)}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = ColorShopDeepPlum
                                                )
                                                val caseOutcomeText = when {
                                                    mixed.storeOwedByDriver > 0.009 ->
                                                        "✅ Se descuenta su envío ($cur ${mixed.deliveryFee.toInt()}) y DEBE DEPOSITAR A TIENDA: $cur ${String.format(Locale.US, "%.2f", mixed.storeOwedByDriver)}"
                                                    mixed.driverPendingFromStore <= 0.009 ->
                                                        "✅ Se queda con los $cur ${mixed.deliveryFee.toInt()} de su envío. Cuentas saldadas (Deposita $cur 0 a tienda)."
                                                    else ->
                                                        "⏳ Retiene $cur ${mixed.cashToCollect.toInt()} para su envío y TIENDA LE DEBE PAGAR: $cur ${String.format(Locale.US, "%.2f", mixed.driverPendingFromStore)}"
                                                }
                                                Text(
                                                    text = caseOutcomeText,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (mixed.driverPendingFromStore > 0.009) Color(0xFFE65100) else Color(0xFF2E7D32)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Si es POS Datáfono
                                if (uiState.selectedPaymentMethod == PaymentMethodType.POS) {
                                    HorizontalDivider(color = ColorShopSurfaceHigh.copy(alpha = 0.6f))
                                    Text(
                                        text = "Cobro con Datáfono POS:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ColorShopMutedPlum
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        FilterChip(
                                            selected = uiState.isPaymentPendingOnDelivery,
                                            onClick = { onSetPaymentPendingOnDelivery(true) },
                                            label = { Text("💳 Cobrar con POS al Entregar", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFFE65100),
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                        FilterChip(
                                            selected = !uiState.isPaymentPendingOnDelivery,
                                            onClick = { onSetPaymentPendingOnDelivery(false) },
                                            label = { Text("✅ Ya Cobrado en Tienda", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFF2E7D32),
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // Resumen de liquidación para el conductor según método de pago y banco
                        if (quote != null) {
                            val settlementPreview = RoutingAndFareCalculator.buildDriverSettlementNote(
                                paymentMethod = uiState.selectedPaymentMethod,
                                transferBank = uiState.selectedTransferBank,
                                productAmount = uiState.productAmount,
                                deliveryFee = quote.effectiveTotal,
                                currencySymbol = cur,
                                isPaymentPendingOnDelivery = uiState.isPaymentPendingOnDelivery,
                                partialTransferAmount = uiState.partialTransferAmount
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
                                    fontSize = 11.sp,
                                    color = ColorShopDeepPlum,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                                )
                            }
                        }

                        // Selector Rápido de Motorizado
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(14.dp),
                            shadowElevation = 1.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, ColorShopSurfaceHigh, RoundedCornerShape(14.dp))
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
                    }
                }

                // Indicador de Formato de Compartir Activo (Texto vs Imagen Digital)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Formato al compartir:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ColorShopMutedPlum
                    )
                    Surface(
                        color = ColorShopSurfaceHigh,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 36.dp)
                            .clickable { onOpenSettings() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (cfg.defaultShareFormat == ShareOutputFormat.IMAGE_RECEIPT) "🖼️ Recibo en Imagen" else "💬 Mensaje de Texto",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorShopPrimary
                            )
                            Text(
                                text = "Ajustar",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorShopMutedPlum
                            )
                        }
                    }
                }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    HorizontalDivider(color = ColorShopSurfaceHigh.copy(alpha = 0.7f))
                }

                // 3. Fila del Total a Pagar (SIEMPRE VISIBLE)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = quote != null) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showEditPriceDialog = true
                            }
                    ) {
                        Text(
                            text = when {
                                quote == null -> "TOTAL A PAGAR"
                                uiState.productAmount > 0.0 -> "GRAN TOTAL (ENVÍO + PRODUCTOS)"
                                quote.customTotalOverride != null -> "TARIFA TOTAL ACORDADA"
                                else -> "TARIFA DE ENVÍO"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF893B92),
                            letterSpacing = 0.6.sp
                        )
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = grandTotalFormatted,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ColorShopPrimary,
                                letterSpacing = (-0.5).sp
                            )
                            Text(
                                text = if (quote != null && quote.customTotalOverride != null) "acordado" else "neto",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ColorShopMutedPlum,
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        }
                    }

                    if (quote != null && dest != null) {
                        Surface(
                            color = ColorShopPinkPill,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "%.1f km • ~%d min",
                                    quote.effectiveDistanceKm,
                                    (quote.durationMinutes?.roundToInt() ?: 14).coerceAtLeast(1)
                                ),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorShopPrimary,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

            // Fila Inferior del Dock: Botones Gemelos de Acción Rápida (Cotizar | Despachar)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val hasDestination = quote != null && dest != null

                // Botón 1: Cotizar a Cliente (#FFEBF3 bg, #8C1D74 text con icono de Mariposa)
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (quote != null && dest != null) {
                            val receiptCode = "#${uiState.currentReceiptCode}"
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
                                productAmount = uiState.productAmount,
                                isPaymentPendingOnDelivery = uiState.isPaymentPendingOnDelivery,
                                receiptCode = receiptCode,
                                partialTransferAmount = uiState.partialTransferAmount
                            )
                            copyToClipboard(context, "Cotización ColorShop", summary)
                            if (cfg.defaultShareFormat == ShareOutputFormat.IMAGE_RECEIPT) {
                                val bitmap = ReceiptImageGenerator.generateCustomerReceiptBitmap(
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
                                    productAmount = uiState.productAmount,
                                    orderStatus = OrderStatus.EN_RUTA,
                                    isPaymentPendingOnDelivery = uiState.isPaymentPendingOnDelivery,
                                    customTemplate = cfg.customerMessageTemplate,
                                    receiptNumber = receiptCode,
                                    partialTransferAmount = uiState.partialTransferAmount
                                )
                                val imageUri = ReceiptImageGenerator.saveBitmapToShareCache(
                                    context,
                                    bitmap,
                                    "recibo_cliente"
                                )
                                ReceiptImageGenerator.shareReceiptImage(context, imageUri, summary)
                            } else {
                                shareTextToWhatsAppOrChooser(context, summary)
                            }
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
                        contentDescription = "Cotizar a Cliente",
                        modifier = Modifier.size(20.dp),
                        tint = Color.Unspecified
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Cotizar a Cliente",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                // Botón 2: Despachar Pedido (con prevención visual y lógica de duplicados)
                val isAlreadyDispatched = uiState.isCurrentQuoteSaved
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (isAlreadyDispatched) {
                            showDuplicateConfirmDialog = true
                        } else {
                            executeDispatch(true)
                        }
                    },
                    enabled = hasDestination,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAlreadyDispatched) Color(0xFF2E7D32) else ColorShopPrimary,
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
                        text = if (isAlreadyDispatched) "✅ Ya Despachado · Reenviar" else "Despachar Pedido",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
            }
        }
    }

    // Diálogo de confirmación para evitar duplicar registros en historial
    if (showDuplicateConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDuplicateConfirmDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "Pedido ya despachado",
                    fontWeight = FontWeight.ExtraBold,
                    color = ColorShopDeepPlum,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "Este pedido ya fue registrado previamente en el historial durante esta sesión.\n\n¿Deseas reenviar la información al repartidor sin duplicar el registro en el historial, o registrar un nuevo despacho idéntico?",
                    fontSize = 13.sp,
                    color = ColorShopDeepPlum,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDuplicateConfirmDialog = false
                        executeDispatch(false)
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = ColorShopPrimary)
                ) {
                    Text("Solo Reenviar WhatsApp", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = {
                            showDuplicateConfirmDialog = false
                            executeDispatch(true)
                        }
                    ) {
                        Text("Registrar Duplicado", color = Color(0xFFC2185B), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = { showDuplicateConfirmDialog = false }) {
                        Text("Cancelar", color = ColorShopMutedPlum, fontSize = 12.sp)
                    }
                }
            }
        )
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
                    if (currentQuote.finalRoundedTotal > uiState.config.preferentialThreshold) {
                        Surface(
                            color = ColorShopPrimary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    editedAmountText = String.format(Locale.US, "%.0f", uiState.suggestedPreferentialRate)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("⭐", fontSize = 14.sp)
                                Text(
                                    text = "Sugerir tarifa preferencial: ${uiState.config.currencySymbol} ${uiState.suggestedPreferentialRate.toInt()}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorShopPrimary
                                )
                            }
                        }
                    }
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

    // Diálogo emergente (Pop-up) para aviso y aplicación de Tarifa Preferencial
    if (uiState.showPreferentialRateDialog && uiState.quoteBreakdown != null) {
        val currentQuote = uiState.quoteBreakdown
        val cur = uiState.config.currencySymbol
        val calculated = currentQuote.finalRoundedTotal
        val suggested = if (uiState.suggestedPreferentialRate > 0.0) {
            uiState.suggestedPreferentialRate
        } else {
            RoutingAndFareCalculator.calculatePreferentialFare(
                calculatedTotal = calculated,
                threshold = uiState.config.preferentialThreshold,
                discountPercent = uiState.config.preferentialDiscountPercent,
                roundingMode = uiState.config.roundingMode
            )
        }

        var preferentialInputText by remember(suggested, calculated) {
            mutableStateOf(String.format(Locale.US, "%.0f", suggested))
        }

        AlertDialog(
            onDismissRequest = onDismissPreferentialDialog,
            containerColor = Color.White,
            shape = RoundedCornerShape(26.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(ColorShopPrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⭐", fontSize = 18.sp)
                    }
                    Column {
                        Text(
                            text = "Tarifa Preferencial",
                            fontWeight = FontWeight.ExtraBold,
                            color = ColorShopDeepPlum,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Aviso por distancia lejana",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = ColorShopMutedPlum
                        )
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "La ruta calculada suma $cur ${calculated.toInt()} (${String.format(Locale.US, "%.1f km", currentQuote.effectiveDistanceKm)}). Al superar el umbral de $cur ${uiState.config.preferentialThreshold.toInt()}, es recomendable aplicar una tarifa preferencial para que el cliente no rechace el envío por el costo.",
                        fontSize = 12.sp,
                        color = ColorShopDeepPlum,
                        lineHeight = 17.sp
                    )

                    // Comparativa visual de precios
                    Surface(
                        color = ColorShopSurfaceLow.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Tarifa calculada:",
                                    fontSize = 11.sp,
                                    color = ColorShopMutedPlum
                                )
                                Text(
                                    text = "$cur ${calculated.toInt()}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray,
                                    textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Tarifa preferencial:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorShopPrimary
                                )
                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "$cur ${suggested.toInt()}",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = ColorShopPrimary
                                    )
                                    val saving = (calculated - suggested).toInt()
                                    if (saving > 0) {
                                        Text(
                                            text = "(-$cur $saving)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2E7D32)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Campo editable para confirmar o personalizar el monto preferencial
                    OutlinedTextField(
                        value = preferentialInputText,
                        onValueChange = { preferentialInputText = it },
                        label = { Text("Monto preferencial a cobrar ($cur)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ColorShopPrimary,
                            unfocusedBorderColor = ColorShopSurfaceHigh
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Presets rápidos
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val presets = listOf(
                            suggested.toInt(),
                            (suggested - 10).toInt(),
                            (suggested + 10).toInt()
                        ).distinct().filter { it > 0 }

                        presets.forEach { p ->
                            val isSel = preferentialInputText.trim() == p.toString()
                            Surface(
                                color = if (isSel) ColorShopPrimary else ColorShopSurfaceLow,
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .clickable { preferentialInputText = p.toString() }
                            ) {
                                Text(
                                    text = if (p == suggested.toInt()) "$cur $p (Sugerido)" else "$cur $p",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.White else ColorShopDeepPlum,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = preferentialInputText.replace(",", ".").trim().toDoubleOrNull()
                        onApplyPreferentialRate(if (parsed != null && parsed > 0) parsed else suggested)
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = ColorShopPrimary)
                ) {
                    Text(
                        "Aplicar Tarifa ($cur ${preferentialInputText.trim().ifBlank { suggested.toInt().toString() }})",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissPreferentialDialog) {
                    Text("Mantener $cur ${calculated.toInt()}", color = ColorShopMutedPlum)
                }
            }
        )
    }

    // Diálogo de confirmación no agresivo cuando se detecta una Zona Aledaña
    if (uiState.showZoneConfirmDialog && uiState.suggestedZone != null && uiState.quoteBreakdown != null && uiState.frozenDestination != null) {
        val sZone = uiState.suggestedZone
        val currentQuote = uiState.quoteBreakdown
        val destCoord = uiState.frozenDestination.coordinate
        val distFromCenter = RoutingAndFareCalculator.calculateHaversineKm(
            GeoCoordinate(sZone.centerLat, sZone.centerLng),
            destCoord
        )
        val extraOutKm = kotlin.math.max(0.0, distFromCenter - sZone.radiusKm)
        val extraCharge = if (sZone.appliesExtraKm) extraOutKm * sZone.extraKmPrice else 0.0
        val zoneTotal = RoutingAndFareCalculator.applyRounding(
            sZone.basePrice + extraCharge + currentQuote.offHoursCharge + currentQuote.holidayCharge + currentQuote.rainCharge + currentQuote.manualExtraAdjustment,
            uiState.config.roundingMode
        )

        AlertDialog(
            onDismissRequest = onDismissZoneConfirmDialog,
            containerColor = Color.White,
            shape = RoundedCornerShape(26.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(ColorShopSurfaceLow),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(sZone.iconEmoji, fontSize = 18.sp)
                    }
                    Column {
                        Text(
                            text = "Zona Detectada: ${sZone.name}",
                            fontWeight = FontWeight.ExtraBold,
                            color = ColorShopDeepPlum,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "Confirmación de tarifa de zona",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = ColorShopMutedPlum
                        )
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "El punto de entrega coincide con la zona configurada '${sZone.name}' (Radio base: ${String.format(Locale.US, "%.1f", sZone.radiusKm)} km).",
                        fontSize = 12.sp,
                        color = ColorShopDeepPlum,
                        lineHeight = 17.sp
                    )

                    Surface(
                        color = ColorShopSurfaceLow.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Precio base zona:", fontSize = 11.5.sp, color = ColorShopMutedPlum)
                                Text("$cur ${sZone.basePrice.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ColorShopDeepPlum)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Distancia al centro de zona:", fontSize = 11.5.sp, color = ColorShopMutedPlum)
                                Text("${String.format(Locale.US, "%.2f", distFromCenter)} km", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = ColorShopDeepPlum)
                            }
                            if (extraOutKm > 0.05 && sZone.appliesExtraKm) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Exceso fuera del círculo (${String.format(Locale.US, "%.2f", extraOutKm)} km):", fontSize = 11.5.sp, color = ColorShopMutedPlum)
                                    Text("+$cur ${extraCharge.roundToInt()}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ColorShopPrimary)
                                }
                            } else {
                                Text(
                                    text = "✓ Dentro del radio de cobertura base (${String.format(Locale.US, "%.1f", sZone.radiusKm)} km)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                            HorizontalDivider(color = ColorShopSurfaceHigh)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Total con tarifa de zona:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ColorShopDeepPlum)
                                Text("$cur ${zoneTotal.toInt()}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = ColorShopPrimary)
                            }
                        }
                    }

                    Text(
                        text = "¿Deseas aplicar la tarifa de ${sZone.name} o mantener el cálculo por distancia estándar ($cur ${currentQuote.finalRoundedTotal.toInt()})?",
                        fontSize = 11.5.sp,
                        color = ColorShopMutedPlum
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = onConfirmSuggestedZone,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = ColorShopPrimary)
                ) {
                    Text(
                        "Aplicar Zona ($cur ${zoneTotal.toInt()})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissZoneConfirmDialog) {
                    Text(
                        "Usar tarifa estándar ($cur ${currentQuote.finalRoundedTotal.toInt()})",
                        color = ColorShopMutedPlum,
                        fontSize = 12.sp
                    )
                }
            }
        )
    }

    // Bottom Sheet de "Ajustes" (Ajustes rápidos: recargos, zona aledaña y modo de cálculo)
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
private fun CustomerFieldsForm(
    uiState: DeliveryUiState,
    cur: String,
    context: Context,
    haptic: HapticFeedback,
    photoPickerLauncher: ActivityResultLauncher<String>,
    onCustomerNoteChanged: (String) -> Unit,
    onCustomerPhoneChanged: (String) -> Unit,
    onApproximateDeliveryTimeChanged: (String) -> Unit,
    onProductAmountChanged: (String) -> Unit,
    onOrderCommentsChanged: (String) -> Unit,
    onReceiptPhotoSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Nombre del cliente o persona a recibir
        OutlinedTextField(
            value = uiState.customerNote,
            onValueChange = onCustomerNoteChanged,
            label = { Text("Cliente / Persona que recibe", fontSize = 11.sp) },
            placeholder = { Text("Ej. María Fernanda Pérez", fontSize = 12.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = ColorShopPrimary,
                    modifier = Modifier.size(17.dp)
                )
            },
            trailingIcon = {
                if (uiState.customerNote.isNotBlank()) {
                    IconButton(
                        onClick = { onCustomerNoteChanged("") },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Limpiar cliente",
                            tint = ColorShopMutedPlum,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ColorShopPrimary,
                unfocusedBorderColor = ColorShopSurfaceHigh,
                focusedContainerColor = ColorShopSurfaceLow.copy(alpha = 0.35f),
                unfocusedContainerColor = ColorShopSurfaceLow.copy(alpha = 0.35f)
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
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = ColorShopPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ColorShopPrimary,
                    unfocusedBorderColor = ColorShopSurfaceHigh,
                    focusedContainerColor = ColorShopSurfaceLow.copy(alpha = 0.35f),
                    unfocusedContainerColor = ColorShopSurfaceLow.copy(alpha = 0.35f)
                ),
                modifier = Modifier.weight(1f)
            )

            OutlinedTextField(
                value = uiState.approximateDeliveryTime,
                onValueChange = onApproximateDeliveryTimeChanged,
                label = { Text("Hora aprox.", fontSize = 11.sp) },
                placeholder = { Text("Ej. 3:30 PM", fontSize = 12.sp) },
                leadingIcon = {
                    IconButton(
                        onClick = {
                            val cal = Calendar.getInstance()
                            TimePickerDialog(
                                context,
                                { _, hourOfDay, minute ->
                                    val pickedCal = Calendar.getInstance().apply {
                                        set(Calendar.HOUR_OF_DAY, hourOfDay)
                                        set(Calendar.MINUTE, minute)
                                    }
                                    val sdf = SimpleDateFormat("hh:mm a", Locale.US)
                                    onApproximateDeliveryTimeChanged(sdf.format(pickedCal.time))
                                },
                                cal.get(Calendar.HOUR_OF_DAY),
                                cal.get(Calendar.MINUTE),
                                false
                            ).show()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = "Abrir reloj",
                            tint = ColorShopPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ColorShopPrimary,
                    unfocusedBorderColor = ColorShopSurfaceHigh,
                    focusedContainerColor = ColorShopSurfaceLow.copy(alpha = 0.35f),
                    unfocusedContainerColor = ColorShopSurfaceLow.copy(alpha = 0.35f)
                ),
                modifier = Modifier.weight(1f)
            )
        }

        // Chips de Selección Rápida de Hora Estimada
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Hora rápida:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = ColorShopMutedPlum
            )
            val quickIntervals = listOf(
                "En 30 min" to 30,
                "En 45 min" to 45,
                "En 1 hr" to 60
            )
            quickIntervals.forEach { (label, mins) ->
                val cal = Calendar.getInstance()
                cal.add(Calendar.MINUTE, mins)
                val targetTime = SimpleDateFormat("hh:mm a", Locale.US).format(cal.time)
                val isSelected = uiState.approximateDeliveryTime.startsWith(label)

                Surface(
                    color = if (isSelected) ColorShopPrimary else ColorShopSurfaceLow,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) ColorShopPrimary else ColorShopSurfaceHigh
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onApproximateDeliveryTimeChanged(label + " (~" + targetTime + ")")
                        }
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else ColorShopDeepPlum,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
            }

            // Chip para abrir TimePickerDialog nativo
            Surface(
                color = ColorShopPinkPill,
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, ColorShopPrimary.copy(alpha = 0.35f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val cal = Calendar.getInstance()
                        TimePickerDialog(
                            context,
                            { _, hourOfDay, minute ->
                                val pickedCal = Calendar.getInstance().apply {
                                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                                    set(Calendar.MINUTE, minute)
                                }
                                val sdf = SimpleDateFormat("hh:mm a", Locale.US)
                                onApproximateDeliveryTimeChanged(sdf.format(pickedCal.time))
                            },
                            cal.get(Calendar.HOUR_OF_DAY),
                            cal.get(Calendar.MINUTE),
                            false
                        ).show()
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = ColorShopPrimary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Reloj 🕒",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorShopPrimary
                    )
                }
            }
        }

        // Total de Productos / Mercadería por separado
        OutlinedTextField(
            value = uiState.productAmountInput,
            onValueChange = onProductAmountChanged,
            label = { Text("Total Productos / Mercadería (" + cur + ")", fontSize = 11.sp) },
            placeholder = { Text("Ej. 450", fontSize = 12.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Inventory2,
                    contentDescription = null,
                    tint = ColorShopPrimary,
                    modifier = Modifier.size(17.dp)
                )
            },
            trailingIcon = {
                if (uiState.productAmountInput.isNotBlank()) {
                    IconButton(
                        onClick = { onProductAmountChanged("") },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Limpiar monto productos",
                            tint = ColorShopMutedPlum,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next
            ),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ColorShopPrimary,
                unfocusedBorderColor = ColorShopSurfaceHigh,
                focusedContainerColor = ColorShopSurfaceLow.copy(alpha = 0.35f),
                unfocusedContainerColor = ColorShopSurfaceLow.copy(alpha = 0.35f)
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
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Notes,
                    contentDescription = null,
                    tint = ColorShopPrimary,
                    modifier = Modifier.size(17.dp)
                )
            },
            trailingIcon = {
                if (uiState.orderComments.isNotBlank()) {
                    IconButton(
                        onClick = { onOrderCommentsChanged("") },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Limpiar comentarios",
                            tint = ColorShopMutedPlum,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            maxLines = 2,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ColorShopPrimary,
                unfocusedBorderColor = ColorShopSurfaceHigh,
                focusedContainerColor = ColorShopSurfaceLow.copy(alpha = 0.35f),
                unfocusedContainerColor = ColorShopSurfaceLow.copy(alpha = 0.35f)
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
                    containerColor = ColorShopSurfaceLow,
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

@Composable
private fun ActiveMiniBadge(text: String) {
    Surface(
        color = ColorShopSurfaceLow,
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = ColorShopPrimary,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
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
