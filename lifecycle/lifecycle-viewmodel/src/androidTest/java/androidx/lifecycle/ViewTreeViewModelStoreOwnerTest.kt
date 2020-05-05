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
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class ViewTreeViewModelStoreOwnerTest {
    /**
     * Tests that a direct set/get on a single view survives a round trip
     */
    @Test
    fun setGetSameView() {
        val v = View(InstrumentationRegistry.getInstrumentation().context)

        assertWithMessage("initial ViewModelStoreOwner expects null")
            .that(ViewTreeViewModelStoreOwner.get(v))
            .isNull()

        val dummyOwner: ViewModelStoreOwner = DummyViewModelStoreOwner()
        ViewTreeViewModelStoreOwner.set(v, dummyOwner)

        assertWithMessage("get the ViewModelStoreOwner set directly")
            .that(ViewTreeViewModelStoreOwner.get(v))
            .isEqualTo(dummyOwner)
    }

    /**
     * Tests that the owner set on a root of a subhierarchy is seen by both direct children
     * and other descendants
     */
    @Test
    fun getAncestorOwner() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val root: ViewGroup = FrameLayout(context)
        val parent: ViewGroup = FrameLayout(context)
        val child = View(context)
        root.addView(parent)
        parent.addView(child)

        assertWithMessage("initial ViewModelStoreOwner expects null")
            .that(ViewTreeViewModelStoreOwner.get(child))
            .isNull()

        val dummyOwner: ViewModelStoreOwner = DummyViewModelStoreOwner()
        ViewTreeViewModelStoreOwner.set(root, dummyOwner)

        assertWithMessage("root sees owner")
            .that(ViewTreeViewModelStoreOwner.get(root))
            .isEqualTo(dummyOwner)
        assertWithMessage("direct child sees owner")
            .that(ViewTreeViewModelStoreOwner.get(parent))
            .isEqualTo(dummyOwner)
        assertWithMessage("grandchild sees owner")
            .that(ViewTreeViewModelStoreOwner.get(child))
            .isEqualTo(dummyOwner)
    }

    /**
     * Tests that a new owner set between a root and a descendant is seen by the descendant
     * instead of the root value
     */
    @Test
    fun shadowedOwner() {
        val context =
            InstrumentationRegistry.getInstrumentation().context
        val root: ViewGroup = FrameLayout(context)
        val parent: ViewGroup = FrameLayout(context)
        val child = View(context)
        root.addView(parent)
        parent.addView(child)

        assertWithMessage("initial ViewModelStoreOwner expects null")
            .that(ViewTreeViewModelStoreOwner.get(child))
            .isNull()

        val rootDummyOwner: ViewModelStoreOwner = DummyViewModelStoreOwner()
        ViewTreeViewModelStoreOwner.set(root, rootDummyOwner)

        val parentDummyOwner: ViewModelStoreOwner = DummyViewModelStoreOwner()
        ViewTreeViewModelStoreOwner.set(parent, parentDummyOwner)

        assertWithMessage("root sees owner")
            .that(ViewTreeViewModelStoreOwner.get(root))
            .isEqualTo(rootDummyOwner)
        assertWithMessage("direct child sees owner")
            .that(ViewTreeViewModelStoreOwner.get(parent))
            .isEqualTo(parentDummyOwner)
        assertWithMessage("grandchild sees owner")
            .that(ViewTreeViewModelStoreOwner.get(child))
            .isEqualTo(parentDummyOwner)
    }

    internal class DummyViewModelStoreOwner : ViewModelStoreOwner {
        override fun getViewModelStore(): ViewModelStore {
            throw UnsupportedOperationException("not a real ViewModelStoreOwner")
        }
    }
}
