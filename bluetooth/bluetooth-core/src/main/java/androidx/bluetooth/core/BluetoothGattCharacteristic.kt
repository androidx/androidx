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

import android.bluetooth.BluetoothGattCharacteristic as FwkBluetoothGattCharacteristic
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import java.util.UUID

/**
 * @hide
 */
class BluetoothGattCharacteristic internal constructor(
    fwkCharacteristic: FwkBluetoothGattCharacteristic
) : Bundleable {

    companion object {
        /**
         * Characteristic value format type float (32-bit float)
         */
        const val FORMAT_FLOAT = FwkBluetoothGattCharacteristic.FORMAT_FLOAT
        /**
         * Characteristic value format type sfloat (16-bit float)
         */
        const val FORMAT_SFLOAT = FwkBluetoothGattCharacteristic.FORMAT_SFLOAT
        /**
         * Characteristic value format type uint16
         */
        const val FORMAT_SINT16 = FwkBluetoothGattCharacteristic.FORMAT_SINT16
        /**
         * Characteristic value format type sint32
         */
        const val FORMAT_SINT32 = FwkBluetoothGattCharacteristic.FORMAT_SINT32
        /**
         * Characteristic value format type sint8
         */
        const val FORMAT_SINT8 = FwkBluetoothGattCharacteristic.FORMAT_SINT8
        /**
         * Characteristic value format type uint16
         */
        const val FORMAT_UINT16 = FwkBluetoothGattCharacteristic.FORMAT_UINT16
        /**
         * Characteristic value format type uint32
         */
        const val FORMAT_UINT32 = FwkBluetoothGattCharacteristic.FORMAT_UINT32
        /**
         * Characteristic value format type uint8
         */
        const val FORMAT_UINT8 = FwkBluetoothGattCharacteristic.FORMAT_UINT8

        /**
         * Characteristic property: Characteristic is broadcastable.
         */
        const val PROPERTY_BROADCAST =
            FwkBluetoothGattCharacteristic.PROPERTY_BROADCAST
        /**
         * Characteristic property: Characteristic is readable.
         */
        const val PROPERTY_READ = FwkBluetoothGattCharacteristic.PROPERTY_READ
        /**
         * Characteristic property: Characteristic can be written without response.
         */
        const val PROPERTY_WRITE_NO_RESPONSE =
            FwkBluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE
        /**
         * Characteristic property: Characteristic can be written.
         */
        const val PROPERTY_WRITE = FwkBluetoothGattCharacteristic.PROPERTY_WRITE
        /**
         * Characteristic property: Characteristic supports notification
         */
        const val PROPERTY_NOTIFY = FwkBluetoothGattCharacteristic.PROPERTY_NOTIFY
        /**
         * Characteristic property: Characteristic supports indication
         */
        const val PROPERTY_INDICATE =
            FwkBluetoothGattCharacteristic.PROPERTY_BROADCAST
        /**
         * Characteristic property: Characteristic supports write with signature
         */
        const val PROPERTY_SIGNED_WRITE =
            FwkBluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE
        /**
         * Characteristic property: Characteristic has extended properties
         */
        const val PROPERTY_EXTENDED_PROPS =
            FwkBluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS
        /**
         * Characteristic read permission
         */
        const val PERMISSION_READ = FwkBluetoothGattCharacteristic.PERMISSION_READ
        /**
         * Characteristic permission: Allow encrypted read operations
         */
        const val PERMISSION_READ_ENCRYPTED =
            FwkBluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED
        /**
         * Characteristic permission: Allow reading with person-in-the-middle protection
         */
        const val PERMISSION_READ_ENCRYPTED_MITM =
            FwkBluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM
        /**
         * Characteristic write permission
         */
        const val PERMISSION_WRITE = FwkBluetoothGattCharacteristic.PERMISSION_WRITE
        /**
         * Characteristic permission: Allow encrypted writes
         */
        const val PERMISSION_WRITE_ENCRYPTED =
            FwkBluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED
        /**
         * Characteristic permission: Allow encrypted writes with person-in-the-middle protection
         */
        const val PERMISSION_WRITE_ENCRYPTED_MITM =
            FwkBluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM
        /**
         * Characteristic permission: Allow signed write operations
         */
        const val PERMISSION_WRITE_SIGNED =
            FwkBluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED
        /**
         * Characteristic permission: Allow signed write operations with
         * person-in-the-middle protection
         */
        const val PERMISSION_WRITE_SIGNED_MITM =
            FwkBluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED_MITM
        /**
         * Write characteristic, requesting acknowledgement by the remote device
         */
        const val WRITE_TYPE_DEFAULT =
            FwkBluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        /**
         * Write characteristic without requiring a response by the remote device
         */
        const val WRITE_TYPE_NO_RESPONSE =
            FwkBluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        /**
         * Write characteristic including authentication signature
         */
        const val WRITE_TYPE_SIGNED =
            FwkBluetoothGattCharacteristic.WRITE_TYPE_SIGNED

        internal fun keyForField(field: Int): String {
            return field.toString(Character.MAX_RADIX)
        }

        /**
         * A companion object to create [BluetoothGattCharacteristic] from bundle
         */
        val CREATOR: Bundleable.Creator<BluetoothGattCharacteristic> =
            if (Build.VERSION.SDK_INT >= 24) {
                GattCharacteristicImplApi24.CREATOR
            } else {
                GattCharacteristicImplApi21.CREATOR
            }
    }

    /**
     * Implementation based on version
     */
    private val impl: GattCharacteristicImpl =
        if (Build.VERSION.SDK_INT >= 24) {
            GattCharacteristicImplApi24(fwkCharacteristic, this)
        } else {
            GattCharacteristicImplApi21(fwkCharacteristic, this)
        }

    /**
     * Underlying framework's [android.bluetooth.BluetoothGattCharacteristic]
     */
    internal val fwkCharacteristic
        get() = impl.fwkCharacteristic
    /**
     * the UUID of this characteristic.
     */
    val uuid
        get() = impl.uuid
    /**
     * Characteristic properties
     */
    val properties
        get() = impl.properties
    /**
     * Write type for this characteristic.
     */
    val permissions
        get() = impl.permissions
    /**
     * Write type for this characteristic.
     */
    val instanceId
        get() = impl.instanceId

    /**
     * Write type for this characteristic.
     */
    var writeType: Int
        get() = impl.writeType
        set(value) {
            impl.writeType = value
        }

    /**
     * Library's [BluetoothGattDescriptor] list that this characteristic owns
     */
    val descriptors: List<BluetoothGattDescriptor>
        get() = impl.descriptors

    /**
    * Library's [BluetoothGattService] that this belongs to
    */
    var service: BluetoothGattService?
        get() = impl.service
        internal set(value) {
            impl.service = value
        }

    /**
     * Create a new BluetoothGattCharacteristic.
     *
     * @param uuid The UUID for this characteristic
     * @param properties Properties of this characteristic
     * @param permissions Permissions for this characteristic
     */
    constructor (uuid: UUID, properties: Int, permissions: Int) : this(
        FwkBluetoothGattCharacteristic(uuid, properties, permissions)
    )

    /**
     * Adds a descriptor to this characteristic.
     *
     * @param descriptor Descriptor to be added to this characteristic.
     * @return true, if the descriptor was added to the characteristic
     */
    fun addDescriptor(descriptor: BluetoothGattDescriptor): Boolean {
        return impl.addDescriptor(descriptor)
    }

    /**
     * Get a descriptor by UUID
     */
    fun getDescriptor(uuid: UUID): BluetoothGattDescriptor? {
        return impl.getDescriptor(uuid)
    }

    /**
     * Create a [Bundle] from this object
     */
    override fun toBundle(): Bundle {
        return impl.toBundle()
    }

    private interface GattCharacteristicImpl : Bundleable {

        val fwkCharacteristic: FwkBluetoothGattCharacteristic
        val uuid: UUID
        val properties: Int
        val permissions: Int
        val instanceId: Int
        var writeType: Int
        val descriptors: List<BluetoothGattDescriptor>
        var service: BluetoothGattService?

        fun addDescriptor(descriptor: BluetoothGattDescriptor): Boolean
        fun getDescriptor(uuid: UUID): BluetoothGattDescriptor?

        override fun toBundle(): Bundle
    }

    private open class GattCharacteristicImplApi21(
        final override val fwkCharacteristic: FwkBluetoothGattCharacteristic,
        private val characteristic: BluetoothGattCharacteristic,
    ) : GattCharacteristicImpl {

        companion object {
            internal const val FIELD_FWK_CHARACTERISTIC_UUID = 1
            internal const val FIELD_FWK_CHARACTERISTIC_INSTANCE_ID = 2
            internal const val FIELD_FWK_CHARACTERISTIC_PROPERTIES = 3
            internal const val FIELD_FWK_CHARACTERISTIC_PERMISSIONS = 4
            internal const val FIELD_FWK_CHARACTERISTIC_WRITE_TYPE = 5
            internal const val FIELD_FWK_CHARACTERISTIC_KEY_SIZE = 6
            internal const val FIELD_FWK_CHARACTERISTIC_DESCRIPTORS = 7

            val CREATOR: Bundleable.Creator<BluetoothGattCharacteristic> =
                object : Bundleable.Creator<BluetoothGattCharacteristic> {
                    override fun fromBundle(bundle: Bundle): BluetoothGattCharacteristic {
                        val uuid = bundle.getString(keyForField(FIELD_FWK_CHARACTERISTIC_UUID))
                            ?: throw IllegalArgumentException("Bundle doesn't include uuid")
                        val instanceId =
                            bundle.getInt(keyForField(FIELD_FWK_CHARACTERISTIC_INSTANCE_ID), 0)
                        val properties =
                            bundle.getInt(keyForField(FIELD_FWK_CHARACTERISTIC_PROPERTIES), -1)
                        val permissions =
                            bundle.getInt(keyForField(FIELD_FWK_CHARACTERISTIC_PERMISSIONS), -1)
                        val writeType =
                            bundle.getInt(keyForField(FIELD_FWK_CHARACTERISTIC_WRITE_TYPE), -1)
                        val keySize =
                            bundle.getInt(keyForField(FIELD_FWK_CHARACTERISTIC_KEY_SIZE), -1)
                        if (permissions == -1) {
                            throw IllegalArgumentException("Bundle doesn't include permission")
                        }
                        if (properties == -1) {
                            throw IllegalArgumentException("Bundle doesn't include properties")
                        }
                        if (writeType == -1) {
                            throw IllegalArgumentException("Bundle doesn't include writeType")
                        }
                        if (keySize == -1) {
                            throw IllegalArgumentException("Bundle doesn't include keySize")
                        }

                        val fwkCharacteristic =
                            FwkBluetoothGattCharacteristic::class.java.getConstructor(
                                UUID::class.java,
                                Integer.TYPE,
                                Integer.TYPE,
                                Integer.TYPE,
                            ).newInstance(
                                UUID.fromString(uuid),
                                instanceId,
                                properties,
                                permissions,
                            )
                        fwkCharacteristic.writeType = writeType

                        // Asserted, will always be true
                        if (Build.VERSION.SDK_INT < 24) {
                            fwkCharacteristic.javaClass.getDeclaredField("mKeySize")
                                .setInt(fwkCharacteristic, keySize)
                        }

                        val gattCharacteristic = BluetoothGattCharacteristic(fwkCharacteristic)

                        Utils.getParcelableArrayListFromBundle(
                            bundle,
                            keyForField(FIELD_FWK_CHARACTERISTIC_DESCRIPTORS),
                            Bundle::class.java
                        ).forEach {
                            gattCharacteristic.addDescriptor(
                                BluetoothGattDescriptor.CREATOR.fromBundle(it)
                            )
                        }

                        return gattCharacteristic
                    }
                }
        }

        override val uuid: UUID
            get() = fwkCharacteristic.uuid
        override val properties
            get() = fwkCharacteristic.properties
        override val permissions
            get() = fwkCharacteristic.permissions
        override val instanceId
            get() = fwkCharacteristic.instanceId
        override var writeType: Int
            get() = fwkCharacteristic.writeType
            set(value) {
                fwkCharacteristic.writeType = value
            }
        private var _descriptors = mutableListOf<BluetoothGattDescriptor>()
        override val descriptors
            get() = _descriptors.toList()
        override var service: BluetoothGattService? = null
        init {
            fwkCharacteristic.descriptors.forEach {
                val descriptor = BluetoothGattDescriptor(it)
                _descriptors.add(descriptor)
                descriptor.characteristic = characteristic
            }
        }

        override fun addDescriptor(
            descriptor: BluetoothGattDescriptor,
        ): Boolean {
            return if (fwkCharacteristic.addDescriptor(descriptor.fwkDescriptor)) {
                _descriptors.add(descriptor)
                descriptor.characteristic = characteristic
                true
            } else {
                false
            }
        }

        override fun getDescriptor(uuid: UUID): BluetoothGattDescriptor? {
            return _descriptors.firstOrNull {
                it.uuid == uuid
            }
        }

        override fun toBundle(): Bundle {
            assert(Build.VERSION.SDK_INT < 24)

            val bundle = Bundle()
            bundle.putString(keyForField(FIELD_FWK_CHARACTERISTIC_UUID), uuid.toString())
            bundle.putInt(keyForField(FIELD_FWK_CHARACTERISTIC_INSTANCE_ID), instanceId)
            bundle.putInt(keyForField(FIELD_FWK_CHARACTERISTIC_PROPERTIES), properties)
            bundle.putInt(keyForField(FIELD_FWK_CHARACTERISTIC_PERMISSIONS), permissions)
            bundle.putInt(keyForField(FIELD_FWK_CHARACTERISTIC_WRITE_TYPE), writeType)
            // Asserted, this will always work
            if (Build.VERSION.SDK_INT < 24) {
                bundle.putInt(
                    keyForField(FIELD_FWK_CHARACTERISTIC_KEY_SIZE),
                    fwkCharacteristic.javaClass.getDeclaredField("mKeySize")
                        .getInt(fwkCharacteristic)
                )
            }
            val descriptorBundles = ArrayList(descriptors.map { it.toBundle() })
            bundle.putParcelableArrayList(
                keyForField(FIELD_FWK_CHARACTERISTIC_DESCRIPTORS),
                descriptorBundles
            )

            return bundle
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private class GattCharacteristicImplApi24(
        fwkCharacteristic: FwkBluetoothGattCharacteristic,
        characteristic: BluetoothGattCharacteristic,
    ) : GattCharacteristicImplApi21(fwkCharacteristic, characteristic) {
        companion object {
            internal const val FIELD_FWK_CHARACTERISTIC = 0

            val CREATOR: Bundleable.Creator<BluetoothGattCharacteristic> =
                object : Bundleable.Creator<BluetoothGattCharacteristic> {
                    @Suppress("deprecation")
                    override fun fromBundle(bundle: Bundle): BluetoothGattCharacteristic {
                        val fwkCharacteristic =
                            Utils.getParcelableFromBundle(
                                bundle,
                                keyForField(FIELD_FWK_CHARACTERISTIC),
                                FwkBluetoothGattCharacteristic::class.java
                            ) ?: throw IllegalArgumentException(
                                "Bundle doesn't include framework characteristic"
                            )
                        return BluetoothGattCharacteristic(fwkCharacteristic)
                    }
                }
        }

        override fun toBundle(): Bundle {
            val bundle = Bundle()
            bundle.putParcelable(
                keyForField(FIELD_FWK_CHARACTERISTIC),
                fwkCharacteristic
            )
            return bundle
        }
    }
}
