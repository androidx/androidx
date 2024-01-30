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
package androidx.activity

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertWithMessage
import leakcanary.DetectLeaksAfterTestSuccess
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class ViewTreeOnBackPressedDispatcherTest {

    @get:Rule
    val rule = DetectLeaksAfterTestSuccess()

    /**
     * Tests that a direct set/get on a single view survives a round trip
     */
    @Test
    fun setGetSameView() {
        val v = View(InstrumentationRegistry.getInstrumentation().context)
        val fakeOwner: OnBackPressedDispatcherOwner = FakeOnBackPressedDispatcherOwner()
        assertWithMessage("Initial OnBackPressedDispatcherOwner should be null")
            .that(v.findViewTreeOnBackPressedDispatcherOwner())
            .isNull()

        v.setViewTreeOnBackPressedDispatcherOwner(fakeOwner)
        assertWithMessage("Get returns the OnBackPressedDispatcherOwner set directly")
            .that(v.findViewTreeOnBackPressedDispatcherOwner())
            .isSameInstanceAs(fakeOwner)
    }

    /**
     * Tests that the owner set on a root of a subhierarchy is seen by both direct children
     * and other descendants
     */
    @Test
    fun ancestorOwner() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val root: ViewGroup = FrameLayout(context)
        val parent: ViewGroup = FrameLayout(context)
        val child = View(context)
        root.addView(parent)
        parent.addView(child)
        assertWithMessage("Initial OnBackPressedDispatcherOwner should be null")
            .that(child.findViewTreeOnBackPressedDispatcherOwner())
            .isNull()

        val fakeOwner: OnBackPressedDispatcherOwner = FakeOnBackPressedDispatcherOwner()
        root.setViewTreeOnBackPressedDispatcherOwner(fakeOwner)

        assertWithMessage("Get on root returns the OnBackPressedDispatcherOwner")
            .that(root.findViewTreeOnBackPressedDispatcherOwner())
            .isSameInstanceAs(fakeOwner)
        assertWithMessage("Get on direct child returns the OnBackPressedDispatcherOwner")
            .that(root.findViewTreeOnBackPressedDispatcherOwner())
            .isSameInstanceAs(fakeOwner)
        assertWithMessage("Get on grandchild returns the OnBackPressedDispatcherOwner")
            .that(root.findViewTreeOnBackPressedDispatcherOwner())
            .isSameInstanceAs(fakeOwner)
    }

    /**
     * Tests that a new owner set between a root and a descendant is seen by the descendant
     * instead of the root value
     */
    @Test
    fun shadowedOwner() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val root: ViewGroup = FrameLayout(context)
        val parent: ViewGroup = FrameLayout(context)
        val child = View(context)
        root.addView(parent)
        parent.addView(child)
        assertWithMessage("Initial OnBackPressedDispatcherOwner should be null")
            .that(child.findViewTreeOnBackPressedDispatcherOwner())
            .isNull()

        val rootFakeOwner: OnBackPressedDispatcherOwner = FakeOnBackPressedDispatcherOwner()
        root.setViewTreeOnBackPressedDispatcherOwner(rootFakeOwner)
        val parentFakeOwner: OnBackPressedDispatcherOwner = FakeOnBackPressedDispatcherOwner()
        parent.setViewTreeOnBackPressedDispatcherOwner(parentFakeOwner)

        assertWithMessage("Get on root returns the root OnBackPressedDispatcherOwner")
            .that(root.findViewTreeOnBackPressedDispatcherOwner())
            .isSameInstanceAs(rootFakeOwner)
        assertWithMessage("Get on direct child returns the OnBackPressedDispatcherOwner")
            .that(parent.findViewTreeOnBackPressedDispatcherOwner())
            .isSameInstanceAs(parentFakeOwner)
        assertWithMessage("Get on grandchild returns the OnBackPressedDispatcherOwner")
            .that(child.findViewTreeOnBackPressedDispatcherOwner())
            .isSameInstanceAs(parentFakeOwner)
    }

    private class FakeOnBackPressedDispatcherOwner : OnBackPressedDispatcherOwner {
        override val lifecycle: Lifecycle
            get() = throw UnsupportedOperationException("not a real OnBackPressedDispatcherOwner")

        override val onBackPressedDispatcher
            get() = throw UnsupportedOperationException("not a real OnBackPressedDispatcherOwner")
    }
}
