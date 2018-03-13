/*
 * Copyright (C) 2015 The Android Open Source Project
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
package androidx.appcompat.app;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.PositionAssertions.isBelow;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.LayoutMatchers.hasEllipsizedText;
import static android.support.test.espresso.matcher.RootMatchers.isDialog;
import static android.support.test.espresso.matcher.ViewMatchers.hasSibling;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Message;
import android.support.test.annotation.UiThreadTest;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.ViewInteraction;
import android.support.test.filters.LargeTest;
import android.support.test.filters.MediumTest;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;

import androidx.annotation.ColorInt;
import androidx.annotation.StringRes;
import androidx.appcompat.test.R;
import androidx.appcompat.testutils.TestUtilsMatchers;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

/**
 * Tests in this class make a few assumptions about the underlying implementation of
 * <code>AlertDialog</code>. While the assumptions don't go all the way down to individual
 * <code>R.id</code> references or very specific layout arrangements, internal refactoring
 * of <code>AlertDialog</code> might require corresponding restructuring of the matching
 * tests. Specifically:
 *
 * <ul>
 *     <li>Testing <code>setIcon</code> API assumes that the icon is displayed by a separate
 *     <code>ImageView</code> which is a sibling of a title view.</li>
 *     <li>Testing <code>setMultiChoiceItems</code> API assumes that each item in the list
 *     is rendered by a single <code>CheckedTextView</code>.</li>
 *     <li>Testing <code>setSingleChoiceItems</code> API assumes that each item in the list
 *     is rendered by a single <code>CheckedTextView</code>.</li>
 * </ul>
 */
public class AlertDialogTest {
    @Rule
    public final ActivityTestRule<AlertDialogTestActivity> mActivityTestRule;

    private Button mButton;

    private AlertDialog mAlertDialog;

    public AlertDialogTest() {
        mActivityTestRule = new ActivityTestRule<>(AlertDialogTestActivity.class);
    }

    @Before
    public void setUp() {
        final AlertDialogTestActivity activity = mActivityTestRule.getActivity();
        mButton = (Button) activity.findViewById(R.id.test_button);
    }

    @After
    public void tearDown() throws Throwable {
        if ((mAlertDialog != null) && mAlertDialog.isShowing()) {
            mActivityTestRule.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mAlertDialog.hide();
                }
            });
        }
    }

    private void wireBuilder(final AlertDialog.Builder builder) {
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAlertDialog = builder.show();
            }
        });
    }

    @Test
    @SmallTest
    @UiThreadTest
    public void testBuilderTheme() {
        final Context context = mActivityTestRule.getActivity();
        final AlertDialog dialog = new AlertDialog.Builder(context, R.style.Theme_TextColors)
                .setTitle(R.string.alert_dialog_title)
                .setMessage(R.string.alert_dialog_content)
                .create();

        final TypedValue tv = new TypedValue();
        dialog.getContext().getTheme().resolveAttribute(android.R.attr.textColorPrimary, tv, true);
        assertEquals(0xFF0000FF, tv.data);
    }

    @Test
    @MediumTest
    public void testBasicContent() {
        final Context context = mActivityTestRule.getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(R.string.alert_dialog_title)
                .setMessage(R.string.alert_dialog_content);
        wireBuilder(builder);

        onView(withId(R.id.test_button)).perform(click());

        // Test that we're showing a dialog with vertically stacked title and content
        final String expectedTitle = context.getString(R.string.alert_dialog_title);
        final String expectedMessage = context.getString(R.string.alert_dialog_content);
        onView(withText(expectedTitle)).inRoot(isDialog()).check(matches(isDisplayed()));
        onView(withText(expectedMessage)).inRoot(isDialog()).check(matches(isDisplayed()));
        onView(withText(expectedMessage)).inRoot(isDialog()).check(
                isBelow(withText(expectedTitle)));

        assertNull("No list view", mAlertDialog.getListView());

        assertEquals("Positive button not shown", View.GONE,
                mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE).getVisibility());
        assertEquals("Negative button not shown", View.GONE,
                mAlertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).getVisibility());
        assertEquals("Neutral button not shown", View.GONE,
                mAlertDialog.getButton(AlertDialog.BUTTON_NEUTRAL).getVisibility());
    }

    // Tests for message logic

    @Test
    @MediumTest
    public void testMessageString() {
        final String dialogMessage = "Dialog message";
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivityTestRule.getActivity())
                .setTitle(R.string.alert_dialog_title)
                .setMessage(dialogMessage);
        wireBuilder(builder);

        onView(withId(R.id.test_button)).perform(click());
        onView(withText(dialogMessage)).inRoot(isDialog()).check(matches(isDisplayed()));
    }

    @Test
    @MediumTest
    public void testMessageStringPostCreation() throws Throwable {
        final String dialogInitialMessage = "Initial message";
        final String dialogUpdatedMessage = "Updated message";
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivityTestRule.getActivity())
                .setTitle(R.string.alert_dialog_title)
                .setMessage(dialogInitialMessage);
        wireBuilder(builder);

        // Click the button to show the dialog and check that it shows the initial message
        onView(withId(R.id.test_button)).perform(click());
        onView(withText(dialogInitialMessage)).inRoot(isDialog()).check(matches(isDisplayed()));

        // Update the dialog message
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAlertDialog.setMessage(dialogUpdatedMessage);
            }
        });
        // Check that the old message is not showing
        onView(withText(dialogInitialMessage)).inRoot(isDialog()).check(doesNotExist());
        // and that the new message is showing
        onView(withText(dialogUpdatedMessage)).inRoot(isDialog()).check(matches(isDisplayed()));
    }

    // Tests for custom title logic

    /**
     * Helper method to verify that setting custom title hides the default title and shows
     * the custom title above the dialog message.
     */
    private void verifyCustomTitle() {
        final Context context = mActivityTestRule.getActivity();

        // Test that we're showing a dialog with vertically stacked custom title and content
        final String title = context.getString(R.string.alert_dialog_title);
        final String expectedCustomTitle = context.getString(R.string.alert_dialog_custom_title);
        final String expectedMessage = context.getString(R.string.alert_dialog_content);

        // Check that the default title is not showing
        onView(withText(title)).inRoot(isDialog()).check(doesNotExist());
        // Check that the custom title is fully displayed with no text eliding and is
        // stacked above the message
        onView(withText(expectedCustomTitle)).inRoot(isDialog()).check(
                matches(isCompletelyDisplayed()));
        onView(withText(expectedCustomTitle)).inRoot(isDialog()).check(
                matches(not(hasEllipsizedText())));
        onView(withText(expectedMessage)).inRoot(isDialog()).check(matches(isDisplayed()));
        onView(withText(expectedMessage)).inRoot(isDialog()).check(
                isBelow(withText(expectedCustomTitle)));
    }

    @Test
    @MediumTest
    public void testCustomTitle() {
        final Context context = mActivityTestRule.getActivity();
        final LayoutInflater inflater = LayoutInflater.from(context);
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(R.string.alert_dialog_title)
                .setMessage(R.string.alert_dialog_content)
                .setCustomTitle(inflater.inflate(R.layout.alert_dialog_custom_title, null, false));
        wireBuilder(builder);

        onView(withId(R.id.test_button)).perform(click());

        verifyCustomTitle();
    }

    @Test
    @MediumTest
    public void testCustomTitlePostCreation() {
        final Context context = mActivityTestRule.getActivity();
        final LayoutInflater inflater = LayoutInflater.from(context);
        final AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(R.string.alert_dialog_title)
                .setMessage(R.string.alert_dialog_content);

        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAlertDialog = builder.create();

                // Configure custom title
                mAlertDialog.setCustomTitle(inflater.inflate(
                        R.layout.alert_dialog_custom_title, null, false));

                mAlertDialog.show();
            }
        });

        // Click the button to create the dialog, configure custom title and show the dialog
        onView(withId(R.id.test_button)).perform(click());

        verifyCustomTitle();
    }

    // Tests for custom view logic

    /**
     * Helper method to verify that setting custom view shows the content of that view.
     */
    private void verifyCustomView() {
        final Context context = mActivityTestRule.getActivity();

        // Test that we're showing a dialog with vertically stacked custom title and content
        final String expectedCustomText1 = context.getString(R.string.alert_dialog_custom_text1);
        final String expectedCustomText2 = context.getString(R.string.alert_dialog_custom_text2);

        // Check that we're showing the content of our custom view
        onView(withId(R.id.alert_dialog_custom_view)).inRoot(isDialog()).check(
                matches(isCompletelyDisplayed()));
        onView(withText(expectedCustomText1)).inRoot(isDialog()).check(
                matches(isCompletelyDisplayed()));
        onView(withText(expectedCustomText1)).inRoot(isDialog()).check(
                matches(not(hasEllipsizedText())));
        onView(withText(expectedCustomText2)).inRoot(isDialog()).check(
                matches(isCompletelyDisplayed()));
        onView(withText(expectedCustomText2)).inRoot(isDialog()).check(
                matches(not(hasEllipsizedText())));
    }

    @Test
    @MediumTest
    public void testCustomView() {
        final Context context = mActivityTestRule.getActivity();
        final LayoutInflater inflater = LayoutInflater.from(context);
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(R.string.alert_dialog_title)
                .setMessage(R.string.alert_dialog_content)
                .setView(inflater.inflate(R.layout.alert_dialog_custom_view, null, false));
        wireBuilder(builder);

        onView(withId(R.id.test_button)).perform(click());

        verifyCustomView();
    }

    @Test
    @MediumTest
    public void testCustomViewById() {
        final Context context = mActivityTestRule.getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(R.string.alert_dialog_title)
                .setMessage(R.string.alert_dialog_content)
                .setView(R.layout.alert_dialog_custom_view);
        wireBuilder(builder);

        onView(withId(R.id.test_button)).perform(click());

        verifyCustomView();
    }

    @Test
    @MediumTest
    public void testCustomViewPostCreation() {
        final Context context = mActivityTestRule.getActivity();
        final LayoutInflater inflater = LayoutInflater.from(context);
        final AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(R.string.alert_dialog_title)
                .setMessage(R.string.alert_dialog_content);

        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAlertDialog = builder.create();

                // Configure custom view
                mAlertDialog.setView(inflater.inflate(
                        R.layout.alert_dialog_custom_view, null, false));

                mAlertDialog.show();
            }
        });

        // Click the button to create the dialog, configure custom view and show the dialog
        onView(withId(R.id.test_button)).perform(click());

        verifyCustomView();
    }

    // Tests for cancel logic

    @Test
    @MediumTest
    public void testCancelCancelableDialog() {
        DialogInterface.OnCancelListener mockCancelListener =
                mock(DialogInterface.OnCancelListener.class);
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivityTestRule.getActivity())
                .setTitle(R.string.alert_dialog_title)
                .setMessage(R.string.alert_dialog_content)
                .setCancelable(true)
                .setOnCancelListener(mockCancelListener);
        wireBuilder(builder);

        onView(withId(R.id.test_button)).perform(click());

        // Emulate a tap on the device BACK button
        Espresso.pressBack();

        // Since our dialog is cancelable, check that the cancel listener has been invoked
        verify(mockCancelListener, times(1)).onCancel(mAlertDialog);
    }

    @Test
    @MediumTest
    public void testCancelNonCancelableDialog() {
        DialogInterface.OnCancelListener mockCancelListener =
                mock(DialogInterface.OnCancelListener.class);
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivityTestRule.getActivity())
                .setTitle(R.string.alert_dialog_title)
                .setMessage(R.string.alert_dialog_content)
                .setCancelable(false)
                .setOnCancelListener(mockCancelListener);
        wireBuilder(builder);

        onView(withId(R.id.test_button)).perform(click());

        // Emulate a tap on the device BACK button
        Espresso.pressBack();

        // Since our dialog is not cancelable, check that the cancel listener has not been invoked
        verify(mockCancelListener, never()).onCancel(mAlertDialog);
    }

    // Tests for items content logic (simple, single-choice, multi-choice)

    private void verifySimpleItemsContent(String[] expectedContent,
            DialogInterface.OnClickListener onClickListener) {
        final int expectedCount = expectedContent.length;

        onView(withId(R.id.test_button)).perform(click());

        final ListView listView = mAlertDialog.getListView();
        assertNotNull("List view is shown", listView);

        final ListAdapter listAdapter = listView.getAdapter();
        assertEquals("List has " + expectedCount + " entries",
                expectedCount, listAdapter.getCount());
        for (int i = 0; i < expectedCount; i++) {
            assertEquals("List entry #" + i, expectedContent[i], listAdapter.getItem(i));
        }

        // Test that all items are showing
        onView(withText("Dialog title")).inRoot(isDialog()).check(matches(isDisplayed()));
        for (int i = 0; i < expectedCount; i++) {
            onData(allOf(is(instanceOf(String.class)), is(expectedContent[i]))).inRoot(isDialog()).
                    check(matches(isDisplayed()));
        }

        // Verify that our click listener hasn't been called yet
        verify(onClickListener, never()).onClick(any(DialogInterface.class), any(int.class));
        // Test that a click on an item invokes the registered listener
        int indexToClick = expectedCount - 2;
        onData(allOf(is(instanceOf(String.class)), is(expectedContent[indexToClick]))).
                inRoot(isDialog()).perform(click());
        verify(onClickListener, times(1)).onClick(mAlertDialog, indexToClick);
    }

    @Test
    @MediumTest
    public void testCustomAdapter() {
        final Context context = mActivityTestRule.getActivity();
        final String[] content = context.getResources().getStringArray(R.array.alert_dialog_items);
        final DialogInterface.OnClickListener mockClickListener =
                mock(DialogInterface.OnClickListener.class);
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivityTestRule.getActivity())
                .setTitle(R.string.alert_dialog_title)
                .setAdapter(
                        new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, content),
                        mockClickListener);
        wireBuilder(builder);

        verifySimpleItemsContent(content, mockClickListener);
    }

    @Test
    @MediumTest
    public void testSimpleItemsFromRuntimeArray() {
        final String[] content = new String[] { "Alice", "Bob", "Charlie", "Delta" };
        final DialogInterface.OnClickListener mockClickListener =
                mock(DialogInterface.OnClickListener.class);
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivityTestRule.getActivity())
                .setTitle(R.string.alert_dialog_title)
                .setItems(content, mockClickListener);
        wireBuilder(builder);

        verifySimpleItemsContent(content, mockClickListener);
    }

    @Test
    @MediumTest
    public void testSimpleItemsFromResourcesArray() {
        final DialogInterface.OnClickListener mockClickListener =
                mock(DialogInterface.OnClickListener.class);
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivityTestRule.getActivity())
                .setTitle(R.string.alert_dialog_title)
                .setItems(R.array.alert_dialog_items, mockClickListener);
        wireBuilder(builder);

        verifySimpleItemsContent(mActivityTestRule.getActivity().getResources().getStringArray(
                R.array.alert_dialog_items), mockClickListener);
    }

    /**
     * Helper method to verify the state of the multi-choice items list. It gets the String
     * array of content and verifies that:
     *
     * 1. The items in the array are rendered as CheckedTextViews inside a ListView
     * 2. Each item in the array is displayed
     * 3. Checked state of each row in the ListView corresponds to the matching entry in the
     *    passed boolean array
     */
    private void verifyMultiChoiceItemsState(String[] expectedContent,
            boolean[] checkedTracker) {
        final int expectedCount = expectedContent.length;

        final ListView listView = mAlertDialog.getListView();
        assertNotNull("List view is shown", listView);

        final ListAdapter listAdapter = listView.getAdapter();
        assertEquals("List has " + expectedCount + " entries",
                expectedCount, listAdapter.getCount());
        for (int i = 0; i < expectedCount; i++) {
            assertEquals("List entry #" + i, expectedContent[i], listAdapter.getItem(i));
        }

        for (int i = 0; i < expectedCount; i++) {
            Matcher checkedStateMatcher = checkedTracker[i] ? TestUtilsMatchers.isCheckedTextView() :
                    TestUtilsMatchers.isNonCheckedTextView();
            // Check that the corresponding row is rendered as CheckedTextView with expected
            // checked state.
            onData(allOf(is(instanceOf(String.class)), is(expectedContent[i]))).inRoot(isDialog()).
                    check(matches(allOf(
                            isDisplayed(),
                            isAssignableFrom(CheckedTextView.class),
                            isDescendantOfA(isAssignableFrom(ListView.class)),
                            checkedStateMatcher)));
        }
    }

    private void verifyMultiChoiceItemsContent(String[] expectedContent,
            final boolean[] checkedTracker) {
        final int expectedCount = expectedContent.length;

        onView(withId(R.id.test_button)).perform(click());

        final ListView listView = mAlertDialog.getListView();
        assertNotNull("List view is shown", listView);

        final ListAdapter listAdapter = listView.getAdapter();
        assertEquals("List has " + expectedCount + " entries",
                expectedCount, listAdapter.getCount());
        for (int i = 0; i < expectedCount; i++) {
            assertEquals("List entry #" + i, expectedContent[i], listAdapter.getItem(i));
        }

        // Test that all items are showing
        onView(withText("Dialog title")).inRoot(isDialog()).check(matches(isDisplayed()));
        verifyMultiChoiceItemsState(expectedContent, checkedTracker);

        // We're going to click item #1 and test that the click listener has been invoked to
        // update the original state array
        boolean[] expectedAfterClick1 = checkedTracker.clone();
        expectedAfterClick1[1] = !expectedAfterClick1[1];
        onData(allOf(is(instanceOf(String.class)), is(expectedContent[1]))).
                inRoot(isDialog()).perform(click());
        verifyMultiChoiceItemsState(expectedContent, expectedAfterClick1);

        // Now click item #1 again and test that the click listener has been invoked to update the
        // original state array again
        expectedAfterClick1[1] = !expectedAfterClick1[1];
        onData(allOf(is(instanceOf(String.class)), is(expectedContent[1]))).
                inRoot(isDialog()).perform(click());
        verifyMultiChoiceItemsState(expectedContent, expectedAfterClick1);

        // Now we're going to click the last item and test that the click listener has been invoked
        // to update the original state array
        boolean[] expectedAfterClickLast = checkedTracker.clone();
        expectedAfterClickLast[expectedCount - 1] = !expectedAfterClickLast[expectedCount - 1];
        onData(allOf(is(instanceOf(String.class)), is(expectedContent[expectedCount - 1]))).
                inRoot(isDialog()).perform(click());
        verifyMultiChoiceItemsState(expectedContent, expectedAfterClickLast);
    }

    @Test
    @MediumTest
    public void testMultiChoiceItemsFromRuntimeArray() {
        final String[] content = new String[] { "Alice", "Bob", "Charlie", "Delta" };
        final boolean[] checkedTracker = new boolean[] { false, true, false, false };
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivityTestRule.getActivity())
                .setTitle(R.string.alert_dialog_title)
                .setMultiChoiceItems(
                        content, checkedTracker,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which,
                                    boolean isChecked) {
                                checkedTracker[which] = isChecked;
                            }
                        });
        wireBuilder(builder);

        // Pass the same boolean[] array as used for initialization since our click listener
        // will be updating its content.
        verifyMultiChoiceItemsContent(content, checkedTracker);
    }

    @Test
    @MediumTest
    public void testMultiChoiceItemsFromResourcesArray() {
        final boolean[] checkedTracker = new boolean[] { true, false, true, false };
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivityTestRule.getActivity())
                .setTitle(R.string.alert_dialog_title)
                .setMultiChoiceItems(R.array.alert_dialog_items, checkedTracker,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which,
                                    boolean isChecked) {
                                checkedTracker[which] = isChecked;
                            }
                        });
        wireBuilder(builder);

        verifyMultiChoiceItemsContent(
                mActivityTestRule.getActivity().getResources().getStringArray(
                        R.array.alert_dialog_items),
                checkedTracker);
    }

    /**
     * Helper method to verify the state of the single-choice items list. It gets the String
     * array of content and verifies that:
     *
     * 1. The items in the array are rendered as CheckedTextViews inside a ListView
     * 2. Each item in the array is displayed
     * 3. Only one row in the ListView is checked, and that corresponds to the passed
     *    integer index.
     */
    private void verifySingleChoiceItemsState(String[] expectedContent,
            int currentlyExpectedSelectionIndex) {
        final int expectedCount = expectedContent.length;

        final ListView listView = mAlertDialog.getListView();
        assertNotNull("List view is shown", listView);

        final ListAdapter listAdapter = listView.getAdapter();
        assertEquals("List has " + expectedCount + " entries",
                expectedCount, listAdapter.getCount());
        for (int i = 0; i < expectedCount; i++) {
            assertEquals("List entry #" + i, expectedContent[i], listAdapter.getItem(i));
        }

        for (int i = 0; i < expectedCount; i++) {
            Matcher checkedStateMatcher = (i == currentlyExpectedSelectionIndex) ?
                    TestUtilsMatchers.isCheckedTextView() :
                    TestUtilsMatchers.isNonCheckedTextView();
            // Check that the corresponding row is rendered as CheckedTextView with expected
            // checked state.
            onData(allOf(is(instanceOf(String.class)), is(expectedContent[i]))).inRoot(isDialog()).
                    check(matches(allOf(
                            isDisplayed(),
                            isAssignableFrom(CheckedTextView.class),
                            isDescendantOfA(isAssignableFrom(ListView.class)),
                            checkedStateMatcher)));
        }
    }

    private void verifySingleChoiceItemsContent(String[] expectedContent,
            int initialSelectionIndex, DialogInterface.OnClickListener onClickListener) {
        final int expectedCount = expectedContent.length;
        int currentlyExpectedSelectionIndex = initialSelectionIndex;

        onView(withId(R.id.test_button)).perform(click());

        // Test that all items are showing
        onView(withText("Dialog title")).inRoot(isDialog()).check(matches(isDisplayed()));
        verifySingleChoiceItemsState(expectedContent, currentlyExpectedSelectionIndex);

        // We're going to click the first unselected item and test that the click listener has
        // been invoked.
        currentlyExpectedSelectionIndex = (currentlyExpectedSelectionIndex == 0) ? 1 : 0;
        onData(allOf(is(instanceOf(String.class)),
                is(expectedContent[currentlyExpectedSelectionIndex]))).
                    inRoot(isDialog()).perform(click());
        verify(onClickListener, times(1)).onClick(mAlertDialog, currentlyExpectedSelectionIndex);
        verifySingleChoiceItemsState(expectedContent, currentlyExpectedSelectionIndex);

        // Now click the same item again and test that the selection has not changed
        onData(allOf(is(instanceOf(String.class)),
                is(expectedContent[currentlyExpectedSelectionIndex]))).
                inRoot(isDialog()).perform(click());
        verify(onClickListener, times(2)).onClick(mAlertDialog, currentlyExpectedSelectionIndex);
        verifySingleChoiceItemsState(expectedContent, currentlyExpectedSelectionIndex);

        // Now we're going to click the last item and test that the click listener has been invoked
        // to update the original state array
        currentlyExpectedSelectionIndex = expectedCount - 1;
        onData(allOf(is(instanceOf(String.class)),
                is(expectedContent[currentlyExpectedSelectionIndex]))).
                inRoot(isDialog()).perform(click());
        verify(onClickListener, times(1)).onClick(mAlertDialog, currentlyExpectedSelectionIndex);
        verifySingleChoiceItemsState(expectedContent, currentlyExpectedSelectionIndex);
    }

    @Test
    @LargeTest
    public void testSingleChoiceItemsFromRuntimeArray() {
        final String[] content = new String[] { "Alice", "Bob", "Charlie", "Delta" };
        final DialogInterface.OnClickListener mockClickListener =
                mock(DialogInterface.OnClickListener.class);
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivityTestRule.getActivity())
                .setTitle(R.string.alert_dialog_title)
                .setSingleChoiceItems(content, 2, mockClickListener);
        wireBuilder(builder);

        verifySingleChoiceItemsContent(content, 2, mockClickListener);
    }

    @Test
    @LargeTest
    public void testSingleChoiceItemsFromResourcesArray() {
        final DialogInterface.OnClickListener mockClickListener =
                mock(DialogInterface.OnClickListener.class);
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivityTestRule.getActivity())
                .setTitle(R.string.alert_dialog_title)
                .setSingleChoiceItems(R.array.alert_dialog_items, 1, mockClickListener);
        wireBuilder(builder);

        verifySingleChoiceItemsContent(new String[] { "Albania", "Belize", "Chad", "Djibouti" }, 1,
                mockClickListener);
    }

    // Tests for icon logic

    @Test
    @MediumTest
    public void testIconResource() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivityTestRule.getActivity())
                .setTitle(R.string.alert_dialog_title)
                .setMessage(R.string.alert_dialog_content)
                .setIcon(R.drawable.test_drawable_red);

        wireBuilder(builder);

        onView(withId(R.id.test_button)).perform(click());

        // Find the title icon as a visible view that is the sibling of our title
        ViewInteraction titleIconInteraction = onView(allOf(
                isAssignableFrom(ImageView.class),
                isDisplayed(),
                hasSibling(withText("Dialog title"))));
        // And check that it's the expected red color
        titleIconInteraction.check(matches(TestUtilsMatchers.drawable(0xFFFF6030)));
    }

    @Test
    @MediumTest
    public void testIconResourceChangeAfterInitialSetup() throws Throwable {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivityTestRule.getActivity())
                .setTitle(R.string.alert_dialog_title)
                .setMessage(R.string.alert_dialog_content)
                .setIcon(R.drawable.test_drawable_red);

        wireBuilder(builder);

        onView(withId(R.id.test_button)).perform(click());

        // Emulate background loading of the new icon
        Thread.sleep(1000);

        // Change the icon
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAlertDialog.setIcon(R.drawable.test_drawable_green);
            }
        });

        // Find the title icon as a visible view that is the sibling of our title
        ViewInteraction titleIconInteraction = onView(allOf(
                isAssignableFrom(ImageView.class),
                isDisplayed(),
                hasSibling(withText("Dialog title"))));
        // And check that it's the expected (newly set) green color
        titleIconInteraction.check(matches(TestUtilsMatchers.drawable(0xFF50E080)));
    }

    @Test
    @MediumTest
    public void testIconResourceChangeWithNoInitialSetup() throws Throwable {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivityTestRule.getActivity())
                .setTitle(R.string.alert_dialog_title)
                .setMessage(R.string.alert_dialog_content);

        wireBuilder(builder);

        onView(withId(R.id.test_button)).perform(click());

        // Emulate background loading of the new icon
        Thread.sleep(1000);

        // Change the icon
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAlertDialog.setIcon(R.drawable.test_drawable_green);
            }
        });

        // Find the title icon as a visible view that is the sibling of our title
        ViewInteraction titleIconInteraction = onView(allOf(
                isAssignableFrom(ImageView.class),
                isDisplayed(),
                hasSibling(withText("Dialog title"))));
        // And check that it's the expected (newly set) green color
        titleIconInteraction.check(matches(TestUtilsMatchers.drawable(0xFF50E080)));
    }

    @Test
    @MediumTest
    public void testIconResourceRemoveAfterInitialSetup() throws Throwable {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivityTestRule.getActivity())
                .setTitle(R.string.alert_dialog_title)
                .setMessage(R.string.alert_dialog_content)
                .setIcon(R.drawable.test_drawable_red);

        wireBuilder(builder);

        onView(withId(R.id.test_button)).perform(click());

        // Emulate background resetting of the icon
        Thread.sleep(1000);

        // Change the icon
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAlertDialog.setIcon(0);
            }
        });

        // Find the title icon as a visible view that is the sibling of our title
        ViewInteraction titleIconInteraction = onView(allOf(
                isAssignableFrom(ImageView.class),
                isDisplayed(),
                hasSibling(withText("Dialog title"))));
        // And check that we couldn't find the title icon (since it's expected to be GONE)
        titleIconInteraction.check(doesNotExist());
    }

    @Test
    @MediumTest
    public void testIconDrawable() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivityTestRule.getActivity())
                .setTitle(R.string.alert_dialog_title)
                .setMessage(R.string.alert_dialog_content)
                .setIcon(new TestDrawable(0xFF807060, 40, 40));

        wireBuilder(builder);

        onView(withId(R.id.test_button)).perform(click());

        // Find the title icon as a visible view that is the sibling of our title
        ViewInteraction titleIconInteraction = onView(allOf(
                isAssignableFrom(ImageView.class),
                isDisplayed(),
                hasSibling(withText("Dialog title"))));
        // And check that it's the expected red color
        titleIconInteraction.check(matches(TestUtilsMatchers.drawable(0xFF807060)));
    }

    @Test
    @MediumTest
    public void testIconResourceDrawableAfterInitialSetup() throws Throwable {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivityTestRule.getActivity())
                .setTitle(R.string.alert_dialog_title)
                .setMessage(R.string.alert_dialog_content)
                .setIcon(new TestDrawable(0xFF807060, 40, 40));

        wireBuilder(builder);

        onView(withId(R.id.test_button)).perform(click());

        // Emulate background loading of the new icon
        Thread.sleep(1000);

        // Change the icon
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAlertDialog.setIcon(new TestDrawable(0xFF503090, 40, 40));
            }
        });

        // Find the title icon as a visible view that is the sibling of our title
        ViewInteraction titleIconInteraction = onView(allOf(
                isAssignableFrom(ImageView.class),
                isDisplayed(),
                hasSibling(withText("Dialog title"))));
        // And check that it's the expected (newly set) green color
        titleIconInteraction.check(matches(TestUtilsMatchers.drawable(0xFF503090)));
    }

    @Test
    @MediumTest
    public void testIconDrawableChangeWithNoInitialSetup() throws Throwable {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivityTestRule.getActivity())
                .setTitle(R.string.alert_dialog_title)
                .setMessage(R.string.alert_dialog_content);

        wireBuilder(builder);

        onView(withId(R.id.test_button)).perform(click());

        // Emulate background loading of the new icon
        Thread.sleep(1000);

        // Change the icon
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAlertDialog.setIcon(new TestDrawable(0xFF503090, 40, 40));
            }
        });

        // Find the title icon as a visible view that is the sibling of our title
        ViewInteraction titleIconInteraction = onView(allOf(
                isAssignableFrom(ImageView.class),
                isDisplayed(),
                hasSibling(withText("Dialog title"))));
        // And check that it's the expected (newly set) green color
        titleIconInteraction.check(matches(TestUtilsMatchers.drawable(0xFF503090)));
    }

    @Test
    @MediumTest
    public void testIconDrawableRemoveAfterInitialSetup() throws Throwable {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivityTestRule.getActivity())
                .setTitle(R.string.alert_dialog_title)
                .setMessage(R.string.alert_dialog_content)
                .setIcon(new TestDrawable(0xFF807060, 40, 40));

        wireBuilder(builder);

        onView(withId(R.id.test_button)).perform(click());

        // Emulate background resetting of the icon
        Thread.sleep(1000);

        // Change the icon
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAlertDialog.setIcon(null);
            }
        });

        // Find the title icon as a visible view that is the sibling of our title
        ViewInteraction titleIconInteraction = onView(allOf(
                isAssignableFrom(ImageView.class),
                isDisplayed(),
                hasSibling(withText("Dialog title"))));
        // And check that we couldn't find the title icon (since it's expected to be GONE)
        titleIconInteraction.check(doesNotExist());
    }

    // Tests for buttons logic

    /**
     * Helper method to verify visibility and text content of dialog buttons. Gets expected texts
     * for three buttons (positive, negative and neutral) and for each button verifies that:
     *
     * If the text is null or empty, that the button is GONE
     * If the text is not empty, that the button is VISIBLE and shows the corresponding text
     */
    private void verifyButtonContent(String expectedPositiveButtonText,
            String expectedNegativeButtonText, String expectedNeutralButtonText) {
        assertTrue("Dialog is showing", mAlertDialog.isShowing());

        final Button positiveButton = mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        final Button negativeButton = mAlertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        final Button neutralButton = mAlertDialog.getButton(AlertDialog.BUTTON_NEUTRAL);

        if (TextUtils.isEmpty(expectedPositiveButtonText)) {
            assertEquals("Positive button not shown", View.GONE, positiveButton.getVisibility());
        } else {
            assertEquals("Positive button shown", View.VISIBLE, positiveButton.getVisibility());
            assertEquals("Positive button text", expectedPositiveButtonText,
                    positiveButton.getText());
        }

        if (TextUtils.isEmpty(expectedNegativeButtonText)) {
            assertEquals("Negative button not shown", View.GONE, negativeButton.getVisibility());
        } else {
            assertEquals("Negative button shown", View.VISIBLE, negativeButton.getVisibility());
            assertEquals("Negative button text", expectedNegativeButtonText,
                    negativeButton.getText());
        }

        if (TextUtils.isEmpty(expectedNeutralButtonText)) {
            assertEquals("Neutral button not shown", View.GONE, neutralButton.getVisibility());
        } else {
            assertEquals("Neutral button shown", View.VISIBLE, neutralButton.getVisibility());
            assertEquals("Neutral button text", expectedNeutralButtonText,
                    neutralButton.getText());
        }
    }

    /**
     * Helper method to verify dialog state after a button has been clicked.
     */
    private void verifyPostButtonClickState(int whichButtonClicked,
            DialogInterface.OnDismissListener onDismissListener,
            Handler messageHandler) {
        // Verify that a Message with expected 'what' field has been posted on our mock handler
        ArgumentCaptor<Message> messageArgumentCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageHandler, times(1)).sendMessageDelayed(
                messageArgumentCaptor.capture(), anyLong());
        assertEquals("Button clicked", whichButtonClicked, messageArgumentCaptor.getValue().what);
        // Verify that the dialog is no longer showing
        assertFalse("Dialog is not showing", mAlertDialog.isShowing());
        if (onDismissListener != null) {
            // And that our mock listener has been called when the dialog was dismissed
            verify(onDismissListener, times(1)).onDismiss(mAlertDialog);
        }
    }

    /**
     * Helper method to verify dialog state after a button has been clicked.
     */
    private void verifyPostButtonClickState(int whichButtonClicked,
            DialogInterface.OnClickListener onClickListener,
            DialogInterface.OnDismissListener onDismissListener) {
        if (onClickListener != null) {
            verify(onClickListener, times(1)).onClick(mAlertDialog, whichButtonClicked);
        }
        assertFalse("Dialog is not showing", mAlertDialog.isShowing());
        if (onDismissListener != null) {
            verify(onDismissListener, times(1)).onDismiss(mAlertDialog);
        }
    }

    /**
     * Helper method to verify button-related logic for setXXXButton on AlertDialog.Builder
     * that gets CharSequence parameter. This method configures the dialog buttons based
     * on the passed texts (some of which may be null or empty, in which case the corresponding
     * button is not configured), tests the buttons visibility and texts, simulates a click
     * on the specified button and then tests the post-click dialog state.
     */
    private void verifyDialogButtons(String positiveButtonText, String negativeButtonText,
            String neutralButtonText, int whichButtonToClick) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(mActivityTestRule.getActivity())
                .setTitle(R.string.alert_dialog_title);
        // Configure buttons with non-empty texts
        DialogInterface.OnClickListener mockClickListener =
                mock(DialogInterface.OnClickListener.class);
        if (!TextUtils.isEmpty(positiveButtonText)) {
            builder.setPositiveButton(positiveButtonText, mockClickListener);
        }
        if (!TextUtils.isEmpty(negativeButtonText)) {
            builder.setNegativeButton(negativeButtonText, mockClickListener);
        }
        if (!TextUtils.isEmpty(neutralButtonText)) {
            builder.setNeutralButton(neutralButtonText, mockClickListener);
        }
        // Set a dismiss listener to verify that the dialog is dismissed on clicking any button
        DialogInterface.OnDismissListener mockDismissListener =
                mock(DialogInterface.OnDismissListener.class);
        builder.setOnDismissListener(mockDismissListener);

        // Wire the builder to the button click and click that button to show the dialog
        wireBuilder(builder);
        onView(withId(R.id.test_button)).perform(click());

        // Check that the dialog is showing the configured buttons
        verifyButtonContent(positiveButtonText, negativeButtonText, neutralButtonText);

        // Click the specified button and verify the post-click state
        String textOfButtonToClick = null;
        switch (whichButtonToClick) {
            case DialogInterface.BUTTON_POSITIVE:
                textOfButtonToClick = positiveButtonText;
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                textOfButtonToClick = negativeButtonText;
                break;
            case DialogInterface.BUTTON_NEUTRAL:
                textOfButtonToClick = neutralButtonText;
                break;
        }
        onView(withText(textOfButtonToClick)).inRoot(isDialog()).perform(click());
        verifyPostButtonClickState(whichButtonToClick, mockClickListener, mockDismissListener);
    }

    /**
     * Helper method to verify button-related logic for setXXXButton on AlertDialog.Builder
     * that gets string resource ID parameter. This method configures the dialog buttons based
     * on the passed texts (some of which may be null or empty, in which case the corresponding
     * button is not configured), tests the buttons visibility and texts, simulates a click
     * on the specified button and then tests the post-click dialog state.
     */
    private void verifyDialogButtons(@StringRes int positiveButtonTextResId,
            @StringRes int negativeButtonTextResId,
            @StringRes int neutralButtonTextResId, int whichButtonToClick) {
        Context context = mActivityTestRule.getActivity();
        String positiveButtonText = null;
        String negativeButtonText = null;
        String neutralButtonText = null;

        final AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(R.string.alert_dialog_title);
        DialogInterface.OnClickListener mockClickListener =
                mock(DialogInterface.OnClickListener.class);
        // Configure buttons with non-zero text resource IDs
        if (positiveButtonTextResId != 0) {
            positiveButtonText = context.getString(positiveButtonTextResId);
            builder.setPositiveButton(positiveButtonTextResId, mockClickListener);
        }
        if (negativeButtonTextResId != 0) {
            negativeButtonText = context.getString(negativeButtonTextResId);
            builder.setNegativeButton(negativeButtonTextResId, mockClickListener);
        }
        if (neutralButtonTextResId != 0) {
            neutralButtonText = context.getString(neutralButtonTextResId);
            builder.setNeutralButton(neutralButtonTextResId, mockClickListener);
        }
        // Set a dismiss listener to verify that the dialog is dismissed on clicking any button
        DialogInterface.OnDismissListener mockDismissListener =
                mock(DialogInterface.OnDismissListener.class);
        builder.setOnDismissListener(mockDismissListener);

        // Wire the builder to the button click and click that button to show the dialog
        wireBuilder(builder);
        onView(withId(R.id.test_button)).perform(click());

        // Check that the dialog is showing the configured buttons
        verifyButtonContent(positiveButtonText, negativeButtonText, neutralButtonText);

        // Click the specified button and verify the post-click state
        String textOfButtonToClick = null;
        switch (whichButtonToClick) {
            case DialogInterface.BUTTON_POSITIVE:
                textOfButtonToClick = positiveButtonText;
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                textOfButtonToClick = negativeButtonText;
                break;
            case DialogInterface.BUTTON_NEUTRAL:
                textOfButtonToClick = neutralButtonText;
                break;
        }
        onView(withText(textOfButtonToClick)).inRoot(isDialog()).perform(click());
        verifyPostButtonClickState(whichButtonToClick, mockClickListener, mockDismissListener);
    }

    /**
     * Helper method to verify button-related logic for setButton on AlertDialog after the
     * dialog has been create()'d. This method configures the dialog buttons based
     * on the passed texts (some of which may be null or empty, in which case the corresponding
     * button is not configured), tests the buttons visibility and texts, simulates a click
     * on the specified button and then tests the post-click dialog state.
     */
    private void verifyDialogButtonsPostCreation(final String positiveButtonText,
            final String negativeButtonText, final String neutralButtonText,
            int whichButtonToClick) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(mActivityTestRule.getActivity())
                .setTitle(R.string.alert_dialog_title);
        // Set a dismiss listener to verify that the dialog is dismissed on clicking any button
        DialogInterface.OnDismissListener mockDismissListener =
                mock(DialogInterface.OnDismissListener.class);
        builder.setOnDismissListener(mockDismissListener);

        final DialogInterface.OnClickListener mockClickListener =
                mock(DialogInterface.OnClickListener.class);

        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAlertDialog = builder.create();
                // Configure buttons with non-empty texts
                if (!TextUtils.isEmpty(positiveButtonText)) {
                    mAlertDialog.setButton(DialogInterface.BUTTON_POSITIVE, positiveButtonText,
                            mockClickListener);
                }
                if (!TextUtils.isEmpty(negativeButtonText)) {
                    mAlertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, negativeButtonText,
                            mockClickListener);
                }
                if (!TextUtils.isEmpty(neutralButtonText)) {
                    mAlertDialog.setButton(DialogInterface.BUTTON_NEUTRAL, neutralButtonText,
                            mockClickListener);
                }

                mAlertDialog.show();
            }
        });

        // Click the button to create the dialog, configure the buttons and show the dialog
        onView(withId(R.id.test_button)).perform(click());

        // Check that the dialog is showing the configured buttons
        verifyButtonContent(positiveButtonText, negativeButtonText, neutralButtonText);

        // Click the specified button and verify the post-click state
        String textOfButtonToClick = null;
        switch (whichButtonToClick) {
            case DialogInterface.BUTTON_POSITIVE:
                textOfButtonToClick = positiveButtonText;
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                textOfButtonToClick = negativeButtonText;
                break;
            case DialogInterface.BUTTON_NEUTRAL:
                textOfButtonToClick = neutralButtonText;
                break;
        }
        onView(withText(textOfButtonToClick)).inRoot(isDialog()).perform(click());
        verifyPostButtonClickState(whichButtonToClick, mockClickListener, null);
    }

    /**
     * Helper method to verify button-related logic for setButton on AlertDialog after the
     * dialog has been create()'d. This method configures the dialog buttons based
     * on the passed texts (some of which may be null or empty, in which case the corresponding
     * button is not configured), tests the buttons visibility and texts, simulates a click
     * on the specified button and then tests the post-click dialog state.
     */
    private void verifyDialogButtonsPostCreationMessage(final String positiveButtonText,
            final String negativeButtonText, final String neutralButtonText,
            int whichButtonToClick) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(mActivityTestRule.getActivity())
                .setTitle(R.string.alert_dialog_title);
        // Set a dismiss listener to verify that the dialog is dismissed on clicking any button
        DialogInterface.OnDismissListener mockDismissListener =
                mock(DialogInterface.OnDismissListener.class);
        builder.setOnDismissListener(mockDismissListener);

        final Handler mockMessageHandler = mock(Handler.class);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAlertDialog = builder.create();
                // Configure buttons with non-empty texts
                if (!TextUtils.isEmpty(positiveButtonText)) {
                    mAlertDialog.setButton(DialogInterface.BUTTON_POSITIVE, positiveButtonText,
                            Message.obtain(mockMessageHandler, DialogInterface.BUTTON_POSITIVE));
                }
                if (!TextUtils.isEmpty(negativeButtonText)) {
                    mAlertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, negativeButtonText,
                            Message.obtain(mockMessageHandler, DialogInterface.BUTTON_NEGATIVE));
                }
                if (!TextUtils.isEmpty(neutralButtonText)) {
                    mAlertDialog.setButton(DialogInterface.BUTTON_NEUTRAL, neutralButtonText,
                            Message.obtain(mockMessageHandler, DialogInterface.BUTTON_NEUTRAL));
                }

                mAlertDialog.show();
            }
        });

        // Click the button to create the dialog, configure the buttons and show the dialog
        onView(withId(R.id.test_button)).perform(click());

        // Check that the dialog is showing the configured buttons
        verifyButtonContent(positiveButtonText, negativeButtonText, neutralButtonText);

        // Click the specified button and verify the post-click state
        String textOfButtonToClick = null;
        switch (whichButtonToClick) {
            case DialogInterface.BUTTON_POSITIVE:
                textOfButtonToClick = positiveButtonText;
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                textOfButtonToClick = negativeButtonText;
                break;
            case DialogInterface.BUTTON_NEUTRAL:
                textOfButtonToClick = neutralButtonText;
                break;
        }
        onView(withText(textOfButtonToClick)).inRoot(isDialog()).perform(click());
        verifyPostButtonClickState(whichButtonToClick, mockDismissListener, mockMessageHandler);
    }

    @Test
    @MediumTest
    public void testButtonVisibility() {
        final String positiveButtonText = "Positive button";
        final String negativeButtonText = "Negative button";
        final String neutralButtonText = "Neutral button";
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivityTestRule.getActivity())
                .setTitle(R.string.alert_dialog_title)
                .setPositiveButton(positiveButtonText, null)
                .setNegativeButton(negativeButtonText, null)
                .setNeutralButton(neutralButtonText, null);
        wireBuilder(builder);

        onView(withId(R.id.test_button)).perform(click());

        // Positive button should be fully displayed with no text eliding
        onView(withText(positiveButtonText)).inRoot(isDialog()).check(
                matches(isCompletelyDisplayed()));
        onView(withText(positiveButtonText)).inRoot(isDialog()).check(
                matches(not(hasEllipsizedText())));

        // Negative button should be fully displayed with no text eliding
        onView(withText(negativeButtonText)).inRoot(isDialog()).check(
                matches(isCompletelyDisplayed()));
        onView(withText(negativeButtonText)).inRoot(isDialog()).check(
                matches(not(hasEllipsizedText())));

        // Neutral button should be fully displayed with no text eliding
        onView(withText(neutralButtonText)).inRoot(isDialog()).check(
                matches(isCompletelyDisplayed()));
        onView(withText(neutralButtonText)).inRoot(isDialog()).check(
                matches(not(hasEllipsizedText())));
    }

    @Test
    @LargeTest
    public void testButtons() {
        // Positive-only button
        verifyDialogButtons("Positive", null, null, AlertDialog.BUTTON_POSITIVE);
        verifyDialogButtons(R.string.alert_dialog_positive_button, 0, 0,
                AlertDialog.BUTTON_POSITIVE);
        verifyDialogButtonsPostCreation("Post positive", null, null, AlertDialog.BUTTON_POSITIVE);
        verifyDialogButtonsPostCreationMessage("Message positive", null, null,
                AlertDialog.BUTTON_POSITIVE);

        // Negative-only button
        verifyDialogButtons(null, "Negative", null, AlertDialog.BUTTON_NEGATIVE);
        verifyDialogButtons(0, R.string.alert_dialog_negative_button, 0,
                AlertDialog.BUTTON_NEGATIVE);
        verifyDialogButtonsPostCreation(null, "Post negative", null, AlertDialog.BUTTON_NEGATIVE);
        verifyDialogButtonsPostCreationMessage(null, "Message negative", null,
                AlertDialog.BUTTON_NEGATIVE);

        // Neutral-only button
        verifyDialogButtons(null, null, "Neutral", AlertDialog.BUTTON_NEUTRAL);
        verifyDialogButtons(0, 0, R.string.alert_dialog_neutral_button, AlertDialog.BUTTON_NEUTRAL);
        verifyDialogButtonsPostCreation(null, null, "Post neutral", AlertDialog.BUTTON_NEUTRAL);
        verifyDialogButtonsPostCreationMessage(null, null, "Message neutral",
                AlertDialog.BUTTON_NEUTRAL);

        // Show positive and negative, click positive
        verifyDialogButtons(R.string.alert_dialog_positive_button,
                R.string.alert_dialog_negative_button, 0, AlertDialog.BUTTON_POSITIVE);

        // Show positive and neutral, click neutral
        verifyDialogButtons("Positive", null, "Neutral", AlertDialog.BUTTON_NEUTRAL);

        // Show negative and neutral, click negative
        verifyDialogButtonsPostCreationMessage(null, "Message negative",
                "Message neutral", AlertDialog.BUTTON_NEGATIVE);

        // Show all, click positive
        verifyDialogButtonsPostCreation("Post positive", "Post negative", "Post neutral",
                AlertDialog.BUTTON_POSITIVE);
    }

    private static class TestDrawable extends ColorDrawable {
        private int mWidth;
        private int mHeight;

        public TestDrawable(@ColorInt int color, int width, int height) {
            super(color);
            mWidth = width;
            mHeight = height;
        }

        @Override
        public int getIntrinsicWidth() {
            return mWidth;
        }

        @Override
        public int getIntrinsicHeight() {
            return mHeight;
        }
    }
}
