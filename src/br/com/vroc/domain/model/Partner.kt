package br.com.vroc.domain.model

import org.geojson.MultiPolygon
import org.geojson.Point

data class Partner (
    val id : String,
    val tradingName: String,
    val ownerName: String,
    val document: String,
    val coverageArea: MultiPolygon,
    val address: Point
)