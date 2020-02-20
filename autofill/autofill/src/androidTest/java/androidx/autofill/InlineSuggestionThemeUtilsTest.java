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

package androidx.autofill;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import android.app.Instrumentation;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.widget.ImageView;

import androidx.arch.core.util.Function;
import androidx.autofill.test.R;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 29) // Needed only on 29 and above
public class InlineSuggestionThemeUtilsTest {
    private Instrumentation mInstrumentation;
    private Context mContext;
    private Resources mResources;

    @Before
    public void setup() {
        mInstrumentation = InstrumentationRegistry.getInstrumentation();
        mContext = mInstrumentation.getContext();
        mResources = mContext.getResources();
    }

    @Test
    public void getContextThemeWrapper_nullStyle() {
        Context context = InlineSuggestionThemeUtils.getContextThemeWrapper(mContext,
                /* style= */null);
        verifyDefaultTheme(context);
    }

    @Test
    public void getContextThemeWrapper_invalidStyle_badName() {
        Context context = InlineSuggestionThemeUtils.getContextThemeWrapper(mContext,
                /* style= */"aaa:bb");
        verifyDefaultTheme(context);
    }

    @Test
    public void getContextThemeWrapper_invalidStyle_badName2() {
        Context context = InlineSuggestionThemeUtils.getContextThemeWrapper(mContext,
                /* style= */"aaa:bb/cc");
        verifyDefaultTheme(context);
    }

    @Test
    public void getContextThemeWrapper_invalidStyle_badParent() {
        String style = mResources.getResourceName(R.style.InvalidTheme);
        Context context = InlineSuggestionThemeUtils.getContextThemeWrapper(mContext,
                /* style= */style);
        verifyDefaultTheme(context);
    }

    @Test
    public void getContextThemeWrapper_validStyle() {
        String style = mResources.getResourceName(R.style.ValidTheme);
        Context context = InlineSuggestionThemeUtils.getContextThemeWrapper(mContext,
                /* style= */style);
        verifyIntValue(context, android.R.attr.background, R.attr.autofillInlineSuggestionChip,
                Color.parseColor("#33FF00FF")); // Pink background
        verifyIntValue(context, android.R.attr.textColor, R.attr.autofillInlineSuggestionTitle,
                Color.parseColor("#FF00FF00")); // Green text
        verifyIntValue(context, android.R.attr.textColor, R.attr.autofillInlineSuggestionSubtitle,
                Color.parseColor("#FF00FF00")); // Green text
        verifyIntValue(context, android.R.attr.scaleType,
                R.attr.autofillInlineSuggestionStartIconStyle,
                ImageView.ScaleType.FIT_CENTER.ordinal());
        verifyIntValue(context, android.R.attr.scaleType,
                R.attr.autofillInlineSuggestionEndIconStyle,
                ImageView.ScaleType.FIT_CENTER.ordinal());
    }

    private static void verifyDefaultTheme(Context context) {
        verifyStyleValue(context, android.R.attr.background,
                R.attr.autofillInlineSuggestionChip,
                R.drawable.autofill_inline_suggestion_chip_background);
        verifyIntValue(context, android.R.attr.textColor,
                R.attr.autofillInlineSuggestionTitle,
                Color.parseColor("#FF424242"));
        verifyIntValue(context, android.R.attr.textColor,
                R.attr.autofillInlineSuggestionSubtitle,
                Color.parseColor("#66FFFFFF"));
        verifyIntValue(context, android.R.attr.scaleType,
                R.attr.autofillInlineSuggestionStartIconStyle,
                ImageView.ScaleType.FIT_CENTER.ordinal());
        verifyIntValue(context, android.R.attr.scaleType,
                R.attr.autofillInlineSuggestionEndIconStyle,
                ImageView.ScaleType.FIT_CENTER.ordinal());
    }

    private static void verifyIntValue(Context context, int attr, int styleAttr,
            int expectedValue) {
        verifyAttrValue(context, attr, styleAttr, expectedValue,
                ta -> ta.getInt(ta.getIndex(0), 0));
    }

    private static void verifyStyleValue(Context context, int attr, int styleAttr,
            int expectedValue) {
        verifyAttrValue(context, attr, styleAttr, expectedValue,
                ta -> ta.getResourceId(ta.getIndex(0),
                        0));
    }

    private static void verifyAttrValue(Context context, int attr, int styleAttr,
            int expectedValue, Function<TypedArray, Integer> valueFun) {
        TypedArray ta = null;
        try {
            ta = context.getTheme().obtainStyledAttributes(null, new int[]{attr}, styleAttr, 0);
            assertNotEquals(0, ta.getIndexCount());
            assertEquals(expectedValue, (int) valueFun.apply(ta));
        } finally {
            if (ta != null) {
                ta.recycle();
            }
        }
    }
}
