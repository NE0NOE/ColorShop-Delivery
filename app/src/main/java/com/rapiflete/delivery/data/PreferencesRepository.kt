package com.rapiflete.delivery.data

import android.content.Context
import android.content.SharedPreferences
import com.rapiflete.delivery.model.CourierDriver
import com.rapiflete.delivery.model.CourierStatus
import com.rapiflete.delivery.model.DistanceCalculationMode
import com.rapiflete.delivery.model.PaymentMethodType
import com.rapiflete.delivery.model.RoundingMode
import com.rapiflete.delivery.model.SavedQuoteRecord
import com.rapiflete.delivery.model.SpecialZoneRule
import com.rapiflete.delivery.model.StoreAndPricingConfig
import com.rapiflete.delivery.model.SurchargeType
import org.json.JSONArray
import org.json.JSONObject

class PreferencesRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("colorshop_delivery_prefs", Context.MODE_PRIVATE)

    fun loadConfig(): StoreAndPricingConfig {
        val defaults = StoreAndPricingConfig()
        val modeName = prefs.getString("defaultCalculationMode", defaults.defaultCalculationMode.name)
        val roundingName = prefs.getString("roundingMode", defaults.roundingMode.name)
        val offType = prefs.getString("offHoursSurchargeType", defaults.offHoursSurchargeType.name)
        val holType = prefs.getString("holidaySurchargeType", defaults.holidaySurchargeType.name)
        val rainType = prefs.getString("rainOrHighDemandSurchargeType", defaults.rainOrHighDemandSurchargeType.name)

        return StoreAndPricingConfig(
            storeName = prefs.getString("storeName", defaults.storeName) ?: defaults.storeName,
            storePlusCode = prefs.getString("storePlusCode", defaults.storePlusCode) ?: defaults.storePlusCode,
            storeMapsUrl = prefs.getString("storeMapsUrl", defaults.storeMapsUrl) ?: defaults.storeMapsUrl,
            storeLat = prefs.getString("storeLat", defaults.storeLat.toString())?.toDoubleOrNull() ?: defaults.storeLat,
            storeLng = prefs.getString("storeLng", defaults.storeLng.toString())?.toDoubleOrNull() ?: defaults.storeLng,
            currencySymbol = prefs.getString("currencySymbol", defaults.currencySymbol) ?: defaults.currencySymbol,
            minimumFare = prefs.getString("minimumFare", defaults.minimumFare.toString())?.toDoubleOrNull() ?: defaults.minimumFare,
            includedKmInMinimum = prefs.getString("includedKmInMinimum", defaults.includedKmInMinimum.toString())?.toDoubleOrNull() ?: defaults.includedKmInMinimum,
            costPerExtraKm = prefs.getString("costPerExtraKm", defaults.costPerExtraKm.toString())?.toDoubleOrNull() ?: defaults.costPerExtraKm,
            defaultCalculationMode = runCatching { DistanceCalculationMode.valueOf(modeName!!) }.getOrDefault(defaults.defaultCalculationMode),
            straightLineFactor = prefs.getString("straightLineFactor", defaults.straightLineFactor.toString())?.toDoubleOrNull() ?: defaults.straightLineFactor,
            businessStartHour = prefs.getInt("businessStartHour", defaults.businessStartHour),
            businessEndHour = prefs.getInt("businessEndHour", defaults.businessEndHour),
            weekdayOpenText = prefs.getString("weekdayOpenText", defaults.weekdayOpenText) ?: defaults.weekdayOpenText,
            weekdayCloseText = prefs.getString("weekdayCloseText", defaults.weekdayCloseText) ?: defaults.weekdayCloseText,
            saturdayEnabled = prefs.getBoolean("saturdayEnabled", defaults.saturdayEnabled),
            saturdayOpenText = prefs.getString("saturdayOpenText", defaults.saturdayOpenText) ?: defaults.saturdayOpenText,
            saturdayCloseText = prefs.getString("saturdayCloseText", defaults.saturdayCloseText) ?: defaults.saturdayCloseText,
            sundayEnabled = prefs.getBoolean("sundayEnabled", defaults.sundayEnabled),
            sundayOpenText = prefs.getString("sundayOpenText", defaults.sundayOpenText) ?: defaults.sundayOpenText,
            sundayCloseText = prefs.getString("sundayCloseText", defaults.sundayCloseText) ?: defaults.sundayCloseText,
            autoDetectOffHours = prefs.getBoolean("autoDetectOffHours", defaults.autoDetectOffHours),
            offHoursSurchargeType = runCatching { SurchargeType.valueOf(offType!!) }.getOrDefault(defaults.offHoursSurchargeType),
            offHoursSurchargeValue = prefs.getString("offHoursSurchargeValue", defaults.offHoursSurchargeValue.toString())?.toDoubleOrNull() ?: defaults.offHoursSurchargeValue,
            holidaySurchargeType = runCatching { SurchargeType.valueOf(holType!!) }.getOrDefault(defaults.holidaySurchargeType),
            holidaySurchargeValue = prefs.getString("holidaySurchargeValue", defaults.holidaySurchargeValue.toString())?.toDoubleOrNull() ?: defaults.holidaySurchargeValue,
            rainOrHighDemandSurchargeType = runCatching { SurchargeType.valueOf(rainType!!) }.getOrDefault(defaults.rainOrHighDemandSurchargeType),
            rainOrHighDemandSurchargeValue = prefs.getString("rainOrHighDemandSurchargeValue", defaults.rainOrHighDemandSurchargeValue.toString())?.toDoubleOrNull() ?: defaults.rainOrHighDemandSurchargeValue,
            roundingMode = runCatching { RoundingMode.valueOf(roundingName!!) }.getOrDefault(defaults.roundingMode)
        )
    }

    fun saveConfig(config: StoreAndPricingConfig) {
        prefs.edit().apply {
            putString("storeName", config.storeName)
            putString("storePlusCode", config.storePlusCode)
            putString("storeMapsUrl", config.storeMapsUrl)
            putString("storeLat", config.storeLat.toString())
            putString("storeLng", config.storeLng.toString())
            putString("currencySymbol", config.currencySymbol)
            putString("minimumFare", config.minimumFare.toString())
            putString("includedKmInMinimum", config.includedKmInMinimum.toString())
            putString("costPerExtraKm", config.costPerExtraKm.toString())
            putString("defaultCalculationMode", config.defaultCalculationMode.name)
            putString("straightLineFactor", config.straightLineFactor.toString())
            putInt("businessStartHour", config.businessStartHour)
            putInt("businessEndHour", config.businessEndHour)
            putString("weekdayOpenText", config.weekdayOpenText)
            putString("weekdayCloseText", config.weekdayCloseText)
            putBoolean("saturdayEnabled", config.saturdayEnabled)
            putString("saturdayOpenText", config.saturdayOpenText)
            putString("saturdayCloseText", config.saturdayCloseText)
            putBoolean("sundayEnabled", config.sundayEnabled)
            putString("sundayOpenText", config.sundayOpenText)
            putString("sundayCloseText", config.sundayCloseText)
            putBoolean("autoDetectOffHours", config.autoDetectOffHours)
            putString("offHoursSurchargeType", config.offHoursSurchargeType.name)
            putString("offHoursSurchargeValue", config.offHoursSurchargeValue.toString())
            putString("holidaySurchargeType", config.holidaySurchargeType.name)
            putString("holidaySurchargeValue", config.holidaySurchargeValue.toString())
            putString("rainOrHighDemandSurchargeType", config.rainOrHighDemandSurchargeType.name)
            putString("rainOrHighDemandSurchargeValue", config.rainOrHighDemandSurchargeValue.toString())
            putString("roundingMode", config.roundingMode.name)
            apply()
        }
    }

    fun loadCouriers(): List<CourierDriver> {
        val raw = prefs.getString("couriers_json", null) ?: return defaultCouriers()
        return try {
            val arr = JSONArray(raw)
            val list = ArrayList<CourierDriver>(arr.length())
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    CourierDriver(
                        id = obj.optString("id"),
                        name = obj.optString("name"),
                        initials = obj.optString("initials", "RP"),
                        badgeTag = obj.optString("badgeTag", "Tienda"),
                        vehicleType = obj.optString("vehicleType", "Motocicleta"),
                        vehicleModel = obj.optString("vehicleModel", "Moto 125cc"),
                        plate = obj.optString("plate"),
                        phone = obj.optString("phone"),
                        documentId = obj.optString("documentId", ""),
                        shift = obj.optString("shift", "Mañana (8a-4p)"),
                        rating = obj.optDouble("rating", 4.9),
                        status = runCatching { CourierStatus.valueOf(obj.optString("status")) }.getOrDefault(CourierStatus.AVAILABLE),
                        statusDetail = obj.optString("statusDetail", "Disponible en tienda matriz"),
                        includeNameInCustomerMessage = obj.optBoolean("includeNameInCustomerMessage", true),
                        includePhoneInCustomerMessage = obj.optBoolean("includePhoneInCustomerMessage", true)
                    )
                )
            }
            if (list.isEmpty()) defaultCouriers() else list
        } catch (_: Exception) {
            defaultCouriers()
        }
    }

    fun saveCouriers(couriers: List<CourierDriver>) {
        val arr = JSONArray()
        couriers.forEach { c ->
            arr.put(
                JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("initials", c.initials)
                    put("badgeTag", c.badgeTag)
                    put("vehicleType", c.vehicleType)
                    put("vehicleModel", c.vehicleModel)
                    put("plate", c.plate)
                    put("phone", c.phone)
                    put("documentId", c.documentId)
                    put("shift", c.shift)
                    put("rating", c.rating)
                    put("status", c.status.name)
                    put("statusDetail", c.statusDetail)
                    put("includeNameInCustomerMessage", c.includeNameInCustomerMessage)
                    put("includePhoneInCustomerMessage", c.includePhoneInCustomerMessage)
                }
            )
        }
        prefs.edit().putString("couriers_json", arr.toString()).apply()
    }

    fun loadZones(): List<SpecialZoneRule> {
        val raw = prefs.getString("zones_json", null) ?: return defaultZones()
        return try {
            val arr = JSONArray(raw)
            val list = ArrayList<SpecialZoneRule>(arr.length())
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    SpecialZoneRule(
                        id = obj.optString("id"),
                        name = obj.optString("name"),
                        iconEmoji = obj.optString("iconEmoji", "🏙️"),
                        basePrice = obj.optDouble("basePrice", 100.0),
                        appliesExtraKm = obj.optBoolean("appliesExtraKm", false)
                    )
                )
            }
            list
        } catch (_: Exception) {
            defaultZones()
        }
    }

    fun saveZones(zones: List<SpecialZoneRule>) {
        val arr = JSONArray()
        zones.forEach { z ->
            arr.put(
                JSONObject().apply {
                    put("id", z.id)
                    put("name", z.name)
                    put("iconEmoji", z.iconEmoji)
                    put("basePrice", z.basePrice)
                    put("appliesExtraKm", z.appliesExtraKm)
                }
            )
        }
        prefs.edit().putString("zones_json", arr.toString()).apply()
    }

    fun loadHistory(): List<SavedQuoteRecord> {
        val raw = prefs.getString("quote_history_json", null) ?: return emptyList()
        val todayIso = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        return try {
            val arr = JSONArray(raw)
            val list = ArrayList<SavedQuoteRecord>(arr.length())
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    SavedQuoteRecord(
                        id = obj.optString("id"),
                        dateIso = obj.optString("dateIso", todayIso).ifBlank { todayIso },
                        timestamp = obj.optString("timestamp"),
                        customerNote = obj.optString("customerNote"),
                        customerPhone = obj.optString("customerPhone", ""),
                        orderComments = obj.optString("orderComments", ""),
                        approximateDeliveryTime = obj.optString("approximateDeliveryTime", ""),
                        receiptPhotoUri = obj.optString("receiptPhotoUri", ""),
                        courierName = obj.optString("courierName", "Marcos Morales"),
                        courierInitials = obj.optString("courierInitials", "MM"),
                        courierPhone = obj.optString("courierPhone", ""),
                        paymentMethod = runCatching { PaymentMethodType.valueOf(obj.optString("paymentMethod")) }.getOrDefault(PaymentMethodType.CASH),
                        transferBank = obj.optString("transferBank", "BAC"),
                        paymentDetail = obj.optString("paymentDetail", "Pago contra entrega"),
                        receiptId = obj.optString("receiptId", "#CS-88421"),
                        destinationLat = obj.optDouble("destinationLat"),
                        destinationLng = obj.optDouble("destinationLng"),
                        destinationLabel = obj.optString("destinationLabel"),
                        capturedAtTime = obj.optString("capturedAtTime", ""),
                        distanceKm = obj.optDouble("distanceKm"),
                        durationMinutes = obj.optInt("durationMinutes", 14),
                        modeLabel = obj.optString("modeLabel"),
                        surchargesSummary = obj.optString("surchargesSummary"),
                        totalAmount = obj.optDouble("totalAmount"),
                        currencySymbol = obj.optString("currencySymbol", "C$")
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveHistory(records: List<SavedQuoteRecord>) {
        val arr = JSONArray()
        records.take(300).forEach { record ->
            val obj = JSONObject().apply {
                put("id", record.id)
                put("dateIso", record.dateIso)
                put("timestamp", record.timestamp)
                put("customerNote", record.customerNote)
                put("customerPhone", record.customerPhone)
                put("orderComments", record.orderComments)
                put("approximateDeliveryTime", record.approximateDeliveryTime)
                put("receiptPhotoUri", record.receiptPhotoUri)
                put("courierName", record.courierName)
                put("courierInitials", record.courierInitials)
                put("courierPhone", record.courierPhone)
                put("paymentMethod", record.paymentMethod.name)
                put("transferBank", record.transferBank)
                put("paymentDetail", record.paymentDetail)
                put("receiptId", record.receiptId)
                put("destinationLat", record.destinationLat)
                put("destinationLng", record.destinationLng)
                put("destinationLabel", record.destinationLabel)
                put("capturedAtTime", record.capturedAtTime)
                put("distanceKm", record.distanceKm)
                put("durationMinutes", record.durationMinutes)
                put("modeLabel", record.modeLabel)
                put("surchargesSummary", record.surchargesSummary)
                put("totalAmount", record.totalAmount)
                put("currencySymbol", record.currencySymbol)
            }
            arr.put(obj)
        }
        prefs.edit().putString("quote_history_json", arr.toString()).apply()
    }

    private fun defaultCouriers(): List<CourierDriver> = listOf(
        CourierDriver(
            id = "c1",
            name = "Marcos Morales",
            initials = "MM",
            badgeTag = "Senior",
            vehicleType = "Motocicleta",
            vehicleModel = "Suzuki GN 125",
            plate = "M-28491",
            phone = "8823-4567",
            documentId = "001-290492-0044K",
            shift = "Mañana (8a-4p)",
            rating = 4.9,
            status = CourierStatus.ON_ROUTE,
            statusDetail = "En ruta • Pedido #1042"
        ),
        CourierDriver(
            id = "c2",
            name = "Carlos Mendoza",
            initials = "CM",
            badgeTag = "Tienda",
            vehicleType = "Motocicleta",
            vehicleModel = "Pulsar NS 200",
            plate = "M-10492",
            phone = "8432-1100",
            documentId = "001-140895-0012A",
            shift = "Completo",
            rating = 5.0,
            status = CourierStatus.AVAILABLE,
            statusDetail = "Disponible en tienda matriz"
        ),
        CourierDriver(
            id = "c3",
            name = "Javier Peña",
            initials = "JP",
            badgeTag = "Urbano",
            vehicleType = "Motocicleta",
            vehicleModel = "Yamaha YBR",
            plate = "M-39210",
            phone = "7654-9988",
            documentId = "001-031198-0088M",
            shift = "Tarde/Noche",
            rating = 4.8,
            status = CourierStatus.AVAILABLE,
            statusDetail = "Disponible para despacho"
        )
    )

    private fun defaultZones(): List<SpecialZoneRule> = listOf(
        SpecialZoneRule(
            id = "z1",
            name = "Ciudad Sandino",
            iconEmoji = "🏙️",
            basePrice = 100.0,
            appliesExtraKm = false
        ),
        SpecialZoneRule(
            id = "z2",
            name = "Ticuantepe",
            iconEmoji = "🌲",
            basePrice = 120.0,
            appliesExtraKm = true
        )
    )

    private fun defaultSampleHistory(): List<SavedQuoteRecord> = listOf(
        SavedQuoteRecord(
            id = "h1",
            timestamp = "12:20 PM",
            customerNote = "María Fernanda Pérez",
            customerPhone = "+505 8899-1122",
            courierName = "Marcos Morales",
            courierInitials = "MM",
            paymentMethod = PaymentMethodType.CASH,
            paymentDetail = "Vuelto: C$ 38.00 (Billete C$ 100)",
            receiptId = "#CP-88421",
            destinationLat = 12.132800,
            destinationLng = -86.269400,
            destinationLabel = "Rotonda Cristo Rey 2c al sur, Casa #14",
            capturedAtTime = "12:18 PM",
            distanceKm = 4.2,
            durationMinutes = 14,
            modeLabel = "Ruta Calles",
            surchargesSummary = "Entregar en portón negro, llamar antes de llegar.",
            totalAmount = 62.0,
            currencySymbol = "C$"
        ),
        SavedQuoteRecord(
            id = "h2",
            timestamp = "11:05 AM",
            customerNote = "Andrea Morales",
            customerPhone = "+505 7744-8833",
            courierName = "Javier Peña",
            courierInitials = "JP",
            paymentMethod = PaymentMethodType.TRANSFER,
            paymentDetail = "Ref: #TR-9942",
            receiptId = "#TR-9942",
            destinationLat = 12.144500,
            destinationLng = -86.273100,
            destinationLabel = "Plaza Inter 1c al lago, Módulo B-4",
            capturedAtTime = "11:00 AM",
            distanceKm = 6.8,
            durationMinutes = 22,
            modeLabel = "Ruta Calles",
            surchargesSummary = "Dejar con recepción de oficinas. Cliente pagó anticipado con BAC Móvil.",
            totalAmount = 85.0,
            currencySymbol = "C$"
        ),
        SavedQuoteRecord(
            id = "h3",
            timestamp = "09:40 AM",
            customerNote = "Dr. Javier Ruiz",
            customerPhone = "+505 8433-2211",
            courierName = "Carlos Mendoza",
            courierInitials = "CM",
            paymentMethod = PaymentMethodType.POS,
            paymentDetail = "Llevar POS (Tarjeta)",
            receiptId = "#POS-3019",
            destinationLat = 12.138900,
            destinationLng = -86.286200,
            destinationLabel = "Hospital Militar 3c abajo",
            capturedAtTime = "09:35 AM",
            distanceKm = 2.1,
            durationMinutes = 8,
            modeLabel = "Ruta Calles",
            surchargesSummary = "Entrega de documentos urgentes, firma recibida en recepción.",
            totalAmount = 45.0,
            currencySymbol = "C$"
        )
    )
}
