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
package android.support.v7.app;

import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.ColorInt;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.ViewInteraction;
import android.support.v7.appcompat.test.R;
import android.support.v7.testutils.TestUtilsMatchers;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.SmallTest;
import android.view.View;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import org.hamcrest.Matcher;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.PositionAssertions.isBelow;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.RootMatchers.isDialog;
import static android.support.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.Is.is;

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
 *     is rendered by a single <code></code>CheckedTextView</code>.</li>
 *     <li>Testing <code>setSingleChoiceItems</code> API assumes that each item in the list
 *     is rendered by a single <code></code>CheckedTextView</code>.</li>
 * </ul>
 */
public class AlertDialogTest extends ActivityInstrumentationTestCase2<AlertDialogTestActivity> {
    private Button mButton;

    private boolean mIsCanceledCalled = false;

    private boolean mIsDismissedCalled = false;

    private int mClickedItemIndex = -1;

    private AlertDialog mAlertDialog;

    public AlertDialogTest() {
        super(AlertDialogTestActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        final AlertDialogTestActivity activity = getActivity();
        mButton = (Button) activity.findViewById(R.id.test_button);
    }

    private void wireBuilder(final AlertDialog.Builder builder) {
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAlertDialog = builder.show();
            }
        });
    }

    @SmallTest
    public void testBasicContent() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.alert_dialog_title)
                .setMessage(R.string.alert_dialog_content);
        wireBuilder(builder);

        onView(withId(R.id.test_button)).perform(click());

        // Test that we're showing a dialog with vertically stacked title and content
        onView(withText("Dialog title")).inRoot(isDialog()).check(matches(isDisplayed()));
        onView(withText("Dialog content")).inRoot(isDialog()).check(matches(isDisplayed()));
        onView(withText("Dialog content")).inRoot(isDialog()).check(
                isBelow(withText("Dialog title")));

        ListView listView = mAlertDialog.getListView();
        assertNull("No list view", listView);
    }

    @SmallTest
    public void testCancelCancelableDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.alert_dialog_title)
                .setMessage(R.string.alert_dialog_content)
                .setCancelable(true)
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        mIsCanceledCalled = true;
                    }
                });
        wireBuilder(builder);

        onView(withId(R.id.test_button)).perform(click());

        // Emulate a tap on the device BACK button
        Espresso.pressBack();

        // Since our dialog is cancelable, check that the cancel listener has been invoked
        assertTrue("Dialog is canceled", mIsCanceledCalled);
    }

    @SmallTest
    public void testCancelNonCancelableDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.alert_dialog_title)
                .setMessage(R.string.alert_dialog_content)
                .setCancelable(false)
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        mIsCanceledCalled = true;
                    }
                });
        wireBuilder(builder);

        onView(withId(R.id.test_button)).perform(click());

        // Emulate a tap on the device BACK button
        Espresso.pressBack();

        // Since our dialog is not cancelable, check that the cancel listener has not been invoked
        assertFalse("Dialog is not canceled", mIsCanceledCalled);
    }

    private void verifySimpleItemsContent(String[] expectedContent) {
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

        // Test that a click on an item invokes the registered listener
        assertEquals("Before list item click", -1, mClickedItemIndex);
        int indexToClick = expectedCount - 2;
        onData(allOf(is(instanceOf(String.class)), is(expectedContent[indexToClick]))).
                inRoot(isDialog()).perform(click());
        assertEquals("List item clicked", indexToClick, mClickedItemIndex);
    }

    @SmallTest
    public void testSimpleItemsFromRuntimeArray() {
        final String[] content = new String[] { "Alice", "Bob", "Charlie", "Delta" };
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.alert_dialog_title)
                .setItems(content,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mClickedItemIndex = which;
                            }
                        });
        wireBuilder(builder);

        verifySimpleItemsContent(content);
    }

    @SmallTest
    public void testSimpleItemsFromResourcesArray() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.alert_dialog_title)
                .setItems(R.array.alert_dialog_items,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mClickedItemIndex = which;
                            }
                        });
        wireBuilder(builder);

        verifySimpleItemsContent(
                getActivity().getResources().getStringArray(R.array.alert_dialog_items));
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

    @SmallTest
    public void testMultiChoiceItemsFromRuntimeArray() {
        final String[] content = new String[] { "Alice", "Bob", "Charlie", "Delta" };
        final boolean[] checkedTracker = new boolean[] { false, true, false, false };
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
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

    @SmallTest
    public void testMultiChoiceItemsFromResourcesArray() {
        final boolean[] checkedTracker = new boolean[] { true, false, true, false };
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
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
                getActivity().getResources().getStringArray(R.array.alert_dialog_items),
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
            int initialSelectionIndex) {
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
        assertEquals("Selected first single-choice item",
                currentlyExpectedSelectionIndex, mClickedItemIndex);
        verifySingleChoiceItemsState(expectedContent, currentlyExpectedSelectionIndex);

        // Now click the same item again and test that the selection has not changed
        onData(allOf(is(instanceOf(String.class)),
                is(expectedContent[currentlyExpectedSelectionIndex]))).
                inRoot(isDialog()).perform(click());
        assertEquals("Selected first single-choice item again",
                currentlyExpectedSelectionIndex, mClickedItemIndex);
        verifySingleChoiceItemsState(expectedContent, currentlyExpectedSelectionIndex);

        // Now we're going to click the last item and test that the click listener has been invoked
        // to update the original state array
        currentlyExpectedSelectionIndex = expectedCount - 1;
        onData(allOf(is(instanceOf(String.class)),
                is(expectedContent[currentlyExpectedSelectionIndex]))).
                inRoot(isDialog()).perform(click());
        assertEquals("Selected last single-choice item",
                currentlyExpectedSelectionIndex, mClickedItemIndex);
        verifySingleChoiceItemsState(expectedContent, currentlyExpectedSelectionIndex);
    }

    @SmallTest
    public void testSingleChoiceItemsFromRuntimeArray() {
        final String[] content = new String[] { "Alice", "Bob", "Charlie", "Delta" };
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.alert_dialog_title)
                .setSingleChoiceItems(
                        content, 2,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mClickedItemIndex = which;
                            }
                        });
        wireBuilder(builder);

        verifySingleChoiceItemsContent(content, 2);
    }

    @SmallTest
    public void testSingleChoiceItemsFromResourcesArray() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.alert_dialog_title)
                .setSingleChoiceItems(R.array.alert_dialog_items, 1,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mClickedItemIndex = which;
                            }
                        });
        wireBuilder(builder);

        verifySingleChoiceItemsContent(new String[] { "Albania", "Belize", "Chad", "Djibouti" }, 1);
    }

    @SmallTest
    public void testIconResource() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
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

    @SmallTest
    public void testIconResourceChangeAfterInitialSetup() throws Throwable {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.alert_dialog_title)
                .setMessage(R.string.alert_dialog_content)
                .setIcon(R.drawable.test_drawable_red);

        wireBuilder(builder);

        onView(withId(R.id.test_button)).perform(click());

        // Emulate background loading of the new icon
        Thread.sleep(1000);

        // Change the icon
        runTestOnUiThread(new Runnable() {
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

    @SmallTest
    public void testIconResourceChangeWithNoInitialSetup() throws Throwable {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.alert_dialog_title)
                .setMessage(R.string.alert_dialog_content);

        wireBuilder(builder);

        onView(withId(R.id.test_button)).perform(click());

        // Emulate background loading of the new icon
        Thread.sleep(1000);

        // Change the icon
        runTestOnUiThread(new Runnable() {
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

    @SmallTest
    public void testIconResourceRemoveAfterInitialSetup() throws Throwable {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.alert_dialog_title)
                .setMessage(R.string.alert_dialog_content)
                .setIcon(R.drawable.test_drawable_red);

        wireBuilder(builder);

        onView(withId(R.id.test_button)).perform(click());

        // Emulate background resetting of the icon
        Thread.sleep(1000);

        // Change the icon
        runTestOnUiThread(new Runnable() {
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

    @SmallTest
    public void testIconDrawable() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
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

    @SmallTest
    public void testIconResourceDrawableAfterInitialSetup() throws Throwable {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.alert_dialog_title)
                .setMessage(R.string.alert_dialog_content)
                .setIcon(new TestDrawable(0xFF807060, 40, 40));

        wireBuilder(builder);

        onView(withId(R.id.test_button)).perform(click());

        // Emulate background loading of the new icon
        Thread.sleep(1000);

        // Change the icon
        runTestOnUiThread(new Runnable() {
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

    @SmallTest
    public void testIconDrawableChangeWithNoInitialSetup() throws Throwable {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.alert_dialog_title)
                .setMessage(R.string.alert_dialog_content);

        wireBuilder(builder);

        onView(withId(R.id.test_button)).perform(click());

        // Emulate background loading of the new icon
        Thread.sleep(1000);

        // Change the icon
        runTestOnUiThread(new Runnable() {
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

    @SmallTest
    public void testIconDrawableRemoveAfterInitialSetup() throws Throwable {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.alert_dialog_title)
                .setMessage(R.string.alert_dialog_content)
                .setIcon(new TestDrawable(0xFF807060, 40, 40));

        wireBuilder(builder);

        onView(withId(R.id.test_button)).perform(click());

        // Emulate background resetting of the icon
        Thread.sleep(1000);

        // Change the icon
        runTestOnUiThread(new Runnable() {
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