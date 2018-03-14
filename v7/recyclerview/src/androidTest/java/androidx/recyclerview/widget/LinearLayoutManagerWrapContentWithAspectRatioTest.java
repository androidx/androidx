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

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import static androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL;
import static androidx.recyclerview.widget.LinearLayoutManager.VERTICAL;

import static org.hamcrest.MatcherAssert.assertThat;

import android.graphics.Color;
import android.support.test.filters.MediumTest;
import android.view.View;
import android.view.ViewGroup;

import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.List;

@RunWith(Parameterized.class)
@MediumTest
public class LinearLayoutManagerWrapContentWithAspectRatioTest
        extends BaseWrapContentWithAspectRatioTest {

    final LinearLayoutManagerTest.Config mConfig;
    final float mRatio;

    public LinearLayoutManagerWrapContentWithAspectRatioTest(
            BaseLinearLayoutManagerTest.Config config,
            BaseWrapContentTest.WrapContentConfig wrapContentConfig,
            float ratio) {
        super(wrapContentConfig);
        mConfig = config;
        mRatio = ratio;
    }

    @Parameterized.Parameters(name = "{0},{1},ratio:{2}")
    public static Iterable<Object[]> data() {
        List<Object[]> params = new ArrayList<>();
        for (float ratio : new float[]{.5f, 1f, 2f}) {
            for (int orientation : new int[]{VERTICAL, HORIZONTAL}) {
                for (boolean reverseLayout : new boolean[]{false, true}) {
                    for (boolean stackFromBottom : new boolean[]{false, true}) {
                        params.add(
                                new Object[]{
                                        new BaseLinearLayoutManagerTest.Config(orientation,
                                                reverseLayout, stackFromBottom),
                                        new BaseWrapContentTest.WrapContentConfig(
                                                false, false),
                                        ratio
                                }
                        );
                        params.add(
                                new Object[]{
                                        new BaseLinearLayoutManagerTest.Config(orientation,
                                                reverseLayout, stackFromBottom),
                                        new BaseWrapContentTest.WrapContentConfig(
                                                HORIZONTAL == orientation,
                                                VERTICAL == orientation),
                                        ratio
                                }
                        );
                        params.add(
                                new Object[]{
                                        new BaseLinearLayoutManagerTest.Config(orientation,
                                                reverseLayout, stackFromBottom),
                                        new BaseWrapContentTest.WrapContentConfig(
                                                VERTICAL == orientation,
                                                HORIZONTAL == orientation),
                                        ratio
                                }
                        );
                        params.add(
                                new Object[]{
                                        new BaseLinearLayoutManagerTest.Config(orientation,
                                                reverseLayout, stackFromBottom),
                                        new BaseWrapContentTest.WrapContentConfig(
                                                true, true),
                                        ratio
                                }
                        );
                    }
                }
            }
        }
        return params;
    }

    @Test
    public void wrapContentAffectsOtherOrientation() throws Throwable {
        TestedFrameLayout.FullControlLayoutParams
                wrapContent = new TestedFrameLayout.FullControlLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        int testOrientation = mConfig.mOrientation == VERTICAL ? HORIZONTAL : VERTICAL;
        boolean unlimitedSize = false;
        if (mWrapContentConfig.isUnlimitedHeight()) {
            wrapContent.hSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            unlimitedSize = testOrientation == VERTICAL;
        }
        if (mWrapContentConfig.isUnlimitedWidth()) {
            wrapContent.wSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            unlimitedSize |= testOrientation == HORIZONTAL;
        }

        RecyclerView.LayoutManager layoutManager = createFromConfig();
        WrappedRecyclerView
                recyclerView = new WrappedRecyclerView(getActivity());
        recyclerView.setBackgroundColor(Color.rgb(0, 0, 255));
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setLayoutParams(wrapContent);

        AspectRatioMeasureBehavior behavior1 = new AspectRatioMeasureBehavior(10, 10,
                WRAP_CONTENT, WRAP_CONTENT).aspectRatio(testOrientation, mRatio);
        AspectRatioMeasureBehavior behavior2 = new AspectRatioMeasureBehavior(15, 15,
                testOrientation == HORIZONTAL ? MATCH_PARENT : WRAP_CONTENT,
                testOrientation == VERTICAL ? MATCH_PARENT : WRAP_CONTENT)
                .aspectRatio(testOrientation, mRatio);
        AspectRatioMeasureBehavior behavior3 = new AspectRatioMeasureBehavior(8, 8,
                testOrientation == HORIZONTAL ? MATCH_PARENT : WRAP_CONTENT,
                testOrientation == VERTICAL ? MATCH_PARENT : WRAP_CONTENT)
                .aspectRatio(testOrientation, mRatio);

        WrapContentAdapter adapter = new WrapContentAdapter(
                behavior1, behavior2, behavior3
        );

        recyclerView.setAdapter(adapter);
        setRecyclerView(recyclerView);
        recyclerView.waitUntilLayout();

        int parentDim = getSize((View) recyclerView.getParent(), testOrientation);

        View itemView1 = recyclerView.findViewHolderForAdapterPosition(0).itemView;
        assertThat("first child test size", getSize(itemView1, testOrientation),
                CoreMatchers.is(10));
        assertThat("first child dependant size", getSize(itemView1, mConfig.mOrientation),
                CoreMatchers.is(behavior1.getSecondary(10)));

        View itemView2 = recyclerView.findViewHolderForAdapterPosition(1).itemView;
        assertThat("second child test size", getSize(itemView2, testOrientation),
                CoreMatchers.is(15));
        assertThat("second child dependant size", getSize(itemView2, mConfig.mOrientation),
                CoreMatchers.is(behavior2.getSecondary(15)));

        View itemView3 = recyclerView.findViewHolderForAdapterPosition(2).itemView;
        assertThat("third child test size", getSize(itemView3, testOrientation),
                CoreMatchers.is(15));
        assertThat("third child dependant size", getSize(itemView3, mConfig.mOrientation),
                CoreMatchers.is(behavior3.getSecondary(15)));

        assertThat("it should be measured only once", behavior1.measureSpecs.size(),
                CoreMatchers.is(1));
        if (unlimitedSize) {
            assertThat(behavior1.getSpec(0, testOrientation),
                    MeasureSpecMatcher.is(0, View.MeasureSpec.UNSPECIFIED));
        } else {
            assertThat(behavior1.getSpec(0, testOrientation),
                    MeasureSpecMatcher.is(parentDim, View.MeasureSpec.AT_MOST));
        }

        assertThat("it should be measured once", behavior2.measureSpecs.size(), CoreMatchers.is(1));
        if (unlimitedSize) {
            assertThat(behavior2.getSpec(0, testOrientation),
                    MeasureSpecMatcher.is(0, View.MeasureSpec.UNSPECIFIED));
        } else {
            assertThat(behavior2.getSpec(0, testOrientation),
                    MeasureSpecMatcher.is(parentDim, View.MeasureSpec.AT_MOST));
        }

        assertThat("it should be measured twice", behavior3.measureSpecs.size(),
                CoreMatchers.is(2));
        if (unlimitedSize) {
            assertThat(behavior3.getSpec(0, testOrientation),
                    MeasureSpecMatcher.is(0, View.MeasureSpec.UNSPECIFIED));
        } else {
            assertThat(behavior3.getSpec(0, testOrientation),
                    MeasureSpecMatcher.is(parentDim, View.MeasureSpec.AT_MOST));
        }

        assertThat(behavior3.getSpec(1, testOrientation),
                MeasureSpecMatcher.is(15, View.MeasureSpec.EXACTLY));
        final int totalScrollSize = getSize(itemView1, mConfig.mOrientation)
                + getSize(itemView2, mConfig.mOrientation)
                + getSize(itemView3, mConfig.mOrientation);
        assertThat("RecyclerView should wrap its content in the scroll direction",
                getSize(mRecyclerView, mConfig.mOrientation), CoreMatchers.is(totalScrollSize));
        assertThat("RecyclerView should wrap its content in the scroll direction",
                getSize(mRecyclerView, testOrientation), CoreMatchers.is(15));
    }

    private LinearLayoutManager createFromConfig() {
        LinearLayoutManager llm = new LinearLayoutManager(getActivity(), mConfig.mOrientation,
                mConfig.mReverseLayout);
        llm.setStackFromEnd(mConfig.mStackFromEnd);
        return llm;
    }
}
