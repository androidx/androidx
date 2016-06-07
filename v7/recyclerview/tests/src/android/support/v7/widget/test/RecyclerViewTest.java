/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.support.v7.widget.test;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.app.Activity;
import android.app.Instrumentation;
import android.os.Build;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v7.recyclerview.test.CustomLayoutManager;
import android.support.v7.recyclerview.test.R;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.test.suitebuilder.annotation.SmallTest;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class RecyclerViewTest {

    @Rule
    public ActivityTestRule<RecyclerViewTestActivity> mActivityRule
            = new ActivityTestRule<>(RecyclerViewTestActivity.class);

    private void setContentView(final int layoutId) throws Throwable {
        final Activity activity = mActivityRule.getActivity();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                activity.setContentView(layoutId);
            }
        });
    }

    @Test
    public void savedStateAccess() throws ClassNotFoundException {
        // this class should be accessible outside RecyclerView package
        assertNotNull(RecyclerView.SavedState.class);
        assertNotNull(LinearLayoutManager.SavedState.class);
        assertNotNull(GridLayoutManager.SavedState.class);
        assertNotNull(StaggeredGridLayoutManager.SavedState.class);
    }

    @Test
    public void inflation() throws Throwable {
        setContentView(R.layout.inflation_test);
        getInstrumentation().waitForIdleSync();
        RecyclerView view;
        view = (RecyclerView) getActivity().findViewById(R.id.clipToPaddingUndefined);
        assertTrue(view.getLayoutManager().getClipToPadding());
        view = (RecyclerView) getActivity().findViewById(R.id.clipToPaddingYes);
        assertTrue(view.getLayoutManager().getClipToPadding());
        view = (RecyclerView) getActivity().findViewById(R.id.clipToPaddingNo);
        assertFalse(view.getLayoutManager().getClipToPadding());

        view = (RecyclerView) getActivity().findViewById(R.id.recyclerView);
        RecyclerView.LayoutManager layoutManager = view.getLayoutManager();
        assertNotNull("LayoutManager not created.", layoutManager);
        assertEquals("Incorrect LayoutManager created",
                layoutManager.getClass().getName(), GridLayoutManager.class.getName());
        GridLayoutManager gridLayoutManager = ((GridLayoutManager) layoutManager);
        assertEquals("Incorrect span count.", 3, gridLayoutManager.getSpanCount());
        assertEquals("Expected horizontal orientation.",
                LinearLayout.HORIZONTAL, gridLayoutManager.getOrientation());
        assertTrue("Expected reversed layout", gridLayoutManager.getReverseLayout());

        view = (RecyclerView) getActivity().findViewById(R.id.recyclerView2);
        layoutManager = view.getLayoutManager();
        assertNotNull("LayoutManager not created.", layoutManager);
        assertEquals("Incorrect LayoutManager created",
                layoutManager.getClass().getName(),
                CustomLayoutManager.class.getName());
        CustomLayoutManager customLayoutManager =
                (CustomLayoutManager) layoutManager;
        assertEquals("Expected vertical orientation.",
                LinearLayout.VERTICAL, customLayoutManager.getOrientation());
        assertTrue("Expected items to be stacked from end", customLayoutManager.getStackFromEnd());

        view = (RecyclerView) getActivity().findViewById(R.id.recyclerView3);
        layoutManager = view.getLayoutManager();
        assertNotNull("LayoutManager not created.", layoutManager);
        assertEquals("Incorrect LayoutManager created",
                layoutManager.getClass().getName(),
                CustomLayoutManager.LayoutManager.class.getName());

        view = (RecyclerView) getActivity().findViewById(R.id.recyclerView4);
        layoutManager = view.getLayoutManager();
        assertNotNull("LayoutManager not created.", layoutManager);
        assertEquals("Incorrect LayoutManager created",
                "android.support.v7.recyclerview.test.PrivateLayoutManager",
                layoutManager.getClass().getName());

        view = (RecyclerView) getActivity().findViewById(R.id.recyclerView5);
        assertTrue("Incorrect default nested scrolling value", view.isNestedScrollingEnabled());

        if (Build.VERSION.SDK_INT >= 21) {
            view = (RecyclerView) getActivity().findViewById(R.id.recyclerView6);
            assertFalse("Incorrect explicit nested scrolling value",
                    view.isNestedScrollingEnabled());
        }

        view = (RecyclerView) getActivity().findViewById(R.id.focusability_undefined);
        assertEquals(ViewGroup.FOCUS_AFTER_DESCENDANTS, view.getDescendantFocusability());

        view = (RecyclerView) getActivity().findViewById(R.id.focusability_after);
        assertEquals(ViewGroup.FOCUS_AFTER_DESCENDANTS, view.getDescendantFocusability());

        view = (RecyclerView) getActivity().findViewById(R.id.focusability_before);
        assertEquals(ViewGroup.FOCUS_BEFORE_DESCENDANTS, view.getDescendantFocusability());

        view = (RecyclerView) getActivity().findViewById(R.id.focusability_block);
        assertEquals(ViewGroup.FOCUS_BLOCK_DESCENDANTS, view.getDescendantFocusability());
    }

    private Activity getActivity() {
        return mActivityRule.getActivity();
    }

    private Instrumentation getInstrumentation() {
        return InstrumentationRegistry.getInstrumentation();
    }
}
