/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.autofill.inline.common;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.graphics.drawable.RippleDrawable;
import android.view.View;

import androidx.autofill.R;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 29) // Needed only on 29 and above
public class ViewStyleTest {

    private Context mContext;

    @Before
    public void setup() {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
    }

    @Test
    public void testStyleWithNothing() {
        ViewStyle.Builder builder = new ViewStyle.Builder();
        ViewStyle style = builder.build();
        View view = new View(mContext);
        style.applyStyleOnViewIfValid(view);
    }

    @Test
    public void testStyleWithPaddingAndBackgroundColor() {
        ViewStyle.Builder builder = new ViewStyle.Builder();
        ViewStyle style = builder
                .setPadding(1, 2, 3, 4)
                .setBackgroundColor(Color.BLUE)
                .build();
        View view = new View(mContext);
        style.applyStyleOnViewIfValid(view);
        TestUtils.verifyPadding(view, 1, 2, 3, 4);
        TestUtils.verifyBackgroundColor(view, Color.BLUE);
    }

    @Test
    public void testStyleWithLayoutMarginAndBackground() {
        Icon backgroundIcon = Icon.createWithResource(mContext,
                R.drawable.autofill_inline_suggestion_chip_background);
        ViewStyle.Builder builder = new ViewStyle.Builder();
        ViewStyle style = builder
                .setLayoutMargin(5, 6, 7, 8)
                .setBackground(backgroundIcon)
                .build();
        View view = new View(mContext);
        style.applyStyleOnViewIfValid(view);
        TestUtils.verifyLayoutMargin(view, 5, 6, 7, 8);
        Assert.assertTrue(view.getBackground() instanceof RippleDrawable);
    }

    @Test
    public void testStyleWithEverything() {
        Icon backgroundIcon = Icon.createWithResource(mContext,
                R.drawable.autofill_inline_suggestion_chip_background);
        ViewStyle.Builder builder = new ViewStyle.Builder();
        ViewStyle style = builder
                .setPadding(1, 2, 3, 4)
                .setLayoutMargin(5, 6, 7, 8)
                .setBackground(backgroundIcon)
                .setBackgroundColor(Color.YELLOW)
                .build();
        View view = new View(mContext);
        style.applyStyleOnViewIfValid(view);
        TestUtils.verifyPadding(view, 1, 2, 3, 4);
        TestUtils.verifyLayoutMargin(view, 5, 6, 7, 8);
        // When both background and background color are set, the background color takes precedence.
        TestUtils.verifyBackgroundColor(view, Color.YELLOW);
    }
}
