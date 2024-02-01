/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.tv.foundation.text

import androidx.compose.ui.text.input.PlatformImeOptions
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.tv.foundation.ExperimentalTvFoundationApi
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTvFoundationApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class TvImeOptionsTest {
    @Test
    fun privateImeOptions_keyboardAlignment() {
        val privateImeOptions = "testOptions"
        val keyboardAlignment = TvKeyboardAlignment.Left
        val imeOptions = PlatformImeOptions(privateImeOptions).keyboardAlignment(keyboardAlignment)

        assertThat(
            imeOptions.privateImeOptions == "$privateImeOptions,${keyboardAlignment.option}"
        ).isTrue()
    }
}
