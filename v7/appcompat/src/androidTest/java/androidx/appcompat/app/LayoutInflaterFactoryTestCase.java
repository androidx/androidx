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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.res.Resources;
import android.support.test.annotation.UiThreadTest;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.appcompat.custom.ContextWrapperFrameLayout;
import androidx.appcompat.test.R;
import androidx.appcompat.widget.AppCompatAutoCompleteTextView;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatMultiAutoCompleteTextView;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.appcompat.widget.AppCompatRatingBar;
import androidx.appcompat.widget.AppCompatSpinner;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class LayoutInflaterFactoryTestCase {
    @Rule
    public final ActivityTestRule<LayoutInflaterFactoryTestActivity> mActivityTestRule =
            new ActivityTestRule<>(LayoutInflaterFactoryTestActivity.class);

    @Before
    public void setup() {
        // Needed for any vector tests below
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    @UiThreadTest
    @Test
    @SmallTest
    public void testAndroidThemeInflation() {
        final LayoutInflater inflater = LayoutInflater.from(mActivityTestRule.getActivity());
        assertThemedContext(inflater.inflate(R.layout.layout_android_theme, null));
    }

    @UiThreadTest
    @Test
    @SmallTest
    public void testAppThemeInflation() {
        final LayoutInflater inflater = LayoutInflater.from(mActivityTestRule.getActivity());
        assertThemedContext(inflater.inflate(R.layout.layout_app_theme, null));
    }

    // Propagation of themed context to children only works on API 11+.
    @UiThreadTest
    @Test
    @SmallTest
    public void testAndroidThemeWithChildrenInflation() {
        LayoutInflater inflater = LayoutInflater.from(mActivityTestRule.getActivity());
        final ViewGroup root = (ViewGroup) inflater.inflate(
                R.layout.layout_android_theme_children, null);
        assertThemedContext(root);
    }

    @UiThreadTest
    @Test
    @SmallTest
    public void testThemedInflationWithUnattachedParent() {
        final Context activity = mActivityTestRule.getActivity();

        // Create a parent but not attached
        final LinearLayout parent = new LinearLayout(activity);

        // Now create a LayoutInflater with a themed context
        LayoutInflater inflater = LayoutInflater.from(activity)
                .cloneInContext(new ContextThemeWrapper(activity, R.style.MagentaThemeOverlay));

        // Now inflate a layout with children
        final ViewGroup root = (ViewGroup) inflater.inflate(
                R.layout.layout_children, parent, false);

        // And assert that the layout is themed correctly
        assertThemedContext(root);
    }

    @UiThreadTest
    @Test
    @SmallTest
    public void testSpinnerInflation() {
        verifyAppCompatWidgetInflation(R.layout.layout_spinner, AppCompatSpinner.class);
    }

    @UiThreadTest
    @Test
    @SmallTest
    public void testEditTextInflation() {
        verifyAppCompatWidgetInflation(R.layout.layout_edittext, AppCompatEditText.class);
    }

    @UiThreadTest
    @Test
    @SmallTest
    public void testButtonInflation() {
        verifyAppCompatWidgetInflation(R.layout.layout_button, AppCompatButton.class);
    }

    @UiThreadTest
    @Test
    @SmallTest
    public void testRadioButtonInflation() {
        verifyAppCompatWidgetInflation(R.layout.layout_radiobutton, AppCompatRadioButton.class);
    }

    @UiThreadTest
    @Test
    @SmallTest
    public void testRadioButtonInflationWithVectorButton() {
        verifyAppCompatWidgetInflation(R.layout.layout_radiobutton_vector,
                AppCompatRadioButton.class);
    }

    @UiThreadTest
    @Test
    @SmallTest
    public void testImageViewInflationWithVectorSrc() {
        verifyAppCompatWidgetInflation(R.layout.layout_imageview_vector,
                AppCompatImageView.class);
    }

    @UiThreadTest
    @Test
    @SmallTest
    public void testContextWrapperParentImageViewInflationWithVectorSrc() {
        verifyAppCompatWidgetInflation(R.layout.layout_contextwrapperparent_imageview_vector,
                ContextWrapperFrameLayout.class);
    }

    @UiThreadTest
    @Test
    @SmallTest
    public void testCheckBoxInflation() {
        verifyAppCompatWidgetInflation(R.layout.layout_checkbox, AppCompatCheckBox.class);
    }

    @UiThreadTest
    @Test
    @SmallTest
    public void testActvInflation() {
        verifyAppCompatWidgetInflation(R.layout.layout_actv, AppCompatAutoCompleteTextView.class);
    }

    @UiThreadTest
    @Test
    @SmallTest
    public void testMactvInflation() {
        verifyAppCompatWidgetInflation(R.layout.layout_mactv,
                AppCompatMultiAutoCompleteTextView.class);
    }

    @UiThreadTest
    @Test
    @SmallTest
    public void testRatingBarInflation() {
        verifyAppCompatWidgetInflation(R.layout.layout_ratingbar, AppCompatRatingBar.class);
    }

    @UiThreadTest
    @Test
    @SmallTest
    public void testDeclarativeOnClickWithContextWrapper() {
        LayoutInflater inflater = LayoutInflater.from(mActivityTestRule.getActivity());
        View view = inflater.inflate(R.layout.layout_button_themed_onclick, null);

        assertTrue(view.performClick());
        assertTrue(mActivityTestRule.getActivity().wasDeclarativeOnClickCalled());
    }

    private void verifyAppCompatWidgetInflation(final int layout, final Class<?> expectedClass) {
        LayoutInflater inflater = LayoutInflater.from(mActivityTestRule.getActivity());
        View view = inflater.inflate(layout, null);
        assertSame("View is " + expectedClass.getSimpleName(), expectedClass,
                view.getClass());
    }

    private static void assertThemedContext(final View view) {
        final Context viewContext = view.getContext();
        final int expectedColor = view.getResources().getColor(R.color.test_magenta);

        final TypedValue colorAccentValue = getColorAccentValue(viewContext.getTheme());
        assertTrue(colorAccentValue.type >= TypedValue.TYPE_FIRST_COLOR_INT
                && colorAccentValue.type <= TypedValue.TYPE_LAST_COLOR_INT);
        assertEquals("View does not have ContextThemeWrapper context",
                expectedColor, colorAccentValue.data);

        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                final View child = vg.getChildAt(i);
                assertThemedContext(child);
            }
        }
    }

    private static TypedValue getColorAccentValue(final Resources.Theme theme) {
        final TypedValue typedValue = new TypedValue();
        theme.resolveAttribute(R.attr.colorAccent, typedValue, true);
        return typedValue;
    }
}
