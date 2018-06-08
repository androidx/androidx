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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import android.support.test.filters.MediumTest;
import android.view.View;

import androidx.annotation.NonNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.List;

@RunWith(Parameterized.class)
public class LinearLayoutManagerPrepareForDropTest extends BaseLinearLayoutManagerTest {

    final BaseLinearLayoutManagerTest.Config mConfig;
    final SelectTargetChildren mSelectTargetChildren;

    public LinearLayoutManagerPrepareForDropTest(
            Config config, SelectTargetChildren selectTargetChildren) {
        mConfig = config;
        mSelectTargetChildren = selectTargetChildren;
    }

    @Parameterized.Parameters(name = "{0},selectTargetChildren:{1}")
    public static Iterable<Object[]> params() {
        SelectTargetChildren[] selectors
                = new SelectTargetChildren[]{
                new SelectTargetChildren() {
                    @Override
                    public int[] selectTargetChildren(int childCount) {
                        return new int[]{1, 0};
                    }
                    @Override
                    public String toString() {
                        return "{1,0}";
                    }
                },
                new SelectTargetChildren() {
                    @Override
                    public int[] selectTargetChildren(int childCount) {
                        return new int[]{0, 1};
                    }
                    @Override
                    public String toString() {
                        return "{0,1}";
                    }
                },
                new SelectTargetChildren() {
                    @Override
                    public int[] selectTargetChildren(int childCount) {
                        return new int[]{childCount - 1, childCount - 2};
                    }
                    @Override
                    public String toString() {
                        return "{childCount-1,childCount-2}";
                    }
                },
                new SelectTargetChildren() {
                    @Override
                    public int[] selectTargetChildren(int childCount) {
                        return new int[]{childCount - 2, childCount - 1};
                    }
                    @Override
                    public String toString() {
                        return "{childCount-2,childCount-1}";
                    }
                },
                new SelectTargetChildren() {
                    @Override
                    public int[] selectTargetChildren(int childCount) {
                        return new int[]{childCount / 2, childCount / 2 + 1};
                    }
                    @Override
                    public String toString() {
                        return "{childCount/2,childCount/2+1}";
                    }
                },
                new SelectTargetChildren() {
                    @Override
                    public int[] selectTargetChildren(int childCount) {
                        return new int[]{childCount / 2 + 1, childCount / 2};
                    }
                    @Override
                    public String toString() {
                        return "{childCount/2+1,childCount/2}";
                    }
                }
        };
        List<Object[]> variations = new ArrayList<>();
        for (SelectTargetChildren selector : selectors) {
            for (BaseLinearLayoutManagerTest.Config config : createBaseVariations()) {
                variations.add(new Object[]{config, selector});
            }
        }
        return variations;
    }

    @Test
    @MediumTest
    public void prepareForDropTest()
            throws Throwable {
        final Config config = (Config) mConfig.clone();
        config.mTestAdapter = new BaseRecyclerViewInstrumentationTest.TestAdapter(100) {
            @Override
            public void onBindViewHolder(
                    @NonNull BaseRecyclerViewInstrumentationTest.TestViewHolder holder,
                    int position) {
                super.onBindViewHolder(holder, position);
                if (config.mOrientation == HORIZONTAL) {
                    final int base = mLayoutManager.getWidth() / 5;
                    final int itemRand = holder.mBoundItem.mText.hashCode() % base;
                    holder.itemView.setMinimumWidth(base + itemRand);
                } else {
                    final int base = mLayoutManager.getHeight() / 5;
                    final int itemRand = holder.mBoundItem.mText.hashCode() % base;
                    holder.itemView.setMinimumHeight(base + itemRand);
                }
            }
        };
        setupByConfig(config, true);
        mLayoutManager.expectLayouts(1);
        scrollToPosition(mTestAdapter.getItemCount() / 2);
        mLayoutManager.waitForLayout(1);
        int[] positions = mSelectTargetChildren.selectTargetChildren(mRecyclerView.getChildCount());
        final View fromChild = mLayoutManager.getChildAt(positions[0]);
        final int fromPos = mLayoutManager.getPosition(fromChild);
        final View onChild = mLayoutManager.getChildAt(positions[1]);
        final int toPos = mLayoutManager.getPosition(onChild);
        final OrientationHelper helper = mLayoutManager.mOrientationHelper;
        final int dragCoordinate;
        final boolean towardsHead = toPos < fromPos;
        final int referenceLine;
        if (config.mReverseLayout == towardsHead) {
            referenceLine = helper.getDecoratedEnd(onChild);
            dragCoordinate = referenceLine + 3 -
                    helper.getDecoratedMeasurement(fromChild);
        } else {
            referenceLine = helper.getDecoratedStart(onChild);
            dragCoordinate = referenceLine - 3;
        }
        mLayoutManager.expectLayouts(2);

        final int x, y;
        if (config.mOrientation == HORIZONTAL) {
            x = dragCoordinate;
            y = fromChild.getTop();
        } else {
            y = dragCoordinate;
            x = fromChild.getLeft();
        }
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTestAdapter.moveInUIThread(fromPos, toPos);
                mTestAdapter.notifyItemMoved(fromPos, toPos);
                mLayoutManager.prepareForDrop(fromChild, onChild, x, y);
            }
        });
        mLayoutManager.waitForLayout(2);

        assertSame(fromChild, mRecyclerView.findViewHolderForAdapterPosition(toPos).itemView);
        // make sure it has the position we wanted
        if (config.mReverseLayout == towardsHead) {
            assertEquals(referenceLine, helper.getDecoratedEnd(fromChild));
        } else {
            assertEquals(referenceLine, helper.getDecoratedStart(fromChild));
        }
    }

    protected interface SelectTargetChildren {
        int[] selectTargetChildren(int childCount);
    }
}
