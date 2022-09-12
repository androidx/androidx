/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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

import android.bluetooth.BluetoothDevice as FwkBluetoothDevice
import android.bluetooth.BluetoothGatt as FwkBluetoothGatt
import android.bluetooth.BluetoothGattCallback as FwkBluetoothGattCallback
import android.bluetooth.BluetoothSocket as FwkBluetoothSocket
import android.Manifest
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothStatusCodes
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.ParcelUuid
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import java.util.UUID

/**
 * Represents a remote Bluetooth device. A {@link BluetoothDevice} lets you
 * create a connection with the respective device or query information about
 * it, such as the name, address, class, and bonding state.
 *
 * <p>This class is really just a thin wrapper for a Bluetooth hardware
 * address. Objects of this class are immutable. Operations on this class
 * are performed on the remote Bluetooth hardware address, using the
 * {@link BluetoothAdapter} that was used to create this {@link
 * BluetoothDevice}.
 *
 * <p>To get a {@link BluetoothDevice}, use
 * {@link BluetoothAdapter#getRemoteDevice(String)
 * BluetoothAdapter.getRemoteDevice(String)} to create one representing a device
 * of a known MAC address (which you can get through device discovery with
 * {@link BluetoothAdapter}) or get one from the set of bonded devices
 * returned by {@link BluetoothAdapter#getBondedDevices()
 * BluetoothAdapter.getBondedDevices()}. You can then open a
 * {@link BluetoothSocket} for communication with the remote device, using
 * {@link #createRfcommSocketToServiceRecord(UUID)} over Bluetooth BR/EDR or using
 * {@link #createL2capChannel(int)} over Bluetooth LE.
 *
 * <div class="special reference">
 * <h3>Developer Guides</h3>
 * <p>
 * For more information about using Bluetooth, read the <a href=
 * "{@docRoot}guide/topics/connectivity/bluetooth.html">Bluetooth</a> developer
 * guide.
 * </p>
 * </div>
 *
 * {@see BluetoothAdapter}
 * {@see BluetoothSocket}
 *
 * @hide
 */
class BluetoothDevice internal constructor(private val fwkDevice: FwkBluetoothDevice) : Bundleable {
    companion object {
        /**
         * Broadcast Action: Indicates a low level (ACL) connection has been
         * established with a remote device.
         *
         * Always contains the extra fields [.EXTRA_DEVICE] and [.EXTRA_TRANSPORT].
         *
         * ACL connections are managed automatically by the Android Bluetooth
         * stack.
         *
         * For apps targeting Build.VERSION_CODES#R or lower, this requires the
         * Manifest.permission#BLUETOOTH permission which can be gained with a simple
         * <uses-permission> manifest tag.
         * For apps targeting Build.VERSION_CODES#S or or higher, this requires the
         * Manifest.permission#BLUETOOTH_CONNECT permission which can be gained with
         * Activity.requestPermissions(String[], int).
         */
        @RequiresPermission(
            anyOf = ["android.permission.BLUETOOTH",
                "android.permission.BLUETOOTH_CONNECT"]
        )
        const val ACTION_ACL_CONNECTED = FwkBluetoothDevice.ACTION_ACL_CONNECTED

        /**
         * Broadcast Action: Indicates that a low level (ACL) disconnection has
         * been requested for a remote device, and it will soon be disconnected.
         *
         * This is useful for graceful disconnection. Applications should use
         * this intent as a hint to immediately terminate higher level connections
         * (RFCOMM, L2CAP, or profile connections) to the remote device.
         *
         * Always contains the extra field [.EXTRA_DEVICE].
         * For apps targeting Build.VERSION_CODES#R or lower, this requires the
         * Manifest.permission#BLUETOOTH permission which can be gained with a simple
         * <uses-permission> manifest tag.
         * For apps targeting Build.VERSION_CODES#S or or higher, this requires the
         * Manifest.permission#BLUETOOTH_CONNECT permission which can be gained with
         * Activity.requestPermissions(String[], int).
         */
        @RequiresPermission(
            anyOf = ["android.permission.BLUETOOTH",
                "android.permission.BLUETOOTH_CONNECT"]
        )
        const val ACTION_ACL_DISCONNECT_REQUESTED =
            FwkBluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED

        /**
         * Broadcast Action: Indicates a low level (ACL) disconnection from a
         * remote device.
         *
         * Always contains the extra fields [.EXTRA_DEVICE] and [.EXTRA_TRANSPORT].
         *
         * ACL connections are managed automatically by the Android Bluetooth
         * stack.
         * For apps targeting Build.VERSION_CODES#R or lower, this requires the
         * Manifest.permission#BLUETOOTH permission which can be gained with a simple
         * <uses-permission> manifest tag.
         * For apps targeting Build.VERSION_CODES#S or or higher, this requires the
         * Manifest.permission#BLUETOOTH_CONNECT permission which can be gained with
         * Activity.requestPermissions(String[], int).
         */
        @RequiresPermission(
            anyOf = ["android.permission.BLUETOOTH",
                "android.permission.BLUETOOTH_CONNECT"]
        )
        const val ACTION_ACL_DISCONNECTED = FwkBluetoothDevice.ACTION_ACL_DISCONNECTED

        /**
         * Broadcast Action: Indicates the alias of a remote device has been
         * changed.
         *
         * Always contains the extra field [.EXTRA_DEVICE].
         * For apps targeting Build.VERSION_CODES#R or lower, this requires the
         * Manifest.permission#BLUETOOTH permission which can be gained with a simple
         * <uses-permission> manifest tag.
         * For apps targeting Build.VERSION_CODES#S or or higher, this requires the
         * Manifest.permission#BLUETOOTH_CONNECT permission which can be gained with
         * Activity.requestPermissions(String[], int).
         */
        @RequiresPermission(
            anyOf = ["android.permission.BLUETOOTH",
                "android.permission.BLUETOOTH_CONNECT"]
        )
        const val ACTION_ALIAS_CHANGED = FwkBluetoothDevice.ACTION_ALIAS_CHANGED

        /**
         * Broadcast Action: Indicates a change in the bond state of a remote
         * device. For example, if a device is bonded (paired).
         * <p>Always contains the extra fields {@link #EXTRA_DEVICE}, {@link
         * #EXTRA_BOND_STATE} and {@link #EXTRA_PREVIOUS_BOND_STATE}.
         *
         * For apps targeting Build.VERSION_CODES#R or lower, this requires the
         * Manifest.permission#BLUETOOTH permission which can be gained with a simple
         * <uses-permission> manifest tag.
         * For apps targeting Build.VERSION_CODES#S or or higher, this requires the
         * Manifest.permission#BLUETOOTH_CONNECT permission which can be gained with
         * Activity.requestPermissions(String[], int).
         */
        @RequiresPermission(
            anyOf = ["android.permission.BLUETOOTH",
                "android.permission.BLUETOOTH_CONNECT"]
        )
        const val ACTION_BOND_STATE_CHANGED = FwkBluetoothDevice.ACTION_BOND_STATE_CHANGED

        /**
         * Broadcast Action: Bluetooth class of a remote device has changed.
         * <p>Always contains the extra fields {@link #EXTRA_DEVICE} and {@link
         * #EXTRA_CLASS}.
         * {@see BluetoothClass}
         *
         * For apps targeting Build.VERSION_CODES#R or lower, this requires the
         * Manifest.permission#BLUETOOTH permission which can be gained with a simple
         * <uses-permission> manifest tag.
         * For apps targeting Build.VERSION_CODES#S or or higher, this requires the
         * Manifest.permission#BLUETOOTH_CONNECT permission which can be gained with
         * Activity.requestPermissions(String[], int).
         */
        @RequiresPermission(
            anyOf = ["android.permission.BLUETOOTH",
                "android.permission.BLUETOOTH_CONNECT"]
        )
        const val ACTION_CLASS_CHANGED = FwkBluetoothDevice.ACTION_CLASS_CHANGED

        /**
         * Broadcast Action: Remote device discovered.
         * <p>Sent when a remote device is found during discovery.
         * <p>Always contains the extra fields {@link #EXTRA_DEVICE} and {@link
         * #EXTRA_CLASS}. Can contain the extra fields {@link #EXTRA_NAME} and/or
         * {@link #EXTRA_RSSI} and/or {@link #EXTRA_IS_COORDINATED_SET_MEMBER} if they are available
         *
         * For apps targeting Build.VERSION_CODES#R or lower, this requires the
         * Manifest.permission#BLUETOOTH permission which can be gained with a simple
         * <uses-permission> manifest tag.
         * For apps targeting Build.VERSION_CODES#S or or higher, this requires the
         * Manifest.permission#BLUETOOTH_SCAN permission which can be gained with
         * Activity.requestPermissions(String[], int).
         *
         * In addition, this requires either the Manifest.permission#ACCESS_FINE_LOCATION permission
         * or a strong assertion that you will never derive the physical location of the device. You
         * can make this assertion by declaring usesPermissionFlags="neverForLocation" on the
         * relevant <uses-permission> manifest tag, but it may restrict the types of Bluetooth
         * devices you can interact with.
         */
        @RequiresPermission(
            anyOf = ["android.permission.BLUETOOTH",
                "android.permission.BLUETOOTH_SCAN"]
        )
        const val ACTION_FOUND = FwkBluetoothDevice.ACTION_FOUND

        /**
         * Broadcast Action: Indicates the friendly name of a remote device has
         * been retrieved for the first time, or changed since the last retrieval.
         * <p>Always contains the extra fields {@link #EXTRA_DEVICE} and {@link
         * #EXTRA_NAME}.
         *
         * For apps targeting Build.VERSION_CODES#R or lower, this requires the
         * Manifest.permission#BLUETOOTH permission which can be gained with a simple
         * <uses-permission> manifest tag.
         * For apps targeting Build.VERSION_CODES#S or or higher, this requires the
         * Manifest.permission#BLUETOOTH_CONNECT permission which can be gained with
         * Activity.requestPermissions(String[], int).
         */
        @RequiresPermission(
            anyOf = ["android.permission.BLUETOOTH",
                "android.permission.BLUETOOTH_CONNECT"]
        )
        const val ACTION_NAME_CHANGED = FwkBluetoothDevice.ACTION_NAME_CHANGED

        /**
         * Broadcast Action: This intent is used to broadcast PAIRING REQUEST
         *
         * For apps targeting Build.VERSION_CODES#R or lower, this requires the
         * Manifest.permission#BLUETOOTH_ADMIN permission which can be gained with a simple
         * <uses-permission> manifest tag.
         * For apps targeting Build.VERSION_CODES#S or or higher, this requires the
         * Manifest.permission#BLUETOOTH_CONNECT permission which can be gained with
         * Activity.requestPermissions(String[], int).
         */
        @RequiresPermission(
            anyOf = ["android.permission.BLUETOOTH",
                "android.permission.BLUETOOTH_CONNECT"]
        )
        const val ACTION_PAIRING_REQUEST = FwkBluetoothDevice.ACTION_PAIRING_REQUEST

        /**
         * Broadcast Action: This intent is used to broadcast the {@link UUID}
         * wrapped as a {@link android.os.ParcelUuid} of the remote device after it
         * has been fetched. This intent is sent only when the UUIDs of the remote
         * device are requested to be fetched using Service Discovery Protocol
         * <p> Always contains the extra field {@link #EXTRA_DEVICE}
         * <p> Always contains the extra field {@link #EXTRA_UUID}
         *
         * For apps targeting Build.VERSION_CODES#R or lower, this requires the
         * Manifest.permission#BLUETOOTH_ADMIN permission which can be gained with a simple
         * <uses-permission> manifest tag.
         * For apps targeting Build.VERSION_CODES#S or or higher, this requires the
         * Manifest.permission#BLUETOOTH_CONNECT permission which can be gained with
         * Activity.requestPermissions(String[], int).
         */
        @RequiresPermission(
            anyOf = ["android.permission.BLUETOOTH",
                "android.permission.BLUETOOTH_CONNECT"]
        )
        const val ACTION_UUID = FwkBluetoothDevice.ACTION_UUID

        /** Hardware MAC Address of the device */
        const val ADDRESS_TYPE_PUBLIC = FwkBluetoothDevice.ADDRESS_TYPE_PUBLIC

        /** Address is either resolvable, non-resolvable or static. */
        const val ADDRESS_TYPE_RANDOM = FwkBluetoothDevice.ADDRESS_TYPE_RANDOM

        /** Address type is unknown or unavailable **/
        const val ADDRESS_TYPE_UNKNOWN = FwkBluetoothDevice.ADDRESS_TYPE_UNKNOWN

        /**
         * Indicates the remote device is bonded (paired).
         * <p>A shared link keys exists locally for the remote device, so
         * communication can be authenticated and encrypted.
         * <p><i>Being bonded (paired) with a remote device does not necessarily
         * mean the device is currently connected. It just means that the pending
         * procedure was completed at some earlier time, and the link key is still
         * stored locally, ready to use on the next connection.
         * </i>
         */
        const val BOND_BONDED = FwkBluetoothDevice.BOND_BONDED

        /**
         * Indicates bonding (pairing) is in progress with the remote device.
         */
        const val BOND_BONDING = FwkBluetoothDevice.BOND_BONDING

        /**
         * Indicates the remote device is not bonded (paired).
         * <p>There is no shared link key with the remote device, so communication
         * (if it is allowed at all) will be unauthenticated and unencrypted.
         */
        const val BOND_NONE = FwkBluetoothDevice.BOND_NONE

        /**
         * Bluetooth device type, Classic - BR/EDR devices
         */
        const val DEVICE_TYPE_CLASSIC = FwkBluetoothDevice.DEVICE_TYPE_CLASSIC

        /**
         * Bluetooth device type, Dual Mode - BR/EDR/LE
         */
        const val DEVICE_TYPE_DUAL = FwkBluetoothDevice.DEVICE_TYPE_DUAL

        /**
         * Bluetooth device type, Low Energy - LE-only
         */
        const val DEVICE_TYPE_LE = FwkBluetoothDevice.DEVICE_TYPE_LE

        /**
         * Bluetooth device type, Unknown
         */
        const val DEVICE_TYPE_UNKNOWN = FwkBluetoothDevice.DEVICE_TYPE_UNKNOWN

        /**
         * Sentinel error value for this class. Guaranteed to not equal any other
         * integer constant in this class. Provided as a convenience for functions
         * that require a sentinel error value, for example:
         * <p><code>Intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,
         * BluetoothDevice.ERROR)</code>
         */
        const val ERROR = FwkBluetoothDevice.ERROR

        /**
         * Used as an int extra field in {@link #ACTION_BOND_STATE_CHANGED} intents.
         * Contains the bond state of the remote device.
         * <p>Possible values are:
         * {@link #BOND_NONE},
         * {@link #BOND_BONDING},
         * {@link #BOND_BONDED}.
         */
        const val EXTRA_BOND_STATE = FwkBluetoothDevice.EXTRA_BOND_STATE

        /**
         * Used as a Parcelable {@link BluetoothClass} extra field in {@link
         * #ACTION_FOUND} and {@link #ACTION_CLASS_CHANGED} intents.
         */
        const val EXTRA_CLASS = FwkBluetoothDevice.EXTRA_CLASS

        /**
         * Used as a Parcelable {@link BluetoothDevice} extra field in every intent
         * broadcast by this class. It contains the framework's BluetoothDevice that
         * the intent applies to.
         */
        const val EXTRA_DEVICE = FwkBluetoothDevice.EXTRA_DEVICE

        /**
         * Used as an bool extra field in {@link #ACTION_FOUND} intents.
         * It contains the information if device is discovered as member of a coordinated set or
         * not. Pairing with device that belongs to a set would trigger pairing with the rest of set
         * members.
         * See Bluetooth CSIP specification for more details.
         */
        const val EXTRA_IS_COORDINATED_SET_MEMBER =
            FwkBluetoothDevice.EXTRA_IS_COORDINATED_SET_MEMBER

        /**
         * Used as a String extra field in {@link #ACTION_NAME_CHANGED} and {@link
         * #ACTION_FOUND} intents. It contains the friendly Bluetooth name.
         */
        const val EXTRA_NAME = FwkBluetoothDevice.EXTRA_NAME

        /**
         * Used as an int extra field in {@link #ACTION_PAIRING_REQUEST}
         * intents as the value of passkey.
         * The Bluetooth Passkey is a 6-digit numerical value represented as integer value
         * in the range 0x00000000 – 0x000F423F (000000 to 999999).
         */
        const val EXTRA_PAIRING_KEY = FwkBluetoothDevice.EXTRA_PAIRING_KEY

        /**
         * Used as an int extra field in {@link #ACTION_PAIRING_REQUEST}
         * intents to indicate pairing method used. Possible values are:
         * {@link #PAIRING_VARIANT_PIN},
         * {@link #PAIRING_VARIANT_PASSKEY_CONFIRMATION},
         */
        const val EXTRA_PAIRING_VARIANT = FwkBluetoothDevice.EXTRA_PAIRING_VARIANT

        /**
         * Used as an int extra field in {@link #ACTION_BOND_STATE_CHANGED} intents.
         * Contains the previous bond state of the remote device.
         * <p>Possible values are:
         * {@link #BOND_NONE},
         * {@link #BOND_BONDING},
         * {@link #BOND_BONDED}.
         */
        const val EXTRA_PREVIOUS_BOND_STATE = FwkBluetoothDevice.EXTRA_PREVIOUS_BOND_STATE

        /**
         * Used as an optional short extra field in {@link #ACTION_FOUND} intents.
         * Contains the RSSI value of the remote device as reported by the
         * Bluetooth hardware.
         */
        const val EXTRA_RSSI = FwkBluetoothDevice.EXTRA_RSSI

        /**
         * Used as an int extra field in {@link #ACTION_ACL_CONNECTED} and
         * {@link #ACTION_ACL_DISCONNECTED} intents to indicate which transport is connected.
         * Possible values are: {@link #TRANSPORT_BREDR} and {@link #TRANSPORT_LE}.
         */
        const val EXTRA_TRANSPORT = FwkBluetoothDevice.EXTRA_TRANSPORT

        /**
         * Used as an extra field in {@link #ACTION_UUID} intents,
         * Contains the {@link android.os.ParcelUuid}s of the remote device which
         * is a parcelable version of {@link UUID}.
         */
        const val EXTRA_UUID = FwkBluetoothDevice.EXTRA_UUID

        /**
         * The user will be prompted to confirm the passkey displayed on the screen or
         * an app will confirm the passkey for the user.
         */
        const val PAIRING_VARIANT_PASSKEY_CONFIRMATION =
            FwkBluetoothDevice.PAIRING_VARIANT_PASSKEY_CONFIRMATION

        /**
         * The user will be prompted to enter a pin or
         * an app will enter a pin for user.
         */
        const val PAIRING_VARIANT_PIN = FwkBluetoothDevice.PAIRING_VARIANT_PIN

        /**
         * Bluetooth LE 1M PHY. Used to refer to LE 1M Physical Channel for advertising, scanning or
         * connection.
         */
        const val PHY_LE_1M = FwkBluetoothDevice.PHY_LE_1M

        /**
         * Bluetooth LE 1M PHY mask. Used to specify LE 1M Physical Channel as one of many available
         * options in a bitmask.
         */
        const val PHY_LE_1M_MASK = FwkBluetoothDevice.PHY_LE_1M_MASK

        /**
         * Bluetooth LE 2M PHY. Used to refer to LE 2M Physical Channel for advertising, scanning or
         * connection.
         */
        const val PHY_LE_2M = FwkBluetoothDevice.PHY_LE_2M

        /**
         * Bluetooth LE 2M PHY mask. Used to specify LE 2M Physical Channel as one of many available
         * options in a bitmask.
         */
        const val PHY_LE_2M_MASK = FwkBluetoothDevice.PHY_LE_2M_MASK

        /**
         * Bluetooth LE Coded PHY. Used to refer to LE Coded Physical Channel for advertising,
         * scanning or connection.
         */
        const val PHY_LE_CODED = FwkBluetoothDevice.PHY_LE_CODED

        /**
         * Bluetooth LE Coded PHY mask. Used to specify LE Coded Physical Channel as one of many
         * available options in a bitmask.
         */
        const val PHY_LE_CODED_MASK = FwkBluetoothDevice.PHY_LE_CODED_MASK

        /**
         * No preferred coding when transmitting on the LE Coded PHY.
         */
        const val PHY_OPTION_NO_PREFERRED = FwkBluetoothDevice.PHY_OPTION_NO_PREFERRED

        /**
         * Prefer the S=2 coding to be used when transmitting on the LE Coded PHY.
         */
        const val PHY_OPTION_S2 = FwkBluetoothDevice.PHY_OPTION_S2

        /**
         * Prefer the S=8 coding to be used when transmitting on the LE Coded PHY.
         */
        const val PHY_OPTION_S8 = FwkBluetoothDevice.PHY_OPTION_S8

        /**
         * No preference of physical transport for GATT connections to remote dual-mode devices
         */
        const val TRANSPORT_AUTO = FwkBluetoothDevice.TRANSPORT_AUTO

        /**
         * Constant representing the BR/EDR transport.
         */
        const val TRANSPORT_BREDR = FwkBluetoothDevice.TRANSPORT_BREDR

        /**
         * Constant representing the Bluetooth Low Energy (BLE) Transport.
         */
        const val TRANSPORT_LE = FwkBluetoothDevice.TRANSPORT_LE
        internal fun keyForField(field: Int): String {
            return field.toString(Character.MAX_RADIX)
        }

        val CREATOR: Bundleable.Creator<BluetoothDevice> =
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) BluetoothDeviceImplBase.CREATOR
            else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) BluetoothDeviceImplApi26.CREATOR
            else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                BluetoothDeviceImplApi29.CREATOR
            } else BluetoothDeviceImplApi33.CREATOR
    }

    private val impl: BluetoothDeviceImpl =
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) BluetoothDeviceImplBase(fwkDevice)
        else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) BluetoothDeviceImplApi26(fwkDevice)
        else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            BluetoothDeviceImplApi29(fwkDevice)
        } else BluetoothDeviceImplApi33(fwkDevice)

    /**
     * Returns the hardware address of this BluetoothDevice.
     * <p> For example, "00:11:22:AA:BB:CC".
     *
     * @return Bluetooth hardware address as string
     */
    @get:RequiresPermission(
        anyOf = ["android.permission.BLUETOOTH_CONNECT",
            "android.permission.BLUETOOTH"]
    )
    val address: String
        get() = impl.address

    @get:RequiresPermission(
        anyOf = ["android.permission.BLUETOOTH_CONNECT",
            "android.permission.BLUETOOTH"]
    )
    @set:RequiresPermission(
        anyOf = ["android.permission.BLUETOOTH_CONNECT",
            "android.permission.BLUETOOTH", "android.permission.BLUETOOTH_PRIVILEGED"]
    )
    var alias: String?
        /**
         * Get the locally modifiable name (alias) of the remote Bluetooth device.
         *
         * @return the Bluetooth alias, the friendly device name if no alias, or
         * null if there was a problem
         */
        get() = impl.alias
        /**
         * Sets the locally modifiable name (alias) of the remote Bluetooth device. This method
         * overwrites the previously stored alias. The new alias is saved in local
         * storage so that the change is preserved over power cycles.
         *
         * <p>From SDK 33, This method requires the calling app to be associated with Companion
         * Device Manager (see{@link android.companion.CompanionDeviceManager#associate(
         * AssociationRequest,android.companion.CompanionDeviceManager.Callback, Handler)}) and have the
         * {@link android.Manifest.permission#BLUETOOTH_CONNECT} permission. Alternatively, if the
         * caller has the {@link android.Manifest.permission#BLUETOOTH_PRIVILEGED} permission, they can
         * bypass the Companion Device Manager association requirement as well as other permission
         * requirements.
         *
         * @param alias is the new locally modifiable name for the remote Bluetooth device which must
         *              be the empty string. If null, we clear the alias.
         * @return whether the alias was successfully changed
         * @throws IllegalArgumentException if the alias is the empty string
         */
        set(alias) {
            impl.alias = alias
        }

    /**
     * Sets the locally modifiable name (alias) of the remote Bluetooth device. This method
     * overwrites the previously stored alias. The new alias is saved in local
     * storage so that the change is preserved over power cycles.
     *
     * <p>From SDK 33, This method requires the calling app to be associated with Companion
     * Device Manager (see{@link android.companion.CompanionDeviceManager#associate(
     * AssociationRequest,android.companion.CompanionDeviceManager.Callback, Handler)}) and have the
     * {@link android.Manifest.permission#BLUETOOTH_CONNECT} permission. Alternatively, if the
     * caller has the {@link android.Manifest.permission#BLUETOOTH_PRIVILEGED} permission, they can
     * bypass the Companion Device Manager association requirement as well as other permission
     * requirements.
     *
     * @param alias is the new locally modifiable name for the remote Bluetooth device which must
     *              be the empty string. If null, we clear the alias.
     * @return whether the alias was successfully changed
     * @throws IllegalArgumentException if the alias is the empty string
     */
    @RequiresPermission(
        anyOf = ["android.permission.BLUETOOTH_CONNECT",
            "android.permission.BLUETOOTH", "android.permission.BLUETOOTH_PRIVILEGED"]
    )
    fun setAlias(name: String?): Int {
        return impl.setAlias(name)
    }

    // TODO: change to framework's BluetoothClass
    /**
     * Get the Bluetooth class of the remote device.
     *
     * @return Bluetooth class object, or null on error
     */
    @get:RequiresPermission(
        anyOf = ["android.permission.BLUETOOTH_CONNECT",
            "android.permission.BLUETOOTH"]
    )
    val bluetoothClass: BluetoothClass?
        get() = impl.bluetoothClass

    /**
     * Get the bond state of the remote device.
     * <p>Possible values for the bond state are:
     * {@link #BOND_NONE},
     * {@link #BOND_BONDING},
     * {@link #BOND_BONDED}.
     *
     * @return the bond state
     */
    @get:RequiresPermission(
        anyOf = ["android.permission.BLUETOOTH_CONNECT",
            "android.permission.BLUETOOTH"]
    )
    val bondState: Int
        get() = impl.bondState

    /**
     * Get the friendly Bluetooth name of the remote device.
     *
     * <p>The local adapter will automatically retrieve remote names when
     * performing a device scan, and will cache them. This method just returns
     * the name for this device from the cache.
     *
     * @return the Bluetooth name, or null if there was a problem.
     */
    @get:RequiresPermission(
        anyOf = ["android.permission.BLUETOOTH_CONNECT",
            "android.permission.BLUETOOTH"]
    )
    val name: String?
        get() = impl.name

    /**
     * Get the Bluetooth device type of the remote device.
     *
     * @return the device type {@link #DEVICE_TYPE_CLASSIC}, {@link #DEVICE_TYPE_LE} {@link
     * #DEVICE_TYPE_DUAL}. {@link #DEVICE_TYPE_UNKNOWN} if it's not available
     */
    @get:RequiresPermission(
        anyOf = ["android.permission.BLUETOOTH_CONNECT",
            "android.permission.BLUETOOTH"]
    )
    val type: Int
        get() = impl.type

    /**
     * Returns the supported features (UUIDs) of the remote device.
     *
     * <p>This method does not start a service discovery procedure to retrieve the UUIDs
     * from the remote device. Instead, the local cached copy of the service
     * UUIDs are returned.
     * <p>Use {@link #fetchUuidsWithSdp} if fresh UUIDs are desired.
     *
     * @return the supported features (UUIDs) of the remote device, or null on error
     */
    @get:RequiresPermission(
        anyOf = ["android.permission.BLUETOOTH_CONNECT",
            "android.permission.BLUETOOTH"]
    )
    val uuids: Array<ParcelUuid>?
        get() = impl.uuids

    /**
     * Confirm passkey for {@link #PAIRING_VARIANT_PASSKEY_CONFIRMATION} pairing.
     *
     * @return true confirmation has been sent out false for error
     */
    @RequiresPermission(
        anyOf = ["android.permission.BLUETOOTH_CONNECT",
            "android.permission.BLUETOOTH_PRIVILEGED"]
    )
    fun setPairingConfirmation(confirm: Boolean): Boolean {
        return impl.setPairingConfirmation(confirm)
    }

    /**
     * Set the pin during pairing when the pairing method is {@link #PAIRING_VARIANT_PIN}
     *
     * @return true pin has been set false for error
     */
    @RequiresPermission(
        anyOf = ["android.permission.BLUETOOTH_CONNECT",
            "android.permission.BLUETOOTH_ADMIN"]
    )
    fun setPin(pin: ByteArray): Boolean {
        return impl.setPin(pin)
    }

    /**
     * Connect to GATT Server hosted by this device. Caller acts as GATT client.
     * The callback is used to deliver results to Caller, such as connection status as well
     * as any further GATT client operations.
     * The method returns a BluetoothGatt instance. You can use BluetoothGatt to conduct
     * GATT client operations.
     *
     * @param callback GATT callback handler that will receive asynchronous callbacks.
     * @param autoConnect Whether to directly connect to the remote device (false) or to
     * automatically connect as soon as the remote device becomes available (true).
     * @param transport preferred transport for GATT connections to remote dual-mode devices {@link
     * BluetoothDevice#TRANSPORT_AUTO} or {@link BluetoothDevice#TRANSPORT_BREDR} or {@link
     * BluetoothDevice#TRANSPORT_LE}
     * @param phy preferred PHY for connections to remote LE device. Bitwise OR of any of {@link
     * BluetoothDevice#PHY_LE_1M_MASK}, {@link BluetoothDevice#PHY_LE_2M_MASK}, an d{@link
     * BluetoothDevice#PHY_LE_CODED_MASK}. This option does not take effect if {@code autoConnect}
     * is set to true.
     * @param handler The handler to use for the callback. If {@code null}, callbacks will happen on
     * an un-specified background thread.
     * @throws NullPointerException if callback is null
     *
     * For apps targeting Build.VERSION_CODES#S or or higher, this requires the
     * Manifest.permission#BLUETOOTH_CONNECT permission which can be gained with
     * Activity.requestPermissions(String[], int).
     */
    // TODO: Change into lib's BluetoothGatt & BluetoothGattCallback
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun connectGatt(
        context: Context,
        autoConnect: Boolean,
        callback: FwkBluetoothGattCallback,
        transport: Int = TRANSPORT_AUTO,
        phy: Int = PHY_LE_1M_MASK,
        handler: Handler? = null
    ): FwkBluetoothGatt? {
        return impl.connectGatt(context, autoConnect, callback, transport, phy, handler)
    }

    /**
     * Start the bonding (pairing) process with the remote device.
     *
     * This is an asynchronous call, it will return immediately. Register
     * for [.ACTION_BOND_STATE_CHANGED] intents to be notified when
     * the bonding process completes, and its result.
     *
     * Android system services will handle the necessary user interactions
     * to confirm and complete the bonding process.
     *
     * @return false on immediate error, true if bonding will begin
     *
     * For apps targeting Build.VERSION_CODES#R or lower, this requires the
     * Manifest.permission#BLUETOOTH permission which can be gained with a simple
     * <uses-permission> manifest tag.
     * For apps targeting Build.VERSION_CODES#S or or higher, this requires the
     * Manifest.permission#BLUETOOTH_CONNECT permission which can be gained with
     * Activity.requestPermissions(String[], int).
     */
    @RequiresPermission(
        anyOf = ["android.permission.BLUETOOTH_CONNECT",
            "android.permission.BLUETOOTH"]
    )
    fun createBond(): Boolean {
        return impl.createBond()
    }

    /**
     * Create a Bluetooth L2CAP Connection-oriented Channel (CoC) {@link BluetoothSocket} that can
     * be used to start a secure outgoing connection to the remote device with the same dynamic
     * protocol/service multiplexer (PSM) value. The supported Bluetooth transport is LE only.
     * <p>This is designed to be used with {@link
     * BluetoothAdapter#listenUsingInsecureL2capChannel()} for peer-peer Bluetooth applications.
     * <p>Use {@link BluetoothSocket#connect} to initiate the outgoing connection.
     * <p>Application using this API is responsible for obtaining PSM value from remote device.
     * <p> The communication channel may not have an authenticated link key, i.e. it may be subject
     * to person-in-the-middle attacks. Use {@link #createL2capChannel(int)} if an encrypted and
     * authenticated communication channel is possible.
     *
     * @param psm dynamic PSM value from remote device
     * @return a CoC #BluetoothSocket ready for an outgoing connection
     * @throws IOException on error, for example Bluetooth not available, or insufficient
     * permissions
     *
     * For apps targeting Build.VERSION_CODES#R or lower, this requires the
     * Manifest.permission#BLUETOOTH permission which can be gained with a simple
     * <uses-permission> manifest tag.
     * For apps targeting Build.VERSION_CODES#S or or higher, this requires the
     * Manifest.permission#BLUETOOTH_CONNECT permission which can be gained with
     * Activity.requestPermissions(String[], int).
     */
    @RequiresPermission(
        anyOf = ["android.permission.BLUETOOTH_CONNECT",
            "android.permission.BLUETOOTH"]
    )
    fun createInsecureL2capChannel(psm: Int): FwkBluetoothSocket {
        return impl.createInsecureL2capChannel(psm)
    }

    /**
     * Create an RFCOMM {@link BluetoothSocket} socket ready to start an insecure
     * outgoing connection to this remote device using SDP lookup of uuid.
     * <p> The communication channel will not have an authenticated link key
     * i.e. it will be subject to person-in-the-middle attacks. For Bluetooth 2.1
     * devices, the link key will be encrypted, as encryption is mandatory.
     * For legacy devices (pre Bluetooth 2.1 devices) the link key will
     * be not be encrypted. Use {@link #createRfcommSocketToServiceRecord} if an
     * encrypted and authenticated communication channel is desired.
     * <p>This is designed to be used with {@link
     * BluetoothAdapter#listenUsingInsecureRfcommWithServiceRecord} for peer-peer
     * Bluetooth applications.
     * <p>Use {@link BluetoothSocket#connect} to initiate the outgoing
     * connection. This will also perform an SDP lookup of the given uuid to
     * determine which channel to connect to.
     * <p>The remote device will be authenticated and communication on this
     * socket will be encrypted.
     * <p>Hint: If you are connecting to a Bluetooth serial board then try
     * using the well-known SPP UUID 00001101-0000-1000-8000-00805F9B34FB.
     * However if you are connecting to an Android peer then please generate
     * your own unique UUID.
     *
     * @param uuid service record uuid to lookup RFCOMM channel
     * @return a RFCOMM BluetoothServerSocket ready for an outgoing connection
     * @throws IOException on error, for example Bluetooth not available, or insufficient
     * permissions
     *
     * For apps targeting Build.VERSION_CODES#R or lower, this requires the
     * Manifest.permission#BLUETOOTH permission which can be gained with a simple
     * <uses-permission> manifest tag.
     * For apps targeting Build.VERSION_CODES#S or or higher, this requires the
     * Manifest.permission#BLUETOOTH_CONNECT permission which can be gained with
     * Activity.requestPermissions(String[], int).
     */
    @RequiresPermission(
        anyOf = ["android.permission.BLUETOOTH_CONNECT",
            "android.permission.BLUETOOTH"]
    )
    fun createInsecureRfcommSocketToServiceRecord(uuid: UUID): FwkBluetoothSocket {
        return impl.createInsecureRfcommSocketToServiceRecord(uuid)
    }

    /**
     * Create a Bluetooth L2CAP Connection-oriented Channel (CoC) [BluetoothSocket] that can
     * be used to start a secure outgoing connection to the remote device with the same dynamic
     * protocol/service multiplexer (PSM) value. The supported Bluetooth transport is LE only.
     *
     * This is designed to be used with [BluetoothAdapter.listenUsingL2capChannel] for
     * peer-peer Bluetooth applications.
     *
     * Use [BluetoothSocket.connect] to initiate the outgoing connection.
     *
     * Application using this API is responsible for obtaining PSM value from remote device.
     *
     * The remote device will be authenticated and communication on this socket will be
     * encrypted.
     *
     *  Use this socket if an authenticated socket link is possible. Authentication refers
     * to the authentication of the link key to prevent person-in-the-middle type of attacks.
     *
     * @param psm dynamic PSM value from remote device
     * @return a CoC #BluetoothSocket ready for an outgoing connection
     * @throws IOException on error, for example Bluetooth not available, or insufficient
     * permissions
     *
     * For apps targeting Build.VERSION_CODES#R or lower, this requires the
     * Manifest.permission#BLUETOOTH permission which can be gained with a simple
     * <uses-permission> manifest tag.
     * For apps targeting Build.VERSION_CODES#S or or higher, this requires the
     * Manifest.permission#BLUETOOTH_CONNECT permission which can be gained with
     * Activity.requestPermissions(String[], int).
     */
    @RequiresPermission(
        anyOf = ["android.permission.BLUETOOTH_CONNECT",
            "android.permission.BLUETOOTH"]
    )

    fun createL2capChannel(psm: Int): FwkBluetoothSocket {
        return impl.createL2capChannel(psm)
    }

    /**
     * Create an RFCOMM [BluetoothSocket] ready to start a secure
     * outgoing connection to this remote device using SDP lookup of uuid.
     *
     * This is designed to be used with [ ][BluetoothAdapter.listenUsingRfcommWithServiceRecord]
     * for peer-peer Bluetooth applications.
     *
     * Use [BluetoothSocket.connect] to initiate the outgoing
     * connection. This will also perform an SDP lookup of the given uuid to
     * determine which channel to connect to.
     *
     * The remote device will be authenticated and communication on this
     * socket will be encrypted.
     *
     *  Use this socket only if an authenticated socket link is possible.
     * Authentication refers to the authentication of the link key to
     * prevent person-in-the-middle type of attacks.
     * For example, for Bluetooth 2.1 devices, if any of the devices does not
     * have an input and output capability or just has the ability to
     * display a numeric key, a secure socket connection is not possible.
     * In such a case, use [.createInsecureRfcommSocketToServiceRecord].
     * For more details, refer to the Security Model section 5.2 (vol 3) of
     * Bluetooth Core Specification version 2.1 + EDR.
     *
     * Hint: If you are connecting to a Bluetooth serial board then try
     * using the well-known SPP UUID 00001101-0000-1000-8000-00805F9B34FB.
     * However if you are connecting to an Android peer then please generate
     * your own unique UUID.
     *
     * @param uuid service record uuid to lookup RFCOMM channel
     * @return a RFCOMM BluetoothServerSocket ready for an outgoing connection
     * @throws IOException on error, for example Bluetooth not available, or insufficient
     * permissions
     *
     * For apps targeting Build.VERSION_CODES#R or lower, this requires the
     * Manifest.permission#BLUETOOTH permission which can be gained with a simple
     * <uses-permission> manifest tag.
     * For apps targeting Build.VERSION_CODES#S or or higher, this requires the
     * Manifest.permission#BLUETOOTH_CONNECT permission which can be gained with
     * Activity.requestPermissions(String[], int).
     */
    @RequiresPermission(
        anyOf = ["android.permission.BLUETOOTH_CONNECT",
            "android.permission.BLUETOOTH"]
    )
    fun createRfcommSocketToServiceRecord(uuid: UUID): FwkBluetoothSocket {
        return impl.createRfcommSocketToServiceRecord(uuid)
    }

    /**
     * Perform a service discovery on the remote device to get the UUIDs supported.
     *
     * This API is asynchronous and [.ACTION_UUID] intent is sent,
     * with the UUIDs supported by the remote end. If there is an error
     * in getting the SDP records or if the process takes a long time, or the device is bonding and
     * we have its UUIDs cached, [.ACTION_UUID] intent is sent with the UUIDs that is
     * currently present in the cache. Clients should use the [.getUuids] to get UUIDs
     * if service discovery is not to be performed. If there is an ongoing bonding process,
     * service discovery or device inquiry, the request will be queued.
     *
     * @return False if the check fails, True if the process of initiating an ACL connection
     * to the remote device was started or cached UUIDs will be broadcast.
     *
     * For apps targeting Build.VERSION_CODES#R or lower, this requires the
     * Manifest.permission#BLUETOOTH permission which can be gained with a simple
     * <uses-permission> manifest tag.
     * For apps targeting Build.VERSION_CODES#S or or higher, this requires the
     * Manifest.permission#BLUETOOTH_CONNECT permission which can be gained with
     * Activity.requestPermissions(String[], int).
     */
    @RequiresPermission(
        anyOf = ["android.permission.BLUETOOTH_CONNECT",
            "android.permission.BLUETOOTH"]
    )
    fun fetchUuidsWithSdp(): Boolean {
        return impl.fetchUuidsWithSdp()
    }

    override fun toBundle(): Bundle {
        return impl.toBundle()
    }

    interface BluetoothDeviceImpl : Bundleable {
        fun connectGatt(
            context: Context,
            autoConnect: Boolean,
            callback: FwkBluetoothGattCallback,
            transport: Int = TRANSPORT_AUTO,
            phy: Int = PHY_LE_1M_MASK,
            handler: Handler? = null
        ): FwkBluetoothGatt?

        val address: String
        var alias: String?

        // TODO: change to framework's BluetoothClass
        val bluetoothClass: BluetoothClass?
        val bondState: Int
        val name: String?
        val type: Int
        val uuids: Array<ParcelUuid>?

        fun setAlias(name: String?): Int
        fun createBond(): Boolean
        fun createInsecureL2capChannel(psm: Int): FwkBluetoothSocket
        fun createInsecureRfcommSocketToServiceRecord(uuid: UUID): FwkBluetoothSocket
        fun createL2capChannel(psm: Int): FwkBluetoothSocket
        fun createRfcommSocketToServiceRecord(uuid: UUID): FwkBluetoothSocket
        fun fetchUuidsWithSdp(): Boolean
        fun setPairingConfirmation(confirm: Boolean): Boolean
        fun setPin(pin: ByteArray): Boolean
    }

    internal open class BluetoothDeviceImplBase(
        private val fwkDevice: android.bluetooth.BluetoothDevice
    ) : BluetoothDeviceImpl {
        companion object {
            internal const val FIELD_FWK_BLUETOOTH_DEVICE = 1

            val CREATOR: Bundleable.Creator<BluetoothDevice> =
                object : Bundleable.Creator<BluetoothDevice> {
                    override fun fromBundle(bundle: Bundle): BluetoothDevice {
                        val fwkDevice = Utils.getParcelableFromBundle(
                            bundle,
                            keyForField(BluetoothDeviceImplBase.FIELD_FWK_BLUETOOTH_DEVICE),
                            FwkBluetoothDevice::class.java
                        ) ?: throw IllegalArgumentException("No BluetoothDevice found in bundle")
                        return BluetoothDevice(fwkDevice)
                    }
                }
        }

        override val address: String
            get() = fwkDevice.address

        override var alias: String?
            get() {
                // If to pass presubmit
                return if (Build.VERSION.SDK_INT < 33) {
                    try {
                        // get/set alias can be access through reflection until SDK 27
                        // get/set alias is marked as @UnsupportedAppUsage from SDK 29
                        FwkBluetoothDevice::class.java.getDeclaredMethod(
                            "getAlias",
                        ).let {
                            it.isAccessible = true
                            it.invoke(fwkDevice)
                        } as String?
                    } catch (e: Exception) {
                        null
                    }
                } else null
            }
            set(value) {
                setAlias(value)
            }

        override fun setAlias(name: String?): Int {
            return if (Build.VERSION.SDK_INT < 33) {
                return try {
                    FwkBluetoothDevice::class.java.getDeclaredMethod(
                        "setAlias",
                        String::class.java
                    ).let {
                        it.isAccessible = true
                        it.invoke(fwkDevice, name)
                    } as Int
                } catch (_: Exception) {
                    BluetoothStatusCodes.ERROR_UNKNOWN
                }
            } else BluetoothStatusCodes.ERROR_UNKNOWN
        }

        // TODO: change to framework's BluetoothClass
        @get:RequiresPermission(
            anyOf = ["android.permission.BLUETOOTH_CONNECT",
                "android.permission.BLUETOOTH"]
        )
        override val bluetoothClass: BluetoothClass?
            get() = fwkDevice.bluetoothClass

        @get:RequiresPermission(
            anyOf = ["android.permission.BLUETOOTH_CONNECT",
                "android.permission.BLUETOOTH"]
        )
        override val bondState: Int
            get() = fwkDevice.bondState

        @get:RequiresPermission(
            anyOf = ["android.permission.BLUETOOTH_CONNECT",
                "android.permission.BLUETOOTH"]
        )
        override val name: String?
            get() = fwkDevice.name

        @get:RequiresPermission(
            anyOf = ["android.permission.BLUETOOTH_CONNECT",
                "android.permission.BLUETOOTH"]
        )
        override val type: Int
            get() = fwkDevice.type

        @get:RequiresPermission(
            anyOf = ["android.permission.BLUETOOTH_CONNECT",
                "android.permission.BLUETOOTH"]
        )
        override val uuids: Array<ParcelUuid>?
            get() = fwkDevice.uuids

        override fun connectGatt(
            context: Context,
            autoConnect: Boolean,
            callback: android.bluetooth.BluetoothGattCallback,
            transport: Int,
            phy: Int,
            handler: Handler?
        ): android.bluetooth.BluetoothGatt? {
            TODO("Support backward compatibility")
        }

        @RequiresPermission(
            anyOf = ["android.permission.BLUETOOTH_CONNECT",
                "android.permission.BLUETOOTH"]
        )
        override fun createBond(): Boolean {
            return fwkDevice.createBond()
        }

        override fun createInsecureL2capChannel(psm: Int): android.bluetooth.BluetoothSocket {
            TODO("Support backward compatibility")
        }

        @RequiresPermission(
            anyOf = ["android.permission.BLUETOOTH_CONNECT",
                "android.permission.BLUETOOTH"]
        )
        override fun createInsecureRfcommSocketToServiceRecord(uuid: UUID): FwkBluetoothSocket {
            return fwkDevice.createInsecureRfcommSocketToServiceRecord(uuid)
        }

        override fun createL2capChannel(psm: Int): android.bluetooth.BluetoothSocket {
            TODO("Support backward compatibility")
        }

        @RequiresPermission(
            anyOf = ["android.permission.BLUETOOTH_CONNECT",
                "android.permission.BLUETOOTH"]
        )
        override fun createRfcommSocketToServiceRecord(uuid: UUID): FwkBluetoothSocket {
            return fwkDevice.createRfcommSocketToServiceRecord(uuid)
        }

        @RequiresPermission(
            anyOf = ["android.permission.BLUETOOTH_CONNECT",
                "android.permission.BLUETOOTH"]
        )
        override fun fetchUuidsWithSdp(): Boolean {
            return fwkDevice.fetchUuidsWithSdp()
        }

        override fun toBundle(): Bundle {
            val bundle = Bundle()
            bundle.putParcelable(
                keyForField(BluetoothDeviceImplBase.FIELD_FWK_BLUETOOTH_DEVICE),
                fwkDevice
            )
            return bundle
        }

        @RequiresPermission(
            allOf = ["android.permission.BLUETOOTH_CONNECT",
                "android.permission.BLUETOOTH_PRIVILEGED"]
        )
        override fun setPairingConfirmation(confirm: Boolean): Boolean {
            return fwkDevice.setPairingConfirmation(confirm)
        }

        @RequiresPermission(
            anyOf = ["android.permission.BLUETOOTH_CONNECT",
                "android.permission.BLUETOOTH_ADMIN"]
        )
        override fun setPin(pin: ByteArray): Boolean {
            return fwkDevice.setPin(pin)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    internal open class BluetoothDeviceImplApi26(
        private val fwkDevice: android.bluetooth.BluetoothDevice
    ) : BluetoothDeviceImplBase(fwkDevice) {
        companion object {
            internal const val FIELD_FWK_BLUETOOTH_DEVICE = 1

            val CREATOR = BluetoothDeviceImplBase.CREATOR
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun connectGatt(
            context: Context,
            autoConnect: Boolean,
            callback: android.bluetooth.BluetoothGattCallback,
            transport: Int,
            phy: Int,
            handler: Handler?
        ): android.bluetooth.BluetoothGatt? {
            return fwkDevice.connectGatt(context, autoConnect, callback, transport, phy, handler)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    internal open class BluetoothDeviceImplApi29(
        private val fwkDevice: android.bluetooth.BluetoothDevice
    ) :
        BluetoothDeviceImplApi26(fwkDevice) {
        companion object {
            val CREATOR = BluetoothDeviceImplApi26.CREATOR
        }

        @RequiresPermission(
            anyOf = ["android.permission.BLUETOOTH_CONNECT",
                "android.permission.BLUETOOTH"]
        )
        override fun createInsecureL2capChannel(psm: Int): android.bluetooth.BluetoothSocket {
            return fwkDevice.createInsecureL2capChannel(psm)
        }

        @RequiresPermission(
            anyOf = ["android.permission.BLUETOOTH_CONNECT",
                "android.permission.BLUETOOTH"]
        )
        override fun createL2capChannel(psm: Int): FwkBluetoothSocket {
            return fwkDevice.createL2capChannel(psm)
        }
    }
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    internal open class BluetoothDeviceImplApi33(
        private val fwkDevice: android.bluetooth.BluetoothDevice
    ) :
        BluetoothDeviceImplApi29(fwkDevice) {
        companion object {
            val CREATOR = BluetoothDeviceImplApi29.CREATOR
        }

        override val address: String
            get() = fwkDevice.address

        @get:RequiresPermission(
            anyOf = ["android.permission.BLUETOOTH_CONNECT",
                "android.permission.BLUETOOTH"]
        )
        @set:RequiresPermission(
            anyOf = ["android.permission.BLUETOOTH_CONNECT",
                "android.permission.BLUETOOTH", "android.permission.BLUETOOTH_PRIVILEGED"]
        )
        override var alias: String?
            get() {
                return fwkDevice.alias
            }
            set(value) {
                setAlias(value)
            }

        @RequiresPermission(
            anyOf = ["android.permission.BLUETOOTH_CONNECT",
                "android.permission.BLUETOOTH", "android.permission.BLUETOOTH_PRIVILEGED"]
        )
        override fun setAlias(name: String?): Int {
            return fwkDevice.setAlias(name)
        }
    }
}