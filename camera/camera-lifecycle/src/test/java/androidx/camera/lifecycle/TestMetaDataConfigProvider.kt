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

package androidx.camera.lifecycle

import androidx.camera.core.CameraXConfig
import androidx.camera.testing.fakes.FakeAppConfig
import kotlinx.atomicfu.atomic

internal class TestMetaDataConfigProvider : CameraXConfig.Provider {

    companion object {
        private val count = atomic(0)

        @JvmStatic
        val invokeCount: Int
            get() = count.value
    }

    override fun getCameraXConfig(): CameraXConfig {
        count += 1
        return FakeAppConfig.create()
    }
}
