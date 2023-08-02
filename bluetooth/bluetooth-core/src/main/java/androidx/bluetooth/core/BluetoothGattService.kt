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

import android.bluetooth.BluetoothGattService as FwkBluetoothGattService
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.bluetooth.core.utils.Bundleable
import androidx.bluetooth.core.utils.Utils
import java.util.UUID

/**
 * @hide
 */
class BluetoothGattService internal constructor(service: FwkBluetoothGattService) :
    Bundleable {

    companion object {
        /**
         * Primary service
         */
        const val SERVICE_TYPE_PRIMARY = FwkBluetoothGattService.SERVICE_TYPE_PRIMARY

        /**
         * Secondary service (included by primary services)
         */
        const val SERVICE_TYPE_SECONDARY = FwkBluetoothGattService.SERVICE_TYPE_SECONDARY
        /**
         * A companion object to create [BluetoothGattService] from bundle
         */
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

    /**
     * Create a new BluetoothGattService.
     *
     * @param uuid The UUID for this service
     * @param type The type of this service,
     * {@link BluetoothGattService#SERVICE_TYPE_PRIMARY}
     * or {@link BluetoothGattService#SERVICE_TYPE_SECONDARY}
     */
    constructor(uuid: UUID, type: Int = SERVICE_TYPE_PRIMARY) : this(
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

    /**
     * The underlying framework's [android.bluetooth.BluetoothGattService]
     */
    internal val fwkService: FwkBluetoothGattService
        get() = impl.fwkService

    /**
     * Service's instanceId
     */
    val instanceId: Int
        get() = impl.instanceId

    /**
     * Service's type (Primary or Secondary)
     */
    val type: Int
        get() = impl.type

    /**
     * Service's universal unique identifier
     */
    val uuid: UUID
        get() = impl.uuid

    /**
     * List of [BluetoothGattCharacteristic] that this service include
     */
    val characteristics: List<BluetoothGattCharacteristic>
        get() = impl.characteristics

    /**
     * List of [BluetoothGattService] that this service include
     */
    val includedServices: List<BluetoothGattService>
        get() = impl.includedServices

    /**
     * Add a characteristic to this service
     *
     * @param characteristic The [BluetoothGattCharacteristic] to be included
     */
    fun addCharacteristic(characteristic: BluetoothGattCharacteristic): Boolean {
        return impl.addCharacteristic(characteristic)
    }

    /**
     * Returns a characteristic with a given UUID out of the list of
     * characteristics offered by this service.
     *
     * <p>This is a convenience function to allow access to a given characteristic
     * without enumerating over the list returned by {@link #getCharacteristics}
     * manually.
     *
     * <p>If a remote service offers multiple characteristics with the same
     * UUID, the first instance of a characteristic with the given UUID
     * is returned.
     *
     * @return GATT characteristic object or null if no characteristic with the given UUID was
     * found.
     */
    fun getCharacteristic(uuid: UUID): BluetoothGattCharacteristic? {
        return impl.getCharacteristic(uuid)
    }

    /**
     * Add a included service to this service
     *
     * @param service The [BluetoothGattService] to be included
     */
    fun addService(service: BluetoothGattService): Boolean {
        return impl.addService(service)
    }

    /**
     * Returns a [BluetoothGattService] with a given UUID out of the list of included
     * services offered by this service.
     *
     * <p>If a remote service offers multiple [BluetoothGattService] with the same
     * UUID, the first instance of a [BluetoothGattService] with the given UUID
     * is returned.
     *
     * @return GATT [BluetoothGattService] object or null if no service with the given UUID
     * was found.
     */
    fun getIncludedService(uuid: UUID): BluetoothGattService? {
        return impl.getIncludedService(uuid)
    }

    /**
     * Create a [Bundle] from this object.
     */
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
        private val service: BluetoothGattService
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
                characteristic.service = service
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
                    BluetoothGattService(it.uuid, it.type).toBundle()
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