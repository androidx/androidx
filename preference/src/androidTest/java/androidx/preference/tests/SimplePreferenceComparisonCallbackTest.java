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
 * limitations under the License
 */

package androidx.preference.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.preference.CheckBoxPreference;
import androidx.preference.DropDownPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.TwoStatePreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class SimplePreferenceComparisonCallbackTest {

    private Preference mPref1;
    private Preference mPref2;
    private PreferenceManager.PreferenceComparisonCallback mComparisonCallback;

    @Before
    public void setup() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();
        mPref1 = new Preference(context);
        mPref2 = new Preference(context);
        mComparisonCallback = new PreferenceManager.SimplePreferenceComparisonCallback();
    }

    /**
     * Basic sanity test, all fields blank should compare the same
     * @throws Exception
     */
    @Test
    public void testNull() throws Exception {
        assertTrue("Compare all null",
                mComparisonCallback.arePreferenceContentsTheSame(mPref1, mPref2));
    }

    /**
     * Two different classes should not compare the same
     * @throws Exception
     */
    @Test
    public void testClassComparison() throws Exception {
        final Preference checkboxPreference =
                new CheckBoxPreference(InstrumentationRegistry.getTargetContext());
        assertFalse("Compare class",
                mComparisonCallback.arePreferenceContentsTheSame(mPref1, checkboxPreference));
    }

    /**
     * Same instance, but detached and reattached should not compare the same
     * @throws Exception
     */
    @Test
    public void testDetached() throws Exception {
        mPref1.onDetached();
        mPref1.onAttached();
        assertFalse("Compare same, detached",
                mComparisonCallback.arePreferenceContentsTheSame(mPref1, mPref1));
    }

    /**
     * Title differences should be detected
     * @throws Exception
     */
    @Test
    public void testTitleComparison() throws Exception {
        mPref1.setTitle("value 1");

        assertFalse("Compare non-null to null",
                mComparisonCallback.arePreferenceContentsTheSame(mPref1, mPref2));
        assertFalse("Compare null to non-null",
                mComparisonCallback.arePreferenceContentsTheSame(mPref2, mPref1));

        mPref2.setTitle("value 1");

        assertTrue("Compare identical",
                mComparisonCallback.arePreferenceContentsTheSame(mPref1, mPref2));

        mPref2.setTitle("value 2");

        assertFalse("Compare different",
                mComparisonCallback.arePreferenceContentsTheSame(mPref1, mPref2));
    }

    /**
     * Summary differences should be detected
     * @throws Exception
     */
    @Test
    public void testSummaryComparison() throws Exception {
        mPref1.setSummary("value 1");

        assertFalse("Compare non-null to null",
                mComparisonCallback.arePreferenceContentsTheSame(mPref1, mPref2));
        assertFalse("Compare null to non-null",
                mComparisonCallback.arePreferenceContentsTheSame(mPref2, mPref1));

        mPref2.setSummary("value 1");

        assertTrue("Compare identical",
                mComparisonCallback.arePreferenceContentsTheSame(mPref1, mPref2));

        mPref2.setSummary("value 2");

        assertFalse("Compare different",
                mComparisonCallback.arePreferenceContentsTheSame(mPref1, mPref2));
    }

    private static class ComparisonDrawable extends Drawable {

        private final int mId;

        public ComparisonDrawable(int id) {
            mId = id;
        }

        public int getId() {
            return mId;
        }

        @Override
        public void draw(Canvas canvas) {}

        @Override
        public void setAlpha(int alpha) {}

        @Override
        public void setColorFilter(ColorFilter colorFilter) {}

        @Override
        public int getOpacity() {
            return 0;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof ComparisonDrawable && ((ComparisonDrawable)o).getId() == mId;
        }

        @Override
        public int hashCode() {
            return mId;
        }
    }

    /**
     * Icon differences should be detected
     * @throws Exception
     */
    @Test
    public void testIconComparison() throws Exception {
        final Drawable drawable1 = new ComparisonDrawable(1);
        final Drawable drawable1a = new ComparisonDrawable(1);
        final Drawable drawable2 = new ComparisonDrawable(2);

        mPref1.setIcon(drawable1);

        assertFalse("Compare non-null to null",
                mComparisonCallback.arePreferenceContentsTheSame(mPref1, mPref2));
        assertFalse("Compare null to non-null",
                mComparisonCallback.arePreferenceContentsTheSame(mPref2, mPref1));

        mPref2.setIcon(drawable1);

        assertTrue("Compare aliased",
                mComparisonCallback.arePreferenceContentsTheSame(mPref1, mPref2));

        mPref2.setIcon(drawable1a);

        assertTrue("Compare equal",
                mComparisonCallback.arePreferenceContentsTheSame(mPref1, mPref2));

        mPref2.setIcon(drawable2);

        assertFalse("Compare unequal",
                mComparisonCallback.arePreferenceContentsTheSame(mPref1, mPref2));
    }

    /**
     * Enabled differences should be detected
     * @throws Exception
     */
    @Test
    public void testEnabledComparison() throws Exception {
        mPref1.setEnabled(true);
        mPref2.setEnabled(true);

        assertTrue("Compare enabled",
                mComparisonCallback.arePreferenceContentsTheSame(mPref1, mPref2));

        mPref2.setEnabled(false);

        assertFalse("Compare enabled/disabled",
                mComparisonCallback.arePreferenceContentsTheSame(mPref1, mPref2));
        assertFalse("Compare disable/enabled",
                mComparisonCallback.arePreferenceContentsTheSame(mPref2, mPref1));

        mPref1.setEnabled(false);

        assertTrue("Compare disabled",
                mComparisonCallback.arePreferenceContentsTheSame(mPref1, mPref2));
    }

    /**
     * Selectable differences should be detected
     * @throws Exception
     */
    @Test
    public void testSelectableComparison() throws Exception {
        mPref1.setSelectable(true);
        mPref2.setSelectable(true);

        assertTrue("Compare selectable",
                mComparisonCallback.arePreferenceContentsTheSame(mPref1, mPref2));

        mPref2.setSelectable(false);

        assertFalse("Compare selectable/unselectable",
                mComparisonCallback.arePreferenceContentsTheSame(mPref1, mPref2));
        assertFalse("Compare unselectable/selectable",
                mComparisonCallback.arePreferenceContentsTheSame(mPref2, mPref1));

        mPref1.setSelectable(false);

        assertTrue("Compare unselectable",
                mComparisonCallback.arePreferenceContentsTheSame(mPref1, mPref2));
    }

    /**
     * For {@link TwoStatePreference} objects, checked state differences should be detected
     * @throws Exception
     */
    @Test
    public void testTwoStateComparison() throws Exception {
        final TwoStatePreference checkbox1 =
                new CheckBoxPreference(InstrumentationRegistry.getTargetContext());
        final TwoStatePreference checkbox2 =
                new CheckBoxPreference(InstrumentationRegistry.getTargetContext());

        checkbox1.setChecked(true);
        checkbox2.setChecked(true);

        assertTrue("Compare checked",
                mComparisonCallback.arePreferenceContentsTheSame(checkbox1, checkbox2));

        checkbox2.setChecked(false);

        assertFalse("Compare checked/unchecked",
                mComparisonCallback.arePreferenceContentsTheSame(checkbox1, checkbox2));
        assertFalse("Compare unchecked/checked",
                mComparisonCallback.arePreferenceContentsTheSame(checkbox2, checkbox1));

        checkbox1.setChecked(false);

        assertTrue("Compare unchecked",
                mComparisonCallback.arePreferenceContentsTheSame(checkbox1, checkbox2));
    }

    /**
     * {@link DropDownPreference} is a special case, the pref object will need to re-bind the
     * spinner when recycled, so distinct instances are never evaluated as equal
     * @throws Exception
     */
    @Test
    public void testDropDownComparison() throws Exception {
        final Preference dropdown1 =
                new DropDownPreference(InstrumentationRegistry.getTargetContext());
        final Preference dropdown2 =
                new DropDownPreference(InstrumentationRegistry.getTargetContext());

        assertTrue("Compare aliased drop down pref",
                mComparisonCallback.arePreferenceContentsTheSame(dropdown1, dropdown1));
        assertFalse("Compare distinct drop down prefs",
                mComparisonCallback.arePreferenceContentsTheSame(dropdown1, dropdown2));
    }
}
