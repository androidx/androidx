/*
 * Copyright (C) 2021 The Android Open Source Project
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

package androidx.health.services.client.impl.internal

import android.os.RemoteException
import androidx.health.services.client.data.ExerciseInfo
import androidx.health.services.client.impl.response.ExerciseInfoResponse
import com.google.common.util.concurrent.SettableFuture

/**
 * A callback for ipc invocations dealing with [ExerciseInfo].
 *
 * @hide
 */
public class ExerciseInfoCallback(private val resultFuture: SettableFuture<ExerciseInfo>) :
    IExerciseInfoCallback.Stub() {

    @Throws(RemoteException::class)
    override fun onExerciseInfo(response: ExerciseInfoResponse) {
        resultFuture.set(response.exerciseInfo)
    }

    @Throws(RemoteException::class)
    override fun onFailure(message: String) {
        resultFuture.setException(Exception(message))
    }
}
