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

package androidx.a2ui.engine.model

import androidx.a2ui.engine.catalog.A2uiCoreCatalog
import androidx.a2ui.engine.catalog.A2uiCoreComponentDefinition
import androidx.a2ui.engine.platform.A2uiCoreComponentRegistry
import androidx.a2ui.engine.platform.A2uiCoreDataModel
import androidx.a2ui.model.catalog.A2uiFunction
import androidx.a2ui.model.protocol.A2uiClientErrorMessage
import androidx.a2ui.model.protocol.A2uiComponentPayload
import androidx.a2ui.model.protocol.A2uiDataPath
import androidx.a2ui.model.protocol.A2uiException
import androidx.a2ui.model.protocol.A2uiUserAction
import androidx.a2ui.model.schema.A2uiSchema
import com.google.common.testing.EqualsTester
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class A2uiCoreSurfaceGroupModelTest {

    private companion object {
        const val SURFACE_ID_1 = "surf-1"
        const val SURFACE_PREFIX = "surf"
        const val NON_EXISTENT_ID = "non-existent"

        val emptyActionHandler: (A2uiUserAction) -> Unit = {}
        val emptyErrorHandler: (A2uiClientErrorMessage) -> Unit = {}
    }

    @Test
    fun activeSurfaces_initially_isEmpty() {
        val group = A2uiCoreSurfaceGroupModel()

        val activeSurfaces = group.activeSurfaces.value

        assertThat(activeSurfaces).isEmpty()
    }

    @Test
    fun add_newSurface_addsSurfaceAndUpdatesFlow() {
        val group = A2uiCoreSurfaceGroupModel()
        val surface = createTestSurface(SURFACE_ID_1)

        group.add(surface)

        assertThat(group.activeSurfaces.value).containsExactly(surface)
    }

    @Test
    fun add_existingId_throwsIllegalArgumentException() {
        val group = A2uiCoreSurfaceGroupModel()
        val dataModel1 = TestDataModel()
        val registry1 = TestComponentRegistry()
        val surface1 = createTestSurface(SURFACE_ID_1, dataModel1, registry1)
        val surface2 = createTestSurface(SURFACE_ID_1)
        group.add(surface1)

        val exception = assertFailsWith<IllegalArgumentException> { group.add(surface2) }

        assertThat(exception).hasMessageThat().contains("Surface '$SURFACE_ID_1' already exists.")
        assertThat(group.activeSurfaces.value).containsExactly(surface1)
        assertThat(group.getSurface(SURFACE_ID_1)).isSameInstanceAs(surface1)
    }

    @Test
    fun add_concurrentDifferentIds_addsAllSurfaces() = runBlocking {
        val group = A2uiCoreSurfaceGroupModel()
        val numCoroutines = 50

        val jobs =
            List(numCoroutines) { index ->
                launch(Dispatchers.Default) {
                    val id = "$SURFACE_PREFIX-$index"
                    val surface = createTestSurface(id)
                    group.add(surface)
                }
            }
        jobs.joinAll()

        val activeSurfaces = group.activeSurfaces.value
        assertThat(activeSurfaces).hasSize(numCoroutines)

        for (i in 0 until numCoroutines) {
            assertThat(group.getSurface("$SURFACE_PREFIX-$i")).isNotNull()
        }
    }

    @Test
    fun delete_concurrentDifferentIds_removesAllSurfaces() = runBlocking {
        val group = A2uiCoreSurfaceGroupModel()
        val numCoroutines = 50

        // Pre-populate
        for (i in 0 until numCoroutines) {
            group.add(createTestSurface("$SURFACE_PREFIX-$i"))
        }

        val jobs =
            List(numCoroutines) { index ->
                launch(Dispatchers.Default) {
                    val id = "$SURFACE_PREFIX-$index"
                    group.delete(id)
                }
            }
        jobs.joinAll()

        val activeSurfaces = group.activeSurfaces.value
        assertThat(activeSurfaces).isEmpty()
    }

    @Test
    fun addAndDelete_concurrentDifferentIds_completesWithoutCorruption() = runBlocking {
        val group = A2uiCoreSurfaceGroupModel()
        val numCoroutines = 50
        val numOperationsPerCoroutine = 100

        val jobs =
            List(numCoroutines) { index ->
                launch(Dispatchers.Default) {
                    val id = "$SURFACE_PREFIX-$index"
                    repeat(numOperationsPerCoroutine) {
                        val surface = createTestSurface(id)
                        group.add(surface)
                        group.delete(id)
                    }
                }
            }
        jobs.joinAll()

        val activeSurfaces = group.activeSurfaces.value

        assertThat(activeSurfaces).isEmpty()
    }

    @Test
    fun getSurface_concurrentAccess_returnsCorrectly() = runBlocking {
        val group = A2uiCoreSurfaceGroupModel()
        val surface1 = createTestSurface(SURFACE_ID_1)
        val surface2 = createTestSurface("surf-2")
        group.add(surface1)
        group.add(surface2)

        val numCoroutines = 100
        val numOperationsPerCoroutine = 100

        val jobs =
            List(numCoroutines) {
                launch(Dispatchers.Default) {
                    repeat(numOperationsPerCoroutine) {
                        assertThat(group.getSurface(SURFACE_ID_1)).isSameInstanceAs(surface1)
                        assertThat(group.getSurface("surf-2")).isSameInstanceAs(surface2)
                        assertThat(group.getSurface(NON_EXISTENT_ID)).isNull()
                    }
                }
            }
        jobs.joinAll()
    }

    @Test
    fun delete_nonExistentId_doesNothing() {
        val group = A2uiCoreSurfaceGroupModel()

        group.delete(NON_EXISTENT_ID)

        assertThat(group.activeSurfaces.value).isEmpty()
    }

    @Test
    fun delete_existingId_removesAndDisposesSurface() {
        val group = A2uiCoreSurfaceGroupModel()
        val dataModel = TestDataModel()
        val registry = TestComponentRegistry()
        val surface = createTestSurface(SURFACE_ID_1, dataModel, registry)
        group.add(surface)

        group.delete(SURFACE_ID_1)

        assertThat(group.activeSurfaces.value).isEmpty()
        assertThat(group.getSurface(SURFACE_ID_1)).isNull()
        assertThat(dataModel.isDisposed).isTrue()
        assertThat(registry.isDisposed).isTrue()
    }

    @Test
    fun clear_removesAllSurfacesAndReturnsThem() {
        val group = A2uiCoreSurfaceGroupModel()
        val dataModel1 = TestDataModel()
        val registry1 = TestComponentRegistry()
        val surface1 = createTestSurface("id-1", dataModel1, registry1)
        val dataModel2 = TestDataModel()
        val registry2 = TestComponentRegistry()
        val surface2 = createTestSurface("id-2", dataModel2, registry2)

        group.add(surface1)
        group.add(surface2)

        val removed = group.clear()

        assertThat(group.activeSurfaces.value).isEmpty()
        assertThat(removed).containsExactly(surface1, surface2)
    }

    @Test
    fun getSurface_existingId_returnsSurface() {
        val group = A2uiCoreSurfaceGroupModel()
        val surface = createTestSurface(SURFACE_ID_1)
        group.add(surface)

        val result = group.getSurface(SURFACE_ID_1)

        assertThat(result).isSameInstanceAs(surface)
    }

    @Test
    fun getSurface_nonExistentId_returnsNull() {
        val group = A2uiCoreSurfaceGroupModel()

        val result = group.getSurface(NON_EXISTENT_ID)

        assertThat(result).isNull()
    }

    @Test
    fun equalsAndHashCode_differentInstances_behavesCorrectly() {
        val group1 = A2uiCoreSurfaceGroupModel()
        val group2 = A2uiCoreSurfaceGroupModel()
        val group3 = A2uiCoreSurfaceGroupModel()
        val surface = createTestSurface(SURFACE_ID_1)
        group1.add(surface)
        group2.add(surface)

        val equalsTester = EqualsTester().addEqualityGroup(group1, group2).addEqualityGroup(group3)

        equalsTester.testEquals()
    }

    @Test
    fun toString_withActiveSurfaces_containsSurfaceIds() {
        val group = A2uiCoreSurfaceGroupModel()
        val surface = createTestSurface(SURFACE_ID_1)
        group.add(surface)

        val result = group.toString()

        assertThat(result).contains(SURFACE_ID_1)
    }

    private fun createTestSurface(
        id: String,
        dataModel: A2uiCoreDataModel = TestDataModel(),
        componentRegistry: A2uiCoreComponentRegistry = TestComponentRegistry(),
    ): A2uiCoreSurfaceModel {
        return A2uiCoreSurfaceModel(
            id = id,
            catalog = TestCatalog(),
            theme = emptyMap(),
            shouldSendDataModel = false,
            dataModel = dataModel,
            componentRegistry = componentRegistry,
            onDispatchAction = emptyActionHandler,
            onDispatchError = emptyErrorHandler,
        )
    }

    private class TestCatalog : A2uiCoreCatalog {
        override val id: String = "test_catalog"
        override val componentDefinitions: List<A2uiCoreComponentDefinition> = emptyList()
        override val functions: List<A2uiFunction> = emptyList()
        override val themeSchema: A2uiSchema? = null

        override fun getComponentDefinition(name: String): A2uiCoreComponentDefinition? = null

        override fun getFunction(name: String): A2uiFunction? = null

        override fun equals(other: Any?): Boolean = other is TestCatalog

        override fun hashCode(): Int = TestCatalog::class.hashCode()

        override fun toString(): String = "TestCatalog"
    }

    private class TestDataModel : A2uiCoreDataModel {
        var isDisposed = false

        override fun update(path: A2uiDataPath, value: Any?) {}

        override fun get(path: A2uiDataPath): Any? = null

        override fun close() {
            isDisposed = true
        }
    }

    private class TestComponentRegistry : A2uiCoreComponentRegistry {
        var isDisposed = false

        override fun update(components: List<A2uiComponentPayload>) {}

        override fun reportError(id: String, exception: A2uiException) {}

        override fun close() {
            isDisposed = true
        }
    }
}
