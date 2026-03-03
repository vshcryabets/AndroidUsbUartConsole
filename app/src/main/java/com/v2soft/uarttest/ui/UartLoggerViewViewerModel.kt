package com.v2soft.uarttest.ui

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log
import androidx.lifecycle.ViewModel
import com.v2soft.uarttest.domain.AddControllerUseCase
import com.v2soft.uarttest.domain.ConstructionError
import com.v2soft.uarttest.domain.Result
import com.v2soft.uarttest.repo.UartController
import com.v2soft.uarttest.repo.UartRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class State(
    val noPermissionDevice: UsbDevice? = null,
    val noDeviceDriver: String = "",
    val activePortId: Int = -1,
    val currentConfiguration: UartController.Configuration = UartController.Configuration(
        baudRate = 115200,
        dataBits = 8,
        stopBits = 1,
        parity = 0
    ),
)

class UartLoggerViewViewerModel(
    private val addControllerUseCase: AddControllerUseCase
) : ViewModel() {
    private val _state = MutableStateFlow<State>(State())
    val state: StateFlow<State> = _state

    fun onUsbDeviceAttached(device: UsbDevice) {
        Log.d("UartLoggerViewViewerModel", "USB device attached: ${device}")

        val result = addControllerUseCase(device, state.value.currentConfiguration)
        if (result is Result.Error) {
            val error = result.error
            when (error) {
                is ConstructionError.NoPermission ->
                    _state.update { it.copy(noPermissionDevice = error.device) }
                is ConstructionError.NoDriver ->
                    _state.update { it.copy(noDeviceDriver = "Can't find driver for ${error.device}") }
                is ConstructionError.CantOpen ->
                    _state.update { it.copy(noDeviceDriver = "Can't open ${error.device}") }
                else ->
                    _state.update { it.copy(noDeviceDriver = "Can't open device") }
            }
        } else if (result is Result.Value) {
            val controller = result.value
            _state.update { it.copy(activePortId = controller.id) }
            controller.controller.open()
        }
    }

    fun onUsbDevicePermissionDeclined(device: UsbDevice?) {
        Log.e("UartLoggerViewViewerModel", "No permission for USB device: ${device}")
    }

    fun onPermissionRequested() = _state.update { it.copy(noPermissionDevice = null) }
}