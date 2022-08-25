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

import android.bluetooth.BluetoothClass as FwkBluetoothClass
import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import androidx.bluetooth.core.utils.Bundleable
import androidx.bluetooth.core.utils.Utils
/**
 * Represents a Bluetooth class, which describes general characteristics
 * and capabilities of a device. For example, a Bluetooth class will
 * specify the general device type such as a phone, a computer, or
 * headset, and whether it's capable of services such as audio or telephony.
 *
 * <p>Every Bluetooth class is composed of zero or more service classes, and
 * exactly one device class. The device class is further broken down into major
 * and minor device class components.
 *
 * <p>{@link BluetoothClass} is useful as a hint to roughly describe a device
 * (for example to show an icon in the UI), but does not reliably describe which
 * Bluetooth profiles or services are actually supported by a device. Accurate
 * service discovery is done through SDP requests, which are automatically
 * performed when creating an RFCOMM socket with {@link
 * BluetoothDevice#createRfcommSocketToServiceRecord} and {@link
 * BluetoothAdapter#listenUsingRfcommWithServiceRecord}</p>
 *
 * <p>Use {@link BluetoothDevice#getBluetoothClass} to retrieve the class for
 * a remote device.
 *
 * <!--
 * The Bluetooth class is a 32 bit field. The format of these bits is defined at
 * http://www.bluetooth.org/Technical/AssignedNumbers/baseband.htm
 * (login required). This class contains that 32 bit field, and provides
 * constants and methods to determine which Service Class(es) and Device Class
 * are encoded in that field.
 * -->
 *
 * @hide
 */
class BluetoothClass internal constructor(private val fwkBluetoothClass: FwkBluetoothClass) :
    Bundleable {
    companion object {

        const val PROFILE_HEADSET = 0 // FwkBluetoothClass.PROFILE_HEADSET
        const val PROFILE_A2DP = 1 // FwkBluetoothClass.PROFILE_A2DP
        const val PROFILE_HID = 3 // FwkBluetoothClass.PROFILE_HID

        const val PROFILE_OPP = 2
        const val PROFILE_PANU = 4
        private const val PROFILE_NAP = 5
        private const val PROFILE_A2DP_SINK = 6

        internal const val FIELD_FWK_BLUETOOTH_CLASS = 1

        internal fun keyForField(field: Int): String {
            return field.toString(Character.MAX_RADIX)
        }

        val CREATOR: Bundleable.Creator<BluetoothClass> =
            object : Bundleable.Creator<BluetoothClass> {
                override fun fromBundle(bundle: Bundle): BluetoothClass {
                    val fwkBluetoothClass = Utils.getParcelableFromBundle(
                        bundle,
                        keyForField(FIELD_FWK_BLUETOOTH_CLASS),
                        FwkBluetoothClass::class.java
                    ) ?: throw IllegalArgumentException(
                        "Bundle doesn't include " +
                            "BluetoothClass instance"
                    )

                    return BluetoothClass(fwkBluetoothClass)
                }
            }
    }

    /**
     * Defines all service class constants.
     *
     * Each [BluetoothClass] encodes zero or more service classes.
     */
    object Service {
        const val LIMITED_DISCOVERABILITY = FwkBluetoothClass.Service.LIMITED_DISCOVERABILITY

        /** Represent devices LE audio service  */
        const val LE_AUDIO = FwkBluetoothClass.Service.LE_AUDIO
        const val POSITIONING = FwkBluetoothClass.Service.POSITIONING
        const val NETWORKING = FwkBluetoothClass.Service.NETWORKING
        const val RENDER = FwkBluetoothClass.Service.RENDER
        const val CAPTURE = FwkBluetoothClass.Service.CAPTURE
        const val OBJECT_TRANSFER = FwkBluetoothClass.Service.OBJECT_TRANSFER
        const val AUDIO = FwkBluetoothClass.Service.AUDIO
        const val TELEPHONY = FwkBluetoothClass.Service.TELEPHONY
        const val INFORMATION = FwkBluetoothClass.Service.INFORMATION
    }

    /**
     * Defines all device class constants.
     *
     * Each [BluetoothClass] encodes exactly one device class, with
     * major and minor components.
     *
     * The constants in [ ] represent a combination of major and minor
     * device components (the complete device class). The constants in [ ] represent only major
     * device classes.
     *
     * See [BluetoothClass.Service] for service class constants.
     */
    object Device {
        // Devices in the COMPUTER major class
        const val COMPUTER_UNCATEGORIZED = FwkBluetoothClass.Device.COMPUTER_UNCATEGORIZED
        const val COMPUTER_DESKTOP = FwkBluetoothClass.Device.COMPUTER_DESKTOP
        const val COMPUTER_SERVER = FwkBluetoothClass.Device.COMPUTER_SERVER
        const val COMPUTER_LAPTOP = FwkBluetoothClass.Device.COMPUTER_LAPTOP
        const val COMPUTER_HANDHELD_PC_PDA = FwkBluetoothClass.Device.COMPUTER_HANDHELD_PC_PDA
        const val COMPUTER_PALM_SIZE_PC_PDA = FwkBluetoothClass.Device.COMPUTER_PALM_SIZE_PC_PDA
        const val COMPUTER_WEARABLE = FwkBluetoothClass.Device.COMPUTER_WEARABLE

        // Devices in the PHONE major class
        const val PHONE_UNCATEGORIZED = FwkBluetoothClass.Device.PHONE_UNCATEGORIZED
        const val PHONE_CELLULAR = FwkBluetoothClass.Device.PHONE_CELLULAR
        const val PHONE_CORDLESS = FwkBluetoothClass.Device.PHONE_CORDLESS
        const val PHONE_SMART = FwkBluetoothClass.Device.PHONE_SMART
        const val PHONE_MODEM_OR_GATEWAY = FwkBluetoothClass.Device.PHONE_MODEM_OR_GATEWAY
        const val PHONE_ISDN = FwkBluetoothClass.Device.PHONE_ISDN

        // Minor classes for the AUDIO_VIDEO major class
        const val AUDIO_VIDEO_UNCATEGORIZED = FwkBluetoothClass.Device.AUDIO_VIDEO_UNCATEGORIZED
        const val AUDIO_VIDEO_WEARABLE_HEADSET =
            FwkBluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET
        const val AUDIO_VIDEO_HANDSFREE = FwkBluetoothClass.Device.AUDIO_VIDEO_HANDSFREE

        const val AUDIO_VIDEO_MICROPHONE = FwkBluetoothClass.Device.AUDIO_VIDEO_MICROPHONE
        const val AUDIO_VIDEO_LOUDSPEAKER = FwkBluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER
        const val AUDIO_VIDEO_HEADPHONES = FwkBluetoothClass.Device.AUDIO_VIDEO_HEADPHONES
        const val AUDIO_VIDEO_PORTABLE_AUDIO =
            FwkBluetoothClass.Device.AUDIO_VIDEO_PORTABLE_AUDIO
        const val AUDIO_VIDEO_CAR_AUDIO = FwkBluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO
        const val AUDIO_VIDEO_SET_TOP_BOX = FwkBluetoothClass.Device.AUDIO_VIDEO_SET_TOP_BOX
        const val AUDIO_VIDEO_HIFI_AUDIO = FwkBluetoothClass.Device.AUDIO_VIDEO_HIFI_AUDIO
        const val AUDIO_VIDEO_VCR = FwkBluetoothClass.Device.AUDIO_VIDEO_VCR
        const val AUDIO_VIDEO_VIDEO_CAMERA = FwkBluetoothClass.Device.AUDIO_VIDEO_VIDEO_CAMERA
        const val AUDIO_VIDEO_CAMCORDER = FwkBluetoothClass.Device.AUDIO_VIDEO_CAMCORDER
        const val AUDIO_VIDEO_VIDEO_MONITOR = FwkBluetoothClass.Device.AUDIO_VIDEO_VIDEO_MONITOR
        const val AUDIO_VIDEO_VIDEO_DISPLAY_AND_LOUDSPEAKER =
            FwkBluetoothClass.Device.AUDIO_VIDEO_VIDEO_DISPLAY_AND_LOUDSPEAKER
        const val AUDIO_VIDEO_VIDEO_CONFERENCING =
            FwkBluetoothClass.Device.AUDIO_VIDEO_VIDEO_CONFERENCING
        const val AUDIO_VIDEO_VIDEO_GAMING_TOY =
            FwkBluetoothClass.Device.AUDIO_VIDEO_VIDEO_GAMING_TOY

        // Devices in the WEARABLE major class
        const val WEARABLE_UNCATEGORIZED = FwkBluetoothClass.Device.WEARABLE_UNCATEGORIZED
        const val WEARABLE_WRIST_WATCH = FwkBluetoothClass.Device.WEARABLE_WRIST_WATCH
        const val WEARABLE_PAGER = FwkBluetoothClass.Device.WEARABLE_PAGER
        const val WEARABLE_JACKET = FwkBluetoothClass.Device.WEARABLE_JACKET
        const val WEARABLE_HELMET = FwkBluetoothClass.Device.WEARABLE_HELMET
        const val WEARABLE_GLASSES = FwkBluetoothClass.Device.WEARABLE_GLASSES

        // Devices in the TOY major class
        const val TOY_UNCATEGORIZED = FwkBluetoothClass.Device.TOY_UNCATEGORIZED
        const val TOY_ROBOT = FwkBluetoothClass.Device.TOY_ROBOT
        const val TOY_VEHICLE = FwkBluetoothClass.Device.TOY_VEHICLE
        const val TOY_DOLL_ACTION_FIGURE = FwkBluetoothClass.Device.TOY_DOLL_ACTION_FIGURE
        const val TOY_CONTROLLER = FwkBluetoothClass.Device.TOY_CONTROLLER
        const val TOY_GAME = FwkBluetoothClass.Device.TOY_GAME

        // Devices in the HEALTH major class
        const val HEALTH_UNCATEGORIZED = FwkBluetoothClass.Device.HEALTH_UNCATEGORIZED
        const val HEALTH_BLOOD_PRESSURE = FwkBluetoothClass.Device.HEALTH_BLOOD_PRESSURE
        const val HEALTH_THERMOMETER = FwkBluetoothClass.Device.HEALTH_THERMOMETER
        const val HEALTH_WEIGHING = FwkBluetoothClass.Device.HEALTH_WEIGHING
        const val HEALTH_GLUCOSE = FwkBluetoothClass.Device.HEALTH_GLUCOSE
        const val HEALTH_PULSE_OXIMETER = FwkBluetoothClass.Device.HEALTH_PULSE_OXIMETER
        const val HEALTH_PULSE_RATE = FwkBluetoothClass.Device.HEALTH_PULSE_RATE
        const val HEALTH_DATA_DISPLAY = FwkBluetoothClass.Device.HEALTH_DATA_DISPLAY

        // Devices in PERIPHERAL major class
        const val PERIPHERAL_NON_KEYBOARD_NON_POINTING =
            0x0500 // FwkBluetoothClass.Device.PERIPHERAL_NON_KEYBOARD_NON_POINTING

        const val PERIPHERAL_KEYBOARD =
            0x0540 // FwkBluetoothClass.Device.PERIPHERAL_KEYBOARD
        const val PERIPHERAL_POINTING =
            0x0580 // FwkBluetoothClass.Device.PERIPHERAL_POINTING
        const val PERIPHERAL_KEYBOARD_POINTING =
            0x05C0 // FwkBluetoothClass.Device.PERIPHERAL_KEYBOARD_POINTING

        /**
         * Defines all major device class constants.
         *
         * See [BluetoothClass.Device] for minor classes.
         */
        object Major {
            const val MISC = FwkBluetoothClass.Device.Major.MISC
            const val COMPUTER = FwkBluetoothClass.Device.Major.COMPUTER
            const val PHONE = FwkBluetoothClass.Device.Major.PHONE
            const val NETWORKING = FwkBluetoothClass.Device.Major.NETWORKING
            const val AUDIO_VIDEO = FwkBluetoothClass.Device.Major.AUDIO_VIDEO
            const val PERIPHERAL = FwkBluetoothClass.Device.Major.PERIPHERAL
            const val IMAGING = FwkBluetoothClass.Device.Major.IMAGING
            const val WEARABLE = FwkBluetoothClass.Device.Major.WEARABLE
            const val TOY = FwkBluetoothClass.Device.Major.TOY
            const val HEALTH = FwkBluetoothClass.Device.Major.HEALTH
            const val UNCATEGORIZED = FwkBluetoothClass.Device.Major.UNCATEGORIZED
        }
    }

    /**
     * Return the major device class component of this [BluetoothClass].
     *
     * Values returned from this function can be compared with the
     * public constants in [BluetoothClass.Device.Major] to determine
     * which major class is encoded in this Bluetooth class.
     *
     * @return major device class component
     */
    val majorDeviceClass: Int
        get() = fwkBluetoothClass.majorDeviceClass

    /**
     * Return the (major and minor) device class component of this
     * [BluetoothClass].
     *
     * Values returned from this function can be compared with the
     * public constants in [BluetoothClass.Device] to determine which
     * device class is encoded in this Bluetooth class.
     *
     * @return device class component
     */
    val deviceClass: Int
        get() = fwkBluetoothClass.deviceClass

    /**
     * Return true if the specified service class is supported by this
     * [BluetoothClass].
     *
     * Valid service classes are the public constants in
     * [BluetoothClass.Service]. For example, [ ][BluetoothClass.Service.AUDIO].
     *
     * @param service valid service class
     * @return true if the service class is supported
     */
    fun hasService(service: Int): Boolean {
        return fwkBluetoothClass.hasService(service)
    }

    override fun toBundle(): Bundle {
        val bundle = Bundle()
        bundle.putParcelable(keyForField(FIELD_FWK_BLUETOOTH_CLASS), fwkBluetoothClass)
        return bundle
    }

    @SuppressLint("ClassVerificationFailure") // fwkBluetoothClass.doesClassMatch(profile)
    fun doesClassMatch(profile: Int): Boolean {
        if (Build.VERSION.SDK_INT >= 33) {
            // TODO: change to Impl hierarchy when additional SDK checks are needed
            return fwkBluetoothClass.doesClassMatch(profile)
        }
        return when (profile) {
            PROFILE_A2DP -> {
                if (hasService(Service.RENDER)) {
                    return true
                }
                when (deviceClass) {
                    Device.AUDIO_VIDEO_HIFI_AUDIO, Device.AUDIO_VIDEO_HEADPHONES,
                    Device.AUDIO_VIDEO_LOUDSPEAKER, Device.AUDIO_VIDEO_CAR_AUDIO -> true
                    else -> false
                }
            }
            PROFILE_A2DP_SINK -> {
                if (hasService(Service.CAPTURE)) {
                    return true
                }
                when (deviceClass) {
                    Device.AUDIO_VIDEO_HIFI_AUDIO, Device.AUDIO_VIDEO_SET_TOP_BOX,
                    Device.AUDIO_VIDEO_VCR -> true
                    else -> false
                }
            }
            PROFILE_HEADSET -> {
                if (hasService(Service.RENDER)) {
                    return true
                }
                when (deviceClass) {
                    Device.AUDIO_VIDEO_HANDSFREE, Device.AUDIO_VIDEO_WEARABLE_HEADSET,
                    Device.AUDIO_VIDEO_CAR_AUDIO -> true
                    else -> false
                }
            }
            PROFILE_OPP -> {
                if (hasService(Service.OBJECT_TRANSFER)) {
                    return true
                }
                when (deviceClass) {
                    Device.COMPUTER_UNCATEGORIZED, Device.COMPUTER_DESKTOP, Device.COMPUTER_SERVER,
                    Device.COMPUTER_LAPTOP, Device.COMPUTER_HANDHELD_PC_PDA,
                    Device.COMPUTER_PALM_SIZE_PC_PDA, Device.COMPUTER_WEARABLE,
                    Device.PHONE_UNCATEGORIZED, Device.PHONE_CELLULAR, Device.PHONE_CORDLESS,
                    Device.PHONE_SMART, Device.PHONE_MODEM_OR_GATEWAY, Device.PHONE_ISDN -> true
                    else -> false
                }
            }
            PROFILE_HID -> {
                majorDeviceClass == Device.Major.PERIPHERAL
            }
            PROFILE_NAP, PROFILE_PANU -> {
                if (hasService(Service.NETWORKING)) {
                    true
                } else majorDeviceClass == Device.Major.NETWORKING
            }
            else -> false
        }
    }
}