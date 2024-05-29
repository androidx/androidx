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
class ListDelegateImpl : ListDelegate {
    private var _size: Int = -1
    private lateinit var mStub: IRemoteList

    constructor(content: List<*>) {
        _size = content.size
        mStub = RemoteListStub(content)
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

    private class RemoteListStub(private val mContent: List<*>) : IRemoteList.Stub() {
        @Throws(RemoteException::class)
        override fun requestItemRange(startIndex: Int, endIndex: Int, callback: IOnDoneCallback) {
            RemoteUtils.dispatchCallFromHost(callback, "lazy load content") {
                // Send sublist to host, including both endpoints
                mContent.subList(startIndex, endIndex + 1)
            }
        }
    }
}
