/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.car.utils;


import static org.mockito.Mockito.verify;

import android.view.View;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import androidx.car.R;

/**
 * Tests for {@link ListItemBackgroundResolver}.
 */
@RunWith(JUnit4.class)
public class ListItemBackgroundResolverTest {

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock
    View mMockView;

    @Test
    public void testSingleItemInListHasAllRoundedCorners() {
        ListItemBackgroundResolver.setBackground(mMockView, 0, 1);

        verify(mMockView).setBackgroundResource(R.drawable.car_card_rounded_background);
    }

    @Test
    public void testOnlyTopItemHasTopRoundedCorners() {
        ListItemBackgroundResolver.setBackground(mMockView, 0, 2);

        verify(mMockView).setBackgroundResource(R.drawable.car_card_rounded_top_background);
    }

    @Test
    public void testOnlyBottomItemHasBottomRoundedCorners() {
        ListItemBackgroundResolver.setBackground(mMockView, 1, 2);

        verify(mMockView).setBackgroundResource(R.drawable.car_card_rounded_bottom_background);
    }

    @Test
    public void testMiddleItemHasNoRoundedCorner() {
        ListItemBackgroundResolver.setBackground(mMockView, 1, 3);

        verify(mMockView).setBackgroundResource(R.drawable.car_card_background);
    }
}
