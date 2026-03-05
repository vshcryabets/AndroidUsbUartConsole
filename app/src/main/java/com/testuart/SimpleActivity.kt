package com.testuart

import android.app.ComponentCaller
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import com.v2soft.uarttest.ui.theme.UsbUartTestTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SimpleActivityViewModelFactory(
    private val usbManager: UsbManager
): ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        SimpleActivityViewModel(
            usbManager = usbManager
        ) as T
}

data class SimpleActivityState(
    val noPermissionDevice: UsbDevice? = null,
    val deviceError: String? = null,
    val activePortId: Int = -1,
    val linesBuffer: List<String> = emptyList(),
    val connectedDevice: UsbDevice? = null
)

class SimpleActivityViewModel(
    private val usbManager: UsbManager
): ViewModel(), SerialInputOutputManager.Listener {
    private val _state = MutableStateFlow(SimpleActivityState())
    val state: StateFlow<SimpleActivityState> = _state
    private var serialInputOutputManager: SerialInputOutputManager? = null
    private var port : UsbSerialPort? = null
    private var usbConnection: UsbDeviceConnection? = null

    fun onUsbDeviceAttached(device: UsbDevice) {
        if (_state.value.connectedDevice != null) {
            Log.d("ASD", "Device already connected, ignoring new device: $device")
            return
        }
        Log.d("ASD", "New device attached: $device")
        // check permission
        if (!usbManager.hasPermission(device)) {
            _state.update { it.copy(noPermissionDevice = device) }
            return
        }
        val driver = UsbSerialProber
            .getDefaultProber()
            .probeDevice(device)
        if (driver == null) {
            _state.update { it.copy(deviceError = "No driver for $device") }
            return
        }
        val connection = usbManager.openDevice(driver.device)
        if (connection == null) {
            _state.update { it.copy(deviceError = "Can't open $device") }
            return
        }
        usbConnection = connection

        val port = driver.ports.firstOrNull()
        if (port == null) {
            _state.update { it.copy(deviceError = "No port for $device") }
            return
        }
        this.port = port
        try {
            port.open(connection)
            port.setParameters(
                115200,
                UsbSerialPort.DATABITS_8,
                UsbSerialPort.STOPBITS_1,
                UsbSerialPort.PARITY_NONE
            )
        } catch (e: Exception) {
            _state.update { it.copy(deviceError = "Can't open port for $device") }
            return
        }
        serialInputOutputManager = SerialInputOutputManager(port, this)
        serialInputOutputManager?.start()
        _state.update {
            it.copy(connectedDevice = device)
        }
    }

    fun onUsbDevicePermissionDeclined(device: UsbDevice?) {
        _state.update { it.copy(deviceError = "No permission") }
    }

    fun onPermissionRequested() = _state.update { it.copy(noPermissionDevice = null) }

    override fun onNewData(p0: ByteArray?) {
        Log.d("ASD", "Got new data: ${p0?.size ?: 0} byte(s)")
    }

    override fun onRunError(p0: java.lang.Exception?) {
        Log.e("ASD", "Run error", p0)
        closeConnection()
    }

    fun closeConnection() {
        serialInputOutputManager?.stop()
        port?.close()
        usbConnection?.close()
        _state.update {
            it.copy(connectedDevice = null)
        }
        serialInputOutputManager = null
        port = null
        usbConnection = null
    }

    fun onStop() {
        closeConnection()
    }
}

class SimpleActivity : ComponentActivity() {
    lateinit var viewModel: SimpleActivityViewModel

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

    private val deviceAttachmentReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d("ASD", "onReceive called with intent: ${intent.action}")
            checkIntent(intent)
        }
    }

    fun checkIntent(intent: Intent) {
        if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(intent.action)) {
            Log.d("ASD", "Activity started with USB_DEVICE_ATTACHED intent")
            val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
            device?.let {
                viewModel.onUsbDeviceAttached(it)
            }
        } else {
            Log.d("ASD", "Activity started with intent: ${intent.action}")
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel = ViewModelProvider(
            this, SimpleActivityViewModelFactory(
                usbManager = getSystemService(USB_SERVICE) as UsbManager
            )
        ).get(SimpleActivityViewModel::class.java)
        checkIntent(intent)
        setContent {
            val state = viewModel.state.collectAsState()
            UsbUartTestTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(title = { Text(state.value.connectedDevice?.deviceName ?: "No USB UART")})
                    },
                ) { innerPadding ->
                    Text(
                        text = "Test simple activity",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent, caller: ComponentCaller) {
        super.onNewIntent(intent, caller)
        Log.d("ASD", "onNewIntent called with intent: ${intent.action}")
        checkIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        Log.d("ASD", "onResume called")

        // Register broadcast receiver for USB device attachment
        val deviceAttachmentFilter = IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        ContextCompat.registerReceiver(
            this,
            deviceAttachmentReceiver,
            deviceAttachmentFilter,
            ContextCompat.RECEIVER_EXPORTED
        )

        lifecycleScope.launch {
            viewModel.state.collect {
                if (it.noPermissionDevice != null) {
                    val usbManager = this@SimpleActivity.getSystemService(USB_SERVICE) as UsbManager
                    // request permission
                    val permissionIntent = PendingIntent.getBroadcast(
                        this@SimpleActivity,
                        0,
                        Intent(ACTION_USB_PERMISSION),
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                    usbManager.requestPermission(it.noPermissionDevice, permissionIntent)
                    viewModel.onPermissionRequested()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Unregister the device attachment receiver when activity is not visible
        unregisterReceiver(deviceAttachmentReceiver)
    }

    override fun onStop() {
        viewModel.onStop()
        super.onStop()
    }

    companion object {
        const val ACTION_USB_PERMISSION: String = "com.testuart.USB_PERMISSION"
    }
}
