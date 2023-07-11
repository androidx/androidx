/*
 * Copyright 2019 The Android Open Source Project
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
package androidx.lifecycle

import android.view.View
import android.widget.FrameLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class ViewTreeLifecycleOwnerTest {
    /**
     * Tests that a direct set/get on a single view survives a round trip
     */
    @Test
    fun setGetSameView() {
        val v = View(getInstrumentation().context)
        val fakeOwner = FakeLifecycleOwner()
        assertNull("initial LifecycleOwner expects null", v.findViewTreeLifecycleOwner())
        v.setViewTreeLifecycleOwner(fakeOwner)
        assertEquals(
            "get the LifecycleOwner set directly",
            fakeOwner,
            v.findViewTreeLifecycleOwner()
        )
    }

    /**
     * Tests that the owner set on a root of a sub-hierarchy is seen by both direct children
     * and other descendants
     */
    @Test
    fun getAncestorOwner() {
        val context = getInstrumentation().context
        val root = FrameLayout(context)
        val parent = FrameLayout(context)
        val child = View(context)
        root.addView(parent)
        parent.addView(child)
        assertNull(
            "initial LifecycleOwner expects null",
            child.findViewTreeLifecycleOwner()
        )
        val fakeOwner = FakeLifecycleOwner()
        root.setViewTreeLifecycleOwner(fakeOwner)
        assertEquals(
            "root sees owner",
            fakeOwner,
            root.findViewTreeLifecycleOwner()
        )
        assertEquals(
            "direct child sees owner",
            fakeOwner,
            parent.findViewTreeLifecycleOwner()
        )
        assertEquals(
            "grandchild sees owner",
            fakeOwner,
            child.findViewTreeLifecycleOwner()
        )
    }

    /**
     * Tests that a new owner set between a root and a descendant is seen by the descendant
     * instead of the root value
     */
    @Test
    fun shadowedOwner() {
        val context = getInstrumentation().context
        val root = FrameLayout(context)
        val parent = FrameLayout(context)
        val child = View(context)
        root.addView(parent)
        parent.addView(child)
        assertNull(
            "initial LifecycleOwner expects null",
            child.findViewTreeLifecycleOwner()
        )
        val rootFakeOwner = FakeLifecycleOwner()
        root.setViewTreeLifecycleOwner(rootFakeOwner)
        val parentFakeOwner = FakeLifecycleOwner()
        parent.setViewTreeLifecycleOwner(parentFakeOwner)
        assertEquals(
            "root sees owner",
            rootFakeOwner,
            root.findViewTreeLifecycleOwner()
        )
        assertEquals(
            "direct child sees owner",
            parentFakeOwner,
            parent.findViewTreeLifecycleOwner()
        )
        assertEquals(
            "grandchild sees owner",
            parentFakeOwner,
            child.findViewTreeLifecycleOwner()
        )
    }

    internal class FakeLifecycleOwner : LifecycleOwner {
        override val lifecycle = NoOpLifecycle()
    }

    internal class NoOpLifecycle : Lifecycle() {
        override fun addObserver(observer: LifecycleObserver) {}
        override fun removeObserver(observer: LifecycleObserver) {}
        // use arbitrary State
        override val currentState = State.RESUMED
    }
}
