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

import android.os.Build
import android.os.Bundle
import android.bluetooth.BluetoothGattDescriptor as FwkBluetoothGattDescriptor
import androidx.annotation.RequiresApi
import java.util.UUID

/**
 * @hide
 */
class BluetoothGattDescriptor internal constructor(
    fwkDescriptor: FwkBluetoothGattDescriptor
) : Bundleable {
    private val impl: GattDescriptorImpl =
        if (Build.VERSION.SDK_INT >= 24) {
            GattDescriptorImplApi24(fwkDescriptor)
        } else {
            GattDescriptorImplApi21(fwkDescriptor)
        }
    internal val fwkDescriptor: FwkBluetoothGattDescriptor
        get() = impl.fwkDescriptor
    val permissions: Int
        get() = impl.permissions
    val uuid: UUID
        get() = impl.uuid
    var characteristic: BluetoothGattCharacteristic?
        get() = impl.characteristic
        internal set(value) {
            impl.characteristic = value
        }

    constructor(uuid: UUID, permissions: Int) : this(
        FwkBluetoothGattDescriptor(
            uuid,
            permissions
        )
    )

    companion object {
        /**
         * Descriptor read permission
         */
        const val PERMISSION_READ = FwkBluetoothGattDescriptor.PERMISSION_READ
        /**
         * Descriptor permission: Allow encrypted read operations
         */
        const val PERMISSION_READ_ENCRYPTED =
            FwkBluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED
        /**
         * Descriptor permission: Allow reading with person-in-the-middle protection
         */
        const val PERMISSION_READ_ENCRYPTED_MITM =
            FwkBluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED_MITM
        /**
         * Descriptor write permission
         */
        const val PERMISSION_WRITE = FwkBluetoothGattDescriptor.PERMISSION_WRITE
        /**
         * Descriptor permission: Allow encrypted writes
         */
        const val PERMISSION_WRITE_ENCRYPTED =
            FwkBluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED
        /**
         * Descriptor permission: Allow encrypted writes with person-in-the-middle
         * protection
         */
        const val PERMISSION_WRITE_ENCRYPTED_MITM =
            FwkBluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED_MITM
        /**
         * Descriptor permission: Allow signed write operations
         */
        const val PERMISSION_WRITE_SIGNED =
            FwkBluetoothGattDescriptor.PERMISSION_WRITE_SIGNED
        /**
         * Descriptor permission: Allow signed write operations with person-in-the-middle protection
         */
        const val PERMISSION_WRITE_SIGNED_MITM =
            FwkBluetoothGattDescriptor.PERMISSION_WRITE_SIGNED_MITM

        /**
         * Value used to enable notification for a client configuration descriptor
         */
        val ENABLE_NOTIFICATION_VALUE = byteArrayOf(0x01, 0x00)
        /**
         * Value used to enable indication for a client configuration descriptor
         */
        val ENABLE_INDICATION_VALUE = byteArrayOf(0x02, 0x00)
        /**
         * Value used to disable notifications or indicatinos
         */
        val DISABLE_NOTIFICATION_VALUE = byteArrayOf(0x00, 0x00)

        internal fun keyForField(field: Int): String {
            return field.toString(Character.MAX_RADIX)
        }

        /**
         * A companion object to create [BluetoothGattDescriptor] from bundle
         */
        val CREATOR: Bundleable.Creator<BluetoothGattDescriptor> =
            if (Build.VERSION.SDK_INT >= 24) {
                GattDescriptorImplApi24.CREATOR
            } else {
                GattDescriptorImplApi21.CREATOR
            }
    }

    /**
     * Create a [Bundle] from this object
     */
    override fun toBundle(): Bundle {
        return impl.toBundle()
    }

    private interface GattDescriptorImpl {
        val fwkDescriptor: FwkBluetoothGattDescriptor
        val permissions: Int
        val uuid: UUID
        var characteristic: BluetoothGattCharacteristic?

        fun toBundle(): Bundle
    }

    private open class GattDescriptorImplApi21(
        override val fwkDescriptor: FwkBluetoothGattDescriptor
    ) : GattDescriptorImpl {
        companion object {

            internal const val FIELD_FWK_DESCRIPTOR_PERMISSIONS = 1
            internal const val FIELD_FWK_DESCRIPTOR_UUID = 2
            internal const val FIELD_FWK_DESCRIPTOR_INSTANCE = 3

            val CREATOR: Bundleable.Creator<BluetoothGattDescriptor> =
                object : Bundleable.Creator<BluetoothGattDescriptor> {
                    override fun fromBundle(bundle: Bundle): BluetoothGattDescriptor {
                        val permissions =
                            bundle.getInt(
                                keyForField(FIELD_FWK_DESCRIPTOR_PERMISSIONS),
                                -1
                            )
                        val uuid = bundle.getString(
                            keyForField(FIELD_FWK_DESCRIPTOR_UUID),
                        ) ?: throw IllegalArgumentException("Bundle doesn't include uuid")

                        val instanceId = bundle.getInt(
                            keyForField(FIELD_FWK_DESCRIPTOR_INSTANCE),
                            0
                        )

                        if (permissions == -1) {
                            throw IllegalArgumentException("Bundle doesn't include permission")
                        }

                        val descriptorWithoutInstanceId =
                            FwkBluetoothGattDescriptor(UUID.fromString(uuid), permissions)
                        val descriptor: FwkBluetoothGattDescriptor = descriptorWithoutInstanceId
                            .runCatching {
                                this::class.java.getDeclaredField("mInstance").let {
                                    it.isAccessible = true
                                    it.setInt(this, instanceId)
                                    this
                                }
                            }.getOrDefault(descriptorWithoutInstanceId)

                        return BluetoothGattDescriptor(descriptor)
                    }
                }
        }

        override val permissions: Int
            get() = fwkDescriptor.permissions
        override val uuid: UUID
            get() = fwkDescriptor.uuid
        override var characteristic: BluetoothGattCharacteristic? = null

        override fun toBundle(): Bundle {
            val bundle = Bundle()
            bundle.putString(keyForField(FIELD_FWK_DESCRIPTOR_UUID), uuid.toString())
            bundle.putInt(keyForField(FIELD_FWK_DESCRIPTOR_PERMISSIONS), permissions)
            val instanceId = fwkDescriptor.javaClass.getDeclaredField("mInstance").runCatching {
                this.isAccessible = true
                this.getInt(fwkDescriptor)
            }.getOrDefault(0) // constructor will set instanceId to 0 by default
            bundle.putInt(keyForField(FIELD_FWK_DESCRIPTOR_INSTANCE), instanceId)
            return bundle
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private open class GattDescriptorImplApi24(
        fwkDescriptor: FwkBluetoothGattDescriptor
    ) : GattDescriptorImplApi21(fwkDescriptor) {
        companion object {
            internal const val FIELD_FWK_DESCRIPTOR = 0
            val CREATOR: Bundleable.Creator<BluetoothGattDescriptor> =
                object : Bundleable.Creator<BluetoothGattDescriptor> {
                    override fun fromBundle(bundle: Bundle): BluetoothGattDescriptor {
                        val fwkDescriptor =
                            Utils.getParcelableFromBundle(
                                bundle,
                                keyForField(FIELD_FWK_DESCRIPTOR),
                                FwkBluetoothGattDescriptor::class.java
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