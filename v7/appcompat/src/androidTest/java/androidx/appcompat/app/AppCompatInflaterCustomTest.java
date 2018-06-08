/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.view.ViewGroup;
import android.widget.ScrollView;

import androidx.appcompat.app.inflater.CustomViewInflater;
import androidx.appcompat.test.R;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.appcompat.widget.AppCompatTextView;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Testing the custom view inflation where app-specific views are used for specific
 * views in layouts specified in XML.
 */
@SmallTest
public class AppCompatInflaterCustomTest {
    private ViewGroup mContainer;
    private AppCompatInflaterCustomActivity mActivity;

    @Rule
    public final ActivityTestRule<AppCompatInflaterCustomActivity> mActivityTestRule =
            new ActivityTestRule<>(AppCompatInflaterCustomActivity.class);

    @Before
    public void setUp() {
        mActivity = mActivityTestRule.getActivity();
        mContainer = mActivity.findViewById(R.id.container);
    }

    @Test
    public void testViewClasses() {
        // View defined as <Button> should be inflated as CustomButton
        assertEquals(CustomViewInflater.CustomButton.class,
                mContainer.findViewById(R.id.button).getClass());

        // View defined as <AppCompatButton> should be inflated as AppCompatButton
        assertEquals(AppCompatButton.class, mContainer.findViewById(R.id.ac_button).getClass());

        // View defined as <TextView> should be inflated as CustomTextView
        assertEquals(CustomViewInflater.CustomTextView.class,
                mContainer.findViewById(R.id.textview).getClass());

        // View defined as <AppCompatTextView> should be inflated as AppCompatTextView
        assertEquals(AppCompatTextView.class, mContainer.findViewById(R.id.ac_textview).getClass());

        // View defined as <RadioButton> should be inflated as AppCompatRadioButton
        assertEquals(AppCompatRadioButton.class,
                mContainer.findViewById(R.id.radiobutton).getClass());

        // View defined as <ImageButton> should be inflated as CustomImageButton
        assertEquals(CustomViewInflater.CustomImageButton.class,
                mContainer.findViewById(R.id.imagebutton).getClass());

        // View defined as <Spinner> should be inflated as AppCompatSpinner
        assertEquals(AppCompatSpinner.class, mContainer.findViewById(R.id.spinner).getClass());

        // View defined as <ToggleButton> should be inflated as CustomToggleButton
        assertEquals(CustomViewInflater.CustomToggleButton.class,
                mContainer.findViewById(R.id.togglebutton).getClass());

        // View defined as <ScrollView> should be inflated as ScrollView
        assertEquals(ScrollView.class,
                mContainer.findViewById(R.id.scrollview).getClass());
    }

}
