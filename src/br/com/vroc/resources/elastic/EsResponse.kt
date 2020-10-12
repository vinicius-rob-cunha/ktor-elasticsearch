package br.com.vroc.resources.elastic

import com.fasterxml.jackson.annotation.JsonProperty

data class ESResponse<T> (
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