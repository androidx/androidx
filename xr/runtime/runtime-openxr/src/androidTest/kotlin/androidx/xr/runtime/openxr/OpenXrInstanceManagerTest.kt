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

package androidx.xr.runtime.openxr

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Test

// TODO - b/382119583: Remove the @SdkSuppress annotation once "androidx.xr.runtime.openxr.test"
// supports a lower SDK version.
@SdkSuppress(minSdkVersion = 29)
class OpenXrInstanceManagerTest {

    companion object {
        init {
            System.loadLibrary("androidx.xr.runtime.openxr.test")
        }
    }

    @Test
    fun initialize_setsXrInstanceHandleAndInstanceProcAddrToNonZero() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val provider = OpenXrInstanceManager()

        provider.initialize(context, emptyList())

        // The values below comes from kInstance a in
        // third_party/jetpack_xr_natives/common/openxr_stub.cc
        assertThat(provider.xrInstanceHandle).isEqualTo(1111L)
        assertThat(provider.xrInstanceProcAddr).isNotEqualTo(0L)
    }
}
