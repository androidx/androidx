/*
 * Copyright 2024 The Android Open Source Project
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
package androidx.car.app.serialization

import android.os.RemoteException
import androidx.annotation.RestrictTo
import androidx.car.app.IOnDoneCallback
import androidx.car.app.OnDoneCallback
import androidx.car.app.annotations.CarProtocol
import androidx.car.app.annotations.ExperimentalCarApi
import androidx.car.app.annotations.KeepFields
import androidx.car.app.utils.RemoteUtils

/** Implementation for [ListDelegate] */
@ExperimentalCarApi
@CarProtocol
@KeepFields
@RestrictTo(RestrictTo.Scope.LIBRARY)
class ListDelegateImpl<T> : ListDelegate<T> {
    private var _size: Int = -1

    /**
     * The hash of the underlying list.
     *
     * This hash is used to determine whether two [ListDelegate]s contain the same items, without
     * needing to load every item in the list.
     */
    private var listHashCode: Int = -1

    private lateinit var mStub: IRemoteList

    constructor(content: List<T>) {
        _size = content.size
        listHashCode = content.hashCode()
        mStub = RemoteListStub<T>(content)
    }

    /** For Serialization */
    @Suppress("unused") private constructor()

    override val size
        get() = _size

    override fun requestItemRange(startIndex: Int, endIndex: Int, callback: OnDoneCallback) {
        assert(endIndex >= startIndex)
        assert(startIndex >= 0)
        assert(endIndex < size)

        try {
            mStub.requestItemRange(
                startIndex,
                endIndex,
                RemoteUtils.createOnDoneCallbackStub(callback)
            )
        } catch (e: RemoteException) {
            throw RuntimeException(e)
        }
    }

    override fun equals(other: Any?) =
        other is ListDelegateImpl<*> && other.listHashCode == listHashCode

    override fun hashCode(): Int = listHashCode

    private class RemoteListStub<T>(private val mContent: List<T>) : IRemoteList.Stub() {
        @Throws(RemoteException::class)
        override fun requestItemRange(startIndex: Int, endIndex: Int, callback: IOnDoneCallback) {
            RemoteUtils.dispatchCallFromHost(callback, "lazy load content") {
                // Send sublist to host, including both endpoints
                mContent.subList(startIndex, endIndex + 1)
            }
        }
    }
}
