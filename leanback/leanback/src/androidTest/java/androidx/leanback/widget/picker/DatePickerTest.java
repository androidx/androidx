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
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.leanback.test.R;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
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
public class DatePickerTest {

    private static final String TAG = "DatePickerTest";
    private static final long TRANSITION_LENGTH = 1000;

    private Context mContext;
    private View mViewAbove;
    private DatePicker mDatePickerView;
    private ViewGroup mDatePickerInnerView;
    private View mViewBelow;

    @Rule
    public ActivityTestRule<DatePickerActivity> mActivityTestRule =
            new ActivityTestRule<>(DatePickerActivity.class, false, false);
    private DatePickerActivity mActivity;

    @Before
    public void setUp() throws Exception {
        mContext = ApplicationProvider.getApplicationContext();
    }

    public void initActivity(Intent intent) throws Throwable {
        mActivity = mActivityTestRule.launchActivity(intent);
        mDatePickerView = mActivity.findViewById(R.id.date_picker);
        mDatePickerInnerView = mDatePickerView.findViewById(androidx.leanback.R.id.picker);
        mDatePickerView.setActivatedVisibleItemCount(3);
        mDatePickerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDatePickerView.setActivated(!mDatePickerView.isActivated());
            }
        });
        if (intent.getIntExtra(DatePickerActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.datepicker_with_other_widgets) == R.layout.datepicker_with_other_widgets) {
            mViewAbove = mActivity.findViewById(R.id.above_picker);
            mViewBelow = mActivity.findViewById(R.id.below_picker);
        } else if (intent.getIntExtra(DatePickerActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.datepicker_with_other_widgets) == R.layout.datepicker_alone) {
            // A layout with only a DatePicker widget that is initially activated.
            mActivityTestRule.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mDatePickerView.setActivated(true);
                }
            });
            Thread.sleep(500);
        }
    }

    @Test
    public void testFocusTravel() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(DatePickerActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.datepicker_with_other_widgets);
        initActivity(intent);

        assertThat("TextView above should have focus initially", mViewAbove.hasFocus(), is(true));

        sendKeys(KeyEvent.KEYCODE_DPAD_DOWN);
        Thread.sleep(TRANSITION_LENGTH);
        assertThat("DatePicker should have focus now", mDatePickerView.isFocused(), is(true));

        sendKeys(KeyEvent.KEYCODE_DPAD_CENTER);
        Thread.sleep(TRANSITION_LENGTH);
        assertThat("The first column of DatePicker should hold focus",
                mDatePickerInnerView.getChildAt(0).hasFocus(), is(true));

        // skipping the separator in the child indices
        sendKeys(KeyEvent.KEYCODE_DPAD_RIGHT);
        Thread.sleep(TRANSITION_LENGTH);
        assertThat("The second column of DatePicker should hold focus",
                mDatePickerInnerView.getChildAt(2).hasFocus(), is(true));

        sendKeys(KeyEvent.KEYCODE_DPAD_RIGHT);
        Thread.sleep(TRANSITION_LENGTH);
        assertThat("The third column of DatePicker should hold focus",
                mDatePickerInnerView.getChildAt(4).hasFocus(), is(true));
    }

    @Test
    public void testFocusRetainedForASelectedColumn()
            throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(DatePickerActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.datepicker_with_other_widgets);
        initActivity(intent);
        mDatePickerView.setFocusable(true);
        sendKeys(KeyEvent.KEYCODE_DPAD_DOWN);
        Thread.sleep(TRANSITION_LENGTH);

        assertThat("DatePicker should have focus when it's focusable",
                mDatePickerView.isFocused(), is(true));


        sendKeys(KeyEvent.KEYCODE_DPAD_CENTER);
        Thread.sleep(TRANSITION_LENGTH);
        assertThat("After the first activation, the first column of DatePicker should hold focus",
                mDatePickerInnerView.getChildAt(0).hasFocus(), is(true));

        sendKeys(KeyEvent.KEYCODE_DPAD_RIGHT);
        Thread.sleep(TRANSITION_LENGTH);

        sendKeys(KeyEvent.KEYCODE_DPAD_RIGHT);
        Thread.sleep(TRANSITION_LENGTH);
        assertThat("The third column of DatePicker should hold focus",
                mDatePickerInnerView.getChildAt(4).hasFocus(), is(true));

        sendKeys(KeyEvent.KEYCODE_DPAD_CENTER);
        Thread.sleep(TRANSITION_LENGTH);
        assertThat("After the first deactivation, the DatePicker itself should hold focus",
                mDatePickerView.isFocused(), is(true));

        sendKeys(KeyEvent.KEYCODE_DPAD_CENTER);
        Thread.sleep(TRANSITION_LENGTH);
        assertThat("After the second activation, the last selected column (3rd) should hold focus",
                mDatePickerInnerView.getChildAt(4).hasFocus(), is(true));
    }

    @Test
    public void testFocusSkippedWhenDatePickerUnFocusable()
            throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(DatePickerActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.datepicker_with_other_widgets);
        initActivity(intent);

        mDatePickerView.setFocusable(false);
        assertThat("TextView above should have focus initially.", mViewAbove.hasFocus(), is(true));

        sendKeys(KeyEvent.KEYCODE_DPAD_DOWN);
        Thread.sleep(TRANSITION_LENGTH);

        assertThat("DatePicker should be skipped and TextView below should have focus.",
                mViewBelow.hasFocus(), is(true));
    }

    @Test
    public void testTemporaryFocusLossWhenDeactivated()
            throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(DatePickerActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.datepicker_with_other_widgets);
        initActivity(intent);

        final int[] currentFocusChangeCountForViewAbove = {0};
        mDatePickerView.setFocusable(true);
        Log.d(TAG, "view above: " + mViewAbove);
        mViewAbove.setOnFocusChangeListener(new View.OnFocusChangeListener(){
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                currentFocusChangeCountForViewAbove[0]++;
            }
        });
        assertThat("TextView above should have focus initially.", mViewAbove.hasFocus(), is(true));

        // Traverse to the third column of date picker
        sendKeys(KeyEvent.KEYCODE_DPAD_DOWN);
        Thread.sleep(TRANSITION_LENGTH);
        // Click once to activate
        sendKeys(KeyEvent.KEYCODE_DPAD_CENTER);
        Thread.sleep(TRANSITION_LENGTH);
        // Traverse to the third column
        sendKeys(KeyEvent.KEYCODE_DPAD_RIGHT);
        Thread.sleep(TRANSITION_LENGTH);
        sendKeys(KeyEvent.KEYCODE_DPAD_RIGHT);
        Thread.sleep(TRANSITION_LENGTH);
        // Click to deactivate. Before that we remember the focus change count for the view above.
        // This view should NOT receive temporary focus when DatePicker is deactivated, and
        // DatePicker itself should capture the focus.
        int[] lastFocusChangeCountForViewAbove = {currentFocusChangeCountForViewAbove[0]};
        sendKeys(KeyEvent.KEYCODE_DPAD_CENTER);
        Thread.sleep(TRANSITION_LENGTH);
        assertThat("DatePicker should have focus now since it's focusable",
                mDatePickerView.isFocused(), is(true));
        assertThat("Focus change count of view above should not be changed after last click.",
                currentFocusChangeCountForViewAbove[0], is(lastFocusChangeCountForViewAbove[0]));
    }

    @Test
    public void testTemporaryFocusLossWhenActivated() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(DatePickerActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.datepicker_with_other_widgets);
        initActivity(intent);
        final int[] currentFocusChangeCountForColumns = {0, 0, 0};
        mDatePickerView.setFocusable(true);
        mDatePickerInnerView.getChildAt(0).setOnFocusChangeListener(
                new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                        currentFocusChangeCountForColumns[0]++;
                    }
                });

        mDatePickerInnerView.getChildAt(2).setOnFocusChangeListener(
                new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                        currentFocusChangeCountForColumns[1]++;
                    }
                });

        mDatePickerInnerView.getChildAt(4).setOnFocusChangeListener(
                new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                        currentFocusChangeCountForColumns[2]++;
                    }
                });

        // Traverse to the third column of date picker
        sendKeys(KeyEvent.KEYCODE_DPAD_DOWN);
        Thread.sleep(TRANSITION_LENGTH);
        // Click once to activate
        sendKeys(KeyEvent.KEYCODE_DPAD_CENTER);
        Thread.sleep(TRANSITION_LENGTH);
        // Traverse to the third column
        sendKeys(KeyEvent.KEYCODE_DPAD_RIGHT);
        Thread.sleep(TRANSITION_LENGTH);
        sendKeys(KeyEvent.KEYCODE_DPAD_RIGHT);
        Thread.sleep(TRANSITION_LENGTH);
        // Click to deactivate
        sendKeys(KeyEvent.KEYCODE_DPAD_CENTER);
        Thread.sleep(TRANSITION_LENGTH);
        // Click again. The focus should NOT be temporarily moved to the other columns and the third
        // column should receive focus.
        // Before that we will remember the last focus change count to compare it against after the
        // click.
        int[] lastFocusChangeCountForColumns = {currentFocusChangeCountForColumns[0],
                currentFocusChangeCountForColumns[1], currentFocusChangeCountForColumns[2]};
        sendKeys(KeyEvent.KEYCODE_DPAD_CENTER);
        Thread.sleep(TRANSITION_LENGTH);
        assertThat("Focus change count of column 0 should not be changed after last click.",
                currentFocusChangeCountForColumns[0], is(lastFocusChangeCountForColumns[0]));
        assertThat("Focus change count of column 1 should not be changed after last click.",
                currentFocusChangeCountForColumns[1], is(lastFocusChangeCountForColumns[1]));
        assertThat("Focus change count of column 2 should not be changed after last click.",
                currentFocusChangeCountForColumns[2], is(lastFocusChangeCountForColumns[2]));
    }

    @Test
    public void testInitiallyActiveDatePicker()
            throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(DatePickerActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.datepicker_alone);
        initActivity(intent);

        assertThat("The first column of DatePicker should initially hold focus",
                mDatePickerInnerView.getChildAt(0).hasFocus(), is(true));

        // focus on first column
        sendKeys(KeyEvent.KEYCODE_DPAD_DOWN);
        Thread.sleep(TRANSITION_LENGTH);
        assertThat("The first column of DatePicker should still hold focus after scrolling down",
                mDatePickerInnerView.getChildAt(0).hasFocus(), is(true));

        // focus on second column
        sendKeys(KeyEvent.KEYCODE_DPAD_RIGHT);
        Thread.sleep(TRANSITION_LENGTH);
        assertThat("The second column of DatePicker should hold focus after scrolling right",
                mDatePickerInnerView.getChildAt(2).hasFocus(), is(true));

        sendKeys(KeyEvent.KEYCODE_DPAD_DOWN);
        Thread.sleep(TRANSITION_LENGTH);
        assertThat("The second column of DatePicker should still hold focus after scrolling down",
                mDatePickerInnerView.getChildAt(2).hasFocus(), is(true));

        // focus on third column
        sendKeys(KeyEvent.KEYCODE_DPAD_RIGHT);
        Thread.sleep(TRANSITION_LENGTH);
        assertThat("The third column of DatePicker should hold focus after scrolling right",
                mDatePickerInnerView.getChildAt(4).hasFocus(), is(true));

        sendKeys(KeyEvent.KEYCODE_DPAD_DOWN);
        Thread.sleep(TRANSITION_LENGTH);
        assertThat("The third column of DatePicker should still hold focus after scrolling down",
                mDatePickerInnerView.getChildAt(4).hasFocus(), is(true));
    }

    @Test
    public void testInvisibleColumnsAlpha() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(DatePickerActivity.EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.datepicker_with_other_widgets);
        initActivity(intent);

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mDatePickerView.setDate(2017, 2, 21, false);
            }
        });

        Thread.sleep(TRANSITION_LENGTH);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mDatePickerView.setDate(2017, 2, 20, false);
            }
        });
        Thread.sleep(TRANSITION_LENGTH);

        sendKeys(KeyEvent.KEYCODE_DPAD_DOWN);
        Thread.sleep(TRANSITION_LENGTH);
        // Click once to activate
        sendKeys(KeyEvent.KEYCODE_DPAD_CENTER);
        Thread.sleep(TRANSITION_LENGTH);

        int activeColumn = 0;
        // For the inactive columns: the alpha for all the rows except the selected row should be
        // zero: Picker#mInvisibleColumnAlpha (they should all be invisible).
        for (int i = 0; i < 3; i++) {
            ViewGroup gridView = (ViewGroup) mDatePickerInnerView.getChildAt(2 * i);
            int childCount = gridView.getChildCount();
            int alpha1RowsCount = 0;
            int alphaNonZeroRowsCount = 0;
            for (int j = 0; j < childCount; j++) {
                View pickerItem = gridView.getChildAt(j);
                if (pickerItem.getAlpha() > 0) {
                    alphaNonZeroRowsCount++;
                }
                if (pickerItem.getAlpha() == 1) {
                    alpha1RowsCount++;
                }
            }
            if (i == activeColumn) {
                assertThat("The active column "  + i + " should have only one row with an alpha of "
                                + "1", alpha1RowsCount, is(1));
                assertTrue("The active column "  + i + " should have more than one view with alpha "
                        + "greater than 1", alphaNonZeroRowsCount > 1);
            } else {
                assertThat("The inactive column " + i + " should have only one row with an alpha of"
                        + " 1", alpha1RowsCount, is(1));
                assertThat("The inactive column " + i + " should have only one row with a non-zero"
                        + " alpha", alphaNonZeroRowsCount, is(1));
            }
        }
    }

    @Test
    public void testExtractSeparatorsForDifferentLocales() throws Throwable {
        // date pattern for en_US (English)
        DatePicker datePicker = new DatePicker(mContext, null) {
            @Override
            String getBestYearMonthDayPattern(String datePickerFormat) {
                return "M/d/y";
            }
        };
        List<CharSequence> actualSeparators = datePicker.extractSeparators();
        List<String> expectedSeparators = Arrays.asList("", "/", "/", "");
        assertEquals(expectedSeparators, actualSeparators);

        // date pattern for fa_IR (Farsi)
        datePicker = new DatePicker(mContext, null) {
            @Override
            String getBestYearMonthDayPattern(String datePickerFormat) {
                return "y/M/d";
            }
        };
        actualSeparators = datePicker.extractSeparators();
        expectedSeparators = Arrays.asList("", "/", "/", "");
        assertEquals(expectedSeparators, actualSeparators);

        // date pattern for ar_EG (Arabic)
        datePicker = new DatePicker(mContext, null) {
            @Override
            String getBestYearMonthDayPattern(String datePickerFormat) {
                return "d/M/y";
            }
        };
        actualSeparators = datePicker.extractSeparators();
        expectedSeparators = Arrays.asList("", "/", "/", "");
        assertEquals(expectedSeparators, actualSeparators);

        // date pattern for cs_CZ (Czech)
        datePicker = new DatePicker(mContext, null) {
            @Override
            String getBestYearMonthDayPattern(String datePickerFormat) {
                return "d. M. y";
            }
        };
        actualSeparators = datePicker.extractSeparators();
        expectedSeparators = Arrays.asList("", ".", ".", "");
        assertEquals(expectedSeparators, actualSeparators);

        // date pattern for hr_HR (Croatian)
        datePicker = new DatePicker(mContext, null) {
            @Override
            String getBestYearMonthDayPattern(String datePickerFormat) {
                return "dd. MM. y.";
            }
        };
        actualSeparators = datePicker.extractSeparators();
        expectedSeparators = Arrays.asList("", ".", ".", ".");
        assertEquals(expectedSeparators, actualSeparators);

        // date pattern for hr_HR (Bulgarian)
        datePicker = new DatePicker(mContext, null) {
            @Override
            String getBestYearMonthDayPattern(String datePickerFormat) {
                return "d.MM.y 'r'.";
            }
        };
        actualSeparators = datePicker.extractSeparators();
        expectedSeparators = Arrays.asList("", ".", ".", "r.");
        assertEquals(expectedSeparators, actualSeparators);

        // date pattern for en_XA (English pseudo-locale)
        datePicker = new DatePicker(mContext, null) {
            @Override
            String getBestYearMonthDayPattern(String datePickerFormat) {
                return "[M/d/y]";
            }
        };
        actualSeparators = datePicker.extractSeparators();
        expectedSeparators = Arrays.asList("[", "/", "/", "]");
        assertEquals(expectedSeparators, actualSeparators);
    }

    private void sendKeys(int ...keys) {
        for (int i = 0; i < keys.length; i++) {
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(keys[i]);
        }
    }
}
