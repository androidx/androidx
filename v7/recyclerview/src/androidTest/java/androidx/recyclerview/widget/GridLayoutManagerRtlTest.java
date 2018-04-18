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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
public class GridLayoutManagerRtlTest extends BaseGridLayoutManagerTest {

    public GridLayoutManagerRtlTest(Config config, boolean changeRtlAfter, boolean oneLine,
            boolean itemsWrapContent) {
        mConfig = config;
        mChangeRtlAfter = changeRtlAfter;
        mOneLine = oneLine;
        mItemsWrapContent = itemsWrapContent;
    }

    @Parameterized.Parameters(name = "conf:{0},changeRl:{1},oneLine:{2},itemsWrap:{3}")
    public static List<Object[]> params() {
        List<Object[]> result = new ArrayList<>();
        for (boolean changeRtlAfter : new boolean[]{false, true}) {
            for (boolean oneLine : new boolean[]{false, true}) {
                for (boolean itemsWrapContent : new boolean[]{false, true}) {
                    for (Config config : createBaseVariations()) {
                        result.add(new Object[] {
                                config,
                                changeRtlAfter,
                                oneLine,
                                itemsWrapContent
                        });
                    }
                }
            }
        }
        return result;
    }
    final Config mConfig;
    final boolean mChangeRtlAfter;
    final boolean mOneLine;
    final boolean mItemsWrapContent;


    @Test
    public void rtlTest() throws Throwable {
        if (mOneLine && mConfig.mOrientation != VERTICAL) {
            return;// nothing to test
        }
        if (mConfig.mSpanCount == 1) {
            mConfig.mSpanCount = 2;
        }
        String logPrefix = mConfig + ", changeRtlAfterLayout:" + mChangeRtlAfter + ","
                + "oneLine:" + mOneLine + " itemsWrap:" + mItemsWrapContent;
        mConfig.mItemCount = 5;
        if (mOneLine) {
            mConfig.mSpanCount = mConfig.mItemCount + 1;
        } else {
            mConfig.mSpanCount = Math.min(mConfig.mItemCount - 1, mConfig.mSpanCount);
        }

        RecyclerView rv = setupBasic(mConfig, new GridTestAdapter(mConfig.mItemCount) {
            @Override
            public void onBindViewHolder(@NonNull TestViewHolder holder,
                    int position) {
                super.onBindViewHolder(holder, position);
                if (mItemsWrapContent) {
                    ViewGroup.LayoutParams lp = holder.itemView.getLayoutParams();
                    if (lp == null) {
                        lp = mGlm.generateDefaultLayoutParams();
                    }
                    if (mConfig.mOrientation == HORIZONTAL) {
                        lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    } else {
                        lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                    }
                }
            }
        });
        if (mChangeRtlAfter) {
            waitForFirstLayout(rv);
            mGlm.expectLayout(1);
            mGlm.setFakeRtl(true);
            mGlm.waitForLayout(2);
        } else {
            mGlm.mFakeRTL = true;
            waitForFirstLayout(rv);
        }

        assertEquals("view should become rtl", true, mGlm.isLayoutRTL());
        OrientationHelper helper = OrientationHelper.createHorizontalHelper(mGlm);
        View child0 = mGlm.findViewByPosition(0);
        final int secondChildPos = mConfig.mOrientation == VERTICAL ? 1
                : mConfig.mSpanCount;
        View child1 = mGlm.findViewByPosition(secondChildPos);
        assertNotNull(logPrefix + " child position 0 should be laid out", child0);
        assertNotNull(
                logPrefix + " second child position " + (secondChildPos) + " should be laid out",
                child1);
        if (mConfig.mOrientation == VERTICAL || !mConfig.mReverseLayout) {
            assertTrue(logPrefix + " second child should be to the left of first child",
                    helper.getDecoratedStart(child0) >= helper.getDecoratedEnd(child1));
            assertEquals(logPrefix + " first child should be right aligned",
                    helper.getDecoratedEnd(child0), helper.getEndAfterPadding());
        } else {
            assertTrue(logPrefix + " first child should be to the left of second child",
                    helper.getDecoratedStart(child1) >= helper.getDecoratedEnd(child0));
            assertEquals(logPrefix + " first child should be left aligned",
                    helper.getDecoratedStart(child0), helper.getStartAfterPadding());
        }
        checkForMainThreadException();
    }
}
