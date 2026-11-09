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
import com.rapiflete.delivery.model.calculateMixedPaymentSettlement
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
    private val COLOR_BLUE = Color.parseColor("#2956B7")         // Tertiary Blue
    private val COLOR_BLUE_BG = Color.parseColor("#EBF2FF")      // Tertiary Fixed / Light Blue

    /**
     * Genera un código de recibo / factura numérico estructurado:
     * [Fecha AAAAMMDD] + [Número de Pedido correlativo 2 dígitos, ej. 01] + [Dígito del Motorizado, ej. 1..9 o 0]
     * Ejemplo: 20261105011 (o con prefijo opcional #20261105011)
     */
    fun generateReceiptCode(
        date: Date = Date(),
        orderNumber: Int = 1,
        courierIndex: Int = 0,
        withHashPrefix: Boolean = true
    ): String {
        val datePart = SimpleDateFormat("yyyyMMdd", Locale.US).format(date)
        val orderPart = String.format(Locale.US, "%02d", orderNumber.coerceAtLeast(1))
        val courierPart = courierIndex.coerceIn(0, 9).toString()
        val num = "$datePart$orderPart$courierPart"
        return if (withHashPrefix) "#$num" else num
    }

    /**
     * Genera un recibo oficial digital en formato de imagen Bitmap EXCLUSIVO para el CLIENTE.
     * No contiene notas internas de liquidación al motorizado ni frases de auto-descuento.
     * Adapta dinámicamente sus campos en base a la plantilla de texto activa.
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
        orderStatus: OrderStatus = OrderStatus.EN_RUTA,
        isPaymentPendingOnDelivery: Boolean = false,
        partialTransferAmount: Double = 0.0,
        customTemplate: String = config.customerMessageTemplate,
        receiptNumber: String? = null
    ): Bitmap {
        val cur = config.currencySymbol
        val deliveryFee = quote.effectiveTotal
        val grandTotal = productAmount + deliveryFee
        val now = SimpleDateFormat("dd/MM/yyyy • hh:mm a", Locale.US).format(Date())
        val effectiveReceiptNumber = receiptNumber ?: generateReceiptCode(
            date = Date(),
            orderNumber = 1,
            courierIndex = if (courier != null) 1 else 0
        )

        val cleanDest = cleanDestinationLabel(destination.label)

        return renderCustomerReceiptBitmap(
            storeName = config.storeName,
            receiptNumber = effectiveReceiptNumber,
            dateString = now,
            orderStatus = orderStatus,
            customerName = customerNote.ifBlank { "Cliente General" },
            customerPhone = customerPhone,
            destinationAddress = cleanDest,
            distanceKm = quote.effectiveDistanceKm,
            approxDeliveryTime = approximateDeliveryTime,
            orderComments = orderComments,
            courierName = if (courier != null && courier.includeNameInCustomerMessage) courier.name else "",
            courierPhone = if (courier != null && courier.includePhoneInCustomerMessage) courier.phone else "",
            paymentMethod = paymentMethod,
            transferBankName = transferBank.displayName,
            productAmount = productAmount,
            deliveryFee = deliveryFee,
            grandTotal = grandTotal,
            currencySymbol = cur,
            scheduleInfo = RoutingAndFareCalculator.buildScheduleSummaryLine(config),
            isPaymentPendingOnDelivery = isPaymentPendingOnDelivery,
            partialTransferAmount = partialTransferAmount,
            template = customTemplate
        )
    }

    /**
     * Genera la hoja de despacho digital en formato de imagen Bitmap EXCLUSIVA para el MOTORIZADO / REPARTIDOR.
     * Contiene las indicaciones de cobro al cliente, mercadería a liquidar y estado de liquidación de su envío.
     * Adapta dinámicamente sus campos en base a la plantilla de despacho activa.
     */
    fun generateDriverDispatchBitmap(
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
        orderStatus: OrderStatus = OrderStatus.EN_RUTA,
        isPaymentPendingOnDelivery: Boolean = false,
        partialTransferAmount: Double = 0.0,
        customTemplate: String = config.driverDispatchTemplate,
        dispatchNumber: String? = null
    ): Bitmap {
        val cur = config.currencySymbol
        val deliveryFee = quote.effectiveTotal
        val grandTotal = productAmount + deliveryFee
        val now = SimpleDateFormat("dd/MM/yyyy • hh:mm a", Locale.US).format(Date())
        val effectiveDispatchNumber = dispatchNumber ?: generateReceiptCode(
            date = Date(),
            orderNumber = 1,
            courierIndex = if (courier != null) 1 else 0
        )

        val cleanDest = cleanDestinationLabel(destination.label)
        val mixed = calculateMixedPaymentSettlement(productAmount, deliveryFee, partialTransferAmount)

        val isDriverPaid = paymentMethod == PaymentMethodType.CASH ||
            (paymentMethod == PaymentMethodType.MIXED && mixed.isDriverFullyPaidByCash)

        val driverPaidAt = when (paymentMethod) {
            PaymentMethodType.CASH -> "Auto-descontado en efectivo"
            PaymentMethodType.MIXED -> if (mixed.isDriverFullyPaidByCash) "Auto-descontado en efectivo" else ""
            else -> ""
        }

        return renderDriverDispatchBitmap(
            storeName = config.storeName,
            dispatchNumber = effectiveDispatchNumber,
            dateString = now,
            orderStatus = orderStatus,
            customerName = customerNote.ifBlank { "Cliente General" },
            customerPhone = customerPhone,
            destinationAddress = cleanDest,
            distanceKm = quote.effectiveDistanceKm,
            approxDeliveryTime = approximateDeliveryTime,
            orderComments = orderComments,
            courierName = courier?.name ?: "Repartidor asignado",
            courierPhone = courier?.phone ?: "",
            paymentMethod = paymentMethod,
            transferBankName = transferBank.displayName,
            productAmount = productAmount,
            deliveryFee = deliveryFee,
            grandTotal = grandTotal,
            currencySymbol = cur,
            isDriverPaid = isDriverPaid,
            driverPaidAt = driverPaidAt,
            isPaymentPendingOnDelivery = isPaymentPendingOnDelivery,
            partialTransferAmount = partialTransferAmount,
            template = customTemplate
        )
    }

    /**
     * Genera el recibo digital para un registro ya guardado en el historial (vista cliente).
     */
    fun generateRecordReceiptBitmap(
        record: SavedQuoteRecord,
        config: StoreAndPricingConfig,
        isPaymentPendingOnDelivery: Boolean = record.isPaymentPendingOnDelivery
    ): Bitmap {
        val cur = record.currencySymbol.ifBlank { config.currencySymbol }
        val grandTotal = if (record.productAmount > 0) record.totalAmount else record.deliveryFee.coerceAtLeast(record.totalAmount)
        val cleanDest = cleanDestinationLabel(record.destinationLabel)

        return renderCustomerReceiptBitmap(
            storeName = config.storeName,
            receiptNumber = record.receiptId,
            dateString = "${record.dateIso} • ${record.timestamp}",
            orderStatus = record.orderStatus,
            customerName = record.customerNote.ifBlank { "Cliente ColorShop" },
            customerPhone = record.customerPhone,
            destinationAddress = cleanDest,
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
            scheduleInfo = RoutingAndFareCalculator.buildScheduleSummaryLine(config),
            isPaymentPendingOnDelivery = isPaymentPendingOnDelivery,
            partialTransferAmount = record.effectiveTransferAmount,
            template = config.customerMessageTemplate
        )
    }

    /**
     * Genera la hoja de despacho para un registro guardado en el historial (vista motorizado).
     */
    fun generateRecordDispatchBitmap(
        record: SavedQuoteRecord,
        config: StoreAndPricingConfig,
        isPaymentPendingOnDelivery: Boolean = record.isPaymentPendingOnDelivery
    ): Bitmap {
        val cur = record.currencySymbol.ifBlank { config.currencySymbol }
        val deliveryFee = if (record.deliveryFee > 0) record.deliveryFee else (if (record.productAmount > 0) record.totalAmount - record.productAmount else record.totalAmount)
        val cleanDest = cleanDestinationLabel(record.destinationLabel)

        return renderDriverDispatchBitmap(
            storeName = config.storeName,
            dispatchNumber = record.receiptId.replace("#CS-", "#DS-"),
            dateString = "${record.dateIso} • ${record.timestamp}",
            orderStatus = record.orderStatus,
            customerName = record.customerNote.ifBlank { "Cliente ColorShop" },
            customerPhone = record.customerPhone,
            destinationAddress = cleanDest,
            distanceKm = record.distanceKm,
            approxDeliveryTime = record.approximateDeliveryTime,
            orderComments = record.orderComments,
            courierName = record.courierName,
            courierPhone = record.courierPhone,
            paymentMethod = record.paymentMethod,
            transferBankName = record.transferBank,
            productAmount = record.productAmount,
            deliveryFee = deliveryFee,
            grandTotal = record.totalAmount,
            currencySymbol = cur,
            isDriverPaid = record.isDriverPaid,
            driverPaidAt = record.driverPaidAt,
            isPaymentPendingOnDelivery = isPaymentPendingOnDelivery,
            partialTransferAmount = record.effectiveTransferAmount,
            template = config.driverDispatchTemplate
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
        val activeRecords = records.filter { it.orderStatus != OrderStatus.CANCELADO }
        val totalDeliveries = activeRecords.size
        val cashAndMixedRecords = activeRecords.filter { it.paymentMethod == PaymentMethodType.CASH || it.paymentMethod == PaymentMethodType.MIXED }

        val totalCashDriverDeducted = activeRecords.sumOf { it.driverCashRetained }
        val totalCashStoreOwed = activeRecords.sumOf { it.storeCashOwedByDriver }

        val storeOwedToDriverRecords = activeRecords.filter { it.driverPendingFromStore > 0.009 }
        val electronicPaid = storeOwedToDriverRecords.filter { it.isDriverPaid }
        val electronicPending = storeOwedToDriverRecords.filter { !it.isDriverPaid }

        val totalElectronicPaidToDriver = electronicPaid.sumOf { it.driverPendingFromStore }
        val totalElectronicPendingToDriver = electronicPending.sumOf { it.driverPendingFromStore }

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
        canvas.drawText("• Envíos en Efectivo / Mixtos (Tarifa retenida por motorizado):", 80f, y + 120f, paint)
        paint.color = COLOR_TEXT_MAIN
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("${cashAndMixedRecords.size} envíos • Tarifa retenida: $currencySymbol ${totalCashDriverDeducted.toInt()}", 80f, y + 160f, paint)

        paint.color = COLOR_TEXT_MUTED
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("• Envíos por Transferencia / POS / Mixto (A cargo de ColorShop):", 80f, y + 220f, paint)
        paint.color = COLOR_TEXT_MAIN
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("${storeOwedToDriverRecords.size} envíos • Pagado: $currencySymbol ${totalElectronicPaidToDriver.toInt()} • Pendiente: $currencySymbol ${totalElectronicPendingToDriver.toInt()}", 80f, y + 260f, paint)

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
            "Corresponde a ${electronicPending.size} envíos (Transf./POS/Mixtos) con saldo a favor pendiente."
        } else {
            "Todos los envíos a cargo de tienda han sido liquidados al motorizado."
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
        canvas.drawText("Depósito a Tienda por Mercadería / Efectivo:", 80f, y + 60f, paint)

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

    /**
     * Renderiza el recibo oficial de cara al CLIENTE.
     * El recuadro final (Card 4) está limpio de detalles de conductor y muestra confirmación e indicaciones al cliente.
     */
    private fun renderCustomerReceiptBitmap(
        storeName: String = "ColorShop Managua",
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
        scheduleInfo: String,
        isPaymentPendingOnDelivery: Boolean = false,
        partialTransferAmount: Double = 0.0,
        template: String = ""
    ): Bitmap {
        val checkTemplate = template.isNotBlank()
        val isMixed = paymentMethod == PaymentMethodType.MIXED
        val mixed = calculateMixedPaymentSettlement(productAmount, deliveryFee, partialTransferAmount)

        // Verificación de campos incluidos en la plantilla activa
        val showCustomer = !checkTemplate || template.contains("{cliente}", ignoreCase = true) || template.contains("Cliente", ignoreCase = true) || template.contains("Recibe", ignoreCase = true)
        val showPhone = (!checkTemplate || template.contains("{telefono_cliente}", ignoreCase = true) || template.contains("Teléfono", ignoreCase = true) || template.contains("Celular", ignoreCase = true)) && customerPhone.isNotBlank()
        val showAddress = !checkTemplate || template.contains("{direccion}", ignoreCase = true) || template.contains("{ubicacion_maps}", ignoreCase = true) || template.contains("Destino", ignoreCase = true) || template.contains("Dirección", ignoreCase = true)
        val showDistance = !checkTemplate || template.contains("{distancia}", ignoreCase = true) || template.contains("Distancia", ignoreCase = true)
        val showTime = (!checkTemplate || template.contains("{hora_entrega}", ignoreCase = true) || template.contains("Hora", ignoreCase = true)) && approxDeliveryTime.isNotBlank()
        val showCourier = (!checkTemplate || template.contains("{repartidor}", ignoreCase = true) || template.contains("Motorizado", ignoreCase = true)) && courierName.isNotBlank()
        val showCourierPhone = (!checkTemplate || template.contains("{telefono_repartidor}", ignoreCase = true)) && courierPhone.isNotBlank()
        val showComments = (!checkTemplate || template.contains("{comentarios}", ignoreCase = true) || template.contains("Comentarios", ignoreCase = true) || template.contains("Notas", ignoreCase = true)) && orderComments.isNotBlank()

        val showMerchandise = (!checkTemplate || template.contains("{mercaderia}", ignoreCase = true) || template.contains("Mercadería", ignoreCase = true) || template.contains("Productos", ignoreCase = true)) && productAmount > 0
        val showDeliveryFee = !checkTemplate || template.contains("{tarifa_envio}", ignoreCase = true) || template.contains("Tarifa", ignoreCase = true) || template.contains("Envío", ignoreCase = true)
        val showTotal = !checkTemplate || template.contains("{total}", ignoreCase = true) || template.contains("TOTAL", ignoreCase = true)

        val showPaymentMethod = !checkTemplate || template.contains("{metodo_pago}", ignoreCase = true) || template.contains("{banco}", ignoreCase = true) || template.contains("Pago", ignoreCase = true) || template.contains("Transferencia", ignoreCase = true) || template.contains("Efectivo", ignoreCase = true) || template.contains("POS", ignoreCase = true) || template.contains("Mixto", ignoreCase = true)
        val showSchedule = (!checkTemplate || template.contains("{horarios_atencion}", ignoreCase = true) || template.contains("Horario", ignoreCase = true)) && scheduleInfo.isNotBlank()

        // Construir filas de Card 2
        data class ReceiptItem(val label: String, val value: String, val isPrimaryColor: Boolean = false)
        val clientItems = mutableListOf<ReceiptItem>()
        if (showCustomer) {
            val customerDisplay = if (showPhone) "$customerName ($customerPhone)" else customerName
            clientItems.add(ReceiptItem("Cliente:", customerDisplay))
        } else if (showPhone) {
            clientItems.add(ReceiptItem("Teléfono:", customerPhone))
        }
        if (showAddress) {
            val truncatedDest = if (destinationAddress.length > 52) destinationAddress.take(50) + "..." else destinationAddress
            clientItems.add(ReceiptItem("Dirección:", truncatedDest))
        }
        if (showDistance || showTime) {
            val distStr = if (showDistance) String.format(Locale.US, "%.2f km", distanceKm) else ""
            val timeExtra = if (showTime) " • Aprox: $approxDeliveryTime" else ""
            val combined = (distStr + timeExtra).trimStart(' ', '•', ' ')
            if (combined.isNotBlank()) {
                clientItems.add(ReceiptItem("Distancia:", combined))
            }
        }
        if (showCourier) {
            val courierDisplay = if (showCourierPhone) "$courierName • 📞 $courierPhone" else courierName
            clientItems.add(ReceiptItem("Motorizado:", courierDisplay))
        }
        if (showComments) {
            val cleanNotes = if (orderComments.length > 50) orderComments.take(48) + "..." else orderComments
            clientItems.add(ReceiptItem("Notas:", cleanNotes, isPrimaryColor = true))
        }

        // Card 3 verificación
        val hasFinanceRows = showMerchandise || showDeliveryFee || isMixed
        val hasFinanceCard = hasFinanceRows || showTotal

        // Precalcular altura dinámica total
        var estHeight = 240f + 120f // Header + Card 1
        if (clientItems.isNotEmpty()) {
            val c2H = 70f + (clientItems.size * 50f) + 15f
            estHeight += c2H + 25f
        }
        if (hasFinanceCard) {
            var rowsIn3 = 0
            if (showMerchandise) rowsIn3++
            if (showDeliveryFee) rowsIn3++
            if (isMixed) rowsIn3 += 2
            val grandBoxH = if (showTotal) 155f else 0f
            val dividerH = if (rowsIn3 > 0 && showTotal) 20f else 0f
            val c3H = 65f + (rowsIn3 * 50f) + dividerH + grandBoxH + 15f
            estHeight += c3H + 25f
        }
        if (showPaymentMethod) {
            estHeight += 250f + 25f
        }
        if (showSchedule) {
            estHeight += 35f
        }
        estHeight += 90f // Bottom footer
        val totalHeight = estHeight.toInt().coerceAtLeast(600)

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
        val subHeader = if (storeName.isNotBlank()) "$storeName • Envío a Domicilio" else "ColorShop Delivery • Envío a Domicilio"
        canvas.drawText(subHeader, 60f, 130f, paint)

        // Receipt Tag Pill Top Right
        val tagRect = RectF(BITMAP_WIDTH - 380f, 55f, BITMAP_WIDTH - 60f, 125f)
        paint.color = Color.WHITE.copy(alpha = 0.25f)
        canvas.drawRoundRect(tagRect, 35f, 35f, paint)
        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 26f
        canvas.drawText(receiptNumber, BITMAP_WIDTH - 360f, 100f, paint)

        var y = 230f

        // Card 1: Estado del Pedido y Fecha
        val statusRect = RectF(50f, y, BITMAP_WIDTH - 50f, y + 100f)
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

        val pillRect = RectF(80f, y + 20f, 360f, y + 80f)
        paint.color = badgeBg
        canvas.drawRoundRect(pillRect, 30f, 30f, paint)
        paint.color = badgeColor
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 28f
        canvas.drawText("${orderStatus.emoji} ${orderStatus.label.uppercase(Locale.getDefault())}", 115f, y + 62f, paint)

        paint.color = COLOR_TEXT_MUTED
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 24f
        canvas.drawText("Fecha: $dateString", BITMAP_WIDTH - 480f, y + 62f, paint)

        y += 125f

        // Card 2: Destinatario y Entrega (Sólo si hay elementos activos)
        if (clientItems.isNotEmpty()) {
            val card2Height = 65f + (clientItems.size * 50f) + 15f
            val infoRect = RectF(50f, y, BITMAP_WIDTH - 50f, y + card2Height)
            paint.color = COLOR_CARD
            canvas.drawRoundRect(infoRect, 24f, 24f, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2.5f
            paint.color = COLOR_BORDER
            canvas.drawRoundRect(infoRect, 24f, 24f, paint)
            paint.style = Paint.Style.FILL

            paint.color = COLOR_PRIMARY_DARK
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 28f
            canvas.drawText("Datos de Entrega y Cliente", 80f, y + 50f, paint)

            clientItems.forEachIndexed { idx, item ->
                val rowY = y + 95f + (idx * 50f)
                paint.color = COLOR_TEXT_MUTED
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.textSize = 26f
                canvas.drawText(item.label, 80f, rowY, paint)

                paint.color = if (item.isPrimaryColor) COLOR_PRIMARY_DARK else COLOR_TEXT_MAIN
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(item.value, 230f, rowY, paint)
            }

            y += card2Height + 25f
        }

        // Card 3: Desglose Financiero y Totales (Sólo si hay elementos activos)
        if (hasFinanceCard) {
            var rowsIn3 = 0
            if (showMerchandise) rowsIn3++
            if (showDeliveryFee) rowsIn3++
            if (isMixed) rowsIn3 += 2
            val grandBoxH = if (showTotal) 155f else 0f
            val dividerH = if (rowsIn3 > 0 && showTotal) 20f else 0f
            val card3Height = 65f + (rowsIn3 * 50f) + dividerH + grandBoxH + 15f

            val financeRect = RectF(50f, y, BITMAP_WIDTH - 50f, y + card3Height)
            paint.color = COLOR_CARD
            canvas.drawRoundRect(financeRect, 24f, 24f, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2.5f
            paint.color = COLOR_BORDER
            canvas.drawRoundRect(financeRect, 24f, 24f, paint)
            paint.style = Paint.Style.FILL

            paint.color = COLOR_PRIMARY_DARK
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 28f
            canvas.drawText("Detalle de Cobro y Tarifa", 80f, y + 50f, paint)

            var rY = y + 95f
            if (showMerchandise) {
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.color = COLOR_TEXT_MUTED
                paint.textSize = 26f
                canvas.drawText("Mercadería / Productos:", 80f, rY, paint)
                paint.color = COLOR_TEXT_MAIN
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("$currencySymbol ${String.format(Locale.US, "%.2f", productAmount)}", BITMAP_WIDTH - 280f, rY, paint)
                rY += 50f
            }

            if (showDeliveryFee) {
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.color = COLOR_TEXT_MUTED
                paint.textSize = 26f
                canvas.drawText("Servicio de Envío a Domicilio:", 80f, rY, paint)
                paint.color = COLOR_PRIMARY
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("$currencySymbol ${String.format(Locale.US, "%.2f", deliveryFee)}", BITMAP_WIDTH - 280f, rY, paint)
                rY += 50f
            }

            if (isMixed) {
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.color = COLOR_TEXT_MUTED
                paint.textSize = 26f
                canvas.drawText("Total Pedido (Prod + Envío):", 80f, rY, paint)
                paint.color = COLOR_TEXT_MAIN
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("$currencySymbol ${String.format(Locale.US, "%.2f", grandTotal)}", BITMAP_WIDTH - 280f, rY, paint)
                rY += 50f

                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.color = COLOR_TEXT_MUTED
                paint.textSize = 26f
                canvas.drawText("Abono Transferencia ($transferBankName):", 80f, rY, paint)
                paint.color = COLOR_BLUE
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("- $currencySymbol ${String.format(Locale.US, "%.2f", mixed.transferAmount)}", BITMAP_WIDTH - 280f, rY, paint)
                rY += 50f
            }

            if (rowsIn3 > 0 && showTotal) {
                paint.strokeWidth = 2f
                paint.color = COLOR_BORDER
                canvas.drawLine(80f, rY, BITMAP_WIDTH - 80f, rY, paint)
                rY += 20f
            }

            if (showTotal) {
                val grandBox = RectF(75f, rY, BITMAP_WIDTH - 75f, rY + 130f)
                paint.color = COLOR_PRIMARY_LIGHT
                canvas.drawRoundRect(grandBox, 18f, 18f, paint)

                paint.color = COLOR_PRIMARY_DARK
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 30f
                val totalBoxTitle = if (isMixed) "EFECTIVO A PAGAR AL RECIBIR:" else "GRAN TOTAL A PAGAR:"
                canvas.drawText(totalBoxTitle, 100f, rY + 75f, paint)

                paint.color = COLOR_PRIMARY
                paint.textSize = 50f
                val totalBoxAmount = if (isMixed) {
                    "$currencySymbol ${String.format(Locale.US, "%.2f", mixed.cashToCollect)}"
                } else {
                    "$currencySymbol ${String.format(Locale.US, "%.2f", grandTotal)}"
                }
                canvas.drawText(totalBoxAmount, BITMAP_WIDTH - 390f, rY + 80f, paint)
            }

            y += card3Height + 25f
        }

        // Card 4: Forma de Pago y Confirmación para el Cliente (Sólo si está habilitado)
        if (showPaymentMethod) {
            val clientCardRect = RectF(50f, y, BITMAP_WIDTH - 50f, y + 240f)
            val isCash = paymentMethod == PaymentMethodType.CASH
            val isPendingTransfer = paymentMethod == PaymentMethodType.TRANSFER && isPaymentPendingOnDelivery
            val isPendingPos = paymentMethod == PaymentMethodType.POS && isPaymentPendingOnDelivery
            val isPendingMixed = paymentMethod == PaymentMethodType.MIXED && isPaymentPendingOnDelivery
            val isPendingElectronic = isPendingTransfer || isPendingPos || isPendingMixed

            val boxBg = when {
                isPendingElectronic -> COLOR_ORANGE_BG
                isCash -> COLOR_GREEN_BG
                else -> COLOR_BLUE_BG
            }
            val boxBorder = when {
                isPendingElectronic -> COLOR_ORANGE
                isCash -> COLOR_GREEN
                else -> COLOR_BLUE
            }
            val boxTitleColor = when {
                isPendingElectronic -> COLOR_ORANGE
                isCash -> COLOR_GREEN
                else -> COLOR_BLUE
            }

            paint.color = boxBg
            canvas.drawRoundRect(clientCardRect, 24f, 24f, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2.5f
            paint.color = boxBorder
            canvas.drawRoundRect(clientCardRect, 24f, 24f, paint)
            paint.style = Paint.Style.FILL

            paint.color = boxTitleColor
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 28f
            val paymentMethodLabel = when (paymentMethod) {
                PaymentMethodType.CASH -> "Forma de Pago: Efectivo contra entrega"
                PaymentMethodType.TRANSFER -> if (isPaymentPendingOnDelivery) {
                    "Forma de Pago: Transferencia ($transferBankName) - PENDIENTE AL RECIBIR"
                } else {
                    "Forma de Pago: Transferencia Bancaria ($transferBankName)"
                }
                PaymentMethodType.POS -> if (isPaymentPendingOnDelivery) {
                    "Forma de Pago: Datáfono POS - COBRO AL RECIBIR"
                } else {
                    "Forma de Pago: Datáfono / Tarjeta POS al recibir"
                }
                PaymentMethodType.MIXED -> if (isPaymentPendingOnDelivery) {
                    "Forma de Pago: Mixto (Transf. $transferBankName + Efectivo)"
                } else {
                    "Forma de Pago: Pago Parcial / Mixto ($transferBankName + Efectivo)"
                }
            }
            canvas.drawText(paymentMethodLabel, 80f, y + 55f, paint)

            paint.textSize = 25f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val customerStatusHeader = when (paymentMethod) {
                PaymentMethodType.CASH -> "💵 Pago en efectivo al momento de la entrega"
                PaymentMethodType.TRANSFER -> if (isPaymentPendingOnDelivery) {
                    "⏳ Transferencia pendiente al momento de recibir entrega"
                } else {
                    "✅ Pago acreditado vía Transferencia ($transferBankName)"
                }
                PaymentMethodType.POS -> if (isPaymentPendingOnDelivery) {
                    "💳 Cobro con tarjeta / datáfono al recibir sus productos"
                } else {
                    "✅ Pago con tarjeta procesado previamente"
                }
                PaymentMethodType.MIXED -> "💳 Transf: $currencySymbol ${mixed.transferAmount.toInt()}  •  💵 Efectivo al recibir: $currencySymbol ${mixed.cashToCollect.toInt()}"
            }
            canvas.drawText(customerStatusHeader, 80f, y + 105f, paint)

            paint.textSize = 22f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.color = COLOR_TEXT_MAIN
            val customerAdvice = when (paymentMethod) {
                PaymentMethodType.CASH -> "Por favor tener listo el monto en efectivo al recibir su paquete. Verifique sus artículos con el repartidor."
                PaymentMethodType.TRANSFER -> if (isPaymentPendingOnDelivery) {
                    "⏳ Por favor realizar la transferencia bancaria y mostrar el comprobante al repartidor al recibir su paquete."
                } else {
                    "Su transferencia ha sido confirmada por ColorShop. ¡Muchas gracias por su compra y preferencia!"
                }
                PaymentMethodType.POS -> if (isPaymentPendingOnDelivery) {
                    "💳 El repartidor llevará el datáfono para cobrar su tarjeta al momento de la entrega."
                } else {
                    "El repartidor llevará su pedido con el pago verificado. ¡Muchas gracias por su preferencia!"
                }
                PaymentMethodType.MIXED -> if (isPaymentPendingOnDelivery) {
                    "⏳ Transferir $currencySymbol ${mixed.transferAmount.toInt()} a $transferBankName y entregar $currencySymbol ${mixed.cashToCollect.toInt()} en efectivo al repartidor al recibir."
                } else {
                    "✅ Abono de $currencySymbol ${mixed.transferAmount.toInt()} acreditado ($transferBankName). Por favor entregar $currencySymbol ${mixed.cashToCollect.toInt()} en efectivo al repartidor."
                }
            }
            if (customerAdvice.length > 70) {
                val splitIndex = customerAdvice.lastIndexOf(' ', 70).takeIf { it > 0 } ?: 70
                val line1 = customerAdvice.substring(0, splitIndex).trim()
                val line2 = customerAdvice.substring(splitIndex).trim()
                canvas.drawText(line1, 80f, y + 155f, paint)
                canvas.drawText(line2, 80f, y + 185f, paint)
            } else {
                canvas.drawText(customerAdvice, 80f, y + 155f, paint)
            }

            y += 265f
        }

        // Footer
        paint.color = COLOR_TEXT_MUTED
        paint.textSize = 22f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        if (showSchedule && scheduleInfo.isNotBlank()) {
            canvas.drawText("⏰ Horario: $scheduleInfo", 60f, y + 25f, paint)
            y += 35f
        }
        canvas.drawText("ColorShop Delivery • Calidad y Rapidez en Managua, Nicaragua", 60f, y + 25f, paint)

        return bitmap
    }

    /**
     * Renderiza la hoja de despacho interna exclusiva para el MOTORIZADO / REPARTIDOR.
     * Adapta dinámicamente sus campos en base a la plantilla de despacho activa.
     */
    private fun renderDriverDispatchBitmap(
        storeName: String = "ColorShop Managua",
        dispatchNumber: String,
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
        isPaymentPendingOnDelivery: Boolean = false,
        partialTransferAmount: Double = 0.0,
        template: String = ""
    ): Bitmap {
        val checkTemplate = template.isNotBlank()
        val isMixed = paymentMethod == PaymentMethodType.MIXED
        val mixed = calculateMixedPaymentSettlement(productAmount, deliveryFee, partialTransferAmount)

        val showCustomer = !checkTemplate || template.contains("{cliente}", ignoreCase = true) || template.contains("Cliente", ignoreCase = true) || template.contains("Recibe", ignoreCase = true)
        val showPhone = (!checkTemplate || template.contains("{telefono_cliente}", ignoreCase = true) || template.contains("Teléfono", ignoreCase = true)) && customerPhone.isNotBlank()
        val showAddress = !checkTemplate || template.contains("{direccion}", ignoreCase = true) || template.contains("{ubicacion_maps}", ignoreCase = true) || template.contains("Dirección", ignoreCase = true)
        val showDistance = !checkTemplate || template.contains("{distancia}", ignoreCase = true) || template.contains("Distancia", ignoreCase = true)
        val showTime = (!checkTemplate || template.contains("{hora_entrega}", ignoreCase = true) || template.contains("Hora", ignoreCase = true)) && approxDeliveryTime.isNotBlank()
        val showCourier = (!checkTemplate || template.contains("{repartidor}", ignoreCase = true) || template.contains("Motorizado", ignoreCase = true)) && courierName.isNotBlank()
        val showCourierPhone = (!checkTemplate || template.contains("{telefono_repartidor}", ignoreCase = true)) && courierPhone.isNotBlank()
        val showComments = (!checkTemplate || template.contains("{comentarios}", ignoreCase = true) || template.contains("Notas", ignoreCase = true)) && orderComments.isNotBlank()

        val showMerchandise = (!checkTemplate || template.contains("{mercaderia}", ignoreCase = true) || template.contains("Mercadería", ignoreCase = true)) && productAmount > 0
        val showDeliveryFee = !checkTemplate || template.contains("{tarifa_envio}", ignoreCase = true) || template.contains("Tarifa", ignoreCase = true)
        val showTotal = !checkTemplate || template.contains("{total}", ignoreCase = true) || template.contains("TOTAL", ignoreCase = true) || template.contains("Cobro", ignoreCase = true)
        val showSettlement = !checkTemplate || template.contains("Liquidación", ignoreCase = true) || template.contains("Modalidad", ignoreCase = true) || template.contains("PAGO", ignoreCase = true)

        data class ReceiptItem(val label: String, val value: String, val isPrimaryColor: Boolean = false)
        val clientItems = mutableListOf<ReceiptItem>()
        if (showCustomer) {
            val customerDisplay = if (showPhone) "$customerName (📞 $customerPhone)" else customerName
            clientItems.add(ReceiptItem("Cliente:", customerDisplay))
        } else if (showPhone) {
            clientItems.add(ReceiptItem("Teléfono:", customerPhone))
        }
        if (showAddress) {
            val truncatedDest = if (destinationAddress.length > 52) destinationAddress.take(50) + "..." else destinationAddress
            clientItems.add(ReceiptItem("Dirección:", truncatedDest))
        }
        if (showDistance || showTime) {
            val distStr = if (showDistance) String.format(Locale.US, "%.2f km", distanceKm) else ""
            val timeExtra = if (showTime) " • Aprox: $approxDeliveryTime" else ""
            val combined = (distStr + timeExtra).trimStart(' ', '•', ' ')
            if (combined.isNotBlank()) {
                clientItems.add(ReceiptItem("Distancia:", combined))
            }
        }
        if (showCourier) {
            val courierDisplay = if (showCourierPhone) "$courierName • 📞 $courierPhone" else courierName
            clientItems.add(ReceiptItem("Motorizado:", courierDisplay))
        }
        if (showComments) {
            val cleanNotes = if (orderComments.length > 50) orderComments.take(48) + "..." else orderComments
            clientItems.add(ReceiptItem("Notas:", cleanNotes, isPrimaryColor = true))
        }

        val hasFinanceRows = showMerchandise || showDeliveryFee || isMixed
        val hasFinanceCard = hasFinanceRows || showTotal

        // Precalcular altura dinámica total
        var estHeight = 240f + 120f
        if (clientItems.isNotEmpty()) {
            estHeight += (65f + clientItems.size * 50f + 15f) + 25f
        }
        if (hasFinanceCard) {
            var rowsIn3 = 0
            if (showMerchandise) rowsIn3++
            if (showDeliveryFee) rowsIn3++
            if (isMixed) rowsIn3++
            val collectBoxH = if (showTotal) 155f else 0f
            val dividerH = if (rowsIn3 > 0 && showTotal) 20f else 0f
            estHeight += 65f + (rowsIn3 * 50f) + dividerH + collectBoxH + 40f
        }
        if (showSettlement) {
            estHeight += 250f + 25f
        }
        estHeight += 80f // Footer
        val totalHeight = estHeight.toInt().coerceAtLeast(600)

        val bitmap = Bitmap.createBitmap(BITMAP_WIDTH, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(COLOR_BG)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Header Background
        paint.color = COLOR_PRIMARY_DARK
        canvas.drawRect(0f, 0f, BITMAP_WIDTH.toFloat(), 200f, paint)

        // Brand Title
        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 44f
        canvas.drawText("COLORSHOP DELIVERY", 60f, 85f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 26f
        paint.color = Color.parseColor("#FFD9EC")
        canvas.drawText("Hoja de Despacho • Orden para Motorizado", 60f, 130f, paint)

        // Tag Pill Top Right
        val tagRect = RectF(BITMAP_WIDTH - 380f, 55f, BITMAP_WIDTH - 60f, 125f)
        paint.color = Color.WHITE.copy(alpha = 0.25f)
        canvas.drawRoundRect(tagRect, 35f, 35f, paint)
        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 26f
        canvas.drawText(dispatchNumber, BITMAP_WIDTH - 360f, 100f, paint)

        var y = 230f

        // Card 1: Estado del Pedido y Fecha
        val statusRect = RectF(50f, y, BITMAP_WIDTH - 50f, y + 100f)
        paint.color = COLOR_CARD
        canvas.drawRoundRect(statusRect, 24f, 24f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.5f
        paint.color = COLOR_BORDER
        canvas.drawRoundRect(statusRect, 24f, 24f, paint)
        paint.style = Paint.Style.FILL

        val pillRect = RectF(80f, y + 20f, 360f, y + 80f)
        paint.color = COLOR_ORANGE_BG
        canvas.drawRoundRect(pillRect, 30f, 30f, paint)
        paint.color = COLOR_ORANGE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 28f
        canvas.drawText("${orderStatus.emoji} ${orderStatus.label.uppercase(Locale.getDefault())}", 115f, y + 62f, paint)

        paint.color = COLOR_TEXT_MUTED
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 24f
        canvas.drawText("Fecha: $dateString", BITMAP_WIDTH - 480f, y + 62f, paint)

        y += 125f

        // Card 2: Destinatario y Punto de Entrega
        if (clientItems.isNotEmpty()) {
            val card2Height = 65f + (clientItems.size * 50f) + 15f
            val infoRect = RectF(50f, y, BITMAP_WIDTH - 50f, y + card2Height)
            paint.color = COLOR_CARD
            canvas.drawRoundRect(infoRect, 24f, 24f, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2.5f
            paint.color = COLOR_BORDER
            canvas.drawRoundRect(infoRect, 24f, 24f, paint)
            paint.style = Paint.Style.FILL

            paint.color = COLOR_PRIMARY_DARK
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 28f
            canvas.drawText("Punto de Entrega y Cliente", 80f, y + 50f, paint)

            clientItems.forEachIndexed { idx, item ->
                val rowY = y + 95f + (idx * 50f)
                paint.color = COLOR_TEXT_MUTED
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.textSize = 26f
                canvas.drawText(item.label, 80f, rowY, paint)

                paint.color = if (item.isPrimaryColor) COLOR_PRIMARY_DARK else COLOR_TEXT_MAIN
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(item.value, 230f, rowY, paint)
            }

            y += card2Height + 25f
        }

        // Card 3: Desglose Financiero y Cobro a Realizar
        if (hasFinanceCard) {
            var rowsIn3 = 0
            if (showMerchandise) rowsIn3++
            if (showDeliveryFee) rowsIn3++
            if (isMixed) rowsIn3++
            val collectBoxH = if (showTotal) 155f else 0f
            val dividerH = if (rowsIn3 > 0 && showTotal) 20f else 0f
            val card3Height = 65f + (rowsIn3 * 50f) + dividerH + collectBoxH + 15f

            val financeRect = RectF(50f, y, BITMAP_WIDTH - 50f, y + card3Height)
            paint.color = COLOR_CARD
            canvas.drawRoundRect(financeRect, 24f, 24f, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2.5f
            paint.color = COLOR_BORDER
            canvas.drawRoundRect(financeRect, 24f, 24f, paint)
            paint.style = Paint.Style.FILL

            paint.color = COLOR_PRIMARY_DARK
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 28f
            canvas.drawText("Mercadería y Cobro en Entrega", 80f, y + 50f, paint)

            var rY = y + 95f
            if (showMerchandise) {
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.color = COLOR_TEXT_MUTED
                paint.textSize = 26f
                canvas.drawText("Mercadería / Productos:", 80f, rY, paint)
                paint.color = COLOR_TEXT_MAIN
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("$currencySymbol ${String.format(Locale.US, "%.2f", productAmount)}", BITMAP_WIDTH - 280f, rY, paint)
                rY += 50f
            }

            if (showDeliveryFee) {
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.color = COLOR_TEXT_MUTED
                paint.textSize = 26f
                canvas.drawText("Tarifa Servicio de Envío:", 80f, rY, paint)
                paint.color = COLOR_PRIMARY
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("$currencySymbol ${String.format(Locale.US, "%.2f", deliveryFee)}", BITMAP_WIDTH - 280f, rY, paint)
                rY += 50f
            }

            if (isMixed) {
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.color = COLOR_TEXT_MUTED
                paint.textSize = 26f
                canvas.drawText("Abono Transf. ($transferBankName):", 80f, rY, paint)
                paint.color = COLOR_BLUE
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("- $currencySymbol ${String.format(Locale.US, "%.2f", mixed.transferAmount)}", BITMAP_WIDTH - 280f, rY, paint)
                rY += 50f
            }

            if (rowsIn3 > 0 && showTotal) {
                paint.strokeWidth = 2f
                paint.color = COLOR_BORDER
                canvas.drawLine(80f, rY, BITMAP_WIDTH - 80f, rY, paint)
                rY += 20f
            }

            if (showTotal) {
                val collectBox = RectF(75f, rY, BITMAP_WIDTH - 75f, rY + 130f)
                val isCash = paymentMethod == PaymentMethodType.CASH
                val isPendingTransfer = paymentMethod == PaymentMethodType.TRANSFER && isPaymentPendingOnDelivery
                val isPendingPos = paymentMethod == PaymentMethodType.POS && isPaymentPendingOnDelivery
                val isPendingElectronic = isPendingTransfer || isPendingPos
                val isCollectRequired = if (isMixed) {
                    mixed.cashToCollect > 0.0 || isPaymentPendingOnDelivery
                } else {
                    isCash || isPendingElectronic
                }

                paint.color = if (isCollectRequired) COLOR_ORANGE_BG else COLOR_BLUE_BG
                canvas.drawRoundRect(collectBox, 18f, 18f, paint)

                paint.color = if (isCollectRequired) COLOR_ORANGE else COLOR_BLUE
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 26f
                val collectTitle = when (paymentMethod) {
                    PaymentMethodType.MIXED -> if (isPaymentPendingOnDelivery) {
                        "COBRAR EFECTIVO + VERIFICAR TRANSF.:"
                    } else {
                        "COBRAR AL CLIENTE (EFECTIVO RESTANTE):"
                    }
                    PaymentMethodType.CASH -> "COBRAR AL CLIENTE (EFECTIVO):"
                    PaymentMethodType.TRANSFER -> if (isPaymentPendingOnDelivery) {
                        "COBRAR / VERIFICAR TRANSFERENCIA:"
                    } else {
                        "COBRO AL CLIENTE EN ENTREGA:"
                    }
                    PaymentMethodType.POS -> if (isPaymentPendingOnDelivery) {
                        "COBRAR CON DATÁFONO POS:"
                    } else {
                        "COBRO AL CLIENTE EN ENTREGA:"
                    }
                }
                canvas.drawText(collectTitle, 95f, rY + 45f, paint)

                paint.textSize = 46f
                val collectAmountText = when (paymentMethod) {
                    PaymentMethodType.MIXED -> "$currencySymbol ${mixed.cashToCollect.toInt()} EFECTIVO"
                    PaymentMethodType.CASH -> "$currencySymbol ${grandTotal.toInt()}"
                    PaymentMethodType.TRANSFER -> if (isPaymentPendingOnDelivery) {
                        "$currencySymbol ${grandTotal.toInt()}"
                    } else {
                        "$currencySymbol 0.00 (YA PAGADO)"
                    }
                    PaymentMethodType.POS -> if (isPaymentPendingOnDelivery) {
                        "$currencySymbol ${grandTotal.toInt()}"
                    } else {
                        "$currencySymbol 0.00 (YA PAGADO)"
                    }
                }
                canvas.drawText(collectAmountText, 95f, rY + 105f, paint)
            }

            y += card3Height + 25f
        }

        // Card 4: Liquidación y Cuentas del Conductor (Sólo si está habilitado)
        if (showSettlement) {
            val settlementCardRect = RectF(50f, y, BITMAP_WIDTH - 50f, y + 240f)
            val isPendingTransfer = paymentMethod == PaymentMethodType.TRANSFER && isPaymentPendingOnDelivery
            val isPendingPos = paymentMethod == PaymentMethodType.POS && isPaymentPendingOnDelivery
            val isPendingElectronic = isPendingTransfer || isPendingPos
            val isSettlementPaid = when (paymentMethod) {
                PaymentMethodType.MIXED -> !isPaymentPendingOnDelivery && (isDriverPaid || mixed.isDriverFullyPaidByCash)
                PaymentMethodType.CASH -> true
                else -> !isPendingElectronic && isDriverPaid
            }

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
                PaymentMethodType.CASH -> "Modalidad: Cobro Efectivo al Cliente"
                PaymentMethodType.TRANSFER -> if (isPaymentPendingOnDelivery) {
                    "Modalidad: Transferencia PENDIENTE al recibir ($transferBankName)"
                } else {
                    "Modalidad: Ya Pagado por Transferencia ($transferBankName)"
                }
                PaymentMethodType.POS -> if (isPaymentPendingOnDelivery) {
                    "Modalidad: Cobro con Datáfono POS al entregar"
                } else {
                    "Modalidad: Cobro con Datáfono / POS"
                }
                PaymentMethodType.MIXED -> "Modalidad: Pago Mixto (Transf. $currencySymbol${mixed.transferAmount.toInt()} + Efectivo $currencySymbol${mixed.cashToCollect.toInt()})"
            }
            canvas.drawText(paymentMethodLabel, 80f, y + 55f, paint)

            paint.textSize = 25f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val driverStatusHeader = when (paymentMethod) {
                PaymentMethodType.MIXED -> {
                    when {
                        mixed.storeOwedByDriver > 0.009 -> {
                            "✅ ENVÍO CUBIERTO ($currencySymbol ${deliveryFee.toInt()}) • DEPOSITAR A TIENDA: $currencySymbol ${mixed.storeOwedByDriver.toInt()}"
                        }
                        mixed.driverPendingFromStore <= 0.009 -> {
                            "✅ ENVÍO CUBIERTO EN EFECTIVO ($currencySymbol ${deliveryFee.toInt()}) • SALDO TIENDA: $currencySymbol 0"
                        }
                        else -> {
                            if (isDriverPaid) {
                                "✅ ENVÍO LIQUIDADO: Cobraste $currencySymbol${mixed.cashToCollect.toInt()} + Tienda pagó $currencySymbol${mixed.driverPendingFromStore.toInt()}"
                            } else {
                                "⏳ RETIENES $currencySymbol ${mixed.cashToCollect.toInt()} EFECTIVO • TIENDA TE DEBE: $currencySymbol ${mixed.driverPendingFromStore.toInt()}"
                            }
                        }
                    }
                }
                PaymentMethodType.CASH -> "✅ PAGO MOTORIZADO: Te descuentas tu tarifa de envío ($currencySymbol ${deliveryFee.toInt()})"
                PaymentMethodType.TRANSFER -> if (isPaymentPendingOnDelivery) {
                    "⏳ PAGO MOTORIZADO: Tienda te pagará ($currencySymbol ${deliveryFee.toInt()}) tras confirmarse"
                } else if (isDriverPaid) {
                    val paidDetail = if (driverPaidAt.isNotBlank()) " (Liquidado: $driverPaidAt)" else ""
                    "✅ PAGO MOTORIZADO: Pagado por tienda$paidDetail"
                } else {
                    "⏳ PAGO MOTORIZADO: Tienda te pagará ($currencySymbol ${deliveryFee.toInt()})"
                }
                PaymentMethodType.POS -> if (isPaymentPendingOnDelivery) {
                    "⏳ PAGO MOTORIZADO: Tienda te pagará ($currencySymbol ${deliveryFee.toInt()}) tras cobro POS"
                } else if (isDriverPaid) {
                    val paidDetail = if (driverPaidAt.isNotBlank()) " (Liquidado: $driverPaidAt)" else ""
                    "✅ PAGO MOTORIZADO: Pagado por tienda$paidDetail"
                } else {
                    "⏳ PAGO MOTORIZADO: Tienda te pagará ($currencySymbol ${deliveryFee.toInt()})"
                }
            }
            canvas.drawText(driverStatusHeader, 80f, y + 105f, paint)

            paint.textSize = 22f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.color = COLOR_TEXT_MAIN
            val settlementAdvice = when (paymentMethod) {
                PaymentMethodType.MIXED -> {
                    when {
                        mixed.storeOwedByDriver > 0.009 -> {
                            "Cobras $currencySymbol ${mixed.cashToCollect.toInt()} en efectivo. Te descuentas tu envío ($currencySymbol ${deliveryFee.toInt()}) y depositas $currencySymbol ${mixed.storeOwedByDriver.toInt()} a tienda."
                        }
                        mixed.driverPendingFromStore <= 0.009 -> {
                            "Cobras $currencySymbol ${mixed.cashToCollect.toInt()} en efectivo y te lo quedas por tu envío ($currencySymbol ${deliveryFee.toInt()}). No depositas nada a tienda."
                        }
                        else -> {
                            "Cobras $currencySymbol ${mixed.cashToCollect.toInt()} en efectivo para tu envío. No depositas a tienda y ColorShop te pagará el resto ($currencySymbol ${mixed.driverPendingFromStore.toInt()})."
                        }
                    }
                }
                PaymentMethodType.CASH -> "Cobras $currencySymbol ${grandTotal.toInt()} al cliente. Te retienes $currencySymbol ${deliveryFee.toInt()} de tu envío y depositas $currencySymbol ${productAmount.toInt()} a tienda."
                PaymentMethodType.TRANSFER -> if (isPaymentPendingOnDelivery) {
                    "El cliente transferirá $currencySymbol ${grandTotal.toInt()} a $transferBankName. Verificar comprobante o confirmación de tienda antes de entregar. Tienda te pagará tu envío ($currencySymbol ${deliveryFee.toInt()})."
                } else if (isDriverPaid) {
                    "Cobro al cliente: $currencySymbol 0. La tienda ya te transfirió tu tarifa de entrega."
                } else {
                    "Cobro al cliente: $currencySymbol 0 (Ya pagado). ${storeName.ifBlank { "ColorShop" }} te transferirá tu tarifa de entrega ($currencySymbol ${deliveryFee.toInt()}) hoy o mañana."
                }
                PaymentMethodType.POS -> if (isPaymentPendingOnDelivery) {
                    "Cobrar $currencySymbol ${grandTotal.toInt()} con datáfono POS de tienda al cliente. ${storeName.ifBlank { "ColorShop" }} te pagará tu tarifa de entrega ($currencySymbol ${deliveryFee.toInt()}) hoy o mañana."
                } else if (isDriverPaid) {
                    "Cobrar con POS. Tarifa de entrega pagada por tienda."
                } else {
                    "Cobrar con POS de la tienda. ${storeName.ifBlank { "ColorShop" }} te pagará tu tarifa de entrega ($currencySymbol ${deliveryFee.toInt()}) hoy o mañana."
                }
            }
            if (settlementAdvice.length > 70) {
                val splitIndex = settlementAdvice.lastIndexOf(' ', 70).takeIf { it > 0 } ?: 70
                val line1 = settlementAdvice.substring(0, splitIndex).trim()
                val line2 = settlementAdvice.substring(splitIndex).trim()
                canvas.drawText(line1, 80f, y + 155f, paint)
                canvas.drawText(line2, 80f, y + 185f, paint)
            } else {
                canvas.drawText(settlementAdvice, 80f, y + 155f, paint)
            }

            y += 265f
        }

        // Footer
        paint.color = COLOR_TEXT_MUTED
        paint.textSize = 22f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("${storeName.ifBlank { "ColorShop Delivery" }} • Hoja Interna para Repartidor", 60f, y + 25f, paint)

        return bitmap
    }

    private fun cleanDestinationLabel(label: String): String {
        val clean = label
            .replace("congelada", "", ignoreCase = true)
            .replace("congelado", "", ignoreCase = true)
            .trim()
            .trimEnd(',', '-')
            .trim()
        return if (clean.isBlank() || clean.startsWith("Ubicación en mapa", ignoreCase = true) || clean.startsWith("Punto en mapa", ignoreCase = true)) {
            "Ubicación en mapa (Google Maps)"
        } else {
            clean
        }
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
