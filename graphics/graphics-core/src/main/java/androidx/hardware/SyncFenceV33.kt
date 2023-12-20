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

package androidx.hardware

import android.hardware.SyncFence
import android.os.Build
import androidx.annotation.RequiresApi
import java.time.Duration

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal class SyncFenceV33 internal constructor(syncFence: SyncFence) : SyncFenceImpl {
    internal val mSyncFence: SyncFence = syncFence

    /**
     * See [SyncFenceImpl.await]
     */
    override fun await(timeoutNanos: Long): Boolean {
        return mSyncFence.await(Duration.ofNanos(timeoutNanos))
    }

    /**
     * See [SyncFenceImpl.awaitForever]
     */
    override fun awaitForever(): Boolean {
        return mSyncFence.awaitForever()
    }

    /**
     * See [SyncFenceImpl.close]
     */
    override fun close() {
        mSyncFence.close()
    }

    /**
     * See [SyncFenceImpl.getSignalTimeNanos]
     */
    override fun getSignalTimeNanos(): Long {
        return mSyncFence.signalTime
    }

    /**
     * Checks if the SyncFence object is valid.
     * @return `true` if it is valid, `false` otherwise
     */
    override fun isValid(): Boolean {
        return mSyncFence.isValid
    }
}
