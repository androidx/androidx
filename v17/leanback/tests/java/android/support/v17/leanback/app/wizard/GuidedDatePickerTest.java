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

package android.support.v17.leanback.app.wizard;

import android.app.Instrumentation;
import android.content.Intent;
import android.content.res.Resources;
import android.support.v17.leanback.app.GuidedStepFragment;
import android.support.v17.leanback.test.R;
import android.support.v17.leanback.widget.GuidanceStylist;
import android.support.v17.leanback.widget.GuidedAction;
import android.support.v17.leanback.widget.GuidedDatePickerAction;
import android.support.v17.leanback.widget.VerticalGridView;
import android.support.v17.leanback.widget.picker.DatePicker;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.MediumTest;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@MediumTest
public class GuidedDatePickerTest extends
        ActivityInstrumentationTestCase2<GuidedStepAttributesTestActivity> {

    static final long TRANSITION_LENGTH = 1000;
    static long VERTICAL_SCROLL_WAIT = 500;
    static long HORIZONTAL_SCROLL_WAIT = 500;
    static final long FINAL_WAIT = 3000;

    static final String TAG = "GuidedDatePickerTest";

    private static final int DAY_INDEX = 0;
    private static final int MONTH_INDEX = 1;
    private static final int YEAR_INDEX = 2;
    Instrumentation mInstrumentation;
    GuidedStepAttributesTestActivity mActivity;

    public GuidedDatePickerTest() {
        super(GuidedStepAttributesTestActivity.class);
    }

    private void initActivity(Intent intent) {

        setActivityIntent(intent);
        mActivity = getActivity();
        try {
            Thread.sleep(2000);
        } catch(InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void scrollOnField(int field, int[] columnIndices, DatePicker mPickerView,
                               int SCROLL_DIR) throws Throwable {

        final GuidedStepFragment mFragment = (GuidedStepFragment)
                mActivity.getGuidedStepTestFragment();

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


        LinearLayout columnsLayout = (LinearLayout) mPickerView.getChildAt(0);

        int focusedFieldPos = columnsLayout.indexOfChild(columnsLayout.getFocusedChild());
        if (focusedFieldPos == -1) {
            sendKeys(KeyEvent.KEYCODE_DPAD_CENTER);
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
            sendKeys(horizontalScrollDir);
            Thread.sleep(HORIZONTAL_SCROLL_WAIT);
        }


        Calendar currentActionCal = Calendar.getInstance();
        currentActionCal.setTimeInMillis(mPickerView.getDate());

        Calendar minCal = Calendar.getInstance();
        minCal.setTimeInMillis(mPickerView.getMinDate());

        Calendar maxCal = Calendar.getInstance();
        maxCal.setTimeInMillis(mPickerView.getMaxDate());


        int prevColumnVal = -1;
        int currentColumnVal = mPickerView.getColumnAt(columnIndex).getCurrentValue();
        while( currentColumnVal != prevColumnVal ){
            assertTrue(getActivity().getString(R.string.datepicker_test_wrong_day_value),
                    mPickerView.getColumnAt(mColDayIndex).getCurrentValue() ==
                            currentActionCal.get(Calendar.DAY_OF_MONTH)
            );
            assertTrue(getActivity().getString(R.string.datepicker_test_wrong_month_value),
                    mPickerView.getColumnAt(mColMonthIndex).getCurrentValue() ==
                            currentActionCal.get(Calendar.MONTH)
            );
            assertTrue(getActivity().getString(R.string.datepicker_test_wrong_year_value),
                    mPickerView.getColumnAt(mColYearIndex).getCurrentValue() ==
                            currentActionCal.get(Calendar.YEAR)
            );

            int offset = SCROLL_DIR == KeyEvent.KEYCODE_DPAD_DOWN ? 1 : -1;
            addDate(currentActionCal, field, offset, minCal, maxCal);

            sendKeys(SCROLL_DIR);
            Thread.sleep(VERTICAL_SCROLL_WAIT);

            prevColumnVal = currentColumnVal;
            currentColumnVal = mPickerView.getColumnAt(columnIndex).getCurrentValue();
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

    public void testDifferentMonthLengths() throws Throwable {

        mInstrumentation = getInstrumentation();
        Intent intent = new Intent(mInstrumentation.getContext(),
                GuidedStepAttributesTestActivity.class);
        Resources res = mInstrumentation.getContext().getResources();

        final int NUM_DATE_ACTIONS = 1;

        String title = "Date Picker Transition Test";
        String breadcrumb = "Month Transition Test Demo";
        String description = "Testing the transition between longer to shorter months";
        GuidanceStylist.Guidance guidance = new GuidanceStylist.Guidance(title, description,
                breadcrumb, null);

        List<GuidedAction> actionList = new ArrayList<>();

        Calendar cal = Calendar.getInstance();

        cal.set(Calendar.YEAR, 2016);
        cal.set(Calendar.MONTH, Calendar.JANUARY);
        cal.set(Calendar.DAY_OF_MONTH, 30);
        Date initialDate = cal.getTime();

        GuidedDatePickerAction action = new GuidedDatePickerAction.Builder(
                mInstrumentation.getContext())
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

        final GuidedStepFragment mFragment = (GuidedStepFragment) mActivity.
                getGuidedStepTestFragment();
        traverseMonths(mPickerView, (GuidedDatePickerAction) actionList.get(0));
        Thread.sleep(FINAL_WAIT);
    }

    private void traverseMonths(DatePicker mPickerView, GuidedDatePickerAction dateAction)
            throws Throwable{

        final GuidedStepFragment mFragment = (GuidedStepFragment)
                mActivity.getGuidedStepTestFragment();

        Calendar currentActionCal = Calendar.getInstance();
        currentActionCal.setTimeInMillis(dateAction.getDate());

        sendKeys(KeyEvent.KEYCODE_DPAD_CENTER);
        Thread.sleep(TRANSITION_LENGTH);

        int prevMonth = -1;
        int currentMonth = mPickerView.getColumnAt(MONTH_INDEX).getCurrentValue();
        while (currentMonth != prevMonth) {
            int prevDayOfMonth = -1;
            int currentDayOfMonth = mPickerView.getColumnAt(DAY_INDEX).getCurrentValue();
            // scroll down the days till reaching the last day of month
            while (currentDayOfMonth != prevDayOfMonth) {
                sendKeys(KeyEvent.KEYCODE_DPAD_DOWN);
                Thread.sleep(VERTICAL_SCROLL_WAIT);
                prevDayOfMonth = currentDayOfMonth;
                currentDayOfMonth = mPickerView.getColumnAt(DAY_INDEX).getCurrentValue();
            }
            int oldDayValue = mPickerView.getColumnAt(DAY_INDEX).getCurrentValue();
            int oldMonthValue = mPickerView.getColumnAt(MONTH_INDEX).getCurrentValue();
            // increment the month
            sendKeys(KeyEvent.KEYCODE_DPAD_RIGHT);
            Thread.sleep(VERTICAL_SCROLL_WAIT);

            sendKeys(KeyEvent.KEYCODE_DPAD_DOWN);
            Thread.sleep(TRANSITION_LENGTH);

            int newDayValue = mPickerView.getColumnAt(DAY_INDEX).getCurrentValue();
            int newMonthValue = mPickerView.getColumnAt(MONTH_INDEX).getCurrentValue();
            verifyMonthTransition(currentActionCal,
                    oldDayValue, oldMonthValue, newDayValue, newMonthValue);

            sendKeys(KeyEvent.KEYCODE_DPAD_LEFT);
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
        int expectedNewDayValue = (expectedOldDayValue <= numDaysInNewMonth) ?
                expectedOldDayValue : numDaysInNewMonth;

        assertTrue(getActivity().getString(
                R.string.datepicker_test_transition_error1, oldMonthValue),
                oldDayValue == expectedOldDayValue
        );
        assertTrue(getActivity().getString(
                R.string.datepicker_test_transition_error2, newDayValue, newMonthValue),
                newDayValue == expectedNewDayValue
        );
    }

    public void testDateRanges() throws Throwable {

        mInstrumentation = getInstrumentation();
        Intent intent = new Intent(mInstrumentation.getContext(),
                GuidedStepAttributesTestActivity.class);
        Resources res = mInstrumentation.getContext().getResources();

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
                mInstrumentation.getContext())
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
                mInstrumentation.getContext())
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
                mInstrumentation.getContext())
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
                mInstrumentation.getContext())
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
                mInstrumentation.getContext())
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

        final GuidedStepFragment mFragment = (GuidedStepFragment) mActivity.
                getGuidedStepTestFragment();

        scrollToMinAndMaxDates(new int[] {1, 0, 2}, dateAction1);
        scrollToMinAndMaxDates(new int[] {0, 1, 2}, dateAction2);
        scrollToMinAndMaxDates(new int[] {0, 1, 2}, dateAction3);
        scrollToMinAndMaxDates(new int[] {0, 1, 2}, dateAction4);
        scrollToMinAndMaxDates(new int[] {0, 1, 2}, dateAction5);

        Thread.sleep(FINAL_WAIT);
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
            sendKeys(verticalScrollDir);
            Thread.sleep(TRANSITION_LENGTH);
        }

        assertTrue("The wrong action was selected!", mFragment.getSelectedActionPosition() ==
                dateAction.getId());
        DatePicker mPickerView = (DatePicker) mFragment.getActionItemView((int) dateAction.getId())
                .findViewById(R.id.guidedactions_activator_item);

        Calendar currentActionCal = Calendar.getInstance();
        currentActionCal.setTimeInMillis(dateAction.getDate());


        // scrolling to the minimum date

        scrollOnField(Calendar.YEAR, columnIndices, mPickerView, KeyEvent.KEYCODE_DPAD_UP);
        dateAction.setDate(mPickerView.getDate());

        scrollOnField(Calendar.MONTH, columnIndices, mPickerView, KeyEvent.KEYCODE_DPAD_UP);
        dateAction.setDate(mPickerView.getDate());

        scrollOnField(Calendar.DAY_OF_MONTH, columnIndices, mPickerView, KeyEvent.KEYCODE_DPAD_UP);
        dateAction.setDate(mPickerView.getDate());

        Thread.sleep(VERTICAL_SCROLL_WAIT);

        // now scrolling to the maximum date

        scrollOnField(Calendar.YEAR, columnIndices, mPickerView, KeyEvent.KEYCODE_DPAD_DOWN);
        dateAction.setDate(mPickerView.getDate());

        scrollOnField(Calendar.MONTH, columnIndices, mPickerView, KeyEvent.KEYCODE_DPAD_DOWN);
        dateAction.setDate(mPickerView.getDate());

        scrollOnField(Calendar.DAY_OF_MONTH, columnIndices, mPickerView, KeyEvent.KEYCODE_DPAD_DOWN);
        dateAction.setDate(mPickerView.getDate());

        sendKeys(KeyEvent.KEYCODE_DPAD_CENTER);
        Thread.sleep(TRANSITION_LENGTH);
    }

}
