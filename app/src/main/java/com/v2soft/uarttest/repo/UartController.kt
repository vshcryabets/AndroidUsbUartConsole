package com.v2soft.uarttest.repo

import android.hardware.usb.UsbDevice
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.v2soft.uarttest.domain.ConstructionError
import com.v2soft.uarttest.domain.Result
import java.io.Closeable

class UartController(
    private val device: UsbDevice,
    private val driver: UsbSerialDriver,
    private val port: UsbSerialPort
): Closeable {

    override fun close() {
        port.close()
    }

    data class Configuration(
        val baudRate: Int = 420000,
        val dataBits: Int = 8,
        val stopBits: Int = UsbSerialPort.STOPBITS_1,
        val parity: Int = UsbSerialPort.PARITY_NONE
    )


    companion object {
        fun construct(device: UsbDevice, configuration: Configuration): Result<UartController> {
            val driver: UsbSerialDriver =
                UsbSerialProber.getDefaultProber().probeDevice(device) ?:
                return Result.Error(
                    ConstructionError.NoDriver(device)
                )
            val port = driver.ports.firstOrNull() ?: return Result.Error(
                ConstructionError.NoDriver(device)
            )
            try {
                port.open(null)
                port.setParameters(
                    configuration.baudRate,
                    configuration.dataBits,
                    configuration.stopBits,
                    configuration.parity
                )
            } catch (e: Exception) {
                return Result.Error(
                    ConstructionError.CantOpen(device)
                )
            }
            return Result.Value(
                UartController(
                    device = device,
                    driver = driver,
                    port = port
                )
            )
        }
    }
}