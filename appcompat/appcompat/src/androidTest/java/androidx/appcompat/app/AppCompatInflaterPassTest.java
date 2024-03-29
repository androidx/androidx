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

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertEquals;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;

import androidx.appcompat.test.R;
import androidx.appcompat.testutils.BaseTestActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.AppCompatToggleButton;
import androidx.core.view.ViewCompat;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.rule.ActivityTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Testing the default view inflation where appcompat views are used for specific
 * views in layouts specified in XML.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public abstract class AppCompatInflaterPassTest<A extends BaseTestActivity> {
    private ViewGroup mContainer;
    private A mActivity;

    @Rule
    public final ActivityTestRule<A> mActivityTestRule;

    public AppCompatInflaterPassTest(Class<A> clazz) {
        mActivityTestRule = new ActivityTestRule<A>(clazz);
    }

    @Before
    public void setUp() {
        mActivity = mActivityTestRule.getActivity();
        mContainer = mActivity.findViewById(R.id.container);
    }

    @Test
    public void testViewClasses() {
        // View defined as <Button> should be inflated as AppCompatButton
        assertEquals(AppCompatButton.class, mContainer.findViewById(R.id.button).getClass());

        // View defined as <AppCompatButton> should be inflated as AppCompatButton
        assertEquals(AppCompatButton.class, mContainer.findViewById(R.id.ac_button).getClass());

        // View defined as <TextView> should be inflated as AppCompatTextView
        assertEquals(AppCompatTextView.class, mContainer.findViewById(R.id.textview).getClass());

        // View defined as <AppCompatTextView> should be inflated as AppCompatTextView
        assertEquals(AppCompatTextView.class, mContainer.findViewById(R.id.ac_textview).getClass());

        // View defined as <RadioButton> should be inflated as AppCompatRadioButton
        assertEquals(AppCompatRadioButton.class,
                mContainer.findViewById(R.id.radiobutton).getClass());

        // View defined as <ImageButton> should be inflated as AppCompatImageButton
        assertEquals(AppCompatImageButton.class,
                mContainer.findViewById(R.id.imagebutton).getClass());

        // View defined as <Spinner> should be inflated as AppCompatSpinner
        assertEquals(AppCompatSpinner.class, mContainer.findViewById(R.id.spinner).getClass());

        // View defined as <ToggleButton> should be inflated as AppCompatToggleButton
        assertEquals(AppCompatToggleButton.class,
                mContainer.findViewById(R.id.togglebutton).getClass());

        // View defined as <AppCompatToggleButton> should be inflated as AppCompatToggleButton
        assertEquals(AppCompatToggleButton.class,
                mContainer.findViewById(R.id.ac_togglebutton).getClass());

        // View defined as <ScrollView> should be inflated as ScrollView
        assertEquals(ScrollView.class,
                mContainer.findViewById(R.id.scrollview).getClass());
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    public void testBackportAccessibilityAttributes() {
        View view = mContainer.findViewById(R.id.accessibility_heading_view);
        assertThat(ViewCompat.isAccessibilityHeading(view)).isTrue();

        view = mContainer.findViewById(R.id.accessibility_pane_view);
        assertThat(ViewCompat.getAccessibilityPaneTitle(view)).isEqualTo("Pane");

        view = mContainer.findViewById(R.id.screen_reader_focusable_view);
        assertThat(ViewCompat.isScreenReaderFocusable(view)).isTrue();

        view = mContainer.findViewById(R.id.not_accessible_view);
        assertThat(ViewCompat.isAccessibilityHeading(view)).isFalse();
        assertThat(ViewCompat.getAccessibilityPaneTitle(view)).isNull();
        assertThat(ViewCompat.isScreenReaderFocusable(view)).isFalse();
    }
}
