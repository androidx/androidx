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

package androidx.bluetooth

import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.bluetooth.utils.Bundleable

import java.util.UUID

/**
 * @hide
 */
class BluetoothGattDescriptor internal constructor(
    descriptor: android.bluetooth.BluetoothGattDescriptor
) : Bundleable {
    private val impl: GattDescriptorImpl =
        if (Build.VERSION.SDK_INT >= 24) {
            GattDescriptorImplApi24(descriptor)
        } else {
            GattDescriptorImplApi21(descriptor)
        }
    internal val fwkDescriptor: android.bluetooth.BluetoothGattDescriptor
        get() = impl.fwkDescriptor
    val permissions: Int
        get() = impl.permissions
    val uuid: UUID
        get() = impl.uuid
    val characteristic: android.bluetooth.BluetoothGattCharacteristic?
        get() = impl.characteristic

    constructor(uuid: UUID, permissions: Int) : this(
        android.bluetooth.BluetoothGattDescriptor(
            uuid,
            permissions
        )
    )

    companion object {
        const val PERMISSION_READ = android.bluetooth.BluetoothGattDescriptor.PERMISSION_READ
        const val PERMISSION_READ_ENCRYPTED =
            android.bluetooth.BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED
        const val PERMISSION_READ_ENCRYPTED_MITM =
            android.bluetooth.BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED_MITM
        const val PERMISSION_WRITE = android.bluetooth.BluetoothGattDescriptor.PERMISSION_WRITE
        const val PERMISSION_WRITE_ENCRYPTED =
            android.bluetooth.BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED
        const val PERMISSION_WRITE_ENCRYPTED_MITM =
            android.bluetooth.BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED_MITM
        const val PERMISSION_WRITE_SIGNED =
            android.bluetooth.BluetoothGattDescriptor.PERMISSION_WRITE_SIGNED
        const val PERMISSION_WRITE_SIGNED_MITM =
            android.bluetooth.BluetoothGattDescriptor.PERMISSION_WRITE_SIGNED_MITM

        val ENABLE_NOTIFICATION_VALUE = byteArrayOf(0x01, 0x00)
        val ENABLE_INDICATION_VALUE = byteArrayOf(0x02, 0x00)
        val DISABLE_NOTIFICATION_VALUE = byteArrayOf(0x00, 0x00)

        internal fun keyForField(field: Int): String? {
            return field.toString(Character.MAX_RADIX)
        }

        val CREATOR: Bundleable.Creator<BluetoothGattDescriptor> =
            if (Build.VERSION.SDK_INT >= 24) GattDescriptorImplApi24.CREATOR
            else GattDescriptorImplApi21.CREATOR
    }

    override fun toBundle(): Bundle {
        return impl.toBundle()
    }

    private interface GattDescriptorImpl {
        val fwkDescriptor: android.bluetooth.BluetoothGattDescriptor
        val permissions: Int
        val uuid: UUID
        val characteristic: android.bluetooth.BluetoothGattCharacteristic?
        fun toBundle(): Bundle
    }

    private open class GattDescriptorImplApi21(
        descriptor: android.bluetooth.BluetoothGattDescriptor
    ) : GattDescriptorImpl {
        companion object {

            internal const val FIELD_FWK_DESCRIPTOR_PERMISSIONS = 1
            internal const val FIELD_FWK_DESCRIPTOR_UUID = 2
            internal const val FIELD_FWK_DESCRIPTOR_INSTANCE = 3

            val CREATOR: Bundleable.Creator<BluetoothGattDescriptor> =
                object : Bundleable.Creator<BluetoothGattDescriptor> {

                    @Suppress("DEPRECATION")
                    override fun fromBundle(bundle: Bundle): BluetoothGattDescriptor {
                        val permissions =
                            bundle.getInt(
                                keyForField(FIELD_FWK_DESCRIPTOR_PERMISSIONS),
                                -1
                            )
                        val uuid = bundle.getString(
                            keyForField(FIELD_FWK_DESCRIPTOR_UUID),
                        ) ?: throw IllegalArgumentException("Bundle doesn't include uuid")

                        if (permissions == -1) {
                            throw IllegalArgumentException("Bundle doesn't include permission")
                        }

                        val descriptor =
                            android.bluetooth.BluetoothGattDescriptor(
                                UUID.fromString(uuid),
                                permissions
                            )

                        descriptor.javaClass.getDeclaredField("mInstance").setInt(
                            descriptor, bundle.getInt(
                                keyForField(FIELD_FWK_DESCRIPTOR_INSTANCE), 0
                            )
                        )
                        return BluetoothGattDescriptor(descriptor)
                    }
                }
        }

        override val fwkDescriptor: android.bluetooth.BluetoothGattDescriptor = descriptor
        override val permissions: Int
            get() = fwkDescriptor.permissions
        override val uuid: UUID
            get() = fwkDescriptor.uuid
        override val characteristic: android.bluetooth.BluetoothGattCharacteristic?
            get() = fwkDescriptor.characteristic

        override fun toBundle(): Bundle {
            val bundle = Bundle()
            bundle.putString(keyForField(FIELD_FWK_DESCRIPTOR_UUID), uuid.toString())
            bundle.putInt(keyForField(FIELD_FWK_DESCRIPTOR_PERMISSIONS), permissions)
            val instanceId: Int =
                fwkDescriptor.javaClass.getDeclaredField("mInstance").getInt(fwkDescriptor)
            bundle.putInt(keyForField(FIELD_FWK_DESCRIPTOR_INSTANCE), instanceId)
            return bundle
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private open class GattDescriptorImplApi24(
        descriptor: android.bluetooth.BluetoothGattDescriptor
    ) : GattDescriptorImplApi21(descriptor) {
        companion object {
            internal const val FIELD_FWK_DESCRIPTOR = 0
            val CREATOR: Bundleable.Creator<BluetoothGattDescriptor> =
                object : Bundleable.Creator<BluetoothGattDescriptor> {
                    @Suppress("DEPRECATION")
                    override fun fromBundle(bundle: Bundle): BluetoothGattDescriptor {
                        val fwkDescriptor =
                            bundle.getParcelable<android.bluetooth.BluetoothGattDescriptor>(
                                keyForField(FIELD_FWK_DESCRIPTOR)
                            ) ?: throw IllegalArgumentException("Bundle doesn't contain descriptor")
                        return BluetoothGattDescriptor(fwkDescriptor)
                    }
                }
        }

        override fun toBundle(): Bundle {
            val bundle = Bundle()
            bundle.putParcelable(
                keyForField(FIELD_FWK_DESCRIPTOR),
                fwkDescriptor
            )
            return bundle
        }
    }
}