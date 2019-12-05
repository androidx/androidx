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
import androidx.test.platform.app.InstrumentationRegistry
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
        val v = View(InstrumentationRegistry.getInstrumentation().context)
        val dummyOwner = DummyLifecycleOwner()

        assertNull("initial LifecycleOwner expects null", v.findViewTreeLifecycleOwner())

        ViewTreeLifecycleOwner.set(v, dummyOwner)

        assertEquals("get the LifecycleOwner set directly",
            dummyOwner, v.findViewTreeLifecycleOwner())
    }

    /**
     * Tests that the owner set on a root of a subhierarchy is seen by both direct children
     * and other descendants
     */
    @Test
    fun getAncestorOwner() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val root = FrameLayout(context)
        val parent = FrameLayout(context)
        val child = View(context)
        root.addView(parent)
        parent.addView(child)

        assertNull("initial LifecycleOwner expects null", child.findViewTreeLifecycleOwner())

        val dummyOwner = DummyLifecycleOwner()
        ViewTreeLifecycleOwner.set(root, dummyOwner)

        assertEquals("root sees owner", dummyOwner, root.findViewTreeLifecycleOwner())
        assertEquals("direct child sees owner", dummyOwner, parent.findViewTreeLifecycleOwner())
        assertEquals("grandchild sees owner", dummyOwner, child.findViewTreeLifecycleOwner())
    }

    /**
     * Tests that a new owner set between a root and a descendant is seen by the descendant
     * instead of the root value
     */
    @Test
    fun shadowedOwner() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val root = FrameLayout(context)
        val parent = FrameLayout(context)
        val child = View(context)
        root.addView(parent)
        parent.addView(child)

        assertNull("initial LifecycleOwner expects null", child.findViewTreeLifecycleOwner())

        val rootDummyOwner = DummyLifecycleOwner()
        ViewTreeLifecycleOwner.set(root, rootDummyOwner)

        val parentDummyOwner = DummyLifecycleOwner()
        ViewTreeLifecycleOwner.set(parent, parentDummyOwner)

        assertEquals("root sees owner", rootDummyOwner, root.findViewTreeLifecycleOwner())
        assertEquals("direct child sees owner",
            parentDummyOwner, parent.findViewTreeLifecycleOwner())
        assertEquals("grandchild sees owner", parentDummyOwner, child.findViewTreeLifecycleOwner())
    }

    private class DummyLifecycleOwner : LifecycleOwner {
        override fun getLifecycle(): Lifecycle {
            throw UnsupportedOperationException("not a real LifecycleOwner")
        }
    }
}
