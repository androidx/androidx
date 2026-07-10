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

package androidx.xr.scenecore.runtime.impl

import android.app.Activity
import android.content.Context
import android.os.Looper
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.runtime.CleanupAction
import androidx.xr.scenecore.runtime.Entity
import androidx.xr.scenecore.runtime.InputEventListener
import androidx.xr.scenecore.testing.MemoryUtils
import com.google.common.truth.Truth.assertThat
import java.lang.ref.WeakReference
import java.util.concurrent.Executor
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BaseEntityTest {

    private open class TestBaseEntity(context: Context) : BaseEntity(context) {
        override val activitySpacePose: Pose = Pose()
        override val worldSpaceScale: Vector3 = Vector3(1f, 1f, 1f)
        override val activitySpaceScale: Vector3 = Vector3(1f, 1f, 1f)

        override fun addInputEventListener(executor: Executor?, listener: InputEventListener) {}

        override fun removeInputEventListener(listener: InputEventListener) {}

        fun hasContext(): Boolean = context != null

        fun publicRegisterCleanup(executor: Executor, action: CleanupAction) {
            registerCleanup(executor, action)
        }
    }

    @Test
    fun disposeIsIdempotent() {
        val context = mock<Context>()
        val executor = Executor { it.run() }
        val cleanupAction = mock<CleanupAction>()
        val entity = TestBaseEntity(context)
        entity.publicRegisterCleanup(executor, cleanupAction)

        entity.dispose()
        entity.dispose()

        // Cleanup action should only run once
        verify(cleanupAction, times(1)).run()
    }

    @Test
    fun disposeDetachesFromParent() {
        val context = mock<Context>()
        val parent = TestBaseEntity(context)
        val child = TestBaseEntity(context)
        parent.addChild(child)

        child.dispose()

        assertThat(child.parent).isNull()
        assertThat(parent.children).isEmpty()
    }

    @Test
    fun disposeHandlesRootEntitiesGracefully() {
        // Mock a root entity that throws on setParent(null) if called blindly
        class RootEntity(context: Context) : TestBaseEntity(context) {
            override var parent: Entity?
                get() = null
                set(value) {
                    if (value != null)
                        throw UnsupportedOperationException("Root cannot have parent")
                    // Some root entities in the codebase throw even for null if not handled
                    // carefully
                    throw UnsupportedOperationException("Cannot set parent on Root")
                }
        }

        val context = mock<Context>()
        val root = RootEntity(context)

        // This should not throw because dispose() should only call parent = null if parent is NOT
        // null.
        root.dispose()
    }

    @Test
    fun baseEntityIsCleanedWhenUnreachable() {
        val context = mock<Context>()

        fun createEntity(): WeakReference<TestBaseEntity> {
            val entity = TestBaseEntity(context)
            // Ensure it's not attached to anything (default is null parent in this test class)
            return WeakReference(entity)
        }

        val entityRef = createEntity()
        assertNotNull(entityRef.get())

        MemoryUtils.assertGarbageCollected(entityRef)

        assertNull("BaseEntity should be garbage collected", entityRef.get())
    }

    @Test
    fun detachedNestedTreeIsGarbageCollected() {
        val context = mock<Context>()

        fun createNestedTree(): Pair<WeakReference<TestBaseEntity>, WeakReference<TestBaseEntity>> {
            val parent = TestBaseEntity(context)
            val child = TestBaseEntity(context)
            parent.addChild(child)
            return Pair(WeakReference(parent), WeakReference(child))
        }

        val (parentRef, childRef) = createNestedTree()
        assertNotNull(parentRef.get())
        assertNotNull(childRef.get())

        // Both should be collected if the root (parent) is unreachable
        MemoryUtils.assertGarbageCollected(parentRef)
        MemoryUtils.assertGarbageCollected(childRef)
    }

    @Test
    fun rootEntityIsGarbageCollected() {
        // Some entities like AnchorEntity are roots (parent is always null)
        class RootEntity(context: Context) : TestBaseEntity(context) {
            override var parent: Entity?
                get() = null
                set(_) {}
        }

        val context = mock<Context>()

        fun createRoot(): WeakReference<RootEntity> = WeakReference(RootEntity(context))

        val rootRef = createRoot()
        assertNotNull(rootRef.get())
        MemoryUtils.assertGarbageCollected(rootRef)
    }

    @Test
    fun baseEntityDisposeClearsContext() {
        val context = mock<Context>()
        val entity = TestBaseEntity(context)
        assertThat(entity.hasContext()).isTrue()

        entity.dispose()
        assertThat(entity.hasContext()).isFalse()
    }

    @Test
    fun baseEntityIsGarbageCollectedAndRemovesAccessibilityView() {
        val activity = Robolectric.buildActivity(Activity::class.java).create().start().get()
        val mainLayout = activity.window.decorView as ViewGroup

        var entity: TestBaseEntity? = TestBaseEntity(activity)
        val weakRef = WeakReference(entity)

        // Trigger accessibility view creation
        entity?.contentDescription = "test description"

        // Verify accessibility view was added to decorView
        var accessibilityLayout: ViewGroup? = null
        for (i in 0 until mainLayout.childCount) {
            val child = mainLayout.getChildAt(i)
            if (child is FrameLayout) {
                accessibilityLayout = child as ViewGroup
                break
            }
        }
        assertThat(accessibilityLayout).isNotNull()

        // Nullify and GC
        @Suppress("UNUSED_VALUE")
        entity = null

        // Verify cleanup: accessibilityLayout is removed from decorView
        // Robolectric should run main thread tasks.
        MemoryUtils.assertGarbageCollected(
            weakRef,
            onAttempt = { shadowOf(Looper.getMainLooper()).idle() },
            afterGcCondition = { mainLayout.indexOfChild(accessibilityLayout) == -1 },
        )
    }

    @Test
    fun entityIsRemovedFromRegistryWhenGc() {
        val registry = BaseSceneNodeRegistry<String>()

        fun createEntity(): Pair<WeakReference<Entity>, String> {
            val entity = mock<Entity>()
            val node = "test-node"
            registry.setEntityForNode(node, entity)
            return Pair(WeakReference(entity), node)
        }

        val (weakRef, node) = createEntity()
        assertThat(weakRef.get()).isNotNull()
        assertThat(registry.getEntityForNode(node)).isNotNull()

        MemoryUtils.assertGarbageCollected(weakRef)

        assertThat(registry.getEntityForNode(node)).isNull()
    }

    @Test
    fun disposeIsNotRecursive() {
        val context = mock<Context>()
        val parent = TestBaseEntity(context)
        val child = TestBaseEntity(context)
        parent.addChild(child)

        parent.dispose()

        // Parent should be disposed (context cleared)
        assertThat(parent.hasContext()).isFalse()
        // Child should NOT be disposed (context still there)
        assertThat(child.hasContext()).isTrue()
        // Child should be detached
        assertThat(child.parent).isNull()
        assertThat(parent.children).isEmpty()
    }
}
