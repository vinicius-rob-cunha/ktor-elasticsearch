package br.com.vroc

import br.com.vroc.application.config.ObjectMapperBuilder
import br.com.vroc.application.main
import br.com.vroc.domain.model.Partner
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpMethod.Companion.Post
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import kotlin.random.Random
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS

@TestInstance(PER_CLASS)
class ApplicationTest {

    private val mapper = ObjectMapperBuilder.getMapper()
    lateinit var pdvs: List<Partner>

    @BeforeAll
    fun setUp() {
        val pdvsJson = javaClass.getResource("/pdvs.json").readBytes()

        val mapper = jacksonObjectMapper()
        val pdvsTree = mapper.readTree(pdvsJson)["pdvs"].toString()
        val listType = mapper.typeFactory.constructCollectionType(MutableList::class.java, Partner::class.java)
        pdvs = mapper.readValue(pdvsTree, listType)
    }

    @Test
    fun `should return CREATED_201 when create partner `() { withBaseTestApplication {
        val newPartner = PartnerSample.create()
        handleRequest(Post, "/") {
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(mapper.writeValueAsBytes(newPartner))
        }.apply {
            assertThat(HttpStatusCode.Created).isEqualTo(response.status())

            val partner = mapper.readValue(response.content!!, Partner::class.java)
            assertThat(partner.id).isEqualTo(newPartner.id)
        }
    } }

    @Test
    fun `should return OK_200 when find a partner `() { withBaseTestApplication {
        val partnerId = Random.nextInt(pdvs.size) //baseado na base previamente carregada
        handleRequest(Get, "/$partnerId").apply {
            assertThat(HttpStatusCode.OK).isEqualTo(response.status())
            val partner = mapper.readValue(response.content!!, Partner::class.java)
            assertThat(partner.id).isEqualTo(partnerId.toString())
        }
    } }

    @Test
    fun `should return NOT_FOUND_404 when don't find a partner `() { withBaseTestApplication {
        handleRequest(Get, "/UNKNOWN").apply {
            assertThat(HttpStatusCode.NotFound).isEqualTo(response.status())
        }
    } }

    @Test
    fun `should return OK_200 when find nearest partner`() { withBaseTestApplication {
        val partner = pdvs[Random.nextInt(pdvs.size)]
        val coordinates = partner.address.coordinates

        handleRequest(Get, "/nearest?lat=${coordinates.latitude}&lon=${coordinates.longitude}").apply {
            assertThat(HttpStatusCode.OK).isEqualTo(response.status())
            val resPartner = mapper.readValue(response.content!!, Partner::class.java)
            assertThat(resPartner.id).isEqualTo(partner.id)
        }
    } }

    @Test
    fun `should return NOT_FOUND_404 when find nearest partner`() { withBaseTestApplication {
        handleRequest(Get, "/nearest?lat=0.0&lon=0.0").apply {
            assertThat(HttpStatusCode.NotFound).isEqualTo(response.status())
        }
    } }
}

fun <R> withBaseTestApplication(test: TestApplicationEngine.() -> R) {
    withTestApplication({ main(testing = true) }) {
        test()
    }
}
