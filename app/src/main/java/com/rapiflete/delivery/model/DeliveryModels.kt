package com.rapiflete.delivery.model

import java.util.Locale

data class GeoCoordinate(
    val latitude: Double,
    val longitude: Double
) {
    fun format(decimals: Int = 6): String {
        return String.format(Locale.US, "%.${decimals}f, %.${decimals}f", latitude, longitude)
    }

    fun toStaticGoogleMapsUrl(): String {
        return String.format(Locale.US, "https://maps.google.com/?q=%.6f,%.6f", latitude, longitude)
    }

    fun toGeoUri(label: String = "Punto de entrega"): String {
        val encodedLabel = java.net.URLEncoder.encode(label, "UTF-8")
        return String.format(
            Locale.US,
            "geo:%.6f,%.6f?q=%.6f,%.6f(%s)",
            latitude,
            longitude,
            latitude,
            longitude,
            encodedLabel
        )
    }
}

enum class DistanceCalculationMode(val displayName: String, val shortLabel: String) {
    OSRM_DRIVING("Ruta real por calles (OSRM)", "Ruta Calles"),
    STRAIGHT_LINE_ADJUSTED("Línea recta + factor de ajuste", "Línea Recta")
}

enum class RoundingMode(val displayName: String, val step: Int) {
    NONE("Exacto (sin redondeo decimal)", 1),
    MULTIPLE_5("Redondear a múltiplos de 5", 5),
    MULTIPLE_10("Redondear a múltiplos de 10", 10)
}

enum class SurchargeType(val displayName: String) {
    FIXED("Fijo"),
    PERCENTAGE("Porcentaje (%)")
}

enum class LocationSourceType(val badgeText: String) {
    WHATSAPP_SHARE("Compartido desde WhatsApp / Maps"),
    SHORT_LINK("Enlace de Google Maps resuelto"),
    PLUS_CODE("Plus Code (Código Abierto)"),
    COORDINATES("Coordenadas directas"),
    ADDRESS_SEARCH("Búsqueda de dirección"),
    MAP_TAP("Pin seleccionado en mapa")
}

enum class CourierStatus(val label: String) {
    AVAILABLE("Disponibles"),
    ON_ROUTE("En Ruta"),
    OFF_DUTY("Fuera de Turno")
}

enum class OrderStatus(val label: String, val emoji: String) {
    EN_RUTA("En Ruta", "🛵"),
    ENTREGADO("Entregado", "✅"),
    CANCELADO("Cancelado", "❌")
}

enum class ShareOutputFormat(val label: String, val shortLabel: String) {
    WHATSAPP_TEXT("Texto para WhatsApp", "Texto"),
    IMAGE_RECEIPT("Imagen de Recibo Digital", "Imagen")
}

enum class PaymentMethodType(val label: String, val badgeLabel: String) {
    CASH("Efectivo", "Cobro Efectivo"),
    TRANSFER("Transferencia", "Transferencia Bancaria"),
    POS("POS / Tarjeta", "Pagar por POS")
}

enum class TransferBankType(val displayName: String) {
    BAC("BAC"),
    LAFISE("Lafise"),
    BANPRO("Banpro"),
    FICOHSA("Ficohsa")
}

data class CourierDriver(
    val id: String,
    val name: String,
    val initials: String,
    val badgeTag: String = "Tienda",
    val vehicleType: String = "Motocicleta",
    val vehicleModel: String = "Suzuki GN 125",
    val plate: String,
    val phone: String,
    val documentId: String = "",
    val shift: String = "Mañana (8a-4p)",
    val rating: Double = 4.9,
    val status: CourierStatus = CourierStatus.AVAILABLE,
    val statusDetail: String = "Disponible en tienda matriz",
    val includeNameInCustomerMessage: Boolean = true,
    val includePhoneInCustomerMessage: Boolean = true
)

data class SpecialZoneRule(
    val id: String,
    val name: String,
    val iconEmoji: String = "🏙️",
    val basePrice: Double,
    val appliesExtraKm: Boolean = false
)

data class CustomSurchargeRule(
    val id: String,
    val name: String,
    val type: SurchargeType = SurchargeType.FIXED,
    val value: Double = 15.0
)

data class FrozenDestination(
    val coordinate: GeoCoordinate,
    val label: String,
    val sourceRawText: String,
    val capturedAtTime: String,
    val isFrozen: Boolean = true,
    val sourceType: LocationSourceType = LocationSourceType.WHATSAPP_SHARE,
    val detectedZoneMunicipality: String? = null
)

data class StoreAndPricingConfig(
    val storeName: String = "Tienda Matriz",
    val storePlusCode: String = "5M5V+9P Managua",
    val storeMapsUrl: String = "https://maps.app.goo.gl/mv39EgWrVBGNy1GJ7",
    val storeLat: Double = 12.158438,
    val storeLng: Double = -86.305688,
    val currencySymbol: String = "C$",
    val minimumFare: Double = 40.0,
    val includedKmInMinimum: Double = 2.0,
    val costPerExtraKm: Double = 10.0,
    val defaultCalculationMode: DistanceCalculationMode = DistanceCalculationMode.OSRM_DRIVING,
    val straightLineFactor: Double = 1.30,
    val businessStartHour: Int = 9,
    val businessEndHour: Int = 17,
    val weekdayOpenText: String = "09:00",
    val weekdayCloseText: String = "05:00",
    val saturdayEnabled: Boolean = true,
    val saturdayOpenText: String = "09:00",
    val saturdayCloseText: String = "12:00",
    val sundayEnabled: Boolean = false,
    val sundayOpenText: String = "10:00",
    val sundayCloseText: String = "02:00",
    val autoDetectOffHours: Boolean = true,
    val offHoursSurchargeType: SurchargeType = SurchargeType.FIXED,
    val offHoursSurchargeValue: Double = 25.0,
    val holidaySurchargeType: SurchargeType = SurchargeType.FIXED,
    val holidaySurchargeValue: Double = 20.0,
    val rainOrHighDemandSurchargeType: SurchargeType = SurchargeType.FIXED,
    val rainOrHighDemandSurchargeValue: Double = 20.0,
    val roundingMode: RoundingMode = RoundingMode.MULTIPLE_5,
    val defaultShareFormat: ShareOutputFormat = ShareOutputFormat.WHATSAPP_TEXT,
    val preferentialThreshold: Double = 150.0,
    val preferentialDiscountPercent: Double = 15.0,
    val preferentialAutoPrompt: Boolean = true
) {
    val storeCoordinate: GeoCoordinate
        get() = GeoCoordinate(storeLat, storeLng)
}

data class RouteResult(
    val straightLineKm: Double,
    val adjustedStraightLineKm: Double,
    val osrmDrivingKm: Double?,
    val osrmDurationMinutes: Double?,
    val routeGeometry: List<GeoCoordinate>,
    val usedMode: DistanceCalculationMode,
    val effectiveDistanceKm: Double,
    val fallbackReason: String? = null
)

data class QuoteBreakdown(
    val effectiveDistanceKm: Double,
    val straightLineKm: Double,
    val adjustedStraightLineKm: Double,
    val osrmDrivingKm: Double?,
    val durationMinutes: Double?,
    val usedMode: DistanceCalculationMode,
    val baseMinimumFare: Double,
    val includedKm: Double,
    val extraKm: Double,
    val costPerExtraKm: Double,
    val extraDistanceCharge: Double,
    val subtotalBeforeSurcharges: Double,
    val offHoursApplied: Boolean,
    val offHoursCharge: Double,
    val holidayApplied: Boolean,
    val holidayCharge: Double,
    val rainApplied: Boolean,
    val rainCharge: Double,
    val manualExtraAdjustment: Double,
    val selectedZoneName: String? = null,
    val rawTotal: Double,
    val finalRoundedTotal: Double,
    val roundingDifference: Double,
    val customTotalOverride: Double? = null
) {
    val effectiveTotal: Double
        get() = customTotalOverride ?: finalRoundedTotal
}

data class SavedQuoteRecord(
    val id: String,
    val dateIso: String = "",
    val timestamp: String,
    val customerNote: String,
    val customerPhone: String = "",
    val orderComments: String = "",
    val approximateDeliveryTime: String = "",
    val receiptPhotoUri: String = "",
    val courierName: String = "Walter",
    val courierInitials: String = "W",
    val courierPhone: String = "",
    val paymentMethod: PaymentMethodType = PaymentMethodType.CASH,
    val transferBank: String = "BAC",
    val paymentDetail: String = "Pago contra entrega",
    val receiptId: String = "#CS-88421",
    val destinationLat: Double = 0.0,
    val destinationLng: Double = 0.0,
    val destinationLabel: String = "",
    val capturedAtTime: String = "",
    val distanceKm: Double = 0.0,
    val durationMinutes: Int = 14,
    val modeLabel: String = "",
    val surchargesSummary: String = "",
    val productAmount: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val totalAmount: Double = 0.0,
    val currencySymbol: String = "C$",
    val orderStatus: OrderStatus = OrderStatus.EN_RUTA,
    val driverSettlementNote: String = "",
    val isDriverPaid: Boolean = false,
    val driverPaidAt: String = ""
)
