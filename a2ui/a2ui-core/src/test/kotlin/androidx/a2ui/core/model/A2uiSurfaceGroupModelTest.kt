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

package androidx.a2ui.core.model

import androidx.a2ui.core.catalog.A2uiCoreCatalog
import androidx.a2ui.core.platform.A2uiComponentRegistry
import androidx.a2ui.core.platform.A2uiDataModel
import androidx.a2ui.core.protocol.A2uiComponentPayload
import androidx.a2ui.core.protocol.A2uiDataPath
import androidx.a2ui.core.protocol.A2uiException
import com.google.common.testing.EqualsTester
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class A2uiSurfaceGroupModelTest {

    private companion object {
        const val SURFACE_ID_1 = "surf-1"
        const val SURFACE_ID_2 = "surf-2"
        const val SURFACE_PREFIX = "surf"
        const val NON_EXISTENT_ID = "non-existent"

        val emptyActionHandler: (androidx.a2ui.core.protocol.A2uiUserAction) -> Unit = {}
        val emptyErrorHandler: (androidx.a2ui.core.protocol.A2uiClientError) -> Unit = {}
    }

    @Test
    fun activeSurfaces_initially_isEmpty() {
        val group = A2uiSurfaceGroupModel()

        val activeSurfaces = group.activeSurfaces.value

        assertThat(activeSurfaces).isEmpty()
    }

    @Test
    fun add_newSurface_addsSurfaceAndUpdatesFlow() {
        val group = A2uiSurfaceGroupModel()
        val surface = createTestSurface(SURFACE_ID_1)

        val added = group.add(surface)

        assertThat(added).isTrue()
        assertThat(group.activeSurfaces.value).containsExactly(surface)
    }

    @Test
    fun add_existingId_replacesAndDisposesOldSurface() {
        val group = A2uiSurfaceGroupModel()
        val dataModel1 = TestDataModel()
        val registry1 = TestComponentRegistry()
        val surface1 = createTestSurface(SURFACE_ID_1, dataModel1, registry1)
        val surface2 = createTestSurface(SURFACE_ID_1)
        group.add(surface1)

        val added = group.add(surface2)

        assertThat(added).isTrue()
        assertThat(group.activeSurfaces.value).containsExactly(surface2)
        assertThat(group.getSurface(SURFACE_ID_1)).isSameInstanceAs(surface2)
        assertThat(dataModel1.isDisposed).isTrue()
        assertThat(registry1.isDisposed).isTrue()
    }

    @Test
    fun addAndDelete_concurrentDifferentIds_completesWithoutCorruption() = runBlocking {
        val group = A2uiSurfaceGroupModel()
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
    fun add_afterDispose_returnsFalse() {
        val group = A2uiSurfaceGroupModel()
        val surface = createTestSurface(SURFACE_ID_1)
        group.dispose()

        val added = group.add(surface)

        assertThat(added).isFalse()
        assertThat(group.activeSurfaces.value).isEmpty()
    }

    @Test
    fun delete_existingId_removesAndDisposesSurface() {
        val group = A2uiSurfaceGroupModel()
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
    fun delete_nonExistentId_doesNothing() {
        val group = A2uiSurfaceGroupModel()

        group.delete(NON_EXISTENT_ID)

        assertThat(group.activeSurfaces.value).isEmpty()
    }

    @Test
    fun delete_afterDispose_doesNothing() {
        val group = A2uiSurfaceGroupModel()
        val dataModel = TestDataModel()
        val registry = TestComponentRegistry()
        val surface = createTestSurface(SURFACE_ID_1, dataModel, registry)
        group.add(surface)
        group.dispose()

        group.delete(SURFACE_ID_1)

        assertThat(group.activeSurfaces.value).isEmpty()
    }

    @Test
    fun getSurface_existingId_returnsSurface() {
        val group = A2uiSurfaceGroupModel()
        val surface = createTestSurface(SURFACE_ID_1)
        group.add(surface)

        val result = group.getSurface(SURFACE_ID_1)

        assertThat(result).isSameInstanceAs(surface)
    }

    @Test
    fun getSurface_nonExistentId_returnsNull() {
        val group = A2uiSurfaceGroupModel()

        val result = group.getSurface(NON_EXISTENT_ID)

        assertThat(result).isNull()
    }

    @Test
    fun dispose_activeSurfaces_clearsAndDisposesAllSurfaces() {
        val group = A2uiSurfaceGroupModel()
        val dataModel1 = TestDataModel()
        val registry1 = TestComponentRegistry()
        val surface1 = createTestSurface(SURFACE_ID_1, dataModel1, registry1)
        val dataModel2 = TestDataModel()
        val registry2 = TestComponentRegistry()
        val surface2 = createTestSurface(SURFACE_ID_2, dataModel2, registry2)
        group.add(surface1)
        group.add(surface2)

        group.dispose()

        assertThat(group.activeSurfaces.value).isEmpty()
        assertThat(dataModel1.isDisposed).isTrue()
        assertThat(registry1.isDisposed).isTrue()
        assertThat(dataModel2.isDisposed).isTrue()
        assertThat(registry2.isDisposed).isTrue()
    }

    @Test
    fun dispose_calledMultipleTimes_isIdempotent() {
        val group = A2uiSurfaceGroupModel()
        val dataModel = TestDataModel()
        val registry = TestComponentRegistry()
        val surface = createTestSurface(SURFACE_ID_1, dataModel, registry)
        group.add(surface)

        group.dispose()
        group.dispose()

        assertThat(group.activeSurfaces.value).isEmpty()
        assertThat(dataModel.isDisposed).isTrue()
        assertThat(registry.isDisposed).isTrue()
    }

    @Test
    fun equalsAndHashCode_differentInstances_behavesCorrectly() {
        val group1 = A2uiSurfaceGroupModel()
        val group2 = A2uiSurfaceGroupModel()
        val group3 = A2uiSurfaceGroupModel()
        val surface = createTestSurface(SURFACE_ID_1)
        group1.add(surface)
        group2.add(surface)

        val equalsTester = EqualsTester().addEqualityGroup(group1, group2).addEqualityGroup(group3)

        equalsTester.testEquals()
    }

    @Test
    fun toString_withActiveSurfaces_containsSurfaceIds() {
        val group = A2uiSurfaceGroupModel()
        val surface = createTestSurface(SURFACE_ID_1)
        group.add(surface)

        val result = group.toString()

        assertThat(result).contains(SURFACE_ID_1)
    }

    private fun createTestSurface(
        id: String,
        dataModel: A2uiDataModel = TestDataModel(),
        componentRegistry: A2uiComponentRegistry = TestComponentRegistry(),
    ): A2uiSurfaceModel {
        return A2uiSurfaceModel(
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
        override fun equals(other: Any?): Boolean = other is TestCatalog

        override fun hashCode(): Int = TestCatalog::class.hashCode()

        override fun toString(): String = "TestCatalog"
    }

    private class TestDataModel : A2uiDataModel {
        var isDisposed = false

        override fun update(path: A2uiDataPath, value: Any?) {}

        override fun get(path: A2uiDataPath): Any? = null

        override fun dispose() {
            isDisposed = true
        }
    }

    private class TestComponentRegistry : A2uiComponentRegistry {
        var isDisposed = false

        override fun update(components: List<A2uiComponentPayload>) {}

        override fun reportError(id: String, exception: A2uiException) {}

        override fun dispose() {
            isDisposed = true
        }
    }
}
