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

import static android.view.View.MeasureSpec.AT_MOST;
import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.View.MeasureSpec.UNSPECIFIED;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import static androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL;
import static androidx.recyclerview.widget.LinearLayoutManager.VERTICAL;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import android.graphics.Color;
import android.support.test.filters.MediumTest;
import android.view.View;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.List;

@RunWith(Parameterized.class)
@MediumTest
public class GridLayoutManagerWrapContentWithAspectRatioTest
        extends BaseWrapContentWithAspectRatioTest {

    @Parameterized.Parameters(name = "{0},{1},{2}")
    public static List<Object[]> params() {
        List<Object[]> params = new ArrayList<>();
        for (int orientation : new int[]{VERTICAL, HORIZONTAL}) {
            for (boolean reverseLayout : new boolean[]{false, true}) {
                for (boolean unlimitedW : new boolean[]{true, false}) {
                    for (boolean unlimitedH : new boolean[]{true, false}) {
                        for (int behavior1Size : new int[]{8, 10, 12}) {
                            params.add(new Object[]{
                                    new BaseWrapContentTest.WrapContentConfig(unlimitedW, unlimitedH),
                                    new BaseGridLayoutManagerTest.Config(3, orientation, reverseLayout),
                                    behavior1Size
                            });
                        }

                    }
                }

            }
        }
        return params;
    }

    private GridLayoutManagerTest.Config mConfig;

    private int mBehavior1Size;

    int mTestOrientation;

    boolean mUnlimited;

    RecyclerView.LayoutManager mLayoutManager;

    WrappedRecyclerView mRecyclerView;

    OrientationHelper mHelper;

    public GridLayoutManagerWrapContentWithAspectRatioTest(BaseWrapContentTest.WrapContentConfig wrapContentConfig,
            GridLayoutManagerTest.Config config, int behavior1Size) {
        super(wrapContentConfig);
        mConfig = config;
        mBehavior1Size = behavior1Size;
    }

    @Before
    public final void init() {
        TestedFrameLayout.FullControlLayoutParams lp =
                mWrapContentConfig.toLayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        if (mConfig.mOrientation == VERTICAL) {
            mTestOrientation = HORIZONTAL;
            mUnlimited = lp.wSpec != null;
        } else {
            mTestOrientation = VERTICAL;
            mUnlimited = lp.hSpec != null;
        }
        mLayoutManager = createFromConfig();

        mRecyclerView = new WrappedRecyclerView(getActivity());
        mHelper = OrientationHelper.createOrientationHelper(
                mLayoutManager, 1 - mConfig.mOrientation);

        mRecyclerView.setBackgroundColor(Color.rgb(0, 0, 255));
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setLayoutParams(lp);
    }

    @Test
    public void testChildWithMultipleSpans() throws Throwable {
        MeasureBehavior behavior1;
        behavior1 = new MeasureBehavior(mBehavior1Size, mBehavior1Size,
                mTestOrientation == HORIZONTAL ? MATCH_PARENT : WRAP_CONTENT,
                mTestOrientation == VERTICAL ? MATCH_PARENT : WRAP_CONTENT);

        MeasureBehavior behavior2 = new MeasureBehavior(
                mTestOrientation == HORIZONTAL ? 30 : 10,
                mTestOrientation == VERTICAL ? 30 : 10, WRAP_CONTENT, WRAP_CONTENT);
        WrapContentAdapter adapter = new WrapContentAdapter(behavior1, behavior2);
        ((GridLayoutManager) mLayoutManager).setSpanSizeLookup(
                new GridLayoutManager.SpanSizeLookup() {
                    @Override
                    public int getSpanSize(int position) {
                        return position == 1 ? 2 : 1;
                    }
                });
        mRecyclerView.setAdapter(adapter);
        setRecyclerView(mRecyclerView);
        mRecyclerView.waitUntilLayout();

        final int parentSize = getSize((View) mRecyclerView.getParent(), mTestOrientation);

        if (mUnlimited) {
            assertThat(behavior1.getSpec(0, mTestOrientation),
                    MeasureSpecMatcher.mode(UNSPECIFIED));
            assertThat(behavior2.getSpec(0, mTestOrientation),
                    MeasureSpecMatcher.mode(UNSPECIFIED));
        } else {
            int[] borders = GridLayoutManager.calculateItemBorders(null,
                    mConfig.mSpanCount, parentSize);
            assertThat(behavior1.getSpec(0, mTestOrientation),
                    MeasureSpecMatcher.is(borders[1] - borders[0], AT_MOST));
            assertThat(behavior2.getSpec(0, mTestOrientation),
                    MeasureSpecMatcher.is(borders[3] - borders[1], AT_MOST));
        }
        // child0 should be measured again because it measured its size as 10
        assertThat(behavior1.getSpec(1, mTestOrientation),
                MeasureSpecMatcher.is(15, EXACTLY));
        assertThat(behavior1.getSpec(1, mConfig.mOrientation),
                MeasureSpecMatcher.mode(UNSPECIFIED));
        switch (mBehavior1Size) {
            case 10:
                assertThat(behavior1.measureSpecs.size(), is(2));
                assertThat(behavior2.measureSpecs.size(), is(1));
                break;
            case 8:
                assertThat(behavior2.measureSpecs.size(), is(1));
                assertThat(behavior1.measureSpecs.size(), is(3));
                assertThat(behavior1.getSpec(2, mTestOrientation),
                        MeasureSpecMatcher.is(15, EXACTLY));
                assertThat(behavior1.getSpec(2, mConfig.mOrientation),
                        MeasureSpecMatcher.is(10, EXACTLY));
                break;
            case 12:
                assertThat(behavior1.measureSpecs.size(), is(2));
                assertThat(behavior2.measureSpecs.size(), is(2));
                assertThat(behavior2.getSpec(1, mTestOrientation),
                        MeasureSpecMatcher.is(30, AT_MOST));
                assertThat(behavior2.getSpec(1, mConfig.mOrientation),
                        MeasureSpecMatcher.is(12, EXACTLY));
                break;
        }

        View child0 = mRecyclerView.getChildAt(0);
        assertThat(getSize(child0, mTestOrientation), is(15));

        View child1 = mRecyclerView.getChildAt(1);
        assertThat(getSize(child1, mTestOrientation), is(30));

        assertThat(mHelper.getDecoratedStart(child0), is(0));
        assertThat(mHelper.getDecoratedStart(child1), is(15));

        assertThat(mHelper.getDecoratedEnd(child0), is(15));
        assertThat(mHelper.getDecoratedEnd(child1), is(45));

        assertThat(mHelper.getDecoratedMeasurementInOther(child0),
                is(Math.max(10, mBehavior1Size)));
        assertThat(mHelper.getDecoratedMeasurementInOther(child1),
                is(Math.max(10, mBehavior1Size)));

        assertThat(getSize(mRecyclerView, mTestOrientation), is(45));
        assertThat(getSize(mRecyclerView, 1 - mTestOrientation), is(Math.max(10, mBehavior1Size)));
    }

    @Test
    public void testChildWithMatchParentInOtherDirection() throws Throwable {
        MeasureBehavior behavior1;
        behavior1 = new MeasureBehavior(mBehavior1Size, mBehavior1Size,
                mTestOrientation == HORIZONTAL ? MATCH_PARENT : WRAP_CONTENT,
                mTestOrientation == VERTICAL ? MATCH_PARENT : WRAP_CONTENT);

        MeasureBehavior behavior2 = new MeasureBehavior(
                mTestOrientation == HORIZONTAL ? 15 : 10,
                mTestOrientation == VERTICAL ? 15 : 10, WRAP_CONTENT, WRAP_CONTENT);
        WrapContentAdapter adapter = new WrapContentAdapter(behavior1, behavior2);

        mRecyclerView.setAdapter(adapter);
        setRecyclerView(mRecyclerView);
        mRecyclerView.waitUntilLayout();
        final int parentSize = getSize((View) mRecyclerView.getParent(), mTestOrientation);
        if (mUnlimited) {
            assertThat(behavior1.getSpec(0, mTestOrientation),
                    MeasureSpecMatcher.mode(UNSPECIFIED));
            assertThat(behavior2.getSpec(0, mTestOrientation),
                    MeasureSpecMatcher.mode(UNSPECIFIED));
        } else {
            int[] borders = GridLayoutManager.calculateItemBorders(null, mConfig.mSpanCount,
                    parentSize);
            assertThat(behavior1.getSpec(0, mTestOrientation),
                    MeasureSpecMatcher.is(borders[1] - borders[0], AT_MOST));
            assertThat(behavior2.getSpec(0, mTestOrientation),
                    MeasureSpecMatcher.is(borders[2] - borders[1], AT_MOST));
        }
        // child0 should be measured again because it measured its size as 10
        assertThat(behavior1.getSpec(1, mTestOrientation),
                MeasureSpecMatcher.is(15, EXACTLY));
        assertThat(behavior1.getSpec(1, mConfig.mOrientation),
                MeasureSpecMatcher.mode(UNSPECIFIED));
        switch (mBehavior1Size) {
            case 10:
                assertThat(behavior1.measureSpecs.size(), is(2));
                assertThat(behavior2.measureSpecs.size(), is(1));
                break;
            case 8:
                assertThat(behavior2.measureSpecs.size(), is(1));
                assertThat(behavior1.measureSpecs.size(), is(3));
                assertThat(behavior1.getSpec(2, mTestOrientation),
                        MeasureSpecMatcher.is(15, EXACTLY));
                assertThat(behavior1.getSpec(2, mConfig.mOrientation),
                        MeasureSpecMatcher.is(10, EXACTLY));
                break;
            case 12:
                assertThat(behavior1.measureSpecs.size(), is(2));
                assertThat(behavior2.measureSpecs.size(), is(2));
                assertThat(behavior2.getSpec(1, mTestOrientation),
                        MeasureSpecMatcher.is(15, AT_MOST));
                assertThat(behavior2.getSpec(1, mConfig.mOrientation),
                        MeasureSpecMatcher.is(12, EXACTLY));
                break;
        }

        View child0 = mRecyclerView.getChildAt(0);
        assertThat(getSize(child0, mTestOrientation), is(15));

        View child1 = mRecyclerView.getChildAt(1);
        assertThat(getSize(child1, mTestOrientation), is(15));

        assertThat(mHelper.getDecoratedStart(child0), is(0));
        assertThat(mHelper.getDecoratedStart(child1), is(15));

        assertThat(mHelper.getDecoratedEnd(child0), is(15));
        assertThat(mHelper.getDecoratedEnd(child1), is(30));

        assertThat(mHelper.getDecoratedMeasurementInOther(child0),
                is(Math.max(10, mBehavior1Size)));
        assertThat(mHelper.getDecoratedMeasurementInOther(child1),
                is(Math.max(10, mBehavior1Size)));

        assertThat(getSize(mRecyclerView, mTestOrientation), is(45));
        assertThat(getSize(mRecyclerView, 1 - mTestOrientation), is(Math.max(10, mBehavior1Size)));
    }

    @Test
    public void testAllChildrenWrapContentInOtherDirection() throws Throwable {
        MeasureBehavior behavior1;
        behavior1 = new MeasureBehavior(mBehavior1Size, mBehavior1Size, WRAP_CONTENT, WRAP_CONTENT);

        MeasureBehavior behavior2 = new MeasureBehavior(
                mTestOrientation == HORIZONTAL ? 15 : 10,
                mTestOrientation == VERTICAL ? 15 : 10, WRAP_CONTENT, WRAP_CONTENT);
        WrapContentAdapter adapter = new WrapContentAdapter(behavior1, behavior2);

        mRecyclerView.setAdapter(adapter);
        setRecyclerView(mRecyclerView);
        mRecyclerView.waitUntilLayout();
        final int parentSize = getSize((View) mRecyclerView.getParent(), mTestOrientation);
        if (mUnlimited) {
            assertThat(behavior1.getSpec(0, mTestOrientation),
                    MeasureSpecMatcher.mode(UNSPECIFIED));
            assertThat(behavior2.getSpec(0, mTestOrientation),
                    MeasureSpecMatcher.mode(UNSPECIFIED));
        } else {
            int[] borders = GridLayoutManager.calculateItemBorders(null,
                    mConfig.mSpanCount, parentSize);
            assertThat(behavior1.getSpec(0, mTestOrientation),
                    MeasureSpecMatcher.is(borders[1] - borders[0], AT_MOST));
            assertThat(behavior2.getSpec(0, mTestOrientation),
                    MeasureSpecMatcher.is(borders[2] - borders[1], AT_MOST));
        }

        switch (mBehavior1Size) {
            case 10:
                assertThat(behavior1.measureSpecs.size(), is(1));
                assertThat(behavior2.measureSpecs.size(), is(1));
                break;
            case 8:
                assertThat(behavior2.measureSpecs.size(), is(1));
                assertThat(behavior1.measureSpecs.size(), is(2));
                assertThat(behavior1.getSpec(1, mTestOrientation),
                        MeasureSpecMatcher.is(15, AT_MOST));
                assertThat(behavior1.getSpec(1, mConfig.mOrientation),
                        MeasureSpecMatcher.is(10, EXACTLY));
                break;
            case 12:
                assertThat(behavior1.measureSpecs.size(), is(1));
                assertThat(behavior2.measureSpecs.size(), is(2));
                assertThat(behavior2.getSpec(1, mTestOrientation),
                        MeasureSpecMatcher.is(15, AT_MOST));
                assertThat(behavior2.getSpec(1, mConfig.mOrientation),
                        MeasureSpecMatcher.is(12, EXACTLY));
                break;
        }

        View child0 = mRecyclerView.getChildAt(0);
        assertThat(getSize(child0, mTestOrientation), is(mBehavior1Size));

        View child1 = mRecyclerView.getChildAt(1);
        assertThat(getSize(child1, mTestOrientation), is(15));

        assertThat(mHelper.getDecoratedStart(child0), is(0));
        assertThat(mHelper.getDecoratedStart(child1), is(15));

        assertThat(mHelper.getDecoratedEnd(child0), is(mBehavior1Size));
        assertThat(mHelper.getDecoratedEnd(child1), is(30));

        assertThat(mHelper.getDecoratedMeasurementInOther(child0),
                is(Math.max(10, mBehavior1Size)));
        assertThat(mHelper.getDecoratedMeasurementInOther(child1),
                is(Math.max(10, mBehavior1Size)));

        assertThat(getSize(mRecyclerView, mTestOrientation), is(45));
        assertThat(getSize(mRecyclerView, 1 - mTestOrientation), is(Math.max(10, mBehavior1Size)));
    }

    private RecyclerView.LayoutManager createFromConfig() {
        return new GridLayoutManager(getActivity(), mConfig.mSpanCount,
                mConfig.mOrientation, mConfig.mReverseLayout);
    }
}
