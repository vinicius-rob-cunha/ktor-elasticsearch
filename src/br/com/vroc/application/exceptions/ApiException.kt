package br.com.vroc.application.exceptions

import org.apache.http.HttpStatus.SC_NOT_FOUND

open class ApiException(val errorCode: String, val statusCode: Int, msg: String?) : Exception(msg)

class ResourceNotFoundException(msg: String) :
    ApiException("resource_not_found", SC_NOT_FOUND, msg)

