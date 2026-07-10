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

package androidx.a2ui.model.catalog.functions

import androidx.a2ui.model.protocol.A2uiException
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class A2uiOpenUrlFunctionTest {

    @Test
    fun execute_validUrl_evaluatesCorrectly() {
        var callCount = 0
        var openedUrl: String? = null
        val openUrlMock = A2uiOpenUrlFunction { url ->
            callCount++
            openedUrl = url
        }

        assertThat(openUrlMock.execute(mapOf(ARG_URL to "https://example.com"))).isEqualTo(Unit)
        assertThat(openedUrl).isEqualTo("https://example.com")
        assertThat(callCount).isEqualTo(1)
    }

    @Test
    fun execute_missingUrl_throwsValidationException() {
        var callCount = 0
        val openUrlMock = A2uiOpenUrlFunction { _ -> callCount++ }

        assertThrows(A2uiException.A2uiValidationException::class.java) {
            openUrlMock.execute(emptyMap())
        }
        assertThat(callCount).isEqualTo(0)
    }

    @Test
    fun execute_emptyUrl_throwsRuntimeException() {
        var callCount = 0
        val openUrlMock = A2uiOpenUrlFunction { _ -> callCount++ }

        assertThrows(A2uiException.A2uiRuntimeException::class.java) {
            openUrlMock.execute(mapOf(ARG_URL to ""))
        }
        assertThat(callCount).isEqualTo(0)
    }

    private companion object {
        private const val ARG_URL = "url"
    }
}
