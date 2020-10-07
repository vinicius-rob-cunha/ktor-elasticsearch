package br.com.vroc

import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.features.DataConversion
import io.ktor.features.StatusPages
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import org.apache.http.HttpHost
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.client.create
import org.geojson.MultiPolygon
import org.geojson.Point

fun main(args: Array<String>) {
    val restClientBuilder = RestClient.builder(
        HttpHost("localhost", 9200, "http")
    )
    val restHighLevelClient = RestHighLevelClient(restClientBuilder)

    val esClient = create(host = "localhost", port = 9999)

    io.ktor.server.netty.EngineMain.main(args)
}

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    install(DataConversion)
    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
        }
    }

    install(StatusPages) {
        exception<Throwable> { e ->
            call.respondText(e.localizedMessage, ContentType.Text.Plain, HttpStatusCode.InternalServerError)
        }
    }

    routing {
        get("/") {
            call.respond(Response (status = "OK"))
        }

        post("/"){
            val request = call.receive<Partner>()
            call.respond(request)
        }
    }
}

data class Partner (
    val id : Int,
    val tradingName: String,
    val ownerName: String,
    val document: String,
    val coverageArea: MultiPolygon,
    val address: Point
)

data class Response (val status: String)

/*
curl -v POST http://localhost:8080 -H 'Content-Type: application/json' \
-d '{
"id": 1,
"tradingName": "Adega da Cerveja - Pinheiros",
"ownerName": "Zé da Silva",
"document": "1432132123891/0001",
"coverageArea": {
    "type": "MultiPolygon",
    "coordinates": [
        [
            [[30, 20], [45, 40], [10, 40], [30, 20]]
        ],
        [
            [[15, 5], [40, 10], [10, 20], [5, 10], [15, 5]]
        ]
    ]
},
"address": {
    "type": "Point",
    "coordinates": [-46.57421, -21.785741]
}
}'
*/

