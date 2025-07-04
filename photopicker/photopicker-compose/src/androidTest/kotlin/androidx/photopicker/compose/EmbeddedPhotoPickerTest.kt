/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.photopicker.compose

import android.os.Build
import androidx.annotation.RequiresExtension
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.IntSize
import androidx.photopicker.testing.TestEmbeddedPhotoPickerProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@RequiresExtension(extension = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, version = 15)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA)
class EmbeddedPhotoPickerTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    @ExperimentalPhotoPickerComposeApi
    fun testEmbeddedPhotoPickerProvidesSurfaceHostTokenToState() = runTest {
        val testProvider = TestEmbeddedPhotoPickerProvider.get()
        lateinit var state: EmbeddedPhotoPickerState

        composeTestRule.setContent {
            state = rememberEmbeddedPhotoPickerState()
            EmbeddedPhotoPicker(state = state, provider = testProvider)
        }

        composeTestRule.waitUntil(5_000L, { state.surfaceHostToken != null })
        assertThat(state.surfaceHostToken).isNotNull()
    }

    @Test
    @ExperimentalPhotoPickerComposeApi
    fun testEmbeddedPhotoPickerProvidesSurfaceSizeToState() = runTest {
        val testProvider = TestEmbeddedPhotoPickerProvider.get()
        lateinit var state: EmbeddedPhotoPickerStateImpl

        composeTestRule.setContent {
            state = rememberEmbeddedPhotoPickerState() as EmbeddedPhotoPickerStateImpl
            EmbeddedPhotoPicker(state = state, provider = testProvider)
        }

        composeTestRule.waitUntil(5_000L, { state.surfaceSize != IntSize.Zero })
        assertThat(state.surfaceSize).isNotEqualTo(IntSize.Zero)
    }

    @Test
    @ExperimentalPhotoPickerComposeApi
    fun testEmbeddedPhotoPickerOpensSession() = runTest {
        val testProvider = TestEmbeddedPhotoPickerProvider.get()

        composeTestRule.setContent {
            EmbeddedPhotoPicker(state = rememberEmbeddedPhotoPickerState(), provider = testProvider)
        }

        composeTestRule.waitUntil(5_000L, { testProvider.sessions.isNotEmpty() })
        val session = testProvider.sessions.first()
        assertThat(session).isNotNull()
    }
}
