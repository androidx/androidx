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

package androidx.window.area

import androidx.window.core.ExperimentalWindowApi
import androidx.window.extensions.area.WindowAreaComponent
import org.junit.Test

/**
 * Unit tests for [WindowAreaStatus] that run on the JVM.
 */
@OptIn(ExperimentalWindowApi::class)
class WindowAreaStatusUnitTest {

    @Test
    fun testWindowAreaStatusTranslateValueAvailable() {
        val expected = WindowAreaStatus.AVAILABLE
        val translateValue = WindowAreaStatus.translate(WindowAreaComponent.STATUS_AVAILABLE)
        assert(expected == translateValue)
    }

    @Test
    fun testWindowAreaStatusTranslateValueUnavailable() {
        val expected = WindowAreaStatus.UNAVAILABLE
        val translateValue = WindowAreaStatus.translate(WindowAreaComponent.STATUS_UNAVAILABLE)
        assert(expected == translateValue)
    }

    @Test
    fun testWindowAreaStatusTranslateValueUnsupported() {
        val expected = WindowAreaStatus.UNSUPPORTED
        val translateValue = WindowAreaStatus.translate(WindowAreaComponent.STATUS_UNSUPPORTED)
        assert(expected == translateValue)
    }
}