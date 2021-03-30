/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.work.multiprocess

import android.content.Context
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.impl.utils.futures.SettableFuture
import com.google.common.util.concurrent.ListenableFuture

/**
 * A Remote Listenable Worker which always fails.
 */
public class RemoteFailureWorker(context: Context, workerParameters: WorkerParameters) :
    RemoteListenableWorker(context, workerParameters) {
    override fun startRemoteWork(): ListenableFuture<Result> {
        val future = SettableFuture.create<Result>()
        val result = Result.failure(outputData())
        future.set(result)
        return future
    }

    public companion object {
        public fun outputData(): Data {
            return Data.Builder()
                .put("output_1", 1)
                .put("output_2,", "test")
                .build()
        }
    }
}
