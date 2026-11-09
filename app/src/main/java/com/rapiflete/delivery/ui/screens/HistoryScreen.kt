package com.rapiflete.delivery.ui.screens

import android.app.DatePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.rapiflete.delivery.model.CourierDriver
import com.rapiflete.delivery.model.GeoCoordinate
import com.rapiflete.delivery.model.OrderStatus
import com.rapiflete.delivery.model.PaymentMethodType
import com.rapiflete.delivery.model.SavedQuoteRecord
import com.rapiflete.delivery.model.StoreAndPricingConfig
import com.rapiflete.delivery.ui.components.ColorShopTopBar
import com.rapiflete.delivery.ui.theme.ColorShopDeepPlum
import com.rapiflete.delivery.ui.theme.ColorShopMutedPlum
import com.rapiflete.delivery.ui.theme.ColorShopOutlineVariant
import com.rapiflete.delivery.ui.theme.ColorShopPrimary
import com.rapiflete.delivery.ui.theme.ColorShopSurface
import com.rapiflete.delivery.ui.theme.ColorShopSurfaceHigh
import com.rapiflete.delivery.ui.theme.ColorShopSurfaceLow
import com.rapiflete.delivery.ui.theme.ColorShopTertiaryBlue
import com.rapiflete.delivery.ui.theme.ColorShopTertiaryFixed
import com.rapiflete.delivery.util.ReceiptImageGenerator
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    records: List<SavedQuoteRecord>,
    couriers: List<CourierDriver>,
    currencySymbol: String,
    config: StoreAndPricingConfig = StoreAndPricingConfig(),
    onSelectRecord: (SavedQuoteRecord) -> Unit,
    onDeleteRecord: (String) -> Unit,
    onUpdateOrderStatus: (String, OrderStatus) -> Unit = { _, _ -> },
    onToggleDriverPaid: (String) -> Unit = {},
    onNavigateToQuote: (() -> Unit)? = null,
    onClearAll: () -> Unit
) {
    val context = LocalContext.current
    val verticalScroll = rememberScrollState()

    val todayIso = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }
    var selectedDateIso by remember { mutableStateOf(todayIso) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedPaymentFilter by remember { mutableStateOf("ALL") } // ALL, CASH, TRANSFER
    var selectedCourierName by remember { mutableStateOf<String?>(null) }
    var selectedStatusFilter by remember { mutableStateOf<OrderStatus?>(null) }
    var showOnlyUnpaidDriverDeliveries by remember { mutableStateOf(false) }
    var showFiltersPanel by remember { mutableStateOf(false) }

    var recordPendingDelete by remember { mutableStateOf<SavedQuoteRecord?>(null) }
    var recordPendingCancel by remember { mutableStateOf<SavedQuoteRecord?>(null) }

    if (recordPendingDelete != null) {
        val target = recordPendingDelete!!
        AlertDialog(
            onDismissRequest = { recordPendingDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = null,
                    tint = Color(0xFFC62828),
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "¿Eliminar pedido permanentemente?",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorShopDeepPlum
                )
            },
            text = {
                Text(
                    text = "Se eliminará el registro de \"${target.customerNote}\" (${target.receiptId}). Esta acción no se puede deshacer.",
                    fontSize = 13.sp,
                    color = ColorShopMutedPlum
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteRecord(target.id)
                        recordPendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) {
                    Text("Eliminar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { recordPendingDelete = null }) {
                    Text("Cancelar", color = ColorShopMutedPlum)
                }
            }
        )
    }

    if (recordPendingCancel != null) {
        val target = recordPendingCancel!!
        AlertDialog(
            onDismissRequest = { recordPendingCancel = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = Color(0xFFC62828),
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "¿Cancelar este pedido?",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorShopDeepPlum
                )
            },
            text = {
                Text(
                    text = "¿Estás seguro de cancelar el pedido de \"${target.customerNote}\" (${target.receiptId})? Cambiará al estado Cancelado ❌.",
                    fontSize = 13.sp,
                    color = ColorShopMutedPlum
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateOrderStatus(target.id, OrderStatus.CANCELADO)
                        recordPendingCancel = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) {
                    Text("Sí, cancelar pedido", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { recordPendingCancel = null }) {
                    Text("Volver", color = ColorShopMutedPlum)
                }
            }
        )
    }

    // Envíos con saldo pendiente de pago al motorizado (acumula hoy, ayer y días anteriores)
    val allUnpaidElectronicDeliveries = remember(records, selectedCourierName) {
        records.filter { rec ->
            !rec.isDriverPaid &&
            rec.driverPendingFromStore > 0.009 &&
            rec.orderStatus != OrderStatus.CANCELADO &&
            (selectedCourierName == null || rec.courierName.equals(selectedCourierName, ignoreCase = true))
        }
    }

    val totalOwedAllTime = remember(allUnpaidElectronicDeliveries) {
        allUnpaidElectronicDeliveries.sumOf { it.driverPendingFromStore }
    }

    val unpaidPastDays = remember(allUnpaidElectronicDeliveries, todayIso) {
        allUnpaidElectronicDeliveries.filter { (it.dateIso.ifBlank { todayIso }) != todayIso }
    }

    val unpaidToday = remember(allUnpaidElectronicDeliveries, todayIso) {
        allUnpaidElectronicDeliveries.filter { (it.dateIso.ifBlank { todayIso }) == todayIso }
    }

    // Filtrar estrictamente por la fecha seleccionada para no acarrear datos de días pasados
    val recordsForSelectedDate = remember(records, selectedDateIso) {
        records.filter { rec ->
            val recDate = rec.dateIso.ifBlank { todayIso }
            recDate == selectedDateIso
        }
    }

    val availableDatesWithRecords = remember(records, todayIso) {
        (records.map { it.dateIso.ifBlank { todayIso } } + todayIso)
            .distinct()
            .sortedDescending()
    }

    val filteredRecords = remember(
        recordsForSelectedDate,
        selectedPaymentFilter,
        selectedCourierName,
        selectedStatusFilter,
        searchQuery
    ) {
        val q = searchQuery.trim().lowercase(Locale.ROOT)
        recordsForSelectedDate.filter { rec ->
            val matchesPayment = when (selectedPaymentFilter) {
                "CASH" -> rec.paymentMethod == PaymentMethodType.CASH
                "TRANSFER" -> rec.paymentMethod == PaymentMethodType.TRANSFER
                "MIXED" -> rec.paymentMethod == PaymentMethodType.MIXED
                else -> true
            }
            val matchesCourier = selectedCourierName == null ||
                rec.courierName.equals(selectedCourierName, ignoreCase = true)
            val matchesStatus = selectedStatusFilter == null || rec.orderStatus == selectedStatusFilter

            val matchesSearch = q.isEmpty() ||
                rec.customerNote.lowercase(Locale.ROOT).contains(q) ||
                rec.customerPhone.lowercase(Locale.ROOT).contains(q) ||
                rec.receiptId.lowercase(Locale.ROOT).contains(q) ||
                rec.courierName.lowercase(Locale.ROOT).contains(q) ||
                rec.destinationLabel.lowercase(Locale.ROOT).contains(q)

            matchesPayment && matchesCourier && matchesStatus && matchesSearch
        }
    }

    val displayedRecords = remember(
        showOnlyUnpaidDriverDeliveries,
        allUnpaidElectronicDeliveries,
        filteredRecords,
        selectedStatusFilter,
        searchQuery
    ) {
        val q = searchQuery.trim().lowercase(Locale.ROOT)
        if (showOnlyUnpaidDriverDeliveries) {
            allUnpaidElectronicDeliveries.filter { rec ->
                val matchesStatus = selectedStatusFilter == null || rec.orderStatus == selectedStatusFilter
                val matchesSearch = q.isEmpty() ||
                    rec.customerNote.lowercase(Locale.ROOT).contains(q) ||
                    rec.customerPhone.lowercase(Locale.ROOT).contains(q) ||
                    rec.receiptId.lowercase(Locale.ROOT).contains(q) ||
                    rec.courierName.lowercase(Locale.ROOT).contains(q) ||
                    rec.destinationLabel.lowercase(Locale.ROOT).contains(q)
                matchesStatus && matchesSearch
            }
        } else {
            filteredRecords
        }
    }

    // Métricas del día para la Hero Card (filtradas por repartidor si se selecciona)
    val dailyBaseRecords = remember(recordsForSelectedDate, selectedCourierName) {
        if (selectedCourierName == null) recordsForSelectedDate
        else recordsForSelectedDate.filter { it.courierName.equals(selectedCourierName, ignoreCase = true) }
    }
    val dailyTotalCount = dailyBaseRecords.size
    val dailyTotalRevenue = dailyBaseRecords.sumOf { it.totalAmount }
    val dailyInRouteCount = dailyBaseRecords.count { it.orderStatus == OrderStatus.EN_RUTA }
    val dailyDeliveredCount = dailyBaseRecords.count { it.orderStatus == OrderStatus.ENTREGADO }

    val totalAmount = filteredRecords.sumOf { it.totalAmount }
    val totalKm = filteredRecords.sumOf { it.distanceKm }
    val selectedDateLabel = remember(selectedDateIso, todayIso) {
        formatDateIsoHuman(selectedDateIso, todayIso)
    }

    fun openNativeDatePicker() {
        val cal = Calendar.getInstance()
        runCatching {
            val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(selectedDateIso)
            if (parsed != null) cal.time = parsed
        }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                selectedDateIso = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorShopSurface)
            .verticalScroll(verticalScroll)
    ) {
        ColorShopTopBar(showQuickSearchButton = false)

        // Encabezado de página: Historial de Envíos + Botones circulares (Calendario + Descargar)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Historial de Envíos",
                    fontSize = 23.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ColorShopDeepPlum
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(ColorShopPrimary)
                    )
                    Text(
                        text = if (selectedDateIso == todayIso) "Registros de hoy" else "Viendo fecha: $selectedDateLabel",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ColorShopMutedPlum
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    color = if (selectedDateIso != todayIso) ColorShopPrimary else Color.White,
                    shape = CircleShape,
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .size(40.dp)
                        .clickable {
                            openNativeDatePicker()
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Seleccionar fecha",
                            tint = if (selectedDateIso != todayIso) Color.White else ColorShopPrimary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                Surface(
                    color = Color.White,
                    shape = CircleShape,
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .size(40.dp)
                        .clickable {
                            val report = buildDailyReportText(
                                records = filteredRecords,
                                currencySymbol = currencySymbol,
                                courierFilter = selectedCourierName,
                                dateLabel = selectedDateLabel
                            )
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, report)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Exportar reporte del día"))
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Descargar reporte",
                            tint = ColorShopPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Barra rápida de fechas con envíos compacta
        if (availableDatesWithRecords.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = ColorShopSurfaceHigh,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .height(28.dp)
                        .clickable { openNativeDatePicker() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = ColorShopPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Otra fecha",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorShopPrimary
                        )
                    }
                }

                availableDatesWithRecords.forEach { dateIso ->
                    val countForDate = records.count { (it.dateIso.ifBlank { todayIso }) == dateIso }
                    val isSelected = dateIso == selectedDateIso
                    Surface(
                        color = if (isSelected) ColorShopPrimary else Color.White,
                        shape = RoundedCornerShape(50),
                        shadowElevation = if (isSelected) 2.dp else 1.dp,
                        modifier = Modifier
                            .height(28.dp)
                            .clickable { selectedDateIso = dateIso }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        ) {
                            Text(
                                text = "${formatDateIsoShort(dateIso, todayIso)} ($countForDate)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else ColorShopDeepPlum
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Hero Card Compacta de Métricas Diarias (Surface con esquinas redondeadas 20.dp, fondo tonal/blanco, divisores verticales sutiles)
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(20.dp),
            shadowElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(vertical = 12.dp, horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Fila de encabezado compacta: Selector de fecha + navegación rápida entre días
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { openNativeDatePicker() }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(ColorShopPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalShipping,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = selectedDateLabel,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ColorShopDeepPlum
                                )
                                if (selectedDateIso == todayIso) {
                                    Surface(
                                        color = ColorShopPrimary.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text(
                                            text = "HOY",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = ColorShopPrimary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = if (selectedDateIso == todayIso) "Métricas del día actual" else "Toca para cambiar de fecha",
                                fontSize = 11.sp,
                                color = ColorShopMutedPlum
                            )
                        }
                    }

                    // Navegación rápida entre fechas: anterior / hoy / siguiente
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            color = ColorShopSurfaceLow,
                            shape = CircleShape,
                            modifier = Modifier
                                .size(28.dp)
                                .clickable {
                                    selectedDateIso = shiftDateIsoByDays(selectedDateIso, -1)
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = "Día anterior",
                                    tint = ColorShopDeepPlum,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        if (selectedDateIso != todayIso) {
                            Surface(
                                color = ColorShopSurfaceHigh,
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.clickable { selectedDateIso = todayIso }
                            ) {
                                Text(
                                    text = "Hoy",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorShopPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Surface(
                            color = ColorShopSurfaceLow,
                            shape = CircleShape,
                            modifier = Modifier
                                .size(28.dp)
                                .clickable {
                                    selectedDateIso = shiftDateIsoByDays(selectedDateIso, 1)
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Día siguiente",
                                    tint = ColorShopDeepPlum,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Indicador sutil de filtro por motorizado en métricas
                if (selectedCourierName != null) {
                    Surface(
                        color = ColorShopSurfaceLow.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterAlt,
                                    contentDescription = null,
                                    tint = ColorShopPrimary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Filtrando por: $selectedCourierName",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorShopPrimary
                                )
                            }
                            Text(
                                text = "Quitar ✕",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorShopMutedPlum,
                                modifier = Modifier.clickable { selectedCourierName = null }
                            )
                        }
                    }
                }

                HorizontalDivider(color = ColorShopSurfaceHigh)

                // Tarjeta horizontal tonal unificada y segmentada con las 3 métricas
                Surface(
                    color = ColorShopSurfaceLow.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Segmento 1: Envíos Totales del Día
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Inventory2,
                                    contentDescription = null,
                                    tint = ColorShopPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Envíos",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ColorShopMutedPlum
                                )
                            }
                            Text(
                                text = "$dailyTotalCount",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ColorShopDeepPlum
                            )
                            Text(
                                text = "$dailyDeliveredCount entregados",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (dailyDeliveredCount > 0) Color(0xFF2E7D32) else ColorShopMutedPlum
                            )
                        }

                        // Divisor vertical sutil
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(38.dp)
                                .background(ColorShopOutlineVariant.copy(alpha = 0.4f))
                        )

                        // Segmento 2: Total Recaudado C$
                        Column(
                            modifier = Modifier.weight(1.2f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Payments,
                                    contentDescription = null,
                                    tint = ColorShopPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Recaudado",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ColorShopMutedPlum
                                )
                            }
                            Text(
                                text = "$currencySymbol ${dailyTotalRevenue.toInt()}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ColorShopPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (totalKm > 0.0) String.format(Locale.US, "%.1f km ruta", totalKm) else "Total fecha",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = ColorShopMutedPlum
                            )
                        }

                        // Divisor vertical sutil
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(38.dp)
                                .background(ColorShopOutlineVariant.copy(alpha = 0.4f))
                        )

                        // Segmento 3: Pedidos en ruta o pendientes
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.DirectionsBike,
                                    contentDescription = null,
                                    tint = if (dailyInRouteCount > 0) Color(0xFFE65100) else Color(0xFF2E7D32),
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "En Ruta",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ColorShopMutedPlum
                                )
                            }
                            Text(
                                text = "$dailyInRouteCount",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (dailyInRouteCount > 0) Color(0xFFE65100) else Color(0xFF2E7D32)
                            )
                            Text(
                                text = if (dailyInRouteCount > 0) "En camino 🛵" else "Al día ✅",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (dailyInRouteCount > 0) Color(0xFFE65100) else Color(0xFF2E7D32)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Recuadro Bento Destacado: Saldo Pendiente a Pagar al Motorizado (Hoy y Acumulado de Días Anteriores)
        if (totalOwedAllTime == 0.0) {
            Surface(
                color = Color(0xFFF1F8E9),
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .border(1.dp, Color(0xFF81C784).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4CAF50)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Al día con motorizados ✅",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF2E7D32)
                            )
                            Text(
                                text = if (selectedCourierName != null) "Repartidor: $selectedCourierName" else "Sin pagos pendientes de transferencia ni POS",
                                fontSize = 10.sp,
                                color = Color(0xFF388E3C)
                            )
                        }
                    }
                    Surface(
                        color = Color(0xFF2E7D32),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "$currencySymbol 0",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        } else {
            Surface(
                color = Color(0xFFFFF8E1),
                shape = RoundedCornerShape(20.dp),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .border(
                        1.dp,
                        Color(0xFFFFB300).copy(alpha = 0.7f),
                        RoundedCornerShape(20.dp)
                    )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
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
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFB300)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Payments,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Total a Pagar al Motorizado",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ColorShopDeepPlum
                                )
                                Text(
                                    text = if (selectedCourierName != null) "Repartidor: $selectedCourierName" else "Todos los repartidores",
                                    fontSize = 11.sp,
                                    color = ColorShopMutedPlum
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "$currencySymbol ${totalOwedAllTime.toInt()}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFE65100)
                            )
                            Text(
                                text = "Pendiente tienda",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100)
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0xFFFFE082))

                    // Desglose de saldos: acumulado de días anteriores vs saldo de hoy
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        if (unpaidPastDays.isNotEmpty()) {
                            Text(
                                text = "⚠️ Acumulado de días anteriores: $currencySymbol ${unpaidPastDays.sumOf { it.driverPendingFromStore }.toInt()} (${unpaidPastDays.size} envíos)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD84315)
                            )
                        }
                        Text(
                            text = "• Saldo pendiente de hoy: $currencySymbol ${unpaidToday.sumOf { it.driverPendingFromStore }.toInt()} (${unpaidToday.size} envíos)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = ColorShopDeepPlum
                        )
                        Text(
                            text = "ℹ️ Los envíos en efectivo fueron auto-descontados por el motorizado (ya liquidados).",
                            fontSize = 10.sp,
                            color = ColorShopMutedPlum
                        )
                    }

                    // Botones rápidos: Conmutar lista de pendientes acumulados y Generar Liquidación Gráfica
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = if (showOnlyUnpaidDriverDeliveries) Color(0xFFE65100) else Color.White,
                            shape = RoundedCornerShape(50),
                            shadowElevation = 1.dp,
                            modifier = Modifier
                                .weight(1.1f)
                                .height(34.dp)
                                .clickable { showOnlyUnpaidDriverDeliveries = !showOnlyUnpaidDriverDeliveries }
                                .border(1.dp, Color(0xFFE65100), RoundedCornerShape(50))
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
                                Text(
                                    text = if (showOnlyUnpaidDriverDeliveries) "Ver vista normal por fecha" else "Ver Pendientes (${allUnpaidElectronicDeliveries.size})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (showOnlyUnpaidDriverDeliveries) Color.White else Color(0xFFE65100)
                                )
                            }
                        }

                        Surface(
                            color = ColorShopPrimary,
                            shape = RoundedCornerShape(50),
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .weight(0.9f)
                                .height(34.dp)
                                .clickable {
                                    val targetRecords = if (selectedCourierName != null) {
                                        records.filter { it.courierName.equals(selectedCourierName, ignoreCase = true) }
                                    } else {
                                        records
                                    }
                                    val reportBitmap = ReceiptImageGenerator.generateDriverSettlementReportBitmap(
                                        courierName = selectedCourierName ?: "General",
                                        records = targetRecords,
                                        currencySymbol = currencySymbol
                                    )
                                    val uri = ReceiptImageGenerator.saveBitmapToShareCache(context, reportBitmap, "liquidacion_cuentas")
                                    ReceiptImageGenerator.shareReceiptImage(
                                        context,
                                        uri,
                                        "Corte de Cuentas y Liquidación • ColorShop Delivery • Repartidor: ${selectedCourierName ?: "Todos"}"
                                    )
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
                                Text(
                                    text = "Liquidación 📊",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Buscador Instantáneo en Tiempo Real
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 2.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "Buscar cliente, teléfono, #recibo, motorizado...",
                        fontSize = 13.sp,
                        color = ColorShopMutedPlum.copy(alpha = 0.75f)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Buscar",
                        tint = if (searchQuery.isNotBlank()) ColorShopPrimary else ColorShopMutedPlum,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Limpiar búsqueda",
                                tint = ColorShopMutedPlum,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ColorShopPrimary,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedTextColor = ColorShopDeepPlum,
                    unfocusedTextColor = ColorShopDeepPlum,
                    cursorColor = ColorShopPrimary
                )
            )
        }

        if (searchQuery.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filtrando por \"$searchQuery\" (${displayedRecords.size} resultados)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ColorShopPrimary
                )
                Text(
                    text = "Limpiar ✕",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorShopMutedPlum,
                    modifier = Modifier.clickable { searchQuery = "" }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Barra / Sección Compacta y Colapsable de Filtros Consolidados
        val hasActiveFilters = selectedStatusFilter != null || selectedPaymentFilter != "ALL" || selectedCourierName != null
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 2.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showFiltersPanel = !showFiltersPanel }
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterAlt,
                            contentDescription = "Filtros",
                            tint = if (hasActiveFilters) ColorShopPrimary else ColorShopMutedPlum,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Filtros",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (hasActiveFilters) ColorShopPrimary else ColorShopDeepPlum
                        )
                        if (hasActiveFilters) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.horizontalScroll(rememberScrollState())
                            ) {
                                selectedStatusFilter?.let { st ->
                                    CompactFilterBadge(text = "${st.emoji} ${st.label}") {
                                        selectedStatusFilter = null
                                    }
                                }
                                if (selectedPaymentFilter != "ALL") {
                                    val pText = when (selectedPaymentFilter) {
                                        "CASH" -> "Efectivo"
                                        "TRANSFER" -> "Transferencia"
                                        "MIXED" -> "Mixto"
                                        else -> selectedPaymentFilter
                                    }
                                    CompactFilterBadge(text = pText) {
                                        selectedPaymentFilter = "ALL"
                                    }
                                }
                                selectedCourierName?.let { cName ->
                                    CompactFilterBadge(text = cName.substringBefore(" ")) {
                                        selectedCourierName = null
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "Todos los pedidos (${recordsForSelectedDate.size})",
                                fontSize = 11.sp,
                                color = ColorShopMutedPlum,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (hasActiveFilters) {
                            Text(
                                text = "Limpiar",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorShopPrimary,
                                modifier = Modifier
                                    .clickable {
                                        selectedStatusFilter = null
                                        selectedPaymentFilter = "ALL"
                                        selectedCourierName = null
                                    }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                        Icon(
                            imageVector = if (showFiltersPanel) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (showFiltersPanel) "Colapsar filtros" else "Expandir filtros",
                            tint = ColorShopPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                AnimatedVisibility(visible = showFiltersPanel) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HorizontalDivider(color = ColorShopSurfaceHigh)

                        // 1. Estados
                        Text(
                            text = "Estado del pedido:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorShopMutedPlum
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            HistoryFilterPill(
                                label = "Todos (${recordsForSelectedDate.size})",
                                selected = selectedStatusFilter == null,
                                onClick = { selectedStatusFilter = null }
                            )
                            OrderStatus.entries.forEach { st ->
                                val count = recordsForSelectedDate.count { it.orderStatus == st }
                                HistoryFilterPill(
                                    label = "${st.emoji} ${st.label} ($count)",
                                    selected = selectedStatusFilter == st,
                                    onClick = { selectedStatusFilter = st }
                                )
                            }
                        }

                        // 2. Método de pago
                        Text(
                            text = "Método de pago:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorShopMutedPlum
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            HistoryFilterPill(
                                label = "Todos Pagos",
                                selected = selectedPaymentFilter == "ALL",
                                onClick = { selectedPaymentFilter = "ALL" }
                            )
                            HistoryFilterPill(
                                label = "Efectivo",
                                selected = selectedPaymentFilter == "CASH",
                                onClick = { selectedPaymentFilter = "CASH" }
                            )
                            HistoryFilterPill(
                                label = "Transferencia",
                                selected = selectedPaymentFilter == "TRANSFER",
                                onClick = { selectedPaymentFilter = "TRANSFER" }
                            )
                            HistoryFilterPill(
                                label = "Mixto / Parcial",
                                selected = selectedPaymentFilter == "MIXED",
                                onClick = { selectedPaymentFilter = "MIXED" }
                            )
                        }

                        // 3. Repartidor
                        Text(
                            text = "Repartidor:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorShopMutedPlum
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HistoryFilterPill(
                                label = "Todos (${couriers.size})",
                                selected = selectedCourierName == null,
                                onClick = { selectedCourierName = null }
                            )

                            couriers.forEach { courier ->
                                val count = recordsForSelectedDate.count { it.courierName.equals(courier.name, ignoreCase = true) }
                                HistoryFilterPill(
                                    label = "${courier.initials} · ${courier.name.substringBefore(" ")} ($count)",
                                    selected = selectedCourierName == courier.name,
                                    onClick = {
                                        selectedCourierName = if (selectedCourierName == courier.name) null else courier.name
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Banner informativo cuando el filtro de pendientes acumulados está activado
        if (showOnlyUnpaidDriverDeliveries) {
            Surface(
                color = Color(0xFFFFF3E0),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .border(1.dp, Color(0xFFE65100).copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mostrando ${displayedRecords.size} envíos con tarifa pendiente al motorizado (acumulado)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFBF360C),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Ver por fecha",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ColorShopPrimary,
                        modifier = Modifier.clickable { showOnlyUnpaidDriverDeliveries = false }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Línea de Tiempo Vertical de Envíos / Empty State Expresivo y Amigable
        if (displayedRecords.isEmpty()) {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 3.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Contenedor circular con ilustración/emoji destacado
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(ColorShopSurfaceHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when {
                                searchQuery.isNotBlank() -> "🔍"
                                showOnlyUnpaidDriverDeliveries -> "🎉"
                                else -> "🛵"
                            },
                            fontSize = 36.sp
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val title = when {
                            searchQuery.isNotBlank() -> "No encontramos pedidos para \"$searchQuery\""
                            showOnlyUnpaidDriverDeliveries -> "¡Cuentas al día con motorizados!"
                            else -> "Sin envíos registrados en esta fecha"
                        }

                        val subtitle = when {
                            searchQuery.isNotBlank() -> "Intenta buscar por otro nombre, teléfono o código #recibo, o limpia la barra de búsqueda."
                            showOnlyUnpaidDriverDeliveries -> "Todas las transferencias y cobros con datáfono están liquidados al 100%."
                            selectedDateIso == todayIso -> "Aún no se han generado envíos el día de hoy. ¡Crea una cotización para registrar la primera entrega!"
                            else -> "No hay pedidos guardados en $selectedDateLabel. Consulta otras fechas desde el calendario superior."
                        }

                        Text(
                            text = title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ColorShopDeepPlum,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = subtitle,
                            fontSize = 12.sp,
                            color = ColorShopMutedPlum,
                            textAlign = TextAlign.Center,
                            lineHeight = 17.sp,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }

                    // Botones de acción según el estado
                    when {
                        searchQuery.isNotBlank() -> {
                            Surface(
                                color = ColorShopSurfaceHigh,
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .height(42.dp)
                                    .clickable { searchQuery = "" }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 18.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null,
                                        tint = ColorShopPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Limpiar búsqueda",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ColorShopPrimary
                                    )
                                }
                            }
                        }
                        showOnlyUnpaidDriverDeliveries -> {
                            Surface(
                                color = ColorShopSurfaceHigh,
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .height(42.dp)
                                    .clickable { showOnlyUnpaidDriverDeliveries = false }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 18.dp)
                                ) {
                                    Text(
                                        text = "Ver vista normal por fecha",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ColorShopPrimary
                                    )
                                }
                            }
                        }
                        else -> {
                            Button(
                                onClick = {
                                    onNavigateToQuote?.invoke()
                                },
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(containerColor = ColorShopPrimary),
                                modifier = Modifier
                                    .height(46.dp)
                                    .shadow(4.dp, RoundedCornerShape(50), ambientColor = ColorShopPrimary, spotColor = ColorShopPrimary)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "+ Nueva Cotización",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                displayedRecords.forEach { record ->
                    TimelineHistoryItem(
                        record = record,
                        config = config,
                        onSelectInMap = { onSelectRecord(record) },
                        onUpdateStatus = { newStatus -> onUpdateOrderStatus(record.id, newStatus) },
                        onRequestCancel = { recordPendingCancel = record },
                        onToggleDriverPaid = { onToggleDriverPaid(record.id) },
                        onRequestDelete = { recordPendingDelete = record }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(95.dp))
    }
}

@Composable
private fun HistoryFilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) ColorShopPrimary else Color.White,
        shape = RoundedCornerShape(50),
        shadowElevation = if (selected) 4.dp else 1.dp,
        modifier = Modifier
            .heightIn(min = 36.dp)
            .clickable { onClick() }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) Color.White else ColorShopMutedPlum
            )
        }
    }
}

@Composable
private fun CompactFilterBadge(
    text: String,
    onRemove: () -> Unit
) {
    Surface(
        color = ColorShopSurfaceHigh,
        shape = RoundedCornerShape(50),
        modifier = Modifier.clickable { onRemove() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = text,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = ColorShopPrimary
            )
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Quitar filtro",
                tint = ColorShopPrimary,
                modifier = Modifier.size(10.dp)
            )
        }
    }
}

@Composable
private fun TimelineHistoryItem(
    record: SavedQuoteRecord,
    config: StoreAndPricingConfig = StoreAndPricingConfig(),
    onSelectInMap: () -> Unit,
    onUpdateStatus: (OrderStatus) -> Unit = {},
    onRequestCancel: () -> Unit = {},
    onToggleDriverPaid: () -> Unit = {},
    onRequestDelete: () -> Unit
) {
    val context = LocalContext.current
    var expandedNotes by remember { mutableStateOf(false) }
    var showFullPhotoDialog by remember { mutableStateOf(false) }

    val timeParts = record.timestamp.split(" ")
    val hourPart = timeParts.getOrNull(0) ?: record.timestamp
    val amPmPart = timeParts.getOrNull(1) ?: ""

    val receiptBitmap = remember(record.receiptPhotoUri) {
        if (record.receiptPhotoUri.isNotBlank() && File(record.receiptPhotoUri).exists()) {
            BitmapFactory.decodeFile(record.receiptPhotoUri)
        } else {
            null
        }
    }

    if (showFullPhotoDialog && receiptBitmap != null) {
        Dialog(onDismissRequest = { showFullPhotoDialog = false }) {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(22.dp),
                shadowElevation = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Comprobante / Respaldo (${record.receiptId})",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ColorShopDeepPlum
                        )
                        IconButton(
                            onClick = { showFullPhotoDialog = false },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = ColorShopDeepPlum
                            )
                        }
                    }
                    Image(
                        bitmap = receiptBitmap.asImageBitmap(),
                        contentDescription = "Foto de respaldo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                }
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Columna Izquierda: Nodo Circular + Hora + Línea Vertical
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(42.dp)
                .fillMaxHeight()
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .shadow(3.dp, CircleShape, ambientColor = ColorShopPrimary)
                    .clip(CircleShape)
                    .background(ColorShopSurfaceHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (record.orderStatus) {
                        OrderStatus.EN_RUTA -> Icons.AutoMirrored.Filled.DirectionsBike
                        OrderStatus.ENTREGADO -> Icons.Default.CheckCircle
                        OrderStatus.CANCELADO -> Icons.Default.Close
                    },
                    contentDescription = null,
                    tint = when (record.orderStatus) {
                        OrderStatus.EN_RUTA -> Color(0xFFE65100)
                        OrderStatus.ENTREGADO -> Color(0xFF2E7D32)
                        OrderStatus.CANCELADO -> Color(0xFFC62828)
                    },
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = hourPart,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = ColorShopMutedPlum
            )
            if (amPmPart.isNotEmpty()) {
                Text(
                    text = amPmPart,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ColorShopMutedPlum.copy(alpha = 0.7f)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(ColorShopSurfaceHigh)
            )
        }

        // Tarjeta Derecha del Envío
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(20.dp),
            shadowElevation = 5.dp,
            modifier = Modifier.weight(1f)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // FILA 1: Estado del Pedido (Pill) y Botones de Cambio de Estado
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val statusBg = when (record.orderStatus) {
                        OrderStatus.EN_RUTA -> Color(0xFFFFF3E0)
                        OrderStatus.ENTREGADO -> Color(0xFFE8F5E9)
                        OrderStatus.CANCELADO -> Color(0xFFFFEBEE)
                    }
                    val statusFg = when (record.orderStatus) {
                        OrderStatus.EN_RUTA -> Color(0xFFE65100)
                        OrderStatus.ENTREGADO -> Color(0xFF2E7D32)
                        OrderStatus.CANCELADO -> Color(0xFFC62828)
                    }

                    Surface(
                        color = statusBg,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.border(1.dp, statusFg.copy(alpha = 0.4f), RoundedCornerShape(50))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(record.orderStatus.emoji, fontSize = 11.sp)
                            Text(
                                text = record.orderStatus.label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = statusFg
                            )
                        }
                    }

                    // Acciones rápidas de cambio de estado
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when (record.orderStatus) {
                            OrderStatus.EN_RUTA -> {
                                Surface(
                                    color = Color(0xFFE8F5E9),
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier
                                        .heightIn(min = 38.dp)
                                        .clickable { onUpdateStatus(OrderStatus.ENTREGADO) }
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = "Entregado ✅",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2E7D32)
                                        )
                                    }
                                }
                                Surface(
                                    color = Color(0xFFFFEBEE),
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier
                                        .heightIn(min = 38.dp)
                                        .clickable { onRequestCancel() }
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = "Cancelar ❌",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFC62828)
                                        )
                                    }
                                }
                            }
                            OrderStatus.ENTREGADO -> {
                                Surface(
                                    color = ColorShopSurfaceLow,
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier
                                        .heightIn(min = 38.dp)
                                        .clickable { onUpdateStatus(OrderStatus.EN_RUTA) }
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = "Reabrir 🛵",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ColorShopPrimary
                                        )
                                    }
                                }
                            }
                            OrderStatus.CANCELADO -> {
                                Surface(
                                    color = ColorShopSurfaceLow,
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier
                                        .heightIn(min = 38.dp)
                                        .clickable { onUpdateStatus(OrderStatus.EN_RUTA) }
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = "Reactivar 🛵",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ColorShopPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Fila superior: Repartidor | Tarifa y Totales
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.DirectionsBike,
                                contentDescription = null,
                                tint = ColorShopPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Repartidor",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ColorShopMutedPlum
                            )
                        }
                        Text(
                            text = record.courierName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ColorShopDeepPlum
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        if (record.productAmount > 0.0) {
                            Text(
                                text = "Gran Total",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ColorShopMutedPlum
                            )
                            Text(
                                text = String.format(Locale.US, "%s %.2f", record.currencySymbol, record.totalAmount),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ColorShopPrimary
                            )
                            Text(
                                text = "Prod: ${record.currencySymbol}${record.productAmount.toInt()} + Env: ${record.currencySymbol}${record.deliveryFee.toInt()}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorShopMutedPlum
                            )
                        } else {
                            Text(
                                text = "Tarifa Delivery",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ColorShopMutedPlum
                            )
                            Text(
                                text = String.format(Locale.US, "%s %.2f", record.currencySymbol, record.totalAmount),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ColorShopPrimary
                            )
                        }
                    }
                }

                // Detalle de Liquidación / Cuentas del Conductor (si existe)
                if (record.driverSettlementNote.isNotBlank()) {
                    Surface(
                        color = ColorShopSurfaceLow.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, ColorShopPrimary.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                    ) {
                        Text(
                            text = record.driverSettlementNote,
                            fontSize = 10.sp,
                            color = ColorShopDeepPlum,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }

                // Caja de método de pago y detalle
                Surface(
                    color = ColorShopSurfaceLow.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val (badgeText, badgeBgColor, badgeTextColor) = when (record.paymentMethod) {
                            PaymentMethodType.TRANSFER -> {
                                if (record.isPaymentPendingOnDelivery) {
                                    Triple("Transferencia • Al recibir ⏳", Color(0xFFFFEBD8), Color(0xFFD84315))
                                } else {
                                    Triple("Transferencia • Acreditada ✅", ColorShopTertiaryFixed, ColorShopTertiaryBlue)
                                }
                            }
                            PaymentMethodType.POS -> {
                                if (record.isPaymentPendingOnDelivery) {
                                    Triple("POS • Cobrar al recibir 💳", Color(0xFFFFEBD8), Color(0xFFD84315))
                                } else {
                                    Triple("POS • Cobrado ✅", Color(0xFFE8F5E9), Color(0xFF2E7D32))
                                }
                            }
                            PaymentMethodType.CASH -> {
                                Triple("Efectivo 💵", ColorShopSurfaceHigh, ColorShopPrimary)
                            }
                            PaymentMethodType.MIXED -> {
                                Triple("Pago Mixto 💵+🏦", Color(0xFFF3E5F5), ColorShopPrimary)
                            }
                        }
                        Surface(
                            color = badgeBgColor,
                            shape = RoundedCornerShape(50)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = when (record.paymentMethod) {
                                        PaymentMethodType.CASH, PaymentMethodType.MIXED -> Icons.Default.Payments
                                        PaymentMethodType.TRANSFER -> Icons.Default.AccountBalance
                                        PaymentMethodType.POS -> Icons.Default.PointOfSale
                                    },
                                    contentDescription = null,
                                    tint = badgeTextColor,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = badgeText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeTextColor
                                )
                            }
                        }

                        Text(
                            text = record.paymentDetail,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorShopPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Estado de Liquidación con el Motorizado (Efectivo auto-descontado vs Mixto vs Transferencia/POS pendiente o pagado)
                if (record.paymentMethod == PaymentMethodType.CASH) {
                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF2E7D32).copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "💵 Tarifa auto-descontada en efectivo",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B5E20)
                                )
                                Text(
                                    text = "El motorizado cobró ${record.currencySymbol}${record.totalAmount.toInt()} en mano y retuvo su tarifa de ${record.currencySymbol}${record.deliveryFee.toInt()} (Liquidado).",
                                    fontSize = 10.sp,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                            Surface(
                                color = Color(0xFF2E7D32),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "Liquidado ✅",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                } else if (record.paymentMethod == PaymentMethodType.MIXED) {
                    if (record.driverPendingFromStore <= 0.009) {
                        // Caso A o B: El motorizado cubrió su tarifa completa con el efectivo cobrado
                        Surface(
                            color = Color(0xFFE8F5E9),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFF2E7D32).copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "💵 Envío cubierto con efectivo del Pago Mixto",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1B5E20)
                                    )
                                    val subText = if (record.storeCashOwedByDriver > 0.009) {
                                        "Cobró ${record.currencySymbol}${record.effectiveCashToCollect.toInt()} en efectivo, retuvo su envío (${record.currencySymbol}${record.deliveryFee.toInt()}) y debe depositar ${record.currencySymbol}${record.storeCashOwedByDriver.toInt()} a tienda."
                                    } else {
                                        "Cobró ${record.currencySymbol}${record.effectiveCashToCollect.toInt()} en efectivo cubriendo exacto su envío (${record.currencySymbol}${record.deliveryFee.toInt()}). Deposita ${record.currencySymbol}0 a tienda."
                                    }
                                    Text(
                                        text = subText,
                                        fontSize = 10.sp,
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                                Surface(
                                    color = Color(0xFF2E7D32),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = "Liquidado ✅",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        // Caso C: El efectivo cobrado no cubrió la totalidad del envío, tienda debe pagarle el resto
                        Surface(
                            color = if (record.isDriverPaid) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    if (record.isDriverPaid) Color(0xFF2E7D32).copy(alpha = 0.25f) else Color(0xFFE65100).copy(alpha = 0.35f),
                                    RoundedCornerShape(10.dp)
                                )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (record.isDriverPaid) {
                                            "✅ Saldo de envío pagado al motorizado (${record.currencySymbol}${record.driverPendingFromStore.toInt()})"
                                        } else {
                                            "⏳ Saldo de envío pendiente al motorizado (${record.currencySymbol}${record.driverPendingFromStore.toInt()})"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (record.isDriverPaid) Color(0xFF1B5E20) else Color(0xFFBF360C)
                                    )
                                    Text(
                                        text = "Retuvo ${record.currencySymbol}${record.driverCashRetained.toInt()} en efectivo de su envío (${record.currencySymbol}${record.deliveryFee.toInt()}). Tienda cubre el resto (${record.currencySymbol}${record.driverPendingFromStore.toInt()}).",
                                        fontSize = 10.sp,
                                        color = if (record.isDriverPaid) Color(0xFF2E7D32) else Color(0xFFE65100)
                                    )
                                }
                                Surface(
                                    color = if (record.isDriverPaid) ColorShopSurfaceHigh else Color(0xFFE65100),
                                    shape = RoundedCornerShape(50),
                                    shadowElevation = if (record.isDriverPaid) 0.dp else 2.dp,
                                    modifier = Modifier
                                        .heightIn(min = 38.dp)
                                        .clickable { onToggleDriverPaid() }
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = if (record.isDriverPaid) "Desmarcar ↩️" else "Marcar Pagado ✅",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (record.isDriverPaid) ColorShopPrimary else Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Transferencia bancaria o Datafono POS
                    Surface(
                        color = if (record.isDriverPaid) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                if (record.isDriverPaid) Color(0xFF2E7D32).copy(alpha = 0.25f) else Color(0xFFE65100).copy(alpha = 0.35f),
                                RoundedCornerShape(10.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (record.isDriverPaid) {
                                        "✅ Tarifa pagada al motorizado (${record.currencySymbol}${record.deliveryFee.toInt()})"
                                    } else {
                                        "⏳ Pendiente pagar al motorizado (${record.currencySymbol}${record.deliveryFee.toInt()})"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (record.isDriverPaid) Color(0xFF1B5E20) else Color(0xFFBF360C)
                                )
                                Text(
                                    text = if (record.isDriverPaid) {
                                        if (record.driverPaidAt.isNotBlank()) "Liquidado: ${record.driverPaidAt}" else "Pago completado por la tienda"
                                    } else {
                                        "Cobro vía ${record.paymentMethod.badgeLabel}. La tienda debe liquidar este saldo al motorizado."
                                    },
                                    fontSize = 10.sp,
                                    color = if (record.isDriverPaid) Color(0xFF2E7D32) else Color(0xFFE65100)
                                )
                            }
                            Surface(
                                color = if (record.isDriverPaid) ColorShopSurfaceHigh else Color(0xFFE65100),
                                shape = RoundedCornerShape(50),
                                shadowElevation = if (record.isDriverPaid) 0.dp else 2.dp,
                                modifier = Modifier
                                    .heightIn(min = 38.dp)
                                    .clickable { onToggleDriverPaid() }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = if (record.isDriverPaid) "Desmarcar ↩️" else "Marcar Pagado ✅",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (record.isDriverPaid) ColorShopPrimary else Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                // Cliente y Ubicación de destino
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(ColorShopSurfaceHigh),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = ColorShopPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Column {
                            Text(
                                text = record.customerNote,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorShopDeepPlum
                            )
                            Text(
                                text = record.customerPhone,
                                fontSize = 11.sp,
                                color = ColorShopMutedPlum
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(ColorShopSurfaceHigh),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = ColorShopTertiaryBlue,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Column {
                            Text(
                                text = record.destinationLabel,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = ColorShopDeepPlum
                            )
                            Text(
                                text = "${String.format(Locale.US, "%.1f km", record.distanceKm)} de distancia",
                                fontSize = 11.sp,
                                color = ColorShopMutedPlum
                            )
                        }
                    }

                    if (record.approximateDeliveryTime.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(ColorShopSurfaceHigh),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = ColorShopPrimary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Text(
                                text = "Hora aprox. de entrega: ${record.approximateDeliveryTime}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ColorShopDeepPlum
                            )
                        }
                    }
                }

                // Acordeón desplegable: Ver Comprobante y Notas
                Surface(
                    color = ColorShopSurfaceLow.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedNotes = !expandedNotes }
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
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
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = ColorShopPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Ver Comprobante y Notas (${record.receiptId})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorShopPrimary
                                )
                            }
                            Icon(
                                imageVector = if (expandedNotes) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = ColorShopPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        AnimatedVisibility(visible = expandedNotes) {
                            Column(
                                modifier = Modifier.padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                HorizontalDivider(color = ColorShopSurfaceHigh)

                                val coord = GeoCoordinate(record.destinationLat, record.destinationLng)

                                Text(
                                    text = "📍 Ubicación de destino: ${record.destinationLabel}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ColorShopDeepPlum
                                )

                                if (record.orderComments.isNotBlank()) {
                                    Text(
                                        text = "📝 Comentarios del pedido: ${record.orderComments}",
                                        fontSize = 12.sp,
                                        color = ColorShopDeepPlum
                                    )
                                }

                                if (record.approximateDeliveryTime.isNotBlank()) {
                                    Text(
                                        text = "🕒 Hora aproximada de entrega: ${record.approximateDeliveryTime}",
                                        fontSize = 12.sp,
                                        color = ColorShopDeepPlum
                                    )
                                }

                                Text(
                                    text = "💰 Detalle de tarifa: ${record.surchargesSummary}",
                                    fontSize = 11.sp,
                                    color = ColorShopMutedPlum
                                )

                                // Miniatura de foto de recibo / respaldo si existe
                                if (receiptBitmap != null) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "📷 Foto de respaldo adjunta (toque para ampliar):",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ColorShopPrimary
                                        )
                                        Image(
                                            bitmap = receiptBitmap.asImageBitmap(),
                                            contentDescription = "Comprobante adjunto",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(140.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .border(1.dp, ColorShopOutlineVariant, RoundedCornerShape(12.dp))
                                                .clickable { showFullPhotoDialog = true }
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = ColorShopSurfaceHigh,
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(38.dp)
                                            .clickable { onSelectInMap() }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Map,
                                                contentDescription = null,
                                                tint = ColorShopPrimary,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Mapa",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ColorShopPrimary
                                            )
                                        }
                                    }

                                    Surface(
                                        color = ColorShopPrimary,
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier
                                            .weight(1.1f)
                                            .height(38.dp)
                                            .clickable {
                                                val bitmap = ReceiptImageGenerator.generateRecordReceiptBitmap(record, config)
                                                val uri = ReceiptImageGenerator.saveBitmapToShareCache(
                                                    context,
                                                    bitmap,
                                                    "recibo_${record.receiptId.replace("#", "")}"
                                                )
                                                ReceiptImageGenerator.shareReceiptImage(
                                                    context,
                                                    uri,
                                                    "Comprobante de Entrega ${record.receiptId} • ColorShop Delivery"
                                                )
                                            }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ReceiptLong,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Recibo 🖼️",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }

                                    Surface(
                                        color = ColorShopSurfaceLow,
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(38.dp)
                                            .clickable {
                                                val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                                clip?.setPrimaryClip(
                                                    ClipData.newPlainText(
                                                        "Ubicación de destino ColorShop",
                                                        coord.toStaticGoogleMapsUrl()
                                                    )
                                                )
                                                Toast.makeText(
                                                    context,
                                                    "Enlace de ubicación copiado",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = null,
                                                tint = ColorShopDeepPlum,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Copiar",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ColorShopDeepPlum
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = onRequestDelete,
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Eliminar",
                                            tint = Color(0xFFB41340),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatDateIsoHuman(dateIso: String, todayIso: String): String {
    if (dateIso == todayIso) {
        return runCatching {
            val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateIso) ?: Date()
            SimpleDateFormat("'Hoy,' dd MMMM", Locale("es", "NI")).format(parsed)
        }.getOrDefault("Hoy")
    }
    return runCatching {
        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateIso) ?: return dateIso
        SimpleDateFormat("EEE, dd MMM yyyy", Locale("es", "NI")).format(parsed)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "NI")) else it.toString() }
    }.getOrDefault(dateIso)
}

private fun formatDateIsoShort(dateIso: String, todayIso: String): String {
    if (dateIso == todayIso) return "Hoy"
    return runCatching {
        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateIso) ?: return dateIso
        SimpleDateFormat("dd MMM", Locale("es", "NI")).format(parsed)
    }.getOrDefault(dateIso)
}

private fun shiftDateIsoByDays(dateIso: String, deltaDays: Int): String {
    return runCatching {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val parsed = sdf.parse(dateIso) ?: Date()
        val cal = Calendar.getInstance()
        cal.time = parsed
        cal.add(Calendar.DAY_OF_MONTH, deltaDays)
        sdf.format(cal.time)
    }.getOrDefault(dateIso)
}

private fun buildDailyReportText(
    records: List<SavedQuoteRecord>,
    currencySymbol: String,
    courierFilter: String?,
    dateLabel: String
): String {
    val sb = StringBuilder()
    sb.appendLine("🦋 *REPORTE DE ENVÍOS - COLORSHOP DELIVERY*")
    sb.appendLine("Fecha: $dateLabel")
    sb.appendLine("Repartidor: ${courierFilter ?: "Todos los repartidores"}")
    sb.appendLine("Total Envíos: ${records.size}")
    sb.appendLine("Monto Acumulado: $currencySymbol ${records.sumOf { it.totalAmount }.toInt()}")
    sb.appendLine("Kilometraje Total: ${String.format(Locale.US, "%.1f km", records.sumOf { it.distanceKm })}")
    sb.appendLine("────────────────────")
    records.forEachIndexed { idx, r ->
        sb.appendLine("${idx + 1}. [${r.timestamp}] ${r.customerNote} (${r.courierName}) -> $currencySymbol ${r.totalAmount.toInt()} (${String.format(Locale.US, "%.1f km", r.distanceKm)})")
    }
    return sb.toString()
}
