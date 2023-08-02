/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.bluetooth.core

import android.bluetooth.BluetoothAdapter as FwkBluetoothAdapter
import android.bluetooth.BluetoothProfile as FwkBluetoothProfile
import android.bluetooth.le.BluetoothLeAdvertiser as FwkBluetoothLeAdvertiser
import android.bluetooth.le.BluetoothLeScanner as FwkBluetoothLeScanner
import android.bluetooth.BluetoothDevice as FwkBluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import java.time.Duration
import java.util.UUID

/**
 * Represents the local device Bluetooth adapter. The {@link BluetoothAdapter}
 * lets you perform fundamental Bluetooth tasks, such as initiate
 * device discovery, query a list of bonded (paired) devices,
 * instantiate a {@link BluetoothDevice} using a known MAC address, and create
 * a {@link BluetoothServerSocket} to listen for connection requests from other
 * devices, and start a scan for Bluetooth LE devices.
 *
 * <p>To get a {@link BluetoothAdapter} representing the local Bluetooth
 * adapter, call the {@link BluetoothManager#getAdapter} function on {@link BluetoothManager}.
 * </p><p>
 * * Fundamentally, this is your starting point for all
 * Bluetooth actions. * </p>
 * <p>This class is thread safe.</p>
 *
 * @hide
 */
class BluetoothAdapter private constructor(private val fwkAdapter: FwkBluetoothAdapter) {
    companion object {
        /**
         * Intent used to broadcast the change in connection state of the local
         * Bluetooth adapter to a profile of the remote device. When the adapter is
         * not connected to any profiles of any remote devices and it attempts a
         * connection to a profile this intent will be sent. Once connected, this intent
         * will not be sent for any more connection attempts to any profiles of any
         * remote device. When the adapter disconnects from the last profile its
         * connected to of any remote device, this intent will be sent.
         *
         * <p> This intent is useful for applications that are only concerned about
         * whether the local adapter is connected to any profile of any device and
         * are not really concerned about which profile. For example, an application
         * which displays an icon to display whether Bluetooth is connected or not
         * can use this intent.
         *
         * <p>This intent will have 3 extras:
         * {@link #EXTRA_CONNECTION_STATE} - The current connection state.
         * {@link #EXTRA_PREVIOUS_CONNECTION_STATE}- The previous connection state.
         * {@link BluetoothDevice#EXTRA_DEVICE} - The remote device.
         *
         * {@link #EXTRA_CONNECTION_STATE} or {@link #EXTRA_PREVIOUS_CONNECTION_STATE}
         * can be any of {@link #STATE_DISCONNECTED}, {@link #STATE_CONNECTING},
         * {@link #STATE_CONNECTED}, {@link #STATE_DISCONNECTING}.
         */
        @RequiresPermission(
            anyOf = ["android.permission.BLUETOOTH_CONNECT",
                "android.permission.BLUETOOTH"]
        )
        const val ACTION_CONNECTION_STATE_CHANGED =
            FwkBluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED

        /**
         * Broadcast Action: The local Bluetooth adapter has finished the device
         * discovery process.
         */
        @RequiresPermission(
            anyOf = ["android.permission.BLUETOOTH_SCAN",
                "android.permission.BLUETOOTH"]
        )
        const val ACTION_DISCOVERY_FINISHED = FwkBluetoothAdapter.ACTION_DISCOVERY_FINISHED

        /**
         * Broadcast Action: The local Bluetooth adapter has started the remote
         * device discovery process.
         * <p>This usually involves an inquiry scan of about 12 seconds, followed
         * by a page scan of each new device to retrieve its Bluetooth name.
         * <p>Register for {@link BluetoothDevice#ACTION_FOUND} to be notified as
         * remote Bluetooth devices are found.
         * <p>Device discovery is a heavyweight procedure. New connections to
         * remote Bluetooth devices should not be attempted while discovery is in
         * progress, and existing connections will experience limited bandwidth
         * and high latency. Use {@link #cancelDiscovery()} to cancel an ongoing
         * discovery.
         */
        @RequiresPermission(
            anyOf = ["android.permission.BLUETOOTH_SCAN",
                "android.permission.BLUETOOTH"]
        )
        const val ACTION_DISCOVERY_STARTED = FwkBluetoothAdapter.ACTION_DISCOVERY_STARTED

        /**
         * Broadcast Action: The local Bluetooth adapter has changed its friendly
         * Bluetooth name.
         * <p>This name is visible to remote Bluetooth devices.
         * <p>Always contains the extra field {@link #EXTRA_LOCAL_NAME} containing
         * the name.
         */
        @RequiresPermission(
            anyOf = ["android.permission.BLUETOOTH_CONNECT",
                "android.permission.BLUETOOTH"]
        )
        const val ACTION_LOCAL_NAME_CHANGED = FwkBluetoothAdapter.ACTION_LOCAL_NAME_CHANGED

        /**
         * Activity Action: Show a system activity that requests discoverable mode.
         * This activity will also request the user to turn on Bluetooth if it
         * is not currently enabled.
         * <p>Discoverable mode is equivalent to {@link
         * #SCAN_MODE_CONNECTABLE_DISCOVERABLE}. It allows remote devices to see
         * this Bluetooth adapter when they perform a discovery.
         * <p>For privacy, Android is not discoverable by default.
         * <p>The sender of this Intent can optionally use extra field {@link
         * #EXTRA_DISCOVERABLE_DURATION} to request the duration of
         * discoverability. Currently the default duration is 120 seconds, and
         * maximum duration is capped at 300 seconds for each request.
         * <p>Notification of the result of this activity is posted using the
         * {@link android.app.Activity#onActivityResult} callback. The
         * <code>resultCode</code>
         * will be the duration (in seconds) of discoverability or
         * {@link android.app.Activity#RESULT_CANCELED} if the user rejected
         * discoverability or an error has occurred.
         * <p>Applications can also listen for {@link #ACTION_SCAN_MODE_CHANGED}
         * for global notification whenever the scan mode changes. For example, an
         * application can be notified when the device has ended discoverability.
         */
        @RequiresPermission(
            anyOf = ["android.permission.BLUETOOTH_ADVERTISE",
                "android.permission.BLUETOOTH"]
        )
        const val ACTION_REQUEST_DISCOVERABLE = FwkBluetoothAdapter.ACTION_REQUEST_DISCOVERABLE

        /**
         * Activity Action: Show a system activity that allows the user to turn on
         * Bluetooth.
         * <p>This system activity will return once Bluetooth has completed turning
         * on, or the user has decided not to turn Bluetooth on.
         * <p>Notification of the result of this activity is posted using the
         * {@link android.app.Activity#onActivityResult} callback. The
         * <code>resultCode</code>
         * will be {@link android.app.Activity#RESULT_OK} if Bluetooth has been
         * turned on or {@link android.app.Activity#RESULT_CANCELED} if the user
         * has rejected the request or an error has occurred.
         * <p>Applications can also listen for {@link #ACTION_STATE_CHANGED}
         * for global notification whenever Bluetooth is turned on or off.
         */
        @RequiresPermission(
            anyOf = ["android.permission.BLUETOOTH_CONNECT",
                "android.permission.BLUETOOTH"]
        )
        const val ACTION_REQUEST_ENABLE = FwkBluetoothAdapter.ACTION_REQUEST_ENABLE

        /**
         * Broadcast Action: Indicates the Bluetooth scan mode of the local Adapter
         * has changed.
         * <p>Always contains the extra fields {@link #EXTRA_SCAN_MODE} and {@link
         * #EXTRA_PREVIOUS_SCAN_MODE} containing the new and old scan modes
         * respectively.
         */
        @RequiresPermission(
            anyOf = ["android.permission.BLUETOOTH_SCAN",
                "android.permission.BLUETOOTH"]
        )
        const val ACTION_SCAN_MODE_CHANGED = FwkBluetoothAdapter.ACTION_SCAN_MODE_CHANGED

        /**
         * Broadcast Action: The local Bluetooth adapter has changed its friendly
         * Bluetooth name.
         * <p>This name is visible to remote Bluetooth devices.
         * <p>Always contains the extra field {@link #EXTRA_LOCAL_NAME} containing
         * the name.
         *
         * This require {@link Manifest#BLUETOOTH} permission on {@link Build.VERSION_CODE#R} or
         * lower
         */
        @RequiresPermission(
            anyOf = ["android.permission.BLUETOOTH_CONNECT",
                "android.permission.BLUETOOTH"]
        )
        const val ACTION_STATE_CHANGED = FwkBluetoothAdapter.ACTION_STATE_CHANGED

        /**
         * Sentinel error value for this class. Guaranteed to not equal any other
         * integer constant in this class. Provided as a convenience for functions
         * that require a sentinel error value, for example:
         * <p><code>Intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
         * BluetoothAdapter.ERROR)</code>
         */
        const val ERROR = FwkBluetoothAdapter.ERROR

        /**
         * Extra used by {@link #ACTION_CONNECTION_STATE_CHANGED}
         *
         * This extra represents the current connection state.
         */
        const val EXTRA_CONNECTION_STATE = FwkBluetoothAdapter.EXTRA_CONNECTION_STATE

        /**
         * Used as an optional int extra field in {@link
         * #ACTION_REQUEST_DISCOVERABLE} intents to request a specific duration
         * for discoverability in seconds. The current default is 120 seconds, and
         * requests over 300 seconds will be capped. These values could change.
         */
        const val EXTRA_DISCOVERABLE_DURATION = FwkBluetoothAdapter.EXTRA_DISCOVERABLE_DURATION

        /**
         * Used as a String extra field in {@link #ACTION_LOCAL_NAME_CHANGED}
         * intents to request the local Bluetooth name.
         */
        const val EXTRA_LOCAL_NAME = FwkBluetoothAdapter.EXTRA_LOCAL_NAME

        /**
         * Extra used by {@link #ACTION_CONNECTION_STATE_CHANGED}
         *
         * This extra represents the previous connection state.
         */
        const val EXTRA_PREVIOUS_CONNECTION_STATE =
            FwkBluetoothAdapter.EXTRA_PREVIOUS_CONNECTION_STATE

        /**
         * Used as an int extra field in {@link #ACTION_SCAN_MODE_CHANGED}
         * intents to request the previous scan mode. Possible values are:
         * {@link #SCAN_MODE_NONE},
         * {@link #SCAN_MODE_CONNECTABLE},
         * {@link #SCAN_MODE_CONNECTABLE_DISCOVERABLE},
         */
        const val EXTRA_PREVIOUS_SCAN_MODE = FwkBluetoothAdapter.EXTRA_PREVIOUS_SCAN_MODE

        /**
         * Used as an int extra field in {@link #ACTION_STATE_CHANGED}
         * intents to request the previous power state. Possible values are:
         * {@link #STATE_OFF},
         * {@link #STATE_TURNING_ON},
         * {@link #STATE_ON},
         * {@link #STATE_TURNING_OFF}
         */
        const val EXTRA_PREVIOUS_STATE = FwkBluetoothAdapter.EXTRA_PREVIOUS_STATE

        /**
         * Used as an int extra field in {@link #ACTION_SCAN_MODE_CHANGED}
         * intents to request the current scan mode. Possible values are:
         * {@link #SCAN_MODE_NONE},
         * {@link #SCAN_MODE_CONNECTABLE},
         * {@link #SCAN_MODE_CONNECTABLE_DISCOVERABLE},
         */
        const val EXTRA_SCAN_MODE = FwkBluetoothAdapter.EXTRA_SCAN_MODE

        /**
         * Used as an int extra field in {@link #ACTION_STATE_CHANGED}
         * intents to request the current power state. Possible values are:
         * {@link #STATE_OFF},
         * {@link #STATE_TURNING_ON},
         * {@link #STATE_ON},
         * {@link #STATE_TURNING_OFF},
         */
        const val EXTRA_STATE = FwkBluetoothAdapter.EXTRA_STATE

        /**
         * Indicates that inquiry scan is disabled, but page scan is enabled on the
         * local Bluetooth adapter. Therefore this device is not discoverable from
         * remote Bluetooth devices, but is connectable from remote devices that
         * have previously discovered this device.
         */
        const val SCAN_MODE_CONNECTABLE = FwkBluetoothAdapter.SCAN_MODE_CONNECTABLE

        /**
         * Indicates that both inquiry scan and page scan are enabled on the local
         * Bluetooth adapter. Therefore this device is both discoverable and
         * connectable from remote Bluetooth devices.
         */
        const val SCAN_MODE_CONNECTABLE_DISCOVERABLE =
            FwkBluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE

        /**
         * Indicates that both inquiry scan and page scan are disabled on the local
         * Bluetooth adapter. Therefore this device is neither discoverable
         * nor connectable from remote Bluetooth devices.
         */
        const val SCAN_MODE_NONE = FwkBluetoothAdapter.SCAN_MODE_NONE

        /** The profile is in connected state */
        const val STATE_CONNECTED = FwkBluetoothAdapter.STATE_CONNECTED

        /** The profile is in connecting state */
        const val STATE_CONNECTING = FwkBluetoothAdapter.STATE_CONNECTING

        /** The profile is in disconnected state */
        const val STATE_DISCONNECTED = FwkBluetoothAdapter.STATE_DISCONNECTED

        /** The profile is in disconnecting state */
        const val STATE_DISCONNECTING = FwkBluetoothAdapter.STATE_DISCONNECTING

        /**
         * Indicates the local Bluetooth adapter is off.
         */
        const val STATE_OFF = FwkBluetoothAdapter.STATE_OFF

        /**
         * Indicates the local Bluetooth adapter is on.
         */
        const val STATE_ON = FwkBluetoothAdapter.STATE_ON

        /**
         * Indicates the local Bluetooth adapter is turning off. Local clients
         * should immediately attempt graceful disconnection of any remote links.
         */
        const val STATE_TURNING_OFF = FwkBluetoothAdapter.STATE_TURNING_OFF

        /**
         * Indicates the local Bluetooth adapter is turning on. However local
         * clients should wait for {@link #STATE_ON} before attempting to
         * use the adapter.
         */
        const val STATE_TURNING_ON = FwkBluetoothAdapter.STATE_TURNING_ON

        /**
         * Validate a String Bluetooth address, such as "00:43:A8:23:10:F0"
         * <p>Alphabetic characters must be uppercase to be valid.
         *
         * @param address Bluetooth address as string
         * @return true if the address is valid, false otherwise
         */
        fun checkBluetoothAddress(address: String): Boolean {
            return FwkBluetoothAdapter.checkBluetoothAddress(address)
        }
    }

    private val impl: BluetoothAdapterImpl =
        if (Build.VERSION.SDK_INT < 26) {
            // Min Sdk is 21
            BluetoothAdapterImplBase()
        } else if (Build.VERSION.SDK_INT < 29) {
            BluetoothAdapterImplApi26()
        } else if (Build.VERSION.SDK_INT < 33) {
            BluetoothAdapterImplApi29()
        } else {
            BluetoothAdapterImplApi33()
        }

    internal interface BluetoothAdapterImpl {
        fun startDiscovery(): Boolean
        fun cancelDiscovery(): Boolean
        fun closeProfileProxy(profile: Int, proxy: FwkBluetoothProfile)

        val address: String

        val bluetoothLeAdvertiser: FwkBluetoothLeAdvertiser?

        val bluetoothLeScanner: FwkBluetoothLeScanner?

        val bondedDevices: Set<FwkBluetoothDevice>

        val discoverableTimeout: Duration?

        val leMaximumAdvertisingDataLength: Int

        val maxConnectedAudioDevices: Int

        val name: String
        fun setName(name: String): Boolean

        // TODO: Implement getProfileConnectionState when BluetoothX support Bluetooth Classic

        // TODO: implement getProfileProxy when library support Bluetooth Classic

        fun getRemoteDevice(address: ByteArray): FwkBluetoothDevice
        fun getRemoteDevice(address: String): FwkBluetoothDevice
        fun getRemoteLeDevice(address: String, addressType: Int): FwkBluetoothDevice

        val state: Int

        val isDiscovering: Boolean

        val isEnabled: Boolean

        val isLe2MPhySupported: Boolean

        val isLeCodedPhySupported: Boolean

        val isLeExtendedAdvertisingSupported: Boolean

        val isLePeriodicAdvertisingSupported: Boolean

        val isMultipleAdvertisementSupported: Boolean

        val isOffloadedScanBatchingSupported: Boolean

        val isLeAudioBroadcastAssistantSupported: Int

        val isLeAudioBroadcastSourceSupported: Int

        val isLeAudioSupported: Int
        fun listenUsingInsecureL2capChannel(): BluetoothServerSocket
        fun listenUsingL2capChannel(): BluetoothServerSocket
        fun listenUsingInsecureRfcommWithServiceRecord(
            name: String,
            uuid: UUID
        ): BluetoothServerSocket

        fun listenUsingRfcommWithServiceRecord(name: String, uuid: UUID): BluetoothServerSocket
    }

    internal open inner class BluetoothAdapterImplBase : BluetoothAdapterImpl {
        @RequiresPermission(
            anyOf = [
                "android.permission.BLUETOOTH_ADMIN",
                "android.permission.BLUETOOTH_SCAN",
            ]
        )
        override fun cancelDiscovery(): Boolean {
            return fwkAdapter.cancelDiscovery()
        }

        @RequiresPermission(
            anyOf = [
                "android.permission.BLUETOOTH_ADMIN",
                "android.permission.BLUETOOTH_SCAN",
            ]
        )
        override fun startDiscovery(): Boolean {
            return fwkAdapter.startDiscovery()
        }

        // TODO: Change proxy into library's BluetoothProfile
        override fun closeProfileProxy(profile: Int, proxy: FwkBluetoothProfile) {
            fwkAdapter.closeProfileProxy(profile, proxy)
        }

        @get:RequiresPermission(
            anyOf = [
                "android.permission.BLUETOOTH",
                "android.permission.BLUETOOTH_CONNECT",
                "android.permission.LOCAL_MAC_ADDRESS",
            ]
        )
        override val address: String
            get() = fwkAdapter.address

        // TODO: Change to Library's BluetoothLeAdvertiser when available
        override val bluetoothLeAdvertiser: FwkBluetoothLeAdvertiser?
            get() = fwkAdapter.bluetoothLeAdvertiser

        // TODO: Change to Library BluetoothLeScanner when available
        override val bluetoothLeScanner: FwkBluetoothLeScanner?
            get() = fwkAdapter.bluetoothLeScanner

        // TODO: Change to Library BluetoothDevice when available
        @get:RequiresPermission(
            anyOf = ["android.permission.BLUETOOTH",
                "android.permission.BLUETOOTH_CONNECT"]
        )
        override val bondedDevices: Set<FwkBluetoothDevice>
            get() = fwkAdapter.bondedDevices

        @get:RequiresPermission("android.permission.BLUETOOTH_SCAN")
        override val discoverableTimeout: Duration?
            get() {
                TODO("Implement backward-compatibility")
            }

        @get:RequiresPermission("android.permission.BLUETOOTH")
        override val leMaximumAdvertisingDataLength: Int
            get() {
                TODO("Add implementation for lower sdk version")
            }

        @get:RequiresPermission("android.permission.BLUETOOTH_CONNECT")
        override val maxConnectedAudioDevices: Int
            get() {
                TODO("Add implementation for lower sdk version")
            }

        /**
         * Get the friendly Bluetooth name of the local Bluetooth adapter.
         * <p>This name is visible to remote Bluetooth devices.
         *
         * @return the Bluetooth name, or null on error
         */
        @get:RequiresPermission(
            anyOf = ["android.permission.BLUETOOTH_CONNECT",
                "android.permission.BLUETOOTH"]
        )
        override val name: String
            get() = fwkAdapter.name

        /**
         * Set the friendly Bluetooth name of the local Bluetooth adapter.
         * <p>This name is visible to remote Bluetooth devices.
         * <p>Valid Bluetooth names are a maximum of 248 bytes using UTF-8
         * encoding, although many remote devices can only display the first
         * 40 characters, and some may be limited to just 20.
         * <p>If Bluetooth state is not {@link #STATE_ON}, this API
         * will return false. After turning on Bluetooth,
         * wait for {@link #ACTION_STATE_CHANGED} with {@link #STATE_ON}
         * to get the updated value.
         *
         * @param name a valid Bluetooth name
         * @return true if the name was set, false otherwise
         */
        @RequiresPermission(
            anyOf = ["android.permission.BLUETOOTH_CONNECT",
                "android.permission.BLUETOOTH_ADMIN"]
        )
        override fun setName(name: String): Boolean {
            return fwkAdapter.setName(name)
        }

        // TODO: Change to Library BluetoothDevice when available
        override fun getRemoteDevice(address: ByteArray): FwkBluetoothDevice {
            return fwkAdapter.getRemoteDevice(address)
        }

        override fun getRemoteDevice(address: String): FwkBluetoothDevice {
            return fwkAdapter.getRemoteDevice(address)
        }

        override fun getRemoteLeDevice(address: String, addressType: Int): FwkBluetoothDevice {
            TODO("Handle backward compatibility")
        }

        override val state: Int get() = fwkAdapter.state

        @get:RequiresPermission(
            anyOf = ["android.permission.BLUETOOTH",
                "android.permission.BLUETOOTH_SCAN"]
        )
        override val isDiscovering: Boolean
            get() = fwkAdapter.isDiscovering

        override val isEnabled: Boolean
            get() = fwkAdapter.isEnabled

        override val isLe2MPhySupported: Boolean
            get() {
                TODO("Handle backward-compatibility")
            }

        override val isLeCodedPhySupported: Boolean
            get() {
                TODO("Handle backward-compatibility")
            }

        override val isLeExtendedAdvertisingSupported: Boolean
            get() {
                TODO("Handle backward-compatibility")
            }

        override val isLePeriodicAdvertisingSupported: Boolean
            get() {
                TODO("Handle backward-compatibility")
            }

        override val isMultipleAdvertisementSupported: Boolean
            get() = fwkAdapter.isMultipleAdvertisementSupported

        override val isOffloadedScanBatchingSupported: Boolean
            get() = fwkAdapter.isOffloadedScanBatchingSupported

        override val isLeAudioBroadcastAssistantSupported: Int
            get() {
                TODO("Handle backward-compatibility")
            }

        override val isLeAudioBroadcastSourceSupported: Int
            get() {
                TODO("Handle backward-compatibility")
            }

        override val isLeAudioSupported: Int
            get() {
                TODO("Handle backward-compatibility")
            }

        override fun listenUsingInsecureL2capChannel(): BluetoothServerSocket {
            TODO("Handle backward-compatibility")
        }

        override fun listenUsingL2capChannel(): BluetoothServerSocket {
            TODO("Handle backward-compatibility")
        }

        @RequiresPermission("android.permission.BLUETOOTH_CONNECT")
        override fun listenUsingInsecureRfcommWithServiceRecord(
            name: String,
            uuid: UUID
        ): BluetoothServerSocket {
            return fwkAdapter.listenUsingInsecureRfcommWithServiceRecord(name, uuid)
        }

        @RequiresPermission("android.permission.BLUETOOTH_CONNECT")
        override fun listenUsingRfcommWithServiceRecord(
            name: String,
            uuid: UUID
        ): BluetoothServerSocket {
            return fwkAdapter.listenUsingRfcommWithServiceRecord(name, uuid)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    internal open inner class BluetoothAdapterImplApi26 : BluetoothAdapterImplBase() {

        @get:RequiresPermission("android.permission.BLUETOOTH")
        override val leMaximumAdvertisingDataLength: Int
            get() {
                return fwkAdapter.leMaximumAdvertisingDataLength
            }

        override val isLe2MPhySupported: Boolean
            get() {
                return fwkAdapter.isLe2MPhySupported
            }

        override val isLeCodedPhySupported: Boolean
            get() {
                return fwkAdapter.isLeCodedPhySupported
            }

        override val isLeExtendedAdvertisingSupported: Boolean
            get() {
                return fwkAdapter.isLeExtendedAdvertisingSupported
            }

        override val isLePeriodicAdvertisingSupported: Boolean
            get() {
                return fwkAdapter.isLePeriodicAdvertisingSupported
            }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    internal open inner class BluetoothAdapterImplApi29() : BluetoothAdapterImplApi26() {

        @RequiresPermission(anyOf = ["android.permission.BLUETOOTH",
            "android.permission.BLUETOOTH_CONNECT"])
        override fun listenUsingInsecureL2capChannel(): BluetoothServerSocket {
            return fwkAdapter.listenUsingInsecureL2capChannel()
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    internal open inner class BluetoothAdapterImplApi33 : BluetoothAdapterImplApi29() {

        @get:RequiresPermission("android.permission.BLUETOOTH_CONNECT")
        override val maxConnectedAudioDevices: Int
            get() {
                return fwkAdapter.maxConnectedAudioDevices
            }

        @get:RequiresPermission("android.permission.BLUETOOTH_SCAN")
        override val discoverableTimeout: Duration?
            get() {
                return fwkAdapter.discoverableTimeout
            }

        override fun getRemoteLeDevice(address: String, addressType: Int): FwkBluetoothDevice {
            return fwkAdapter.getRemoteLeDevice(address, addressType)
        }

        override val isLeAudioBroadcastAssistantSupported: Int
            get() {
                return fwkAdapter.isLeAudioBroadcastAssistantSupported
            }
        override val isLeAudioBroadcastSourceSupported: Int
            get() {
                return fwkAdapter.isLeAudioBroadcastSourceSupported
            }
        override val isLeAudioSupported: Int
            get() {
                return fwkAdapter.isLeAudioSupported
            }
    }

    /**
     * Cancel the current device discovery process.
     * <p>Because discovery is a heavyweight procedure for the Bluetooth
     * adapter, this method should always be called before attempting to connect
     * to a remote device with {@link
     * android.bluetooth.BluetoothSocket#connect()}. Discovery is not managed by
     * the  Activity, but is run as a system service, so an application should
     * always call cancel discovery even if it did not directly request a
     * discovery, just to be sure.
     * <p>If Bluetooth state is not {@link #STATE_ON}, this API
     * will return false. After turning on Bluetooth,
     * wait for {@link #ACTION_STATE_CHANGED} with {@link #STATE_ON}
     * to get the updated value.
     *
     * @return true on success, false on error
     * For apps targeting Build.VERSION_CODES#R or lower, this requires the
     * Manifest.permission#BLUETOOTH_ADMIN permission which can be gained with a simple
     * <uses-permission> manifest tag. For apps targeting Build.VERSION_CODES#S or or higher,
     * this requires the Manifest.permission#BLUETOOTH_SCAN permission which can be gained with
     * Activity.requestPermissions(String[], int).
     */
    @RequiresPermission(
        anyOf = [
            "android.permission.BLUETOOTH_ADMIN",
            "android.permission.BLUETOOTH_SCAN",
        ]
    )
    fun cancelDiscovery(): Boolean {
        return impl.cancelDiscovery()
    }

    /**
     * Start the remote device discovery process.
     * <p>The discovery process usually involves an inquiry scan of about 12
     * seconds, followed by a page scan of each new device to retrieve its
     * Bluetooth name.
     * <p>This is an asynchronous call, it will return immediately. Register
     * for {@link #ACTION_DISCOVERY_STARTED} and {@link
     * #ACTION_DISCOVERY_FINISHED} intents to determine exactly when the
     * discovery starts and completes. Register for {@link
     * BluetoothDevice#ACTION_FOUND} to be notified as remote Bluetooth devices
     * are found.
     * <p>Device discovery is a heavyweight procedure. New connections to
     * remote Bluetooth devices should not be attempted while discovery is in
     * progress, and existing connections will experience limited bandwidth
     * and high latency. Use {@link #cancelDiscovery()} to cancel an ongoing
     * discovery. Discovery is not managed by the Activity,
     * but is run as a system service, so an application should always call
     * {@link BluetoothAdapter#cancelDiscovery()} even if it
     * did not directly request a discovery, just to be sure.
     * <p>Device discovery will only find remote devices that are currently
     * <i>discoverable</i> (inquiry scan enabled). Many Bluetooth devices are
     * not discoverable by default, and need to be entered into a special mode.
     * <p>If Bluetooth state is not {@link #STATE_ON}, this API
     * will return false. After turning on Bluetooth, wait for {@link #ACTION_STATE_CHANGED}
     * with {@link #STATE_ON} to get the updated value.
     * <p>If a device is currently bonding, this request will be queued and executed once that
     * device has finished bonding. If a request is already queued, this request will be ignored.
     *
     * For apps targeting Build.VERSION_CODES#R or lower, this requires the
     * Manifest.permission#BLUETOOTH_ADMIN permission which can be gained with a simple
     * <uses-permission> manifest tag.
     * For apps targeting Build.VERSION_CODES#S or or higher, this requires the
     * Manifest.permission#BLUETOOTH_SCAN permission which can be gained with
     * Activity.requestPermissions(String[], int).
     * In addition, this requires either the Manifest.permission#ACCESS_FINE_LOCATION permission or
     * strong assertion that you will never derive the physical location of the device. You can make
     * this assertion by declaring usesPermissionFlags="neverForLocation" on the relevant
     * <uses-permission> manifest tag, but it may restrict the types of Bluetooth devices you can
     * interact with.
     *
     * @return true on success, false on error
     */
    @RequiresPermission(
        anyOf = [
            "android.permission.BLUETOOTH_ADMIN",
            "android.permission.BLUETOOTH_SCAN",
        ]
    )
    fun startDiscovery(): Boolean {
        return impl.startDiscovery()
    }

    /**
     * Close the connection of the profile proxy to the Service.
     *
     * <p> Clients should call this when they are no longer using
     * the proxy obtained from {@link #getProfileProxy}.
     * Profile can be one of  {@link BluetoothProfile#HEADSET} or {@link BluetoothProfile#A2DP}
     *
     * @param profile
     * @param proxy Profile proxy object
     */
    // TODO: Change proxy into library's BluetoothProfile
    fun closeProfileProxy(profile: Int, proxy: FwkBluetoothProfile) {
        impl.closeProfileProxy(profile, proxy)
    }

    /**
     * Returns the hardware address of the local Bluetooth adapter.
     * For example, "00:11:22:AA:BB:CC".
     * For apps targeting Build.VERSION_CODES#R or lower, this requires the
     * Manifest.permission#BLUETOOTH permission which can be gained with a simple <uses-permission>
     * manifest tag.
     * For apps targeting Build.VERSION_CODES#S or or higher, this requires the
     * Manifest.permission#BLUETOOTH_CONNECT permission which can be gained with
     * Activity.requestPermissions(String[], int).
     */
    @get:RequiresPermission(
        anyOf = [
            "android.permission.BLUETOOTH",
            "android.permission.BLUETOOTH_CONNECT",
            "android.permission.LOCAL_MAC_ADDRESS",
        ]
    )
    val address: String
        get() = impl.address

    /**
     * Returns a {@link BluetoothLeAdvertiser} object for Bluetooth LE Advertising operations.
     * Will return null if Bluetooth is turned off or if Bluetooth LE Advertising is not
     * supported on this device.
     * <p>
     * Use {@link #isMultipleAdvertisementSupported()} to check whether LE Advertising is supported
     * on this device before calling this method.
     */
    // TODO: Change to Library's BluetoothLeAdvertiser when available
    val bluetoothLeAdvertiser: FwkBluetoothLeAdvertiser?
        get() = impl.bluetoothLeAdvertiser

    /**
     * Returns a {@link BluetoothLeScanner} object for Bluetooth LE scan operations.
     */
    // TODO: Change to Library BluetoothLeScanner when available
    val bluetoothLeScanner: FwkBluetoothLeScanner?
        get() = impl.bluetoothLeScanner

    @get:RequiresPermission(
        anyOf = ["android.permission.BLUETOOTH",
            "android.permission.BLUETOOTH_CONNECT"]
    )
    // TODO: Change to Library BluetoothDevice when available
    val bondedDevices: Set<FwkBluetoothDevice>
        get() = impl.bondedDevices

    /**
     * Get the timeout duration of the {@link #SCAN_MODE_CONNECTABLE_DISCOVERABLE}.
     *
     * @return the duration of the discoverable timeout or null if an error has occurred
     */
    @get:RequiresPermission("android.permission.BLUETOOTH_SCAN")
    // TODO: Implement when sdk 33 is available
    // TODO: Implement DurationCompat
    val discoverableTimeout: Duration? = impl.discoverableTimeout

    /**
     * Return the maximum LE advertising data length in bytes, if LE Extended Advertising feature is
     * supported, null otherwise.
     * For apps targeting Build.VERSION_CODES#R or lower, this requires the
     * Manifest.permission#BLUETOOTH permission which can be gained with a simple <uses-permission>
     * manifest tag.
     */
    @get:RequiresPermission("android.permission.BLUETOOTH")
    val leMaximumAdvertisingDataLength: Int
        get() {
            return impl.leMaximumAdvertisingDataLength
        }

    /**
     * Get the maximum number of connected devices per audio profile for this device.
     *
     * @return the number of allowed simultaneous connected devices for each audio profile
     *         for this device, or -1 if the Bluetooth service can't be reached. For SDK versions
     *         smaller than 33, always return -1
     */
    @get:RequiresPermission("android.permission.BLUETOOTH_CONNECT")
    val maxConnectedAudioDevices: Int
        get() {
            return impl.maxConnectedAudioDevices
        }

    @get:RequiresPermission(
        anyOf = ["android.permission.BLUETOOTH_CONNECT",
            "android.permission.BLUETOOTH"]
    )
    val name: String
        /**
         * Get the friendly Bluetooth name of the local Bluetooth adapter.
         * <p>This name is visible to remote Bluetooth devices.
         *
         * @return the Bluetooth name, or null on error
         */
        get() = impl.name

    /**
     * Set the friendly Bluetooth name of the local Bluetooth adapter.
     * <p>This name is visible to remote Bluetooth devices.
     * <p>Valid Bluetooth names are a maximum of 248 bytes using UTF-8
     * encoding, although many remote devices can only display the first
     * 40 characters, and some may be limited to just 20.
     * <p>If Bluetooth state is not {@link #STATE_ON}, this API
     * will return false. After turning on Bluetooth,
     * wait for {@link #ACTION_STATE_CHANGED} with {@link #STATE_ON}
     * to get the updated value.
     *
     * @param name a valid Bluetooth name
     * @return true if the name was set, false otherwise
     */
    @RequiresPermission(
        anyOf = ["android.permission.BLUETOOTH_CONNECT",
            "android.permission.BLUETOOTH_ADMIN"]
    )
    fun setName(name: String): Boolean {
        return impl.setName(name)
    }

    // TODO: Implement getProfileConnectionState when BluetoothX support Bluetooth Classic

    // TODO: implement getProfileProxy when library support Bluetooth Classic

    /**
     * Get a {@link BluetoothDevice} object for the given Bluetooth hardware
     * address.
     * <p>Valid Bluetooth hardware addresses must be 6 bytes. This method
     * expects the address in network byte order (MSB first).
     * <p>A {@link BluetoothDevice} will always be returned for a valid
     * hardware address, even if this adapter has never seen that device.
     *
     * @param address Bluetooth MAC address (6 bytes)
     * @throws IllegalArgumentException if address is invalid
     */
    // TODO: Change to Library BluetoothDevice when available
    fun getRemoteDevice(address: ByteArray): FwkBluetoothDevice {
        return impl.getRemoteDevice(address)
    }

    /**
     * Get a {@link BluetoothDevice} object for the given Bluetooth hardware
     * address.
     * <p>Valid Bluetooth hardware addresses must be upper case, in big endian byte order, and in a
     * format such as "00:11:22:33:AA:BB". The helper {@link #checkBluetoothAddress} is
     * available to validate a Bluetooth address.
     * <p>A {@link BluetoothDevice} will always be returned for a valid
     * hardware address, even if this adapter has never seen that device.
     *
     * @param address valid Bluetooth MAC address
     * @throws IllegalArgumentException if address is invalid
     */
    // TODO: Change to Library BluetoothDevice when available
    fun getRemoteDevice(address: String): FwkBluetoothDevice {
        return impl.getRemoteDevice(address)
    }

    /**
     * Get a {@link BluetoothDevice} object for the given Bluetooth hardware
     * address and addressType.
     * <p>Valid Bluetooth hardware addresses must be upper case, in big endian byte order, and in a
     * format such as "00:11:22:33:AA:BB". The helper {@link #checkBluetoothAddress} is
     * available to validate a Bluetooth address.
     * <p>A {@link BluetoothDevice} will always be returned for a valid
     * hardware address and type, even if this adapter has never seen that device.
     *
     * @param address valid Bluetooth MAC address
     * @param addressType Bluetooth address type
     * @throws IllegalArgumentException if address is invalid
     */
    // TODO: Change to Library BluetoothDevice when available
    fun getRemoteLeDevice(address: String, addressType: Int): FwkBluetoothDevice {
        return impl.getRemoteLeDevice(address, addressType)
    }

    // TODO: Add getScanMode when BluetoothX supports Bluetooth Classic

    /**
     * Get the current state of the local Bluetooth adapter.
     * <p>Possible return values are
     * {@link #STATE_OFF},
     * {@link #STATE_TURNING_ON},
     * {@link #STATE_ON},
     * {@link #STATE_TURNING_OFF}.
     *
     * For apps targeting Build.VERSION_CODES#R or lower, this requires the
     * Manifest.permission#BLUETOOTH permission which can be gained with a simple <uses-permission>
     * manifest tag.
     *
     * @return current state of Bluetooth adapter
     */
    val state: Int get() = impl.state

    /**
     * Return true if the local Bluetooth adapter is currently in the device
     * discovery process.
     * <p>Device discovery is a heavyweight procedure. New connections to
     * remote Bluetooth devices should not be attempted while discovery is in
     * progress, and existing connections will experience limited bandwidth
     * and high latency. Use {@link #cancelDiscovery()} to cancel an ongoing
     * discovery.
     * <p>Applications can also register for {@link #ACTION_DISCOVERY_STARTED}
     * or {@link #ACTION_DISCOVERY_FINISHED} to be notified when discovery
     * starts or completes.
     * <p>If Bluetooth state is not {@link #STATE_ON}, this API
     * will return false. After turning on Bluetooth,
     * wait for {@link #ACTION_STATE_CHANGED} with {@link #STATE_ON}
     * to get the updated value.
     *
     * @return true if discovering
     */
    @get:RequiresPermission(
        anyOf = ["android.permission.BLUETOOTH",
            "android.permission.BLUETOOTH_SCAN"]
    )
    val isDiscovering: Boolean
        get() = impl.isDiscovering

    /**
     * Return true if Bluetooth is currently enabled and ready for use.
     * <p>Equivalent to:
     * <code>getBluetoothState() == STATE_ON</code>
     *
     * For apps targeting Build.VERSION_CODES#R or lower, this requires the
     * Manifest.permission#BLUETOOTH permission which can be gained with a simple <uses-permission>
     * manifest tag.
     *
     * @return true if the local adapter is turned on
     */
    val isEnabled: Boolean
        get() = impl.isEnabled

    /**
     * Return true if LE 2M PHY feature is supported.
     *
     * For apps targeting Build.VERSION_CODES#R or lower, this requires the
     * Manifest.permission#BLUETOOTH permission which can be gained with a simple <uses-permission>
     * manifest tag.
     *
     * @return true if chipset supports LE 2M PHY feature
     */
    val isLe2MPhySupported: Boolean
        get() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                return impl.isLe2MPhySupported
            } else {
                TODO("Handle backward-compatibility")
            }
        }

    /**
     * Return true if LE Coded PHY feature is supported.
     *
     * For apps targeting Build.VERSION_CODES#R or lower, this requires the
     * Manifest.permission#BLUETOOTH permission which can be gained with a simple <uses-permission>
     * manifest tag.
     *
     * @return true if chipset supports LE Coded PHY feature
     */
    val isLeCodedPhySupported: Boolean
        get() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                return impl.isLeCodedPhySupported
            } else {
                TODO("Handle backward-compatibility")
            }
        }

    /**
     * Return true if LE Extended Advertising feature is supported.
     *
     * For apps targeting Build.VERSION_CODES#R or lower, this requires the
     * Manifest.permission#BLUETOOTH permission which can be gained with a simple <uses-permission>
     * manifest tag.
     *
     * @return true if chipset supports LE Extended Advertising feature
     */
    val isLeExtendedAdvertisingSupported: Boolean
        get() {
            return impl.isLeExtendedAdvertisingSupported
        }

    /**
     * Return true if LE Periodic Advertising feature is supported.
     *
     * For apps targeting Build.VERSION_CODES#R or lower, this requires the
     * Manifest.permission#BLUETOOTH permission which can be gained with a simple <uses-permission>
     * manifest tag.
     *
     * @return true if chipset supports LE Periodic Advertising feature
     */
    val isLePeriodicAdvertisingSupported: Boolean
        @RequiresApi(Build.VERSION_CODES.O)
        get() {
            return impl.isLePeriodicAdvertisingSupported
        }

    /**
     * Return true if the multi advertisement is supported by the chipset
     *
     * For apps targeting Build.VERSION_CODES#R or lower, this requires the
     * Manifest.permission#BLUETOOTH permission which can be gained with a simple <uses-permission>
     * manifest tag.
     *
     * @return true if Multiple Advertisement feature is supported
     */
    val isMultipleAdvertisementSupported: Boolean
        get() = impl.isMultipleAdvertisementSupported

    /**
     * Return true if offloaded scan batching is supported
     *
     * For apps targeting Build.VERSION_CODES#R or lower, this requires the
     * Manifest.permission#BLUETOOTH permission which can be gained with a simple <uses-permission>
     * manifest tag.
     *
     * @return true if chipset supports on-chip scan batching
     */
    val isOffloadedScanBatchingSupported: Boolean
        get() = impl.isOffloadedScanBatchingSupported

    /**
     * Returns {@link BluetoothStatusCodes#FEATURE_SUPPORTED} if the LE audio broadcast assistant
     * feature is supported, {@link BluetoothStatusCodes#FEATURE_NOT_SUPPORTED} if the feature is
     * not supported, or an error code.
     *
     * @return whether the LE audio broadcast assistent is supported
     * @throws IllegalStateException if the bluetooth service is null
     */

    val isLeAudioBroadcastAssistantSupported: Int
        get() {
            return impl.isLeAudioBroadcastAssistantSupported
        }

    /**
     * Returns {@link BluetoothStatusCodes#FEATURE_SUPPORTED} if the LE audio broadcast source
     * feature is supported, {@link BluetoothStatusCodes#FEATURE_NOT_SUPPORTED} if the feature
     * is not supported, or an error code.
     *
     * @return whether the LE audio broadcast source is supported
     * @throws IllegalStateException if the bluetooth service is null
     */
    val isLeAudioBroadcastSourceSupported: Int
        get() {
            return impl.isLeAudioBroadcastSourceSupported
        }

    /**
     * Returns {@link BluetoothStatusCodes#FEATURE_SUPPORTED} if the LE audio feature is
     * supported, {@link BluetoothStatusCodes#FEATURE_NOT_SUPPORTED} if the feature is not
     * supported, or an error code.
     *
     * Android thinks LE audio is supported when the device supports all following profiles:
     * {@link BluetoothProfile#LE_AUDIO}, {@link BluetoothProfile#CSIP_SET_COORDINATOR},
     * volume control profile and media control profile for server role.
     *
     * @return whether the LE audio is supported
     * @throws IllegalStateException if the bluetooth service is null
     */
    val isLeAudioSupported: Int
        get() {
            return impl.isLeAudioSupported
        }

    /**
     * Create an insecure L2CAP Connection-oriented Channel (CoC) {@link BluetoothServerSocket} and
     * assign a dynamic PSM value. This socket can be used to listen for incoming connections. The
     * supported Bluetooth transport is LE only.
     * <p>The link key is not required to be authenticated, i.e. the communication may be vulnerable
     * to person-in-the-middle attacks. Use {@link #listenUsingL2capChannel}, if an encrypted and
     * authenticated communication channel is desired.
     * <p>Use {@link BluetoothServerSocket#accept} to retrieve incoming connections from a listening
     * {@link BluetoothServerSocket}.
     * <p>The system will assign a dynamic protocol/service multiplexer (PSM) value. This PSM value
     * can be read from the {@link BluetoothServerSocket#getPsm()} and this value will be released
     * when this server socket is closed, Bluetooth is turned off, or the application exits
     * unexpectedly.
     * <p>The mechanism of disclosing the assigned dynamic PSM value to the initiating peer is
     * defined and performed by the application.
     * <p>Use {@link BluetoothDevice#createInsecureL2capChannel(int)} to connect to this server
     * socket from another Android device that is given the PSM value.
     *
     * @return an L2CAP CoC BluetoothServerSocket
     * @throws IOException on error, for example Bluetooth not available, or insufficient
     * permissions, or unable to start this CoC
     */
    @RequiresPermission("android.permission.BLUETOOTH_CONNECT")
    fun listenUsingInsecureL2capChannel(): BluetoothServerSocket {
        return impl.listenUsingInsecureL2capChannel()
    }

    /**
     * Create a secure L2CAP Connection-oriented Channel (CoC) {@link BluetoothServerSocket} and
     * assign a dynamic protocol/service multiplexer (PSM) value. This socket can be used to listen
     * for incoming connections. The supported Bluetooth transport is LE only.
     * <p>A remote device connecting to this socket will be authenticated and communication on this
     * socket will be encrypted.
     * <p>Use {@link BluetoothServerSocket#accept} to retrieve incoming connections from a listening
     * {@link BluetoothServerSocket}.
     * <p>The system will assign a dynamic PSM value. This PSM value can be read from the {@link
     * BluetoothServerSocket#getPsm()} and this value will be released when this server socket is
     * closed, Bluetooth is turned off, or the application exits unexpectedly.
     * <p>The mechanism of disclosing the assigned dynamic PSM value to the initiating peer is
     * defined and performed by the application.
     * <p>Use {@link BluetoothDevice#createL2capChannel(int)} to connect to this server
     * socket from another Android device that is given the PSM value.
     *
     * @return an L2CAP CoC BluetoothServerSocket
     * @throws IOException on error, for example Bluetooth not available, or insufficient
     * permissions, or unable to start this CoC
     */

    @RequiresPermission("android.permission.BLUETOOTH_CONNECT")
    fun listenUsingL2capChannel(): BluetoothServerSocket {
        return impl.listenUsingL2capChannel()
    }

    /**
     * Create a listening, insecure RFCOMM Bluetooth socket with Service Record.
     * <p>The link key is not required to be authenticated, i.e. the communication may be
     * vulnerable to Person In the Middle attacks. For Bluetooth 2.1 devices,
     * the link will be encrypted, as encryption is mandatory.
     * For legacy devices (pre Bluetooth 2.1 devices) the link will not
     * be encrypted. Use {@link #listenUsingRfcommWithServiceRecord}, if an
     * encrypted and authenticated communication channel is desired.
     * <p>Use {@link BluetoothServerSocket#accept} to retrieve incoming
     * connections from a listening {@link BluetoothServerSocket}.
     * <p>The system will assign an unused RFCOMM channel to listen on.
     * <p>The system will also register a Service Discovery
     * Protocol (SDP) record with the local SDP server containing the specified
     * UUID, service name, and auto-assigned channel. Remote Bluetooth devices
     * can use the same UUID to query our SDP server and discover which channel
     * to connect to. This SDP record will be removed when this socket is
     * closed, or if this application closes unexpectedly.
     * <p>Use {@link BluetoothDevice#createInsecureRfcommSocketToServiceRecord} to
     * connect to this socket from another device using the same {@link UUID}.
     *
     * @param name service name for SDP record
     * @param uuid uuid for SDP record
     * @return a listening RFCOMM BluetoothServerSocket
     * @throws IOException on error, for example Bluetooth not available, or insufficient
     * permissions, or channel in use.
     */

    @RequiresPermission("android.permission.BLUETOOTH_CONNECT")
    fun listenUsingInsecureRfcommWithServiceRecord(
        name: String,
        uuid: UUID
    ): BluetoothServerSocket {
        return impl.listenUsingInsecureRfcommWithServiceRecord(name, uuid)
    }

    /**
     * Create a listening, secure RFCOMM Bluetooth socket with Service Record.
     * <p>A remote device connecting to this socket will be authenticated and
     * communication on this socket will be encrypted.
     * <p>Use {@link BluetoothServerSocket#accept} to retrieve incoming
     * connections from a listening {@link BluetoothServerSocket}.
     * <p>The system will assign an unused RFCOMM channel to listen on.
     * <p>The system will also register a Service Discovery
     * Protocol (SDP) record with the local SDP server containing the specified
     * UUID, service name, and auto-assigned channel. Remote Bluetooth devices
     * can use the same UUID to query our SDP server and discover which channel
     * to connect to. This SDP record will be removed when this socket is
     * closed, or if this application closes unexpectedly.
     * <p>Use {@link BluetoothDevice#createRfcommSocketToServiceRecord} to
     * connect to this socket from another device using the same {@link UUID}.
     *
     * @param name service name for SDP record
     * @param uuid uuid for SDP record
     * @return a listening RFCOMM BluetoothServerSocket
     * @throws IOException on error, for example Bluetooth not available, or insufficient
     * permissions, or channel in use.
     */

    @RequiresPermission("android.permission.BLUETOOTH_CONNECT")
    fun listenUsingRfcommWithServiceRecord(name: String, uuid: UUID): BluetoothServerSocket {
        return impl.listenUsingRfcommWithServiceRecord(name, uuid)
    }
}