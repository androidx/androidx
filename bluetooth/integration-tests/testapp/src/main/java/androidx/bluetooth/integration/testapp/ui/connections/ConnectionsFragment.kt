package androidx.bluetooth.integration.testapp.ui.connections

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.bluetooth.BluetoothDevice
import androidx.bluetooth.BluetoothLe
import androidx.bluetooth.GattCharacteristic
import androidx.bluetooth.integration.testapp.R
import androidx.bluetooth.integration.testapp.data.connection.DeviceConnection
import androidx.bluetooth.integration.testapp.data.connection.OnCharacteristicActionClick
import androidx.bluetooth.integration.testapp.data.connection.Status
import androidx.bluetooth.integration.testapp.databinding.FragmentConnectionsBinding
import androidx.bluetooth.integration.testapp.ui.common.getColor
import androidx.bluetooth.integration.testapp.ui.common.toast
import androidx.bluetooth.integration.testapp.ui.main.MainViewModel
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.Tab
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ConnectionsFragment : Fragment() {

    internal companion object {
        private const val TAG = "ConnectionsFragment"

        internal const val MANUAL_DISCONNECT = "MANUAL_DISCONNECT"
    }

    @Inject
    lateinit var bluetoothLe: BluetoothLe

    private var deviceServicesAdapter: DeviceServicesAdapter? = null

    private val connectScope = CoroutineScope(Dispatchers.Main + Job())

    private val onTabSelectedListener = object : TabLayout.OnTabSelectedListener {
        override fun onTabSelected(tab: Tab) {
            updateDeviceUI(viewModel.deviceConnection(tab.position))
        }

        override fun onTabUnselected(tab: Tab) {
        }

        override fun onTabReselected(tab: Tab) {
        }
    }

    private val onCharacteristicActionClick = object : OnCharacteristicActionClick {
        override fun onClick(
            deviceConnection: DeviceConnection,
            characteristic: GattCharacteristic,
            action: @OnCharacteristicActionClick.Action Int
        ) {
            deviceConnection.onCharacteristicActionClick?.onClick(
                deviceConnection,
                characteristic,
                action
            )
        }
    }

    private val viewModel by viewModels<ConnectionsViewModel>()

    private val mainViewModel by activityViewModels<MainViewModel>()

    private var _binding: FragmentConnectionsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConnectionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tabLayout.addOnTabSelectedListener(onTabSelectedListener)

        deviceServicesAdapter = DeviceServicesAdapter(null, onCharacteristicActionClick)
        binding.recyclerViewDeviceServices.adapter = deviceServicesAdapter
        binding.recyclerViewDeviceServices.addItemDecoration(
            DividerItemDecoration(context, LinearLayoutManager.VERTICAL)
        )

        binding.buttonReconnect.setOnClickListener {
            connectTo(viewModel.deviceConnection(binding.tabLayout.selectedTabPosition))
        }

        binding.buttonDisconnect.setOnClickListener {
            disconnect(viewModel.deviceConnection(binding.tabLayout.selectedTabPosition))
        }

        initData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        connectScope.cancel()
        _binding = null
    }

    private fun initData() {
        viewModel.deviceConnections.map { it.bluetoothDevice }.forEach(::addNewTab)

        mainViewModel.selectedBluetoothDevice?.let { selectedBluetoothDevice ->
            onClickConnect(selectedBluetoothDevice)
            mainViewModel.selectedBluetoothDevice = null
        }
    }

    private fun onClickConnect(bluetoothDevice: BluetoothDevice) {
        Log.d(TAG, "onClickConnect() called with: bluetoothDevice = $bluetoothDevice")

        val index = viewModel.addDeviceConnectionIfNew(bluetoothDevice)

        val deviceTab = if (index == ConnectionsViewModel.NEW_DEVICE) {
            addNewTab(bluetoothDevice)
        } else {
            binding.tabLayout.getTabAt(index)
        }

        // To prevent TabSelectedListener being triggered when a tab is programmatically selected.
        binding.tabLayout.removeOnTabSelectedListener(onTabSelectedListener)
        binding.tabLayout.selectTab(deviceTab)
        binding.tabLayout.addOnTabSelectedListener(onTabSelectedListener)

        connectTo(viewModel.deviceConnection(binding.tabLayout.selectedTabPosition))
    }

    @SuppressLint("MissingPermission")
    private fun addNewTab(bluetoothDevice: BluetoothDevice): Tab {
        Log.d(TAG, "addNewTab() called with: bluetoothDevice = $bluetoothDevice")

        val deviceId = bluetoothDevice.id.toString()
        val deviceName = bluetoothDevice.name

        val newTab = binding.tabLayout.newTab()
        newTab.setCustomView(R.layout.tab_item_device)

        val customView = newTab.customView
        customView?.findViewById<TextView>(R.id.text_view_device_id)?.text = deviceId
        val textViewName = customView?.findViewById<TextView>(R.id.text_view_name)
        textViewName?.text = deviceName
        textViewName?.isVisible = deviceName.isNullOrEmpty().not()
        customView?.findViewById<Button>(R.id.image_button_remove)?.setOnClickListener {
            Log.d(TAG, "removeTab() called with: bluetoothDevice = $bluetoothDevice")

            viewModel.remove(bluetoothDevice)
            binding.tabLayout.removeTab(newTab)
        }

        binding.tabLayout.addTab(newTab)
        return newTab
    }

    @SuppressLint("MissingPermission")
    private fun connectTo(deviceConnection: DeviceConnection) {
        Log.d(TAG, "connectTo() called with: deviceConnection = $deviceConnection")

        deviceConnection.job = connectScope.launch {
            deviceConnection.status = Status.CONNECTING
            updateDeviceUI(deviceConnection)

            try {
                Log.d(
                    TAG, "bluetoothLe.connectGatt() called with: " +
                        "deviceConnection.bluetoothDevice = ${deviceConnection.bluetoothDevice}"
                )

                bluetoothLe.connectGatt(deviceConnection.bluetoothDevice) {
                    Log.d(TAG, "bluetoothLe.connectGatt result: services() = $services")

                    deviceConnection.status = Status.CONNECTED
                    deviceConnection.services = services
                    updateDeviceUI(deviceConnection)

                    deviceConnection.onCharacteristicActionClick =
                        object : OnCharacteristicActionClick {
                            override fun onClick(
                                deviceConnection: DeviceConnection,
                                characteristic: GattCharacteristic,
                                action: @OnCharacteristicActionClick.Action Int
                            ) {
                                Log.d(
                                    TAG,
                                    "onClick() called with: " +
                                        "deviceConnection = $deviceConnection, " +
                                        "characteristic = $characteristic, " +
                                        "action = $action"
                                )

                                when (action) {
                                    OnCharacteristicActionClick.READ -> readCharacteristic(
                                        this@connectGatt,
                                        deviceConnection,
                                        characteristic
                                    )

                                    OnCharacteristicActionClick.WRITE -> writeCharacteristic(
                                        this@connectGatt,
                                        characteristic
                                    )

                                    OnCharacteristicActionClick.SUBSCRIBE ->
                                        subscribeToCharacteristic(
                                            this@connectGatt,
                                            characteristic
                                        )
                                }
                            }
                        }

                    awaitCancellation()
                }
            } catch (exception: Exception) {
                if (exception is CancellationException) {
                    Log.e(TAG, "connectGatt() CancellationException", exception)
                } else {
                    Log.e(TAG, "connectGatt() exception", exception)
                }

                deviceConnection.status = Status.DISCONNECTED
                updateDeviceUI(deviceConnection)
            }
        }
    }

    private fun readCharacteristic(
        gattClientScope: BluetoothLe.GattClientScope,
        deviceConnection: DeviceConnection,
        characteristic: GattCharacteristic
    ) {
        connectScope.launch {
            Log.d(TAG, "readCharacteristic() called with: characteristic = $characteristic")

            val result = gattClientScope.readCharacteristic(characteristic)
            Log.d(TAG, "readCharacteristic() result: result = $result")

            deviceConnection.storeValueFor(characteristic, result.getOrNull())
            updateDeviceUI(deviceConnection)
        }
    }

    private fun writeCharacteristic(
        gattClientScope: BluetoothLe.GattClientScope,
        characteristic: GattCharacteristic
    ) {
        val view = layoutInflater.inflate(R.layout.dialog_write_characteristic, null)
        val editTextValue = view.findViewById<EditText>(R.id.edit_text_value)

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.write))
            .setView(view).setPositiveButton(getString(R.string.write)) { _, _ ->
                val editTextValueString = editTextValue.text.toString()
                val value = editTextValueString.toByteArray()

                connectScope.launch {
                    Log.d(
                        TAG,
                        "writeCharacteristic() called with: " +
                            "characteristic = $characteristic, " +
                            "value = ${value.decodeToString()}"
                    )

                    val result = gattClientScope.writeCharacteristic(characteristic, value)
                    Log.d(TAG, "writeCharacteristic() result: result = $result")

                    toast("Called write with: $editTextValueString, result = $result").show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .create()
            .show()
    }

    private fun subscribeToCharacteristic(
        gattClientScope: BluetoothLe.GattClientScope,
        characteristic: GattCharacteristic
    ) {
        connectScope.launch {
            gattClientScope.subscribeToCharacteristic(characteristic)
                .collect {
                    Log.d(
                        TAG,
                        "subscribeToCharacteristic() collected: " +
                            "characteristic = $characteristic, " +
                            "value.decodeToString() = ${it.decodeToString()}"
                    )
                }

            Log.d(TAG, "subscribeToCharacteristic completed")
        }
    }

    private fun disconnect(deviceConnection: DeviceConnection) {
        Log.d(TAG, "disconnect() called with: deviceConnection = $deviceConnection")

        deviceConnection.job?.cancel(MANUAL_DISCONNECT)
        deviceConnection.job = null
        deviceConnection.status = Status.DISCONNECTED
        updateDeviceUI(deviceConnection)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateDeviceUI(deviceConnection: DeviceConnection) {
        if (_binding == null) return

        binding.progressIndicatorDeviceConnection.isVisible = false
        binding.buttonReconnect.isVisible = false
        binding.buttonDisconnect.isVisible = false

        when (deviceConnection.status) {
            Status.DISCONNECTED -> {
                binding.textViewDeviceConnectionStatus.text = getString(R.string.disconnected)
                binding.textViewDeviceConnectionStatus.setTextColor(getColor(R.color.green_500))
                binding.buttonReconnect.isVisible = true
            }

            Status.CONNECTING -> {
                binding.progressIndicatorDeviceConnection.isVisible = true
                binding.textViewDeviceConnectionStatus.text = getString(R.string.connecting)
                binding.textViewDeviceConnectionStatus.setTextColor(getColor(R.color.indigo_500))
            }

            Status.CONNECTED -> {
                binding.textViewDeviceConnectionStatus.text = getString(R.string.connected)
                binding.textViewDeviceConnectionStatus.setTextColor(getColor(R.color.indigo_500))
                binding.buttonDisconnect.isVisible = true
            }
        }
        deviceServicesAdapter?.deviceConnection = deviceConnection
        deviceServicesAdapter?.notifyDataSetChanged()
    }
}
