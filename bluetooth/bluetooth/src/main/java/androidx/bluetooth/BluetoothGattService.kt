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
import android.bluetooth.BluetoothGattService as FwkBluetoothGattService
import androidx.annotation.RequiresApi
import androidx.bluetooth.utils.Bundleable
import androidx.bluetooth.utils.Utils

import java.util.UUID

/**
 * @hide
 */
class BluetoothGattService internal constructor(service: FwkBluetoothGattService) :
    Bundleable {

    companion object {
        val CREATOR: Bundleable.Creator<BluetoothGattService> =
            if (Build.VERSION.SDK_INT >= 24) {
                GattServiceImplApi24.CREATOR
            } else {
                GattServiceImplApi21.CREATOR
            }

        internal fun keyForField(field: Int): String {
            return field.toString(Character.MAX_RADIX)
        }
    }

    constructor(uuid: UUID, type: Int) : this(
        FwkBluetoothGattService(
            uuid,
            type
        )
    )

    private val impl =
        if (Build.VERSION.SDK_INT >= 24) {
            GattServiceImplApi24(service, this)
        } else {
            GattServiceImplApi21(service, this)
        }

    internal val fwkService: FwkBluetoothGattService
        get() = impl.fwkService
    val instanceId: Int
        get() = impl.instanceId
    val type: Int
        get() = impl.type
    val uuid: UUID
        get() = impl.uuid

    val characteristics: List<BluetoothGattCharacteristic>
        get() = impl.characteristics

    val includedServices: List<BluetoothGattService>
        get() = impl.includedServices

    fun addCharacteristic(characteristic: BluetoothGattCharacteristic): Boolean {
        return impl.addCharacteristic(characteristic)
    }

    fun getCharacteristic(uuid: UUID): BluetoothGattCharacteristic? {
        return impl.getCharacteristic(uuid)
    }

    fun addService(service: BluetoothGattService): Boolean {
        return impl.addService(service)
    }

    fun getIncludedService(uuid: UUID): BluetoothGattService? {
        return impl.getIncludedService(uuid)
    }

    override fun toBundle(): Bundle {
        return impl.toBundle()
    }

    private interface GattServiceImpl {
        val fwkService: FwkBluetoothGattService
        val instanceId: Int
        val type: Int
        val uuid: UUID
        val includedServices: List<BluetoothGattService>
        val characteristics: List<BluetoothGattCharacteristic>

        fun addCharacteristic(characteristic: BluetoothGattCharacteristic): Boolean

        fun getCharacteristic(uuid: UUID): BluetoothGattCharacteristic?

        fun addService(service: BluetoothGattService): Boolean

        fun getIncludedService(uuid: UUID): BluetoothGattService?

        fun toBundle(): Bundle
    }

    private open class GattServiceImplApi21(
        final override val fwkService: FwkBluetoothGattService,
        service: BluetoothGattService
    ) : GattServiceImpl {

        companion object {
            internal const val FIELD_FWK_SERVICE_INSTANCE_ID = 1
            internal const val FIELD_FWK_SERVICE_TYPE = 2
            internal const val FIELD_FWK_SERVICE_CHARACTERISTICS = 3
            internal const val FIELD_FWK_SERVICE_SERVICES = 4
            internal const val FIELD_FWK_SERVICE_UUID = 5

            val CREATOR: Bundleable.Creator<BluetoothGattService> =
                object : Bundleable.Creator<BluetoothGattService> {
                    override fun fromBundle(bundle: Bundle): BluetoothGattService {
                        val uuid = bundle.getString(keyForField(FIELD_FWK_SERVICE_UUID))
                            ?: throw IllegalArgumentException("Bundle doesn't include uuid")
                        val instanceId =
                            bundle.getInt(keyForField(FIELD_FWK_SERVICE_INSTANCE_ID), 0)
                        val type = bundle.getInt(keyForField(FIELD_FWK_SERVICE_TYPE), -1)
                        if (type == -1) {
                            throw IllegalArgumentException("Bundle doesn't include service type")
                        }

                        val fwkService = FwkBluetoothGattService::class.java.getConstructor(
                            UUID::class.java,
                            Integer.TYPE,
                            Integer.TYPE,
                        ).newInstance(UUID.fromString(uuid), instanceId, type)

                        val gattService = BluetoothGattService(fwkService)

                        Utils.getParcelableArrayListFromBundle(
                            bundle,
                            keyForField(FIELD_FWK_SERVICE_CHARACTERISTICS),
                            Bundle::class.java
                        ).forEach {
                            gattService.addCharacteristic(
                                BluetoothGattCharacteristic.CREATOR.fromBundle(
                                    it
                                )
                            )
                        }

                        Utils.getParcelableArrayListFromBundle(
                            bundle,
                            keyForField(FIELD_FWK_SERVICE_SERVICES),
                            Bundle::class.java
                        ).forEach {
                            val includedServices = Utils.getParcelableArrayListFromBundle(
                                it,
                                keyForField(FIELD_FWK_SERVICE_SERVICES),
                                Bundle::class.java
                            )
                            if (includedServices.isNotEmpty()) {
                                throw IllegalArgumentException(
                                    "Included service shouldn't pass " +
                                        "its included services to bundle"
                                )
                            }

                            val includedCharacteristics = Utils.getParcelableArrayListFromBundle(
                                it,
                                keyForField(FIELD_FWK_SERVICE_CHARACTERISTICS),
                                Bundle::class.java
                            )

                            if (includedCharacteristics.isNotEmpty()) {
                                throw IllegalArgumentException(
                                    "Included service shouldn't pass characteristic to bundle"
                                )
                            }

                            gattService.addService(BluetoothGattService.CREATOR.fromBundle(it))
                        }

                        return gattService
                    }
                }
        }

        override val instanceId: Int
            get() = fwkService.instanceId
        override val type: Int
            get() = fwkService.type
        override val uuid: UUID
            get() = fwkService.uuid

        private val _includedServices = mutableListOf<BluetoothGattService>()
        override val includedServices: List<BluetoothGattService>
            get() = _includedServices.toList()

        private val _characteristics = mutableListOf<BluetoothGattCharacteristic>()
        override val characteristics
            get() = _characteristics.toList()

        init {
            this.fwkService.characteristics.forEach {
                val characteristic = BluetoothGattCharacteristic(it)
                _characteristics.add(characteristic)
                characteristic.service = service
            }
            this.fwkService.includedServices.forEach {
                _includedServices.add(BluetoothGattService(it))
            }
        }

        override fun addCharacteristic(characteristic: BluetoothGattCharacteristic): Boolean {
            return if (fwkService.addCharacteristic(characteristic.fwkCharacteristic)) {
                _characteristics.add(characteristic)
                true
            } else {
                false
            }
        }

        override fun getCharacteristic(uuid: UUID): BluetoothGattCharacteristic? {
            return _characteristics.firstOrNull {
                it.uuid == uuid
            }
        }

        override fun addService(service: BluetoothGattService): Boolean {
            return if (fwkService.addService(service.fwkService)) {
                _includedServices.add(service)
                true
            } else {
                false
            }
        }

        override fun getIncludedService(uuid: UUID): BluetoothGattService? {
            return _includedServices.firstOrNull {
                it.uuid == uuid
            }
        }

        override fun toBundle(): Bundle {
            val bundle = Bundle()
            bundle.putString(keyForField(FIELD_FWK_SERVICE_UUID), uuid.toString())
            bundle.putInt(keyForField(FIELD_FWK_SERVICE_INSTANCE_ID), instanceId)
            bundle.putInt(keyForField(FIELD_FWK_SERVICE_TYPE), type)
            bundle.putParcelableArrayList(
                keyForField(FIELD_FWK_SERVICE_CHARACTERISTICS),
                ArrayList(_characteristics.map { it.toBundle() })
            )
            bundle.putParcelableArrayList(
                keyForField(FIELD_FWK_SERVICE_SERVICES),

                // Cut all included services & characteristics of included services. Developers
                // should directly send the included services if need it
                ArrayList(_includedServices.map {
                    BluetoothGattService(it.uuid,
                                         it.type).toBundle()
                })
            )

            return bundle
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private class GattServiceImplApi24(
        fwkService: FwkBluetoothGattService,
        service: BluetoothGattService
    ) : GattServiceImplApi21(fwkService, service) {

        companion object {
            internal const val FIELD_FWK_SERVICE = 0

            @Suppress("DEPRECATION")
            val CREATOR: Bundleable.Creator<BluetoothGattService> =
                object : Bundleable.Creator<BluetoothGattService> {
                    override fun fromBundle(bundle: Bundle): BluetoothGattService {
                        val fwkService =
                            Utils.getParcelableFromBundle(
                                bundle,
                                keyForField(FIELD_FWK_SERVICE),
                                FwkBluetoothGattService::class.java
                            )
                                ?: throw IllegalArgumentException(
                                    "Bundle doesn't contain framework service"
                                )
                        return BluetoothGattService(fwkService)
                    }
                }
        }

        override fun toBundle(): Bundle {
            val bundle = Bundle()
            bundle.putParcelable(keyForField(FIELD_FWK_SERVICE), fwkService)
            return bundle
        }
    }
}