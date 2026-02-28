package com.v2soft.uarttest.ui

import android.hardware.usb.UsbManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.v2soft.uarttest.domain.UartRepo

class UartLoggerViewViewerModelFactory(
    private val usbManager: UsbManager,
    private val uartRepo: UartRepo,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        UartLoggerViewViewerModel(
            usbManager = usbManager,
            uartRepo = uartRepo
        ) as T
}
