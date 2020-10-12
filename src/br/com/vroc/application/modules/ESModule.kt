package br.com.vroc.application.modules

import br.com.vroc.application.config.ESConfig
import br.com.vroc.application.config.ObjectMapperBuilder
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.defaultRequest
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.header
import io.ktor.client.request.host
import io.ktor.client.request.port
import org.elasticsearch.client.create
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.ext.getOrCreateScope

object ESModule {
    fun modules(): Module = module {
        single {
            val config = get<ESConfig>()
            create(host = config.host, port = config.port)
        }
        single {
            val config = get<ESConfig>()
            HttpClient(Apache) {
                install(JsonFeature) {
                    serializer = JacksonSerializer(get())
                }
                defaultRequest {
                    host = config.host
                    port = config.port
                    header("Content-Type", "application/json")
                }
            }
        }
    }
}