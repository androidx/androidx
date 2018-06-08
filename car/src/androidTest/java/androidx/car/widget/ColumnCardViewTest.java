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

package androidx.car.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import android.support.test.annotation.UiThreadTest;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.ViewTreeObserver;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.car.test.R;
import androidx.car.utils.ColumnCalculator;

/** Instrumentation unit tests for {@link ColumnCardView}. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public final class ColumnCardViewTest {
    @Rule
    public ActivityTestRule<ColumnCardViewTestActivity> mActivityRule =
            new ActivityTestRule<>(ColumnCardViewTestActivity.class);

    private ColumnCalculator mCalculator;
    private ColumnCardViewTestActivity mActivity;

    @Before
    public void setUp() {
        mActivity = mActivityRule.getActivity();
        mCalculator = ColumnCalculator.getInstance(mActivity);
    }

    @Test
    public void defaultCardWidthMatchesCalculation() {
        ColumnCardView card = mActivity.findViewById(R.id.default_width_column_card);

        assertEquals(mCalculator.getSizeForColumnSpan(mActivity.getResources().getInteger(
                R.integer.column_card_default_column_span)),
                card.getWidth());
    }

    @Test
    public void customXmlColumnSpanMatchesCalculation() {
        ColumnCardView card = mActivity.findViewById(R.id.span_2_column_card);

        assertEquals(mCalculator.getSizeForColumnSpan(2), card.getWidth());
    }

    @UiThreadTest
    @Test
    public void settingColumnSpanMatchesCalculation() {
        final int columnSpan = 4;
        final ColumnCardView card = mActivity.findViewById(R.id.span_2_column_card);
        assertNotEquals(columnSpan, card.getColumnSpan());

        card.setColumnSpan(columnSpan);
        // When card finishes layout, verify its updated width.
        card.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        assertEquals(mCalculator.getSizeForColumnSpan(columnSpan), card.getWidth());
                    }
                });
    }

    @UiThreadTest
    @Test
    public void nonPositiveColumnSpanIsIgnored() {
        final ColumnCardView card = mActivity.findViewById(R.id.default_width_column_card);
        final int original = card.getColumnSpan();

        card.setColumnSpan(0);
        // When card finishes layout, verify its width remains unchanged.
        card.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        assertEquals(mCalculator.getSizeForColumnSpan(original), card.getWidth());
                    }
                });
    }
}
