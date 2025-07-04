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

package androidx.compose.ui.test.junit4

import android.os.Bundle
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.core.os.bundleOf
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class StateRestorationTesterTest {

    @get:Rule val rule = createComposeRule()

    @Before
    fun setUp() {
        IS_PLATFORM_ENCODING_AND_DECODING_ENABLED = true
    }

    @After
    fun tearDown() {
        IS_PLATFORM_ENCODING_AND_DECODING_ENABLED = false
    }

    @Test
    fun emulateSavedInstanceStateRestore_maxBytes() {
        with(StateRestorationTester(rule)) {
            val expectedState = ByteArray(size = ARRAY_BYTES_MAX_SIZE - 100)
            var actualState: ByteArray? = null
            setContent { actualState = rememberSaveable { expectedState } }

            emulateSavedInstanceStateRestore()

            assertThat(actualState).isNotSameInstanceAs(expectedState)
            assertThat(actualState?.size).isEqualTo(expectedState.size)
        }
    }

    @Test
    fun emulateSavedInstanceStateRestore_tooLargeException() {
        with(StateRestorationTester(rule)) {
            setContent {
                // Explicitly define type argument to avoid 'kotlin.Unit cannot be saved' error.
                @Suppress("RemoveExplicitTypeArguments")
                rememberSaveable<ByteArray> { ByteArray(size = ARRAY_BYTES_MAX_SIZE + 100) }
            }

            assertThrows(IllegalStateException::class.java) { emulateSavedInstanceStateRestore() }
        }
    }

    @Test
    fun emulateSavedInstanceStateRestore_encodesParcelable() {
        with(StateRestorationTester(rule)) {
            // Bundle is a Parcelable.
            val expectedState = bundleOf("KEY" to Int.MIN_VALUE)
            var actualState: Bundle? = null
            setContent { actualState = rememberSaveable { expectedState } }

            emulateSavedInstanceStateRestore()

            assertThat(actualState).isNotSameInstanceAs(expectedState)
            assertThat(actualState?.getInt("KEY")).isEqualTo(Int.MIN_VALUE)
        }
    }

    private companion object {

        /**
         * Maximum ByteArray size that can be saved with [rememberSaveable].
         *
         * This is based on Android's 1MB per [Bundle] limit, without accounting for the overhead
         * from [rememberSaveable] storing state in an `ArrayList`.
         */
        private const val ARRAY_BYTES_MAX_SIZE = 1024 * 1024
    }
}
