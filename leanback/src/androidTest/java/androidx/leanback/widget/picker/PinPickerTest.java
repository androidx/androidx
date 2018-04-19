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

package androidx.leanback.widget.picker;


import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.pressKey;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.KeyEvent;

import androidx.leanback.test.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class PinPickerTest {
    @Rule
    public final ActivityTestRule<PinPickerActivity> mActivityTestRule;

    public PinPickerTest() {
        mActivityTestRule = new ActivityTestRule<>(PinPickerActivity.class);
    }

    private PinPicker mPinPicker;

    @Before
    public void setUp() {
        mPinPicker = mActivityTestRule.getActivity().findViewById(R.id.test_picker);
    }

    @Test
    @LargeTest
    public void keyInputTest() {
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mPinPicker.resetPin();
            }
        });
        onView(withId(R.id.test_picker)).perform(pressKey(KeyEvent.KEYCODE_1));
        onView(withId(R.id.test_picker)).perform(pressKey(KeyEvent.KEYCODE_2));
        onView(withId(R.id.test_picker)).perform(pressKey(KeyEvent.KEYCODE_3));
        onView(withId(R.id.test_picker)).perform(pressKey(KeyEvent.KEYCODE_4));
        assertThat("keyboard input should set pin", mPinPicker.getPin(), is("1234"));

        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mPinPicker.resetPin();
            }
        });
        assertThat("resetPin should reset pin", mPinPicker.getPin(), is("0000"));
    }

    @Test
    @LargeTest
    public void dpadInputTest() {
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mPinPicker.resetPin();
            }
        });
        onView(withId(R.id.test_picker)).perform(pressKey(KeyEvent.KEYCODE_DPAD_DOWN));
        onView(withId(R.id.test_picker)).perform(pressKey(KeyEvent.KEYCODE_DPAD_CENTER));
        onView(withId(R.id.test_picker)).perform(pressKey(KeyEvent.KEYCODE_DPAD_DOWN));
        onView(withId(R.id.test_picker)).perform(pressKey(KeyEvent.KEYCODE_DPAD_DOWN));
        onView(withId(R.id.test_picker)).perform(pressKey(KeyEvent.KEYCODE_DPAD_CENTER));
        onView(withId(R.id.test_picker)).perform(pressKey(KeyEvent.KEYCODE_DPAD_DOWN));
        onView(withId(R.id.test_picker)).perform(pressKey(KeyEvent.KEYCODE_DPAD_DOWN));
        onView(withId(R.id.test_picker)).perform(pressKey(KeyEvent.KEYCODE_DPAD_DOWN));
        onView(withId(R.id.test_picker)).perform(pressKey(KeyEvent.KEYCODE_DPAD_CENTER));
        onView(withId(R.id.test_picker)).perform(pressKey(KeyEvent.KEYCODE_DPAD_DOWN));
        onView(withId(R.id.test_picker)).perform(pressKey(KeyEvent.KEYCODE_DPAD_DOWN));
        onView(withId(R.id.test_picker)).perform(pressKey(KeyEvent.KEYCODE_DPAD_DOWN));
        onView(withId(R.id.test_picker)).perform(pressKey(KeyEvent.KEYCODE_DPAD_DOWN));
        onView(withId(R.id.test_picker)).perform(pressKey(KeyEvent.KEYCODE_DPAD_CENTER));
        assertThat("dpad input should set pin", mPinPicker.getPin(), is("1234"));
    }
}
