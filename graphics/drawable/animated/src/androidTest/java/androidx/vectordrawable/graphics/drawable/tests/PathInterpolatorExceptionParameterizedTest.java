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

import android.app.Activity;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;

import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat;
import androidx.vectordrawable.test.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@MediumTest
@RunWith(Parameterized.class)
public class PathInterpolatorExceptionParameterizedTest {
    @Rule
    public ActivityTestRule<DrawableStubActivity> mActivityRule =
            new ActivityTestRule<>(DrawableStubActivity.class);


    private Activity mActivity = null;
    private int mResId;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Parameterized.Parameters
    public static Object[] data() {
        return new Object[]{
                R.drawable.animation_path_interpolator_exception_1, // missing control point
                R.drawable.animation_path_interpolator_exception_2, // missing control points
                R.drawable.animation_path_interpolator_exception_3, // not from 0,0 to 1,1
                R.drawable.animation_path_interpolator_exception_4, // loop back
                R.drawable.animation_path_interpolator_exception_5, // 2 contour
        };
    }

    public PathInterpolatorExceptionParameterizedTest(final int resId) throws Throwable {
        mResId = resId;
    }

    @Before
    public void setup() {
        mActivity = mActivityRule.getActivity();
    }

    @Test
    public void testPathMorphingExceptions() throws Exception {
        thrown.expect(RuntimeException.class);
        final AnimatedVectorDrawableCompat avd = AnimatedVectorDrawableCompat.create(mActivity,
                mResId);
    }
}
