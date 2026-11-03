package com.rapiflete.delivery.ui.screens

import android.content.Intent
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsMotorsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rapiflete.delivery.model.CourierDriver
import com.rapiflete.delivery.model.CourierStatus
import com.rapiflete.delivery.ui.components.ColorShopTopBar
import com.rapiflete.delivery.ui.theme.ColorShopDeepPlum
import com.rapiflete.delivery.ui.theme.ColorShopMutedPlum
import com.rapiflete.delivery.ui.theme.ColorShopPrimary
import com.rapiflete.delivery.ui.theme.ColorShopSurface
import com.rapiflete.delivery.ui.theme.ColorShopSurfaceHigh
import com.rapiflete.delivery.ui.theme.ColorShopSurfaceLow
import com.rapiflete.delivery.ui.theme.ColorShopTertiaryBlue
import com.rapiflete.delivery.ui.theme.ColorShopTertiaryFixed

@Composable
fun CouriersScreen(
    couriers: List<CourierDriver>,
    onAddCourier: (String, String, String, String, String, String, Boolean, Boolean) -> Unit,
    onUpdateCourierVisibility: (String, Boolean, Boolean) -> Unit,
    onUpdateCourier: (CourierDriver) -> Unit = {},
    onDeleteCourier: (String) -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var docId by remember { mutableStateOf("") }
    var vehicleType by remember { mutableStateOf("Motocicleta") }
    var plate by remember { mutableStateOf("") }
    var selectedShift by remember { mutableStateOf("Mañana (8a-4p)") }
    var includeNameInMsg by remember { mutableStateOf(true) }
    var includePhoneInMsg by remember { mutableStateOf(true) }

    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf<CourierStatus?>(null) }
    var courierToDelete by remember { mutableStateOf<CourierDriver?>(null) }
    var courierToEdit by remember { mutableStateOf<CourierDriver?>(null) }

    var showAddCourierForm by remember { mutableStateOf(false) }
    var avatarUri by remember { mutableStateOf<Uri?>(null) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        avatarUri = uri
    }

    val previewInitials = remember(fullName) {
        val words = fullName.trim().split(" ").filter { it.isNotBlank() }
        if (words.size >= 2) {
            "${words[0].first().uppercaseChar()}${words[1].first().uppercaseChar()}"
        } else if (words.isNotEmpty() && words[0].isNotEmpty()) {
            words[0].take(2).uppercase(java.util.Locale.getDefault())
        } else {
            ""
        }
    }

    val avatarBitmap = remember(avatarUri) {
        avatarUri?.let { uri ->
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    val filteredCouriers = remember(couriers, searchQuery, statusFilter) {
        couriers.filter { c ->
            val matchesStatus = statusFilter == null || c.status == statusFilter
            val q = searchQuery.trim().lowercase()
            val matchesText = q.isEmpty() ||
                c.name.lowercase().contains(q) ||
                c.plate.lowercase().contains(q) ||
                c.phone.lowercase().contains(q)
            matchesStatus && matchesText
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Barra de título + Botón volver
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = Color.White,
                        shape = CircleShape,
                        shadowElevation = 3.dp,
                        modifier = Modifier
                            .size(40.dp)
                            .clickable { onBack() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver",
                                tint = ColorShopPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Repartidores",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ColorShopDeepPlum
                        )
                        Text(
                            text = "Registro y plantilla activa en tienda",
                            fontSize = 12.sp,
                            color = ColorShopMutedPlum
                        )
                    }
                }

                Surface(
                    color = ColorShopSurfaceHigh,
                    shape = RoundedCornerShape(50)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(ColorShopPrimary)
                        )
                        Text(
                            text = "${couriers.size} Activos",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorShopPrimary
                        )
                    }
                }
            }

            // 3 Tarjetas Bento de Estadísticas Rápidas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CourierStatBox(
                    icon = Icons.Default.SportsMotorsports,
                    iconBg = ColorShopSurfaceLow,
                    iconTint = ColorShopPrimary,
                    value = "${couriers.size}",
                    label = "En Flota",
                    modifier = Modifier.weight(1f)
                )
                CourierStatBox(
                    icon = Icons.Default.CheckCircle,
                    iconBg = Color(0xFFE8F5E9),
                    iconTint = Color(0xFF2E7D32),
                    value = "${couriers.count { it.status == CourierStatus.AVAILABLE }}",
                    label = "Disponibles",
                    modifier = Modifier.weight(1f)
                )
                CourierStatBox(
                    icon = Icons.Default.Navigation,
                    iconBg = ColorShopTertiaryFixed,
                    iconTint = ColorShopTertiaryBlue,
                    value = "${couriers.count { it.status == CourierStatus.ON_ROUTE }}",
                    label = "En Ruta",
                    modifier = Modifier.weight(1f)
                )
            }

            // Botón Limpio y Visible: + Nuevo Repartidor
            Button(
                onClick = { showAddCourierForm = !showAddCourierForm },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ColorShopPrimary,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = if (showAddCourierForm) Icons.Default.Close else Icons.Default.PersonAdd,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (showAddCourierForm) "Ocultar Formulario" else "+ Nuevo Repartidor",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }

            // Formulario Colapsable con AnimatedVisibility
            AnimatedVisibility(visible = showAddCourierForm) {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(24.dp),
                    shadowElevation = 5.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(ColorShopSurfaceHigh),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PersonAdd,
                                        contentDescription = null,
                                        tint = ColorShopPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = "Registrar Repartidor",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorShopDeepPlum
                                )
                            }

                            Surface(
                                color = ColorShopSurfaceLow,
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "Nuevo Ingreso",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorShopMutedPlum,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Zona de Foto de Perfil / Avatar Dinámico
                        Surface(
                            color = ColorShopSurfaceLow,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { photoPickerLauncher.launch("image/*") }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(ColorShopSurfaceHigh),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (avatarBitmap != null) {
                                        Image(
                                            bitmap = avatarBitmap.asImageBitmap(),
                                            contentDescription = "Foto de perfil",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else if (previewInitials.isNotEmpty()) {
                                        Text(
                                            text = previewInitials,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = ColorShopPrimary
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.AddAPhoto,
                                            contentDescription = "Elegir foto",
                                            tint = ColorShopPrimary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = if (avatarBitmap != null) "Foto seleccionada" else if (previewInitials.isNotEmpty()) "Avatar: $previewInitials" else "Foto o Avatar de perfil",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ColorShopDeepPlum
                                        )
                                        Surface(
                                            color = ColorShopPrimary.copy(alpha = 0.12f),
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Text(
                                                text = if (avatarBitmap != null) "Cambiar" else "Elegir",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ColorShopPrimary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = if (avatarBitmap != null) {
                                            "Toca para cambiar la imagen de galería"
                                        } else if (previewInitials.isNotEmpty()) {
                                            "Iniciales dinámicas generadas (o toca para elegir foto)"
                                        } else {
                                            "Toca para elegir foto de galería o escribe el nombre para iniciales"
                                        },
                                        fontSize = 11.sp,
                                        color = ColorShopMutedPlum
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("Nombre Completo") },
                            placeholder = { Text("Ej. Marcos Morales") },
                            leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = ColorShopMutedPlum) },
                            singleLine = true,
                            shape = RoundedCornerShape(50),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ColorShopPrimary,
                                unfocusedBorderColor = ColorShopSurfaceHigh,
                                focusedContainerColor = ColorShopSurfaceLow.copy(alpha = 0.45f),
                                unfocusedContainerColor = ColorShopSurfaceLow.copy(alpha = 0.45f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Teléfono WhatsApp (+505)") },
                            placeholder = { Text("8823-1100") },
                            singleLine = true,
                            shape = RoundedCornerShape(50),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ColorShopPrimary,
                                unfocusedBorderColor = ColorShopSurfaceHigh,
                                focusedContainerColor = ColorShopSurfaceLow.copy(alpha = 0.45f),
                                unfocusedContainerColor = ColorShopSurfaceLow.copy(alpha = 0.45f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = docId,
                            onValueChange = { docId = it },
                            label = { Text("Cédula / Documento Oficial") },
                            placeholder = { Text("001-290492-0044K") },
                            leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null, tint = ColorShopMutedPlum) },
                            singleLine = true,
                            shape = RoundedCornerShape(50),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ColorShopPrimary,
                                unfocusedBorderColor = ColorShopSurfaceHigh,
                                focusedContainerColor = ColorShopSurfaceLow.copy(alpha = 0.45f),
                                unfocusedContainerColor = ColorShopSurfaceLow.copy(alpha = 0.45f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = vehicleType,
                                onValueChange = { vehicleType = it },
                                label = { Text("Vehículo") },
                                singleLine = true,
                                shape = RoundedCornerShape(50),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ColorShopPrimary,
                                    unfocusedBorderColor = ColorShopSurfaceHigh,
                                    focusedContainerColor = ColorShopSurfaceLow.copy(alpha = 0.45f),
                                    unfocusedContainerColor = ColorShopSurfaceLow.copy(alpha = 0.45f)
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = plate,
                                onValueChange = { plate = it },
                                label = { Text("Placa Oficial") },
                                placeholder = { Text("M-28491") },
                                singleLine = true,
                                shape = RoundedCornerShape(50),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ColorShopPrimary,
                                    unfocusedBorderColor = ColorShopSurfaceHigh,
                                    focusedContainerColor = ColorShopSurfaceLow.copy(alpha = 0.45f),
                                    unfocusedContainerColor = ColorShopSurfaceLow.copy(alpha = 0.45f)
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Selector de Turno Asignado
                        Text(
                            text = "Turno Asignado",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorShopMutedPlum
                        )

                        val shifts = listOf("Mañana (8a-4p)", "Tarde/Noche", "Completo")
                        Surface(
                            color = ColorShopSurfaceLow,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                shifts.forEach { shift ->
                                    val isSel = selectedShift == shift
                                    Surface(
                                        color = if (isSel) ColorShopPrimary else Color.Transparent,
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { selectedShift = shift }
                                    ) {
                                        Text(
                                            text = shift,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSel) Color.White else ColorShopMutedPlum,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        // Checks para incluir nombre y número del motorizado en el mensaje al cliente
                        Surface(
                            color = ColorShopSurfaceLow.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Datos visibles en mensaje al cliente:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorShopDeepPlum
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable { includeNameInMsg = !includeNameInMsg }
                                    ) {
                                        Checkbox(
                                            checked = includeNameInMsg,
                                            onCheckedChange = { includeNameInMsg = it },
                                            colors = CheckboxDefaults.colors(checkedColor = ColorShopPrimary)
                                        )
                                        Text(
                                            text = "Mostrar nombre",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = ColorShopDeepPlum
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable { includePhoneInMsg = !includePhoneInMsg }
                                    ) {
                                        Checkbox(
                                            checked = includePhoneInMsg,
                                            onCheckedChange = { includePhoneInMsg = it },
                                            colors = CheckboxDefaults.colors(checkedColor = ColorShopPrimary)
                                        )
                                        Text(
                                            text = "Mostrar número",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = ColorShopDeepPlum
                                        )
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = {
                                if (fullName.isBlank()) {
                                    Toast.makeText(context, "Ingresa el nombre del repartidor", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                onAddCourier(
                                    fullName,
                                    phone,
                                    docId,
                                    vehicleType,
                                    plate,
                                    selectedShift,
                                    includeNameInMsg,
                                    includePhoneInMsg
                                )
                                fullName = ""
                                phone = ""
                                docId = ""
                                plate = ""
                                avatarUri = null
                                showAddCourierForm = false
                                Toast.makeText(context, "Repartidor registrado con éxito", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ColorShopPrimary,
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.HowToReg, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Guardar y Asignar Repartidor", fontWeight = FontWeight.ExtraBold, color = Color.White)
                        }
                    }
                }
            }

            // Buscador y Chips de Filtro de Repartidores
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar por nombre, placa o teléfono...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ColorShopMutedPlum) },
                singleLine = true,
                shape = RoundedCornerShape(50),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ColorShopPrimary,
                    unfocusedBorderColor = Color.White,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CourierFilterChip(
                    label = "Todos (${couriers.size})",
                    selected = statusFilter == null,
                    onClick = { statusFilter = null }
                )
                CourierStatus.entries.forEach { st ->
                    val cnt = couriers.count { it.status == st }
                    CourierFilterChip(
                        label = "${st.label} ($cnt)",
                        selected = statusFilter == st,
                        onClick = { statusFilter = st }
                    )
                }
            }

            // Directorio Activo de Repartidores
            filteredCouriers.forEach { courier ->
                CourierDirectoryCard(
                    courier = courier,
                    onToggleIncludeName = { checked ->
                        onUpdateCourierVisibility(courier.id, checked, courier.includePhoneInCustomerMessage)
                    },
                    onToggleIncludePhone = { checked ->
                        onUpdateCourierVisibility(courier.id, courier.includeNameInCustomerMessage, checked)
                    },
                    onEditRequest = {
                        courierToEdit = courier
                    },
                    onDeleteRequest = {
                        courierToDelete = courier
                    }
                )
            }

            if (courierToEdit != null) {
                val currentEditing = courierToEdit!!
                var editName by remember(currentEditing) { mutableStateOf(currentEditing.name) }
                var editPhone by remember(currentEditing) { mutableStateOf(currentEditing.phone) }
                var editPlate by remember(currentEditing) { mutableStateOf(currentEditing.plate) }
                var editVehicle by remember(currentEditing) { mutableStateOf(currentEditing.vehicleModel) }
                var editDocId by remember(currentEditing) { mutableStateOf(currentEditing.documentId) }
                var editShift by remember(currentEditing) { mutableStateOf(currentEditing.shift) }
                var editStatus by remember(currentEditing) { mutableStateOf(currentEditing.status) }
                var editIncludeName by remember(currentEditing) { mutableStateOf(currentEditing.includeNameInCustomerMessage) }
                var editIncludePhone by remember(currentEditing) { mutableStateOf(currentEditing.includePhoneInCustomerMessage) }

                AlertDialog(
                    onDismissRequest = { courierToEdit = null },
                    title = {
                        Text(
                            text = "Editar Repartidor",
                            fontWeight = FontWeight.ExtraBold,
                            color = ColorShopDeepPlum
                        )
                    },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.verticalScroll(rememberScrollState())
                        ) {
                            OutlinedTextField(
                                value = editName,
                                onValueChange = { editName = it },
                                label = { Text("Nombre Completo") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = editPhone,
                                onValueChange = { editPhone = it },
                                label = { Text("Teléfono") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = editPlate,
                                onValueChange = { editPlate = it },
                                label = { Text("Placa") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = editVehicle,
                                onValueChange = { editVehicle = it },
                                label = { Text("Vehículo / Modelo") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = editDocId,
                                onValueChange = { editDocId = it },
                                label = { Text("Cédula / Documento") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Selector de Turno Asignado
                            Text(
                                text = "Turno Asignado",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorShopMutedPlum
                            )
                            val shiftsList = listOf("Mañana (8a-4p)", "Tarde/Noche", "Completo")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                shiftsList.forEach { shift ->
                                    val isSel = editShift == shift
                                    Surface(
                                        color = if (isSel) ColorShopPrimary else ColorShopSurfaceLow,
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { editShift = shift }
                                    ) {
                                        Text(
                                            text = shift,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSel) Color.White else ColorShopDeepPlum,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }

                            // Selector de Estado
                            Text(
                                text = "Estado del Repartidor",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorShopMutedPlum
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                CourierStatus.entries.forEach { st ->
                                    val isSel = editStatus == st
                                    val activeColor = when (st) {
                                        CourierStatus.AVAILABLE -> Color(0xFF2E7D32)
                                        CourierStatus.ON_ROUTE -> ColorShopTertiaryBlue
                                        CourierStatus.OFF_DUTY -> ColorShopMutedPlum
                                    }
                                    Surface(
                                        color = if (isSel) activeColor else ColorShopSurfaceLow,
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { editStatus = st }
                                    ) {
                                        Text(
                                            text = when (st) {
                                                CourierStatus.AVAILABLE -> "Disponible"
                                                CourierStatus.ON_ROUTE -> "En Ruta"
                                                CourierStatus.OFF_DUTY -> "Fuera Turno"
                                            },
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSel) Color.White else ColorShopDeepPlum,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { editIncludeName = !editIncludeName }
                            ) {
                                Checkbox(
                                    checked = editIncludeName,
                                    onCheckedChange = { editIncludeName = it },
                                    colors = CheckboxDefaults.colors(checkedColor = ColorShopPrimary)
                                )
                                Text("Incluir nombre en mensaje", fontSize = 12.sp)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { editIncludePhone = !editIncludePhone }
                            ) {
                                Checkbox(
                                    checked = editIncludePhone,
                                    onCheckedChange = { editIncludePhone = it },
                                    colors = CheckboxDefaults.colors(checkedColor = ColorShopPrimary)
                                )
                                Text("Incluir teléfono en mensaje", fontSize = 12.sp)
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val cleanName = editName.trim().ifBlank { currentEditing.name }
                                val words = cleanName.split(" ").filter { it.isNotBlank() }
                                val initials = if (words.size >= 2) {
                                    "${words[0].first().uppercaseChar()}${words[1].first().uppercaseChar()}"
                                } else {
                                    cleanName.take(2).uppercase(java.util.Locale.getDefault())
                                }
                                val statusDetail = when (editStatus) {
                                    CourierStatus.AVAILABLE -> "Disponible en tienda matriz"
                                    CourierStatus.ON_ROUTE -> "En ruta de entrega"
                                    CourierStatus.OFF_DUTY -> "Fuera de turno"
                                }
                                val updated = currentEditing.copy(
                                    name = cleanName,
                                    initials = initials,
                                    phone = editPhone.trim(),
                                    plate = editPlate.trim().uppercase(java.util.Locale.getDefault()),
                                    vehicleModel = editVehicle.trim(),
                                    vehicleType = editVehicle.trim(),
                                    documentId = editDocId.trim(),
                                    shift = editShift.trim(),
                                    status = editStatus,
                                    statusDetail = statusDetail,
                                    includeNameInCustomerMessage = editIncludeName,
                                    includePhoneInCustomerMessage = editIncludePhone
                                )
                                onUpdateCourier(updated)
                                Toast.makeText(context, "Repartidor \"$cleanName\" actualizado", Toast.LENGTH_SHORT).show()
                                courierToEdit = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ColorShopPrimary)
                        ) {
                            Text("Guardar Cambios", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        OutlinedButton(onClick = { courierToEdit = null }) {
                            Text("Cancelar", color = ColorShopDeepPlum)
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    containerColor = Color.White
                )
            }

            if (courierToDelete != null) {
                val courier = courierToDelete!!
                AlertDialog(
                    onDismissRequest = { courierToDelete = null },
                    title = {
                        Text(
                            text = "Eliminar Repartidor",
                            fontWeight = FontWeight.ExtraBold,
                            color = ColorShopDeepPlum
                        )
                    },
                    text = {
                        Text(
                            text = "¿Estás seguro de que deseas eliminar a \"${courier.name}\"? Esta acción lo removerá de la lista de repartidores.",
                            fontSize = 14.sp,
                            color = ColorShopDeepPlum
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                onDeleteCourier(courier.id)
                                Toast.makeText(context, "Repartidor \"${courier.name}\" eliminado", Toast.LENGTH_SHORT).show()
                                courierToDelete = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                        ) {
                            Text("Eliminar", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        OutlinedButton(onClick = { courierToDelete = null }) {
                            Text("Cancelar", color = ColorShopDeepPlum)
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    containerColor = Color.White
                )
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun CourierStatBox(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBg: Color,
    iconTint: Color,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 3.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
            }
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = ColorShopDeepPlum
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = ColorShopMutedPlum
            )
        }
    }
}

@Composable
private fun CourierFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) ColorShopPrimary else Color.White,
        shape = RoundedCornerShape(50),
        shadowElevation = 2.dp,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) Color.White else ColorShopMutedPlum,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun CourierDirectoryCard(
    courier: CourierDriver,
    onToggleIncludeName: (Boolean) -> Unit,
    onToggleIncludePhone: (Boolean) -> Unit,
    onUpdateCourier: (CourierDriver) -> Unit = {},
    onEditRequest: () -> Unit = {},
    onDeleteRequest: () -> Unit = {}
) {
    val context = LocalContext.current
    val isAvailable = courier.status == CourierStatus.AVAILABLE

    val cardBg = if (isAvailable) Color.White else Color(0xFFF9FAFB)
    val cardBorder = if (isAvailable) {
        BorderStroke(1.5.dp, Color(0xFF2E7D32))
    } else {
        BorderStroke(1.dp, Color(0xFFE5E7EB))
    }
    val cardElevation = if (isAvailable) 4.dp else 1.dp

    val avatarBg = if (isAvailable) Color(0xFFE8F5E9) else ColorShopSurfaceHigh
    val avatarText = if (isAvailable) Color(0xFF2E7D32) else ColorShopMutedPlum

    Surface(
        color = cardBg,
        shape = RoundedCornerShape(22.dp),
        border = cardBorder,
        shadowElevation = cardElevation,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(avatarBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = courier.initials,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = avatarText
                        )
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = courier.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isAvailable) ColorShopDeepPlum else ColorShopDeepPlum.copy(alpha = 0.85f)
                            )
                            Surface(
                                color = if (isAvailable) Color(0xFFE8F5E9) else ColorShopSurfaceHigh,
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = courier.badgeTag,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAvailable) Color(0xFF2E7D32) else ColorShopPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "${courier.vehicleModel} • Placa ${courier.plate} • 🕒 ${courier.shift} • 📞 ${courier.phone}",
                            fontSize = 12.sp,
                            color = ColorShopMutedPlum
                        )
                    }
                }

                Surface(
                    color = ColorShopSurfaceLow,
                    shape = RoundedCornerShape(50)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = ColorShopPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${courier.rating}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorShopDeepPlum
                        )
                    }
                }
            }

            // Casillas (checks) para controlar si el nombre y número salen en los mensajes al cliente
            Surface(
                color = ColorShopSurfaceLow.copy(alpha = 0.45f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onToggleIncludeName(!courier.includeNameInCustomerMessage) }
                    ) {
                        Checkbox(
                            checked = courier.includeNameInCustomerMessage,
                            onCheckedChange = onToggleIncludeName,
                            colors = CheckboxDefaults.colors(checkedColor = ColorShopPrimary)
                        )
                        Text(
                            text = "Nombre en mensaje",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ColorShopDeepPlum
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onToggleIncludePhone(!courier.includePhoneInCustomerMessage) }
                    ) {
                        Checkbox(
                            checked = courier.includePhoneInCustomerMessage,
                            onCheckedChange = onToggleIncludePhone,
                            colors = CheckboxDefaults.colors(checkedColor = ColorShopPrimary)
                        )
                        Text(
                            text = "Número en mensaje",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ColorShopDeepPlum
                        )
                    }
                }
            }

            // Barra de Estado y Gestión (Editar / Eliminar)
            Surface(
                color = ColorShopSurfaceLow.copy(alpha = 0.65f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var statusMenuExpanded by remember { mutableStateOf(false) }
                    val currentStatus = courier.status
                    val (statusColor, statusBg) = when (currentStatus) {
                        CourierStatus.AVAILABLE -> Pair(Color(0xFF2E7D32), Color(0xFFE8F5E9))
                        CourierStatus.ON_ROUTE -> Pair(ColorShopTertiaryBlue, ColorShopTertiaryBlue.copy(alpha = 0.12f))
                        CourierStatus.OFF_DUTY -> Pair(ColorShopMutedPlum, ColorShopMutedPlum.copy(alpha = 0.12f))
                    }

                    Box {
                        Surface(
                            color = statusBg,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.clickable { statusMenuExpanded = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(statusColor)
                                )
                                Text(
                                    text = courier.statusDetail,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Cambiar estado",
                                    tint = statusColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = statusMenuExpanded,
                            onDismissRequest = { statusMenuExpanded = false }
                        ) {
                            CourierStatus.entries.forEach { st ->
                                val stColor = when (st) {
                                    CourierStatus.AVAILABLE -> Color(0xFF2E7D32)
                                    CourierStatus.ON_ROUTE -> ColorShopTertiaryBlue
                                    CourierStatus.OFF_DUTY -> ColorShopMutedPlum
                                }
                                val stLabel = when (st) {
                                    CourierStatus.AVAILABLE -> "Disponible"
                                    CourierStatus.ON_ROUTE -> "En Ruta"
                                    CourierStatus.OFF_DUTY -> "Fuera de Turno"
                                }
                                val stDetail = when (st) {
                                    CourierStatus.AVAILABLE -> "Disponible en tienda matriz"
                                    CourierStatus.ON_ROUTE -> "En ruta de entrega"
                                    CourierStatus.OFF_DUTY -> "Fuera de turno"
                                }
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(stColor)
                                            )
                                            Text(
                                                text = stLabel,
                                                fontWeight = if (st == courier.status) FontWeight.Bold else FontWeight.Normal,
                                                color = if (st == courier.status) stColor else ColorShopDeepPlum
                                            )
                                        }
                                    },
                                    onClick = {
                                        statusMenuExpanded = false
                                        if (st != courier.status) {
                                            val updated = courier.copy(
                                                status = st,
                                                statusDetail = stDetail
                                            )
                                            onUpdateCourier(updated)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            color = ColorShopSurfaceLow,
                            shape = CircleShape,
                            shadowElevation = 1.dp,
                            modifier = Modifier
                                .size(32.dp)
                                .clickable { onEditRequest() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Editar repartidor",
                                    tint = ColorShopPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Surface(
                            color = Color(0xFFFFEBEE),
                            shape = CircleShape,
                            shadowElevation = 1.dp,
                            modifier = Modifier
                                .size(32.dp)
                                .clickable { onDeleteRequest() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Eliminar repartidor",
                                    tint = Color(0xFFD32F2F),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Botones Rápidos de Acción Directa: 📞 Llamar y 💬 WhatsApp
            val rawDigits = courier.phone.filter { it.isDigit() }
            val cleanPhone = if (rawDigits.startsWith("505")) rawDigits else "505$rawDigits"

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 📞 Llamar directo
                Button(
                    onClick = {
                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${courier.phone}"))
                        try {
                            context.startActivity(dialIntent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "No se pudo iniciar la llamada", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAvailable) Color(0xFFE8F5E9) else ColorShopSurfaceHigh,
                        contentColor = if (isAvailable) Color(0xFF2E7D32) else ColorShopDeepPlum
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Llamar",
                        tint = if (isAvailable) Color(0xFF2E7D32) else ColorShopDeepPlum,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Llamar",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isAvailable) Color(0xFF2E7D32) else ColorShopDeepPlum
                    )
                }

                // 💬 WhatsApp directo
                Button(
                    onClick = {
                        val waIntent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone")
                        )
                        try {
                            context.startActivity(waIntent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "No se pudo abrir WhatsApp", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF25D366),
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = "WhatsApp",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "WhatsApp",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
