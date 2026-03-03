package com.v2soft.uarttest.repo

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.v2soft.uarttest.domain.ConstructionError
import com.v2soft.uarttest.domain.Result
import java.util.concurrent.atomic.AtomicInteger

class UartRepo(
    private val usbManager: UsbManager
) {
    data class AddControllerResult(
        val id: Int,
        val controller: UartController
    )
    private val portIdCounter = AtomicInteger(1)

    private val controllers = mutableMapOf<Int, UartController>()

    fun addController(
        device: UsbDevice,
        configuration: UartController.Configuration
    ): Result<AddControllerResult> {
        val id = portIdCounter.incrementAndGet()
        val result = UartController.construct(
            device = device,
            configuration = configuration,
            manager = usbManager,
        )
        if (result is Result.Error) {
            return Result.Error(result.error)
        } else if (result is Result.Value) {
            controllers[id] = result.value
            return Result.Value(AddControllerResult(
                id = id,
                controller = result.value
            ))
        }
        return Result.Error(ConstructionError.NoDriver(device))
    }

    fun getController(id: Int): Result<UartController> {
        val controller = controllers[id]
        return if (controller != null)
            Result.Value(controller)
        else
            Result.Error(ConstructionError.NoSuchController)
    }

    fun removeController(id: Int) {
        val controller = getController(id)
        if (controller is Result.Value) {
            controller.value.close()
        }
        controllers.remove(key = id)
    }
}