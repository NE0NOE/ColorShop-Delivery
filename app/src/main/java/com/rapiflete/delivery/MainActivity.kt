package com.rapiflete.delivery

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rapiflete.delivery.ui.components.ColorShopSplashScreen
import com.rapiflete.delivery.ui.screens.CalculatorScreen
import com.rapiflete.delivery.ui.screens.CouriersScreen
import com.rapiflete.delivery.ui.screens.HistoryScreen
import com.rapiflete.delivery.ui.screens.SettingsScreen
import com.rapiflete.delivery.ui.theme.ColorShopMutedPlum
import com.rapiflete.delivery.ui.theme.ColorShopPinkPill
import com.rapiflete.delivery.ui.theme.ColorShopPrimary
import com.rapiflete.delivery.ui.theme.ColorShopSurface
import com.rapiflete.delivery.ui.theme.RapiFleteTheme
import com.rapiflete.delivery.viewmodel.DeliveryViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: DeliveryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleIncomingShareOrMapIntent(intent)

        setContent {
            RapiFleteTheme {
                val uiState by viewModel.uiState.collectAsState()
                var showSplash by rememberSaveable { mutableStateOf(true) }
                var currentTab by rememberSaveable { mutableIntStateOf(0) } // 0=Cotizar, 1=Historial, 2=Configuración, 3=Repartidores

                BackHandler(enabled = currentTab == 3) {
                    currentTab = 2
                }

                if (showSplash) {
                    ColorShopSplashScreen(
                        onFinished = { showSplash = false }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ColorShopSurface)
                    ) {
                        when (currentTab) {
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
                                onSaveToHistory = viewModel::saveCurrentQuoteToHistory,
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
                                onClearAll = viewModel::clearAllHistory
                            )
                            2 -> SettingsScreen(
                                currentConfig = uiState.config,
                                couriers = uiState.couriers,
                                zones = uiState.zones,
                                onSaveConfig = { updated ->
                                    viewModel.updateStoreAndPricingConfig(updated)
                                    currentTab = 0
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
                            3 -> CouriersScreen(
                                couriers = uiState.couriers,
                                onAddCourier = viewModel::addCourier,
                                onUpdateCourierVisibility = viewModel::updateCourierVisibilityFlags,
                                onUpdateCourier = viewModel::updateCourier,
                                onDeleteCourier = viewModel::deleteCourier,
                                onBack = { currentTab = 2 }
                            )
                        }

                        // Barra de Navegación Inferior Flotante (con píldora rosa activa según Stitch)
                        ColorShopBottomNavBar(
                            selectedIndex = if (currentTab == 3) 2 else currentTab,
                            onSelectTab = { tabIndex -> currentTab = tabIndex },
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingShareOrMapIntent(intent)
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

@Composable
private fun ColorShopBottomNavBar(
    selectedIndex: Int,
    onSelectTab: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = ColorShopSurface.copy(alpha = 0.94f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        shadowElevation = 16.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavPillItem(
                icon = Icons.Default.Calculate,
                label = "Cotizar",
                selected = selectedIndex == 0,
                onClick = { onSelectTab(0) }
            )
            BottomNavPillItem(
                icon = Icons.Default.History,
                label = "Historial",
                selected = selectedIndex == 1,
                onClick = { onSelectTab(1) }
            )
            BottomNavPillItem(
                icon = Icons.Default.Settings,
                label = "Configuración",
                selected = selectedIndex == 2,
                onClick = { onSelectTab(2) }
            )
        }
    }
}

@Composable
private fun BottomNavPillItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (selected) ColorShopPinkPill else Color.Transparent
    val contentColor = if (selected) ColorShopPrimary else ColorShopMutedPlum.copy(alpha = 0.72f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .clickable { onClick() }
            .padding(horizontal = 22.dp, vertical = 7.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = contentColor
        )
    }
}
