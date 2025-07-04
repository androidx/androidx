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

package androidx.camera.integration.core

import android.os.Build
import androidx.camera.camera2.internal.GuaranteedConfigurationsUtil as Camera2GuaranteedConfigurationsUtil
import androidx.camera.camera2.pipe.integration.adapter.GuaranteedConfigurationsUtil
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class GuaranteedConfigurationsUtilTest {
    @Test
    fun queryableFcqCombinations_sameInBothCameraPipeAndCamera2() {
        val cameraPipeFcqCombinations = GuaranteedConfigurationsUtil.QUERYABLE_FCQ_COMBINATIONS
        val cameraCamera2FcqCombinations =
            Camera2GuaranteedConfigurationsUtil.generateQueryableFcqCombinations()

        assertThat(cameraPipeFcqCombinations.map { it.surfaceConfigList })
            .isEqualTo(cameraCamera2FcqCombinations.map { it.surfaceConfigList })
    }
}
