package com.rapiflete.delivery.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.FileProvider
import com.rapiflete.delivery.model.CourierDriver
import com.rapiflete.delivery.model.FrozenDestination
import com.rapiflete.delivery.model.OrderStatus
import com.rapiflete.delivery.model.PaymentMethodType
import com.rapiflete.delivery.model.QuoteBreakdown
import com.rapiflete.delivery.model.SavedQuoteRecord
import com.rapiflete.delivery.model.StoreAndPricingConfig
import com.rapiflete.delivery.model.TransferBankType
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReceiptImageGenerator {

    private const val BITMAP_WIDTH = 1080

    private val COLOR_PRIMARY = Color.parseColor("#A51D89")      // ColorShop Magenta
    private val COLOR_PRIMARY_DARK = Color.parseColor("#5A0B4B") // Deep Plum
    private val COLOR_PRIMARY_LIGHT = Color.parseColor("#FFEBF3")// Soft Pink
    private val COLOR_BG = Color.parseColor("#FBF8F9")           // Surface Tint
    private val COLOR_CARD = Color.WHITE
    private val COLOR_TEXT_MAIN = Color.parseColor("#2B0E23")    // Deep Plum text
    private val COLOR_TEXT_MUTED = Color.parseColor("#805A74")   // Muted text
    private val COLOR_BORDER = Color.parseColor("#EED8E4")       // Border line
    private val COLOR_GREEN = Color.parseColor("#2E7D32")        // Success
    private val COLOR_GREEN_BG = Color.parseColor("#E8F5E9")
    private val COLOR_ORANGE = Color.parseColor("#E65100")       // Warning/En Ruta
    private val COLOR_ORANGE_BG = Color.parseColor("#FFF3E0")
    private val COLOR_RED = Color.parseColor("#C62828")          // Canceled
    private val COLOR_RED_BG = Color.parseColor("#FFEBEE")

    /**
     * Genera un recibo oficial digital en formato de imagen Bitmap para una cotización en curso.
     */
    fun generateCustomerReceiptBitmap(
        config: StoreAndPricingConfig,
        destination: FrozenDestination,
        quote: QuoteBreakdown,
        customerNote: String,
        customerPhone: String = "",
        orderComments: String = "",
        approximateDeliveryTime: String = "",
        courier: CourierDriver? = null,
        paymentMethod: PaymentMethodType = PaymentMethodType.CASH,
        transferBank: TransferBankType = TransferBankType.BAC,
        productAmount: Double = 0.0,
        orderStatus: OrderStatus = OrderStatus.EN_RUTA
    ): Bitmap {
        val cur = config.currencySymbol
        val deliveryFee = quote.effectiveTotal
        val grandTotal = productAmount + deliveryFee
        val now = SimpleDateFormat("dd/MM/yyyy • hh:mm a", Locale.US).format(Date())
        val receiptNumber = "#CS-${(10000..99999).random()}"

        return renderReceiptBitmap(
            title = "RECIBO OFICIAL DE ENTREGA",
            subtitle = "ColorShop Managua • Envío a Domicilio",
            receiptNumber = receiptNumber,
            dateString = now,
            orderStatus = orderStatus,
            customerName = customerNote.ifBlank { "Cliente General" },
            customerPhone = customerPhone,
            destinationAddress = destination.label,
            distanceKm = quote.effectiveDistanceKm,
            approxDeliveryTime = approximateDeliveryTime,
            orderComments = orderComments,
            courierName = if (courier != null && courier.includeNameInCustomerMessage) courier.name else "Walter",
            courierPhone = if (courier != null && courier.includePhoneInCustomerMessage) courier.phone else "",
            paymentMethod = paymentMethod,
            transferBankName = transferBank.displayName,
            productAmount = productAmount,
            deliveryFee = deliveryFee,
            grandTotal = grandTotal,
            currencySymbol = cur,
            isDriverPaid = paymentMethod == PaymentMethodType.CASH,
            driverPaidAt = if (paymentMethod == PaymentMethodType.CASH) "Auto-descontado en efectivo" else "",
            scheduleInfo = RoutingAndFareCalculator.buildScheduleSummaryLine(config)
        )
    }

    /**
     * Genera el recibo digital para un registro ya guardado en el historial.
     */
    fun generateRecordReceiptBitmap(
        record: SavedQuoteRecord,
        config: StoreAndPricingConfig
    ): Bitmap {
        val cur = record.currencySymbol.ifBlank { config.currencySymbol }
        val grandTotal = if (record.productAmount > 0) record.totalAmount else record.deliveryFee.coerceAtLeast(record.totalAmount)

        return renderReceiptBitmap(
            title = "COMPROBANTE DE ENVÍO",
            subtitle = "ColorShop Managua • Registro de Entrega",
            receiptNumber = record.receiptId,
            dateString = "${record.dateIso} • ${record.timestamp}",
            orderStatus = record.orderStatus,
            customerName = record.customerNote.ifBlank { "Cliente ColorShop" },
            customerPhone = record.customerPhone,
            destinationAddress = record.destinationLabel,
            distanceKm = record.distanceKm,
            approxDeliveryTime = record.approximateDeliveryTime,
            orderComments = record.orderComments,
            courierName = record.courierName,
            courierPhone = record.courierPhone,
            paymentMethod = record.paymentMethod,
            transferBankName = record.transferBank,
            productAmount = record.productAmount,
            deliveryFee = if (record.deliveryFee > 0) record.deliveryFee else (if (record.productAmount > 0) record.totalAmount - record.productAmount else record.totalAmount),
            grandTotal = record.totalAmount,
            currencySymbol = cur,
            isDriverPaid = record.isDriverPaid,
            driverPaidAt = record.driverPaidAt,
            scheduleInfo = RoutingAndFareCalculator.buildScheduleSummaryLine(config)
        )
    }

    /**
     * Genera un informe gráfico de corte de cuentas de un motorizado.
     */
    fun generateDriverSettlementReportBitmap(
        courierName: String,
        records: List<SavedQuoteRecord>,
        currencySymbol: String
    ): Bitmap {
        val totalDeliveries = records.size
        val cashRecords = records.filter { it.paymentMethod == PaymentMethodType.CASH }
        val electronicRecords = records.filter { it.paymentMethod != PaymentMethodType.CASH }

        val totalCashCollected = cashRecords.sumOf { it.totalAmount }
        val totalCashDriverDeducted = cashRecords.sumOf { it.deliveryFee }
        val totalCashStoreOwed = cashRecords.sumOf { it.productAmount }

        val electronicPaid = electronicRecords.filter { it.isDriverPaid }
        val electronicPending = electronicRecords.filter { !it.isDriverPaid }

        val totalElectronicPaidToDriver = electronicPaid.sumOf { it.deliveryFee }
        val totalElectronicPendingToDriver = electronicPending.sumOf { it.deliveryFee }

        val height = 1500
        val bitmap = Bitmap.createBitmap(BITMAP_WIDTH, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(COLOR_BG)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Header
        paint.color = COLOR_PRIMARY
        canvas.drawRect(0f, 0f, BITMAP_WIDTH.toFloat(), 180f, paint)

        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 42f
        canvas.drawText("COLORSHOP DELIVERY", 60f, 85f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 28f
        paint.color = Color.parseColor("#FFD9EC")
        canvas.drawText("Corte de Cuentas y Liquidación • Repartidor: $courierName", 60f, 135f, paint)

        var y = 220f

        // Card 1: Balance General
        val cardRect = RectF(50f, y, BITMAP_WIDTH - 50f, y + 360f)
        paint.color = COLOR_CARD
        canvas.drawRoundRect(cardRect, 28f, 28f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        paint.color = COLOR_BORDER
        canvas.drawRoundRect(cardRect, 28f, 28f, paint)
        paint.style = Paint.Style.FILL

        paint.color = COLOR_PRIMARY_DARK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 32f
        canvas.drawText("Resumen General de Entregas ($totalDeliveries pedidos)", 80f, y + 60f, paint)

        paint.textSize = 26f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = COLOR_TEXT_MUTED
        canvas.drawText("• Envíos en Efectivo (Auto-descontados por el motorizado):", 80f, y + 120f, paint)
        paint.color = COLOR_TEXT_MAIN
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("${cashRecords.size} envíos • Tarifa ganada: $currencySymbol ${totalCashDriverDeducted.toInt()}", 80f, y + 160f, paint)

        paint.color = COLOR_TEXT_MUTED
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("• Envíos por Transferencia / POS (Pago a cargo de ColorShop):", 80f, y + 220f, paint)
        paint.color = COLOR_TEXT_MAIN
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("${electronicRecords.size} envíos • Pagado: $currencySymbol ${totalElectronicPaidToDriver.toInt()} • Pendiente: $currencySymbol ${totalElectronicPendingToDriver.toInt()}", 80f, y + 260f, paint)

        y += 400f

        // Card 2: Monto Pendiente por Pagar al Motorizado (Destacado)
        val pendingCardRect = RectF(50f, y, BITMAP_WIDTH - 50f, y + 260f)
        paint.color = if (totalElectronicPendingToDriver > 0) COLOR_ORANGE_BG else COLOR_GREEN_BG
        canvas.drawRoundRect(pendingCardRect, 28f, 28f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        paint.color = if (totalElectronicPendingToDriver > 0) COLOR_ORANGE else COLOR_GREEN
        canvas.drawRoundRect(pendingCardRect, 28f, 28f, paint)
        paint.style = Paint.Style.FILL

        paint.color = if (totalElectronicPendingToDriver > 0) COLOR_ORANGE else COLOR_GREEN
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 30f
        canvas.drawText(
            if (totalElectronicPendingToDriver > 0) "SALDO PENDIENTE A PAGAR AL MOTORIZADO:" else "CUENTAS AL DÍA CON EL MOTORIZADO",
            80f,
            y + 65f,
            paint
        )

        paint.textSize = 62f
        canvas.drawText("$currencySymbol ${totalElectronicPendingToDriver.toInt()}", 80f, y + 150f, paint)

        paint.textSize = 24f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = COLOR_TEXT_MAIN
        val notePending = if (totalElectronicPendingToDriver > 0) {
            "Corresponde a ${electronicPending.size} envíos de transferencias/POS aún no liquidados."
        } else {
            "Todos los envíos por transferencia/POS han sido pagados al motorizado."
        }
        canvas.drawText(notePending, 80f, y + 210f, paint)

        y += 300f

        // Card 3: Saldo por depositar a tienda en Efectivo
        val depositCardRect = RectF(50f, y, BITMAP_WIDTH - 50f, y + 240f)
        paint.color = COLOR_CARD
        canvas.drawRoundRect(depositCardRect, 28f, 28f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        paint.color = COLOR_BORDER
        canvas.drawRoundRect(depositCardRect, 28f, 28f, paint)
        paint.style = Paint.Style.FILL

        paint.color = COLOR_PRIMARY_DARK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 28f
        canvas.drawText("Depósito a Tienda por Mercadería (Efectivo cobrado):", 80f, y + 60f, paint)

        paint.textSize = 50f
        paint.color = COLOR_PRIMARY
        canvas.drawText("$currencySymbol ${totalCashStoreOwed.toInt()}", 80f, y + 135f, paint)

        paint.textSize = 24f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = COLOR_TEXT_MUTED
        canvas.drawText("El motorizado ya retuvo su tarifa de envío y debe depositar este monto a ColorShop.", 80f, y + 190f, paint)

        // Footer
        val footerDate = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.US).format(Date())
        paint.textSize = 22f
        paint.color = COLOR_TEXT_MUTED
        canvas.drawText("Generado por ColorShop Delivery • $footerDate", 60f, height - 40f, paint)

        return bitmap
    }

    private fun renderReceiptBitmap(
        title: String,
        subtitle: String,
        receiptNumber: String,
        dateString: String,
        orderStatus: OrderStatus,
        customerName: String,
        customerPhone: String,
        destinationAddress: String,
        distanceKm: Double,
        approxDeliveryTime: String,
        orderComments: String,
        courierName: String,
        courierPhone: String,
        paymentMethod: PaymentMethodType,
        transferBankName: String,
        productAmount: Double,
        deliveryFee: Double,
        grandTotal: Double,
        currencySymbol: String,
        isDriverPaid: Boolean,
        driverPaidAt: String,
        scheduleInfo: String
    ): Bitmap {
        val totalHeight = 1680
        val bitmap = Bitmap.createBitmap(BITMAP_WIDTH, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(COLOR_BG)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Header Background
        paint.color = COLOR_PRIMARY
        canvas.drawRect(0f, 0f, BITMAP_WIDTH.toFloat(), 200f, paint)

        // Brand Title
        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 44f
        canvas.drawText("COLORSHOP DELIVERY", 60f, 85f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 26f
        paint.color = Color.parseColor("#FFD9EC")
        canvas.drawText(subtitle, 60f, 130f, paint)

        // Receipt Tag Pill Top Right
        val tagRect = RectF(BITMAP_WIDTH - 340f, 55f, BITMAP_WIDTH - 60f, 125f)
        paint.color = Color.WHITE.copy(alpha = 0.25f)
        canvas.drawRoundRect(tagRect, 35f, 35f, paint)
        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 28f
        canvas.drawText(receiptNumber, BITMAP_WIDTH - 305f, 102f, paint)

        var y = 240f

        // Card 1: Estado del Pedido y Fecha
        val statusRect = RectF(50f, y, BITMAP_WIDTH - 50f, y + 110f)
        paint.color = COLOR_CARD
        canvas.drawRoundRect(statusRect, 24f, 24f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.5f
        paint.color = COLOR_BORDER
        canvas.drawRoundRect(statusRect, 24f, 24f, paint)
        paint.style = Paint.Style.FILL

        // Status Badge Pill
        val badgeColor = when (orderStatus) {
            OrderStatus.EN_RUTA -> COLOR_ORANGE
            OrderStatus.ENTREGADO -> COLOR_GREEN
            OrderStatus.CANCELADO -> COLOR_RED
        }
        val badgeBg = when (orderStatus) {
            OrderStatus.EN_RUTA -> COLOR_ORANGE_BG
            OrderStatus.ENTREGADO -> COLOR_GREEN_BG
            OrderStatus.CANCELADO -> COLOR_RED_BG
        }

        val pillRect = RectF(80f, y + 25f, 360f, y + 85f)
        paint.color = badgeBg
        canvas.drawRoundRect(pillRect, 30f, 30f, paint)
        paint.color = badgeColor
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 28f
        canvas.drawText("${orderStatus.emoji} ${orderStatus.label.uppercase(Locale.getDefault())}", 115f, y + 67f, paint)

        paint.color = COLOR_TEXT_MUTED
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 24f
        canvas.drawText("Fecha: $dateString", BITMAP_WIDTH - 480f, y + 67f, paint)

        y += 140f

        // Card 2: Destinatario y Entrega
        val infoRect = RectF(50f, y, BITMAP_WIDTH - 50f, y + 360f)
        paint.color = COLOR_CARD
        canvas.drawRoundRect(infoRect, 24f, 24f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.5f
        paint.color = COLOR_BORDER
        canvas.drawRoundRect(infoRect, 24f, 24f, paint)
        paint.style = Paint.Style.FILL

        paint.color = COLOR_PRIMARY_DARK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 30f
        canvas.drawText("Datos de Entrega y Cliente", 80f, y + 55f, paint)

        paint.textSize = 26f
        paint.color = COLOR_TEXT_MUTED
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Cliente:", 80f, y + 105f, paint)
        paint.color = COLOR_TEXT_MAIN
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val customerDisplay = if (customerPhone.isNotBlank()) "$customerName ($customerPhone)" else customerName
        canvas.drawText(customerDisplay, 220f, y + 105f, paint)

        paint.color = COLOR_TEXT_MUTED
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Dirección:", 80f, y + 155f, paint)
        paint.color = COLOR_TEXT_MAIN
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val truncatedDest = if (destinationAddress.length > 52) destinationAddress.take(50) + "..." else destinationAddress
        canvas.drawText(truncatedDest, 220f, y + 155f, paint)

        paint.color = COLOR_TEXT_MUTED
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Distancia:", 80f, y + 205f, paint)
        paint.color = COLOR_TEXT_MAIN
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val timeExtra = if (approxDeliveryTime.isNotBlank()) " • Aprox: $approxDeliveryTime" else ""
        canvas.drawText(String.format(Locale.US, "%.2f km", distanceKm) + timeExtra, 220f, y + 205f, paint)

        paint.color = COLOR_TEXT_MUTED
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Motorizado:", 80f, y + 255f, paint)
        paint.color = COLOR_TEXT_MAIN
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val courierDisplay = if (courierPhone.isNotBlank()) "$courierName • 📞 $courierPhone" else courierName
        canvas.drawText(courierDisplay, 240f, y + 255f, paint)

        if (orderComments.isNotBlank()) {
            paint.color = COLOR_TEXT_MUTED
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("Notas:", 80f, y + 305f, paint)
            paint.color = COLOR_PRIMARY_DARK
            val cleanNotes = if (orderComments.length > 50) orderComments.take(48) + "..." else orderComments
            canvas.drawText(cleanNotes, 220f, y + 305f, paint)
        }

        y += 390f

        // Card 3: Desglose Financiero y Totales
        val financeRect = RectF(50f, y, BITMAP_WIDTH - 50f, y + 390f)
        paint.color = COLOR_CARD
        canvas.drawRoundRect(financeRect, 24f, 24f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.5f
        paint.color = COLOR_BORDER
        canvas.drawRoundRect(financeRect, 24f, 24f, paint)
        paint.style = Paint.Style.FILL

        paint.color = COLOR_PRIMARY_DARK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 30f
        canvas.drawText("Detalle de Cobro y Tarifa", 80f, y + 55f, paint)

        if (productAmount > 0) {
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.color = COLOR_TEXT_MUTED
            paint.textSize = 26f
            canvas.drawText("Mercadería / Productos:", 80f, y + 110f, paint)
            paint.color = COLOR_TEXT_MAIN
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("$currencySymbol ${String.format(Locale.US, "%.2f", productAmount)}", BITMAP_WIDTH - 280f, y + 110f, paint)
        }

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = COLOR_TEXT_MUTED
        paint.textSize = 26f
        canvas.drawText("Servicio de Envío a Domicilio:", 80f, y + 160f, paint)
        paint.color = COLOR_PRIMARY
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("$currencySymbol ${String.format(Locale.US, "%.2f", deliveryFee)}", BITMAP_WIDTH - 280f, y + 160f, paint)

        // Línea divisoria
        paint.strokeWidth = 2f
        paint.color = COLOR_BORDER
        canvas.drawLine(80f, y + 195f, BITMAP_WIDTH - 80f, y + 195f, paint)

        // Gran Total Box
        val grandBox = RectF(75f, y + 215f, BITMAP_WIDTH - 75f, y + 355f)
        paint.color = COLOR_PRIMARY_LIGHT
        canvas.drawRoundRect(grandBox, 18f, 18f, paint)

        paint.color = COLOR_PRIMARY_DARK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 30f
        canvas.drawText("GRAN TOTAL A PAGAR:", 100f, y + 295f, paint)

        paint.color = COLOR_PRIMARY
        paint.textSize = 54f
        canvas.drawText("$currencySymbol ${String.format(Locale.US, "%.2f", grandTotal)}", BITMAP_WIDTH - 390f, y + 300f, paint)

        y += 420f

        // Card 4: Método de Pago y Estado de Liquidación al Motorizado
        val settlementCardRect = RectF(50f, y, BITMAP_WIDTH - 50f, y + 260f)
        val isSettlementPaid = isDriverPaid || paymentMethod == PaymentMethodType.CASH
        paint.color = if (isSettlementPaid) COLOR_GREEN_BG else COLOR_ORANGE_BG
        canvas.drawRoundRect(settlementCardRect, 24f, 24f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.5f
        paint.color = if (isSettlementPaid) COLOR_GREEN else COLOR_ORANGE
        canvas.drawRoundRect(settlementCardRect, 24f, 24f, paint)
        paint.style = Paint.Style.FILL

        paint.color = if (isSettlementPaid) COLOR_GREEN else COLOR_ORANGE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 28f
        val paymentMethodLabel = when (paymentMethod) {
            PaymentMethodType.CASH -> "Forma de Pago: Efectivo contra entrega"
            PaymentMethodType.TRANSFER -> "Forma de Pago: Transferencia Bancaria ($transferBankName)"
            PaymentMethodType.POS -> "Forma de Pago: Datáfono / Tarjeta POS"
        }
        canvas.drawText(paymentMethodLabel, 80f, y + 60f, paint)

        paint.textSize = 26f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val driverStatusHeader = if (paymentMethod == PaymentMethodType.CASH) {
            "✅ PAGO AL MOTORIZADO: Auto-descontado en efectivo"
        } else if (isDriverPaid) {
            val paidDetail = if (driverPaidAt.isNotBlank()) " (Liquidado: $driverPaidAt)" else ""
            "✅ PAGO AL MOTORIZADO: Pagado por tienda$paidDetail"
        } else {
            "⏳ PAGO AL MOTORIZADO: Pendiente de liquidar por tienda ($currencySymbol ${deliveryFee.toInt()})"
        }
        canvas.drawText(driverStatusHeader, 80f, y + 115f, paint)

        paint.textSize = 22f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = COLOR_TEXT_MAIN
        val settlementAdvice = when (paymentMethod) {
            PaymentMethodType.CASH -> "El motorizado ya retuvo los $currencySymbol ${deliveryFee.toInt()} de su envío y liquidará los productos a la tienda."
            PaymentMethodType.TRANSFER -> if (isDriverPaid) "La tienda ya transfirió la tarifa de entrega al motorizado." else "ColorShop transferirá la tarifa de entrega al motorizado hoy o mañana."
            PaymentMethodType.POS -> if (isDriverPaid) "Tarifa de entrega pagada al motorizado." else "ColorShop pagará la tarifa de entrega al motorizado hoy o mañana."
        }
        canvas.drawText(settlementAdvice, 80f, y + 165f, paint)

        // Footer
        paint.color = COLOR_TEXT_MUTED
        paint.textSize = 22f
        if (scheduleInfo.isNotBlank()) {
            canvas.drawText("⏰ Horario: $scheduleInfo", 60f, totalHeight - 65f, paint)
        }
        canvas.drawText("ColorShop Delivery • Calidad y Rapidez en Managua, Nicaragua", 60f, totalHeight - 30f, paint)

        return bitmap
    }

    /**
     * Guarda el Bitmap generado en la memoria caché y retorna un URI compatible con FileProvider para compartir.
     */
    fun saveBitmapToShareCache(context: Context, bitmap: Bitmap, filenamePrefix: String): Uri {
        val cacheDir = File(context.cacheDir, "receipts").apply { mkdirs() }
        val file = File(cacheDir, "${filenamePrefix}_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    /**
     * Lanza el Intent para compartir el recibo de imagen por WhatsApp u otra app, garantizando
     * multiventana y tarea independiente.
     */
    fun shareReceiptImage(context: Context, imageUri: Uri, captionText: String) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            putExtra(Intent.EXTRA_TEXT, captionText)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        }
        val chooser = Intent.createChooser(sendIntent, "Compartir Recibo de Entrega").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    private fun Int.copy(alpha: Float): Int {
        val a = (alpha * 255).toInt().coerceIn(0, 255)
        return Color.argb(a, Color.red(this), Color.green(this), Color.blue(this))
    }
}
