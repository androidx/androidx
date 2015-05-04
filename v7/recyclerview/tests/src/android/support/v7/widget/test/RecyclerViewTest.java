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

import android.app.Activity;
import android.support.v7.recyclerview.test.CustomLayoutManager;
import android.support.v7.recyclerview.test.R;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.LinearLayout;

public class RecyclerViewTest extends ActivityInstrumentationTestCase2<RecyclerViewTestActivity> {

    public RecyclerViewTest() {
        super("android.support.v7.widget.test", RecyclerViewTestActivity.class);
    }

    private void setContentView(final int layoutId) throws Throwable {
        final Activity activity = getActivity();
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.setContentView(layoutId);
            }
        });
    }

    public void testInflation() throws Throwable {
        setContentView(R.layout.inflation_test);
        getInstrumentation().waitForIdleSync();
        RecyclerView view = (RecyclerView) getActivity().findViewById(R.id.recyclerView);
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

    }
}
