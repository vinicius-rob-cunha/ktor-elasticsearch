package br.com.vroc.application

import br.com.vroc.application.config.ObjectMapperBuilder
import br.com.vroc.application.exceptions.ApiException
import br.com.vroc.application.exceptions.ResourceNotFoundException
import br.com.vroc.application.exceptions.customMessage
import br.com.vroc.application.modules.ConfigModule
import br.com.vroc.application.modules.ESModule
import br.com.vroc.application.modules.PartnerModule
import br.com.vroc.application.util.PartnerDataSample
import br.com.vroc.application.web.response.HttpErrorResponse
import br.com.vroc.domain.model.Partner
import br.com.vroc.domain.services.PartnerService
import com.fasterxml.jackson.core.JsonProcessingException
import io.ktor.application.Application
import io.ktor.application.ApplicationStarted
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.features.DataConversion
import io.ktor.features.StatusPages
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.JacksonConverter
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import org.geojson.LngLatAlt
import org.koin.ktor.ext.Koin
import org.koin.ktor.ext.get
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
            val response = HttpErrorResponse("internal_server_error", e.localizedMessage)
            call.respond(HttpStatusCode.InternalServerError, response)
        }
        exception<ApiException> { e ->
            val response = HttpErrorResponse(e.errorCode, e.message)
            call.respond(HttpStatusCode.fromValue(e.statusCode), response)
        }
        exception<JsonProcessingException> { e ->
            val response = HttpErrorResponse("bad_request", e.customMessage())
            call.respond(HttpStatusCode.BadRequest, response)
        }
    }

    install(Koin) {
        modules(
            PartnerModule.modules(),
            ConfigModule.modules(),
            ESModule.modules()
        )
    }

    environment.monitor.subscribe(ApplicationStarted) {
        PartnerDataSample(get(), get()).load()
    }

    val service: PartnerService by inject()

    routing {
        post("/"){
            val partner = call.receive<Partner>()

            service.create(partner)

            call.respond(HttpStatusCode.Created, partner)
        }

        get("/{id}") {
            val id = call.parameters["id"]!!

            val partner = service.getById(id) ?: throw ResourceNotFoundException("Partner[$id] Not found")

            call.respond(partner)
        }

        get("/nearest") {
            val lon = call.parameters["lon"]!!.toDouble()
            val lat = call.parameters["lat"]!!.toDouble()

            val partner = service.findNearestWithinCoverageArea(LngLatAlt(lon, lat)) ?:
                throw ResourceNotFoundException("No Partner found for this address [$lon,$lat]")

            call.respond(partner)
        }
    }

}
