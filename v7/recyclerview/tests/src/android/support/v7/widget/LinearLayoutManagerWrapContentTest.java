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

package android.support.v7.widget;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import android.support.v4.view.ViewCompat;
import android.test.suitebuilder.annotation.MediumTest;
import android.view.Gravity;

import java.util.ArrayList;
import java.util.List;

import static android.support.v7.widget.BaseLinearLayoutManagerTest.Config;
import static android.support.v7.widget.LinearLayoutManager.HORIZONTAL;
import static android.support.v7.widget.LinearLayoutManager.VERTICAL;

@RunWith(Parameterized.class)
@MediumTest
public class LinearLayoutManagerWrapContentTest extends BaseWrapContentTest {

    Config mConfig;

    public LinearLayoutManagerWrapContentTest(Config config,
            WrapContentConfig wrapContentConfig) {
        super(wrapContentConfig);
        mConfig = config;
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

    @Parameterized.Parameters(name = "{0} {1}")
    public static Iterable<Object[]> data() {
        List<Object[]> params = new ArrayList<>();
        for (int orientation : new int[]{VERTICAL, HORIZONTAL}) {
            for (boolean reverseLayout : new boolean[]{false, true}) {
                for (boolean stackFromBottom : new boolean[]{false, true}) {
                    params.add(
                            new Object[]{
                                    new Config(orientation, reverseLayout, stackFromBottom),
                                    new WrapContentConfig(false, false)
                            }
                    );
                    params.add(
                            new Object[]{
                                    new Config(orientation, reverseLayout, stackFromBottom),
                                    new WrapContentConfig(HORIZONTAL == orientation,
                                            VERTICAL == orientation)
                            }
                    );
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
        boolean rtl = layoutManager.getLayoutDirection() == ViewCompat.LAYOUT_DIRECTION_RTL;
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
