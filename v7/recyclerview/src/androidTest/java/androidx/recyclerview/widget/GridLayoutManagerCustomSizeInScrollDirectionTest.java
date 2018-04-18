/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.recyclerview.widget;

import static androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL;
import static androidx.recyclerview.widget.LinearLayoutManager.VERTICAL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.graphics.Rect;
import android.support.test.filters.MediumTest;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.List;

@MediumTest
@RunWith(Parameterized.class)
public class GridLayoutManagerCustomSizeInScrollDirectionTest extends BaseGridLayoutManagerTest {
    @Parameterized.Parameters(name = "addDecorOffsets:{1},addMargins:{2},config:{0}")
    public static List<Object[]> getParams() {
        List<Object[]> params = new ArrayList<>();
        Boolean[] options = new Boolean[]{true, false};
        for (boolean addMargins : options) {
            for (boolean addDecorOffsets : options) {
                params.add(new Object[] {
                        new Config(3, HORIZONTAL, false), addDecorOffsets, addMargins});
                params.add(new Object[] {
                        new Config(3, VERTICAL, false), addDecorOffsets, addMargins});
            }
        }
        return params;
    }

    private final boolean mAddDecorOffsets;
    private final boolean mAddMargins;
    private final Config mConfig;

    public GridLayoutManagerCustomSizeInScrollDirectionTest(Config config, boolean addDecorOffsets,
            boolean addMargins) {
        mConfig = config;
        mAddDecorOffsets = addDecorOffsets;
        mAddMargins = addMargins;
    }

    @Test
    public void customSizeInScrollDirectionTest() throws Throwable {
        final int decorOffset = mAddDecorOffsets ? 7 : 0;
        final int margin = mAddMargins ? 11 : 0;
        final int[] sizePerPosition = new int[]{3, 5, 9, 21, 3, 5, 9, 6, 9, 1};
        final int[] expectedSizePerPosition = new int[]{9, 9, 9, 21, 3, 5, 9, 9, 9, 1};

        final GridTestAdapter testAdapter = new GridTestAdapter(10) {
            @Override
            public void onBindViewHolder(@NonNull TestViewHolder holder,
                    int position) {
                super.onBindViewHolder(holder, position);
                ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams)
                        holder.itemView.getLayoutParams();
                if (layoutParams == null) {
                    layoutParams = new ViewGroup.MarginLayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
                    holder.itemView.setLayoutParams(layoutParams);
                }
                final int size = sizePerPosition[position];
                if (mConfig.mOrientation == HORIZONTAL) {
                    layoutParams.width = size;
                    layoutParams.leftMargin = margin;
                    layoutParams.rightMargin = margin;
                } else {
                    layoutParams.height = size;
                    layoutParams.topMargin = margin;
                    layoutParams.bottomMargin = margin;
                }
            }
        };
        testAdapter.setFullSpan(3, 5);
        final RecyclerView rv = setupBasic(mConfig, testAdapter);
        if (mAddDecorOffsets) {
            rv.addItemDecoration(new RecyclerView.ItemDecoration() {
                @Override
                public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                        RecyclerView.State state) {
                    if (mConfig.mOrientation == HORIZONTAL) {
                        outRect.set(decorOffset, 0, decorOffset, 0);
                    } else {
                        outRect.set(0, decorOffset, 0, decorOffset);
                    }
                }
            });
        }
        waitForFirstLayout(rv);

        assertTrue("[test sanity] some views should be laid out",
                mRecyclerView.getChildCount() > 0);
        for (int i = 0; i < mRecyclerView.getChildCount(); i++) {
            View child = mRecyclerView.getChildAt(i);
            final int size = mConfig.mOrientation == HORIZONTAL ? child.getWidth()
                    : child.getHeight();
            assertEquals("child " + i + " should have the size specified in its layout params",
                    expectedSizePerPosition[i], size);
        }
        checkForMainThreadException();
    }
}
