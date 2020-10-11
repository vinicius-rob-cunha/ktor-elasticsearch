package br.com.vroc

import br.com.vroc.model.Partner
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import de.huxhorn.sulky.ulid.ULID
import io.inbot.eskotlinwrapper.IndexRepository
import kotlin.random.Random
import org.assertj.core.api.Assertions.assertThat
import org.elasticsearch.action.search.source
import org.elasticsearch.client.configure
import org.elasticsearch.client.create
import org.elasticsearch.client.indexRepository
import org.geojson.LngLatAlt
import org.geojson.MultiPolygon
import org.geojson.Point
import org.geojson.Polygon
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS

@TestInstance(PER_CLASS)
class ElasticsearchTest {

    lateinit var repo: IndexRepository<Partner>
    lateinit var pdvs: List<Partner>

    @BeforeAll
    fun setUp() {
        val esClient = create(host = "localhost", port = 9200)
        repo = esClient.indexRepository("partners")
        createIndex()
        loadData()
    }

    @Test
    fun `should create a partner`() {
        val partner = PartnerSample.create()

        repo.index(partner.id, partner)

        val dbPartner = repo.get(partner.id)

        assertThat(dbPartner).isNotNull()
    }

    @Test
    fun `should load a partner`() {
        val partner = pdvs[Random.nextInt(pdvs.size)]
        val dbPartner = repo.get(partner.id)

        assertThat(dbPartner).isNotNull()
        assertThat(dbPartner).isEqualTo(partner)
    }

    @Test
    fun `should find by geopoint and return nearest partner`() {
        val partner = pdvs[Random.nextInt(pdvs.size)]
        val coordinates = partner.address.coordinates
        val query = """
                {
                  "size": 1,
                  "sort" : [
                    {
                        "_geo_distance" : {
                            "address.coordinates" : [${coordinates.longitude}, ${coordinates.latitude}],
                            "order" : "asc",
                            "unit" : "km",
                            "mode" : "min",
                            "distance_type" : "arc",
                            "ignore_unmapped": true
                        }
                    }
                  ],
                  "query": {
                    "bool": {
                      "must": {
                        "match_all": {}
                      },
                      "filter": {
                        "geo_shape": {
                          "coverage_area": {
                            "relation": "intersects",
                            "shape": {
                              "type": "point",
                              "coordinates": [${coordinates.longitude}", "${coordinates.latitude}]
                            }
                          }
                        }
                      }
                    }
                  }
                }
            """.trimIndent()
        val result = repo.search { source(query) }.mappedHits.toList()

        assertThat(result).isNotEmpty()
        assertThat(result.first().id).isEqualTo(partner.id)
    }

    private fun createIndex() {
        repo.deleteIndex()
        repo.createIndex {
            configure {
                mappings {
                    text("id")
                    text("trading_name")
                    text("owner_name")
                    field("coverage_area", "geo_shape")
                    objField("address") {
                        text("type")
                        field("coordinates", "geo_point")
                    }
                }
            }
        }
    }

    private fun loadData() {
        val pdvsJson = javaClass.getResource("/pdvs.json").readBytes()

        val mapper = jacksonObjectMapper()
        val pdvsTree = mapper.readTree(pdvsJson)["pdvs"].toString()
        val listType = mapper.typeFactory.constructCollectionType(MutableList::class.java, Partner::class.java)
        pdvs = mapper.readValue(pdvsTree, listType)
        pdvs.forEach {
            try {
                repo.index(it.id, it)
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