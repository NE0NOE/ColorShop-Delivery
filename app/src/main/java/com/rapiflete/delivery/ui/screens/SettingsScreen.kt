package com.rapiflete.delivery.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rapiflete.delivery.model.CourierDriver
import com.rapiflete.delivery.model.RoundingMode
import com.rapiflete.delivery.model.SpecialZoneRule
import com.rapiflete.delivery.model.StoreAndPricingConfig
import com.rapiflete.delivery.model.SurchargeType
import com.rapiflete.delivery.ui.components.ColorShopTopBar
import com.rapiflete.delivery.ui.theme.ColorShopDeepPlum
import com.rapiflete.delivery.ui.theme.ColorShopMagenta
import com.rapiflete.delivery.ui.theme.ColorShopMutedPlum
import com.rapiflete.delivery.ui.theme.ColorShopPrimary
import com.rapiflete.delivery.ui.theme.ColorShopSurface
import com.rapiflete.delivery.ui.theme.ColorShopSurfaceHigh
import com.rapiflete.delivery.ui.theme.ColorShopSurfaceLow

@Composable
fun SettingsScreen(
    currentConfig: StoreAndPricingConfig,
    couriers: List<CourierDriver>,
    zones: List<SpecialZoneRule>,
    onSaveConfig: (StoreAndPricingConfig) -> Unit,
    onUpdateConfig: (StoreAndPricingConfig) -> Unit = {},
    onOpenCouriers: () -> Unit,
    onAddSpecialZone: (String, Double, Boolean) -> Unit,
    onDeleteSpecialZone: (String) -> Unit,
    onApplyManualSurchargeAmount: (Double) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var currencySymbol by remember(currentConfig) { mutableStateOf(currentConfig.currencySymbol) }
    var minimumFareText by remember(currentConfig) { mutableStateOf(currentConfig.minimumFare.toString()) }
    var includedKmText by remember(currentConfig) { mutableStateOf(currentConfig.includedKmInMinimum.toString()) }
    var costPerExtraKmText by remember(currentConfig) { mutableStateOf(currentConfig.costPerExtraKm.toString()) }

    var weekdayOpen by remember(currentConfig) { mutableStateOf(currentConfig.weekdayOpenText) }
    var weekdayClose by remember(currentConfig) { mutableStateOf(currentConfig.weekdayCloseText) }
    var saturdayEnabled by remember(currentConfig) { mutableStateOf(currentConfig.saturdayEnabled) }
    var satOpen by remember(currentConfig) { mutableStateOf(currentConfig.saturdayOpenText) }
    var satClose by remember(currentConfig) { mutableStateOf(currentConfig.saturdayCloseText) }
    var sundayEnabled by remember(currentConfig) { mutableStateOf(currentConfig.sundayEnabled) }
    var sunOpen by remember(currentConfig) { mutableStateOf(currentConfig.sundayOpenText) }
    var sunClose by remember(currentConfig) { mutableStateOf(currentConfig.sundayCloseText) }

    var autoDetectOffHours by remember(currentConfig) { mutableStateOf(currentConfig.autoDetectOffHours) }

    var offHoursType by remember(currentConfig) { mutableStateOf(currentConfig.offHoursSurchargeType) }
    var offHoursValText by remember(currentConfig) { mutableStateOf(currentConfig.offHoursSurchargeValue.toString()) }

    var holidayType by remember(currentConfig) { mutableStateOf(currentConfig.holidaySurchargeType) }
    var holidayValText by remember(currentConfig) { mutableStateOf(currentConfig.holidaySurchargeValue.toString()) }

    var rainType by remember(currentConfig) { mutableStateOf(currentConfig.rainOrHighDemandSurchargeType) }
    var rainValText by remember(currentConfig) { mutableStateOf(currentConfig.rainOrHighDemandSurchargeValue.toString()) }

    var customSurchargeName by remember { mutableStateOf("") }
    var customSurchargeType by remember { mutableStateOf(SurchargeType.FIXED) }
    var customSurchargeValueText by remember { mutableStateOf("15.0") }

    var newZoneName by remember { mutableStateOf("") }
    var newZonePriceText by remember { mutableStateOf("100.0") }
    var newZoneAppliesExtraKm by remember { mutableStateOf(false) }

    var roundingMode by remember(currentConfig) { mutableStateOf(currentConfig.roundingMode) }

    fun buildCurrentConfig(): StoreAndPricingConfig = currentConfig.copy(
        currencySymbol = currencySymbol,
        minimumFare = minimumFareText.toDoubleOrNull() ?: currentConfig.minimumFare,
        includedKmInMinimum = includedKmText.toDoubleOrNull() ?: currentConfig.includedKmInMinimum,
        costPerExtraKm = costPerExtraKmText.toDoubleOrNull() ?: currentConfig.costPerExtraKm,
        weekdayOpenText = weekdayOpen,
        weekdayCloseText = weekdayClose,
        saturdayEnabled = saturdayEnabled,
        saturdayOpenText = satOpen,
        saturdayCloseText = satClose,
        sundayEnabled = sundayEnabled,
        sundayOpenText = sunOpen,
        sundayCloseText = sunClose,
        autoDetectOffHours = autoDetectOffHours,
        offHoursSurchargeType = offHoursType,
        offHoursSurchargeValue = offHoursValText.toDoubleOrNull() ?: currentConfig.offHoursSurchargeValue,
        holidaySurchargeType = holidayType,
        holidaySurchargeValue = holidayValText.toDoubleOrNull() ?: currentConfig.holidaySurchargeValue,
        rainOrHighDemandSurchargeType = rainType,
        rainOrHighDemandSurchargeValue = rainValText.toDoubleOrNull() ?: currentConfig.rainOrHighDemandSurchargeValue,
        roundingMode = roundingMode
    )

    DisposableEffect(Unit) {
        onDispose {
            onUpdateConfig(buildCurrentConfig())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorShopSurface)
            .verticalScroll(scrollState)
    ) {
        ColorShopTopBar(showQuickSearchButton = false)

        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Título de pantalla
            Column {
                Text(
                    text = "Configuración y Tarifas",
                    fontSize = 23.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ColorShopDeepPlum
                )
                Text(
                    text = "Ajustes de tienda matriz, cálculo por km y recargos",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = ColorShopMutedPlum
                )
            }

            // SECCIÓN 1: Tarjeta de Gestión de Repartidores (Simplificada)
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(26.dp),
                shadowElevation = 5.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ColorShopSurfaceHigh, RoundedCornerShape(26.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(ColorShopSurfaceLow.copy(alpha = 0.7f), Color.White)
                            )
                        )
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(ColorShopPrimary, ColorShopMagenta)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.DirectionsBike,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Text(
                            text = "Gestión de Repartidores",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ColorShopDeepPlum
                        )
                    }

                    Button(
                        onClick = onOpenCouriers,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ColorShopPrimary),
                        modifier = Modifier.height(42.dp)
                    ) {
                        Text("Administrar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            // SECCIÓN 2: ⚡ Tarifas y Costo Mínimo (Grid 2x2 de Stitch)
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(26.dp),
                shadowElevation = 5.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ColorShopSurfaceLow),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("⚡", fontSize = 16.sp)
                        }
                        Column {
                            Text(
                                text = "Tarifas y Costo Mínimo",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ColorShopDeepPlum
                            )
                            Text(
                                text = "Reglas base de cobro por distancia",
                                fontSize = 11.sp,
                                color = ColorShopMutedPlum
                            )
                        }
                    }

                    // Grid 2x2
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Moneda selector
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "MONEDA",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ColorShopMutedPlum
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(ColorShopSurfaceLow.copy(alpha = 0.45f))
                                    .border(1.dp, ColorShopSurfaceHigh, RoundedCornerShape(16.dp))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf("C$", "$").forEach { sym ->
                                    val sel = currencySymbol == sym
                                    Surface(
                                        color = if (sel) ColorShopPrimary else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                            .clickable { currencySymbol = sym }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = if (sym == "C$") "C$ (NIO)" else "$ (USD)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (sel) Color.White else ColorShopDeepPlum
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Costo Mínimo / Base
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "COSTO MÍNIMO / BASE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ColorShopMutedPlum
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = minimumFareText,
                                onValueChange = { minimumFareText = it },
                                prefix = { Text("$currencySymbol ", fontWeight = FontWeight.Bold, color = ColorShopMutedPlum) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ColorShopPrimary,
                                    unfocusedBorderColor = ColorShopSurfaceHigh,
                                    focusedContainerColor = ColorShopSurfaceLow.copy(alpha = 0.35f),
                                    unfocusedContainerColor = ColorShopSurfaceLow.copy(alpha = 0.35f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Km en Mínimo
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "KM EN MÍNIMO",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ColorShopMutedPlum
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = includedKmText,
                                onValueChange = { includedKmText = it },
                                suffix = { Text("km", fontWeight = FontWeight.Bold, color = ColorShopMutedPlum) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ColorShopPrimary,
                                    unfocusedBorderColor = ColorShopSurfaceHigh,
                                    focusedContainerColor = ColorShopSurfaceLow.copy(alpha = 0.35f),
                                    unfocusedContainerColor = ColorShopSurfaceLow.copy(alpha = 0.35f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Km Adicional
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "KM ADICIONAL",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ColorShopMutedPlum
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = costPerExtraKmText,
                                onValueChange = { costPerExtraKmText = it },
                                prefix = { Text("$currencySymbol ", fontWeight = FontWeight.Bold, color = ColorShopMutedPlum) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ColorShopPrimary,
                                    unfocusedBorderColor = ColorShopSurfaceHigh,
                                    focusedContainerColor = ColorShopSurfaceLow.copy(alpha = 0.35f),
                                    unfocusedContainerColor = ColorShopSurfaceLow.copy(alpha = 0.35f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Surface(
                        color = ColorShopSurfaceLow.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "ℹ️ Si deseas cobrar desde el primer kilómetro además de la tarifa base, coloca 0 en 'Km en Mínimo'.",
                            fontSize = 11.sp,
                            color = ColorShopMutedPlum,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }

            // SECCIÓN 3: 🕒 Horario y Recargos Automáticos + Zonas Aledañas + Redondeo
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(26.dp),
                shadowElevation = 5.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ColorShopSurfaceLow),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🕒", fontSize = 16.sp)
                        }
                        Column {
                            Text(
                                text = "Horario y Recargos Automáticos",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ColorShopDeepPlum
                            )
                            Text(
                                text = "Tarificación por nocturnidad y eventos",
                                fontSize = 11.sp,
                                color = ColorShopMutedPlum
                            )
                        }
                    }

                    // Bloques de Horario: Lunes a Viernes, Sábado, Domingo
                    ScheduleDayBox(
                        title = "🗓️ Lunes a Viernes",
                        badge = "Lun - Vie",
                        openValue = weekdayOpen,
                        onOpenChange = { weekdayOpen = it },
                        closeValue = weekdayClose,
                        onCloseChange = { weekdayClose = it }
                    )

                    ScheduleDayBox(
                        title = "📅 Sábado",
                        badge = if (saturdayEnabled) "Medio día" else "Cerrado",
                        openValue = satOpen,
                        onOpenChange = { satOpen = it },
                        closeValue = satClose,
                        onCloseChange = { satClose = it },
                        showToggle = true,
                        toggleChecked = saturdayEnabled,
                        onToggleChange = {
                            saturdayEnabled = it
                            onUpdateConfig(buildCurrentConfig().copy(saturdayEnabled = it))
                        }
                    )

                    ScheduleDayBox(
                        title = "✨ Domingo",
                        badge = if (sundayEnabled) "Activo" else "Cerrado / Inactivo",
                        openValue = sunOpen,
                        onOpenChange = { sunOpen = it },
                        closeValue = sunClose,
                        onCloseChange = { sunClose = it },
                        showToggle = true,
                        toggleChecked = sundayEnabled,
                        onToggleChange = {
                            sundayEnabled = it
                            onUpdateConfig(buildCurrentConfig().copy(sundayEnabled = it))
                        }
                    )

                    // Switch de Recargo Nocturno Automático
                    Surface(
                        color = ColorShopSurfaceLow.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, ColorShopSurfaceHigh, RoundedCornerShape(18.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Recargo Nocturno Automático",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorShopDeepPlum
                                )
                                Text(
                                    text = "Aplicar fuera de horario según el reloj del dispositivo",
                                    fontSize = 11.sp,
                                    color = ColorShopMutedPlum
                                )
                            }
                            Switch(
                                checked = autoDetectOffHours,
                                onCheckedChange = { autoDetectOffHours = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = ColorShopPrimary
                                )
                            )
                        }
                    }

                    // Lista de Recargos (Nocturno, Festivo, Lluvia)
                    StitchSurchargeItem(
                        title = "🌙 Recargo Nocturno / Fuera Horario",
                        badgeText = "Activo",
                        currencySymbol = currencySymbol,
                        selectedType = offHoursType,
                        onTypeSelected = { offHoursType = it },
                        valueText = offHoursValText,
                        onValueChanged = { offHoursValText = it }
                    )

                    StitchSurchargeItem(
                        title = "🎉 Día Festivo / Domingo",
                        badgeText = "Activo",
                        currencySymbol = currencySymbol,
                        selectedType = holidayType,
                        onTypeSelected = { holidayType = it },
                        valueText = holidayValText,
                        onValueChanged = { holidayValText = it }
                    )

                    StitchSurchargeItem(
                        title = "🌧️ Lluvia / Alta Demanda",
                        badgeText = "Manual",
                        currencySymbol = currencySymbol,
                        selectedType = rainType,
                        onTypeSelected = { rainType = it },
                        valueText = rainValText,
                        onValueChanged = { rainValText = it }
                    )

                    // Item 4: Recargo Manual (Personalizado)
                    Surface(
                        color = ColorShopSurfaceLow.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, ColorShopSurfaceHigh, RoundedCornerShape(18.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "✏️ Recargo Manual (Personalizado)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorShopDeepPlum
                                )
                                Surface(
                                    color = Color.White,
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = "Personalizado",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ColorShopPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = customSurchargeName,
                                onValueChange = { customSurchargeName = it },
                                placeholder = { Text("Ej. Espera adicional, Carga pesada...", fontSize = 12.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ColorShopPrimary,
                                    unfocusedBorderColor = ColorShopSurfaceHigh,
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            StitchSurchargeSegmentedRow(
                                currencySymbol = currencySymbol,
                                selectedType = customSurchargeType,
                                onTypeSelected = { customSurchargeType = it },
                                valueText = customSurchargeValueText,
                                onValueChanged = { customSurchargeValueText = it }
                            )

                            Button(
                                onClick = {
                                    val amt = customSurchargeValueText.toDoubleOrNull() ?: 15.0
                                    onApplyManualSurchargeAmount(amt)
                                    Toast.makeText(
                                        context,
                                        "Recargo manual de $currencySymbol$amt aplicado a la cotización actual",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ColorShopPrimary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("+ Añadir Recargo Manual", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    HorizontalDivider(color = ColorShopSurfaceHigh.copy(alpha = 0.6f))

                    // Zonas de Recargo / Aledañas (Ciudad Sandino, Ticuantepe, etc.)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ColorShopSurfaceLow),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("📍", fontSize = 14.sp)
                            }
                            Column {
                                Text(
                                    text = "Zonas de Recargo / Aledañas",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorShopDeepPlum
                                )
                                Text(
                                    text = "Tarifas planas o bases para municipios y zonas distantes",
                                    fontSize = 10.sp,
                                    color = ColorShopMutedPlum
                                )
                            }
                        }

                        Surface(
                            color = ColorShopSurfaceHigh,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "${zones.size} Activas",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ColorShopPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    zones.forEach { zone ->
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, ColorShopSurfaceHigh, RoundedCornerShape(16.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(ColorShopSurfaceLow),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(zone.iconEmoji, fontSize = 13.sp)
                                    }
                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = zone.name,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ColorShopDeepPlum
                                            )
                                            Surface(
                                                color = ColorShopSurfaceLow,
                                                shape = RoundedCornerShape(50)
                                            ) {
                                                Text(
                                                    text = "Zona Aledaña",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = ColorShopPrimary,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        val extraLabel = if (zone.appliesExtraKm) "+Km estándar" else "Sin km adicional"
                                        Text(
                                            text = "Precio Base: $currencySymbol ${zone.basePrice} · $extraLabel",
                                            fontSize = 11.sp,
                                            color = ColorShopMutedPlum
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { onDeleteSpecialZone(zone.id) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Eliminar zona",
                                        tint = ColorShopMutedPlum,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Formulario Añadir Nueva Zona
                    Surface(
                        color = ColorShopSurfaceLow.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, ColorShopSurfaceHigh, RoundedCornerShape(18.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "AÑADIR NUEVA ZONA",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ColorShopDeepPlum
                            )
                            OutlinedTextField(
                                value = newZoneName,
                                onValueChange = { newZoneName = it },
                                placeholder = { Text("Ej. Ciudad Sandino, Tipitapa...", fontSize = 12.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ColorShopPrimary,
                                    unfocusedBorderColor = ColorShopSurfaceHigh,
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newZonePriceText,
                                    onValueChange = { newZonePriceText = it },
                                    prefix = { Text("$currencySymbol ", fontWeight = FontWeight.Bold) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ColorShopPrimary,
                                        unfocusedBorderColor = ColorShopSurfaceHigh,
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    ),
                                    modifier = Modifier.weight(1f)
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color.White)
                                        .clickable { newZoneAppliesExtraKm = !newZoneAppliesExtraKm }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Aplica Km adic.",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ColorShopDeepPlum
                                    )
                                    Checkbox(
                                        checked = newZoneAppliesExtraKm,
                                        onCheckedChange = { newZoneAppliesExtraKm = it },
                                        colors = CheckboxDefaults.colors(checkedColor = ColorShopPrimary)
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    if (newZoneName.isBlank()) {
                                        Toast.makeText(context, "Escribe el nombre de la zona", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    onAddSpecialZone(
                                        newZoneName,
                                        newZonePriceText.toDoubleOrNull() ?: 100.0,
                                        newZoneAppliesExtraKm
                                    )
                                    newZoneName = ""
                                    Toast.makeText(context, "Zona agregada", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ColorShopPrimary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Guardar Zona", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    HorizontalDivider(color = ColorShopSurfaceHigh.copy(alpha = 0.6f))

                    // Reglas de Redondeo Automático del Precio Final
                    Text(
                        text = "REDONDEO AUTOMÁTICO DEL PRECIO FINAL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ColorShopMutedPlum
                    )

                    RoundingMode.entries.forEach { mode ->
                        val isSel = roundingMode == mode
                        Surface(
                            color = if (isSel) ColorShopSurfaceLow.copy(alpha = 0.5f) else Color.White,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (isSel) 2.dp else 1.dp,
                                    color = if (isSel) ColorShopPrimary else ColorShopSurfaceHigh,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { roundingMode = mode }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = mode.displayName,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSel) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    color = ColorShopDeepPlum
                                )
                                Icon(
                                    imageVector = if (isSel) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (isSel) ColorShopPrimary else ColorShopMutedPlum,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Botón Principal Guardar Configuración y Tarifas
            Button(
                onClick = {
                    val updated = buildCurrentConfig()
                    onSaveConfig(updated)
                    Toast.makeText(context, "Configuración de ColorShop guardada", Toast.LENGTH_SHORT).show()
                },
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ColorShopPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Guardar Configuración y Tarifas",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun ScheduleDayBox(
    title: String,
    badge: String,
    openValue: String,
    onOpenChange: (String) -> Unit,
    closeValue: String,
    onCloseChange: (String) -> Unit,
    showToggle: Boolean = false,
    toggleChecked: Boolean = true,
    onToggleChange: (Boolean) -> Unit = {}
) {
    val isDayActive = !showToggle || toggleChecked
    Surface(
        color = if (isDayActive) ColorShopSurfaceLow.copy(alpha = 0.35f) else ColorShopSurfaceLow.copy(alpha = 0.15f),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ColorShopSurfaceHigh.copy(alpha = 0.7f), RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDayActive) ColorShopDeepPlum else ColorShopMutedPlum
                    )
                    Surface(
                        color = if (isDayActive) ColorShopSurfaceHigh else Color.LightGray.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = badge,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDayActive) ColorShopPrimary else ColorShopMutedPlum,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                if (showToggle) {
                    Switch(
                        checked = toggleChecked,
                        onCheckedChange = onToggleChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = ColorShopPrimary
                        )
                    )
                }
            }

            AnimatedVisibility(visible = isDayActive) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = openValue,
                        onValueChange = onOpenChange,
                        label = { Text("Apertura (AM)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ColorShopPrimary,
                            unfocusedBorderColor = ColorShopSurfaceHigh,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = closeValue,
                        onValueChange = onCloseChange,
                        label = { Text("Cierre (PM)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ColorShopPrimary,
                            unfocusedBorderColor = ColorShopSurfaceHigh,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StitchSurchargeItem(
    title: String,
    badgeText: String,
    currencySymbol: String,
    selectedType: SurchargeType,
    onTypeSelected: (SurchargeType) -> Unit,
    valueText: String,
    onValueChanged: (String) -> Unit
) {
    Surface(
        color = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorShopDeepPlum
                )
                Surface(
                    color = ColorShopSurfaceLow,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ColorShopPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            StitchSurchargeSegmentedRow(
                currencySymbol = currencySymbol,
                selectedType = selectedType,
                onTypeSelected = onTypeSelected,
                valueText = valueText,
                onValueChanged = onValueChanged
            )
        }
    }
}

@Composable
private fun StitchSurchargeSegmentedRow(
    currencySymbol: String,
    selectedType: SurchargeType,
    onTypeSelected: (SurchargeType) -> Unit,
    valueText: String,
    onValueChanged: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = Color(0xFFE2E8F0),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .weight(1.3f)
                .height(42.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                SurchargeType.entries.forEach { t ->
                    val sel = selectedType == t
                    Surface(
                        color = if (sel) Color.White else Color.Transparent,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clickable { onTypeSelected(t) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = t.displayName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (sel) ColorShopPrimary else ColorShopMutedPlum
                            )
                        }
                    }
                }
            }
        }

        OutlinedTextField(
            value = valueText,
            onValueChange = onValueChanged,
            prefix = {
                Text(
                    text = if (selectedType == SurchargeType.FIXED) "$currencySymbol " else "% ",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorShopMutedPlum
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ColorShopPrimary,
                unfocusedBorderColor = Color(0xFFCBD5E1),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            modifier = Modifier.weight(0.8f)
        )
    }
}
