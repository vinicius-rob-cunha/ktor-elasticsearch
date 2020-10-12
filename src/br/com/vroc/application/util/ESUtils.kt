package br.com.vroc.application.util

import br.com.vroc.application.config.ESConfig
import br.com.vroc.domain.model.Partner
import br.com.vroc.domain.repositories.PartnerRepository
import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

suspend fun waitForES(config: ESConfig, block: () -> Unit) {
    val client = HttpClient()
    var up = false
    println("connecting to http://${config.host}:${config.port}")
    while (!up) {
        try {
            val response = client.get<HttpResponse>("http://${config.host}:${config.port}")
            up = response.status == HttpStatusCode.OK
        } catch (e: Exception) { println("elasticsearch is down!") }
        delay(5000)
    }
    println("elasticsearch is up! running scripts")
    block()
}

class PartnerDataSample(
    private val mapper: ObjectMapper,
    private val config: ESConfig,
    private val repository: PartnerRepository
) {

    val pdvsJson = javaClass.getResource("/pdvs.json").readBytes()

    fun load() {
        val pdvsTree = mapper.readTree(pdvsJson)["pdvs"].toString()
        val listType = mapper.typeFactory.constructCollectionType(MutableList::class.java, Partner::class.java)
        val pdvs = mapper.readValue<List<Partner>>(pdvsTree, listType)

        runBlocking {
            waitForES(config) {
                pdvs.forEach {
                    try {
                        repository.insert(it)
                    } catch (e: Exception) {
                        println("Error to insert ${it.id}")
                    }
                }
            }
        }
    }

}