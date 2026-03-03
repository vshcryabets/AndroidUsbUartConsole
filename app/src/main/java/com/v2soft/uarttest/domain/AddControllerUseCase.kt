package com.v2soft.uarttest.domain

import android.hardware.usb.UsbDevice
import com.v2soft.uarttest.repo.UartController
import com.v2soft.uarttest.repo.UartRepo

class AddControllerUseCase(
    private val repo: UartRepo
){
    operator fun invoke(
        device: UsbDevice,
        configuration: UartController.Configuration
    ) = repo.addController(device, configuration)
}