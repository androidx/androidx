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

package androidx.textclassifier.widget;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.actionWithAssertions;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.textclassifier.widget.FloatingToolbarEspressoUtils.onFloatingToolbar;
import static androidx.textclassifier.widget.FloatingToolbarEspressoUtils.onFloatingToolbarItem;
import static androidx.textclassifier.widget.FloatingToolbarEspressoUtils.onFloatingToolbarMainPanel;
import static androidx.textclassifier.widget.FloatingToolbarEspressoUtils.onFloatingToolbarOverflowButton;
import static androidx.textclassifier.widget.FloatingToolbarEspressoUtils.onFloatingToolbarOverflowPanel;

import static com.google.common.truth.Truth.assertThat;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.widget.PopupWindow.OnDismissListener;

import androidx.appcompat.view.menu.MenuBuilder;
import androidx.core.internal.view.SupportMenu;
import androidx.core.internal.view.SupportMenuItem;
import androidx.test.annotation.UiThreadTest;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.ViewAssertion;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.action.CoordinatesProvider;
import androidx.test.espresso.action.GeneralClickAction;
import androidx.test.espresso.action.Press;
import androidx.test.espresso.action.Tap;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.rule.ActivityTestRule;
import androidx.textclassifier.R;
import androidx.textclassifier.TestUtils;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Tests for {@link FloatingToolbar}.
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.M)
public final class FloatingToolbarTest {

    @Rule
    public ActivityTestRule<? extends FloatingToolbarActivity> mActivityTestRule =
            new ActivityTestRule<>(FloatingToolbarActivity.class);

    private FloatingToolbar mFloatingToolbar;
    private Context mContext;
    private View mWidget;

    @Before
    public void setUp() throws Throwable {
        final Activity activity = mActivityTestRule.getActivity();
        TestUtils.keepScreenOn(mActivityTestRule, activity);
        mContext = activity;
        final SupportMenu menu = new MenuBuilder(mContext);
        menu.add("One");
        menu.add("Two");
        menu.add("Three");

        mWidget = activity.findViewById(androidx.textclassifier.test.R.id.textview);
        mFloatingToolbar = new FloatingToolbar(mWidget);
        mFloatingToolbar.setMenu(menu);
    }

    @Test
    @UiThreadTest
    public void setMenu() throws Exception {
        final SupportMenu menu = new MenuBuilder(mContext);
        menu.add("Mine");
        mFloatingToolbar.setMenu(menu);
        assertThat(mFloatingToolbar.getMenu()).isEqualTo(menu);
    }

    @Test
    @UiThreadTest
    public void show() throws Exception {
        assertFloatingToolbarIsDismissed();
        mFloatingToolbar.show();
        assertFloatingToolbarIsShowing();
        mFloatingToolbar.show();
        assertFloatingToolbarIsShowing();
    }

    @Test
    @UiThreadTest
    public void show_noItems() throws Exception {
        mFloatingToolbar.setMenu(new MenuBuilder(mContext));
        mFloatingToolbar.show();
        assertFloatingToolbarIsDismissed();
    }

    @Test
    @UiThreadTest
    public void show_noVisibleItems() throws Exception {
        final SupportMenu menu = new MenuBuilder(mContext);
        final MenuItem item = menu.add("Mine");
        mFloatingToolbar.setMenu(menu);
        mFloatingToolbar.show();
        assertFloatingToolbarIsShowing();

        item.setVisible(false);
        mFloatingToolbar.updateLayout();
        assertFloatingToolbarIsDismissed();
    }

    @Test
    @UiThreadTest
    public void dismiss() throws Exception {
        mFloatingToolbar.show();
        assertFloatingToolbarIsShowing();
        mFloatingToolbar.dismiss();
        assertFloatingToolbarIsDismissed();
        mFloatingToolbar.dismiss();
        assertFloatingToolbarIsDismissed();
    }

    @Test
    @UiThreadTest
    public void hide() throws Exception {
        mFloatingToolbar.show();
        assertFloatingToolbarIsShowing();
        mFloatingToolbar.hide();
        assertFloatingToolbarIsHidden();
        mFloatingToolbar.hide();
        assertFloatingToolbarIsHidden();
    }

    @Test
    public void menuItemsOrder() throws Exception {
        final int groupId = Menu.NONE;
        final int id = Menu.NONE;
        final SupportMenu menu = new MenuBuilder(mContext);
        final MenuItem item1 = menu.add("1");
        final MenuItem item2 = menu.add("2");
        final MenuItem item3 = menu.add(groupId, id, 2 /* order */, "3");
        item3.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        final MenuItem item4 = menu.add(groupId, id, 1 /* order */, "4");
        item4.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        final MenuItem item5 = menu.add(
                groupId, FloatingToolbar.MENU_ID_SMART_ACTION, 9 /* order */, "5");

        final List<SupportMenuItem> items = new ArrayList<>();
        for (int i = 0; i < menu.size(); i++) {
            items.add((SupportMenuItem) menu.getItem(i));
        }
        Collections.sort(items, mFloatingToolbar.mMenuItemComparator);

        assertThat(items).containsExactly(item5, item4, item3, item1, item2).inOrder();
    }

    @Test
    public void ui_showFloatingToolbar() throws Exception {
        onWidget().perform(showFloatingToolbar());
        onFloatingToolbar().check(matches(isDisplayed()));
        assertFloatingToolbarIsShowing();
    }

    @Ignore // b/188568469
    @Test
    public void ui_itemClick() throws Exception {
        final SupportMenu menu = new MenuBuilder(mContext);
        final MenuItem menuItem = menu.add("Mine");
        mFloatingToolbar.setMenu(menu);
        final OnMenuItemClickListener onClickListener = mock(OnMenuItemClickListener.class);
        mFloatingToolbar.setOnMenuItemClickListener(onClickListener);

        onWidget().perform(showFloatingToolbar());
        onFloatingToolbarItem(menuItem.getTitle()).perform(click());

        verify(onClickListener).onMenuItemClick(menuItem);
        onFloatingToolbar().check(matches(isDisplayed()));
    }

    @Ignore // b/188568469
    @Test
    public void ui_dismissOnItemClick() throws Exception {
        final SupportMenu menu = new MenuBuilder(mContext);
        final MenuItem mainItem = menu.add("Main");
        mainItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        final MenuItem overflowItem = menu.add("Overflow");
        overflowItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        mFloatingToolbar.setMenu(menu);
        mFloatingToolbar.setDismissOnMenuItemClick(true);

        onWidget().perform(showFloatingToolbar());
        onFloatingToolbarItem(mainItem.getTitle()).perform(click());
        assertFloatingToolbarIsDismissed();

        onWidget().perform(showFloatingToolbar());
        onFloatingToolbarOverflowButton().perform(click());
        onFloatingToolbarItem(overflowItem.getTitle()).perform(click());
        assertFloatingToolbarIsDismissed();
    }

    @Ignore // b/188568469
    @Test
    @SuppressWarnings("deprecation") /* defaultDisplay */
    public void ui_dismissOnOutsideClick() throws Exception {
        final int toolbarMargin = (int) (mWidget.getY() + 1.5 * mWidget.getResources()
                .getDimensionPixelSize(R.dimen.floating_toolbar_height));
        final DisplayMetrics metrics = new DisplayMetrics();
        mActivityTestRule.getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        final Rect rect = new Rect(0, toolbarMargin, metrics.widthPixels, metrics.heightPixels);

        mFloatingToolbar.setContentRect(rect);
        onWidget().perform(showFloatingToolbar());
        onWidget().perform(clickAt(rect.centerX(), rect.top + 50));
        assertFloatingToolbarIsDismissed();
    }

    @Test
    public void ui_onDismissListener() throws Exception {
        final OnDismissListener onDismissListener = mock(OnDismissListener.class);
        mFloatingToolbar.setOnDismissListener(onDismissListener);
        onWidget().perform(showFloatingToolbar());
        onWidget().perform(dismissFloatingToolbar());
        verify(onDismissListener).onDismiss();
    }

    @Test
    public void ui_setContentRect() throws Exception {
        final int right = ((int) mWidget.getX() + mWidget.getWidth());
        final int bottom = ((int) mWidget.getY() + mWidget.getHeight() / 2);
        final Rect rect = new Rect(0, 0, right, bottom);
        onWidget().perform(showFloatingToolbar());
        onFloatingToolbar().check(obstructs(rect));

        mFloatingToolbar.setContentRect(rect);
        onWidget().perform(updateFloatingToolbar());

        onFloatingToolbar().check(doesNotObstruct(rect));
    }

    @Test
    @SuppressWarnings("deprecation") /* defaultDisplay */
    public void ui_setContentRect_belowToolbar() throws Exception {
        final int toolbarMargin = 5 * mWidget.getResources()
                .getDimensionPixelSize(R.dimen.floating_toolbar_height);
        final DisplayMetrics metrics = new DisplayMetrics();
        mActivityTestRule.getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        final Rect rect = new Rect(0, toolbarMargin, metrics.widthPixels, metrics.heightPixels);
        mFloatingToolbar.setContentRect(rect);

        onWidget().perform(showFloatingToolbar());
        onFloatingToolbar().check(doesNotObstruct(rect));

        onFloatingToolbarOverflowButton().perform(click());
        onFloatingToolbar().check(doesNotObstruct(rect));
    }

    @Test
    public void ui_setSuggestedWidth() throws Exception {
        final SupportMenu menu = new MenuBuilder(mContext);
        menu.add("First").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add("Two").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add("Three").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add("Four").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add("Five").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        mFloatingToolbar.setMenu(menu);
        final int width = 500;
        onWidget().perform(showFloatingToolbar());
        onFloatingToolbar().check(widthNotWithinRange(0, width));

        mFloatingToolbar.setSuggestedWidth(width);
        onWidget().perform(updateFloatingToolbar());

        onFloatingToolbar().check(widthIsWithinRange(0, width));
    }

    @Test
    public void ui_onOrientationChange() throws Exception {
        onWidget().perform(showFloatingToolbar());
        onFloatingToolbar().check(matches(isDisplayed()));
        onWidget().perform(rotateScreen());
        onFloatingToolbar().check(matches(isDisplayed()));
    }

    @Ignore // b/188568469
    @Test
    public void ui_toggleOverflow() throws Exception {
        onWidget().perform(showFloatingToolbar());
        onFloatingToolbarMainPanel().check(matches(isDisplayed()));
        onFloatingToolbarOverflowPanel().check(matches(not(isDisplayed())));

        onFloatingToolbarOverflowButton().perform(click());
        onFloatingToolbarMainPanel().check(matches(not(isDisplayed())));
        onFloatingToolbarOverflowPanel().check(matches(isDisplayed()));

        onFloatingToolbarOverflowButton().perform(click());
        onFloatingToolbarMainPanel().check(matches(isDisplayed()));
        onFloatingToolbarOverflowPanel().check(matches(not(isDisplayed())));
    }

    @Ignore // b/188568469
    @Test
    public void ui_itemPanel() throws Exception {
        final SupportMenu menu = new MenuBuilder(mContext);
        menu.add("1");
        menu.add("2").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add("3").setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(Menu.NONE, FloatingToolbar.MENU_ID_SMART_ACTION, 9 /* order */, "4");
        mFloatingToolbar.setMenu(menu);

        onWidget().perform(showFloatingToolbar());
        onFloatingToolbarItem("2").check(matches(isDisplayed()));
        onFloatingToolbarItem("4").check(matches(isDisplayed()));
        onFloatingToolbarItem("1").check(matches(not(isDisplayed())));
        onFloatingToolbarItem("3").check(matches(not(isDisplayed())));

        onFloatingToolbarOverflowButton().perform(click());
        onFloatingToolbarItem("1").check(matches(isDisplayed()));
        onFloatingToolbarItem("3").check(matches(isDisplayed()));
        onFloatingToolbarItem("2").check(matches(not(isDisplayed())));
        onFloatingToolbarItem("4").check(matches(not(isDisplayed())));
    }

    @Test
    public void ui_updateMenu() throws Exception {
        final SupportMenu menu = new MenuBuilder(mContext);
        menu.add("1").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add("2").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add("3").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        mFloatingToolbar.setMenu(menu);

        onWidget().perform(showFloatingToolbar());
        onFloatingToolbarItem("1").check(matches(isDisplayed()));
        onFloatingToolbarItem("2").check(matches(isDisplayed()));
        onFloatingToolbarItem("3").check(matches(isDisplayed()));

        menu.clear();
        menu.add("A").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add("B").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add("C").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        mFloatingToolbar.setMenu(menu);

        onWidget().perform(updateFloatingToolbar());
        onFloatingToolbarItem("A").check(matches(isDisplayed()));
        onFloatingToolbarItem("B").check(matches(isDisplayed()));
        onFloatingToolbarItem("C").check(matches(isDisplayed()));
    }

    @Ignore // b/188568469
    @Test
    public void ui_horizontalAlignment() throws Exception {
        enableLtRMode();
        onWidget().perform(showFloatingToolbar());
        final int right = mFloatingToolbar.getToolbarContainerBoundsForTesting().right;
        onFloatingToolbarOverflowButton().perform(click());
        final int overflowRight = mFloatingToolbar.getToolbarContainerBoundsForTesting().right;
        assertThat(right).isEqualTo(overflowRight);

        onWidget().perform(dismissFloatingToolbar());

        enableRtlMode();
        onWidget().perform(showFloatingToolbar());
        final int left = mFloatingToolbar.getToolbarContainerBoundsForTesting().left;
        onFloatingToolbarOverflowButton().perform(click());
        final int overflowLeft = mFloatingToolbar.getToolbarContainerBoundsForTesting().left;
        assertThat(left).isEqualTo(overflowLeft);
    }

    private void enableRtlMode() {
        final Context context = mWidget.getContext();
        context.getApplicationInfo().flags |= ApplicationInfo.FLAG_SUPPORTS_RTL;
        context.getResources().getConfiguration().setLayoutDirection(Locale.forLanguageTag("ar"));
    }

    private void enableLtRMode() {
        final Context context = mWidget.getContext();
        context.getResources().getConfiguration().setLayoutDirection(Locale.US);
    }

    private void assertFloatingToolbarIsShowing() throws Exception {
        assertThat(mFloatingToolbar.isShowing()).isTrue();
        assertThat(mFloatingToolbar.isHidden()).isFalse();
    }

    private void assertFloatingToolbarIsHidden() throws Exception {
        assertThat(mFloatingToolbar.isShowing()).isFalse();
        assertThat(mFloatingToolbar.isHidden()).isTrue();
    }

    private void assertFloatingToolbarIsDismissed() throws Exception {
        assertThat(mFloatingToolbar.isShowing()).isFalse();
        assertThat(mFloatingToolbar.isHidden()).isFalse();
    }

    private ViewInteraction onWidget() {
        return onView(withId(mWidget.getId()));
    }

    private ViewAction showFloatingToolbar() {
        return widgetFloatingToolbarAction(
                new Runnable() {
                    @Override
                    public void run() {
                        mFloatingToolbar.show();
                    }
                }, "Show floating toolbar");
    }

    private ViewAction dismissFloatingToolbar() {
        return widgetFloatingToolbarAction(
                new Runnable() {
                    @Override
                    public void run() {
                        mFloatingToolbar.dismiss();
                    }
                }, "Dismiss floating toolbar");
    }

    private ViewAction updateFloatingToolbar() {
        return widgetFloatingToolbarAction(
                new Runnable() {
                    @Override
                    public void run() {
                        mFloatingToolbar.updateLayout();
                    }
                }, "Update floating toolbar");
    }

    private ViewAction widgetFloatingToolbarAction(
            final Runnable toolbarMethod,
            final String methodDescription) {
        return actionWithAssertions(
                new ViewAction() {
                    @Override
                    public Matcher<View> getConstraints() {
                        return is(mWidget);
                    }

                    @Override
                    public String getDescription() {
                        return methodDescription;
                    }

                    @Override
                    public void perform(UiController uiController, View view) {
                        toolbarMethod.run();
                        uiController.loopMainThreadForAtLeast(400);
                    }
                });
    }

    private ViewAction rotateScreen() {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return is(mWidget);
            }

            @Override
            public String getDescription() {
                return "Rotate screen";
            }

            @Override
            public void perform(UiController uiController, View view) {
                final Activity activity = mActivityTestRule.getActivity();
                switch(view.getResources().getConfiguration().orientation) {
                    case Configuration.ORIENTATION_PORTRAIT:
                        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                        break;
                    case Configuration.ORIENTATION_LANDSCAPE:
                        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                        break;
                }
                uiController.loopMainThreadForAtLeast(1000);
            }
        };
    }

    private ViewAction clickAt(final int x, final int y) {
        final CoordinatesProvider coords = new CoordinatesProvider() {
            @Override
            public float[] calculateCoordinates(View view) {
                return new float[]{x, y};
            }
        };
        return new GeneralClickAction(Tap.SINGLE, coords, Press.FINGER, 0, 0);
    }


    private static ViewAssertion doesNotObstruct(final Rect rect) {
        return assertObstructs(rect, false);
    }

    private static ViewAssertion obstructs(final Rect rect) {
        return assertObstructs(rect, true);
    }

    private static ViewAssertion assertObstructs(final Rect rect, final boolean obstructs) {
        return new ViewAssertion() {
            @Override
            public void check(View view, NoMatchingViewException e) {
                final int[] leftTop = new int[2];
                view.getLocationOnScreen(leftTop);
                final int left = leftTop[0];
                final int top = leftTop[1];
                final int right =  left + view.getWidth();
                final int bottom = top + view.getHeight();
                assertThat(rect.intersect(left, top, right, bottom)).isEqualTo(obstructs);
            }
        };
    }

    private static ViewAssertion widthIsWithinRange(final int min, final int max) {
        return assertWidthRange(min, max, true);
    }

    private static ViewAssertion widthNotWithinRange(final int min, final int max) {
        return assertWidthRange(min, max, false);
    }

    private static ViewAssertion assertWidthRange(
            final int min, final int max, final boolean within) {
        return new ViewAssertion() {
            @Override
            public void check(View view, NoMatchingViewException e) {
                final boolean minAssertion = view.getWidth() >= min;
                final boolean maxAssertion = view.getWidth() <= max;
                assertThat(minAssertion && maxAssertion).isEqualTo(within);
            }
        };
    }
}
