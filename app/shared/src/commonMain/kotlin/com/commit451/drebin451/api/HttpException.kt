package com.commit451.drebin451.api

import io.ktor.http.HttpStatusCode

/** Thrown by [Api] when the backend responds with a non-2xx status. */
class HttpException(
    val statusCode: HttpStatusCode,
    message: String = "HTTP error ${statusCode.value}",
) : Exception(message)
