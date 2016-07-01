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

import org.junit.Before;
import org.junit.Test;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.support.v7.appcompat.test.R;
import android.support.v7.custom.ContextWrapperFrameLayout;
import android.support.v7.widget.AppCompatAutoCompleteTextView;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatMultiAutoCompleteTextView;
import android.support.v7.widget.AppCompatRadioButton;
import android.support.v7.widget.AppCompatRatingBar;
import android.support.v7.widget.AppCompatSpinner;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class LayoutInflaterFactoryTestCase
        extends BaseInstrumentationTestCase<LayoutInflaterFactoryTestActivity> {

    public LayoutInflaterFactoryTestCase() {
        super(LayoutInflaterFactoryTestActivity.class);
    }

    @Before
    public void setup() {
        // Needed for any vector tests below
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    @Test
    @SmallTest
    public void testAndroidThemeInflation() throws Throwable {
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                final LayoutInflater inflater = LayoutInflater.from(getActivity());
                assertThemedContext(inflater.inflate(R.layout.layout_android_theme, null));
            }
        });
    }

    @Test
    @SmallTest
    public void testAppThemeInflation() throws Throwable {
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                final LayoutInflater inflater = LayoutInflater.from(getActivity());
                assertThemedContext(inflater.inflate(R.layout.layout_app_theme, null));
            }
        });
    }

    @Test
    @SmallTest
    public void testAndroidThemeWithChildrenInflation() throws Throwable {
        if (Build.VERSION.SDK_INT < 11) {
            // Propagation of themed context to children only works on API 11+. Ignoring test.
            return;
        }
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                LayoutInflater inflater = LayoutInflater.from(getActivity());
                final ViewGroup root = (ViewGroup) inflater.inflate(
                        R.layout.layout_android_theme_children, null);

                assertThemedContext(root);

                for (int i = 0; i < root.getChildCount(); i++) {
                    final View child = root.getChildAt(i);
                    assertThemedContext(child);
                }
            }
        });
    }

    @Test
    @SmallTest
    public void testSpinnerInflation() throws Throwable {
        testAppCompatWidgetInflation(R.layout.layout_spinner, AppCompatSpinner.class);
    }

    @Test
    @SmallTest
    public void testEditTextInflation() throws Throwable {
        testAppCompatWidgetInflation(R.layout.layout_edittext, AppCompatEditText.class);
    }

    @Test
    @SmallTest
    public void testButtonInflation() throws Throwable {
        testAppCompatWidgetInflation(R.layout.layout_button, AppCompatButton.class);
    }

    @Test
    @SmallTest
    public void testRadioButtonInflation() throws Throwable {
        testAppCompatWidgetInflation(R.layout.layout_radiobutton, AppCompatRadioButton.class);
    }

    @Test
    @SmallTest
    public void testRadioButtonInflationWithVectorButton() throws Throwable {
        testAppCompatWidgetInflation(R.layout.layout_radiobutton_vector,
                AppCompatRadioButton.class);
    }

    @Test
    @SmallTest
    public void testImageViewInflationWithVectorSrc() throws Throwable {
        testAppCompatWidgetInflation(R.layout.layout_imageview_vector,
                AppCompatImageView.class);
    }

    @Test
    @SmallTest
    public void testContextWrapperParentImageViewInflationWithVectorSrc() throws Throwable {
        testAppCompatWidgetInflation(R.layout.layout_contextwrapperparent_imageview_vector,
                ContextWrapperFrameLayout.class);
    }

    @Test
    @SmallTest
    public void testCheckBoxInflation() throws Throwable {
        testAppCompatWidgetInflation(R.layout.layout_checkbox, AppCompatCheckBox.class);
    }

    @Test
    @SmallTest
    public void testActvInflation() throws Throwable {
        testAppCompatWidgetInflation(R.layout.layout_actv, AppCompatAutoCompleteTextView.class);
    }

    @Test
    @SmallTest
    public void testMactvInflation() throws Throwable {
        testAppCompatWidgetInflation(R.layout.layout_mactv,
                AppCompatMultiAutoCompleteTextView.class);
    }

    @Test
    @SmallTest
    public void testRatingBarInflation() throws Throwable {
        testAppCompatWidgetInflation(R.layout.layout_ratingbar, AppCompatRatingBar.class);
    }

    @Test
    @SmallTest
    public void testDeclarativeOnClickWithContextWrapper() throws Throwable {
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                LayoutInflater inflater = LayoutInflater.from(getActivity());
                View view = inflater.inflate(R.layout.layout_button_themed_onclick, null);

                assertTrue(view.performClick());
                assertTrue(getActivity().wasDeclarativeOnClickCalled());
            }
        });
    }

    private void testAppCompatWidgetInflation(final int layout, final Class<?> expectedClass)
            throws Throwable {
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                LayoutInflater inflater = LayoutInflater.from(getActivity());
                View view = inflater.inflate(layout, null);
                assertSame("View is " + expectedClass.getSimpleName(), expectedClass,
                        view.getClass());
            }
        });
    }

    private static void assertThemedContext(View view) {
        final Context viewContext = view.getContext();
        final int expectedColor = view.getResources().getColor(R.color.test_magenta);

        final TypedValue colorAccentValue = getColorAccentValue(viewContext.getTheme());
        assertTrue(colorAccentValue.type >= TypedValue.TYPE_FIRST_COLOR_INT
                && colorAccentValue.type <= TypedValue.TYPE_LAST_COLOR_INT);
        assertEquals("View does not have ContextThemeWrapper context",
                expectedColor, colorAccentValue.data);
    }

    private static TypedValue getColorAccentValue(final Resources.Theme theme) {
        final TypedValue typedValue = new TypedValue();
        theme.resolveAttribute(R.attr.colorAccent, typedValue, true);
        return typedValue;
    }
}
