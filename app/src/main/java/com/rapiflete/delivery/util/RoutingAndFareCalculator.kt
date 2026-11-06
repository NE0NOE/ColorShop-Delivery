package com.rapiflete.delivery.util

import com.rapiflete.delivery.model.CourierDriver
import com.rapiflete.delivery.model.DistanceCalculationMode
import com.rapiflete.delivery.model.FrozenDestination
import com.rapiflete.delivery.model.GeoCoordinate
import com.rapiflete.delivery.model.PaymentMethodType
import com.rapiflete.delivery.model.QuoteBreakdown
import com.rapiflete.delivery.model.RoundingMode
import com.rapiflete.delivery.model.RouteResult
import com.rapiflete.delivery.model.SpecialZoneRule
import com.rapiflete.delivery.model.StoreAndPricingConfig
import com.rapiflete.delivery.model.SurchargeType
import com.rapiflete.delivery.model.TransferBankType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object RoutingAndFareCalculator {

    private const val EARTH_RADIUS_KM = 6371.0088

    /**
     * Calcula la distancia en línea recta (Haversine) en kilómetros entre dos coordenadas.
     */
    fun calculateHaversineKm(from: GeoCoordinate, to: GeoCoordinate): Double {
        val dLat = Math.toRadians(to.latitude - from.latitude)
        val dLon = Math.toRadians(to.longitude - from.longitude)
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)

        val a = sin(dLat / 2) * sin(dLat / 2) +
            sin(dLon / 2) * sin(dLon / 2) * cos(lat1) * cos(lat2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_KM * c
    }

    /**
     * Consulta la ruta real de manejo por calles usando el servidor público OSRM (OpenStreetMap)
     * y calcula también la distancia en línea recta + factor de ajuste para permitir comparar o alternar.
     */
    suspend fun calculateRoute(
        origin: GeoCoordinate,
        destination: GeoCoordinate,
        preferredMode: DistanceCalculationMode,
        straightLineFactor: Double
    ): RouteResult = withContext(Dispatchers.IO) {
        val straightKm = calculateHaversineKm(origin, destination)
        val adjustedStraightKm = straightKm * straightLineFactor.coerceAtLeast(1.0)

        var osrmKm: Double? = null
        var osrmMinutes: Double? = null
        var geometryPoints: List<GeoCoordinate> = listOf(origin, destination)
        var fallbackNote: String? = null

        val osrmEndpoints = listOf(
            "https://router.project-osrm.org/route/v1/driving/${origin.longitude},${origin.latitude};${destination.longitude},${destination.latitude}?overview=full&geometries=geojson",
            "https://routing.openstreetmap.de/routed-car/route/v1/driving/${origin.longitude},${origin.latitude};${destination.longitude},${destination.latitude}?overview=full&geometries=geojson"
        )

        for (endpoint in osrmEndpoints) {
            try {
                val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 6000
                    readTimeout = 6000
                    setRequestProperty("User-Agent", "RapiFleteDeliveryApp/1.0 (Android)")
                }
                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(body)
                    if (json.optString("code") == "Ok") {
                        val routes = json.getJSONArray("routes")
                        if (routes.length() > 0) {
                            val firstRoute = routes.getJSONObject(0)
                            val distanceMeters = firstRoute.getDouble("distance")
                            val durationSeconds = firstRoute.getDouble("duration")
                            osrmKm = distanceMeters / 1000.0
                            osrmMinutes = durationSeconds / 60.0

                            val coordsArray = firstRoute
                                .optJSONObject("geometry")
                                ?.optJSONArray("coordinates")
                            if (coordsArray != null && coordsArray.length() > 1) {
                                val pts = ArrayList<GeoCoordinate>(coordsArray.length())
                                for (i in 0 until coordsArray.length()) {
                                    val pair = coordsArray.getJSONArray(i)
                                    val lon = pair.getDouble(0)
                                    val lat = pair.getDouble(1)
                                    pts.add(GeoCoordinate(lat, lon))
                                }
                                geometryPoints = pts
                            }
                            conn.disconnect()
                            break
                        }
                    }
                }
                conn.disconnect()
            } catch (_: Exception) {
                // Intentar siguiente mirror OSRM o usar línea recta ajustada
            }
        }

        val actualModeUsed: DistanceCalculationMode
        val effectiveKm: Double

        if (preferredMode == DistanceCalculationMode.OSRM_DRIVING) {
            if (osrmKm != null) {
                actualModeUsed = DistanceCalculationMode.OSRM_DRIVING
                effectiveKm = osrmKm
            } else {
                actualModeUsed = DistanceCalculationMode.STRAIGHT_LINE_ADJUSTED
                effectiveKm = adjustedStraightKm
                fallbackNote = "Sin conexión a OSRM; usando línea recta × ${String.format(Locale.US, "%.2f", straightLineFactor)}"
            }
        } else {
            actualModeUsed = DistanceCalculationMode.STRAIGHT_LINE_ADJUSTED
            effectiveKm = adjustedStraightKm
        }

        RouteResult(
            straightLineKm = straightKm,
            adjustedStraightLineKm = adjustedStraightKm,
            osrmDrivingKm = osrmKm,
            osrmDurationMinutes = osrmMinutes,
            routeGeometry = geometryPoints,
            usedMode = actualModeUsed,
            effectiveDistanceKm = effectiveKm,
            fallbackReason = fallbackNote
        )
    }

    /**
     * Determina si la hora actual está fuera del horario comercial configurado.
     */
    fun isCurrentlyOffHours(startHour: Int, endHour: Int, calendar: Calendar = Calendar.getInstance()): Boolean {
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        val timeInMinutes = currentHour * 60 + currentMinute
        val startInMinutes = startHour.coerceIn(0, 23) * 60
        val endInMinutes = endHour.coerceIn(0, 23) * 60

        return if (startInMinutes < endInMinutes) {
            timeInMinutes < startInMinutes || timeInMinutes >= endInMinutes
        } else if (startInMinutes > endInMinutes) {
            // Horario que cruza medianoche
            timeInMinutes < startInMinutes && timeInMinutes >= endInMinutes
        } else {
            false
        }
    }

    /**
     * Calcula el desglose completo de la cotización de envío según la distancia, tarifa mínima,
     * kilómetros incluidos, costo por km adicional, recargos y redondeo.
     */
    fun calculateQuoteBreakdown(
        route: RouteResult,
        modeOverride: DistanceCalculationMode,
        config: StoreAndPricingConfig,
        applyOffHours: Boolean,
        applyHoliday: Boolean,
        applyRainOrDemand: Boolean,
        manualExtraAdjustment: Double = 0.0,
        activeZone: SpecialZoneRule? = null,
        destinationCoord: GeoCoordinate? = null
    ): QuoteBreakdown {
        val effectiveKm = when (modeOverride) {
            DistanceCalculationMode.OSRM_DRIVING -> route.osrmDrivingKm ?: route.adjustedStraightLineKm
            DistanceCalculationMode.STRAIGHT_LINE_ADJUSTED -> route.adjustedStraightLineKm
        }

        val actualMode = if (modeOverride == DistanceCalculationMode.OSRM_DRIVING && route.osrmDrivingKm != null) {
            DistanceCalculationMode.OSRM_DRIVING
        } else {
            DistanceCalculationMode.STRAIGHT_LINE_ADJUSTED
        }

        val baseMinimumFare: Double
        val includedKm: Double
        val extraKm: Double
        val effectiveExtraKmRate: Double

        if (activeZone != null) {
            baseMinimumFare = activeZone.basePrice
            includedKm = activeZone.radiusKm.coerceAtLeast(0.0)
            effectiveExtraKmRate = if (activeZone.appliesExtraKm) activeZone.extraKmPrice.coerceAtLeast(0.0) else 0.0
            if (activeZone.appliesExtraKm && destinationCoord != null) {
                val zoneCenter = GeoCoordinate(activeZone.centerLat, activeZone.centerLng)
                val distFromZoneCenter = calculateHaversineKm(zoneCenter, destinationCoord)
                extraKm = max(0.0, distFromZoneCenter - includedKm)
            } else {
                extraKm = 0.0
            }
        } else {
            baseMinimumFare = config.minimumFare
            includedKm = config.includedKmInMinimum.coerceAtLeast(0.0)
            effectiveExtraKmRate = config.costPerExtraKm.coerceAtLeast(0.0)
            extraKm = max(0.0, effectiveKm - includedKm)
        }

        val extraDistanceCharge = extraKm * effectiveExtraKmRate
        val subtotalBeforeSurcharges = max(
            baseMinimumFare,
            baseMinimumFare + extraDistanceCharge
        )

        val offHoursCharge = if (applyOffHours) {
            computeSurcharge(subtotalBeforeSurcharges, config.offHoursSurchargeType, config.offHoursSurchargeValue)
        } else {
            0.0
        }

        val holidayCharge = if (applyHoliday) {
            computeSurcharge(subtotalBeforeSurcharges, config.holidaySurchargeType, config.holidaySurchargeValue)
        } else {
            0.0
        }

        val rainCharge = if (applyRainOrDemand) {
            computeSurcharge(subtotalBeforeSurcharges, config.rainOrHighDemandSurchargeType, config.rainOrHighDemandSurchargeValue)
        } else {
            0.0
        }

        val rawTotal = max(
            baseMinimumFare,
            subtotalBeforeSurcharges + offHoursCharge + holidayCharge + rainCharge + manualExtraAdjustment
        )

        val finalRounded = applyRounding(rawTotal, config.roundingMode)
        val roundingDiff = finalRounded - rawTotal

        return QuoteBreakdown(
            effectiveDistanceKm = effectiveKm,
            straightLineKm = route.straightLineKm,
            adjustedStraightLineKm = route.adjustedStraightLineKm,
            osrmDrivingKm = route.osrmDrivingKm,
            durationMinutes = route.osrmDurationMinutes,
            usedMode = actualMode,
            baseMinimumFare = baseMinimumFare,
            includedKm = includedKm,
            extraKm = extraKm,
            costPerExtraKm = effectiveExtraKmRate,
            extraDistanceCharge = extraDistanceCharge,
            subtotalBeforeSurcharges = subtotalBeforeSurcharges,
            offHoursApplied = applyOffHours,
            offHoursCharge = offHoursCharge,
            holidayApplied = applyHoliday,
            holidayCharge = holidayCharge,
            rainApplied = applyRainOrDemand,
            rainCharge = rainCharge,
            manualExtraAdjustment = manualExtraAdjustment,
            selectedZoneName = activeZone?.name,
            rawTotal = rawTotal,
            finalRoundedTotal = finalRounded,
            roundingDifference = roundingDiff
        )
    }

    private fun computeSurcharge(baseAmount: Double, type: SurchargeType, value: Double): Double {
        if (value <= 0.0) return 0.0
        return when (type) {
            SurchargeType.FIXED -> value
            SurchargeType.PERCENTAGE -> baseAmount * (value / 100.0)
        }
    }

    fun applyRounding(amount: Double, mode: RoundingMode): Double {
        return when (mode) {
            RoundingMode.NONE -> (amount * 100.0).roundToInt() / 100.0
            RoundingMode.MULTIPLE_5 -> {
                val step = 5.0
                (amount / step).roundToInt() * step
            }
            RoundingMode.MULTIPLE_10 -> {
                val step = 10.0
                (amount / step).roundToInt() * step
            }
        }
    }

    /**
     * Calcula la tarifa preferencial sugerida cuando el monto calculado supera el umbral razonable.
     * Por ejemplo: Para una tarifa calculada de C$ 175 con umbral de C$ 150 y reducción del 15%,
     * devuelve exactamente C$ 150 (con redondeo a múltiplos de 5).
     */
    fun calculatePreferentialFare(
        calculatedTotal: Double,
        threshold: Double,
        discountPercent: Double,
        roundingMode: RoundingMode
    ): Double {
        if (calculatedTotal <= threshold) return calculatedTotal
        val discounted = calculatedTotal * (1.0 - (discountPercent.coerceIn(0.0, 99.0) / 100.0))
        val bounded = maxOf(threshold, discounted)
        return applyRounding(bounded, roundingMode)
    }

    fun formatMoney(amount: Double, currencySymbol: String): String {
        val isWhole = amount % 1.0 == 0.0
        return if (isWhole) {
            "$currencySymbol ${amount.toInt()}"
        } else {
            String.format(Locale.US, "%s %.2f", currencySymbol, amount)
        }
    }

    fun buildScheduleSummaryLine(config: StoreAndPricingConfig): String {
        val parts = mutableListOf<String>()
        if (config.weekdayOpenText.isNotBlank() && config.weekdayCloseText.isNotBlank()) {
            parts.add("Lun a Vie: ${config.weekdayOpenText} - ${config.weekdayCloseText}")
        }
        if (config.saturdayEnabled && config.saturdayOpenText.isNotBlank() && config.saturdayCloseText.isNotBlank()) {
            parts.add("Sáb: ${config.saturdayOpenText} - ${config.saturdayCloseText}")
        }
        if (config.sundayEnabled && config.sundayOpenText.isNotBlank() && config.sundayCloseText.isNotBlank()) {
            parts.add("Dom: ${config.sundayOpenText} - ${config.sundayCloseText}")
        }
        return parts.joinToString(" | ")
    }

    /**
     * Resuelve una plantilla reemplazando cada etiqueta {variable} por su valor formateado.
     * Si una línea contiene únicamente etiquetas y todas están vacías (o en blanco), se omite la línea completa
     * para no dejar encabezados sueltos o vacíos.
     */
    fun resolveTemplate(template: String, variables: Map<String, String>): String {
        val tokenRegex = Regex("\\{([a-zA-Z0-9_]+)\\}")
        val lines = template.lines()
        val keptLines = mutableListOf<String>()

        for (rawLine in lines) {
            val matches = tokenRegex.findAll(rawLine).toList()
            if (matches.isNotEmpty()) {
                val allTokensBlank = matches.all { match ->
                    val key = match.groupValues[1]
                    variables[key].isNullOrBlank()
                }
                if (allTokensBlank) {
                    continue
                }
            }

            var line = rawLine
            for (match in matches) {
                val key = match.groupValues[1]
                val value = variables[key] ?: ""
                line = line.replace("{${key}}", value)
            }
            keptLines.add(line)
        }

        return keptLines.joinToString("\n").replace(Regex("\n{3,}"), "\n\n").trim()
    }

    /**
     * Genera el texto listo para pegar en el chat de WhatsApp del cliente.
     * - Sin tiempo estimado de ruta (el motorizado puede llevar varios envíos).
     * - En distancia sin el paréntesis "(Ruta Calles)".
     * - "Destino:" seguido únicamente de la dirección.
     * - Incluye Persona que recibe, Teléfono del cliente, Hora aproximada de entrega, Comentarios del pedido,
     *   datos del motorizado (si tiene habilitados los checks de visibilidad) y los horarios habilitados de la tienda.
     * - Sin decir "tarifa base", mostrando la tarifa mínima + tarifa de distancia (o cálculo por km) y el total de envío.
     * - Si el usuario editó manualmente el monto final acordado (customTotalOverride), usa ese total.
     */
    fun buildCustomerWhatsAppSummary(
        config: StoreAndPricingConfig,
        destination: FrozenDestination,
        quote: QuoteBreakdown,
        customerNote: String,
        customerPhone: String = "",
        orderComments: String = "",
        approximateDeliveryTime: String = "",
        courier: CourierDriver? = null,
        paymentMethod: PaymentMethodType? = null,
        transferBank: TransferBankType = TransferBankType.BAC,
        productAmount: Double = 0.0,
        isPaymentPendingOnDelivery: Boolean = false,
        receiptCode: String = ""
    ): String {
        if (config.customerMessageTemplate.isNotBlank()) {
            val cur = config.currencySymbol
            val deliveryFee = quote.effectiveTotal
            val grandTotal = productAmount + deliveryFee
            val mapUrl = destination.coordinate.toStaticGoogleMapsUrl()
            val destLabelClean = destination.label
                .replace("congelada", "", ignoreCase = true)
                .replace("congelado", "", ignoreCase = true)
                .trim()
                .trimEnd(',', '-')
                .trim()

            val cleanAddress = if (destLabelClean.isNotBlank() && !destLabelClean.startsWith("Ubicación en mapa", ignoreCase = true) && !destLabelClean.startsWith("Punto en mapa", ignoreCase = true)) {
                destLabelClean
            } else {
                "Ubicación enviada en mapa"
            }

            val courierName = if (courier != null && courier.includeNameInCustomerMessage && courier.name.isNotBlank()) {
                courier.name
            } else ""

            val courierPhone = if (courier != null && courier.includePhoneInCustomerMessage && courier.phone.isNotBlank()) {
                courier.phone
            } else ""

            val paymentMethodText = if (paymentMethod != null) {
                when (paymentMethod) {
                    PaymentMethodType.CASH -> "Efectivo al recibir"
                    PaymentMethodType.TRANSFER -> if (isPaymentPendingOnDelivery) {
                        "Transferencia (${transferBank.displayName}) - *Pendiente al recibir entrega*"
                    } else {
                        "Transferencia (${transferBank.displayName}) - *Acreditada / Confirmada*"
                    }
                    PaymentMethodType.POS -> if (isPaymentPendingOnDelivery) {
                        "POS / Tarjeta - *Cobro con datáfono al entregar*"
                    } else {
                        "POS / Tarjeta - *Ya cobrado previamente*"
                    }
                }
            } else ""

            val bankText = if (paymentMethod == PaymentMethodType.TRANSFER) transferBank.displayName else ""

            val incText = if (quote.selectedZoneName != null) {
                " (Zona ${quote.selectedZoneName}, radio ${String.format(Locale.US, "%.1f", quote.includedKm)} km)"
            } else if (quote.includedKm > 0) {
                " (incluye ${String.format(Locale.US, "%.1f", quote.includedKm)} km)"
            } else ""
            val fareLines = mutableListOf<String>()
            fareLines.add("• Tarifa base envío$incText: ${formatMoney(quote.baseMinimumFare, cur)}")
            if (quote.extraKm > 0.01) {
                val extraLabel = if (quote.selectedZoneName != null) "Km extra fuera del radio de zona" else "Tarifa de distancia"
                fareLines.add(
                    "• $extraLabel (${String.format(Locale.US, "%.2f", quote.extraKm)} km × ${formatMoney(quote.costPerExtraKm, cur)}/km): +${formatMoney(quote.extraDistanceCharge, cur)}"
                )
            }
            if (quote.offHoursApplied && quote.offHoursCharge > 0) {
                fareLines.add("• Recargo fuera de horario / nocturno: +${formatMoney(quote.offHoursCharge, cur)}")
            }
            if (quote.holidayApplied && quote.holidayCharge > 0) {
                fareLines.add("• Recargo día festivo / fin de semana: +${formatMoney(quote.holidayCharge, cur)}")
            }
            if (quote.rainApplied && quote.rainCharge > 0) {
                fareLines.add("• Recargo lluvia / alta demanda: +${formatMoney(quote.rainCharge, cur)}")
            }
            if (quote.manualExtraAdjustment != 0.0) {
                val sign = if (quote.manualExtraAdjustment > 0) "+" else ""
                fareLines.add("• Cargo adicional: $sign${formatMoney(quote.manualExtraAdjustment, cur)}")
            }
            if (quote.customTotalOverride != null) {
                val diff = quote.finalRoundedTotal - quote.customTotalOverride
                if (diff > 0.01) {
                    fareLines.add("• Descuento Tarifa Preferencial: -${formatMoney(diff, cur)}")
                } else if (diff < -0.01) {
                    fareLines.add("• Cargo adicional acordado: +${formatMoney(-diff, cur)}")
                }
            }
            if (productAmount > 0.0) {
                fareLines.add("🛵 *Subtotal Envío:* ${formatMoney(deliveryFee, cur)}")
            }

            val fareString = if (config.customerMessageTemplate.lines().any { it.trim() == "{tarifa_envio}" }) {
                fareLines.joinToString("\n")
            } else {
                formatMoney(deliveryFee, cur)
            }

            val totalString = if (productAmount > 0.0) {
                "GRAN TOTAL A PAGAR: ${formatMoney(grandTotal, cur)}"
            } else {
                "TOTAL ENVÍO: ${formatMoney(deliveryFee, cur)}"
            }

            val variables = mapOf(
                "cliente" to customerNote.trim(),
                "telefono_cliente" to customerPhone.trim(),
                "direccion" to cleanAddress,
                "ubicacion_maps" to mapUrl,
                "distancia" to String.format(Locale.US, "%.2f km", quote.effectiveDistanceKm),
                "hora_entrega" to approximateDeliveryTime.trim(),
                "comentarios" to orderComments.trim(),
                "mercaderia" to if (productAmount > 0.0) formatMoney(productAmount, cur) else "",
                "tarifa_envio" to fareString,
                "total" to totalString,
                "metodo_pago" to paymentMethodText,
                "banco" to bankText,
                "repartidor" to courierName,
                "telefono_repartidor" to courierPhone,
                "horarios_atencion" to buildScheduleSummaryLine(config),
                "nombre_tienda" to config.storeName,
                "codigo_recibo" to receiptCode.trim()
            )

            return resolveTemplate(config.customerMessageTemplate, variables)
        }

        val cur = config.currencySymbol
        val deliveryFee = quote.effectiveTotal
        val grandTotal = productAmount + deliveryFee
        val sb = StringBuilder()
        sb.appendLine("🛵 *COTIZACIÓN DE ENVÍO - COLORSHOP DELIVERY*")
        if (customerNote.isNotBlank()) {
            sb.appendLine("👤 *Recibe / Cliente:* $customerNote")
        }
        if (customerPhone.isNotBlank()) {
            sb.appendLine("📞 *Teléfono:* $customerPhone")
        }
        val mapUrl = destination.coordinate.toStaticGoogleMapsUrl()
        val destLabelClean = destination.label
            .replace("congelada", "", ignoreCase = true)
            .replace("congelado", "", ignoreCase = true)
            .trim()
            .trimEnd(',', '-')
            .trim()

        if (destLabelClean.isNotBlank() && !destLabelClean.startsWith("Ubicación en mapa", ignoreCase = true) && !destLabelClean.startsWith("Punto en mapa", ignoreCase = true)) {
            sb.appendLine("📍 *Destino:* $destLabelClean")
            sb.appendLine("🗺️ *Ubicación en mapa:* $mapUrl")
        } else {
            sb.appendLine("📍 *Destino:* Ubicación enviada en mapa")
            sb.appendLine("🗺️ *Ubicación en mapa:* $mapUrl")
        }
        sb.appendLine("📏 *Distancia:* ${String.format(Locale.US, "%.2f km", quote.effectiveDistanceKm)}")
        if (approximateDeliveryTime.isNotBlank()) {
            sb.appendLine("🕒 *Hora aprox. de entrega:* $approximateDeliveryTime")
        }
        if (orderComments.isNotBlank()) {
            sb.appendLine("📝 *Comentarios del pedido:* $orderComments")
        }
        if (courier != null) {
            if (courier.includeNameInCustomerMessage && courier.name.isNotBlank()) {
                sb.appendLine("🛵 *Motorizado asignado:* ${courier.name}")
            }
            if (courier.includePhoneInCustomerMessage && courier.phone.isNotBlank()) {
                sb.appendLine("📱 *Contacto motorizado:* ${courier.phone}")
            }
        }
        if (paymentMethod != null) {
            val paymentText = when (paymentMethod) {
                PaymentMethodType.CASH -> "Efectivo al recibir"
                PaymentMethodType.TRANSFER -> if (isPaymentPendingOnDelivery) {
                    "Transferencia (${transferBank.displayName}) - *Pendiente al recibir entrega*"
                } else {
                    "Transferencia (${transferBank.displayName}) - *Acreditada / Confirmada*"
                }
                PaymentMethodType.POS -> if (isPaymentPendingOnDelivery) {
                    "POS / Tarjeta - *Cobro con datáfono al entregar*"
                } else {
                    "POS / Tarjeta - *Ya cobrado previamente*"
                }
            }
            sb.appendLine("💳 *Método de pago:* $paymentText")
        }
        sb.appendLine("────────────────────")
        if (productAmount > 0.0) {
            sb.appendLine("🛍️ *Total Productos / Mercadería:* ${formatMoney(productAmount, cur)}")
        }
        val incText = if (quote.selectedZoneName != null) {
            " (Zona ${quote.selectedZoneName}, radio ${String.format(Locale.US, "%.1f", quote.includedKm)} km)"
        } else if (quote.includedKm > 0) {
            " (incluye ${String.format(Locale.US, "%.1f", quote.includedKm)} km)"
        } else ""
        sb.appendLine("• Tarifa base envío$incText: ${formatMoney(quote.baseMinimumFare, cur)}")
        if (quote.extraKm > 0.01) {
            val extraLabel = if (quote.selectedZoneName != null) "Km extra fuera del radio de zona" else "Tarifa de distancia"
            sb.appendLine(
                "• $extraLabel (${String.format(Locale.US, "%.2f", quote.extraKm)} km × ${formatMoney(quote.costPerExtraKm, cur)}/km): +${formatMoney(quote.extraDistanceCharge, cur)}"
            )
        }
        if (quote.offHoursApplied && quote.offHoursCharge > 0) {
            sb.appendLine("• Recargo fuera de horario / nocturno: +${formatMoney(quote.offHoursCharge, cur)}")
        }
        if (quote.holidayApplied && quote.holidayCharge > 0) {
            sb.appendLine("• Recargo día festivo / fin de semana: +${formatMoney(quote.holidayCharge, cur)}")
        }
        if (quote.rainApplied && quote.rainCharge > 0) {
            sb.appendLine("• Recargo lluvia / alta demanda: +${formatMoney(quote.rainCharge, cur)}")
        }
        if (quote.manualExtraAdjustment != 0.0) {
            val sign = if (quote.manualExtraAdjustment > 0) "+" else ""
            sb.appendLine("• Cargo adicional: $sign${formatMoney(quote.manualExtraAdjustment, cur)}")
        }
        if (quote.customTotalOverride != null) {
            val diff = quote.finalRoundedTotal - quote.customTotalOverride
            if (diff > 0.01) {
                sb.appendLine("• Descuento Tarifa Preferencial: -${formatMoney(diff, cur)}")
            } else if (diff < -0.01) {
                sb.appendLine("• Cargo adicional acordado: +${formatMoney(-diff, cur)}")
            }
        }
        if (productAmount > 0.0) {
            sb.appendLine("🛵 *Subtotal Envío:* ${formatMoney(deliveryFee, cur)}")
            sb.appendLine("────────────────────")
            sb.appendLine("💰 *GRAN TOTAL A PAGAR: ${formatMoney(grandTotal, cur)}*")
        } else {
            sb.appendLine("────────────────────")
            sb.appendLine("💰 *TOTAL ENVÍO: ${formatMoney(deliveryFee, cur)}*")
        }
        val scheduleSummary = buildScheduleSummaryLine(config)
        if (scheduleSummary.isNotBlank()) {
            sb.append("⏰ *Horarios de atención:* $scheduleSummary")
        }
        return sb.toString().trimEnd()
    }

    /**
     * Construye la nota explicativa para liquidación de cuentas del conductor.
     */
    fun buildDriverSettlementNote(
        paymentMethod: PaymentMethodType,
        transferBank: TransferBankType,
        productAmount: Double,
        deliveryFee: Double,
        currencySymbol: String,
        isPaymentPendingOnDelivery: Boolean = false
    ): String {
        val cur = currencySymbol
        val grandTotal = productAmount + deliveryFee
        return when (paymentMethod) {
            PaymentMethodType.CASH -> {
                "💵 Cobro Efectivo: Cobrar ${formatMoney(grandTotal, cur)}. Te descuentas tu envío (${formatMoney(deliveryFee, cur)}). Depositar/liquidar a tienda por mercadería: ${formatMoney(productAmount, cur)} (al final del día o mañana)."
            }
            PaymentMethodType.TRANSFER -> {
                if (isPaymentPendingOnDelivery) {
                    "💳 Transferencia PENDIENTE (${transferBank.displayName}): El cliente transferirá ${formatMoney(grandTotal, cur)} al momento de la entrega. Verificar comprobante con el cliente o confirmación de tienda antes de entregar el paquete. ColorShop te pagará tu envío (${formatMoney(deliveryFee, cur)}) tras confirmarse."
                } else {
                    "💳 Transferencia (${transferBank.displayName}): Cliente ya pagó a tienda (Cobro al cliente: ${formatMoney(0.0, cur)}). Tienda ColorShop te pagará tu envío (${formatMoney(deliveryFee, cur)}) hoy mismo o mañana."
                }
            }
            PaymentMethodType.POS -> {
                if (isPaymentPendingOnDelivery) {
                    "💳 POS Datáfono PENDIENTE: Cobrar al cliente ${formatMoney(grandTotal, cur)} con datáfono de la tienda al entregar. ColorShop te pagará tu tarifa de envío (${formatMoney(deliveryFee, cur)}) hoy mismo o mañana."
                } else {
                    "💳 POS Tarjeta: Cliente ya pagó con datáfono previamente en tienda (Cobro al cliente: ${formatMoney(0.0, cur)}). Tienda ColorShop te pagará tu tarifa de envío (${formatMoney(deliveryFee, cur)}) hoy mismo o mañana."
                }
            }
        }
    }

    /**
     * Genera el mensaje para el motorizado / repartidor:
     * - Sin salida tienda matriz (solo hay una y ya la conocen).
     * - "Ubicación de entrega:" con el enlace directo de Google Maps.
     * - Sin coordenadas exactas en texto.
     * - Detalle de cobro según forma de pago (Efectivo vs. Transferencia/POS) y liquidación de envío y productos.
     */
    fun buildDriverWhatsAppDispatch(
        config: StoreAndPricingConfig,
        destination: FrozenDestination,
        quote: QuoteBreakdown,
        customerNote: String,
        customerPhone: String = "",
        orderComments: String = "",
        approximateDeliveryTime: String = "",
        productAmount: Double = 0.0,
        paymentMethod: PaymentMethodType = PaymentMethodType.CASH,
        transferBank: TransferBankType = TransferBankType.BAC,
        courier: CourierDriver? = null,
        isPaymentPendingOnDelivery: Boolean = false,
        receiptCode: String = ""
    ): String {
        if (config.driverDispatchTemplate.isNotBlank()) {
            val cur = config.currencySymbol
            val deliveryFee = quote.effectiveTotal
            val grandTotal = productAmount + deliveryFee
            val mapUrl = destination.coordinate.toStaticGoogleMapsUrl()
            val driverDestLabelClean = destination.label
                .replace("congelada", "", ignoreCase = true)
                .replace("congelado", "", ignoreCase = true)
                .trim()
                .trimEnd(',', '-')
                .trim()

            val cleanAddress = if (driverDestLabelClean.isNotBlank() && !driverDestLabelClean.startsWith("Ubicación", ignoreCase = true) && !driverDestLabelClean.startsWith("Pin", ignoreCase = true) && !driverDestLabelClean.startsWith("Punto", ignoreCase = true) && !driverDestLabelClean.startsWith("Enlace", ignoreCase = true)) {
                driverDestLabelClean
            } else {
                ""
            }

            val paymentMethodText = when (paymentMethod) {
                PaymentMethodType.CASH -> "Efectivo"
                PaymentMethodType.TRANSFER -> if (isPaymentPendingOnDelivery) {
                    "Transferencia (${transferBank.displayName}) - Pendiente al recibir"
                } else {
                    "Transferencia (${transferBank.displayName}) - Ya acreditada"
                }
                PaymentMethodType.POS -> if (isPaymentPendingOnDelivery) {
                    "POS / Tarjeta - Datáfono al entregar"
                } else {
                    "POS / Tarjeta - Ya cobrado"
                }
            }

            val bankText = if (paymentMethod == PaymentMethodType.TRANSFER) transferBank.displayName else ""

            val settlementText = when (paymentMethod) {
                PaymentMethodType.CASH -> {
                    "Te descuentas tu envío (${formatMoney(deliveryFee, cur)}). Depositar/liquidar a tienda por mercadería: *${formatMoney(productAmount, cur)}* (al final del día o mañana)."
                }
                PaymentMethodType.TRANSFER -> {
                    if (isPaymentPendingOnDelivery) {
                        "El cliente transferirá al momento de la entrega al banco ${transferBank.displayName}. Verificar comprobante con el cliente o confirmación de tienda antes de entregar el paquete. ColorShop te pagará tu envío (${formatMoney(deliveryFee, cur)}) tras confirmarse."
                    } else {
                        "La tienda ColorShop te pagará tu envío (${formatMoney(deliveryFee, cur)}) hoy mismo o mañana."
                    }
                }
                PaymentMethodType.POS -> {
                    if (isPaymentPendingOnDelivery) {
                        "Cobrar con datáfono POS de la tienda al cliente el total de ${formatMoney(grandTotal, cur)}. ColorShop te pagará tu tarifa de envío (${formatMoney(deliveryFee, cur)}) hoy mismo o mañana."
                    } else {
                        "La tienda ColorShop te pagará tu tarifa de envío (${formatMoney(deliveryFee, cur)}) hoy mismo o mañana."
                    }
                }
            }

            val totalText = when (paymentMethod) {
                PaymentMethodType.CASH -> formatMoney(grandTotal, cur)
                PaymentMethodType.TRANSFER -> {
                    if (isPaymentPendingOnDelivery) {
                        "${formatMoney(grandTotal, cur)} (Transferencia PENDIENTE al recibir)"
                    } else {
                        "${formatMoney(0.0, cur)} (Ya acreditado por Transferencia ${transferBank.displayName})"
                    }
                }
                PaymentMethodType.POS -> {
                    if (isPaymentPendingOnDelivery) {
                        "${formatMoney(grandTotal, cur)} (POS Datáfono PENDIENTE)"
                    } else {
                        "${formatMoney(0.0, cur)} (Ya cobrado previamente con POS en tienda)"
                    }
                }
            }

            val variables = mapOf(
                "cliente" to customerNote.trim(),
                "telefono_cliente" to customerPhone.trim(),
                "hora_entrega" to approximateDeliveryTime.trim(),
                "comentarios" to orderComments.trim(),
                "direccion" to cleanAddress,
                "ubicacion_maps" to mapUrl,
                "distancia" to String.format(Locale.US, "%.2f km", quote.effectiveDistanceKm),
                "mercaderia" to if (productAmount > 0.0) formatMoney(productAmount, cur) else "",
                "tarifa_envio" to formatMoney(deliveryFee, cur),
                "total" to totalText,
                "metodo_pago" to paymentMethodText,
                "banco" to bankText,
                "liquidacion_motorizado" to settlementText,
                "repartidor" to (courier?.name ?: ""),
                "placa_repartidor" to (courier?.plate ?: ""),
                "vehiculo_repartidor" to (courier?.let { "${it.vehicleType} ${it.vehicleModel}".trim() } ?: ""),
                "codigo_recibo" to receiptCode.trim()
            )

            return resolveTemplate(config.driverDispatchTemplate, variables)
        }

        val cur = config.currencySymbol
        val deliveryFee = quote.effectiveTotal
        val grandTotal = productAmount + deliveryFee
        val sb = StringBuilder()
        sb.appendLine("📦 *DESPACHO DE ENVÍO - COLORSHOP DELIVERY*")
        if (customerNote.isNotBlank()) {
            sb.appendLine("👤 *Recibe / Cliente:* $customerNote")
        }
        if (customerPhone.isNotBlank()) {
            sb.appendLine("📞 *Teléfono cliente:* $customerPhone")
        }
        if (approximateDeliveryTime.isNotBlank()) {
            sb.appendLine("🕒 *Hora aprox. de entrega:* $approximateDeliveryTime")
        }
        if (orderComments.isNotBlank()) {
            sb.appendLine("📝 *Comentarios:* $orderComments")
        }
        val mapUrl = destination.coordinate.toStaticGoogleMapsUrl()
        val driverDestLabelClean = destination.label
            .replace("congelada", "", ignoreCase = true)
            .replace("congelado", "", ignoreCase = true)
            .trim()
            .trimEnd(',', '-')
            .trim()

        if (driverDestLabelClean.isNotBlank() && !driverDestLabelClean.startsWith("Ubicación", ignoreCase = true) && !driverDestLabelClean.startsWith("Pin", ignoreCase = true) && !driverDestLabelClean.startsWith("Punto", ignoreCase = true) && !driverDestLabelClean.startsWith("Enlace", ignoreCase = true)) {
            sb.appendLine("🏠 *Dirección:* $driverDestLabelClean")
        }
        sb.appendLine("📍 *Ubicación de entrega:*")
        sb.appendLine(mapUrl)
        sb.appendLine("📏 *Distancia:* ${String.format(Locale.US, "%.2f km", quote.effectiveDistanceKm)}")
        sb.appendLine("────────────────────")
        sb.appendLine("💰 *CUENTAS Y LIQUIDACIÓN MOTORIZADO:*")
        if (productAmount > 0.0) {
            sb.appendLine("• Mercadería / Productos: ${formatMoney(productAmount, cur)}")
        }
        sb.appendLine("• Tarifa Servicio Envío: ${formatMoney(deliveryFee, cur)}")
        when (paymentMethod) {
            PaymentMethodType.CASH -> {
                sb.appendLine("• *Cobro al Cliente (Efectivo):* ${formatMoney(grandTotal, cur)}")
                sb.appendLine("👉 *Liquidación:* Te descuentas tu envío (${formatMoney(deliveryFee, cur)}). Depositar/liquidar a tienda por mercadería: *${formatMoney(productAmount, cur)}* (al final del día o mañana).")
            }
            PaymentMethodType.TRANSFER -> {
                if (isPaymentPendingOnDelivery) {
                    sb.appendLine("• *Cobro al Cliente (Transferencia PENDIENTE al recibir):* ${formatMoney(grandTotal, cur)}")
                    sb.appendLine("👉 *Liquidación:* El cliente transferirá al momento de la entrega al banco ${transferBank.displayName}. Verificar comprobante con el cliente o confirmación de tienda antes de entregar el paquete. ColorShop te pagará tu envío (${formatMoney(deliveryFee, cur)}) tras confirmarse.")
                } else {
                    sb.appendLine("• *Cobro al Cliente:* ${formatMoney(0.0, cur)} (Ya acreditado por Transferencia ${transferBank.displayName})")
                    sb.appendLine("👉 *Liquidación:* La tienda ColorShop te pagará tu envío (${formatMoney(deliveryFee, cur)}) hoy mismo o mañana.")
                }
            }
            PaymentMethodType.POS -> {
                if (isPaymentPendingOnDelivery) {
                    sb.appendLine("• *Cobro al Cliente (POS Datáfono PENDIENTE):* ${formatMoney(grandTotal, cur)}")
                    sb.appendLine("👉 *Liquidación:* Cobrar con datáfono POS de la tienda al cliente el total de ${formatMoney(grandTotal, cur)}. ColorShop te pagará tu tarifa de envío (${formatMoney(deliveryFee, cur)}) hoy mismo o mañana.")
                } else {
                    sb.appendLine("• *Cobro al Cliente:* ${formatMoney(0.0, cur)} (Ya cobrado previamente con POS en tienda)")
                    sb.appendLine("👉 *Liquidación:* La tienda ColorShop te pagará tu tarifa de envío (${formatMoney(deliveryFee, cur)}) hoy mismo o mañana.")
                }
            }
        }
        return sb.toString().trimEnd()
    }
}
