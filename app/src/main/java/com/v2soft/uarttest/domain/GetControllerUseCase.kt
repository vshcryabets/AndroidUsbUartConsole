package com.v2soft.uarttest.domain

import com.v2soft.uarttest.repo.UartRepo

class GetControllerUseCase(
    private val repo: UartRepo
){
    operator fun invoke(id: Int) = repo.getController(id)
}