package com.v2soft.uarttest

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.v2soft.uarttest.domain.AddControllerUseCase
import com.v2soft.uarttest.repo.UartRepo
import com.v2soft.uarttest.ui.Greeting
import com.v2soft.uarttest.ui.UartLoggerViewViewerModel
import com.v2soft.uarttest.ui.UartLoggerViewViewerModelFactory
import com.v2soft.uarttest.ui.theme.UsbUartTestTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    lateinit var viewModel: UartLoggerViewViewerModel
    private lateinit var uartRepo: UartRepo

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_USB_PERMISSION) {
                val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                if (device != null && granted) {
                    viewModel.onUsbDeviceAttached(device)
                } else {
                    viewModel.onUsbDevicePermissionDeclined(device)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        uartRepo = UartRepo(this.getSystemService(USB_SERVICE) as UsbManager)
        val addControllerUseCase = AddControllerUseCase(uartRepo)

        viewModel = ViewModelProvider(
            this, UartLoggerViewViewerModelFactory(
                addControllerUseCase =  addControllerUseCase
            )
        ).get(UartLoggerViewViewerModel::class.java)


        if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(intent.action)) {
            val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
            device?.let {
                viewModel.onUsbDeviceAttached(it)
            }
        }
        setContent {
            UsbUartTestTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            viewModel.state.collect {
                if (it.noPermissionDevice != null) {
                    val usbManager = this@MainActivity.getSystemService(USB_SERVICE) as UsbManager
                    // request permission
                    val permissionIntent = PendingIntent.getBroadcast(
                        this@MainActivity,
                        0,
                        Intent(ACTION_USB_PERMISSION),
                        0
                    )
                    usbManager.requestPermission(it.noPermissionDevice, permissionIntent)
                    viewModel.onPermissionRequested()
                }
            }
        }
    }

    companion object {
        const val CAMERA_PERMISSION_REQUEST_CODE = 1
        const val ACTION_USB_PERMISSION: String = "com.v2soft.uarttest.USB_PERMISSION"
    }
}
