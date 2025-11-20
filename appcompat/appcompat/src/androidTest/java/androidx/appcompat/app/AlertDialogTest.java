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

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.PositionAssertions.isBelow;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.LayoutMatchers.hasEllipsizedText;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.hasSibling;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.StringRes;
import androidx.appcompat.test.R;
import androidx.appcompat.testutils.TestUtilsMatchers;
import androidx.test.annotation.UiThreadTest;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.ViewInteraction;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.rule.ActivityTestRule;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
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
@SuppressWarnings("unchecked")
@LargeTest
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
        if (mAlertDialog != null && mAlertDialog.isShowing()) {
            mActivityTestRule.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mAlertDialog.dismiss();
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

    @SdkSuppress(maxSdkVersion = 34) // b/427246833
    @Test
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

    @SdkSuppress(maxSdkVersion = 34) // b/427246833
    @Test
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
    @SdkSuppress(maxSdkVersion = 34) // b/427246833
    @Test
    public void testMessageString() {
        final String dialogMessage = "Dialog message";
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivityTestRule.getActivity())
                .setTitle(R.string.alert_dialog_title)
                .setMessage(dialogMessage);
        wireBuilder(builder);

        onView(withId(R.id.test_button)).perform(click());
        onView(withText(dialogMessage)).inRoot(isDialog()).check(matches(isDisplayed()));
    }

    @SdkSuppress(maxSdkVersion = 34) // b/427246833
    @Test
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

    // Tests for title
    @SdkSuppress(maxSdkVersion = 34) // b/427246833
    @Test
    public void testThemeMainFontIsAppliedToTitle() {
        final Context context = mActivityTestRule.getActivity();
        context.setTheme(R.style.Theme_CustomFont);

        final AlertDialog.Builder builder = new AlertDialog.Builder(mActivityTestRule.getActivity())
                .setTitle(R.string.alert_dialog_title);
        wireBuilder(builder);

        final String title = context.getString(R.string.alert_dialog_title);
        onView(withId(R.id.test_button)).perform(click());
        onView(withText(title)).inRoot(isDialog()).check(matches(hasA3emFont()));
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

    @SdkSuppress(maxSdkVersion = 34) // b/427246833
    @Test
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

    @SdkSuppress(maxSdkVersion = 34) // b/427246833
    @Test
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

    @SdkSuppress(maxSdkVersion = 34) // b/427246833
    @Test
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

    @SdkSuppress(maxSdkVersion = 34) // b/427246833
    @Test
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

    @SdkSuppress(maxSdkVersion = 34) // b/427246833
    @Test
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

    @SdkSuppress(maxSdkVersion = 34) // b/427246833
    @Test
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

    @SdkSuppress(maxSdkVersion = 34) // b/427246833
    @Test
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

    @SdkSuppress(maxSdkVersion = 34) // b/427246833
    @Test
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

    @SdkSuppress(maxSdkVersion = 34) // b/427246833
    @Test
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

    @SdkSuppress(maxSdkVersion = 34) // b/427246833
    @Test
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

    @SdkSuppress(maxSdkVersion = 34) // b/427246833
    @Test
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

    @SdkSuppress(maxSdkVersion = 34) // b/427246833
    @Test
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

    @SdkSuppress(maxSdkVersion = 34) // b/427246833
    @Test
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

    @SdkSuppress(maxSdkVersion = 34) // b/427246833
    @Test
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

    @SdkSuppress(maxSdkVersion = 34) // b/427246833
    @Test
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

    @SdkSuppress(maxSdkVersion = 34) // b/427246833
    @Test
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

    @SdkSuppress(maxSdkVersion = 34) // b/427246833
    @Test
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

    @SdkSuppress(maxSdkVersion = 34) // b/427246833
    @Test
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

    @SdkSuppress(maxSdkVersion = 34) // b/427246833
    @Test
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

    @SdkSuppress(maxSdkVersion = 34) // b/427246833
    @Test
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

    @SdkSuppress(maxSdkVersion = 34) // b/427246833
    @Test
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

    @SdkSuppress(maxSdkVersion = 34) // b/427246833
    @Test
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

    @SdkSuppress(maxSdkVersion = 34) // b/427246833
    @Test
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

    @SdkSuppress(maxSdkVersion = 34) // b/427246833
    @Test
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

    @SdkSuppress(maxSdkVersion = 34) // b/427246833
    @Test
    @UiThreadTest
    public void testBackgroundDrawable() throws Throwable {
        final AlertDialog dialog = new AlertDialog.Builder(mActivityTestRule.getActivity())
                .setTitle(R.string.alert_dialog_title)
                .setMessage(R.string.alert_dialog_content)
                .create();

        // Now set the windowBackground of the Dialog
        final ColorDrawable background = new ColorDrawable(Color.MAGENTA);
        final Window window = dialog.getWindow();
        final View decorView = window.getDecorView();
        window.setBackgroundDrawable(background);

        // Show the Dialog
        dialog.show();

        // And assert that the background is maintained
        assertSame(background, decorView.getBackground());
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

    /**
     * Matcher checking that:
     * - the View is a TextView
     * - uses a font with the property that the width of glyph 'a' is triple the width of glyph 'b'
     */
    private static class A3emFontMatcher extends TypeSafeMatcher<View> {

        private static float measureText(final String text, final Typeface typeface) {
            final Paint paint = new Paint();

            paint.setTypeface(typeface);
            return paint.measureText(text);
        }

        @Override
        protected boolean matchesSafely(View item) {
            if (!(item instanceof TextView)) {
                return false;
            }

            final TextView textView = (TextView) item;
            final Typeface typeface = textView.getTypeface();

            assertEquals(measureText("a", typeface), measureText("bbb", typeface), 0f);

            return true;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("is a TextView and has a specific font applied");
        }
    }

    private static Matcher<View> hasA3emFont() {
        return new A3emFontMatcher();
    }
}
