package br.com.vroc

import br.com.vroc.application.config.ESConfig
import br.com.vroc.application.config.ObjectMapperBuilder
import br.com.vroc.domain.model.Partner
import br.com.vroc.domain.repositories.PartnerRepository
import br.com.vroc.resources.elastic.PartnerRepositoryImpl
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import de.huxhorn.sulky.ulid.ULID
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.defaultRequest
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.header
import io.ktor.client.request.host
import io.ktor.client.request.port
import io.ktor.util.KtorExperimentalAPI
import kotlin.random.Random
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.elasticsearch.client.create
import org.geojson.LngLatAlt
import org.geojson.MultiPolygon
import org.geojson.Point
import org.geojson.Polygon
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS

@KtorExperimentalAPI
@TestInstance(PER_CLASS)
class ElasticsearchTest {

    private lateinit var repository: PartnerRepository
    lateinit var pdvs: List<Partner>

    @BeforeAll
    fun setUp() {
        val config = ESConfig()
        val mapper = ObjectMapperBuilder.getMapper()
        val httpClient = HttpClient(Apache) {
            install(JsonFeature) {
                serializer = JacksonSerializer(mapper)
            }
            defaultRequest {
                host = config.host
                port = config.port
                header("Content-Type", "application/json")
            }
        }
        val esClient = create(host = config.host, port = config.port)

        repository = PartnerRepositoryImpl(esClient, httpClient, mapper)

        loadData()
    }

    @Test
    fun `should create a partner`() {
        val partner = PartnerSample.create()
        repository.insert(partner)

        val dbPartner = repository.getById(partner.id)
        assertThat(dbPartner).isNotNull()
    }

    @Test
    fun `should load a partner`() {
        val partner = pdvs[Random.nextInt(pdvs.size)]
        val dbPartner = repository.getById(partner.id)

        assertThat(dbPartner).isNotNull()
        assertThat(dbPartner).isEqualTo(partner)
    }

    @Test
    fun `should find nearest partner within coverage area by geo point`() { runBlocking {
        val partner = pdvs[Random.nextInt(pdvs.size)]
        val dbPartner = repository.findNearestWithinCoverageArea(partner.address.coordinates)

        assertThat(dbPartner).isNotNull()
        assertThat(dbPartner?.id).isEqualTo(partner.id)
    } }

    private fun loadData() {
        val pdvsJson = javaClass.getResource("/pdvs.json").readBytes()

        val mapper = jacksonObjectMapper()
        val pdvsTree = mapper.readTree(pdvsJson)["pdvs"].toString()
        val listType = mapper.typeFactory.constructCollectionType(MutableList::class.java, Partner::class.java)
        pdvs = mapper.readValue(pdvsTree, listType)
        pdvs.forEach {
            try {
                repository.insert(it)
            } catch (e: Exception) {
                println("Error to insert ${it.id}")
            }
        }
    }

}

object PartnerSample {

    fun create() = Partner(
        id = ULID().nextULID(),
        tradingName = "Adega da Cerveja - Pinheiros",
        ownerName = "Zé da Silva",
        document = "1432132123891/0001",
        address = Point(-46.57421, -21.785741),
        coverageArea = MultiPolygon(
            Polygon(
                LngLatAlt(30.0, 20.0), LngLatAlt(45.0, 40.0),
                LngLatAlt(10.0, 40.0), LngLatAlt(30.0, 20.0)
            )
        ).add(
            Polygon(
                LngLatAlt(15.0, 5.0), LngLatAlt(40.0, 10.0),
                LngLatAlt(10.0, 20.0), LngLatAlt(5.0, 10.0),
                LngLatAlt(15.0, 5.0)
            )
        )
    )

}