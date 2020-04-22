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
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.ColorInt;

import org.junit.Assert;

public final class TestUtils {

    public static void verifyPadding(View view, int left, int top, int right, int bottom) {
        Assert.assertEquals(left, view.getPaddingLeft());
        Assert.assertEquals(top, view.getPaddingTop());
        Assert.assertEquals(right, view.getPaddingRight());
        Assert.assertEquals(bottom, view.getPaddingBottom());
    }

    public static void verifyLayoutMargin(View view, int left, int top, int right, int bottom) {
        Assert.assertTrue(view.getLayoutParams() instanceof ViewGroup.MarginLayoutParams);
        ViewGroup.MarginLayoutParams marginLayoutParams =
                (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        Assert.assertEquals(left, marginLayoutParams.leftMargin);
        Assert.assertEquals(top, marginLayoutParams.topMargin);
        Assert.assertEquals(right, marginLayoutParams.rightMargin);
        Assert.assertEquals(bottom, marginLayoutParams.bottomMargin);
    }

    public static void verifyBackgroundColor(View view, @ColorInt int color) {
        Drawable drawable = view.getBackground();
        Assert.assertTrue(drawable instanceof ColorDrawable);
        Assert.assertEquals(color, ((ColorDrawable) drawable).getColor());
    }

    public static void verifyTextSize(Context context, TextView textView, float spSize) {
        float density = context.getResources().getDisplayMetrics().density;
        Assert.assertEquals(spSize, toDp(density, textView.getTextSize()), 0.001);
    }

    private static int toDp(float density, float pixel) {
        return (int) (pixel / density);
    }

    private static int toPixel(float density, int dp) {
        return (int) (dp * density) + 1;
    }

    private TestUtils() {
    }
}
