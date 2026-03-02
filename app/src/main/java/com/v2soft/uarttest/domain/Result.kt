package com.v2soft.uarttest.domain

sealed class Result<T> {
    class Value<T>(val value: T) : Result<T>()
    class Error<T>(val error: ConstructionError) : Result<T>()
}