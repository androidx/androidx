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

package androidx.camera.integration.core.util

import android.content.Context
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.fakes.FakeAppConfig
import com.google.common.truth.Truth
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

fun getFakeConfigCameraProvider(context: Context): ProcessCameraProvider {
    var cameraProvider: ProcessCameraProvider? = null
    val latch = CountDownLatch(1)
    ProcessCameraProvider.configureInstance(FakeAppConfig.create())
    ProcessCameraProvider.getInstance(context)
        .addListener(
            {
                cameraProvider = ProcessCameraProvider.getInstance(context).get()
                latch.countDown()
            },
            CameraXExecutors.directExecutor()
        )

    Truth.assertWithMessage("ProcessCameraProvider.getInstance timed out!")
        .that(latch.await(5, TimeUnit.SECONDS))
        .isTrue()

    return cameraProvider!!
}
