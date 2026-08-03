/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.scenecore.openxr

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = 29)
@LargeTest
@RunWith(AndroidJUnit4::class)
class SceneCoreOpenXrNativeTest {

    companion object {
        init {
            System.loadLibrary("androidx.xr.scenecore.openxr.test")
        }
    }

    @Test
    fun initialize_setsNativeScenecoreHandleToNonZero() {
        val nativeWrapper = SceneCoreOpenXrNative()

        assertThat(nativeWrapper.nativeScenecore).isNotEqualTo(0L)
    }

    @Test
    fun destroy_cleansUpHandleAndSetsToZero() {
        val nativeWrapper = SceneCoreOpenXrNative()

        nativeWrapper.destroy()

        assertThat(nativeWrapper.nativeScenecore).isEqualTo(0L)
    }

    @Test
    fun destroy_multipleTimes_isIdempotent() {
        val nativeWrapper = SceneCoreOpenXrNative()

        nativeWrapper.destroy()
        nativeWrapper.destroy()

        assertThat(nativeWrapper.nativeScenecore).isEqualTo(0L)
    }

    @Test
    fun useBlock_autoCloseable_destroysHandle() {
        var wrapperRef: SceneCoreOpenXrNative? = null
        SceneCoreOpenXrNative().use { wrapper ->
            wrapperRef = wrapper
            assertThat(wrapper.nativeScenecore).isNotEqualTo(0L)
        }
        assertThat(wrapperRef?.nativeScenecore).isEqualTo(0L)
    }

    @Test
    fun multipleInstances_createAndDestroy_succeeds() {
        val instance1 = SceneCoreOpenXrNative()
        val instance2 = SceneCoreOpenXrNative()

        assertThat(instance1.nativeScenecore).isNotEqualTo(0L)
        assertThat(instance2.nativeScenecore).isNotEqualTo(0L)

        instance1.destroy()
        instance2.destroy()

        assertThat(instance1.nativeScenecore).isEqualTo(0L)
        assertThat(instance2.nativeScenecore).isEqualTo(0L)
    }
}
