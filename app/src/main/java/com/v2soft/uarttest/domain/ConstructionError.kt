package com.v2soft.uarttest.domain

import android.hardware.usb.UsbDevice

sealed class ConstructionError {
    class NoPermission(val device: UsbDevice) : ConstructionError()
    class NoDriver(val device: UsbDevice) : ConstructionError()
    class CantOpen(val device: UsbDevice) : ConstructionError()
    object NoSuchController: ConstructionError()
}