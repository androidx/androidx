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

package androidx.core.view;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.BaseInstrumentationTestCase;
import android.view.ViewGroup;

import androidx.core.test.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(AndroidJUnit4.class)
@MediumTest
public class ViewGroupCompatTest extends BaseInstrumentationTestCase<ViewCompatActivity> {

    private ViewGroup mViewGroup;

    public ViewGroupCompatTest() {
        super(ViewCompatActivity.class);
    }

    @Before
    public void setUp() {
        final Activity activity = mActivityTestRule.getActivity();
        mViewGroup = activity.findViewById(R.id.container);
    }

    @Test
    public void isTransitionGroup() {
        assertFalse(ViewGroupCompat.isTransitionGroup(mViewGroup));
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                ViewCompat.setBackground(mViewGroup, new ColorDrawable(Color.GRAY));
            }
        });
        assertTrue(ViewGroupCompat.isTransitionGroup(mViewGroup));
    }

    @Test
    public void setTransitionGroup() {
        assertFalse(ViewGroupCompat.isTransitionGroup(mViewGroup));
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                ViewGroupCompat.setTransitionGroup(mViewGroup, true);
            }
        });
        assertTrue(ViewGroupCompat.isTransitionGroup(mViewGroup));
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                ViewGroupCompat.setTransitionGroup(mViewGroup, false);
            }
        });
        assertFalse(ViewGroupCompat.isTransitionGroup(mViewGroup));
    }

}
