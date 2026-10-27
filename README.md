# ColorShop Delivery 🦋 • Cotizador de Envíos y Despacho para Managua (Android Nativo)

Aplicación móvil nativa para Android (**Kotlin + Jetpack Compose + OpenStreetMap + OSRM**) diseñada para **ColorShop Delivery** en Managua, Nicaragua. Permite recibir ubicaciones compartidas desde WhatsApp o Google Maps, calcular tarifas de envío por kilometraje real en calles, gestionar motorizados y despachar cotizaciones con un toque.

---

## ✨ Características Principales

### 1. Mapa Interactivo con Rotación y Prioridad en Managua 🗺️
- **Giro libre del mapa**: Gira el mapa con dos dedos (`RotationGestureOverlay`) y restablece el Norte con un toque en el botón de brújula.
- **Buscador centrado en el Departamento de Managua**: Geocodificación acotada a Managua, Nicaragua (`countrycodes=ni`, `viewbox` de Managua) para evitar resultados fuera del país.
- **Recepción directa desde WhatsApp y Google Maps (`Intent.ACTION_SEND` y `Intent.ACTION_VIEW`)**: Captura coordenadas exactas (`lat, lng`), enlaces cortos (`maps.app.goo.gl`), Plus Codes (`5M5V+9P Managua`) y extrae automáticamente el número telefónico del cliente si viene en el mensaje.

### 2. Cotización Editable y Datos Completos del Pedido 📦
- **Tarifa mínima + kilómetros adicionales** calculados por ruta real en calles (OSRM) o línea recta ajustada.
- **Precio final editable**: Permite acordar un monto personalizado para facturación manteniendo el estimado original como referencia.
- **Datos del Cliente, Entrega y Foto**:
  - Nombre del cliente / persona que recibe.
  - Teléfono del cliente (detección automática desde WhatsApp o ingreso manual).
  - Hora aproximada de entrega.
  - Comentarios y referencias del pedido.
  - **Adjunto de foto de recibo o respaldo** almacenada localmente en la aplicación.
- **Métodos de cobro**: Efectivo, POS / Tarjeta y **Transferencia Bancaria** con selección entre **BAC**, **Lafise**, **Banpro** y **Ficohsa**.

### 3. Mensajes Personalizados para Cliente y Motorizado 📲
- **Botón Cliente (con ícono de mariposa)**: Envía la cotización completa con destino, distancia, hora aproximada de entrega, comentarios, datos del motorizado asignado y **horarios de atención habilitados** de la tienda (omitiendo automáticamente el domingo cuando esté desactivado).
- **Botón Delivery**: Envía al repartidor los datos del cliente, comentarios, hora aproximada de entrega, distancia y el enlace directo de **Ubicación de entrega** en Google Maps (sin incluir montos de pago ni tienda de salida).

### 4. Administración de Repartidores y Visibilidad 🛵
- Directorio de motorizados con registro de nombre, placa, teléfono y turno.
- **Eliminación de repartidores**: Opción para eliminar motorizados con confirmación modal para prevenir borrados accidentales.
- **Casillas de visibilidad individuales** (`Mostrar nombre` y `Mostrar número`) para controlar si el nombre y/o teléfono de cada motorizado se incluyen en la cotización enviada al cliente.

### 5. Historial Diario con Calendario por Fechas 📅
- **Reinicio automático cada nuevo día**: Las métricas diarias (*Envíos*, *Total*, *Ruta*) muestran únicamente los envíos de la fecha seleccionada sin arrastrar datos de días anteriores.
- **Selector de Calendario**: Permite desplazarse entre fechas pasadas para consultar envíos anteriores, ver comprobantes, notas, fotos de respaldo adjuntas y exportar reportes.

---

## 🛠️ Requisitos y Compilación

1. Clonar el repositorio y abrir la carpeta en **Android Studio** (JDK 17 o 21).
2. Compilar el APK de depuración desde terminal:
   ```bash
   ./gradlew assembleDebug
   ```
3. Ejecutar pruebas unitarias:
   ```bash
   ./gradlew testDebugUnitTest
   ```
