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
import android.widget.ImageView;

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


    @SmallTest
    public void testListContent() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.alert_dialog_title)
                .setItems(new String[]{"Alice", "Bob", "Charlie", "Delta"},
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mClickedItemIndex = which;
                            }
                        });
        wireBuilder(builder);

        onView(withId(R.id.test_button)).perform(click());

        // Test that all items are showing
        onView(withText("Dialog title")).inRoot(isDialog()).check(matches(isDisplayed()));
        onData(allOf(is(instanceOf(String.class)), is("Alice"))).inRoot(isDialog()).
                check(matches(isDisplayed()));
        onData(allOf(is(instanceOf(String.class)), is("Bob"))).inRoot(isDialog()).
                check(matches(isDisplayed()));
        onData(allOf(is(instanceOf(String.class)), is("Charlie"))).inRoot(isDialog()).
                check(matches(isDisplayed()));
        onData(allOf(is(instanceOf(String.class)), is("Delta"))).inRoot(isDialog()).
                check(matches(isDisplayed()));

        // Test that a click on an item invokes the registered listener
        onData(allOf(is(instanceOf(String.class)), is("Charlie"))).inRoot(isDialog()).
                perform(click());
        assertEquals("List item clicked", 2, mClickedItemIndex);
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