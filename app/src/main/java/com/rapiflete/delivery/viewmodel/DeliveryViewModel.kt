package com.rapiflete.delivery.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rapiflete.delivery.data.PreferencesRepository
import com.rapiflete.delivery.model.CourierDriver
import com.rapiflete.delivery.model.CourierStatus
import com.rapiflete.delivery.model.DistanceCalculationMode
import com.rapiflete.delivery.model.FrozenDestination
import com.rapiflete.delivery.model.GeoCoordinate
import com.rapiflete.delivery.model.LocationSourceType
import com.rapiflete.delivery.model.OrderStatus
import com.rapiflete.delivery.model.PaymentMethodType
import com.rapiflete.delivery.model.QuoteBreakdown
import com.rapiflete.delivery.model.RouteResult
import com.rapiflete.delivery.model.SavedQuoteRecord
import com.rapiflete.delivery.model.SpecialZoneRule
import com.rapiflete.delivery.model.StoreAndPricingConfig
import com.rapiflete.delivery.model.TransferBankType
import com.rapiflete.delivery.util.LocationParserAndFreezer
import com.rapiflete.delivery.util.RoutingAndFareCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

data class DeliveryUiState(
    val config: StoreAndPricingConfig = StoreAndPricingConfig(),
    val inputLinkOrText: String = "",
    val customerNote: String = "",
    val customerPhone: String = "",
    val orderComments: String = "",
    val approximateDeliveryTime: String = "",
    val receiptPhotoUri: String = "",
    val frozenDestination: FrozenDestination? = null,
    val selectedCalculationMode: DistanceCalculationMode = DistanceCalculationMode.OSRM_DRIVING,
    val routeResult: RouteResult? = null,
    val quoteBreakdown: QuoteBreakdown? = null,
    val applyOffHours: Boolean = false,
    val applyHoliday: Boolean = false,
    val applyRainOrDemand: Boolean = false,
    val manualAdjustment: Double = 0.0,
    val customTotalOverride: Double? = null,
    val selectedZone: SpecialZoneRule? = null,
    val selectedPaymentMethod: PaymentMethodType = PaymentMethodType.CASH,
    val selectedTransferBank: TransferBankType = TransferBankType.BAC,
    val couriers: List<CourierDriver> = emptyList(),
    val selectedCourier: CourierDriver? = null,
    val zones: List<SpecialZoneRule> = emptyList(),
    val productAmount: Double = 0.0,
    val productAmountInput: String = "",
    val isLoadingRouteOrLink: Boolean = false,
    val showPreferentialRateDialog: Boolean = false,
    val suggestedPreferentialRate: Double = 0.0,
    val isPreferentialApplied: Boolean = false,
    val statusBannerMessage: String? = null,
    val errorMessage: String? = null,
    val history: List<SavedQuoteRecord> = emptyList()
)

class DeliveryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PreferencesRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(DeliveryUiState())
    val uiState: StateFlow<DeliveryUiState> = _uiState.asStateFlow()

    init {
        val savedConfig = repository.loadConfig()
        val savedHistory = repository.loadHistory()
        val savedCouriers = repository.loadCouriers()
        val savedZones = repository.loadZones()

        _uiState.update {
            it.copy(
                config = savedConfig,
                selectedCalculationMode = savedConfig.defaultCalculationMode,
                frozenDestination = null,
                routeResult = null,
                quoteBreakdown = null,
                couriers = savedCouriers,
                selectedCourier = savedCouriers.firstOrNull(),
                zones = savedZones,
                history = savedHistory
            )
        }
    }

    fun onInputTextChanged(newText: String) {
        _uiState.update { it.copy(inputLinkOrText = newText, errorMessage = null) }
    }

    fun onCustomerNoteChanged(newNote: String) {
        _uiState.update { it.copy(customerNote = newNote) }
    }

    fun onCustomerPhoneChanged(newPhone: String) {
        _uiState.update { it.copy(customerPhone = newPhone) }
    }

    fun onProductAmountChanged(newInput: String) {
        val parsed = newInput.trim().toDoubleOrNull() ?: 0.0
        _uiState.update {
            it.copy(
                productAmountInput = newInput,
                productAmount = parsed
            )
        }
    }

    fun onOrderCommentsChanged(newComments: String) {
        _uiState.update { it.copy(orderComments = newComments) }
    }

    fun onApproximateDeliveryTimeChanged(newTime: String) {
        _uiState.update { it.copy(approximateDeliveryTime = newTime) }
    }

    fun onReceiptPhotoSelected(uriString: String) {
        _uiState.update { it.copy(receiptPhotoUri = uriString) }
    }

    fun selectCourier(courier: CourierDriver) {
        _uiState.update { it.copy(selectedCourier = courier) }
    }

    fun updateCourierVisibilityFlags(
        courierId: String,
        includeName: Boolean,
        includePhone: Boolean
    ) {
        val updated = _uiState.value.couriers.map { c ->
            if (c.id == courierId) {
                c.copy(
                    includeNameInCustomerMessage = includeName,
                    includePhoneInCustomerMessage = includePhone
                )
            } else {
                c
            }
        }
        repository.saveCouriers(updated)
        _uiState.update { state ->
            val updatedSelected = updated.find { it.id == state.selectedCourier?.id } ?: updated.firstOrNull()
            state.copy(
                couriers = updated,
                selectedCourier = updatedSelected
            )
        }
    }

    fun updateCourier(courier: CourierDriver) {
        val updated = _uiState.value.couriers.map {
            if (it.id == courier.id) courier else it
        }
        repository.saveCouriers(updated)
        _uiState.update { state ->
            val updatedSelected = if (state.selectedCourier?.id == courier.id) {
                courier
            } else {
                state.selectedCourier ?: updated.firstOrNull()
            }
            state.copy(
                couriers = updated,
                selectedCourier = updatedSelected
            )
        }
    }

    fun deleteCourier(courierId: String) {
        val updated = _uiState.value.couriers.filterNot { it.id == courierId }
        repository.saveCouriers(updated)
        _uiState.update { state ->
            val updatedSelected = if (state.selectedCourier?.id == courierId) {
                updated.firstOrNull()
            } else {
                state.selectedCourier
            }
            state.copy(
                couriers = updated,
                selectedCourier = updatedSelected
            )
        }
    }

    fun selectPaymentMethod(method: PaymentMethodType) {
        _uiState.update { it.copy(selectedPaymentMethod = method) }
    }

    fun selectTransferBank(bank: TransferBankType) {
        _uiState.update { it.copy(selectedTransferBank = bank) }
    }

    fun selectSpecialZone(zone: SpecialZoneRule?) {
        _uiState.update { state ->
            state.copy(selectedZone = zone, customTotalOverride = null).recalculateQuote()
        }
    }

    fun setCustomTotalOverride(amount: Double?) {
        _uiState.update { state ->
            val currentQuote = state.quoteBreakdown
            state.copy(
                customTotalOverride = amount,
                isPreferentialApplied = if (amount == null) false else state.isPreferentialApplied,
                quoteBreakdown = currentQuote?.copy(customTotalOverride = amount)
            )
        }
    }

    fun dismissPreferentialRateDialog() {
        _uiState.update { it.copy(showPreferentialRateDialog = false) }
    }

    fun openPreferentialRateDialog() {
        _uiState.update { state ->
            val total = state.quoteBreakdown?.finalRoundedTotal ?: 0.0
            val cfg = state.config
            val suggested = if (total > 0.0) {
                RoutingAndFareCalculator.calculatePreferentialFare(
                    calculatedTotal = total,
                    threshold = cfg.preferentialThreshold,
                    discountPercent = cfg.preferentialDiscountPercent,
                    roundingMode = cfg.roundingMode
                )
            } else 0.0
            state.copy(
                suggestedPreferentialRate = suggested,
                showPreferentialRateDialog = true
            )
        }
    }

    fun applyPreferentialRate(customAmount: Double? = null) {
        _uiState.update { state ->
            val currentQuote = state.quoteBreakdown
            val amountToApply = customAmount ?: state.suggestedPreferentialRate
            state.copy(
                customTotalOverride = amountToApply,
                isPreferentialApplied = true,
                showPreferentialRateDialog = false,
                quoteBreakdown = currentQuote?.copy(customTotalOverride = amountToApply)
            )
        }
    }

    fun removePreferentialRate() {
        _uiState.update { state ->
            val currentQuote = state.quoteBreakdown
            state.copy(
                customTotalOverride = null,
                isPreferentialApplied = false,
                showPreferentialRateDialog = false,
                quoteBreakdown = currentQuote?.copy(customTotalOverride = null)
            )
        }
    }

    fun addCourier(
        name: String,
        phone: String,
        documentId: String,
        vehicleType: String,
        plate: String,
        shift: String,
        includeNameInMsg: Boolean = true,
        includePhoneInMsg: Boolean = true
    ) {
        val cleanName = name.trim().ifBlank { "Nuevo Repartidor" }
        val words = cleanName.split(" ").filter { it.isNotBlank() }
        val initials = if (words.size >= 2) {
            "${words[0].first().uppercaseChar()}${words[1].first().uppercaseChar()}"
        } else {
            cleanName.take(2).uppercase(Locale.getDefault())
        }

        val newCourier = CourierDriver(
            id = UUID.randomUUID().toString(),
            name = cleanName,
            initials = initials,
            badgeTag = "Nuevo",
            vehicleType = vehicleType,
            vehicleModel = vehicleType,
            plate = plate.trim().ifBlank { "M-00000" }.uppercase(Locale.getDefault()),
            phone = phone.trim().ifBlank { "8800-0000" },
            documentId = documentId.trim().uppercase(Locale.getDefault()),
            shift = shift,
            rating = 5.0,
            status = CourierStatus.AVAILABLE,
            statusDetail = "Disponible en tienda matriz",
            includeNameInCustomerMessage = includeNameInMsg,
            includePhoneInCustomerMessage = includePhoneInMsg
        )

        val updated = listOf(newCourier) + _uiState.value.couriers
        repository.saveCouriers(updated)
        _uiState.update {
            it.copy(
                couriers = updated,
                selectedCourier = newCourier,
                statusBannerMessage = null
            )
        }
    }

    fun addSpecialZone(name: String, basePrice: Double, appliesExtraKm: Boolean) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return
        val newZone = SpecialZoneRule(
            id = UUID.randomUUID().toString(),
            name = cleanName,
            iconEmoji = "📍",
            basePrice = basePrice,
            appliesExtraKm = appliesExtraKm
        )
        val updated = _uiState.value.zones + newZone
        repository.saveZones(updated)
        _uiState.update { it.copy(zones = updated) }
    }

    fun deleteSpecialZone(id: String) {
        val updated = _uiState.value.zones.filterNot { it.id == id }
        repository.saveZones(updated)
        _uiState.update { state ->
            val nextSelected = if (state.selectedZone?.id == id) null else state.selectedZone
            state.copy(zones = updated, selectedZone = nextSelected).recalculateQuote()
        }
    }

    fun processAndFreezeLocationInput(rawText: String, fromExternalIntent: Boolean = false) {
        val clean = rawText.trim()
        if (clean.isEmpty()) {
            _uiState.update {
                it.copy(errorMessage = "Pega un enlace de ubicación de WhatsApp/Maps, Plus Code o dirección.")
            }
            return
        }

        // Si el mensaje compartido trae un número telefónico de Nicaragua, extraerlo automáticamente
        val detectedPhone = LocationParserAndFreezer.extractPhoneNumberFromText(clean)

        _uiState.update {
            it.copy(
                inputLinkOrText = clean,
                customerPhone = if (!detectedPhone.isNullOrBlank() && it.customerPhone.isBlank()) detectedPhone else it.customerPhone,
                isLoadingRouteOrLink = true,
                errorMessage = null,
                statusBannerMessage = null
            )
        }

        viewModelScope.launch {
            val cfg = _uiState.value.config
            val parseResult = LocationParserAndFreezer.resolveAndFreezeInput(
                rawInput = clean,
                referenceLat = cfg.storeLat,
                referenceLng = cfg.storeLng
            )

            parseResult.fold(
                onSuccess = { frozenDest ->
                    computeRouteAndQuoteForDestination(
                        destination = frozenDest,
                        bannerMsg = null
                    )
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isLoadingRouteOrLink = false,
                            statusBannerMessage = null,
                            errorMessage = err.message ?: "No se pudo interpretar la ubicación."
                        )
                    }
                }
            )
        }
    }

    fun onMapDestinationSelected(lat: Double, lng: Double, freezeImmediately: Boolean = true) {
        val coord = GeoCoordinate(lat, lng)
        val newDest = FrozenDestination(
            coordinate = coord,
            label = "Ubicación seleccionada en Managua",
            sourceRawText = coord.format(6),
            capturedAtTime = LocationParserAndFreezer.currentTimeFormatted(),
            isFrozen = freezeImmediately,
            sourceType = LocationSourceType.MAP_TAP
        )

        viewModelScope.launch {
            computeRouteAndQuoteForDestination(
                destination = newDest,
                bannerMsg = null
            )
        }
    }

    fun toggleFreezeLock() {
        val current = _uiState.value.frozenDestination ?: return
        val nowFrozen = !current.isFrozen
        val updated = current.copy(
            isFrozen = nowFrozen,
            capturedAtTime = if (nowFrozen) LocationParserAndFreezer.currentTimeFormatted() else current.capturedAtTime
        )
        _uiState.update {
            it.copy(
                frozenDestination = updated,
                statusBannerMessage = null
            )
        }
    }

    fun setCalculationMode(mode: DistanceCalculationMode) {
        _uiState.update { state ->
            state.copy(selectedCalculationMode = mode, customTotalOverride = null).recalculateQuote()
        }
    }

    fun toggleOffHours(enabled: Boolean) {
        _uiState.update { state ->
            state.copy(applyOffHours = enabled, customTotalOverride = null).recalculateQuote()
        }
    }

    fun toggleHoliday(enabled: Boolean) {
        _uiState.update { state ->
            state.copy(applyHoliday = enabled, customTotalOverride = null).recalculateQuote()
        }
    }

    fun toggleRainOrDemand(enabled: Boolean) {
        _uiState.update { state ->
            state.copy(applyRainOrDemand = enabled, customTotalOverride = null).recalculateQuote()
        }
    }

    fun adjustManualExtra(delta: Double) {
        _uiState.update { state ->
            val next = state.manualAdjustment + delta
            state.copy(manualAdjustment = next, customTotalOverride = null).recalculateQuote()
        }
    }

    fun setManualExtraValue(amount: Double) {
        _uiState.update { state ->
            state.copy(manualAdjustment = amount, customTotalOverride = null).recalculateQuote()
        }
    }

    fun resetManualExtra() {
        _uiState.update { state ->
            state.copy(manualAdjustment = 0.0, customTotalOverride = null).recalculateQuote()
        }
    }

    fun updateStoreAndPricingConfig(newConfig: StoreAndPricingConfig) {
        repository.saveConfig(newConfig)
        _uiState.update { state ->
            state.copy(
                config = newConfig,
                selectedCalculationMode = newConfig.defaultCalculationMode,
                customTotalOverride = null,
                statusBannerMessage = null
            ).recalculateQuote()
        }
    }

    fun saveCurrentQuoteToHistory(): Boolean {
        val state = _uiState.value
        val dest = state.frozenDestination ?: return false
        val quote = state.quoteBreakdown ?: return false
        val courier = state.selectedCourier ?: state.couriers.firstOrNull()
        val deliveryFee = quote.effectiveTotal
        val productAmt = state.productAmount
        val totalAmount = productAmt + deliveryFee

        val settlementNote = RoutingAndFareCalculator.buildDriverSettlementNote(
            paymentMethod = state.selectedPaymentMethod,
            transferBank = state.selectedTransferBank,
            productAmount = productAmt,
            deliveryFee = deliveryFee,
            currencySymbol = state.config.currencySymbol
        )

        val surchargesList = mutableListOf<String>()
        if (productAmt > 0) surchargesList.add("Mercadería: ${state.config.currencySymbol}${productAmt.toInt()}")
        if (quote.offHoursApplied) surchargesList.add("Recargo nocturno")
        if (quote.holidayApplied) surchargesList.add("Día festivo")
        if (quote.rainApplied) surchargesList.add("Lluvia/Demanda")
        if (quote.manualExtraAdjustment != 0.0) surchargesList.add("Recargo manual")
        if (quote.customTotalOverride != null) {
            val diff = quote.finalRoundedTotal - quote.customTotalOverride
            if (diff > 0.01) {
                surchargesList.add("Tarifa preferencial (-${state.config.currencySymbol}${diff.toInt()})")
            } else {
                surchargesList.add("Tarifa editada")
            }
        }
        state.selectedZone?.let { surchargesList.add("Zona: ${it.name}") }

        val now = Date()
        val dateIsoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)

        val record = SavedQuoteRecord(
            id = UUID.randomUUID().toString(),
            dateIso = dateIsoFormat.format(now),
            timestamp = timeFormat.format(now),
            customerNote = state.customerNote.ifBlank { "Cliente ColorShop" },
            customerPhone = state.customerPhone,
            orderComments = state.orderComments,
            approximateDeliveryTime = state.approximateDeliveryTime,
            receiptPhotoUri = state.receiptPhotoUri,
            courierName = courier?.name ?: "Walter",
            courierInitials = courier?.initials ?: "W",
            courierPhone = courier?.phone ?: "",
            paymentMethod = state.selectedPaymentMethod,
            transferBank = state.selectedTransferBank.displayName,
            paymentDetail = when (state.selectedPaymentMethod) {
                PaymentMethodType.CASH -> "Cobro Efectivo: ${state.config.currencySymbol} ${totalAmount.toInt()}"
                PaymentMethodType.TRANSFER -> "Transf. ${state.selectedTransferBank.displayName} (${state.config.currencySymbol} ${totalAmount.toInt()})"
                PaymentMethodType.POS -> "POS Tarjeta (${state.config.currencySymbol} ${totalAmount.toInt()})"
            },
            receiptId = "#CS-${(10000..99999).random()}",
            destinationLat = dest.coordinate.latitude,
            destinationLng = dest.coordinate.longitude,
            destinationLabel = dest.label,
            capturedAtTime = dest.capturedAtTime,
            distanceKm = quote.effectiveDistanceKm,
            durationMinutes = (quote.durationMinutes?.roundToInt() ?: 14).coerceAtLeast(1),
            modeLabel = quote.usedMode.shortLabel,
            surchargesSummary = if (surchargesList.isEmpty()) "Entrega estándar a domicilio" else surchargesList.joinToString(" · "),
            productAmount = productAmt,
            deliveryFee = deliveryFee,
            totalAmount = totalAmount,
            currencySymbol = state.config.currencySymbol,
            orderStatus = OrderStatus.EN_RUTA,
            driverSettlementNote = settlementNote,
            isDriverPaid = state.selectedPaymentMethod == PaymentMethodType.CASH,
            driverPaidAt = if (state.selectedPaymentMethod == PaymentMethodType.CASH) "Auto-descontado en efectivo" else ""
        )

        val updatedHistory = listOf(record) + state.history
        repository.saveHistory(updatedHistory)
        _uiState.update {
            it.copy(
                history = updatedHistory,
                statusBannerMessage = null
            )
        }
        return true
    }

    fun updateOrderStatus(recordId: String, newStatus: OrderStatus) {
        val updated = _uiState.value.history.map {
            if (it.id == recordId) it.copy(orderStatus = newStatus) else it
        }
        repository.saveHistory(updated)
        _uiState.update { it.copy(history = updated) }
    }

    fun toggleDriverPaymentStatus(recordId: String) {
        val timeFormat = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.US)
        val nowIso = timeFormat.format(Date())
        val updated = _uiState.value.history.map { record ->
            if (record.id == recordId) {
                val nowPaid = !record.isDriverPaid
                record.copy(
                    isDriverPaid = nowPaid,
                    driverPaidAt = if (nowPaid) nowIso else ""
                )
            } else {
                record
            }
        }
        repository.saveHistory(updated)
        _uiState.update { it.copy(history = updated) }
    }

    fun loadRecordFromHistory(record: SavedQuoteRecord) {
        val restoredDest = FrozenDestination(
            coordinate = GeoCoordinate(record.destinationLat, record.destinationLng),
            label = record.destinationLabel,
            sourceRawText = "${record.destinationLat}, ${record.destinationLng}",
            capturedAtTime = record.capturedAtTime.ifBlank { record.timestamp },
            isFrozen = true,
            sourceType = LocationSourceType.WHATSAPP_SHARE
        )
        _uiState.update {
            it.copy(
                customerNote = record.customerNote,
                customerPhone = record.customerPhone,
                orderComments = record.orderComments,
                approximateDeliveryTime = record.approximateDeliveryTime,
                receiptPhotoUri = record.receiptPhotoUri,
                inputLinkOrText = restoredDest.coordinate.toStaticGoogleMapsUrl()
            )
        }
        viewModelScope.launch {
            computeRouteAndQuoteForDestination(
                destination = restoredDest,
                bannerMsg = null
            )
        }
    }

    fun deleteHistoryRecord(id: String) {
        val updated = _uiState.value.history.filterNot { it.id == id }
        repository.saveHistory(updated)
        _uiState.update { it.copy(history = updated) }
    }

    fun clearAllHistory() {
        repository.saveHistory(emptyList())
        _uiState.update { it.copy(history = emptyList()) }
    }

    private suspend fun computeRouteAndQuoteForDestination(
        destination: FrozenDestination,
        bannerMsg: String?
    ) {
        val stateSnapshot = _uiState.value
        val cfg = stateSnapshot.config
        val route = RoutingAndFareCalculator.calculateRoute(
            origin = cfg.storeCoordinate,
            destination = destination.coordinate,
            preferredMode = stateSnapshot.selectedCalculationMode,
            straightLineFactor = cfg.straightLineFactor
        )

        val effectiveConfig = stateSnapshot.selectedZone?.let { zone ->
            cfg.copy(
                minimumFare = zone.basePrice,
                costPerExtraKm = if (zone.appliesExtraKm) cfg.costPerExtraKm else 0.0
            )
        } ?: cfg

        val quote = RoutingAndFareCalculator.calculateQuoteBreakdown(
            route = route,
            modeOverride = stateSnapshot.selectedCalculationMode,
            config = effectiveConfig,
            applyOffHours = stateSnapshot.applyOffHours,
            applyHoliday = stateSnapshot.applyHoliday,
            applyRainOrDemand = stateSnapshot.applyRainOrDemand,
            manualExtraAdjustment = stateSnapshot.manualAdjustment
        ).copy(
            selectedZoneName = stateSnapshot.selectedZone?.name,
            customTotalOverride = null
        )

        val shouldPrompt = cfg.preferentialAutoPrompt &&
            quote.finalRoundedTotal > cfg.preferentialThreshold &&
            stateSnapshot.customTotalOverride == null

        val suggestedRate = if (quote.finalRoundedTotal > cfg.preferentialThreshold) {
            RoutingAndFareCalculator.calculatePreferentialFare(
                calculatedTotal = quote.finalRoundedTotal,
                threshold = cfg.preferentialThreshold,
                discountPercent = cfg.preferentialDiscountPercent,
                roundingMode = cfg.roundingMode
            )
        } else {
            0.0
        }

        _uiState.update {
            it.copy(
                frozenDestination = destination,
                routeResult = route,
                quoteBreakdown = quote,
                customTotalOverride = null,
                isPreferentialApplied = false,
                showPreferentialRateDialog = shouldPrompt,
                suggestedPreferentialRate = suggestedRate,
                isLoadingRouteOrLink = false,
                statusBannerMessage = bannerMsg,
                errorMessage = null
            )
        }
    }

    private fun DeliveryUiState.recalculateQuote(): DeliveryUiState {
        val currentRoute = routeResult ?: return this
        val effectiveConfig = selectedZone?.let { zone ->
            config.copy(
                minimumFare = zone.basePrice,
                costPerExtraKm = if (zone.appliesExtraKm) config.costPerExtraKm else 0.0
            )
        } ?: config

        val updatedQuote = RoutingAndFareCalculator.calculateQuoteBreakdown(
            route = currentRoute,
            modeOverride = selectedCalculationMode,
            config = effectiveConfig,
            applyOffHours = applyOffHours,
            applyHoliday = applyHoliday,
            applyRainOrDemand = applyRainOrDemand,
            manualExtraAdjustment = manualAdjustment
        ).copy(
            selectedZoneName = selectedZone?.name,
            customTotalOverride = customTotalOverride
        )

        val suggestedRate = if (updatedQuote.finalRoundedTotal > config.preferentialThreshold) {
            RoutingAndFareCalculator.calculatePreferentialFare(
                calculatedTotal = updatedQuote.finalRoundedTotal,
                threshold = config.preferentialThreshold,
                discountPercent = config.preferentialDiscountPercent,
                roundingMode = config.roundingMode
            )
        } else {
            0.0
        }

        val stillPreferential = customTotalOverride != null &&
            customTotalOverride < updatedQuote.finalRoundedTotal &&
            isPreferentialApplied

        return copy(
            quoteBreakdown = updatedQuote,
            suggestedPreferentialRate = suggestedRate,
            isPreferentialApplied = stillPreferential
        )
    }
}
