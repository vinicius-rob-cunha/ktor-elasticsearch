package br.com.vroc.resources.elastic

import br.com.vroc.domain.model.Partner
import br.com.vroc.domain.repositories.PartnerRepository
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlinx.coroutines.runBlocking
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.client.configure
import org.elasticsearch.client.indexRepository
import org.geojson.LngLatAlt

const val INDEX_NAME = "partners"

class PartnerRepositoryImpl(
    esClient: RestHighLevelClient,
    private val client: HttpClient,
    private val mapper: ObjectMapper
) : PartnerRepository {

    private val repo = esClient.indexRepository<Partner>(INDEX_NAME)

    init {
//        createIndex()
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

        val response: ESReponse<Partner> = runBlocking {
            client.get("/$INDEX_NAME/_search") {
                body = request
            }
        }

        val results = response.hits.hits.map { it.source }

        return results.firstOrNull()
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
}

data class ESReponse<T> (
    val hits : HitsResponse<T>
)

data class HitsResponse<T> (
    val total: HitsTotalReponse,
    val hits: List<Hit<T>>
)

data class HitsTotalReponse (
    val value: Int,
    val relation: String
)

data class Hit<T> (
    @JsonProperty("_source")
    val source: T
)