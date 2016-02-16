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

import android.os.Bundle;
import android.support.v4.app.test.FragmentTestActivity;
import android.support.v4.test.R;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Miscellaneous tests for fragments that aren't big enough to belong to their own classes.
 */
public class FragmentTest extends
        ActivityInstrumentationTestCase2<FragmentTestActivity> {
    private FragmentTestActivity mActivity;
    private static AtomicInteger sOrder = new AtomicInteger();

    public FragmentTest() {
        super(FragmentTestActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
    }

    @UiThreadTest
    public void testOnCreateOrder() throws Throwable {
        OrderFragment fragment1 = new OrderFragment();
        OrderFragment fragment2 = new OrderFragment();
        mActivity.getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.content, fragment1)
                .add(R.id.content, fragment2)
                .commitNow();
        assertEquals(0, fragment1.createOrder);
        assertEquals(1, fragment2.createOrder);
    }

    public static class OrderFragment extends Fragment {
        public int createOrder = -1;

        public OrderFragment() {}

        @Override
        public void onCreate(Bundle savedInstanceState) {
            createOrder = sOrder.getAndIncrement();
            super.onCreate(savedInstanceState);
        }
    }
}
