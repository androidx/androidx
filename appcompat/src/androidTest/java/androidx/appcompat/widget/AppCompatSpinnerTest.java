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
package androidx.appcompat.widget;

import static androidx.appcompat.testutils.TestUtilsMatchers.hasChild;
import static androidx.appcompat.testutils.TestUtilsMatchers.isCombinedBackground;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isPlatformPopup;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.os.SystemClock;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.IdRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.test.R;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.action.CoordinatesProvider;
import androidx.test.espresso.action.GeneralSwipeAction;
import androidx.test.espresso.action.Press;
import androidx.test.espresso.action.Swipe;
import androidx.test.filters.LargeTest;
import androidx.test.filters.MediumTest;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Test;

/**
 * In addition to all tinting-related tests done by the base class, this class provides
 * tests specific to {@link AppCompatSpinner} class.
 */
@LargeTest
public class AppCompatSpinnerTest
        extends AppCompatBaseViewTest<AppCompatSpinnerActivity, AppCompatSpinner> {
    private static final String EARTH = "Earth";

    public AppCompatSpinnerTest() {
        super(AppCompatSpinnerActivity.class);
    }

    @Override
    protected boolean hasBackgroundByDefault() {
        // Spinner has default background set on it
        return true;
    }

    @Override
    public void setUp() {
        super.setUp();
        if (mActivity.getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            SystemClock.sleep(250);
        }
    }

    @After
    public void cleanUp() {
        if (mActivity.getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            SystemClock.sleep(250);
        }
    }

    /**
     * Helper method that verifies that the popup for the specified {@link AppCompatSpinner}
     * is themed with the specified color.
     */
    private void verifySpinnerPopupTheming(@IdRes int spinnerId,
            @ColorRes int expectedPopupColorResId, boolean matchDropDownListView) {
        final Resources res = mActivityTestRule.getActivity().getResources();
        final @ColorInt int expectedPopupColor =
                ResourcesCompat.getColor(res, expectedPopupColorResId, null);
        final AppCompatSpinner spinner = (AppCompatSpinner) mContainer.findViewById(spinnerId);

        // Click the spinner to show its popup content
        onView(withId(spinnerId)).perform(click());

        // The internal implementation details of the AppCompatSpinner's popup content depends
        // on the platform version itself (in android.widget.PopupWindow) as well as on when the
        // popup theme is being applied first (in XML or at runtime). Instead of trying to catch
        // all possible variations of how the popup content is wrapped, we use a view matcher that
        // creates a single bitmap that combines backgrounds starting from the parent of the
        // popup content items upwards (drawing them in reverse order), and then tests that the
        // combined bitmap matches the expected color fill. This should remove dependency on the
        // internal implementation details on which exact "chrome" part of the popup has the
        // matching background.
        String itemText = (String) spinner.getAdapter().getItem(2);
        Matcher popupContentMatcher = hasChild(withText(itemText));
        // Note that we are only testing the center pixel of the combined popup background. This
        // is to "eliminate" otherwise hacky code that would need to skip over rounded corners and
        // drop shadow of the combined visual appearance of a popup.
        onView(popupContentMatcher).inRoot(isPlatformPopup()).check(
                matches(isCombinedBackground(expectedPopupColor, true)));

        // Click an entry in the popup to dismiss it
        onView(withText(itemText)).perform(click());
    }

    @LargeTest
    @Test
    public void testPopupThemingFromXmlAttribute() {
        verifySpinnerPopupTheming(R.id.view_magenta_themed_popup, R.color.test_magenta, true);
    }

    @LargeTest
    @Test
    public void testUnthemedPopupRuntimeTheming() {
        final AppCompatSpinner spinner =
                (AppCompatSpinner) mContainer.findViewById(R.id.view_unthemed_popup);
        spinner.setPopupBackgroundResource(R.drawable.test_background_blue);
        verifySpinnerPopupTheming(R.id.view_unthemed_popup, R.color.test_blue, false);

        // Set a different popup background
        spinner.setPopupBackgroundDrawable(ContextCompat.getDrawable(
                mActivityTestRule.getActivity(), R.drawable.test_background_green));
        verifySpinnerPopupTheming(R.id.view_unthemed_popup, R.color.test_green, false);
    }

    @LargeTest
    @Test
    public void testThemedPopupRuntimeTheming() {
        final AppCompatSpinner spinner =
                (AppCompatSpinner) mContainer.findViewById(R.id.view_ocean_themed_popup);
        verifySpinnerPopupTheming(R.id.view_ocean_themed_popup, R.color.ocean_default, true);

        // Now set a different popup background
        spinner.setPopupBackgroundResource(R.drawable.test_background_red);
        verifySpinnerPopupTheming(R.id.view_ocean_themed_popup, R.color.test_red, false);

        // Set a different popup background
        spinner.setPopupBackgroundDrawable(ContextCompat.getDrawable(
                mActivityTestRule.getActivity(), R.drawable.test_background_blue));
        verifySpinnerPopupTheming(R.id.view_ocean_themed_popup, R.color.test_blue, false);
    }

    @MediumTest
    @Test
    public void testHasAppCompatDialogMode() {
        final AppCompatSpinner spinner = mContainer.findViewById(R.id.spinner_dialog_popup);
        final AppCompatSpinner.SpinnerPopup popup = spinner.getInternalPopup();
        assertNotNull(popup);
        assertThat(popup, instanceOf(AppCompatSpinner.DialogPopup.class));

        onView(withId(R.id.spinner_dialog_popup)).perform(click());

        final AppCompatSpinner.DialogPopup dialogPopup = (AppCompatSpinner.DialogPopup) popup;
        assertThat(dialogPopup.mPopup, instanceOf(AlertDialog.class));
    }

    @LargeTest
    @Test
    public void testChangeOrientationDialogPopupPersists() {
        verifyChangeOrientationPopupPersists(R.id.spinner_dialog_popup);
    }

    @LargeTest
    @Test
    public void testChangeOrientationDropdownPopupPersists() {
        verifyChangeOrientationPopupPersists(R.id.spinner_dropdown_popup);
    }

    private void verifyChangeOrientationPopupPersists(@IdRes int spinnerId) {
        onView(withId(spinnerId)).perform(click());
        mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        onView(withText(EARTH)).check(matches(isDisplayed()));
    }

    @LargeTest
    @Test
    public void testSlowScroll() {
        onView(withId(R.id.spinner_dropdown_popup_with_scroll)).perform(click());

        final AppCompatSpinner spinner = mContainer
                .findViewById(R.id.spinner_dropdown_popup_with_scroll);
        String secondItem = (String) spinner.getAdapter().getItem(1);

        onView(isAssignableFrom(DropDownListView.class)).perform(slowScrollPopup());

        // when we scroll slowly a second time the popup list might jump back to the first element
        onView(isAssignableFrom(DropDownListView.class)).perform(slowScrollPopup());

        // because we scroll twice with one element height each,
        // the second item should not be visible
        onView(withText(secondItem))
                .check(doesNotExist());
    }

    private ViewAction slowScrollPopup() {
        return new GeneralSwipeAction(Swipe.SLOW,
                new CoordinatesProvider() {
                    @Override
                    public float[] calculateCoordinates(View view) {
                        final float[] middleLocation = getViewMiddleLocation(view);
                        return new float[] {
                                middleLocation[0],
                                middleLocation[1]
                        };
                    }
                },
                new CoordinatesProvider() {
                    @Override
                    public float[] calculateCoordinates(View view) {
                        final float[] middleLocation = getViewMiddleLocation(view);
                        return new float[] {
                                middleLocation[0],
                                middleLocation[1] - getElementSize(view)
                        };
                    }
                },
                Press.PINPOINT
        );
    }

    private float[] getViewMiddleLocation(View view) {
        final DropDownListView list = (DropDownListView) view;

        final int[] location = new int[2];
        list.getLocationOnScreen(location);

        final float x = location[0] + list.getWidth() / 2f;
        final float y = location[1] + list.getHeight() / 2f;

        return new float[] {x, y};
    }

    private int getElementSize(View view) {
        final DropDownListView list = (DropDownListView) view;

        final View child = list.getChildAt(0);
        final int[] location = new int[2];
        child.getLocationOnScreen(location);

        // espresso doesn't actually scroll for the full amount specified
        // so we add a little bit more to be safe
        return child.getHeight() * 2;
    }
}
