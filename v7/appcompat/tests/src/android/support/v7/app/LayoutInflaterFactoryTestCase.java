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

import org.junit.Test;

import android.os.Build;
import android.support.v7.appcompat.test.R;
import android.support.v7.widget.AppCompatAutoCompleteTextView;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatMultiAutoCompleteTextView;
import android.support.v7.widget.AppCompatRadioButton;
import android.support.v7.widget.AppCompatRatingBar;
import android.support.v7.widget.AppCompatSpinner;
import android.view.LayoutInflater;
import android.view.View;

public class LayoutInflaterFactoryTestCase extends BaseInstrumentationTestCase<AppCompatActivity> {

    public LayoutInflaterFactoryTestCase() {
        super(AppCompatActivity.class);
    }

    @Test
    public void testAndroidThemeInflation() throws Throwable {
        if (Build.VERSION.SDK_INT < 10) {
            // Ignore this test if running on Gingerbread or below
            return;
        }
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                LayoutInflater inflater = LayoutInflater.from(getActivity());
                View view = inflater.inflate(R.layout.layout_android_theme, null);
                assertTrue("View has themed Context", view.getContext() != getActivity());
            }
        });
    }

    @Test
    public void testAppThemeInflation() throws Throwable {
        if (Build.VERSION.SDK_INT < 10) {
            // Ignore this test if running on Gingerbread or below
            return;
        }
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                LayoutInflater inflater = LayoutInflater.from(getActivity());
                View view = inflater.inflate(R.layout.layout_app_theme, null);
                assertTrue("View has themed Context", view.getContext() != getActivity());
            }
        });
    }

    @Test
    public void testSpinnerInflation() throws Throwable {
        testAppCompatWidgetInflation(R.layout.layout_spinner, AppCompatSpinner.class);
    }

    @Test
    public void testEditTextInflation() throws Throwable {
        testAppCompatWidgetInflation(R.layout.layout_edittext, AppCompatEditText.class);
    }

    @Test
    public void testButtonInflation() throws Throwable {
        testAppCompatWidgetInflation(R.layout.layout_button, AppCompatButton.class);
    }

    @Test
    public void testRadioButtonInflation() throws Throwable {
        testAppCompatWidgetInflation(R.layout.layout_radiobutton, AppCompatRadioButton.class);
    }

    @Test
    public void testCheckBoxInflation() throws Throwable {
        testAppCompatWidgetInflation(R.layout.layout_checkbox, AppCompatCheckBox.class);
    }

    @Test
    public void testActvInflation() throws Throwable {
        testAppCompatWidgetInflation(R.layout.layout_actv, AppCompatAutoCompleteTextView.class);
    }

    @Test
    public void testMactvInflation() throws Throwable {
        testAppCompatWidgetInflation(R.layout.layout_mactv,
                AppCompatMultiAutoCompleteTextView.class);
    }

    @Test
    public void testRatingBarInflation() throws Throwable {
        testAppCompatWidgetInflation(R.layout.layout_ratingbar, AppCompatRatingBar.class);
    }

    private void testAppCompatWidgetInflation(final int layout, final Class<?> expectedClass)
            throws Throwable {
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                LayoutInflater inflater = LayoutInflater.from(getActivity());
                View view = inflater.inflate(layout, null);
                assertEquals("View is " + expectedClass.getSimpleName(), expectedClass,
                        view.getClass());
            }
        });
    }
}
