package com.rapiflete.delivery.util

import com.rapiflete.delivery.model.FrozenDestination
import com.rapiflete.delivery.model.GeoCoordinate
import com.rapiflete.delivery.model.LocationSourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow

object LocationParserAndFreezer {

    private const val OLC_ALPHABET = "23456789CFGHJMPQRVWX"

    // Patrones comunes en links de WhatsApp, Google Maps, Waze y geo: URIs
    private val BANG_3D_4D_REGEX = Regex("""!3d(-?\d+\.\d+)!4d(-?\d+\.\d+)""")
    private val AT_COORD_REGEX = Regex("""@(-?\d+\.\d+),(-?\d+\.\d+)""")
    private val QUERY_COORD_REGEX = Regex("""(?:q|ll|center|daddr|destination|saddr|loc|query)=(-?\d+\.\d+)\s*(?:,|%2C|\s)\s*(-?\d+\.\d+)""", RegexOption.IGNORE_CASE)
    private val SEARCH_PATH_COORD_REGEX = Regex("""/(?:search|place|dir)/(-?\d+\.\d+)\s*(?:,|%2C|\+)\s*(-?\d+\.\d+)""")
    private val GEO_URI_REGEX = Regex("""geo:(-?\d+\.\d+),(-?\d+\.\d+)""", RegexOption.IGNORE_CASE)
    private val GENERIC_LAT_LNG_REGEX = Regex("""(?<![0-9.])(-?[1-8]?\d\.\d{3,10})\s*,\s*(-?(?:1[0-7]\d|[1-9]?\d)\.\d{3,10})(?![0-9.])""")
    private val URL_EXTRACTOR_REGEX = Regex("""https?://[^\s"<>]+""", RegexOption.IGNORE_CASE)

    // Plus Code (Open Location Code): completo (ej. 764M5M5V+9P) o corto (ej. 5M5V+9P)
    private val PLUS_CODE_REGEX = Regex("""\b([23456789CFGHJMPQRVWX]{4,8}\+[23456789CFGHJMPQRVWX]{2,4})\b""", RegexOption.IGNORE_CASE)

    // Patrón para detectar números telefónicos en mensajes compartidos de WhatsApp (ej. "+505 8899-1122", "8823-4567", "77448833")
    private val NICARAGUA_PHONE_REGEX = Regex("""(?:\+?505[\s-]*)?\b([578]\d{3}[\s-]?\d{4})\b""")

    fun currentTimeFormatted(): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }

    /**
     * Extrae automáticamente un número telefónico si viene incluido en el texto compartido de WhatsApp.
     */
    fun extractPhoneNumberFromText(rawInput: String): String? {
        val cleaned = rawInput.replace(URL_EXTRACTOR_REGEX, " ")
        val match = NICARAGUA_PHONE_REGEX.find(cleaned) ?: return null
        val digits = match.groupValues[1].filter { it.isDigit() }
        return if (digits.length == 8) {
            "${digits.substring(0, 4)}-${digits.substring(4)}"
        } else {
            match.value.trim()
        }
    }

    /**
     * Intenta extraer coordenadas o Plus Code de forma síncrona (sin red) a partir de texto,
     * un mensaje de WhatsApp, URI geo:, coordenadas o Plus Code como "5M5V+9P Managua".
     */
    fun parseDirectOrPlusCode(
        rawInput: String,
        referenceLat: Double = 12.1364,
        referenceLng: Double = -86.2514
    ): FrozenDestination? {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return null

        val decodedText = try {
            URLDecoder.decode(trimmed.replace("+", "%2B"), "UTF-8")
        } catch (_: Exception) {
            trimmed
        }

        // 1. Priorizar !3dLAT!4dLNG (coordenada exacta del pin en Google Maps)
        BANG_3D_4D_REGEX.find(decodedText)?.let { match ->
            val coord = toValidCoordinate(match.groupValues[1], match.groupValues[2])
            if (coord != null) {
                return buildFrozen(
                    coord = coord,
                    label = extractCustomLabel(decodedText) ?: "Pin exacto de Google Maps",
                    raw = trimmed,
                    sourceType = LocationSourceType.WHATSAPP_SHARE
                )
            }
        }

        // 2. Parámetros ?q=lat,lng / ll=lat,lng (típico de Ubicación de WhatsApp)
        QUERY_COORD_REGEX.find(decodedText)?.let { match ->
            val coord = toValidCoordinate(match.groupValues[1], match.groupValues[2])
            if (coord != null) {
                return buildFrozen(
                    coord = coord,
                    label = extractCustomLabel(decodedText) ?: "Ubicación de WhatsApp congelada",
                    raw = trimmed,
                    sourceType = LocationSourceType.WHATSAPP_SHARE
                )
            }
        }

        // 3. Esquema geo:lat,lng (cuando abren ubicación en tiempo real desde WhatsApp -> Indicaciones)
        GEO_URI_REGEX.find(decodedText)?.let { match ->
            val coord = toValidCoordinate(match.groupValues[1], match.groupValues[2])
            if (coord != null) {
                return buildFrozen(
                    coord = coord,
                    label = extractCustomLabel(decodedText) ?: "Ubicación en tiempo real congelada",
                    raw = trimmed,
                    sourceType = LocationSourceType.WHATSAPP_SHARE
                )
            }
        }

        // 4. Ruta /search/lat,lng o /place/lat,lng
        SEARCH_PATH_COORD_REGEX.find(decodedText)?.let { match ->
            val coord = toValidCoordinate(match.groupValues[1], match.groupValues[2])
            if (coord != null) {
                return buildFrozen(
                    coord = coord,
                    label = "Pin de mapa congelado",
                    raw = trimmed,
                    sourceType = LocationSourceType.WHATSAPP_SHARE
                )
            }
        }

        // 5. @lat,lng en URL de Google Maps
        AT_COORD_REGEX.find(decodedText)?.let { match ->
            val coord = toValidCoordinate(match.groupValues[1], match.groupValues[2])
            if (coord != null) {
                return buildFrozen(
                    coord = coord,
                    label = extractCustomLabel(decodedText) ?: "Ubicación de Maps (@lat,lng)",
                    raw = trimmed,
                    sourceType = LocationSourceType.WHATSAPP_SHARE
                )
            }
        }

        // 6. Plus Code (ej. "5M5V+9P Managua" o "764M5M5V+9P")
        PLUS_CODE_REGEX.find(decodedText)?.let { match ->
            val plusCodeToken = match.groupValues[1].uppercase(Locale.US)
            val decodedCoord = decodePlusCode(plusCodeToken, referenceLat, referenceLng)
            if (decodedCoord != null) {
                return buildFrozen(
                    coord = decodedCoord,
                    label = "Plus Code ($plusCodeToken)",
                    raw = trimmed,
                    sourceType = LocationSourceType.PLUS_CODE
                )
            }
        }

        // 7. Coordenadas directas "12.158438, -86.305688" en cualquier parte del texto
        GENERIC_LAT_LNG_REGEX.find(decodedText)?.let { match ->
            val coord = toValidCoordinate(match.groupValues[1], match.groupValues[2])
            if (coord != null) {
                val isFromUrl = decodedText.contains("http", ignoreCase = true)
                return buildFrozen(
                    coord = coord,
                    label = if (isFromUrl) "Ubicación compartida congelada" else "Coordenadas (${coord.format(5)})",
                    raw = trimmed,
                    sourceType = if (isFromUrl) LocationSourceType.WHATSAPP_SHARE else LocationSourceType.COORDINATES
                )
            }
        }

        return null
    }

    /**
     * Resuelve cualquier entrada: enlaces cortos (maps.app.goo.gl / goo.gl), enlaces de WhatsApp,
     * Plus Codes, coordenadas o nombres de calles/barrios mediante geocodificación OSM.
     */
    suspend fun resolveAndFreezeInput(
        rawInput: String,
        referenceLat: Double = 12.158438,
        referenceLng: Double = -86.305688
    ): Result<FrozenDestination> = withContext(Dispatchers.IO) {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Ingresa o pega una ubicación, enlace o coordenadas."))
        }

        // Paso 1: Intentar extracción directa o Plus Code sin depender de red
        // (Si NO es un link corto maps.app.goo.gl sin coordenadas visibles)
        parseDirectOrPlusCode(trimmed, referenceLat, referenceLng)?.let { direct ->
            return@withContext Result.success(direct)
        }

        // Paso 2: Si contiene una URL (ej. https://maps.app.goo.gl/mv39EgWrVBGNy1GJ7), resolver redirecciones HTTP
        val urlMatch = URL_EXTRACTOR_REGEX.find(trimmed)?.value
        if (urlMatch != null) {
            val resolvedFromUrl = resolveShortMapsUrl(urlMatch, referenceLat, referenceLng)
            if (resolvedFromUrl != null) {
                return@withContext Result.success(resolvedFromUrl)
            }
        }

        // Paso 3: Buscar como dirección / referencia en Nominatim (OpenStreetMap)
        val geocoded = geocodeAddressWithNominatim(trimmed, referenceLat, referenceLng)
        if (geocoded != null) {
            return@withContext Result.success(geocoded)
        }

        Result.failure(
            IllegalArgumentException(
                "No se pudieron extraer coordenadas de \"$trimmed\". Pega el link de WhatsApp/Google Maps, un Plus Code (ej. 5M5V+9P) o coordenadas (lat, lng)."
            )
        )
    }

    private fun resolveShortMapsUrl(
        initialUrl: String,
        referenceLat: Double,
        referenceLng: Double
    ): FrozenDestination? {
        var currentUrl = initialUrl
        var redirectsLeft = 6

        try {
            while (redirectsLeft > 0) {
                parseDirectOrPlusCode(currentUrl, referenceLat, referenceLng)?.let { parsed ->
                    return parsed.copy(
                        label = if (parsed.label.startsWith("Ubicación")) "Enlace Maps congelado" else parsed.label,
                        sourceRawText = initialUrl,
                        sourceType = LocationSourceType.SHORT_LINK
                    )
                }

                val connection = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = false
                    connectTimeout = 7000
                    readTimeout = 7000
                    setRequestProperty(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
                    )
                }

                val code = connection.responseCode
                val locationHeader = connection.getHeaderField("Location")

                if (code in 300..399 && !locationHeader.isNullOrBlank()) {
                    val nextUrl = if (locationHeader.startsWith("http")) {
                        locationHeader
                    } else {
                        URL(URL(currentUrl), locationHeader).toString()
                    }
                    connection.disconnect()
                    currentUrl = nextUrl
                    redirectsLeft--

                    // Revisar si la URL redirigida ya trae coordenadas o Plus Code
                    parseDirectOrPlusCode(currentUrl, referenceLat, referenceLng)?.let { parsed ->
                        return parsed.copy(
                            sourceRawText = initialUrl,
                            sourceType = LocationSourceType.SHORT_LINK
                        )
                    }

                    // O si trae ?q=Nombre+del+Lugar en Google Maps
                    extractQueryPlaceFromMapsUrl(currentUrl)?.let { queryPlace ->
                        parseDirectOrPlusCode(queryPlace, referenceLat, referenceLng)?.let { parsed ->
                            return parsed.copy(
                                sourceRawText = initialUrl,
                                sourceType = LocationSourceType.SHORT_LINK
                            )
                        }
                    }
                } else {
                    // Leer primeros KB del HTML por si contiene coordenadas [lat,lng] o meta refresh
                    val bodySnippet = try {
                        BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                            val sb = StringBuilder()
                            val buf = CharArray(4096)
                            var total = 0
                            while (total < 32768) {
                                val n = reader.read(buf)
                                if (n <= 0) break
                                sb.append(buf, 0, n)
                                total += n
                            }
                            sb.toString()
                        }
                    } catch (_: Exception) {
                        ""
                    } finally {
                        connection.disconnect()
                    }

                    parseDirectOrPlusCode(bodySnippet, referenceLat, referenceLng)?.let { parsed ->
                        return parsed.copy(
                            label = "Enlace de Google Maps congelado",
                            sourceRawText = initialUrl,
                            sourceType = LocationSourceType.SHORT_LINK
                        )
                    }
                    break
                }
            }
        } catch (_: Exception) {
            // Ignorar error de red y permitir fallback
        }
        return null
    }

    /**
     * Bounding box oficial del departamento de Managua, Nicaragua:
     * Abarca los 9 municipios: Managua, Ciudad Sandino, Tipitapa, Ticuantepe,
     * Mateare, El Crucero, Villa El Carmen, San Rafael del Sur y San Francisco Libre.
     * Lat: [11.80, 12.60], Lon: [-86.70, -85.90]
     */
    fun isCoordinateInManaguaDepartment(lat: Double, lon: Double): Boolean {
        return lat in 11.80..12.60 && lon in -86.70..-85.90
    }

    private val MANAGUA_MUNICIPALITIES = listOf(
        "managua", "ciudad sandino", "tipitapa", "ticuantepe",
        "mateare", "el crucero", "villa el carmen", "san rafael del sur", "san francisco libre"
    )

    fun isDisplayNameInManaguaDepartment(displayName: String): Boolean {
        val lower = displayName.lowercase(Locale.ROOT)
        return MANAGUA_MUNICIPALITIES.any { lower.contains(it) }
    }

    /**
     * Busca la dirección priorizando estrictamente el departamento de Managua, Nicaragua.
     * Incorpora ", Managua, Nicaragua" como preposición predefinida obligatoria
     * para evitar errores con nombres homónimos en otras ciudades o países.
     */
    private fun geocodeAddressWithNominatim(
        query: String,
        referenceLat: Double,
        referenceLng: Double
    ): FrozenDestination? {
        val cleanQuery = query.trim()
        val queryWithManagua = when {
            cleanQuery.contains("managua", ignoreCase = true) && cleanQuery.contains("nicaragua", ignoreCase = true) -> cleanQuery
            cleanQuery.contains("managua", ignoreCase = true) -> "$cleanQuery, Nicaragua"
            else -> "$cleanQuery, Managua, Nicaragua"
        }

        // Intento 1: Con ", Managua, Nicaragua" acotado al departamento de Managua (viewbox + bounded=1 + countrycodes=ni)
        val managuaBoundedUrl =
            "https://nominatim.openstreetmap.org/search?format=json&limit=5&countrycodes=ni&viewbox=-86.70,12.60,-85.90,11.80&bounded=1&q=${URLEncoder.encode(queryWithManagua, "UTF-8")}"
        queryNominatimEndpoint(managuaBoundedUrl, cleanQuery, referenceLat, referenceLng, requireManaguaDepartment = true)?.let { return it }

        // Intento 2: Con ", Managua, Nicaragua" dentro de Nicaragua sin forzar bounded estricto
        val managuaExplicitUrl =
            "https://nominatim.openstreetmap.org/search?format=json&limit=5&countrycodes=ni&viewbox=-86.70,12.60,-85.90,11.80&q=${URLEncoder.encode(queryWithManagua, "UTF-8")}"
        queryNominatimEndpoint(managuaExplicitUrl, cleanQuery, referenceLat, referenceLng, requireManaguaDepartment = true)?.let { return it }

        // Intento 3: Consulta original acotada al departamento de Managua
        val rawBoundedUrl =
            "https://nominatim.openstreetmap.org/search?format=json&limit=5&countrycodes=ni&viewbox=-86.70,12.60,-85.90,11.80&bounded=1&q=${URLEncoder.encode(cleanQuery, "UTF-8")}"
        queryNominatimEndpoint(rawBoundedUrl, cleanQuery, referenceLat, referenceLng, requireManaguaDepartment = true)?.let { return it }

        // Intento 4: Si no hubo coincidencia estricta en Managua, buscar en Nicaragua priorizando cercanía a Managua
        val nicaraguaUrl =
            "https://nominatim.openstreetmap.org/search?format=json&limit=5&countrycodes=ni&viewbox=-86.70,12.60,-85.90,11.80&q=${URLEncoder.encode(queryWithManagua, "UTF-8")}"
        return queryNominatimEndpoint(nicaraguaUrl, cleanQuery, referenceLat, referenceLng, requireManaguaDepartment = false)
    }

    private fun queryNominatimEndpoint(
        urlString: String,
        originalQuery: String,
        referenceLat: Double,
        referenceLng: Double,
        requireManaguaDepartment: Boolean = false
    ): FrozenDestination? {
        return try {
            val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
                connectTimeout = 7000
                readTimeout = 7000
                setRequestProperty("User-Agent", "ColorShopDeliveryApp/1.0 (Android; Managua)")
                setRequestProperty("Accept-Language", "es")
            }
            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val arr = JSONArray(response)
                var bestCoord: GeoCoordinate? = null
                var bestDisplayName: String = originalQuery
                var bestDistanceSq = Double.MAX_VALUE

                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val lat = obj.optString("lat").toDoubleOrNull()
                    val lon = obj.optString("lon").toDoubleOrNull()
                    val displayName = obj.optString("display_name", "")

                    if (lat != null && lon != null) {
                        val inDepartment = isCoordinateInManaguaDepartment(lat, lon) || isDisplayNameInManaguaDepartment(displayName)
                        if (requireManaguaDepartment && !inDepartment) {
                            continue
                        }

                        val dLat = lat - referenceLat
                        val dLon = lon - referenceLng
                        val distSq = dLat * dLat + dLon * dLon
                        if (distSq < bestDistanceSq) {
                            bestDistanceSq = distSq
                            bestCoord = GeoCoordinate(lat, lon)
                            bestDisplayName = displayName
                                .split(",")
                                .take(2)
                                .joinToString(",")
                                .trim()
                        }
                    }
                }

                if (bestCoord != null) {
                    return buildFrozen(
                        coord = bestCoord,
                        label = bestDisplayName.ifEmpty { originalQuery },
                        raw = originalQuery,
                        sourceType = LocationSourceType.ADDRESS_SEARCH
                    )
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Decodifica un Open Location Code (Plus Code) completo (ej. "764M5M5V+9P")
     * o corto (ej. "5M5V+9P") usando la ubicación de referencia (por defecto Managua).
     */
    fun decodePlusCode(
        rawCode: String,
        referenceLat: Double = 12.1364,
        referenceLng: Double = -86.2514
    ): GeoCoordinate? {
        val clean = rawCode.trim().uppercase(Locale.US)
        if (!clean.contains("+")) return null

        val parts = clean.split("+")
        if (parts.size != 2) return null
        val prefix = parts[0]
        val suffix = parts[1]

        if (prefix.length < 4 || prefix.length % 2 != 0) return null

        // Si es un Plus Code corto (4 o 6 caracteres antes del '+'), recuperar prefijo con referencia
        val fullCodeNoPlus = if (prefix.length < 8) {
            val paddingLength = 8 - prefix.length
            val resolution = 20.0.pow(2.0 - (paddingLength / 2.0))
            val halfRes = resolution / 2.0

            val refLatShifted = (referenceLat + 90.0).coerceIn(0.0, 179.999999)
            val refLngShifted = (referenceLng + 180.0).coerceIn(0.0, 359.999999)

            // Construir prefijo de área de la referencia
            val areaPrefix = encodeLatLngPrefix(refLatShifted, refLngShifted, paddingLength)
            val candidateCode = areaPrefix + prefix + suffix
            val decodedCandidate = decodeFullPlusCodeWithoutPlus(candidateCode) ?: return null

            var bestLat = decodedCandidate.latitude
            var bestLng = decodedCandidate.longitude

            if (bestLat - referenceLat > halfRes && bestLat - resolution >= -90.0) {
                bestLat -= resolution
            } else if (referenceLat - bestLat > halfRes && bestLat + resolution <= 90.0) {
                bestLat += resolution
            }

            if (bestLng - referenceLng > halfRes && bestLng - resolution >= -180.0) {
                bestLng -= resolution
            } else if (referenceLng - bestLng > halfRes && bestLng + resolution <= 180.0) {
                bestLng += resolution
            }

            return GeoCoordinate(bestLat, bestLng)
        } else {
            prefix + suffix
        }

        return decodeFullPlusCodeWithoutPlus(fullCodeNoPlus)
    }

    private fun encodeLatLngPrefix(latShifted: Double, lngShifted: Double, length: Int): String {
        val sb = StringBuilder()
        var latRem = latShifted
        var lngRem = lngShifted
        var placeValue = 20.0

        val pairs = length / 2
        for (i in 0 until pairs) {
            val latDigit = floor(latRem / placeValue).toInt().coerceIn(0, 19)
            val lngDigit = floor(lngRem / placeValue).toInt().coerceIn(0, 19)
            latRem -= latDigit * placeValue
            lngRem -= lngDigit * placeValue
            sb.append(OLC_ALPHABET[latDigit])
            sb.append(OLC_ALPHABET[lngDigit])
            placeValue /= 20.0
        }
        return sb.toString()
    }

    private fun decodeFullPlusCodeWithoutPlus(code: String): GeoCoordinate? {
        if (code.length < 8) return null
        var lat = 0.0
        var lng = 0.0
        var latPlace = 400.0
        var lngPlace = 400.0

        val pairLimit = minOf(code.length, 10)
        var i = 0
        while (i + 1 < pairLimit) {
            latPlace /= 20.0
            lngPlace /= 20.0
            val c1 = OLC_ALPHABET.indexOf(code[i])
            val c2 = OLC_ALPHABET.indexOf(code[i + 1])
            if (c1 < 0 || c2 < 0) return null
            lat += c1 * latPlace
            lng += c2 * lngPlace
            i += 2
        }

        val finalLat = lat - 90.0 + (latPlace / 2.0)
        val finalLng = lng - 180.0 + (lngPlace / 2.0)
        if (abs(finalLat) > 90.0 || abs(finalLng) > 180.0) return null
        return GeoCoordinate(finalLat, finalLng)
    }

    private fun extractQueryPlaceFromMapsUrl(url: String): String? {
        val regex = Regex("""[?&](?:q|query)=([^&]+)""")
        val match = regex.find(url) ?: return null
        return try {
            URLDecoder.decode(match.groupValues[1], "UTF-8")
        } catch (_: Exception) {
            match.groupValues[1]
        }
    }

    private fun extractCustomLabel(decodedText: String): String? {
        // Si viene de geo:lat,lng?q=lat,lng(Etiqueta)
        val parenRegex = Regex("""\(([^()]{2,50})\)""")
        parenRegex.find(decodedText)?.let {
            val candidate = it.groupValues[1].replace("+", " ").trim()
            if (candidate.any { ch -> ch.isLetter() }) {
                return candidate
            }
        }
        return null
    }

    private fun toValidCoordinate(latStr: String, lngStr: String): GeoCoordinate? {
        val lat = latStr.toDoubleOrNull() ?: return null
        val lng = lngStr.toDoubleOrNull() ?: return null
        if (lat !in -90.0..90.0 || lng !in -180.0..180.0) return null
        if (lat == 0.0 && lng == 0.0) return null
        return GeoCoordinate(lat, lng)
    }

    private fun buildFrozen(
        coord: GeoCoordinate,
        label: String,
        raw: String,
        sourceType: LocationSourceType
    ): FrozenDestination {
        return FrozenDestination(
            coordinate = coord,
            label = label,
            sourceRawText = raw,
            capturedAtTime = currentTimeFormatted(),
            isFrozen = true,
            sourceType = sourceType
        )
    }
}
