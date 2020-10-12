package br.com.vroc.application.modules

import br.com.vroc.domain.services.PartnerService
import br.com.vroc.resources.elastic.PartnerRepositoryImpl
import org.koin.core.module.Module
import org.koin.dsl.module

object PartnerModule {
    fun modules(): Module = module {
        single { PartnerRepositoryImpl(get(), get(), get()) }
        single { PartnerService(get()) }
    }
}