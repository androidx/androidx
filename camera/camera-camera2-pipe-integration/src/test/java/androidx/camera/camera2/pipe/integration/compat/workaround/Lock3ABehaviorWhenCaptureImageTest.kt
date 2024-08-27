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

package androidx.camera.camera2.pipe.integration.compat.workaround

import androidx.camera.camera2.pipe.Lock3ABehavior
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
class Lock3ABehaviorWhenCaptureImageTest {

    @Test
    fun getLock3ABehaviors_noCustomBehaviors_returnsDefaults() {
        val lock3ABehavior = Lock3ABehaviorWhenCaptureImage()
        val defaultAeBehavior = null
        val defaultAfBehavior = Lock3ABehavior.AFTER_NEW_SCAN
        val defaultAwbBehavior = Lock3ABehavior.AFTER_CURRENT_SCAN

        val (ae, af, awb) =
            lock3ABehavior.getLock3ABehaviors(
                defaultAeBehavior,
                defaultAfBehavior,
                defaultAwbBehavior
            )

        assertThat(ae).isEqualTo(defaultAeBehavior)
        assertThat(af).isEqualTo(defaultAfBehavior)
        assertThat(awb).isEqualTo(defaultAwbBehavior)
    }

    @Test
    fun getLock3ABehaviors_withCustomBehaviors_returnsCustom() {
        val customAeBehavior = null
        val customAfBehavior = Lock3ABehavior.AFTER_NEW_SCAN
        val customAwbBehavior = Lock3ABehavior.AFTER_CURRENT_SCAN
        val lock3ABehavior =
            Lock3ABehaviorWhenCaptureImage(
                hasAeLockBehavior = true,
                aeLockBehavior = customAeBehavior,
                hasAfLockBehavior = true,
                afLockBehavior = customAfBehavior,
                hasAwbLockBehavior = true,
                awbLockBehavior = customAwbBehavior
            )

        val (ae, af, awb) = lock3ABehavior.getLock3ABehaviors() // No defaults needed

        assertThat(ae).isEqualTo(customAeBehavior)
        assertThat(af).isEqualTo(customAfBehavior)
        assertThat(awb).isEqualTo(customAwbBehavior)
    }

    @Test
    fun getLock3ABehaviors_mixedBehaviors_returnsCorrectly() {
        val customAfBehavior = Lock3ABehavior.AFTER_NEW_SCAN
        val lock3ABehavior =
            Lock3ABehaviorWhenCaptureImage(
                hasAfLockBehavior = true,
                afLockBehavior = customAfBehavior
            )
        val defaultAeBehavior = null
        val defaultAwbBehavior = Lock3ABehavior.AFTER_CURRENT_SCAN

        val (ae, af, awb) =
            lock3ABehavior.getLock3ABehaviors(
                defaultAeBehavior,
                defaultAwbBehavior = defaultAwbBehavior
            )

        assertThat(ae).isEqualTo(defaultAeBehavior) // Default used
        assertThat(af).isEqualTo(customAfBehavior) // Custom used
        assertThat(awb).isEqualTo(defaultAwbBehavior) // Default used
    }
}
