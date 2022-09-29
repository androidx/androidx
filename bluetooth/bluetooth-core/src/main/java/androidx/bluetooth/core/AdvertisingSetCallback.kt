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

import android.bluetooth.le.AdvertisingSetCallback as FwkAdvertisingSetCallback
import android.bluetooth.le.AdvertisingSet as FwkAdvertisingSet

/**
 * Bluetooth LE advertising set callbacks, used to deliver advertising operation
 * status.
 *
 * @hide
 */
interface AdvertisingSetCallback {

    companion object {
        /**
         * The requested operation was successful.
         */
        const val ADVERTISE_SUCCESS =
            FwkAdvertisingSetCallback.ADVERTISE_SUCCESS

        /**
         * Failed to start advertising as the advertise data to be broadcasted is too
         * large.
         */
        const val ADVERTISE_FAILED_DATA_TOO_LARGE =
            FwkAdvertisingSetCallback.ADVERTISE_FAILED_DATA_TOO_LARGE

        /**
         * Failed to start advertising because no advertising instance is available.
         */
        const val ADVERTISE_FAILED_TOO_MANY_ADVERTISERS =
            FwkAdvertisingSetCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS

        /**
         * Failed to start advertising as the advertising is already started.
         */
        const val ADVERTISE_FAILED_ALREADY_STARTED =
            FwkAdvertisingSetCallback.ADVERTISE_FAILED_ALREADY_STARTED

        /**
         * Operation failed due to an internal error.
         */
        const val ADVERTISE_FAILED_INTERNAL_ERROR =
            FwkAdvertisingSetCallback.ADVERTISE_FAILED_INTERNAL_ERROR

        /**
         * This feature is not supported on this platform.
         */
        const val ADVERTISE_FAILED_FEATURE_UNSUPPORTED =
            FwkAdvertisingSetCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED
    }

    /**
     * Callback triggered in response to [BluetoothLeAdvertiser.startAdvertisingSet]
     * indicating result of the operation. If status is ADVERTISE_SUCCESS, then advertisingSet
     * contains the started set and it is advertising. If error occurred, advertisingSet is
     * null, and status will be set to proper error code.
     *
     * @param advertisingSet The advertising set that was started or null if error.
     * @param txPower tx power that will be used for this set.
     * @param status Status of the operation.
     */
    // TODO(ofy) Change FwkAdvertisingSet to core.AdvertisingSet when it is available
    fun onAdvertisingSetStarted(advertisingSet: FwkAdvertisingSet, txPower: Int, status: Int) {}

    /**
     * Callback triggered in response to [BluetoothLeAdvertiser.stopAdvertisingSet]
     * indicating advertising set is stopped.
     *
     * @param advertisingSet The advertising set.
     */
    // TODO(ofy) Change FwkAdvertisingSet to core.AdvertisingSet when it is available
    fun onAdvertisingSetStopped(advertisingSet: FwkAdvertisingSet) {}

    /**
     * Callback triggered in response to [BluetoothLeAdvertiser.startAdvertisingSet]
     * indicating result of the operation. If status is ADVERTISE_SUCCESS, then advertising set is
     * advertising.
     *
     * @param advertisingSet The advertising set.
     * @param status Status of the operation.
     */
    // TODO(ofy) Change FwkAdvertisingSet to core.AdvertisingSet when it is available
    fun onAdvertisingEnabled(advertisingSet: FwkAdvertisingSet, enable: Boolean, status: Int) {}

    /**
     * Callback triggered in response to [AdvertisingSet.setAdvertisingData] indicating
     * result of the operation. If status is ADVERTISE_SUCCESS, then data was changed.
     *
     * @param advertisingSet The advertising set.
     * @param status Status of the operation.
     */
    // TODO(ofy) Change FwkAdvertisingSet to core.AdvertisingSet when it is available
    fun onAdvertisingDataSet(advertisingSet: FwkAdvertisingSet, status: Int) {}

    /**
     * Callback triggered in response to [AdvertisingSet.setAdvertisingData] indicating
     * result of the operation.
     *
     * @param advertisingSet The advertising set.
     * @param status Status of the operation.
     */
    // TODO(ofy) Change FwkAdvertisingSet to core.AdvertisingSet when it is available
    fun onScanResponseDataSet(advertisingSet: FwkAdvertisingSet, status: Int) {}

    /**
     * Callback triggered in response to [AdvertisingSet.setAdvertisingParameters]
     * indicating result of the operation.
     *
     * @param advertisingSet The advertising set.
     * @param txPower tx power that will be used for this set.
     * @param status Status of the operation.
     */
    // TODO(ofy) Change FwkAdvertisingSet to core.AdvertisingSet when it is available
    fun onAdvertisingParametersUpdated(
        advertisingSet: FwkAdvertisingSet,
        txPower: Int,
        status: Int
    ) {
    }

    /**
     * Callback triggered in response to [AdvertisingSet.setPeriodicAdvertisingParameters]
     * indicating result of the operation.
     *
     * @param advertisingSet The advertising set.
     * @param status Status of the operation.
     */
    // TODO(ofy) Change FwkAdvertisingSet to core.AdvertisingSet when it is available
    fun onPeriodicAdvertisingParametersUpdated(advertisingSet: FwkAdvertisingSet, status: Int) {}

    /**
     * Callback triggered in response to [AdvertisingSet.setPeriodicAdvertisingData]
     * indicating result of the operation.
     *
     * @param advertisingSet The advertising set.
     * @param status Status of the operation.
     */
    // TODO(ofy) Change FwkAdvertisingSet to core.AdvertisingSet when it is available
    fun onPeriodicAdvertisingDataSet(advertisingSet: FwkAdvertisingSet, status: Int) {}

    /**
     * Callback triggered in response to [AdvertisingSet.setPeriodicAdvertisingEnabled]
     * indicating result of the operation.
     *
     * @param advertisingSet The advertising set.
     * @param status Status of the operation.
     */
    // TODO(ofy) Change FwkAdvertisingSet to core.AdvertisingSet when it is available
    fun onPeriodicAdvertisingEnabled(
        advertisingSet: FwkAdvertisingSet,
        enable: Boolean,
        status: Int
    ) {
    }

    /**
     * Callback triggered in response to [AdvertisingSet.getOwnAddress]
     * indicating result of the operation.
     *
     * @param advertisingSet The advertising set.
     * @param addressType type of address.
     * @param address advertising set bluetooth address.
     * @hide
     */
    // TODO(ofy) Change FwkAdvertisingSet to core.AdvertisingSet when it is available
    fun onOwnAddressRead(advertisingSet: FwkAdvertisingSet, addressType: Int, address: String?) {}
}
