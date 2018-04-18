/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.vectordrawable.graphics.drawable.tests;

import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.view.animation.Interpolator;

import androidx.vectordrawable.graphics.drawable.AnimationUtilsCompat;
import androidx.vectordrawable.test.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

@MediumTest
@RunWith(Parameterized.class)
public class PathInterpolatorValueParameterizedTest {
    private static final float EPSILON = 1e-3f;
    @Rule
    public ActivityTestRule<DrawableStubActivity> mActivityRule =
            new ActivityTestRule<>(DrawableStubActivity.class);

    private Activity mActivity = null;
    private int mResId;
    private float mExpected;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {R.interpolator.control_points_interpolator, 0.89f},
                {R.interpolator.single_control_point_interpolator, 0.086f},
                {R.interpolator.path_interpolator, 0.85f}
        });
    }

    public PathInterpolatorValueParameterizedTest(final int resId, float expected)
            throws Throwable {
        mResId = resId;
        mExpected = expected;
    }

    @Before
    public void setup() {
        mActivity = mActivityRule.getActivity();
    }

    @Test
    public void testPathInterpolator() throws Exception {
        Interpolator interpolator = AnimationUtilsCompat.loadInterpolator(mActivity, mResId);
        float value = interpolator.getInterpolation(0.5f);
        float delta = Math.abs(value - mExpected);
        assertTrue("value " + value + " is different than expected " + mExpected, delta < EPSILON);
    }
}
