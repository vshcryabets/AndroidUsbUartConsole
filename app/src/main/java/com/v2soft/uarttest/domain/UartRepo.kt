package com.v2soft.uarttest.domain

import android.hardware.usb.UsbDevice
import java.util.concurrent.atomic.AtomicInteger

class UartRepo {
    private val portIdCounter = AtomicInteger(1)

    private val controllers = mutableMapOf<Int, UartController>()

    fun addController(device: UsbDevice, configuration: UartController.Configuration): Result<Int> {
        val id = portIdCounter.incrementAndGet()
        val result = UartController.construct(
            device = device,
            configuration = configuration)
        if (result is Result.Error) {
            return Result.Error(result.error)
        } else if (result is Result.Value) {
            controllers[id] = result.value
            return Result.Value(id)
        }
        return Result.Error(UartController.ConstructionError.NoDriver(device))
    }

    fun getController(id: Int): UartController? {
        return controllers[id]
    }

    fun removeController(id: Int) {
        // TODO call close
        controllers.remove(key = id)
    }
}