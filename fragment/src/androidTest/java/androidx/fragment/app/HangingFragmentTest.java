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

package androidx.fragment.app;

import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import androidx.fragment.app.test.HangingFragmentActivity;
import androidx.testutils.FragmentActivityUtils;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class HangingFragmentTest {

    @Rule
    public ActivityTestRule<HangingFragmentActivity> mActivityRule =
            new ActivityTestRule<>(HangingFragmentActivity.class);

    @Test
    public void testNoCrash() throws InterruptedException {
        HangingFragmentActivity newActivity = FragmentActivityUtils.recreateActivity(
                mActivityRule, mActivityRule.getActivity());
        Assert.assertNotNull(newActivity);
    }
}
