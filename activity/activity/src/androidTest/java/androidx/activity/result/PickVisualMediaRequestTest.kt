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

package androidx.activity.result

import androidx.activity.result.contract.ActivityResultContracts
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import leakcanary.DetectLeaksAfterTestSuccess
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class PickVisualMediaRequestTest {

    @get:Rule
    val rule = DetectLeaksAfterTestSuccess()

    @Test
    fun buildPickVisualMedia() {
        val request = PickVisualMediaRequest.Builder().setMediaType(
            ActivityResultContracts.PickVisualMedia.VideoOnly
        ).build()
        assertThat(request.mediaType).isEqualTo(ActivityResultContracts.PickVisualMedia.VideoOnly)
    }

    @Test
    fun PickVisualMediaFun() {
        val request = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)

        assertThat(request.mediaType).isEqualTo(ActivityResultContracts.PickVisualMedia.VideoOnly)
    }
}
