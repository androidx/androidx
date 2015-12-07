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
package android.support.percent;

import android.support.percent.PercentFrameLayout;
import android.support.percent.TestFrameActivity;
import android.support.percent.test.R;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.test.suitebuilder.annotation.SmallTest;
import android.view.View;

public class PercentFrameTest extends ActivityInstrumentationTestCase2<TestFrameActivity> {
    private PercentFrameLayout mPercentFrameLayout;
    private View mChild;

    public PercentFrameTest() {
        super("android.support.percent", TestFrameActivity.class);
    }

    public void setUp() throws Exception {
        super.setUp();

        final TestFrameActivity activity = getActivity();
        mPercentFrameLayout = (PercentFrameLayout) activity.findViewById(R.id.percent_frame);
        mChild = mPercentFrameLayout.findViewById(R.id.percent_child);
    }

    @UiThreadTest
    @SmallTest
    public void testBasics() {
        int containerWidth = mPercentFrameLayout.getWidth();
        int containerHeight = mPercentFrameLayout.getHeight();

        int childWidth = mChild.getWidth();
        int childHeight = mChild.getHeight();
        int childLeft = mChild.getLeft();
        int childTop = mChild.getTop();

        assertEquals("Child width as 50% of the container", containerWidth / 2, childWidth);
        assertEquals("Child height as 50% of the container", containerHeight / 2, childHeight);
        assertEquals("Child left margin as 20% of the container", containerWidth / 5, childLeft);
        assertEquals("Child top margin as 20% of the container", containerHeight / 5, childTop);
    }
}
