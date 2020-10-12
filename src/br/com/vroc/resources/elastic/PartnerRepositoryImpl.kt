package br.com.vroc.resources.elastic

import br.com.vroc.domain.model.Partner
import br.com.vroc.domain.repositories.PartnerRepository
import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlinx.coroutines.runBlocking
import org.elasticsearch.ElasticsearchStatusException
import org.elasticsearch.action.support.ActiveShardCount
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.client.configure
import org.elasticsearch.client.indexRepository
import org.geojson.LngLatAlt

const val PARTNERS_INDEX_NAME = "partners"

class PartnerRepositoryImpl(
    esClient: RestHighLevelClient,
    private val client: HttpClient,
    private val mapper: ObjectMapper
) : PartnerRepository {

    private val repo = esClient.indexRepository<Partner>(PARTNERS_INDEX_NAME)

    init {
        createIndex()
    }

    override fun insert(partner: Partner) {
        //create = false habilita para sobreescrever o registro se já existir, isso só esta sendo usado
        //para facilitar os testes e por ser um projeto demo
        repo.index(partner.id, partner, create = false)
    }

    override fun getById(id: String) = repo.get(id)

    override fun findNearestWithinCoverageArea(coordinates: LngLatAlt): Partner? {
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
                          "coordinates": [${coordinates.longitude}, ${coordinates.latitude}]
                        }
                      }
                    }
                  }
                }
              }
            }
        """.trimIndent()

        val mapType = mapper.typeFactory.constructMapType(Map::class.java, String::class.java, Any::class.java)
        val request = mapper.readValue<Map<String, Any>>(query, mapType)

        val response: ESResponse<Partner> = runBlocking {
            client.get("/$PARTNERS_INDEX_NAME/_search") {
                body = request
            }
        }

        val results = response.hits.hits.map { it.source }

        return results.firstOrNull()
    }

    private fun createIndex() {
        try {
            repo.getMappings() //check if exists
        } catch (e: ElasticsearchStatusException) {
            repo.createIndex(waitForActiveShards = ActiveShardCount.ONE) {
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
    }
}
