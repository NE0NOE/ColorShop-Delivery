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
- **Tarifa Preferencial Automática por Umbral de Distancia ⭐**:
  - Detección automática cuando una ruta excede el umbral razonable de costo (por defecto > C$ 150).
  - Pop-up sugerido para aplicar una tarifa preferencial reducida (ej. reduce de C$ 175 a C$ 150 con -C$ 25 de ahorro / 15% de reducción) para evitar que el cliente desista del envío.
  - Opciones de ajuste personalizado, presets rápidos y control en Configuración para activar/desactivar y definir el umbral y porcentaje.
  - Reflejo automático en WhatsApp como descuento de tarifa preferencial para mayor transparencia y satisfacción del cliente.
- **Precio final editable**: Permite acordar un monto personalizado para facturación manteniendo el estimado original como referencia.
- **Datos del Cliente, Entrega y Foto**:
  - Nombre del cliente / persona que recibe.
  - Teléfono del cliente (detección automática desde WhatsApp o ingreso manual).
  - Hora aproximada de entrega.
  - Comentarios y referencias del pedido.
  - **Adjunto de foto de recibo o respaldo** almacenada localmente en la aplicación.
- **Métodos de cobro**: Efectivo, POS / Tarjeta y **Transferencia Bancaria** con selección entre **BAC**, **Lafise**, **Banpro** y **Ficohsa**.

### 3. Mensajes y Comprobantes Diferenciados (Cliente vs. Delivery) 📲🖼️
- **Recibo Oficial Digital para Cliente**:
  - Al generar el comprobante en imagen para el cliente, el recuadro final muestra la forma de pago (Efectivo, Transferencia o POS) y un mensaje cordial de confirmación.
  - Se eliminan por completo las notas internas de liquidación al motorizado, retención de tarifas y auto-descuentos en el recibo del cliente.
- **Hoja de Despacho Digital para Motorizado**:
  - Al generar la imagen para el repartidor, incluye los montos a cobrar en entrega, el desglose de mercadería y el recuadro destacado de liquidación (si retiene su tarifa en efectivo o si ColorShop le liquida su envío por transferencia/POS).
- **Acceso Directo a Google Maps en WhatsApp**:
  - Tanto en la cotización al cliente como en el despacho al motorizado, el destino incluye un enlace directo y clickeable de Google Maps (`https://maps.google.com/?q=...`).
  - Eliminación de etiquetas técnicas como "congelada" o "congelado", mostrando el nombre real del barrio, calle o punto en mapa.
- **Geocodificación Inversa Automática**:
  - Al recibir un pin o link de ubicación de WhatsApp, el sistema consulta OpenStreetMap Nominatim para resolver automáticamente el nombre legible de la calle, barrio o municipio.

### 4. Detección Automática de Zonas Aledañas Fuera de Managua 📍
- **Tarifas estándar para Managua Urbana**:
  - Todo barrio dentro de los 7 distritos urbanos de Managua aplica automáticamente las tarifas normales de la aplicación (tarifa mínima base C$ 40 + km adicional).
- **Detección automática por coordenadas y nombre**:
  - Si el destino se encuentra fuera de los distritos urbanos de Managua, el sistema detecta y aplica automáticamente la zona correspondiente:
    - **Ciudad Sandino**: Tarifa base de **C$ 100**.
    - **Ticuantepe**: Tarifa base de **C$ 120**.
    - **Tipitapa**: Tarifa base de **C$ 120**.
  - Si el usuario cambia el destino a un barrio dentro de Managua, la cotización se restaura automáticamente a la tarifa estándar.

### 5. Administración de Repartidores y Visibilidad 🛵
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
