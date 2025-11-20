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

import android.graphics.Rect;
import android.view.Gravity;
import android.view.View;

import androidx.test.filters.LargeTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(Parameterized.class)
@LargeTest
public class LinearLayoutManagerWrapContentTest extends BaseWrapContentTest {

    BaseLinearLayoutManagerTest.Config mConfig;

    public LinearLayoutManagerWrapContentTest(BaseLinearLayoutManagerTest.Config config,
            WrapContentConfig wrapContentConfig) {
        super(wrapContentConfig);
        mConfig = config;
    }

    @Test
    public void testUnspecifiedWithHint() throws Throwable {
        unspecifiedWithHintTest(mConfig.mOrientation == StaggeredGridLayoutManager.HORIZONTAL);
    }

    @Test
    public void deletion() throws Throwable {
        testScenerio(new Scenario(
                new Step() {
                    @Override
                    void onRun() throws Throwable {
                        mTestAdapter.deleteAndNotify(3, 3);
                    }
                },
                new Step() {
                    @Override
                    void onRun() throws Throwable {
                        mTestAdapter.deleteAndNotify(3, 3);
                    }
                },
                new Step() {
                    @Override
                    void onRun() throws Throwable {
                        mTestAdapter.deleteAndNotify(1, 2);
                    }
                }) {
        });
    }

    @Test
    public void addition() throws Throwable {
        testScenerio(new Scenario(
                new Step() {
                    @Override
                    void onRun() throws Throwable {
                        mTestAdapter.addAndNotify(1, 2);
                    }
                }
                ,
                new Step() {
                    @Override
                    void onRun() throws Throwable {
                        mTestAdapter.addAndNotify(0, 2);
                    }
                },
                new Step() {
                    @Override
                    void onRun() throws Throwable {
                        mTestAdapter.addAndNotify(6, 3);
                    }
                }
        ) {
            @Override
            public int getSeedAdapterSize() {
                return 2;
            }
        });
    }

    @Parameterized.Parameters(name = "{0},{1}")
    public static Iterable<Object[]> data() {
        List<Object[]> params = new ArrayList<>();
        List<Rect> paddings = Arrays.asList(
                new Rect(0, 0, 0, 0),
                new Rect(5, 0, 0, 0),
                new Rect(0, 6, 0, 0),
                new Rect(0, 0, 7, 0),
                new Rect(0, 0, 0, 8),
                new Rect(3, 5, 7, 11)
        );
        for (Rect padding : paddings) {
            for (int orientation : new int[]{VERTICAL, HORIZONTAL}) {
                for (boolean reverseLayout : new boolean[]{false, true}) {
                    for (boolean stackFromBottom : new boolean[]{false, true}) {
                        params.add(
                                new Object[]{
                                        new BaseLinearLayoutManagerTest.Config(orientation, reverseLayout, stackFromBottom),
                                        new WrapContentConfig(false, false, new Rect(padding))
                                }
                        );
                        params.add(
                                new Object[]{
                                        new BaseLinearLayoutManagerTest.Config(orientation, reverseLayout, stackFromBottom),
                                        new WrapContentConfig(HORIZONTAL == orientation,
                                                VERTICAL == orientation, new Rect(padding))
                                }
                        );
                    }
                }
            }
        }
        return params;
    }

    @Override
    RecyclerView.LayoutManager createLayoutManager() {
        return createFromConfig();
    }

    private LinearLayoutManager createFromConfig() {
        LinearLayoutManager llm = new LinearLayoutManager(getActivity(), mConfig.mOrientation,
                mConfig.mReverseLayout);
        llm.setStackFromEnd(mConfig.mStackFromEnd);
        return llm;
    }

    @Override
    protected int getVerticalGravity(RecyclerView.LayoutManager layoutManager) {
        if (mConfig.mOrientation == HORIZONTAL) {
            return Gravity.TOP;
        }
        if (mConfig.mReverseLayout ^ mConfig.mStackFromEnd) {
            return Gravity.BOTTOM;
        }
        return Gravity.TOP;
    }

    @Override
    protected int getHorizontalGravity(RecyclerView.LayoutManager layoutManager) {
        boolean rtl = layoutManager.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
        if (mConfig.mOrientation == VERTICAL) {
            if (rtl) {
                return Gravity.RIGHT;
            }
            return Gravity.LEFT;
        }
        boolean end = mConfig.mReverseLayout ^ mConfig.mStackFromEnd;
        if (rtl ^ end) {
            return Gravity.RIGHT;
        }
        return Gravity.LEFT;
    }
}
