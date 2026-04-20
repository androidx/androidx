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

package androidx.xr.scenecore.spatial.core

import android.app.Activity
import androidx.xr.scenecore.testing.FakeScheduledExecutorService
import androidx.xr.scenecore.testing.MemoryUtils
import com.android.extensions.xr.XrExtensions
import com.android.extensions.xr.node.Node
import com.google.common.truth.Truth.assertThat
import java.lang.ref.WeakReference
import org.junit.Test

/**
 * Abstract base class for [AndroidXrEntity] implementation tests.
 *
 * It contains common tests for [AndroidXrEntity] such as garbage collection.
 */
abstract class AndroidXrEntityImplTest {
    protected abstract val activity: Activity
    protected abstract val xrExtensions: XrExtensions
    protected abstract val sceneNodeRegistry: SceneNodeRegistry
    protected abstract val fakeExecutor: FakeScheduledExecutorService

    /**
     * Creates a new [AndroidXrEntity] for testing.
     *
     * @param node The [Node] to use for the entity.
     * @return The created [AndroidXrEntity].
     */
    protected abstract fun createEntity(node: Node): AndroidXrEntity

    @Test
    fun garbageCollection_disposesEntity() {
        val node = xrExtensions.createNode()

        fun createWeakEntity(): WeakReference<AndroidXrEntity> {
            val entity = createEntity(node)
            return WeakReference(entity)
        }

        val entityRef = createWeakEntity()
        assertThat(entityRef.get()).isNotNull()

        MemoryUtils.assertGarbageCollected(
            entityRef,
            onAttempt = { fakeExecutor.runAll() },
            afterGcCondition = { sceneNodeRegistry.getEntityForNode(node) == null },
        )
    }
}
