package com.v2soft.uarttest.repo

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.util.Log
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import com.v2soft.uarttest.domain.ConstructionError
import com.v2soft.uarttest.domain.Result
import java.io.Closeable

class UartController(
    private val device: UsbDevice,
    private val driver: UsbSerialDriver,
    private val port: UsbSerialPort
) : Closeable, SerialInputOutputManager.Listener {
    private var serialInputOutputManager: SerialInputOutputManager? = null

    data class Configuration(
        val baudRate: Int = 420000,
        val dataBits: Int = 8,
        val stopBits: Int = UsbSerialPort.STOPBITS_1,
        val parity: Int = UsbSerialPort.PARITY_NONE
    )

    fun open() {
        serialInputOutputManager = SerialInputOutputManager(port, this)
        serialInputOutputManager?.start()
    }

    override fun close() {
        serialInputOutputManager?.stop()
        port.close()
    }

    override fun onNewData(data: ByteArray?) {
        Log.d("UartController", "Got data $data")
    }

    override fun onRunError(e: java.lang.Exception?) {
        Log.e("UartController", e?.toString() ?: "null")
    }

    companion object {
        fun construct(
            device: UsbDevice,
            configuration: Configuration,
            manager: UsbManager
        ): Result<UartController> {
            val driver = UsbSerialProber
                .getDefaultProber()
                .probeDevice(device) ?: return Result.Error(ConstructionError.NoDriver(device))

            val connection = manager.openDevice(driver.device)
                ?: return Result.Error(ConstructionError.CantOpen(device))

            val port = driver.ports.firstOrNull() ?: return Result.Error(
                ConstructionError.NoDriver(device)
            )
            try {
                port.open(connection)
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