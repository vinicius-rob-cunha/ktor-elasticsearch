package br.com.vroc.domain.repositories

import br.com.vroc.domain.model.Partner
import org.geojson.LngLatAlt

interface PartnerRepository {
    fun insert(partner: Partner)
    fun getById(id: String): Partner?
    fun findNearestWithinCoverageArea(coordinates: LngLatAlt): Partner?
}