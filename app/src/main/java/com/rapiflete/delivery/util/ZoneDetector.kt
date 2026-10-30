package com.rapiflete.delivery.util

import com.rapiflete.delivery.model.GeoCoordinate
import com.rapiflete.delivery.model.SpecialZoneRule
import java.util.Locale

/**
 * Detector inteligente de zonas aledañas fuera de los distritos urbanos de Managua:
 * - Todo barrio dentro de los 7 distritos de Managua aplica tarifas normales.
 * - Zonas como Ciudad Sandino, Ticuantepe y Tipitapa aplican tarifas especiales automáticas.
 */
object ZoneDetector {

    data class BoundingBox(
        val minLat: Double,
        val maxLat: Double,
        val minLon: Double,
        val maxLon: Double
    ) {
        fun contains(lat: Double, lon: Double): Boolean =
            lat in minLat..maxLat && lon in minLon..maxLon
    }

    // Coordenadas geográficas delimitadas de municipios aledaños de Managua:
    // Ciudad Sandino (oeste de Managua, Cuesta del Plomo hacia afuera)
    private val CIUDAD_SANDINO_BOX = BoundingBox(
        minLat = 12.130,
        maxLat = 12.220,
        minLon = -86.390,
        maxLon = -86.325
    )

    // Ticuantepe (sur de Managua, km 14-22 Carretera a Masaya)
    private val TICUANTEPE_BOX = BoundingBox(
        minLat = 11.980,
        maxLat = 12.065,
        minLon = -86.240,
        maxLon = -86.170
    )

    // Tipitapa (este de Managua, pasando el aeropuerto hacia el este)
    private val TIPITAPA_BOX = BoundingBox(
        minLat = 12.135,
        maxLat = 12.280,
        minLon = -86.155,
        maxLon = -86.035
    )

    // Mateare (noroeste, km 18-30 Carretera Nueva a León)
    private val MATEARE_BOX = BoundingBox(
        minLat = 12.220,
        maxLat = 12.300,
        minLon = -86.490,
        maxLon = -86.390
    )

    // El Crucero (sur-suroeste en las montañas, km 18-30 Carretera Sur)
    private val EL_CRUCERO_BOX = BoundingBox(
        minLat = 11.940,
        maxLat = 12.030,
        minLon = -86.360,
        maxLon = -86.260
    )

    /**
     * Detecta si una coordenada o texto de dirección corresponde a una zona aledaña fuera de los distritos de Managua.
     * Retorna la regla [SpecialZoneRule] correspondiente de la lista de zonas configuradas en la app,
     * o `null` si pertenece a los barrios urbanos normales de Managua.
     */
    fun detectZone(
        coord: GeoCoordinate,
        addressText: String,
        availableZones: List<SpecialZoneRule>,
        townOrMunicipality: String? = null
    ): SpecialZoneRule? {
        val normalizedText = addressText.lowercase(Locale.ROOT)
        val normalizedTown = (townOrMunicipality ?: "").lowercase(Locale.ROOT)

        fun findZoneByKeyword(keyword: String): SpecialZoneRule? {
            return availableZones.firstOrNull { it.name.lowercase(Locale.ROOT).contains(keyword) }
        }

        // 1. Detección por nombre en el texto de la dirección o en los detalles de geocodificación
        if (normalizedText.contains("ciudad sandino") || normalizedTown.contains("ciudad sandino") ||
            (normalizedText.contains("sandino") && !normalizedText.contains("aeropuerto"))
        ) {
            findZoneByKeyword("ciudad sandino")?.let { return it }
        }

        if (normalizedText.contains("ticuantepe") || normalizedTown.contains("ticuantepe")) {
            findZoneByKeyword("ticuantepe")?.let { return it }
        }

        if (normalizedText.contains("tipitapa") || normalizedTown.contains("tipitapa")) {
            findZoneByKeyword("tipitapa")?.let { return it }
        }

        if (normalizedText.contains("mateare") || normalizedTown.contains("mateare")) {
            findZoneByKeyword("mateare")?.let { return it }
        }

        if (normalizedText.contains("el crucero") || normalizedText.contains("crucero") || normalizedTown.contains("crucero")) {
            findZoneByKeyword("crucero")?.let { return it }
        }

        // Detección genérica por cualquier zona personalizada creada por el usuario
        for (zone in availableZones) {
            val zName = zone.name.trim().lowercase(Locale.ROOT)
            if (zName.length >= 4 && (normalizedText.contains(zName) || normalizedTown.contains(zName))) {
                return zone
            }
        }

        // 2. Detección por coordenadas geográficas precisas
        val lat = coord.latitude
        val lon = coord.longitude

        if (CIUDAD_SANDINO_BOX.contains(lat, lon)) {
            findZoneByKeyword("ciudad sandino")?.let { return it }
        }

        if (TICUANTEPE_BOX.contains(lat, lon)) {
            findZoneByKeyword("ticuantepe")?.let { return it }
        }

        if (TIPITAPA_BOX.contains(lat, lon)) {
            findZoneByKeyword("tipitapa")?.let { return it }
        }

        if (MATEARE_BOX.contains(lat, lon)) {
            findZoneByKeyword("mateare")?.let { return it }
        }

        if (EL_CRUCERO_BOX.contains(lat, lon)) {
            findZoneByKeyword("crucero")?.let { return it }
        }

        // Si está dentro de los distritos urbanos de Managua, aplica tarifa normal
        return null
    }
}
