package br.com.vroc.domain.services

import br.com.vroc.domain.model.Partner
import br.com.vroc.domain.repositories.PartnerRepository
import org.geojson.LngLatAlt

class PartnerService (
    private val repository: PartnerRepository
) {

    fun create(partner: Partner) = repository.insert(partner)

    fun getById(id: String) = repository.getById(id)

    fun findNearestWithinCoverageArea(coordinates: LngLatAlt) =
        repository.findNearestWithinCoverageArea(coordinates)

}