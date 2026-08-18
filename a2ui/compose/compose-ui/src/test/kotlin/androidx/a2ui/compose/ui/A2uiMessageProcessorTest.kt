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

package androidx.a2ui.compose.ui

import androidx.a2ui.engine.processor.A2uiCoreMessageProcessor
import androidx.a2ui.model.processor.A2uiActionInterceptor
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class A2uiMessageProcessorTest {

    @Test
    fun factory_validCatalogsAndInterceptors_createsProcessorSuccessfully() {
        val catalog =
            A2uiCatalog(
                catalogId = "test_catalog",
                components = emptyList(),
                functions = emptyList(),
            )
        val interceptor = A2uiActionInterceptor { it }

        val processor =
            A2uiMessageProcessor(catalogs = listOf(catalog), interceptors = listOf(interceptor))

        assertThat(processor).isInstanceOf(A2uiCoreMessageProcessor::class.java)
    }
}
