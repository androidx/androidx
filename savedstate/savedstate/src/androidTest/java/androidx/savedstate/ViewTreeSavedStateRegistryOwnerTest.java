/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.savedstate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ViewTreeSavedStateRegistryOwnerTest {

    /**
     * Tests that a direct set/get on a single view survives a round trip
     */
    @Test
    public void setGetSameView() {
        final View v = new View(InstrumentationRegistry.getInstrumentation().getContext());
        final SavedStateRegistryOwner fakeOwner = new FakeOwner();

        assertNull("initial SavedStateRegistryOwner expects null",
                ViewTreeSavedStateRegistryOwner.get(v));

        ViewTreeSavedStateRegistryOwner.set(v, fakeOwner);

        assertEquals("get the SavedStateRegistryOwner set directly", fakeOwner,
                ViewTreeSavedStateRegistryOwner.get(v));
    }

    /**
     * Tests that the owner set on a root of a subhierarchy is seen by both direct children
     * and other descendants
     */
    @Test
    public void getAncestorOwner() {
        final Context context = InstrumentationRegistry.getInstrumentation().getContext();
        final ViewGroup root = new FrameLayout(context);
        final ViewGroup parent = new FrameLayout(context);
        final View child = new View(context);
        root.addView(parent);
        parent.addView(child);

        assertNull("initial SavedStateRegistryOwner expects null",
                ViewTreeSavedStateRegistryOwner.get(child));

        final SavedStateRegistryOwner fakeOwner = new FakeOwner();
        ViewTreeSavedStateRegistryOwner.set(root, fakeOwner);

        assertEquals("root sees owner", fakeOwner, ViewTreeSavedStateRegistryOwner.get(root));
        assertEquals("direct child sees owner", fakeOwner,
                ViewTreeSavedStateRegistryOwner.get(parent));
        assertEquals("grandchild sees owner", fakeOwner,
                ViewTreeSavedStateRegistryOwner.get(child));
    }

    /**
     * Tests that a new owner set between a root and a descendant is seen by the descendant
     * instead of the root value
     */
    @Test
    public void shadowedOwner() {
        final Context context = InstrumentationRegistry.getInstrumentation().getContext();
        final ViewGroup root = new FrameLayout(context);
        final ViewGroup parent = new FrameLayout(context);
        final View child = new View(context);
        root.addView(parent);
        parent.addView(child);

        assertNull("initial SavedStateRegistryOwner expects null",
                ViewTreeSavedStateRegistryOwner.get(child));

        final SavedStateRegistryOwner rootFakeOwner = new FakeOwner();
        ViewTreeSavedStateRegistryOwner.set(root, rootFakeOwner);

        final SavedStateRegistryOwner parentFakeOwner = new FakeOwner();
        ViewTreeSavedStateRegistryOwner.set(parent, parentFakeOwner);

        assertEquals("root sees owner", rootFakeOwner, ViewTreeSavedStateRegistryOwner.get(root));
        assertEquals("direct child sees owner", parentFakeOwner,
                ViewTreeSavedStateRegistryOwner.get(parent));
        assertEquals("grandchild sees owner", parentFakeOwner,
                ViewTreeSavedStateRegistryOwner.get(child));
    }

    static class FakeOwner implements SavedStateRegistryOwner {
        @NonNull
        @Override
        public Lifecycle getLifecycle() {
            throw new UnsupportedOperationException("not a real SavedStateRegistryOwner");
        }

        @NonNull
        @Override
        public SavedStateRegistry getSavedStateRegistry() {
            throw new UnsupportedOperationException("not a real SavedStateRegistryOwner");
        }
    }
}
