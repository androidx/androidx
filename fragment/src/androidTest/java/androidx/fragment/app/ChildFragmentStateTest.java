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

import static org.junit.Assert.assertTrue;

import android.support.test.annotation.UiThreadTest;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import androidx.fragment.app.test.FragmentTestActivity;
import androidx.fragment.app.test.FragmentTestActivity.ParentFragment;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ChildFragmentStateTest {
    @Rule
    public ActivityTestRule<FragmentTestActivity> mActivityRule =
            new ActivityTestRule<FragmentTestActivity>(FragmentTestActivity.class);

    @Test
    @UiThreadTest
    public void testChildFragmentOrdering() throws Throwable {
        FragmentTestActivity.ParentFragment parent = new ParentFragment();
        FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();
        fm.beginTransaction().add(parent, "parent").commit();
        fm.executePendingTransactions();
        assertTrue(parent.wasAttachedInTime);
    }
}
