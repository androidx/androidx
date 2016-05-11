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
import android.support.fragment.test.R;
import android.support.v4.app.test.FragmentTestActivity;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Miscellaneous tests for fragments that aren't big enough to belong to their own classes.
 */
public class FragmentTest extends
        ActivityInstrumentationTestCase2<FragmentTestActivity> {
    private FragmentTestActivity mActivity;

    public FragmentTest() {
        super(FragmentTestActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
    }

    @SmallTest
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

    @SmallTest
    public void testChildFragmentManagerGone() throws Throwable {
        final FragmentA fragmentA = new FragmentA();
        final FragmentB fragmentB = new FragmentB();
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.getSupportFragmentManager().beginTransaction()
                        .add(R.id.content, fragmentA)
                        .commitNow();
            }
        });
        getInstrumentation().waitForIdleSync();
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(R.anim.long_fade_in, R.anim.long_fade_out,
                                R.anim.long_fade_in, R.anim.long_fade_out)
                        .replace(R.id.content, fragmentB)
                        .addToBackStack(null)
                        .commit();
            }
        });
        // Wait for the middle of the animation
        Thread.sleep(150);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(R.anim.long_fade_in, R.anim.long_fade_out,
                                R.anim.long_fade_in, R.anim.long_fade_out)
                        .replace(R.id.content, fragmentA)
                        .addToBackStack(null)
                        .commit();
            }
        });
        // Wait for the middle of the animation
        Thread.sleep(150);
        getInstrumentation().waitForIdleSync();
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.getSupportFragmentManager().popBackStack();
            }
        });
        // Wait for the middle of the animation
        Thread.sleep(150);
        getInstrumentation().waitForIdleSync();
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.getSupportFragmentManager().popBackStack();
            }
        });
    }

    @MediumTest
    @UiThreadTest
    public void testViewOrder() throws Throwable {
        FragmentA fragmentA = new FragmentA();
        FragmentB fragmentB = new FragmentB();
        FragmentC fragmentC = new FragmentC();
        mActivity.getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.content, fragmentA)
                .add(R.id.content, fragmentB)
                .add(R.id.content, fragmentC)
                .commitNow();
        ViewGroup content = (ViewGroup) mActivity.findViewById(R.id.content);
        assertEquals(3, content.getChildCount());
        assertNotNull(content.getChildAt(0).findViewById(R.id.textA));
        assertNotNull(content.getChildAt(1).findViewById(R.id.textB));
        assertNotNull(content.getChildAt(2).findViewById(R.id.textC));
    }

    public static class OrderFragment extends Fragment {
        private static AtomicInteger sOrder = new AtomicInteger();
        public int createOrder = -1;

        public OrderFragment() {}

        @Override
        public void onCreate(Bundle savedInstanceState) {
            createOrder = sOrder.getAndIncrement();
            super.onCreate(savedInstanceState);
        }
    }

    public static class FragmentA extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_a, container, false);
        }
    }

    public static class FragmentB extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_b, container, false);
        }
    }

    public static class FragmentC extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_c, container, false);
        }
    }
}
