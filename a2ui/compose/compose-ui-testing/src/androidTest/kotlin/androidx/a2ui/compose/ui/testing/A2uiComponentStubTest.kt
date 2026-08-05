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

package androidx.a2ui.compose.ui.testing

import androidx.a2ui.compose.runtime.A2uiComponentState
import androidx.a2ui.compose.runtime.observeA2uiComponentState
import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.a2ui.engine.model.A2uiCoreSurfaceModel
import androidx.a2ui.model.protocol.A2uiComponentPayload
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertIs
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class A2uiComponentStubTest {

    @Test
    fun withId_createsIdStub() = runComposeUiTest {
        val stub = A2uiComponentStub.withId("my_id") { _, _ -> }

        assertIs<IdStubImpl>(stub)
        assertThat(stub.id).isEqualTo("my_id")
        assertThat(stub.content).isNotNull()
        assertThat(evaluateIsReady(stub)).isTrue()
    }

    @Test
    fun withId_withIsReady_storesIsReady() = runComposeUiTest {
        val stub = A2uiComponentStub.withId("my_id", isReady = { false }) { _, _ -> }

        assertIs<IdStubImpl>(stub)
        assertThat(stub.id).isEqualTo("my_id")
        assertThat(evaluateIsReady(stub)).isFalse()
    }

    @Test
    fun withType_createsTypeStub() = runComposeUiTest {
        val stub = A2uiComponentStub.withType("MyType") { _, _ -> }

        assertIs<TypeStubImpl>(stub)
        assertThat(stub.type).isEqualTo("MyType")
        assertThat(stub.content).isNotNull()
        assertThat(evaluateIsReady(stub)).isTrue()
    }

    @Test
    fun withType_withIsReady_storesIsReady() = runComposeUiTest {
        val stub = A2uiComponentStub.withType("MyType", isReady = { false }) { _, _ -> }

        assertIs<TypeStubImpl>(stub)
        assertThat(stub.type).isEqualTo("MyType")
        assertThat(evaluateIsReady(stub)).isFalse()
    }

    private suspend fun ComposeUiTest.evaluateIsReady(stub: A2uiComponentStubImpl): Boolean {
        val controller =
            A2uiTestController(
                catalog = A2uiCatalog(catalogId = "test_catalog", components = emptyList()),
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "StubType",
                            properties = emptyMap(),
                        )
                    ),
                componentStubs = listOf(A2uiComponentStub.withType("StubType") { _, _ -> }),
            )
        val surface = controller.start() as A2uiCoreSurfaceModel
        var result: Boolean? = null
        setContent {
            val state = observeA2uiComponentState(surface) as A2uiComponentState.Success
            result = stub.isReady(state.component.scope, state.component.properties)
        }
        return result!!
    }
}
