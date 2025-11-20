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

package androidx.leanback.widget.picker;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.leanback.test.R;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class TimePickerTest {

    private static final long TRANSITION_LENGTH = 1000;
    private static final long UPDATE_LENGTH = 1000;

    private TimePicker mTimePicker12HourView;
    private TimePicker mTimePicker24HourView;

    @Rule
    public ActivityTestRule<TimePickerActivity> mActivityTestRule =
            new ActivityTestRule<>(TimePickerActivity.class, false, false);
    private TimePickerActivity mActivity;
    private Context mContext;

    @Before
    public void setUp() throws Exception {
        mContext = ApplicationProvider.getApplicationContext();
    }

    public void initActivity(Intent intent) throws Throwable {
        mActivity = mActivityTestRule.launchActivity(intent);
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        mTimePicker12HourView = mActivity.findViewById(R.id.time_picker12);
        mTimePicker12HourView.setActivatedVisibleItemCount(3);
        mTimePicker12HourView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTimePicker12HourView.setActivated(!mTimePicker12HourView.isActivated());
            }
        });

        if (intent.getIntExtra(TimePickerActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.timepicker_with_other_widgets) == R.layout.timepicker_with_other_widgets) {
            mTimePicker24HourView = mActivity.findViewById(R.id.time_picker24);
            mTimePicker24HourView.setActivatedVisibleItemCount(3);
            mTimePicker24HourView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mTimePicker24HourView.setActivated(!mTimePicker24HourView.isActivated());
                }
            });
        } else if (intent.getIntExtra(TimePickerActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.timepicker_with_other_widgets) == R.layout.timepicker_alone) {
            // A layout with only a TimePicker widget that is initially activated.
            mActivityTestRule.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mTimePicker12HourView.setActivated(true);
                }
            });
            Thread.sleep(500);
        }
    }

    @Test
    public void testSetHourIn24hFormat() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(TimePickerActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.timepicker_with_other_widgets);
        initActivity(intent);

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTimePicker24HourView.setHour(0);
            }
        });
        Thread.sleep(UPDATE_LENGTH);
        assertThat("TimePicker in 24-hour mode returns a different hour in getHour()",
                mTimePicker24HourView.getHour(), is(0));

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTimePicker24HourView.setHour(11);
            }
        });
        Thread.sleep(UPDATE_LENGTH);
        assertThat("TimePicker in 24-hour mode returns a different hour in getHour()",
                mTimePicker24HourView.getHour(), is(11));

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTimePicker24HourView.setHour(12);
            }
        });
        Thread.sleep(UPDATE_LENGTH);
        assertThat("TimePicker in 24-hour mode returns a different hour in getHour()",
                mTimePicker24HourView.getHour(), is(12));

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTimePicker24HourView.setHour(13);
            }
        });
        Thread.sleep(UPDATE_LENGTH);
        assertThat("TimePicker in 24-hour mode returns a different hour in getHour()",
                mTimePicker24HourView.getHour(), is(13));

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTimePicker24HourView.setHour(23);
            }
        });
        Thread.sleep(UPDATE_LENGTH);
        assertThat("TimePicker in 24-hour mode returns a different hour in getHour()",
                mTimePicker24HourView.getHour(), is(23));
    }

    @Test
    public void testSetHourIn12hFormat() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(TimePickerActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.timepicker_with_other_widgets);
        initActivity(intent);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTimePicker12HourView.setHour(0);
            }
        });
        Thread.sleep(UPDATE_LENGTH);
        assertThat("TimePicker in 24-hour mode returns a different hour in getHour()",
                mTimePicker12HourView.getHour(), is(0));

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTimePicker12HourView.setHour(11);
            }
        });
        Thread.sleep(UPDATE_LENGTH);
        assertThat("TimePicker in 24-hour mode returns a different hour in getHour()",
                mTimePicker12HourView.getHour(), is(11));

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTimePicker12HourView.setHour(12);
            }
        });
        Thread.sleep(UPDATE_LENGTH);
        assertThat("TimePicker in 24-hour mode returns a different hour in getHour()",
                mTimePicker12HourView.getHour(), is(12));

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTimePicker12HourView.setHour(13);
            }
        });
        Thread.sleep(UPDATE_LENGTH);
        assertThat("TimePicker in 24-hour mode returns a different hour in getHour()",
                mTimePicker12HourView.getHour(), is(13));

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTimePicker12HourView.setHour(23);
            }
        });
        Thread.sleep(UPDATE_LENGTH);
        assertThat("TimePicker in 24-hour mode returns a different hour in getHour()",
                mTimePicker12HourView.getHour(), is(23));
    }

    @Test
    public void testSetMinuteIn24hFormat() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(TimePickerActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.timepicker_with_other_widgets);
        initActivity(intent);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTimePicker24HourView.setMinute(0);
            }
        });
        Thread.sleep(UPDATE_LENGTH);
        assertThat("TimePicker in 24-hour mode returns a different hour in getHour()",
                mTimePicker24HourView.getMinute(), is(0));

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTimePicker24HourView.setMinute(11);
            }
        });
        Thread.sleep(UPDATE_LENGTH);
        assertThat("TimePicker in 24-hour mode returns a different hour in getHour()",
                mTimePicker24HourView.getMinute(), is(11));

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTimePicker24HourView.setMinute(59);
            }
        });
        Thread.sleep(UPDATE_LENGTH);
        assertThat("TimePicker in 24-hour mode returns a different hour in getHour()",
                mTimePicker24HourView.getMinute(), is(59));
    }

    @Test
    public void testSetMinuteIn12hFormat() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(TimePickerActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.timepicker_with_other_widgets);
        initActivity(intent);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTimePicker12HourView.setMinute(0);
            }
        });
        Thread.sleep(UPDATE_LENGTH);
        assertThat("TimePicker in 24-hour mode returns a different hour in getHour()",
                mTimePicker12HourView.getMinute(), is(0));

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTimePicker12HourView.setMinute(11);
            }
        });
        Thread.sleep(UPDATE_LENGTH);
        assertThat("TimePicker in 24-hour mode returns a different hour in getHour()",
                mTimePicker12HourView.getMinute(), is(11));

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTimePicker12HourView.setMinute(59);
            }
        });
        Thread.sleep(UPDATE_LENGTH);
        assertThat("TimePicker in 24-hour mode returns a different hour in getHour()",
                mTimePicker12HourView.getMinute(), is(59));

    }

    @Test
    public void testAmToPmTransition() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(TimePickerActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.timepicker_with_other_widgets);
        initActivity(intent);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTimePicker12HourView.setHour(0);
                mTimePicker12HourView.setMinute(47);
            }
        });
        Thread.sleep(UPDATE_LENGTH);
        assertThat("TimePicker in 12-hour mode returns a different hour in getHour()",
                mTimePicker12HourView.getHour(), is(0));
        assertThat("TimePicker in 12-hour mode returns a different hour in getMinute()",
                mTimePicker12HourView.getMinute(), is(47));

        // traverse to the AM/PM column of 12 hour TimePicker widget
        sendKeys(KeyEvent.KEYCODE_DPAD_DOWN);
        Thread.sleep(TRANSITION_LENGTH);
        // Click once to activate
        sendKeys(KeyEvent.KEYCODE_DPAD_CENTER);
        Thread.sleep(TRANSITION_LENGTH);
        sendKeys(KeyEvent.KEYCODE_DPAD_RIGHT);
        Thread.sleep(TRANSITION_LENGTH);
        sendKeys(KeyEvent.KEYCODE_DPAD_RIGHT);
        Thread.sleep(TRANSITION_LENGTH);
        // scroll down to PM value
        sendKeys(KeyEvent.KEYCODE_DPAD_DOWN);
        Thread.sleep(TRANSITION_LENGTH);
        // Click now to deactivate
        sendKeys(KeyEvent.KEYCODE_DPAD_CENTER);
        Thread.sleep(TRANSITION_LENGTH);

        assertThat("TimePicker in 24-hour mode returns a different hour in getHour() returns",
                mTimePicker12HourView.getHour(), is(12));
        assertThat("TimePicker in 24-hour mode returns a different hour in getHour() returns",
                mTimePicker12HourView.getMinute(), is(47));
    }

    @Test
    public void testPmToAmTransition() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(TimePickerActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.timepicker_with_other_widgets);
        initActivity(intent);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTimePicker12HourView.setHour(12);
                mTimePicker12HourView.setMinute(47);
            }
        });
        Thread.sleep(UPDATE_LENGTH);
        assertThat("TimePicker in 12-hour mode returns a different hour in getHour()",
                mTimePicker12HourView.getHour(), is(12));
        assertThat("TimePicker in 12-hour mode returns a different hour in getMinute()",
                mTimePicker12HourView.getMinute(), is(47));

        // traverse to the AM/PM column of 12 hour TimePicker widget
        sendKeys(KeyEvent.KEYCODE_DPAD_DOWN);
        Thread.sleep(TRANSITION_LENGTH);
        // Click once to activate
        sendKeys(KeyEvent.KEYCODE_DPAD_CENTER);
        Thread.sleep(TRANSITION_LENGTH);
        sendKeys(KeyEvent.KEYCODE_DPAD_RIGHT);
        Thread.sleep(TRANSITION_LENGTH);
        sendKeys(KeyEvent.KEYCODE_DPAD_RIGHT);
        Thread.sleep(TRANSITION_LENGTH);
        // scroll down to PM value
        sendKeys(KeyEvent.KEYCODE_DPAD_UP);
        Thread.sleep(TRANSITION_LENGTH);
        // Click now to deactivate
        sendKeys(KeyEvent.KEYCODE_DPAD_CENTER);
        Thread.sleep(TRANSITION_LENGTH);

        assertThat("TimePicker in 24-hour mode returns a different hour in getHour() returns",
                mTimePicker12HourView.getHour(), is(0));
        assertThat("TimePicker in 24-hour mode returns a different hour in getHour() returns",
                mTimePicker12HourView.getMinute(), is(47));
    }

    @Test
    public void test12To24HourFormatTransition() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(TimePickerActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.timepicker_with_other_widgets);
        initActivity(intent);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTimePicker12HourView.setHour(14);
                mTimePicker12HourView.setMinute(47);
            }
        });
        Thread.sleep(UPDATE_LENGTH);
        assertThat("TimePicker should be in 12-hour format.", mTimePicker12HourView.is24Hour(),
                is(false));
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTimePicker12HourView.setIs24Hour(true);
            }
        });
        Thread.sleep(UPDATE_LENGTH);
        assertThat("TimePicker should now be in 24-hour format.", mTimePicker12HourView.is24Hour(),
                is(true));
        // The hour and minute should not be changed.
        assertThat("TimePicker in 24-hour mode returns a different hour in getHour() returns",
                mTimePicker12HourView.getHour(), is(14));
        assertThat("TimePicker in 24-hour mode returns a different hour in getHour() returns",
                mTimePicker12HourView.getMinute(), is(47));
    }

    @Test
    public void test24To12HourFormatTransition() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(TimePickerActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.timepicker_with_other_widgets);
        initActivity(intent);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTimePicker24HourView.setHour(14);
                mTimePicker24HourView.setMinute(47);
            }
        });
        Thread.sleep(UPDATE_LENGTH);
        assertThat("TimePicker should be in 12-hour format.", mTimePicker24HourView.is24Hour(),
                is(true));
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTimePicker24HourView.setIs24Hour(false);
            }
        });
        Thread.sleep(UPDATE_LENGTH);
        assertThat("TimePicker should now be in 24-hour format.", mTimePicker24HourView.is24Hour(),
                is(false));
        // The hour and minute should not be changed.
        assertThat("TimePicker in 24-hour mode returns a different hour in getHour() returns",
                mTimePicker24HourView.getHour(), is(14));
        assertThat("TimePicker in 24-hour mode returns a different hour in getHour() returns",
                mTimePicker24HourView.getMinute(), is(47));
    }

    @Test
    @SdkSuppress(maxSdkVersion = 33) // b/262909049: Failing on SDK 34
    public void testInitiallyActiveTimePicker()
            throws Throwable {
        if (Build.VERSION.SDK_INT == 33 && !"REL".equals(Build.VERSION.CODENAME)) {
            return; // b/262909049: Do not run this test on pre-release Android U.
        }

        Intent intent = new Intent();
        intent.putExtra(TimePickerActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.timepicker_alone);
        initActivity(intent);

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTimePicker12HourView.setHour(14);
                mTimePicker12HourView.setMinute(47);
            }
        });
        Thread.sleep(UPDATE_LENGTH);

        ViewGroup mTimePickerInnerView = mTimePicker12HourView.findViewById(
                androidx.leanback.R.id.picker);

        assertThat("The first column of TimePicker should initially hold focus",
                mTimePickerInnerView.getChildAt(0).hasFocus(), is(true));

        // focus on first column
        sendKeys(KeyEvent.KEYCODE_DPAD_DOWN);
        Thread.sleep(TRANSITION_LENGTH);
        assertThat("The first column of TimePicker should still hold focus after scrolling down",
                mTimePickerInnerView.getChildAt(0).hasFocus(), is(true));

        // focus on second column
        sendKeys(KeyEvent.KEYCODE_DPAD_RIGHT);
        Thread.sleep(TRANSITION_LENGTH);
        assertThat("The second column of TimePicker should hold focus after scrolling right",
                mTimePickerInnerView.getChildAt(2).hasFocus(), is(true));

        sendKeys(KeyEvent.KEYCODE_DPAD_DOWN);
        Thread.sleep(TRANSITION_LENGTH);
        assertThat("The second column of TimePicker should still hold focus after scrolling down",
                mTimePickerInnerView.getChildAt(2).hasFocus(), is(true));

        // focus on third column
        sendKeys(KeyEvent.KEYCODE_DPAD_RIGHT);
        Thread.sleep(TRANSITION_LENGTH);
        assertThat("The third column of TimePicker should hold focus after scrolling right",
                mTimePickerInnerView.getChildAt(3).hasFocus(), is(true));

        sendKeys(KeyEvent.KEYCODE_DPAD_UP);
        Thread.sleep(TRANSITION_LENGTH);
        assertThat("The third column of TimePicker should still hold focus after scrolling down",
                mTimePickerInnerView.getChildAt(3).hasFocus(), is(true));
    }

    @Test
    public void testExtractSeparatorsForDifferentLocales() throws Throwable {
        // A typical time pattern for different locales in 12-hour format
        TimePicker timePicker = new TimePicker(mContext, null) {
            @Override
            String getBestHourMinutePattern() {
                return "h:mm a";
            }
        };
        List<CharSequence> actualSeparators = timePicker.extractSeparators();
        List<String> expectedSeparators = Arrays.asList("", ":", "", "");
        assertEquals(expectedSeparators, actualSeparators);

        // time pattern for ja_JP in 12 hour format
        timePicker = new TimePicker(mContext, null) {
            @Override
            String getBestHourMinutePattern() {
                return "aK:mm";
            }

            @Override
            public boolean is24Hour() {
                return false;
            }
        };
        actualSeparators = timePicker.extractSeparators();
        expectedSeparators = Arrays.asList("", "", ":", "");
        assertEquals(expectedSeparators, actualSeparators);

        // time pattern for fr_CA in 24 hour format
        timePicker = new TimePicker(mContext, null) {
            @Override
            String getBestHourMinutePattern() {
                return "HH 'h' mm";
            }

            @Override
            public boolean is24Hour() {
                return true;
            }
        };
        actualSeparators = timePicker.extractSeparators();
        expectedSeparators = Arrays.asList("", "h", "");
        assertEquals(expectedSeparators, actualSeparators);

        // time pattern for hsb_DE in 24 hour format
        timePicker = new TimePicker(mContext, null) {
            @Override
            String getBestHourMinutePattern() {
                return "H:mm 'hodz'";
            }

            @Override
            public boolean is24Hour() {
                return true;
            }
        };
        actualSeparators = timePicker.extractSeparators();
        expectedSeparators = Arrays.asList("", ":", "hodz");
        assertEquals(expectedSeparators, actualSeparators);

        // time pattern for ko_KR in 12 hour format
        timePicker = new TimePicker(mContext, null) {
            @Override
            String getBestHourMinutePattern() {
                return "a h:mm";
            }

            @Override
            public boolean is24Hour() {
                return false;
            }
        };
        actualSeparators = timePicker.extractSeparators();
        expectedSeparators = Arrays.asList("", "", ":", "");
        assertEquals(expectedSeparators, actualSeparators);

        // time pattern for fa_IR in 24 hour format
        timePicker = new TimePicker(mContext, null) {
            @Override
            String getBestHourMinutePattern() {
                return "H:mm";
            }

            @Override
            public boolean is24Hour() {
                return true;
            }
        };
        actualSeparators = timePicker.extractSeparators();
        expectedSeparators = Arrays.asList("", ":", "");
        assertEquals(expectedSeparators, actualSeparators);
    }

    private void sendKeys(int ...keys) {
        for (int i = 0; i < keys.length; i++) {
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(keys[i]);
        }
    }
}
