package br.com.vroc.application.util

import br.com.vroc.domain.model.Partner
import br.com.vroc.domain.repositories.PartnerRepository
import com.fasterxml.jackson.databind.ObjectMapper

class PartnerDataSample(
    private val mapper: ObjectMapper,
    private val repository: PartnerRepository
) {

    val pdvsJson = javaClass.getResource("/pdvs.json").readBytes()

    fun load() {
        val pdvsTree = mapper.readTree(pdvsJson)["pdvs"].toString()
        val listType = mapper.typeFactory.constructCollectionType(MutableList::class.java, Partner::class.java)
        val pdvs = mapper.readValue<List<Partner>>(pdvsTree, listType)
        pdvs.forEach {
            try {
                repository.insert(it)
            } catch (e: Exception) {
                println("Error to insert ${it.id}")
            }
        }
    }

}