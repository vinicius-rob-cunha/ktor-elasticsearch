package br.com.vroc.application.exceptions

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import org.apache.http.HttpStatus.SC_NOT_FOUND

open class ApiException(val errorCode: String, val statusCode: Int, msg: String?) : Exception(msg)

class ResourceNotFoundException(msg: String) :
    ApiException("resource_not_found", SC_NOT_FOUND, msg)

fun JsonProcessingException.customMessage() : String {
    return when (this) {
        is InvalidFormatException -> "Could not parse JSON: attribute " +
                "'${this.path.joinToString(separator = ".") { it.fieldName.orEmpty() }}' has unknown value '${this.value}'"
        is MissingKotlinParameterException -> "Could not parse JSON: attribute " +
                "'${this.path.joinToString(separator = ".") { it.fieldName.orEmpty() }}' is required"
        is UnrecognizedPropertyException -> "Could not parse JSON: Attribute " +
                "'${this.path.joinToString(separator = ".") { it.fieldName.orEmpty() }}' is invalid"
        is MismatchedInputException -> "Could not parse JSON: Attribute " +
                "'${this.path.joinToString(separator = ".") { it.fieldName.orEmpty() }}' has an invalid type"
        else -> this.originalMessage
    }
}