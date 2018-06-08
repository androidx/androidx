/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package androidx.leanback.app.wizard;

import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.LinearLayout;

import androidx.leanback.app.GuidedStepFragment;
import androidx.leanback.test.R;
import androidx.leanback.widget.GuidanceStylist;
import androidx.leanback.widget.GuidedAction;
import androidx.leanback.widget.GuidedDatePickerAction;
import androidx.leanback.widget.VerticalGridView;
import androidx.leanback.widget.picker.DatePicker;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class GuidedDatePickerTest {

    static final long TRANSITION_LENGTH = 1000;
    static long VERTICAL_SCROLL_WAIT = 500;
    static long HORIZONTAL_SCROLL_WAIT = 500;
    static final long FINAL_WAIT = 1000;

    static final String TAG = "GuidedDatePickerTest";

    private static final int DAY_INDEX = 0;
    private static final int MONTH_INDEX = 1;
    private static final int YEAR_INDEX = 2;

    @Rule
    public ActivityTestRule<GuidedStepAttributesTestActivity> activityTestRule =
            new ActivityTestRule<>(GuidedStepAttributesTestActivity.class, false, false);

    GuidedStepAttributesTestActivity mActivity;


    private void initActivity(Intent intent) {
        mActivity = activityTestRule.launchActivity(intent);
        try {
            Thread.sleep(2000);
        } catch(InterruptedException e) {
            e.printStackTrace();
        }

    }

    Context mContext;
    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();;
    }

    public static void sendKey(int keyCode) {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(keyCode);
    }

    private int getColumnIndexForDateField(int field, int[] columnIndices) {
        int mColDayIndex = columnIndices[0];
        int mColMonthIndex = columnIndices[1];
        int mColYearIndex = columnIndices[2];
        int columnIndex = -1;
        switch (field) {
            case Calendar.DAY_OF_MONTH:
                columnIndex = mColDayIndex;
                break;
            case Calendar.MONTH:
                columnIndex = mColMonthIndex;
                break;
            case Calendar.YEAR:
                columnIndex = mColYearIndex;
        }
        return columnIndex;
    }

    private void horizontalScrollToDateField(int field, int[] columnIndices,
                                             DatePicker pickerView) throws Throwable{
        int columnIndex = getColumnIndexForDateField(field, columnIndices);

        LinearLayout columnsLayout = (LinearLayout) pickerView.getChildAt(0);

        int focusedFieldPos = columnsLayout.indexOfChild(columnsLayout.getFocusedChild());
        if (focusedFieldPos == -1) {
            sendKey(KeyEvent.KEYCODE_DPAD_CENTER);
            Thread.sleep(TRANSITION_LENGTH);
        }
        focusedFieldPos = columnsLayout.indexOfChild(columnsLayout.getFocusedChild());
        assertTrue("Date field could not be focused!", (focusedFieldPos != -1));

        // following is to skip the separator fields "/" which are unfocusable but counted as
        // children of columnsLayout
        switch (focusedFieldPos) {
            case 0:
                focusedFieldPos = 0;
                break;
            case 2:
                focusedFieldPos = 1;
                break;
            case 4:
                focusedFieldPos = 2;
        }

        // now scroll right or left to the corresponding date field as indicated by the input field
        int horizontalScrollOffset = columnIndex - focusedFieldPos;

        int horizontalScrollDir = KeyEvent.KEYCODE_DPAD_RIGHT;
        if (horizontalScrollOffset < 0) {
            horizontalScrollOffset = -horizontalScrollOffset;
            horizontalScrollDir = KeyEvent.KEYCODE_DPAD_LEFT;
        }
        for(int i = 0; i < horizontalScrollOffset; i++) {
            sendKey(horizontalScrollDir);
            Thread.sleep(HORIZONTAL_SCROLL_WAIT);
        }

    }

    /**
     * Scrolls vertically all the way up or down (depending on the provided scrollDir parameter)
     * to fieldValue if it's not equal to -1; otherwise, the scrolling goes all the way to the end.
     * @param field The date field over which the scrolling is performed
     * @param fieldValue The field value to scroll to or -1 if the scrolling should go all the way.
     * @param columnIndices The date field indices corresponding to day, month, and the year
     * @param pickerView The DatePicker view.
     * @param scrollDir The direction of scrolling to reach the desired field value.
     * @throws Throwable
     */
    private void verticalScrollToFieldValue(int field, int fieldValue, int[] columnIndices,
                                                 DatePicker pickerView, int scrollDir)
            throws Throwable {

        int columnIndex = getColumnIndexForDateField(field, columnIndices);
        int colDayIndex = columnIndices[0];
        int colMonthIndex = columnIndices[1];
        int colYearIndex = columnIndices[2];

        horizontalScrollToDateField(field, columnIndices, pickerView);

        Calendar currentActionCal = Calendar.getInstance();
        currentActionCal.setTimeInMillis(pickerView.getDate());

        Calendar minCal = Calendar.getInstance();
        minCal.setTimeInMillis(pickerView.getMinDate());

        Calendar maxCal = Calendar.getInstance();
        maxCal.setTimeInMillis(pickerView.getMaxDate());


        int prevColumnVal = -1;
        int currentColumnVal = pickerView.getColumnAt(columnIndex).getCurrentValue();
        while( currentColumnVal != prevColumnVal && currentColumnVal != fieldValue){
            assertTrue(mContext.getString(R.string.datepicker_test_wrong_day_value),
                    pickerView.getColumnAt(colDayIndex).getCurrentValue()
                            == currentActionCal.get(Calendar.DAY_OF_MONTH)
            );
            assertTrue(mContext.getString(R.string.datepicker_test_wrong_month_value),
                    pickerView.getColumnAt(colMonthIndex).getCurrentValue()
                            == currentActionCal.get(Calendar.MONTH)
            );
            assertTrue(mContext.getString(R.string.datepicker_test_wrong_year_value),
                    pickerView.getColumnAt(colYearIndex).getCurrentValue()
                            == currentActionCal.get(Calendar.YEAR)
            );

            int offset = scrollDir == KeyEvent.KEYCODE_DPAD_DOWN ? 1 : -1;
            addDate(currentActionCal, field, offset, minCal, maxCal);

            sendKey(scrollDir);
            Thread.sleep(VERTICAL_SCROLL_WAIT);

            prevColumnVal = currentColumnVal;
            currentColumnVal = pickerView.getColumnAt(columnIndex).getCurrentValue();
        }
    }

    private void addDate(Calendar mCurrentDate, int field, int offset,
                         Calendar mMinDate, Calendar mMaxDate) {
        int maxOffset = -1;
        int actualMinFieldValue, actualMaxFieldValue;

        if ( field == Calendar.YEAR ) {
            actualMinFieldValue = mMinDate.get(Calendar.YEAR);
            actualMaxFieldValue = mMaxDate.get(Calendar.YEAR);
        } else {
            actualMinFieldValue = mCurrentDate.getActualMinimum(field);
            actualMaxFieldValue = mCurrentDate.getActualMaximum(field);
        }

        if ( offset > 0 ) {
            maxOffset = Math.min(
                    actualMaxFieldValue - mCurrentDate.get(field), offset);
            mCurrentDate.add(field, maxOffset);
            if (mCurrentDate.after(mMaxDate)) {
                mCurrentDate.setTimeInMillis(mMaxDate.getTimeInMillis());
            }
        } else {
            maxOffset = Math.max(
                    actualMinFieldValue - mCurrentDate.get(field), offset);
            mCurrentDate.add(field, Math.max(offset, maxOffset));
            if (mCurrentDate.before(mMinDate)) {
                mCurrentDate.setTimeInMillis(mMinDate.getTimeInMillis());
            }
        }
    }

    @Test
    public void testJanuaryToFebruaryTransitionForLeapYear() throws Throwable {
        long startTime = System.currentTimeMillis();
        Intent intent = new Intent();

        String title = "Date Picker Transition Test";
        String breadcrumb = "Month Transition Test Demo";
        String description = "Testing the transition from Jan to Feb (leap year)";
        GuidanceStylist.Guidance guidance = new GuidanceStylist.Guidance(title, description,
                breadcrumb, null);

        List<GuidedAction> actionList = new ArrayList<>();

        Calendar cal = Calendar.getInstance();

        cal.set(Calendar.YEAR, 2016);   // 2016 is a leap year
        cal.set(Calendar.MONTH, Calendar.JANUARY);
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        Date initialDate = cal.getTime();

        GuidedDatePickerAction action = new GuidedDatePickerAction.Builder(
                mContext)
                .id(0)
                .title("Date")
                .date(initialDate.getTime())
                .datePickerFormat("DMY")
                .build();

        actionList.add(action);

        GuidedStepAttributesTestFragment.clear();
        GuidedStepAttributesTestFragment.GUIDANCE = guidance;
        GuidedStepAttributesTestFragment.ACTION_LIST = actionList;

        initActivity(intent);

        DatePicker mPickerView = (DatePicker) mActivity.findViewById(
                R.id.guidedactions_activator_item);

        verticalScrollToFieldValue(Calendar.MONTH, Calendar.FEBRUARY, new int[] {0, 1, 2},
                mPickerView, KeyEvent.KEYCODE_DPAD_DOWN);
        long executionTime = System.currentTimeMillis() - startTime;
        Log.d(TAG, "testJanuaryToFebruaryTransitionForLeapYear() Execution time: " + executionTime);
        Thread.sleep(FINAL_WAIT);
    }

    @Test
    public void testFebruaryToMarchTransitionForLeapYear() throws Throwable {
        long startTime = System.currentTimeMillis();
        Intent intent = new Intent();

        String title = "Date Picker Transition Test";
        String breadcrumb = "Month Transition Test Demo";
        String description = "Testing the transition from Feb to Mar (leap year)";
        GuidanceStylist.Guidance guidance = new GuidanceStylist.Guidance(title, description,
                breadcrumb, null);

        List<GuidedAction> actionList = new ArrayList<>();

        Calendar cal = Calendar.getInstance();

        cal.set(Calendar.YEAR, 2016);
        cal.set(Calendar.MONTH, Calendar.FEBRUARY);
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        Date initialDate = cal.getTime();

        GuidedDatePickerAction action = new GuidedDatePickerAction.Builder(
                mContext)
                .id(0)
                .title("Date")
                .date(initialDate.getTime())
                .datePickerFormat("DMY")
                .build();

        actionList.add(action);

        GuidedStepAttributesTestFragment.clear();
        GuidedStepAttributesTestFragment.GUIDANCE = guidance;
        GuidedStepAttributesTestFragment.ACTION_LIST = actionList;

        initActivity(intent);

        DatePicker mPickerView = (DatePicker) mActivity.findViewById(
                R.id.guidedactions_activator_item);

        verticalScrollToFieldValue(Calendar.MONTH, Calendar.MARCH, new int[] {0, 1, 2},
                mPickerView, KeyEvent.KEYCODE_DPAD_DOWN);
        long executionTime = System.currentTimeMillis() - startTime;
        Log.d(TAG, "testFebruaryToMarchTransition() Execution time: " + executionTime);
        Thread.sleep(FINAL_WAIT);
    }

    @Test
    public void testJanuaryToFebruaryTransitionForNonLeapYear() throws Throwable {
        long startTime = System.currentTimeMillis();
        Intent intent = new Intent();

        String title = "Date Picker Transition Test";
        String breadcrumb = "Month Transition Test Demo";
        String description = "Testing the transition from Jan to Feb (nonleap year)";
        GuidanceStylist.Guidance guidance = new GuidanceStylist.Guidance(title, description,
                breadcrumb, null);

        List<GuidedAction> actionList = new ArrayList<>();

        Calendar cal = Calendar.getInstance();

        cal.set(Calendar.YEAR, 2017);   // 2017 is a leap year
        cal.set(Calendar.MONTH, Calendar.JANUARY);
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        Date initialDate = cal.getTime();

        GuidedDatePickerAction action = new GuidedDatePickerAction.Builder(
                mContext)
                .id(0)
                .title("Date")
                .date(initialDate.getTime())
                .datePickerFormat("DMY")
                .build();

        actionList.add(action);

        GuidedStepAttributesTestFragment.clear();
        GuidedStepAttributesTestFragment.GUIDANCE = guidance;
        GuidedStepAttributesTestFragment.ACTION_LIST = actionList;

        initActivity(intent);

        DatePicker mPickerView = (DatePicker) mActivity.findViewById(
                R.id.guidedactions_activator_item);

        verticalScrollToFieldValue(Calendar.MONTH, Calendar.FEBRUARY, new int[] {0, 1, 2},
                mPickerView, KeyEvent.KEYCODE_DPAD_DOWN);
        long executionTime = System.currentTimeMillis() - startTime;
        Log.d(TAG, "testJanuaryToFebruaryTransition() Execution time: " + executionTime);
        Thread.sleep(FINAL_WAIT);
    }

    @Test
    public void testFebruaryToMarchTransitionForNonLeapYear() throws Throwable {
        long startTime = System.currentTimeMillis();
        Intent intent = new Intent();

        String title = "Date Picker Transition Test";
        String breadcrumb = "Month Transition Test Demo";
        String description = "Testing the transition from Feb to Mar (nonleap year)";
        GuidanceStylist.Guidance guidance = new GuidanceStylist.Guidance(title, description,
                breadcrumb, null);

        List<GuidedAction> actionList = new ArrayList<>();

        Calendar cal = Calendar.getInstance();

        cal.set(Calendar.YEAR, 2017);
        cal.set(Calendar.MONTH, Calendar.FEBRUARY);
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        Date initialDate = cal.getTime();

        GuidedDatePickerAction action = new GuidedDatePickerAction.Builder(
                mContext)
                .id(0)
                .title("Date")
                .date(initialDate.getTime())
                .datePickerFormat("DMY")
                .build();

        actionList.add(action);

        GuidedStepAttributesTestFragment.clear();
        GuidedStepAttributesTestFragment.GUIDANCE = guidance;
        GuidedStepAttributesTestFragment.ACTION_LIST = actionList;

        initActivity(intent);

        DatePicker mPickerView = (DatePicker) mActivity.findViewById(
                R.id.guidedactions_activator_item);

        verticalScrollToFieldValue(Calendar.MONTH, Calendar.MARCH, new int[] {0, 1, 2},
                mPickerView, KeyEvent.KEYCODE_DPAD_DOWN);
        long executionTime = System.currentTimeMillis() - startTime;
        Log.d(TAG, "testFebruaryToMarchTransition() Execution time: " + executionTime);
        Thread.sleep(FINAL_WAIT);
    }

    @Test
    public void testDecemberToNovemberTransition() throws Throwable {
        long startTime = System.currentTimeMillis();
        Intent intent = new Intent();

        String title = "Date Picker Transition Test";
        String breadcrumb = "Month Transition Test Demo";
        String description = "Testing the transition from Dec to Nov";
        GuidanceStylist.Guidance guidance = new GuidanceStylist.Guidance(title, description,
                breadcrumb, null);

        List<GuidedAction> actionList = new ArrayList<>();

        Calendar cal = Calendar.getInstance();

        cal.set(Calendar.YEAR, 2016);
        cal.set(Calendar.MONTH, Calendar.DECEMBER);
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        Date initialDate = cal.getTime();

        GuidedDatePickerAction action = new GuidedDatePickerAction.Builder(
                mContext)
                .id(0)
                .title("Date")
                .date(initialDate.getTime())
                .datePickerFormat("DMY")
                .build();

        actionList.add(action);

        GuidedStepAttributesTestFragment.clear();
        GuidedStepAttributesTestFragment.GUIDANCE = guidance;
        GuidedStepAttributesTestFragment.ACTION_LIST = actionList;

        initActivity(intent);

        DatePicker mPickerView = (DatePicker) mActivity.findViewById(
                R.id.guidedactions_activator_item);

        verticalScrollToFieldValue(Calendar.MONTH, Calendar.NOVEMBER, new int[] {0, 1, 2},
                mPickerView, KeyEvent.KEYCODE_DPAD_UP);
        long executionTime = System.currentTimeMillis() - startTime;
        Log.d(TAG, "testDecemberToNovember() Execution time: " + executionTime);
        Thread.sleep(FINAL_WAIT);
    }

    @Test
    public void testNovemberToOctoberTransition() throws Throwable {
        long startTime = System.currentTimeMillis();
        Intent intent = new Intent();

        String title = "Date Picker Transition Test";
        String breadcrumb = "Month Transition Test Demo";
        String description = "Testing the transition from Nov to Oct";
        GuidanceStylist.Guidance guidance = new GuidanceStylist.Guidance(title, description,
                breadcrumb, null);

        List<GuidedAction> actionList = new ArrayList<>();

        Calendar cal = Calendar.getInstance();

        cal.set(Calendar.YEAR, 2016);
        cal.set(Calendar.MONTH, Calendar.NOVEMBER);
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        Date initialDate = cal.getTime();

        GuidedDatePickerAction action = new GuidedDatePickerAction.Builder(
                mContext)
                .id(0)
                .title("Date")
                .date(initialDate.getTime())
                .datePickerFormat("DMY")
                .build();

        actionList.add(action);

        GuidedStepAttributesTestFragment.clear();
        GuidedStepAttributesTestFragment.GUIDANCE = guidance;
        GuidedStepAttributesTestFragment.ACTION_LIST = actionList;

        initActivity(intent);

        DatePicker mPickerView = (DatePicker) mActivity.findViewById(
                R.id.guidedactions_activator_item);

        verticalScrollToFieldValue(Calendar.MONTH, Calendar.OCTOBER, new int[] {0, 1, 2},
                mPickerView, KeyEvent.KEYCODE_DPAD_UP);
        long executionTime = System.currentTimeMillis() - startTime;
        Log.d(TAG, "testNovemberToOctober() Execution time: " + executionTime);
        Thread.sleep(FINAL_WAIT);
    }

    @Test
    public void testLeapToNonLeapYearTransition() throws Throwable {
        long startTime = System.currentTimeMillis();
        Intent intent = new Intent();

        String title = "Date Picker Transition Test";
        String breadcrumb = "Leap Year Transition Test Demo";
        String description = "Testing Feb transition from leap to nonlneap year";
        GuidanceStylist.Guidance guidance = new GuidanceStylist.Guidance(title, description,
                breadcrumb, null);

        List<GuidedAction> actionList = new ArrayList<>();

        Calendar cal = Calendar.getInstance();

        cal.set(Calendar.YEAR, 2016);   // 2016 is a leap year
        cal.set(Calendar.MONTH, Calendar.FEBRUARY);
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        Date initialDate = cal.getTime();

        GuidedDatePickerAction action = new GuidedDatePickerAction.Builder(
                mContext)
                .id(0)
                .title("Date")
                .date(initialDate.getTime())
                .datePickerFormat("DMY")
                .build();

        actionList.add(action);

        GuidedStepAttributesTestFragment.clear();
        GuidedStepAttributesTestFragment.GUIDANCE = guidance;
        GuidedStepAttributesTestFragment.ACTION_LIST = actionList;

        initActivity(intent);

        DatePicker mPickerView = (DatePicker) mActivity.findViewById(
                R.id.guidedactions_activator_item);

        verticalScrollToFieldValue(Calendar.YEAR, 2017, new int[] {0, 1, 2},
                mPickerView, KeyEvent.KEYCODE_DPAD_DOWN);
        long executionTime = System.currentTimeMillis() - startTime;
        Log.d(TAG, "testLeapToNonLeapYearTransition() Execution time: " + executionTime);
        Thread.sleep(FINAL_WAIT);
    }

    @Test
    public void testNonLeapToLeapYearTransition() throws Throwable {
        long startTime = System.currentTimeMillis();
        Intent intent = new Intent();

        String title = "Date Picker Transition Test";
        String breadcrumb = "Leap Year Transition Test Demo";
        String description = "Testing Feb transition from nonleap to leap year";
        GuidanceStylist.Guidance guidance = new GuidanceStylist.Guidance(title, description,
                breadcrumb, null);

        List<GuidedAction> actionList = new ArrayList<>();

        Calendar cal = Calendar.getInstance();

        cal.set(Calendar.YEAR, 2017);   // 2017 is a non-leap year
        cal.set(Calendar.MONTH, Calendar.FEBRUARY);
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        Date initialDate = cal.getTime();

        GuidedDatePickerAction action = new GuidedDatePickerAction.Builder(
                mContext)
                .id(0)
                .title("Date")
                .date(initialDate.getTime())
                .datePickerFormat("DMY")
                .build();

        actionList.add(action);

        GuidedStepAttributesTestFragment.clear();
        GuidedStepAttributesTestFragment.GUIDANCE = guidance;
        GuidedStepAttributesTestFragment.ACTION_LIST = actionList;

        initActivity(intent);

        DatePicker mPickerView = (DatePicker) mActivity.findViewById(
                R.id.guidedactions_activator_item);

        verticalScrollToFieldValue(Calendar.YEAR, 2016, new int[] {0, 1, 2},
                mPickerView, KeyEvent.KEYCODE_DPAD_UP);
        long executionTime = System.currentTimeMillis() - startTime;
        Log.d(TAG, "testNonLeapToLeapYearTransition() Execution time: " + executionTime);
        Thread.sleep(FINAL_WAIT);
    }

    private void traverseMonths(DatePicker mPickerView, GuidedDatePickerAction dateAction)
            throws Throwable {

        final GuidedStepFragment mFragment = (GuidedStepFragment)
                mActivity.getGuidedStepTestFragment();

        Calendar currentActionCal = Calendar.getInstance();
        currentActionCal.setTimeInMillis(dateAction.getDate());

        sendKey(KeyEvent.KEYCODE_DPAD_CENTER);
        Thread.sleep(TRANSITION_LENGTH);

        int prevMonth = -1;
        int currentMonth = mPickerView.getColumnAt(MONTH_INDEX).getCurrentValue();
        while (currentMonth != prevMonth) {
            int prevDayOfMonth = -1;
            int currentDayOfMonth = mPickerView.getColumnAt(DAY_INDEX).getCurrentValue();
            // scroll down the days till reaching the last day of month
            while (currentDayOfMonth != prevDayOfMonth) {
                sendKey(KeyEvent.KEYCODE_DPAD_DOWN);
                Thread.sleep(VERTICAL_SCROLL_WAIT);
                prevDayOfMonth = currentDayOfMonth;
                currentDayOfMonth = mPickerView.getColumnAt(DAY_INDEX).getCurrentValue();
            }
            int oldDayValue = mPickerView.getColumnAt(DAY_INDEX).getCurrentValue();
            int oldMonthValue = mPickerView.getColumnAt(MONTH_INDEX).getCurrentValue();
            // increment the month
            sendKey(KeyEvent.KEYCODE_DPAD_RIGHT);
            Thread.sleep(VERTICAL_SCROLL_WAIT);

            sendKey(KeyEvent.KEYCODE_DPAD_DOWN);
            Thread.sleep(TRANSITION_LENGTH);

            int newDayValue = mPickerView.getColumnAt(DAY_INDEX).getCurrentValue();
            int newMonthValue = mPickerView.getColumnAt(MONTH_INDEX).getCurrentValue();
            verifyMonthTransition(currentActionCal,
                    oldDayValue, oldMonthValue, newDayValue, newMonthValue);

            sendKey(KeyEvent.KEYCODE_DPAD_LEFT);
            Thread.sleep(TRANSITION_LENGTH);
            prevMonth = currentMonth;
            currentMonth = newMonthValue;
        }

    }


    private void verifyMonthTransition(Calendar currentCal, int oldDayValue, int oldMonthValue,
                                       int newDayValue, int newMonthValue) {

        if (oldMonthValue == newMonthValue)
            return;

        currentCal.set(Calendar.DAY_OF_MONTH, 1);
        currentCal.set(Calendar.MONTH, oldMonthValue);
        int expectedOldDayValue = currentCal.getActualMaximum(Calendar.DAY_OF_MONTH);
        currentCal.set(Calendar.MONTH, newMonthValue);
        int numDaysInNewMonth = currentCal.getActualMaximum(Calendar.DAY_OF_MONTH);
        int expectedNewDayValue = (expectedOldDayValue <= numDaysInNewMonth)
                ? expectedOldDayValue : numDaysInNewMonth;

        assertTrue(mContext.getString(
                R.string.datepicker_test_transition_error1, oldMonthValue),
                oldDayValue == expectedOldDayValue
        );
        assertTrue(mContext.getString(
                R.string.datepicker_test_transition_error2, newDayValue, newMonthValue),
                newDayValue == expectedNewDayValue
        );
    }

    @Test
    public void testDateRangesMDYFormat() throws Throwable {

        long startTime = System.currentTimeMillis();

        GuidedDatePickerAction[] datePickerActions = setupDateActionsForMinAndMaxRangeTests();

        scrollToMinAndMaxDates(new int[] {1, 0, 2}, datePickerActions[0]);
        long executionTime = System.currentTimeMillis() - startTime;
        Log.d(TAG, "testDateRangesMDYFormat() Execution time: " + executionTime);
        Thread.sleep(FINAL_WAIT);
    }

    public void testDateRangesDMYFormat() throws Throwable {

        long startTime = System.currentTimeMillis();

        GuidedDatePickerAction[] datePickerActions = setupDateActionsForMinAndMaxRangeTests();
        Log.d(TAG, "setup dateactions complete!");
        scrollToMinAndMaxDates(new int[] {0, 1, 2}, datePickerActions[1]);
        long executionTime = System.currentTimeMillis() - startTime;
        Log.d(TAG, "testDateRangesDMYFormat() Execution time: " + executionTime);
        Thread.sleep(FINAL_WAIT);
    }

    @Test
    public void testDateRangesWithYearEqual() throws Throwable {

        long startTime = System.currentTimeMillis();

        GuidedDatePickerAction[] datePickerActions = setupDateActionsForMinAndMaxRangeTests();

        scrollToMinAndMaxDates(new int[] {0, 1, 2}, datePickerActions[2]);
        long executionTime = System.currentTimeMillis() - startTime;
        Log.d(TAG, "testDateRangesWithYearEqual() Execution time: " + executionTime);
        Thread.sleep(FINAL_WAIT);
    }

    @Test
    public void testDateRangesWithMonthAndYearEqual() throws Throwable {

        long startTime = System.currentTimeMillis();

        GuidedDatePickerAction[] datePickerActions = setupDateActionsForMinAndMaxRangeTests();

        scrollToMinAndMaxDates(new int[] {0, 1, 2}, datePickerActions[3]);
        long executionTime = System.currentTimeMillis() - startTime;
        Log.d(TAG, "testDateRangesWithMonthAndYearEqual() Execution time: " + executionTime);
        Thread.sleep(FINAL_WAIT);
    }

    @Test
    public void testDateRangesWithAllFieldsEqual() throws Throwable {

        long startTime = System.currentTimeMillis();

        GuidedDatePickerAction[] datePickerActions = setupDateActionsForMinAndMaxRangeTests();

        scrollToMinAndMaxDates(new int[] {0, 1, 2}, datePickerActions[4]);
        long executionTime = System.currentTimeMillis() - startTime;
        Log.d(TAG, "testDateRangesWithAllFieldsEqual() Execution time: " + executionTime);
        Thread.sleep(FINAL_WAIT);
    }

    private GuidedDatePickerAction[] setupDateActionsForMinAndMaxRangeTests() {
        Intent intent = new Intent();
        Resources res = mContext.getResources();

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

        Calendar currCal = Calendar.getInstance();
        currCal.set(Calendar.YEAR, 2016);
        currCal.set(Calendar.MONTH, Calendar.JULY);
        currCal.set(Calendar.DAY_OF_MONTH, 15);

        Calendar minCal = Calendar.getInstance();
        minCal.set(Calendar.YEAR, 2014);
        minCal.set(Calendar.MONTH, Calendar.OCTOBER);
        minCal.set(Calendar.DAY_OF_MONTH, 20);

        Calendar maxCal = Calendar.getInstance();
        maxCal.set(Calendar.YEAR, 2018);
        maxCal.set(Calendar.MONTH, Calendar.FEBRUARY);
        maxCal.set(Calendar.DAY_OF_MONTH, 10);

        String title = "Date Picker Range Test";
        String breadcrumb = "Date Picker Range Test Demo";
        String description = "";
        GuidanceStylist.Guidance guidance = new GuidanceStylist.Guidance(title, description,
                breadcrumb, null);

        List<GuidedAction> actionList = new ArrayList<>();

        // testing different date formats and the correctness of range changes as we scroll
        GuidedDatePickerAction dateAction1 = new GuidedDatePickerAction.Builder(
                mContext)
                .id(0)
                .title(res.getString(R.string.datepicker_with_range_title,
                        dateFormat.format(minCal.getTime()),
                        dateFormat.format(maxCal.getTime())))
                .multilineDescription(true)
                .date(currCal.getTimeInMillis())
                .datePickerFormat("MDY")
                .minDate(minCal.getTimeInMillis())
                .maxDate(maxCal.getTimeInMillis())
                .build();

        GuidedDatePickerAction dateAction2 = new GuidedDatePickerAction.Builder(
                mContext)
                .id(1)
                .title(res.getString(R.string.datepicker_with_range_title,
                        dateFormat.format(minCal.getTimeInMillis()),
                        dateFormat.format(maxCal.getTimeInMillis())))
                .multilineDescription(true)
                .date(currCal.getTimeInMillis())
                .datePickerFormat("DMY")
                .minDate(minCal.getTimeInMillis())
                .maxDate(maxCal.getTimeInMillis())
                .build();

        // testing date ranges when Year is equal
        minCal.set(Calendar.YEAR, maxCal.get(Calendar.YEAR));
        int minMonth = Math.min(minCal.get(Calendar.MONTH), maxCal.get(Calendar.MONTH));
        int maxMonth = Math.max(minCal.get(Calendar.MONTH), maxCal.get(Calendar.MONTH));
        minCal.set(Calendar.MONTH, minMonth);
        maxCal.set(Calendar.MONTH, maxMonth);

        GuidedDatePickerAction dateAction3 = new GuidedDatePickerAction.Builder(
                mContext)
                .id(2)
                .title(res.getString(R.string.datepicker_with_range_title,
                        dateFormat.format(minCal.getTimeInMillis()),
                        dateFormat.format(maxCal.getTimeInMillis())))
                .multilineDescription(true)
                .date(currCal.getTimeInMillis())
                .datePickerFormat("DMY")
                .minDate(minCal.getTimeInMillis())
                .maxDate(maxCal.getTimeInMillis())
                .build();


        // testing date ranges when both Month and Year are equal
        minCal.set(Calendar.MONTH, maxCal.get(Calendar.MONTH));
        int minDay = Math.min(minCal.get(Calendar.DAY_OF_MONTH), maxCal.get(Calendar.DAY_OF_MONTH));
        int maxDay = Math.max(minCal.get(Calendar.DAY_OF_MONTH), maxCal.get(Calendar.DAY_OF_MONTH));
        minCal.set(Calendar.DAY_OF_MONTH, minDay);
        maxCal.set(Calendar.DAY_OF_MONTH, maxDay);

        GuidedDatePickerAction dateAction4 = new GuidedDatePickerAction.Builder(
                mContext)
                .id(3)
                .title(res.getString(R.string.datepicker_with_range_title,
                        dateFormat.format(minCal.getTimeInMillis()),
                        dateFormat.format(maxCal.getTimeInMillis())))
                .multilineDescription(true)
                .date(currCal.getTimeInMillis())
                .datePickerFormat("DMY")
                .minDate(minCal.getTimeInMillis())
                .maxDate(maxCal.getTimeInMillis())
                .build();


        // testing date ranges when all fields are equal
        minCal.set(Calendar.DAY_OF_MONTH, maxCal.get(Calendar.DAY_OF_MONTH));

        GuidedDatePickerAction dateAction5 = new GuidedDatePickerAction.Builder(
                mContext)
                .id(4)
                .title(res.getString(R.string.datepicker_with_range_title,
                        dateFormat.format(minCal.getTimeInMillis()),
                        dateFormat.format(maxCal.getTimeInMillis())))
                .multilineDescription(true)
                .date(currCal.getTimeInMillis())
                .datePickerFormat("DMY")
                .minDate(minCal.getTimeInMillis())
                .maxDate(maxCal.getTimeInMillis())
                .build();

        actionList.add(dateAction1);
        actionList.add(dateAction2);
        actionList.add(dateAction3);
        actionList.add(dateAction4);
        actionList.add(dateAction5);

        GuidedStepAttributesTestFragment.clear();
        GuidedStepAttributesTestFragment.GUIDANCE = guidance;
        GuidedStepAttributesTestFragment.ACTION_LIST = actionList;

        initActivity(intent);
        return new GuidedDatePickerAction[] {dateAction1, dateAction2, dateAction3, dateAction4,
                dateAction5};
    }

    private void scrollToMinAndMaxDates(int[] columnIndices, GuidedDatePickerAction dateAction)
            throws Throwable{

        final GuidedStepFragment mFragment = (GuidedStepFragment)
                mActivity.getGuidedStepTestFragment();

        VerticalGridView guidedActionsList = (VerticalGridView)
                mActivity.findViewById(R.id.guidedactions_list);

        int currSelectedAction = mFragment.getSelectedActionPosition();
        // scroll up/down to the requested action
        long verticalScrollOffset = dateAction.getId() - currSelectedAction;

        int verticalScrollDir = KeyEvent.KEYCODE_DPAD_DOWN;
        if (verticalScrollOffset < 0) {
            verticalScrollOffset= -verticalScrollOffset;
            verticalScrollDir = KeyEvent.KEYCODE_DPAD_UP;
        }
        for(int i = 0; i < verticalScrollOffset; i++) {
            sendKey(verticalScrollDir);
            Thread.sleep(TRANSITION_LENGTH);
        }

        assertTrue("The wrong action was selected!", mFragment.getSelectedActionPosition()
                == dateAction.getId());
        DatePicker mPickerView = (DatePicker) mFragment.getActionItemView((int) dateAction.getId())
                .findViewById(R.id.guidedactions_activator_item);

        Calendar currentActionCal = Calendar.getInstance();
        currentActionCal.setTimeInMillis(dateAction.getDate());


        // scrolling to the minimum date

        verticalScrollToFieldValue(Calendar.YEAR, -1, columnIndices, mPickerView,
                KeyEvent.KEYCODE_DPAD_UP);
        dateAction.setDate(mPickerView.getDate());

        verticalScrollToFieldValue(Calendar.MONTH, -1, columnIndices, mPickerView,
                KeyEvent.KEYCODE_DPAD_UP);
        dateAction.setDate(mPickerView.getDate());

        verticalScrollToFieldValue(Calendar.DAY_OF_MONTH, -1, columnIndices, mPickerView,
                KeyEvent.KEYCODE_DPAD_UP);
        dateAction.setDate(mPickerView.getDate());

        Thread.sleep(VERTICAL_SCROLL_WAIT);

        // now scrolling to the maximum date

        verticalScrollToFieldValue(Calendar.YEAR, -1, columnIndices, mPickerView,
                KeyEvent.KEYCODE_DPAD_DOWN);
        dateAction.setDate(mPickerView.getDate());

        verticalScrollToFieldValue(Calendar.MONTH, -1, columnIndices, mPickerView,
                KeyEvent.KEYCODE_DPAD_DOWN);
        dateAction.setDate(mPickerView.getDate());

        verticalScrollToFieldValue(Calendar.DAY_OF_MONTH, -1, columnIndices, mPickerView,
                KeyEvent.KEYCODE_DPAD_DOWN);
        dateAction.setDate(mPickerView.getDate());

        sendKey(KeyEvent.KEYCODE_DPAD_CENTER);
        Thread.sleep(TRANSITION_LENGTH);
    }

}
