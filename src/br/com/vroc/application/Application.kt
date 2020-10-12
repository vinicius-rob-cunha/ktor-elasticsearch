package br.com.vroc.application

import br.com.vroc.application.config.ObjectMapperBuilder
import br.com.vroc.application.exceptions.ApiException
import br.com.vroc.application.exceptions.ResourceNotFoundException
import br.com.vroc.application.modules.ConfigModule
import br.com.vroc.application.modules.ESModule
import br.com.vroc.application.modules.PartnerModule
import br.com.vroc.application.web.response.HttpErrorResponse
import br.com.vroc.domain.model.Partner
import br.com.vroc.domain.services.PartnerService
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.features.DataConversion
import io.ktor.features.StatusPages
import io.ktor.http.ContentType
import io.ktor.http.ContentType.Text
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.JacksonConverter
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import org.koin.ktor.ext.Koin
import org.koin.ktor.ext.inject

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.main(testing: Boolean = false) {
    install(DataConversion)
    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(ObjectMapperBuilder.getMapper()))
    }

    install(StatusPages) {
        exception<Throwable> { e ->
            call.respondText(e.localizedMessage, Text.Plain, HttpStatusCode.InternalServerError)
        }
        exception<ApiException> { e ->
            val response = HttpErrorResponse(e.errorCode, e.message)
            call.respond(HttpStatusCode.fromValue(e.statusCode), response)
        }
    }

    install(Koin) {
        modules(
            PartnerModule.modules(),
            ConfigModule.modules(),
            ESModule.modules()
        )
    }

    val service: PartnerService by inject()

    routing {
        get("/{id}") {
            val id = call.parameters["id"]!!

            val partner = service.getById(id) ?: throw ResourceNotFoundException("Partner[$id] Not found")

            call.respond(partner)
        }

        post("/"){
            val partner = call.receive<Partner>()

            service.create(partner)

            call.respond(HttpStatusCode.Created, partner)
        }
    }
}

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

