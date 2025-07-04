/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.navigationevent

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.viewtree.setViewTreeDisjointParent
import androidx.kruth.assertWithMessage
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class ViewTreeNavigationEventDispatcherTest {

    /** Tests that a direct set/get on a single view survives a round trip */
    @Test
    fun setGetSameView() {
        val v = View(InstrumentationRegistry.getInstrumentation().context)
        val fakeOwner: NavigationEventDispatcherOwner = FakeNavigationEventDispatcherOwner()
        assertWithMessage("Initial NavigationEventDispatcherOwner should be null")
            .that(v.findViewTreeNavigationEventDispatcherOwner())
            .isNull()

        v.setViewTreeNavigationEventDispatcherOwner(fakeOwner)
        assertWithMessage("Get returns the NavigationEventDispatcherOwner set directly")
            .that(v.findViewTreeNavigationEventDispatcherOwner())
            .isSameInstanceAs(fakeOwner)
    }

    /**
     * Tests that the owner set on a root of a subhierarchy is seen by both direct children and
     * other descendants
     */
    @Test
    fun ancestorOwner() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val root: ViewGroup = FrameLayout(context)
        val parent: ViewGroup = FrameLayout(context)
        val child = View(context)
        root.addView(parent)
        parent.addView(child)
        assertWithMessage("Initial NavigationEventDispatcherOwner should be null")
            .that(child.findViewTreeNavigationEventDispatcherOwner())
            .isNull()

        val fakeOwner: NavigationEventDispatcherOwner = FakeNavigationEventDispatcherOwner()
        root.setViewTreeNavigationEventDispatcherOwner(fakeOwner)

        assertWithMessage("Get on root returns the NavigationEventDispatcherOwner")
            .that(root.findViewTreeNavigationEventDispatcherOwner())
            .isSameInstanceAs(fakeOwner)
        assertWithMessage("Get on direct child returns the NavigationEventDispatcherOwner")
            .that(root.findViewTreeNavigationEventDispatcherOwner())
            .isSameInstanceAs(fakeOwner)
        assertWithMessage("Get on grandchild returns the NavigationEventDispatcherOwner")
            .that(root.findViewTreeNavigationEventDispatcherOwner())
            .isSameInstanceAs(fakeOwner)
    }

    /**
     * Tests that a new owner set between a root and a descendant is seen by the descendant instead
     * of the root value
     */
    @Test
    fun shadowedOwner() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val root: ViewGroup = FrameLayout(context)
        val parent: ViewGroup = FrameLayout(context)
        val child = View(context)
        root.addView(parent)
        parent.addView(child)
        assertWithMessage("Initial NavigationEventDispatcherOwner should be null")
            .that(child.findViewTreeNavigationEventDispatcherOwner())
            .isNull()

        val rootFakeOwner: NavigationEventDispatcherOwner = FakeNavigationEventDispatcherOwner()
        root.setViewTreeNavigationEventDispatcherOwner(rootFakeOwner)
        val parentFakeOwner: NavigationEventDispatcherOwner = FakeNavigationEventDispatcherOwner()
        parent.setViewTreeNavigationEventDispatcherOwner(parentFakeOwner)

        assertWithMessage("Get on root returns the root NavigationEventDispatcherOwner")
            .that(root.findViewTreeNavigationEventDispatcherOwner())
            .isSameInstanceAs(rootFakeOwner)
        assertWithMessage("Get on direct child returns the NavigationEventDispatcherOwner")
            .that(parent.findViewTreeNavigationEventDispatcherOwner())
            .isSameInstanceAs(parentFakeOwner)
        assertWithMessage("Get on grandchild returns the NavigationEventDispatcherOwner")
            .that(child.findViewTreeNavigationEventDispatcherOwner())
            .isSameInstanceAs(parentFakeOwner)
    }

    @Test
    fun disjointParentOwner() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val root = FrameLayout(context)
        val disjointParent = FrameLayout(context)
        val parent = FrameLayout(context)
        val child = View(context)

        root.addView(disjointParent)
        parent.addView(child)
        parent.setViewTreeDisjointParent(disjointParent)

        val rootFakeOwner = FakeNavigationEventDispatcherOwner()
        root.setViewTreeNavigationEventDispatcherOwner(rootFakeOwner)

        Assert.assertEquals(
            "disjoint parent sees owner",
            rootFakeOwner,
            parent.findViewTreeNavigationEventDispatcherOwner(),
        )
        Assert.assertEquals(
            "disjoint child sees owner",
            rootFakeOwner,
            child.findViewTreeNavigationEventDispatcherOwner(),
        )
    }

    @Test
    fun shadowedDisjointParentOwner() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val root = FrameLayout(context)
        val disjointParent = FrameLayout(context)
        val parent = FrameLayout(context)
        val child = View(context)

        root.addView(disjointParent)
        parent.addView(child)
        parent.setViewTreeDisjointParent(disjointParent)

        val rootFakeOwner = FakeNavigationEventDispatcherOwner()
        val parentFakeOwner = FakeNavigationEventDispatcherOwner()
        root.setViewTreeNavigationEventDispatcherOwner(rootFakeOwner)
        parent.setViewTreeNavigationEventDispatcherOwner(parentFakeOwner)

        Assert.assertEquals(
            "child sees owner",
            parentFakeOwner,
            child.findViewTreeNavigationEventDispatcherOwner(),
        )
    }

    private class FakeNavigationEventDispatcherOwner : NavigationEventDispatcherOwner {
        override val navigationEventDispatcher
            get() = throw UnsupportedOperationException("not a real NavigationEventDispatcher")
    }
}
