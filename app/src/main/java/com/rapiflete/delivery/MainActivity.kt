package com.rapiflete.delivery

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import com.rapiflete.delivery.ui.components.ColorShopSplashScreen
import com.rapiflete.delivery.ui.screens.CalculatorScreen
import com.rapiflete.delivery.ui.screens.CouriersScreen
import com.rapiflete.delivery.ui.screens.HistoryScreen
import com.rapiflete.delivery.ui.screens.SettingsScreen
import com.rapiflete.delivery.ui.theme.ColorShopSurface
import com.rapiflete.delivery.ui.theme.RapiFleteTheme
import com.rapiflete.delivery.viewmodel.DeliveryViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: DeliveryViewModel by viewModels()
    private var isExternalIntent by mutableStateOf(false)

    @OptIn(ExperimentalLayoutApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isExternalIntent = isExternalLocationIntent(intent)
        handleIncomingShareOrMapIntent(intent)

        setContent {
            RapiFleteTheme {
                val uiState by viewModel.uiState.collectAsState()
                val focusManager = LocalFocusManager.current
                var showSplash by rememberSaveable { mutableStateOf(!isExternalIntent) }
                var currentTab by rememberSaveable { mutableIntStateOf(0) } // 0=Cotizar, 1=Historial, 2=Configuración, 3=Repartidores

                val pagerState = rememberPagerState(
                    initialPage = currentTab.coerceIn(0, 2),
                    pageCount = { 3 }
                )

                // Sincronización cuando cambia currentTab programáticamente (botones, atajos, historial)
                LaunchedEffect(currentTab) {
                    if (currentTab in 0..2 && pagerState.currentPage != currentTab && pagerState.targetPage != currentTab) {
                        focusManager.clearFocus()
                        pagerState.animateScrollToPage(currentTab)
                    }
                }

                // Sincronización cuando el usuario desliza con gestos el HorizontalPager sin interferir con el touch
                LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
                    if (!pagerState.isScrollInProgress && currentTab != 3 && currentTab != pagerState.currentPage) {
                        focusManager.clearFocus()
                        currentTab = pagerState.currentPage
                    }
                }

                BackHandler(enabled = currentTab == 3) {
                    currentTab = 2
                }

                if (showSplash) {
                    ColorShopSplashScreen(
                        isExternalIntent = isExternalIntent,
                        onFinished = { showSplash = false }
                    )
                } else {
                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .imePadding(),
                        containerColor = ColorShopSurface
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(ColorShopSurface)
                                .padding(innerPadding)
                        ) {
                            if (currentTab == 3) {
                                CouriersScreen(
                                    couriers = uiState.couriers,
                                    onAddCourier = viewModel::addCourier,
                                    onUpdateCourierVisibility = viewModel::updateCourierVisibilityFlags,
                                    onUpdateCourier = viewModel::updateCourier,
                                    onDeleteCourier = viewModel::deleteCourier,
                                    onBack = { currentTab = 2 }
                                )
                            } else {
                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier.fillMaxSize(),
                                    beyondViewportPageCount = 1
                                ) { page ->
                                    when (page) {
                                        0 -> CalculatorScreen(
                                            uiState = uiState,
                                            onInputChanged = viewModel::onInputTextChanged,
                                            onCustomerNoteChanged = viewModel::onCustomerNoteChanged,
                                            onCustomerPhoneChanged = viewModel::onCustomerPhoneChanged,
                                            onProductAmountChanged = viewModel::onProductAmountChanged,
                                            onOrderCommentsChanged = viewModel::onOrderCommentsChanged,
                                            onApproximateDeliveryTimeChanged = viewModel::onApproximateDeliveryTimeChanged,
                                            onReceiptPhotoSelected = viewModel::onReceiptPhotoSelected,
                                            onFreezeInputLocation = { raw ->
                                                viewModel.processAndFreezeLocationInput(raw, fromExternalIntent = false)
                                            },
                                            onMapTapLocation = { lat, lng ->
                                                viewModel.onMapDestinationSelected(lat, lng, freezeImmediately = true)
                                            },
                                            onMapLongPressFreeze = { lat, lng ->
                                                viewModel.onMapDestinationSelected(lat, lng, freezeImmediately = true)
                                            },
                                            onToggleFreezeLock = viewModel::toggleFreezeLock,
                                            onSelectCalculationMode = viewModel::setCalculationMode,
                                            onToggleOffHours = viewModel::toggleOffHours,
                                            onToggleHoliday = viewModel::toggleHoliday,
                                            onToggleRain = viewModel::toggleRainOrDemand,
                                            onAdjustManualExtra = viewModel::adjustManualExtra,
                                            onResetManualExtra = viewModel::resetManualExtra,
                                            onSetCustomTotalOverride = viewModel::setCustomTotalOverride,
                                            onSelectZone = viewModel::selectSpecialZone,
                                            onSelectCourier = viewModel::selectCourier,
                                            onSelectPaymentMethod = viewModel::selectPaymentMethod,
                                            onSelectTransferBank = viewModel::selectTransferBank,
                                            onSetPaymentPendingOnDelivery = viewModel::setPaymentPendingOnDelivery,
                                            onSaveToHistory = viewModel::saveCurrentQuoteToHistory,
                                            onClearQuoteAndInputs = viewModel::clearCurrentQuoteAndInputs,
                                            onDismissErrorMessage = viewModel::dismissErrorMessage,
                                            onOpenSettings = { currentTab = 2 },
                                            onApplyPreferentialRate = viewModel::applyPreferentialRate,
                                            onDismissPreferentialDialog = viewModel::dismissPreferentialRateDialog,
                                            onOpenPreferentialDialog = viewModel::openPreferentialRateDialog
                                        )
                                        1 -> HistoryScreen(
                                            records = uiState.history,
                                            couriers = uiState.couriers,
                                            currencySymbol = uiState.config.currencySymbol,
                                            config = uiState.config,
                                            onSelectRecord = { record ->
                                                viewModel.loadRecordFromHistory(record)
                                                currentTab = 0
                                            },
                                            onDeleteRecord = viewModel::deleteHistoryRecord,
                                            onUpdateOrderStatus = viewModel::updateOrderStatus,
                                            onToggleDriverPaid = viewModel::toggleDriverPaymentStatus,
                                            onNavigateToQuote = { currentTab = 0 },
                                            onClearAll = viewModel::clearAllHistory
                                        )
                                        2 -> SettingsScreen(
                                            currentConfig = uiState.config,
                                            couriers = uiState.couriers,
                                            zones = uiState.zones,
                                            onSaveConfig = { updated ->
                                                viewModel.updateStoreAndPricingConfig(updated)
                                            },
                                            onUpdateConfig = { updated ->
                                                viewModel.updateStoreAndPricingConfig(updated)
                                            },
                                            onOpenCouriers = { currentTab = 3 },
                                            onAddSpecialZone = viewModel::addSpecialZone,
                                            onDeleteSpecialZone = viewModel::deleteSpecialZone,
                                            onApplyManualSurchargeAmount = { amount ->
                                                viewModel.setManualExtraValue(amount)
                                                currentTab = 0
                                            }
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (isExternalLocationIntent(intent)) {
            isExternalIntent = true
        }
        handleIncomingShareOrMapIntent(intent)
    }

    private fun isExternalLocationIntent(intent: Intent?): Boolean {
        if (intent == null) return false
        return when (intent.action) {
            Intent.ACTION_SEND -> {
                intent.type == "text/plain" && !intent.getStringExtra(Intent.EXTRA_TEXT).isNullOrBlank()
            }
            Intent.ACTION_VIEW -> {
                !intent.dataString.isNullOrBlank()
            }
            else -> false
        }
    }

    private fun handleIncomingShareOrMapIntent(intent: Intent?) {
        if (intent == null) return
        when (intent.action) {
            Intent.ACTION_SEND -> {
                if (intent.type == "text/plain") {
                    val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                    if (!sharedText.isNullOrBlank()) {
                        viewModel.processAndFreezeLocationInput(sharedText, fromExternalIntent = true)
                    }
                }
            }
            Intent.ACTION_VIEW -> {
                val dataUri = intent.dataString
                if (!dataUri.isNullOrBlank()) {
                    viewModel.processAndFreezeLocationInput(dataUri, fromExternalIntent = true)
                }
            }
        }
    }
}

