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

package androidx.leanback.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.jspecify.annotations.NonNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;

@RunWith(AndroidJUnit4.class)
public class GridLayoutManagerRobolectricTest {
    private Context mContext;

    @Before
    public void setup() {
        mContext = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void testInitialLayoutInTouchMode_noEdge() {
        // Force touch mode
        InstrumentationRegistry.getInstrumentation().setInTouchMode(true);
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        VerticalGridView gridView = new VerticalGridView(activity);
        gridView.setWindowAlignment(BaseGridView.WINDOW_ALIGN_NO_EDGE);

        RecyclerView.Adapter<RecyclerView.ViewHolder> adapter =
                new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                    @Override
                    public RecyclerView.@NonNull ViewHolder onCreateViewHolder(
                            @NonNull ViewGroup parent, int viewType) {
                        View view = new View(parent.getContext());
                        view.setLayoutParams(new ViewGroup.LayoutParams(100, 100));
                        return new RecyclerView.ViewHolder(view) {};
                    }

                    @Override
                    public void onBindViewHolder(
                            RecyclerView.@NonNull ViewHolder holder, int position) {
                    }

                    @Override
                    public int getItemCount() {
                        return 10;
                    }
                };
        gridView.setAdapter(adapter);

        FrameLayout frameLayout = new FrameLayout(activity);
        frameLayout.setLayoutParams(new ViewGroup.LayoutParams(1000, 1000));
        gridView.setLayoutParams(new FrameLayout.LayoutParams(1000, 1000));
        frameLayout.addView(gridView);

        activity.setContentView(frameLayout);

        assertTrue(gridView.isInTouchMode());

        // measure and layout
        frameLayout.measure(
                View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY));
        frameLayout.layout(0, 0, 1000, 1000);

        View child = gridView.getChildAt(0);
        int top = child.getTop();
        int bottom = child.getBottom();

        assertEquals(450, top);
        assertEquals(550, bottom);
    }
}
