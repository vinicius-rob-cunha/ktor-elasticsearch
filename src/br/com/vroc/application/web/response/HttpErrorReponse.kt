package br.com.vroc.application.web.response

data class HttpErrorResponse(
    val type: String,
    val message: String?
)