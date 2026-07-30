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

package androidx.a2ui.compose.runtime

import androidx.a2ui.engine.catalog.A2uiCoreCatalog
import androidx.a2ui.engine.catalog.A2uiCoreComponentDefinitionCollection
import androidx.a2ui.engine.model.A2uiCoreSurfaceModel
import androidx.a2ui.engine.processor.A2uiCoreMessageProcessor
import androidx.a2ui.model.catalog.A2uiFunctionCollection
import androidx.a2ui.model.processor.A2uiActionInterceptor
import androidx.a2ui.model.protocol.A2uiCreateSurfaceMessage
import androidx.a2ui.model.schema.A2uiSchema
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertIs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class A2uiRuntimeMessageProcessorTest {

    @Test
    fun factory_validCatalog_createsProcessor() {
        val catalog = ValidTestCatalog("test-catalog")

        val processor = a2uiRuntimeMessageProcessor(catalogs = listOf(catalog))

        assertThat(processor).isNotNull()
        assertThat(processor).isInstanceOf(A2uiCoreMessageProcessor::class.java)
    }

    @Test
    fun factory_emptyCatalogs_createsProcessor() {
        val processor = a2uiRuntimeMessageProcessor(catalogs = emptyList())

        assertThat(processor).isNotNull()
    }

    @Test
    fun factory_invalidCatalog_throws() {
        val invalidCatalog = object : A2uiRuntimeCatalog {}

        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                a2uiRuntimeMessageProcessor(catalogs = listOf(invalidCatalog))
            }

        assertThat(exception)
            .hasMessageThat()
            .contains("A2uiRuntimeCatalog must be a valid core A2uiCoreCatalog")
    }

    @Test
    fun factory_initializesDataModelAndComponentRegistry() = runTest {
        val catalog = ValidTestCatalog("test-catalog")
        val processor = a2uiRuntimeMessageProcessor(catalogs = listOf(catalog))
        val surfaceId = "test_surface"

        val job = launch { processor.collectMessages() }

        try {
            // Trigger surface creation so the processor invokes the configured Compose factories
            processor.processMessage(A2uiCreateSurfaceMessage(surfaceId, "test-catalog"))
            advanceUntilIdle()

            val surface = assertIs<A2uiCoreSurfaceModel>(processor.activeSurfaces.value.first())
            assertThat(surface).isNotNull()

            assertThat(surface.dataModel).isInstanceOf(A2uiDataModel::class.java)
            assertThat(surface.componentRegistry).isInstanceOf(A2uiComponentRegistry::class.java)
        } finally {
            job.cancel()
        }
    }

    @Test
    fun factory_passesInterceptorsToCoreProcessor() = runTest {
        var intercepted = false
        val interceptor = A2uiActionInterceptor { action ->
            intercepted = true
            action
        }
        val catalog = ValidTestCatalog("test-catalog")
        val processor =
            a2uiRuntimeMessageProcessor(
                catalogs = listOf(catalog),
                interceptors = listOf(interceptor),
            )
        val surfaceId = "test_surface"
        val job = launch { processor.collectMessages() }

        try {
            processor.processMessage(A2uiCreateSurfaceMessage(surfaceId, "test-catalog"))
            advanceUntilIdle()

            val surface = assertIs<A2uiCoreSurfaceModel>(processor.activeSurfaces.value.first())
            surface.dispatchAction(
                componentId = "button",
                actionDefinition = mapOf("event" to mapOf("name" to "click")),
            )
            advanceUntilIdle()

            assertThat(intercepted).isTrue()
        } finally {
            job.cancel()
        }
    }

    private class ValidTestCatalog(override val id: String) : A2uiRuntimeCatalog, A2uiCoreCatalog {
        override val componentDefinitions = A2uiCoreComponentDefinitionCollection()
        override val functions = A2uiFunctionCollection()
        override val themeSchema: A2uiSchema? = null
    }
}
