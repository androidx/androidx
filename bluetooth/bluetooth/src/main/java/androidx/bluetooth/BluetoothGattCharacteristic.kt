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
import android.bluetooth.BluetoothGattCharacteristic as FwkBluetoothGattCharacteristic
import androidx.annotation.RequiresApi
import androidx.bluetooth.utils.Bundleable

import java.util.UUID
/**
 * @hide
 */
class BluetoothGattCharacteristic internal constructor(
    fwkCharacteristic: FwkBluetoothGattCharacteristic
) : Bundleable {

    companion object {
        const val FORMAT_FLOAT = FwkBluetoothGattCharacteristic.FORMAT_FLOAT
        const val FORMAT_SFLOAT = FwkBluetoothGattCharacteristic.FORMAT_SFLOAT
        const val FORMAT_SINT16 = FwkBluetoothGattCharacteristic.FORMAT_SINT16
        const val FORMAT_SINT32 = FwkBluetoothGattCharacteristic.FORMAT_SINT32
        const val FORMAT_SINT8 = FwkBluetoothGattCharacteristic.FORMAT_SINT8
        const val FORMAT_UINT16 = FwkBluetoothGattCharacteristic.FORMAT_UINT16
        const val FORMAT_UINT32 = FwkBluetoothGattCharacteristic.FORMAT_UINT32
        const val FORMAT_UINT8 = FwkBluetoothGattCharacteristic.FORMAT_UINT8
        const val PROPERTY_BROADCAST =
            FwkBluetoothGattCharacteristic.PROPERTY_BROADCAST
        const val PROPERTY_READ = FwkBluetoothGattCharacteristic.PROPERTY_READ
        const val PROPERTY_WRITE_NO_RESPONSE =
            FwkBluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE
        const val PROPERTY_WRITE = FwkBluetoothGattCharacteristic.PROPERTY_WRITE
        const val PROPERTY_NOTIFY = FwkBluetoothGattCharacteristic.PROPERTY_NOTIFY
        const val PROPERTY_INDICATE =
            FwkBluetoothGattCharacteristic.PROPERTY_BROADCAST
        const val PROPERTY_SIGNED_WRITE =
            FwkBluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE
        const val PROPERTY_EXTENDED_PROPS =
            FwkBluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS
        const val PERMISSION_READ = FwkBluetoothGattCharacteristic.PERMISSION_READ
        const val PERMISSION_READ_ENCRYPTED =
            FwkBluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED
        const val PERMISSION_READ_ENCRYPTED_MITM =
            FwkBluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM
        const val PERMISSION_WRITE = FwkBluetoothGattCharacteristic.PERMISSION_WRITE
        const val PERMISSION_WRITE_ENCRYPTED =
            FwkBluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED
        const val PERMISSION_WRITE_ENCRYPTED_MITM =
            FwkBluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM
        const val PERMISSION_WRITE_SIGNED =
            FwkBluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED
        const val PERMISSION_WRITE_SIGNED_MITM =
            FwkBluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED_MITM
        const val WRITE_TYPE_DEFAULT =
            FwkBluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        const val WRITE_TYPE_NO_RESPONSE =
            FwkBluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        const val WRITE_TYPE_SIGNED =
            FwkBluetoothGattCharacteristic.WRITE_TYPE_SIGNED

        internal fun keyForField(field: Int): String {
            return field.toString(Character.MAX_RADIX)
        }

        val CREATOR: Bundleable.Creator<BluetoothGattCharacteristic> =
            if (Build.VERSION.SDK_INT >= 24) {
                GattCharacteristicImplApi24.CREATOR
            } else {
                GattCharacteristicImplApi21.CREATOR
            }
    }

    private val impl: GattCharacteristicImpl =
        if (Build.VERSION.SDK_INT >= 24) {
            GattCharacteristicImplApi24(fwkCharacteristic, this)
        } else {
            GattCharacteristicImplApi21(fwkCharacteristic, this)
        }

    internal val fwkCharacteristic
        get() = impl.fwkCharacteristic
    val uuid
        get() = impl.uuid
    val properties
        get() = impl.properties
    val permissions
        get() = impl.permissions
    val instanceId
        get() = impl.instanceId
    var writeType: Int
        get() = impl.writeType
        set(value) {
            impl.writeType = value
        }
    val descriptors
        get() = impl.descriptors

    constructor (uuid: UUID, properties: Int, permissions: Int) : this(
        FwkBluetoothGattCharacteristic(uuid, properties, permissions)
    )

    fun addDescriptor(descriptor: BluetoothGattDescriptor): Boolean {
        return impl.addDescriptor(descriptor)
    }

    fun getDescriptor(uuid: UUID): BluetoothGattDescriptor? {
        return impl.getDescriptor(uuid)
    }

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
                    @Suppress("deprecation")
                    override fun fromBundle(bundle: Bundle): BluetoothGattCharacteristic {
                        assert(Build.VERSION.SDK_INT < 24)
                        val uuid = bundle.getString(keyForField(FIELD_FWK_CHARACTERISTIC_UUID))
                            ?: throw IllegalArgumentException("Bundle doesn't include uuid")
                        val instanceId =
                            bundle.getInt(keyForField(FIELD_FWK_CHARACTERISTIC_INSTANCE_ID), -1)
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
                        if (instanceId == -1) {
                            throw IllegalArgumentException("Bundle doesn't include instanceId")
                        }
                        if (keySize == -1) {
                            throw IllegalArgumentException("Bundle doesn't include keySize")
                        }

                        val fwkCharacteristic =
                            FwkBluetoothGattCharacteristic(
                                UUID.fromString(uuid),
                                properties,
                                permissions
                            )
                        fwkCharacteristic.writeType = writeType

                        // Asserted, will always be true
                        if (Build.VERSION.SDK_INT < 24) {
                            fwkCharacteristic.javaClass.getDeclaredField("mKeySize")
                                .setInt(fwkCharacteristic, keySize)
                        }

                        val gattCharacteristic = BluetoothGattCharacteristic(fwkCharacteristic)

                        bundle.getParcelableArrayList<Bundle>(
                            keyForField(FIELD_FWK_CHARACTERISTIC_DESCRIPTORS)
                        )?.forEach { it ->
                            gattCharacteristic.addDescriptor(
                                BluetoothGattDescriptor.CREATOR.fromBundle(it)
                            )
                        }

                        return gattCharacteristic
                    }
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

        init {
            fwkCharacteristic.descriptors.forEach {
                val descriptor = BluetoothGattDescriptor(it)
                mDescriptors.add(descriptor)
                descriptor.characteristic = characteristic
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
        private var mDescriptors = mutableListOf<BluetoothGattDescriptor>()
        override val descriptors
            get() = mDescriptors.toList()

        override fun addDescriptor(
            descriptor: BluetoothGattDescriptor,
        ): Boolean {
            return if (fwkCharacteristic.addDescriptor(descriptor.fwkDescriptor)) {
                mDescriptors.add(descriptor)
                descriptor.characteristic = characteristic
                true
            } else {
                false
            }
        }

        override fun getDescriptor(uuid: UUID): BluetoothGattDescriptor? {
            return mDescriptors.firstOrNull {
                it.uuid == uuid
            }
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
                            bundle.getParcelable<FwkBluetoothGattCharacteristic>(
                                keyForField(FIELD_FWK_CHARACTERISTIC)
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
