/*
 * Copyright (C) 2016 The Android Open Source Project
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


package android.support.v4.app;

import android.support.v4.app.test.FragmentTestActivity;
import android.support.v4.app.test.FragmentTestActivity.ParentFragment;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.test.suitebuilder.annotation.SmallTest;

@SmallTest
public class ChildFragmentStateTest extends ActivityInstrumentationTestCase2<FragmentTestActivity> {
    public ChildFragmentStateTest() {
        super(FragmentTestActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @UiThreadTest
    public void testChildFragmentOrdering() throws Throwable {
        FragmentTestActivity.ParentFragment parent = new ParentFragment();
        FragmentManager fm = getActivity().getSupportFragmentManager();
        fm.beginTransaction().add(parent, "parent").commit();
        fm.executePendingTransactions();
        assertTrue(parent.wasAttachedInTime);
    }
}
