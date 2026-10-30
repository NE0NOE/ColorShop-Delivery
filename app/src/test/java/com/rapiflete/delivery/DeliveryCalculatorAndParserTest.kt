package com.rapiflete.delivery

import com.rapiflete.delivery.model.CourierDriver
import com.rapiflete.delivery.model.DistanceCalculationMode
import com.rapiflete.delivery.model.GeoCoordinate
import com.rapiflete.delivery.model.PaymentMethodType
import com.rapiflete.delivery.model.RoundingMode
import com.rapiflete.delivery.model.RouteResult
import com.rapiflete.delivery.model.StoreAndPricingConfig
import com.rapiflete.delivery.model.SurchargeType
import com.rapiflete.delivery.model.TransferBankType
import com.rapiflete.delivery.util.LocationParserAndFreezer
import com.rapiflete.delivery.util.RoutingAndFareCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeliveryCalculatorAndParserTest {

    @Test
    fun `decodes store Plus Code 5M5V+9P Managua accurately`() {
        val frozen = LocationParserAndFreezer.parseDirectOrPlusCode(
            rawInput = "5M5V+9P Managua",
            referenceLat = 12.1364,
            referenceLng = -86.2514
        )
        assertNotNull(frozen)
        assertEquals(12.1584375, frozen!!.coordinate.latitude, 0.001)
        assertEquals(-86.3056875, frozen.coordinate.longitude, 0.001)
        assertTrue(frozen.isFrozen)
    }

    @Test
    fun `parses WhatsApp location link and geo URI and freezes coordinates`() {
        val whatsappNormal = LocationParserAndFreezer.parseDirectOrPlusCode(
            "Ubicación compartida: https://maps.google.com/maps?q=12.127541,-86.264102"
        )
        assertNotNull(whatsappNormal)
        assertEquals(12.127541, whatsappNormal!!.coordinate.latitude, 0.00001)
        assertEquals(-86.264102, whatsappNormal.coordinate.longitude, 0.00001)

        val whatsappLiveGeo = LocationParserAndFreezer.parseDirectOrPlusCode(
            "geo:12.141120,-86.279910?q=12.141120,-86.279910(Cliente+Juan)"
        )
        assertNotNull(whatsappLiveGeo)
        assertEquals(12.141120, whatsappLiveGeo!!.coordinate.latitude, 0.00001)
        assertEquals(-86.279910, whatsappLiveGeo.coordinate.longitude, 0.00001)
    }

    @Test
    fun `calculates fare with minimum included km surcharges and rounding`() {
        val config = StoreAndPricingConfig(
            minimumFare = 60.0,
            includedKmInMinimum = 2.0,
            costPerExtraKm = 15.0,
            offHoursSurchargeType = SurchargeType.FIXED,
            offHoursSurchargeValue = 25.0,
            holidaySurchargeType = SurchargeType.FIXED,
            holidaySurchargeValue = 20.0,
            roundingMode = RoundingMode.MULTIPLE_5
        )

        val store = GeoCoordinate(12.158438, -86.305688)
        val dest = GeoCoordinate(12.127541, -86.264102)
        val route = RouteResult(
            straightLineKm = 5.68,
            adjustedStraightLineKm = 7.38,
            osrmDrivingKm = 6.20,
            osrmDurationMinutes = 14.0,
            routeGeometry = listOf(store, dest),
            usedMode = DistanceCalculationMode.OSRM_DRIVING,
            effectiveDistanceKm = 6.20
        )

        // 6.20 km con 2.0 km incluidos = 4.20 km extra * 15 = 63.0
        // Base 60 + 63 = 123.0 + 25 (nocturno) = 148.0 -> redondeado a múltiplo de 5 = 150.0
        val quote = RoutingAndFareCalculator.calculateQuoteBreakdown(
            route = route,
            modeOverride = DistanceCalculationMode.OSRM_DRIVING,
            config = config,
            applyOffHours = true,
            applyHoliday = false,
            applyRainOrDemand = false
        )

        assertEquals(4.20, quote.extraKm, 0.01)
        assertEquals(63.0, quote.extraDistanceCharge, 0.01)
        assertEquals(148.0, quote.rawTotal, 0.01)
        assertEquals(150.0, quote.finalRoundedTotal, 0.01)

        val frozenDest = LocationParserAndFreezer.parseDirectOrPlusCode(
            "geo:12.127541,-86.264102?q=12.127541,-86.264102(Altamira+Managua)"
        )!!

        // Verificar formato de mensaje al cliente (sin tiempo est, sin (Ruta Calles), con Destino:, Tarifa mínima, Tarifa de distancia)
        val customerMsg = RoutingAndFareCalculator.buildCustomerWhatsAppSummary(
            config = config,
            destination = frozenDest,
            quote = quote.copy(customTotalOverride = 135.0),
            customerNote = "María Pérez"
        )
        assertTrue(customerMsg.contains("📍 *Destino:* Altamira Managua"))
        assertTrue(customerMsg.contains("📏 *Distancia:* 6.20 km"))
        assertTrue(!customerMsg.contains("Ruta Calles"))
        assertTrue(!customerMsg.contains("Tiempo est"))
        assertTrue(customerMsg.contains("• Tarifa mínima envío (incluye 2.0 km): C$ 60"))
        assertTrue(customerMsg.contains("• Tarifa de distancia (4.20 km × C$ 15/km): +C$ 63"))
        assertTrue(customerMsg.contains("💰 *TOTAL ENVÍO: C$ 135*"))

        // Verificar formato de mensaje al motorizado (sin tienda matriz, con Ubicación de entrega, sin coordenadas exactas y sin monto de pago)
        val driverMsg = RoutingAndFareCalculator.buildDriverWhatsAppDispatch(
            config = config,
            destination = frozenDest,
            quote = quote,
            customerNote = "María Pérez"
        )
        assertTrue(driverMsg.contains("📍 *Ubicación de entrega:*"))
        assertTrue(!driverMsg.contains("Salida"))
        assertTrue(!driverMsg.contains("Coordenadas exactas"))
        assertTrue(!driverMsg.contains("Pago estimado"))

        // Verificar extracción de teléfono desde texto de WhatsApp y visibilidad de motorizado + horarios
        val extractedPhone = LocationParserAndFreezer.extractPhoneNumberFromText(
            "Hola, mi número es +505 8844-1122, aquí mi ubicación https://maps.google.com/maps?q=12.127541,-86.264102"
        )
        assertEquals("8844-1122", extractedPhone)

        val courierVisible = CourierDriver(
            id = "c1",
            name = "Carlos Mendoza",
            initials = "CM",
            plate = "M 123-456",
            phone = "+505 8765-4321",
            includeNameInCustomerMessage = true,
            includePhoneInCustomerMessage = false
        )
        val customerMsgWithCourierAndSchedule = RoutingAndFareCalculator.buildCustomerWhatsAppSummary(
            config = config,
            destination = frozenDest,
            quote = quote,
            customerNote = "María Pérez",
            customerPhone = "+505 8844-1122",
            orderComments = "Casa portón negro",
            approximateDeliveryTime = "3:30 PM",
            courier = courierVisible,
            paymentMethod = PaymentMethodType.TRANSFER,
            transferBank = TransferBankType.LAFISE
        )
        assertTrue(customerMsgWithCourierAndSchedule.contains("🛵 *Motorizado asignado:* Carlos Mendoza"))
        assertTrue(!customerMsgWithCourierAndSchedule.contains("+505 8765-4321"))
        assertTrue(customerMsgWithCourierAndSchedule.contains("🕒 *Hora aprox. de entrega:* 3:30 PM"))
        assertTrue(customerMsgWithCourierAndSchedule.contains("💳 *Método de pago:* Transferencia (Lafise)"))
        assertTrue(customerMsgWithCourierAndSchedule.contains("⏰ *Horarios de atención:*"))
    }

    @Test
    fun `driver settlement accounting accurately excludes cash and accumulates electronic debt across days`() {
        val config = StoreAndPricingConfig()

        // 1. Pedido en efectivo de hoy: el motorizado cobra y se descuenta su tarifa (isDriverPaid = true)
        val cashOrderToday = com.rapiflete.delivery.model.SavedQuoteRecord(
            id = "rec-1",
            dateIso = "2026-10-29",
            timestamp = "10:00 AM",
            customerNote = "Cliente Efectivo",
            courierName = "Walter",
            paymentMethod = PaymentMethodType.CASH,
            deliveryFee = 120.0,
            productAmount = 500.0,
            totalAmount = 620.0,
            isDriverPaid = true,
            driverPaidAt = "Auto-descontado en efectivo"
        )

        // 2. Pedido de ayer por transferencia: la tienda le debe la tarifa de delivery al motorizado
        val transferOrderYesterday = com.rapiflete.delivery.model.SavedQuoteRecord(
            id = "rec-2",
            dateIso = "2026-10-28",
            timestamp = "03:15 PM",
            customerNote = "Cliente Ayer Transfer",
            courierName = "Walter",
            paymentMethod = PaymentMethodType.TRANSFER,
            deliveryFee = 150.0,
            productAmount = 800.0,
            totalAmount = 950.0,
            isDriverPaid = false,
            driverPaidAt = ""
        )

        // 3. Pedido de hoy por POS: la tienda le debe la tarifa de delivery al motorizado
        val posOrderToday = com.rapiflete.delivery.model.SavedQuoteRecord(
            id = "rec-3",
            dateIso = "2026-10-29",
            timestamp = "11:30 AM",
            customerNote = "Cliente POS",
            courierName = "Walter",
            paymentMethod = PaymentMethodType.POS,
            deliveryFee = 100.0,
            productAmount = 300.0,
            totalAmount = 400.0,
            isDriverPaid = false,
            driverPaidAt = ""
        )

        val history = listOf(cashOrderToday, transferOrderYesterday, posOrderToday)

        // Filtrar envíos con saldo pendiente a pagar al motorizado:
        // Debe excluir efectivo (porque es auto-descontado) y solo sumar los no pagados de transfer/POS
        val unpaidOrders = history.filter {
            !it.isDriverPaid && it.paymentMethod != PaymentMethodType.CASH
        }

        assertEquals(2, unpaidOrders.size)
        val totalOwedAllTime = unpaidOrders.sumOf { it.deliveryFee }
        // 150 (ayer) + 100 (hoy) = 250
        assertEquals(250.0, totalOwedAllTime, 0.001)

        val unpaidPastDays = unpaidOrders.filter { it.dateIso != "2026-10-29" }
        assertEquals(1, unpaidPastDays.size)
        assertEquals(150.0, unpaidPastDays.sumOf { it.deliveryFee }, 0.001)

        val unpaidToday = unpaidOrders.filter { it.dateIso == "2026-10-29" }
        assertEquals(1, unpaidToday.size)
        assertEquals(100.0, unpaidToday.sumOf { it.deliveryFee }, 0.001)

        // Al marcar el pedido de ayer como pagado:
        val updatedYesterday = transferOrderYesterday.copy(isDriverPaid = true, driverPaidAt = "2026-10-29 12:00 PM")
        val updatedHistory = listOf(cashOrderToday, updatedYesterday, posOrderToday)
        val remainingUnpaid = updatedHistory.filter { !it.isDriverPaid && it.paymentMethod != PaymentMethodType.CASH }

        assertEquals(1, remainingUnpaid.size)
        assertEquals(100.0, remainingUnpaid.sumOf { it.deliveryFee }, 0.001)
    }

    @Test
    fun `share output format enum is supported in config`() {
        val configDefault = StoreAndPricingConfig()
        assertEquals(com.rapiflete.delivery.model.ShareOutputFormat.WHATSAPP_TEXT, configDefault.defaultShareFormat)

        val configImage = configDefault.copy(defaultShareFormat = com.rapiflete.delivery.model.ShareOutputFormat.IMAGE_RECEIPT)
        assertEquals(com.rapiflete.delivery.model.ShareOutputFormat.IMAGE_RECEIPT, configImage.defaultShareFormat)
    }

    @Test
    fun `preferential fare calculation accurately reduces 175 to 150 cordobas`() {
        val config = StoreAndPricingConfig(
            preferentialThreshold = 150.0,
            preferentialDiscountPercent = 15.0,
            roundingMode = RoundingMode.MULTIPLE_5
        )

        // 1. Caso exacto del dueño: 175 C$ calculado se reduce a 150 C$
        val preferentialFor175 = RoutingAndFareCalculator.calculatePreferentialFare(
            calculatedTotal = 175.0,
            threshold = config.preferentialThreshold,
            discountPercent = config.preferentialDiscountPercent,
            roundingMode = config.roundingMode
        )
        assertEquals(150.0, preferentialFor175, 0.001)

        // 2. Si la tarifa no excede el umbral (ej. 150 C$ o 120 C$), se mantiene intacta
        val standard150 = RoutingAndFareCalculator.calculatePreferentialFare(
            calculatedTotal = 150.0,
            threshold = config.preferentialThreshold,
            discountPercent = config.preferentialDiscountPercent,
            roundingMode = config.roundingMode
        )
        assertEquals(150.0, standard150, 0.001)

        val standard120 = RoutingAndFareCalculator.calculatePreferentialFare(
            calculatedTotal = 120.0,
            threshold = config.preferentialThreshold,
            discountPercent = config.preferentialDiscountPercent,
            roundingMode = config.roundingMode
        )
        assertEquals(120.0, standard120, 0.001)

        // 3. Ruta muy lejana (ej. 200 C$): 200 * 0.85 = 170 C$ (ahorro de 30 C$)
        val preferentialFor200 = RoutingAndFareCalculator.calculatePreferentialFare(
            calculatedTotal = 200.0,
            threshold = config.preferentialThreshold,
            discountPercent = config.preferentialDiscountPercent,
            roundingMode = config.roundingMode
        )
        assertEquals(170.0, preferentialFor200, 0.001)
    }

    @Test
    fun `customer whatsapp message reflects preferential rate discount line`() {
        val config = StoreAndPricingConfig(currencySymbol = "C$")
        val frozenDest = LocationParserAndFreezer.parseDirectOrPlusCode(
            "geo:12.127541,-86.264102?q=12.127541,-86.264102(Tipitapa+Centro)"
        )!!

        val store = GeoCoordinate(12.158438, -86.305688)
        val route = RouteResult(
            straightLineKm = 14.5,
            adjustedStraightLineKm = 18.8,
            osrmDrivingKm = 15.5,
            osrmDurationMinutes = 35.0,
            routeGeometry = listOf(store, frozenDest.coordinate),
            usedMode = DistanceCalculationMode.OSRM_DRIVING,
            effectiveDistanceKm = 15.5
        )

        // Tarifa mínima 40 (2 km) + 13.5 km * 10 = 175 C$
        val rawQuote = RoutingAndFareCalculator.calculateQuoteBreakdown(
            route = route,
            modeOverride = DistanceCalculationMode.OSRM_DRIVING,
            config = config,
            applyOffHours = false,
            applyHoliday = false,
            applyRainOrDemand = false
        )
        assertEquals(175.0, rawQuote.finalRoundedTotal, 0.01)

        // Aplicar tarifa preferencial acordada de C$ 150
        val quoteWithPreferential = rawQuote.copy(customTotalOverride = 150.0)
        assertEquals(150.0, quoteWithPreferential.effectiveTotal, 0.01)

        val customerMsg = RoutingAndFareCalculator.buildCustomerWhatsAppSummary(
            config = config,
            destination = frozenDest,
            quote = quoteWithPreferential,
            customerNote = "Cliente Ruta Lejana"
        )

        assertTrue(customerMsg.contains("• Tarifa mínima envío (incluye 2.0 km): C$ 40"))
        assertTrue(customerMsg.contains("• Tarifa de distancia (13.50 km × C$ 10/km): +C$ 135"))
        assertTrue(customerMsg.contains("• Descuento Tarifa Preferencial: -C$ 25"))
        assertTrue(customerMsg.contains("💰 *TOTAL ENVÍO: C$ 150*"))
    }

    @Test
    fun `zone detector automatically identifies Ciudad Sandino Ticuantepe and Tipitapa and excludes urban Managua`() {
        val testZones = listOf(
            com.rapiflete.delivery.model.SpecialZoneRule(id = "z1", name = "Ciudad Sandino", basePrice = 100.0),
            com.rapiflete.delivery.model.SpecialZoneRule(id = "z2", name = "Ticuantepe", basePrice = 120.0),
            com.rapiflete.delivery.model.SpecialZoneRule(id = "z3", name = "Tipitapa", basePrice = 120.0)
        )

        // 1. Ciudad Sandino por coordenadas y por texto
        val sandinoByCoord = com.rapiflete.delivery.util.ZoneDetector.detectZone(
            coord = GeoCoordinate(12.161, -86.345),
            addressText = "Barrio Bello Amanecer",
            availableZones = testZones
        )
        assertNotNull(sandinoByCoord)
        assertEquals("Ciudad Sandino", sandinoByCoord!!.name)
        assertEquals(100.0, sandinoByCoord.basePrice, 0.01)

        val sandinoByText = com.rapiflete.delivery.util.ZoneDetector.detectZone(
            coord = GeoCoordinate(12.136, -86.251), // coord genérica
            addressText = "Ciudad Sandino, zona 4",
            availableZones = testZones
        )
        assertNotNull(sandinoByText)
        assertEquals("Ciudad Sandino", sandinoByText!!.name)

        // 2. Ticuantepe por coordenadas y por texto
        val ticuantepeByCoord = com.rapiflete.delivery.util.ZoneDetector.detectZone(
            coord = GeoCoordinate(12.023, -86.205),
            addressText = "Calle Principal",
            availableZones = testZones
        )
        assertNotNull(ticuantepeByCoord)
        assertEquals("Ticuantepe", ticuantepeByCoord!!.name)
        assertEquals(120.0, ticuantepeByCoord.basePrice, 0.01)

        // 3. Tipitapa por coordenadas y por texto
        val tipitapaByCoord = com.rapiflete.delivery.util.ZoneDetector.detectZone(
            coord = GeoCoordinate(12.198, -86.096),
            addressText = "Empalme de Tipitapa",
            availableZones = testZones
        )
        assertNotNull(tipitapaByCoord)
        assertEquals("Tipitapa", tipitapaByCoord!!.name)
        assertEquals(120.0, tipitapaByCoord.basePrice, 0.01)

        // 4. Barrio dentro de Managua urbana (Altamira, El Riguero, Linda Vista) -> Tarifa normal (null)
        val managuaUrban = com.rapiflete.delivery.util.ZoneDetector.detectZone(
            coord = GeoCoordinate(12.1364, -86.2514),
            addressText = "El Riguero, Distrito I, Managua",
            availableZones = testZones
        )
        org.junit.Assert.assertNull(managuaUrban)
    }

    @Test
    fun `whatsapp messages never contain congelada and always include clickable Google Maps link`() {
        val config = StoreAndPricingConfig()
        val dest = com.rapiflete.delivery.model.FrozenDestination(
            coordinate = GeoCoordinate(12.145678, -86.265432),
            label = "Ubicación de WhatsApp congelada",
            sourceRawText = "https://maps.google.com/?q=12.145678,-86.265432",
            capturedAtTime = "12:00:00"
        )
        val route = RouteResult(
            straightLineKm = 4.0,
            adjustedStraightLineKm = 5.2,
            osrmDrivingKm = 4.8,
            osrmDurationMinutes = 12.0,
            routeGeometry = listOf(config.storeCoordinate, dest.coordinate),
            usedMode = DistanceCalculationMode.OSRM_DRIVING,
            effectiveDistanceKm = 4.8
        )
        val quote = RoutingAndFareCalculator.calculateQuoteBreakdown(
            route = route,
            modeOverride = DistanceCalculationMode.OSRM_DRIVING,
            config = config,
            applyOffHours = false,
            applyHoliday = false,
            applyRainOrDemand = false
        )

        val customerMsg = RoutingAndFareCalculator.buildCustomerWhatsAppSummary(
            config = config,
            destination = dest,
            quote = quote,
            customerNote = "Cliente WhatsApp"
        )

        // Comprobar que NO contiene 'congelad'
        org.junit.Assert.assertFalse(customerMsg.contains("congelada", ignoreCase = true))
        org.junit.Assert.assertFalse(customerMsg.contains("congelado", ignoreCase = true))

        // Comprobar que incluye el enlace clickeable de Google Maps
        assertTrue(customerMsg.contains("🗺️ *Ubicación en mapa:* https://maps.google.com/?q=12.145678,-86.265432"))

        val driverMsg = RoutingAndFareCalculator.buildDriverWhatsAppDispatch(
            config = config,
            destination = dest,
            quote = quote,
            customerNote = "Cliente WhatsApp"
        )
        org.junit.Assert.assertFalse(driverMsg.contains("congelada", ignoreCase = true))
        assertTrue(driverMsg.contains("https://maps.google.com/?q=12.145678,-86.265432"))
    }
}
