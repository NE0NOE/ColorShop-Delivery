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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
    onClearAll: () -> Unit
) {
    val context = LocalContext.current
    val verticalScroll = rememberScrollState()

    val todayIso = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }
    var selectedDateIso by remember { mutableStateOf(todayIso) }
    var showDateSelectorPanel by remember { mutableStateOf(false) }

    var selectedPaymentFilter by remember { mutableStateOf("ALL") } // ALL, CASH, TRANSFER
    var selectedCourierName by remember { mutableStateOf<String?>(null) }
    var selectedStatusFilter by remember { mutableStateOf<OrderStatus?>(null) }
    var showOnlyUnpaidDriverDeliveries by remember { mutableStateOf(false) }

    // Envíos con pago electrónico no liquidados al motorizado (acumula hoy, ayer y días anteriores)
    // Los pagos en efectivo se excluyen porque el motorizado ya se los auto-descuenta de inmediato.
    val allUnpaidElectronicDeliveries = remember(records, selectedCourierName) {
        records.filter { rec ->
            !rec.isDriverPaid &&
            rec.paymentMethod != PaymentMethodType.CASH &&
            rec.orderStatus != OrderStatus.CANCELADO &&
            (selectedCourierName == null || rec.courierName.equals(selectedCourierName, ignoreCase = true))
        }
    }

    val totalOwedAllTime = remember(allUnpaidElectronicDeliveries) {
        allUnpaidElectronicDeliveries.sumOf { it.deliveryFee }
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

    val filteredRecords = remember(recordsForSelectedDate, selectedPaymentFilter, selectedCourierName, selectedStatusFilter) {
        recordsForSelectedDate.filter { rec ->
            val matchesPayment = when (selectedPaymentFilter) {
                "CASH" -> rec.paymentMethod == PaymentMethodType.CASH
                "TRANSFER" -> rec.paymentMethod == PaymentMethodType.TRANSFER
                else -> true
            }
            val matchesCourier = selectedCourierName == null ||
                rec.courierName.equals(selectedCourierName, ignoreCase = true)
            val matchesStatus = selectedStatusFilter == null || rec.orderStatus == selectedStatusFilter
            matchesPayment && matchesCourier && matchesStatus
        }
    }

    val displayedRecords = remember(showOnlyUnpaidDriverDeliveries, allUnpaidElectronicDeliveries, filteredRecords, selectedStatusFilter) {
        if (showOnlyUnpaidDriverDeliveries) {
            allUnpaidElectronicDeliveries.filter { rec ->
                val matchesStatus = selectedStatusFilter == null || rec.orderStatus == selectedStatusFilter
                matchesStatus
            }
        } else {
            filteredRecords
        }
    }

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
                    color = if (showDateSelectorPanel || selectedDateIso != todayIso) ColorShopPrimary else Color.White,
                    shape = CircleShape,
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .size(40.dp)
                        .clickable {
                            showDateSelectorPanel = !showDateSelectorPanel
                            openNativeDatePicker()
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Seleccionar fecha",
                            tint = if (showDateSelectorPanel || selectedDateIso != todayIso) Color.White else ColorShopPrimary,
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

        // Barra rápida de fechas con envíos (se despliega al tocar el calendario o cambiar de día)
        AnimatedVisibility(visible = showDateSelectorPanel || selectedDateIso != todayIso || availableDatesWithRecords.size > 1) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = ColorShopSurfaceHigh,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .height(32.dp)
                            .clickable { openNativeDatePicker() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = ColorShopPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Elegir fecha...",
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
                            shadowElevation = if (isSelected) 3.dp else 1.dp,
                            modifier = Modifier
                                .height(32.dp)
                                .clickable { selectedDateIso = dateIso }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 12.dp)
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
            }
        }

        // Tarjeta de Resumen Diario (3 cuadros tipo Bento de Stitch) con navegación entre días
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                .background(ColorShopPrimary)
                                .clickable { openNativeDatePicker() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalShipping,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column(
                            modifier = Modifier.clickable { openNativeDatePicker() }
                        ) {
                            Text(
                                text = selectedDateLabel,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorShopDeepPlum
                            )
                            Text(
                                text = if (selectedDateIso == todayIso) "Resumen del día actual" else "Toque para cambiar de fecha",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = ColorShopMutedPlum
                            )
                        }
                    }

                    // Controles para desplazarse al día anterior / siguiente / hoy
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            color = ColorShopSurfaceLow,
                            shape = CircleShape,
                            modifier = Modifier
                                .size(30.dp)
                                .clickable {
                                    selectedDateIso = shiftDateIsoByDays(selectedDateIso, -1)
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = "Día anterior",
                                    tint = ColorShopDeepPlum,
                                    modifier = Modifier.size(18.dp)
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
                                    text = "Ir a Hoy",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorShopPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }

                        Surface(
                            color = ColorShopSurfaceLow,
                            shape = CircleShape,
                            modifier = Modifier
                                .size(30.dp)
                                .clickable {
                                    selectedDateIso = shiftDateIsoByDays(selectedDateIso, 1)
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Día siguiente",
                                    tint = ColorShopDeepPlum,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Barra indicadora de filtro activo
                Surface(
                    color = ColorShopSurfaceLow.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
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
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Vista métricas:",
                                fontSize = 11.sp,
                                color = ColorShopMutedPlum
                            )
                            Text(
                                text = selectedCourierName ?: "Todos los repartidores",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorShopPrimary
                            )
                        }
                        if (selectedCourierName != null) {
                            Text(
                                text = "Ver todos",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorShopPrimary,
                                modifier = Modifier.clickable { selectedCourierName = null }
                            )
                        }
                    }
                }

                // 3 Cuadros Bento: Envíos | Total | Ruta
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Cuadro 1: Envíos
                    Surface(
                        color = ColorShopSurfaceLow.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(96.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ColorShopSurfaceHigh),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Inventory2,
                                        contentDescription = null,
                                        tint = ColorShopPrimary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Text(
                                    text = "Envíos",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ColorShopMutedPlum
                                )
                            }
                            Column {
                                Text(
                                    text = "${filteredRecords.size}",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ColorShopDeepPlum
                                )
                                Text(
                                    text = "Completados",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ColorShopPrimary
                                )
                            }
                        }
                    }

                    // Cuadro 2: Total (Sólido Magenta)
                    Surface(
                        color = ColorShopPrimary,
                        shape = RoundedCornerShape(18.dp),
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .weight(1f)
                            .height(96.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Payments,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Text(
                                    text = "Total",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                            Column {
                                Text(
                                    text = "$currencySymbol ${totalAmount.toInt()}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    maxLines = 1
                                )
                                Text(
                                    text = if (selectedDateIso == todayIso) "Generado hoy" else "En la fecha",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }

                    // Cuadro 3: Ruta (km)
                    Surface(
                        color = ColorShopSurfaceLow.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(96.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ColorShopSurfaceHigh),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NearMe,
                                        contentDescription = null,
                                        tint = ColorShopPrimary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Text(
                                    text = "Ruta",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ColorShopMutedPlum
                                )
                            }
                            Column {
                                Text(
                                    text = String.format(Locale.US, "%.1f km", totalKm),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ColorShopDeepPlum,
                                    maxLines = 1
                                )
                                Text(
                                    text = "Recorrido",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = ColorShopMutedPlum
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Recuadro Bento Destacado: Saldo Pendiente a Pagar al Motorizado (Hoy y Acumulado de Días Anteriores)
        Surface(
            color = if (totalOwedAllTime > 0) Color(0xFFFFF8E1) else Color(0xFFF1F8E9),
            shape = RoundedCornerShape(20.dp),
            shadowElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .border(
                    1.dp,
                    if (totalOwedAllTime > 0) Color(0xFFFFB300).copy(alpha = 0.7f) else Color(0xFF81C784).copy(alpha = 0.7f),
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
                                .background(if (totalOwedAllTime > 0) Color(0xFFFFB300) else Color(0xFF4CAF50)),
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
                            color = if (totalOwedAllTime > 0) Color(0xFFE65100) else Color(0xFF2E7D32)
                        )
                        Text(
                            text = if (totalOwedAllTime > 0) "Pendiente tienda" else "Al día ✅",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (totalOwedAllTime > 0) Color(0xFFE65100) else Color(0xFF2E7D32)
                        )
                    }
                }

                HorizontalDivider(color = if (totalOwedAllTime > 0) Color(0xFFFFE082) else Color(0xFFC8E6C9))

                // Desglose de saldos: acumulado de días anteriores vs saldo de hoy
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (totalOwedAllTime > 0) {
                        if (unpaidPastDays.isNotEmpty()) {
                            Text(
                                text = "⚠️ Acumulado de días anteriores: $currencySymbol ${unpaidPastDays.sumOf { it.deliveryFee }.toInt()} (${unpaidPastDays.size} envíos)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD84315)
                            )
                        }
                        Text(
                            text = "• Saldo pendiente de hoy: $currencySymbol ${unpaidToday.sumOf { it.deliveryFee }.toInt()} (${unpaidToday.size} envíos)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = ColorShopDeepPlum
                        )
                        Text(
                            text = "ℹ️ Los envíos en efectivo fueron auto-descontados por el motorizado (ya liquidados).",
                            fontSize = 10.sp,
                            color = ColorShopMutedPlum
                        )
                    } else {
                        Text(
                            text = "No hay tarifas de entrega pendientes de liquidar por transferencias ni POS con el motorizado.",
                            fontSize = 11.sp,
                            color = Color(0xFF2E7D32)
                        )
                    }
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

        Spacer(modifier = Modifier.height(14.dp))

        // Filas Horizontales de Chips de Filtro (Estado del Pedido, Método de cobro y Repartidores)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HistoryFilterPill(
                label = "Todos Estados (${recordsForSelectedDate.size})",
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

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = ColorShopSurfaceHigh.copy(alpha = 0.7f),
                shape = RoundedCornerShape(50)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.DirectionsBike,
                        contentDescription = null,
                        tint = ColorShopPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Repartidores:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorShopPrimary
                    )
                }
            }

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

        // Línea de Tiempo Vertical de Envíos
        if (displayedRecords.isEmpty()) {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(20.dp),
                shadowElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = null,
                        tint = ColorShopMutedPlum.copy(alpha = 0.6f),
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = if (showOnlyUnpaidDriverDeliveries) "No hay envíos pendientes de pago al motorizado" else "Sin envíos registrados en esta fecha",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorShopDeepPlum,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = if (showOnlyUnpaidDriverDeliveries) "Todas las transferencias y cobros con datáfono están al día." else "Los envíos de días anteriores están guardados en sus respectivas fechas. Usa el botón del calendario para consultarlos.",
                        fontSize = 11.sp,
                        color = ColorShopMutedPlum,
                        textAlign = TextAlign.Center
                    )
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
                        onToggleDriverPaid = { onToggleDriverPaid(record.id) },
                        onDelete = { onDeleteRecord(record.id) }
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
            .height(34.dp)
            .clickable { onClick() }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 14.dp)
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
private fun TimelineHistoryItem(
    record: SavedQuoteRecord,
    config: StoreAndPricingConfig = StoreAndPricingConfig(),
    onSelectInMap: () -> Unit,
    onUpdateStatus: (OrderStatus) -> Unit = {},
    onToggleDriverPaid: () -> Unit = {},
    onDelete: () -> Unit
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
                                    modifier = Modifier.clickable { onUpdateStatus(OrderStatus.ENTREGADO) }
                                ) {
                                    Text(
                                        text = "Entregado ✅",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Surface(
                                    color = Color(0xFFFFEBEE),
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.clickable { onUpdateStatus(OrderStatus.CANCELADO) }
                                ) {
                                    Text(
                                        text = "Cancelar ❌",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFC62828),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            OrderStatus.ENTREGADO -> {
                                Surface(
                                    color = ColorShopSurfaceLow,
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.clickable { onUpdateStatus(OrderStatus.EN_RUTA) }
                                ) {
                                    Text(
                                        text = "Reabrir 🛵",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ColorShopPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            OrderStatus.CANCELADO -> {
                                Surface(
                                    color = ColorShopSurfaceLow,
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.clickable { onUpdateStatus(OrderStatus.EN_RUTA) }
                                ) {
                                    Text(
                                        text = "Reactivar 🛵",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ColorShopPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
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
                        val isTransfer = record.paymentMethod == PaymentMethodType.TRANSFER
                        Surface(
                            color = if (isTransfer) ColorShopTertiaryFixed else ColorShopSurfaceHigh,
                            shape = RoundedCornerShape(50)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = when (record.paymentMethod) {
                                        PaymentMethodType.CASH -> Icons.Default.Payments
                                        PaymentMethodType.TRANSFER -> Icons.Default.AccountBalance
                                        PaymentMethodType.POS -> Icons.Default.PointOfSale
                                    },
                                    contentDescription = null,
                                    tint = if (isTransfer) ColorShopTertiaryBlue else ColorShopPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = if (isTransfer) "Transferencia ${record.transferBank.ifBlank { "BAC" }}" else record.paymentMethod.badgeLabel,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isTransfer) ColorShopTertiaryBlue else ColorShopPrimary
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

                // Estado de Liquidación con el Motorizado (Efectivo auto-descontado vs Transferencia/POS pendiente o pagado)
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
                                modifier = Modifier.clickable { onToggleDriverPaid() }
                            ) {
                                Text(
                                    text = if (record.isDriverPaid) "Desmarcar ↩️" else "Marcar Pagado ✅",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (record.isDriverPaid) ColorShopPrimary else Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
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
                                            .height(34.dp)
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
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "Mapa",
                                                fontSize = 11.sp,
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
                                            .height(34.dp)
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
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "Recibo 🖼️",
                                                fontSize = 11.sp,
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
                                            .height(34.dp)
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
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "Copiar",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ColorShopDeepPlum
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = onDelete,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Eliminar",
                                            tint = Color(0xFFB41340),
                                            modifier = Modifier.size(18.dp)
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
