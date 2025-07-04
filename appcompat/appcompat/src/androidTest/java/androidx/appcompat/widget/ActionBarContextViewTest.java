/*
 * Copyright 2025 The Android Open Source Project
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

import static android.view.View.LAYOUT_DIRECTION_LTR;
import static android.view.View.LAYOUT_DIRECTION_RTL;

import static org.junit.Assert.assertEquals;

import android.view.View;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ActionBarContextViewTest {

    @Rule
    public final ActivityScenarioRule<ActionBarContextViewActivity> mActivityTestRule =
            new ActivityScenarioRule<>(ActionBarContextViewActivity.class);

    @Test
    public void testOnConfigurationChanged_contentHeight() {
        mActivityTestRule.getScenario().onActivity(activity -> {
            final ActionBarContextView view = new ActionBarContextView(activity);

            // The value is specified in actionModeStyle
            assertEquals(200, view.getContentHeight());

            view.dispatchConfigurationChanged(view.getResources().getConfiguration());

            // The value must be the same after calling onConfigurationChanged
            assertEquals(200, view.getContentHeight());
        });
    }

    @Test
    public void testOnMeasure_setPaddingForInsets() {
        mActivityTestRule.getScenario().onActivity(activity -> {
            final ActionBarContextView view = new ActionBarContextView(activity);

            // The values are specified in actionModeStyle
            assertEquals(10, view.getPaddingLeft());
            assertEquals(20, view.getPaddingTop());
            assertEquals(30, view.getPaddingRight());
            assertEquals(40, view.getPaddingBottom());

            view.setPaddingForInsets(1, 2, 3, 4);

            view.measure(
                    View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(2000, View.MeasureSpec.AT_MOST));

            // 206 = 200 (content height) + 2 (insets padding top) + 4 (insets padding bottom)
            assertEquals(206, view.getMeasuredHeight());
        });
    }

    @Test
    public void testSetPadding() {
        mActivityTestRule.getScenario().onActivity(activity -> {
            final ActionBarContextView view = new ActionBarContextView(activity);

            // The values are specified in actionModeStyle
            assertEquals(10, view.getPaddingLeft());
            assertEquals(20, view.getPaddingTop());
            assertEquals(30, view.getPaddingRight());
            assertEquals(40, view.getPaddingBottom());

            view.setPaddingForInsets(1, 2, 3, 4);

            // setPaddingForInsets must not overwrite the padding specified in actionModeStyle.
            assertEquals(11, view.getPaddingLeft());
            assertEquals(22, view.getPaddingTop());
            assertEquals(33, view.getPaddingRight());
            assertEquals(44, view.getPaddingBottom());

            view.setPadding(50, 60, 70, 80);

            // setPadding must not overwrite the padding set by setPaddingForInsets.
            assertEquals(51, view.getPaddingLeft());
            assertEquals(62, view.getPaddingTop());
            assertEquals(73, view.getPaddingRight());
            assertEquals(84, view.getPaddingBottom());

            view.setPaddingForInsets(5, 6, 7, 8);

            // setPaddingForInsets must not overwrite the padding set by setPadding.
            assertEquals(55, view.getPaddingLeft());
            assertEquals(66, view.getPaddingTop());
            assertEquals(77, view.getPaddingRight());
            assertEquals(88, view.getPaddingBottom());
        });
    }

    @Test
    public void setPaddingRelative_ltr() {
        mActivityTestRule.getScenario().onActivity(activity -> {
            final ActionBarContextView view = new ActionBarContextView(activity);
            view.setLayoutDirection(LAYOUT_DIRECTION_LTR);

            // The values are specified in actionModeStyle
            assertEquals(10, view.getPaddingLeft());
            assertEquals(20, view.getPaddingTop());
            assertEquals(30, view.getPaddingRight());
            assertEquals(40, view.getPaddingBottom());

            view.setPaddingForInsets(1, 2, 3, 4);

            // setPaddingForInsets must not overwrite the padding specified in actionModeStyle.
            assertEquals(11, view.getPaddingLeft());
            assertEquals(22, view.getPaddingTop());
            assertEquals(33, view.getPaddingRight());
            assertEquals(44, view.getPaddingBottom());

            view.setPaddingRelative(50, 60, 70, 80);

            // setPaddingRelative must not overwrite the padding set by setPaddingForInsets.
            assertEquals(51, view.getPaddingLeft());
            assertEquals(62, view.getPaddingTop());
            assertEquals(73, view.getPaddingRight());
            assertEquals(84, view.getPaddingBottom());

            view.setPaddingForInsets(5, 6, 7, 8);

            // setPaddingForInsets must not overwrite the padding set by setPaddingRelative.
            assertEquals(55, view.getPaddingLeft());
            assertEquals(66, view.getPaddingTop());
            assertEquals(77, view.getPaddingRight());
            assertEquals(88, view.getPaddingBottom());
        });
    }

    @Test
    public void setPaddingRelative_rtl() {
        mActivityTestRule.getScenario().onActivity(activity -> {
            final ActionBarContextView view = new ActionBarContextView(activity);
            view.setLayoutDirection(LAYOUT_DIRECTION_RTL);

            // The values are specified in actionModeStyle
            assertEquals(10, view.getPaddingLeft());
            assertEquals(20, view.getPaddingTop());
            assertEquals(30, view.getPaddingRight());
            assertEquals(40, view.getPaddingBottom());

            view.setPaddingForInsets(1, 2, 3, 4);

            // setPaddingForInsets must not overwrite the padding specified in actionModeStyle.
            assertEquals(11, view.getPaddingLeft());
            assertEquals(22, view.getPaddingTop());
            assertEquals(33, view.getPaddingRight());
            assertEquals(44, view.getPaddingBottom());

            view.setPaddingRelative(50, 60, 70, 80);

            // setPaddingRelative must not overwrite the padding set by setPaddingForInsets.
            assertEquals(71, view.getPaddingLeft());
            assertEquals(62, view.getPaddingTop());
            assertEquals(53, view.getPaddingRight());
            assertEquals(84, view.getPaddingBottom());

            view.setPaddingForInsets(5, 6, 7, 8);

            // setPaddingForInsets must not overwrite the padding set by setPaddingRelative.
            assertEquals(75, view.getPaddingLeft());
            assertEquals(66, view.getPaddingTop());
            assertEquals(57, view.getPaddingRight());
            assertEquals(88, view.getPaddingBottom());
        });
    }
}
