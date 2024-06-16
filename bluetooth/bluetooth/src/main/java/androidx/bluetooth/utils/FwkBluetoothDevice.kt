/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.bluetooth.utils

import android.bluetooth.BluetoothDevice as FwkBluetoothDevice
import android.os.Build
import android.os.Parcel
import androidx.annotation.RequiresApi
import androidx.bluetooth.BluetoothAddress

/** Address type random static bits value */
private const val ADDRESS_TYPE_RANDOM_STATIC_BITS_VALUE: Int = 3

/** Address type random resolvable bits value */
private const val ADDRESS_TYPE_RANDOM_RESOLVABLE_BITS_VALUE: Int = 1

/** Address type random non resolvable bits value */
private const val ADDRESS_TYPE_RANDOM_NON_RESOLVABLE_BITS_VALUE: Int = 0

// mAddressType is added to the parcel in API 34
internal fun FwkBluetoothDevice.addressType(): @BluetoothAddress.AddressType Int {
    return if (Build.VERSION.SDK_INT >= 34) {
        return addressType34()
    } else {
        BluetoothAddress.ADDRESS_TYPE_UNKNOWN
    }
}

@RequiresApi(34)
private fun FwkBluetoothDevice.addressType34(): @BluetoothAddress.AddressType Int {
    val parcel = Parcel.obtain()
    writeToParcel(parcel, 0)
    parcel.setDataPosition(0)
    parcel.readString() // Skip address
    val mAddressType = parcel.readInt()
    parcel.recycle()

    return when (mAddressType) {
        FwkBluetoothDevice.ADDRESS_TYPE_PUBLIC -> BluetoothAddress.ADDRESS_TYPE_PUBLIC
        FwkBluetoothDevice.ADDRESS_TYPE_RANDOM ->
            when (address.substring(0, 1).toInt(16).shr(2)) {
                ADDRESS_TYPE_RANDOM_STATIC_BITS_VALUE -> BluetoothAddress.ADDRESS_TYPE_RANDOM_STATIC
                ADDRESS_TYPE_RANDOM_RESOLVABLE_BITS_VALUE ->
                    BluetoothAddress.ADDRESS_TYPE_RANDOM_RESOLVABLE
                ADDRESS_TYPE_RANDOM_NON_RESOLVABLE_BITS_VALUE ->
                    BluetoothAddress.ADDRESS_TYPE_RANDOM_NON_RESOLVABLE
                else -> BluetoothAddress.ADDRESS_TYPE_UNKNOWN
            }
        FwkBluetoothDevice.ADDRESS_TYPE_UNKNOWN -> BluetoothAddress.ADDRESS_TYPE_UNKNOWN
        else -> BluetoothAddress.ADDRESS_TYPE_UNKNOWN
    }
}
