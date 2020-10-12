package br.com.vroc.application.modules

import br.com.vroc.application.config.ESConfig
import br.com.vroc.application.config.ObjectMapperBuilder
import org.koin.core.module.Module
import org.koin.dsl.module

object ConfigModule {
    fun modules(): Module = module {
        single { ESConfig() }
        single { ObjectMapperBuilder.getMapper() }
    }
}