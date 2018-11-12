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
package androidx.appcompat.widget;

import static android.text.Layout.Alignment.ALIGN_NORMAL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.Build;
import android.text.StaticLayout;
import android.text.method.SingleLineTransformationMethod;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.test.R;
import androidx.core.widget.TextViewCompat;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class AppCompatTextViewAutoSizeTest extends
        AppCompatBaseAutoSizeTest<AppCompatTextViewAutoSizeActivity, AppCompatTextView> {

    public AppCompatTextViewAutoSizeTest() {
        super(AppCompatTextViewAutoSizeActivity.class);
    }

    @Override
    protected AppCompatTextView getNewAutoSizeViewInstance() {
        return new AppCompatTextView(mActivity);
    }

    @Test
    public void testAutoSize_notSupportedByEditText() throws Throwable {
        final AppCompatEditText autoSizeEditText = mActivity.findViewById(
                R.id.edittext_autosize_uniform);
        // Do not force exact height only.
        final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                200,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                autoSizeEditText.setLayoutParams(layoutParams);
            }
        });
        mInstrumentation.waitForIdleSync();
        final float initialTextSize = autoSizeEditText.getTextSize();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                autoSizeEditText.setHeight(autoSizeEditText.getHeight() / 4);
            }
        });
        mInstrumentation.waitForIdleSync();

        assertEquals(initialTextSize, autoSizeEditText.getTextSize(), 0f);
    }

    @Test
    public void testAutoSizeWithMaxLines_shouldNotThrowException() throws Throwable {
        // the layout contains an instance of CustomTextViewWithTransformationMethod
        final AppCompatTextView textView = (AppCompatTextView) mActivity
                .getLayoutInflater().inflate(R.layout.textview_autosize_maxlines, null);
        assertTrue(textView instanceof CustomTextViewWithTransformationMethod);
        // Method added in API 16.
        if (Build.VERSION.SDK_INT >= 16) {
            assertEquals(1, textView.getMaxLines());
        }
        assertEquals(TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM, textView.getAutoSizeTextType());
        assertTrue(textView.getTransformationMethod() instanceof SingleLineTransformationMethod);
    }

    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    public void testSpacingIsSetPre16() throws NoSuchFieldException, IllegalAccessException {
        final AppCompatTextView textView = (AppCompatTextView) mActivity
                .getLayoutInflater().inflate(R.layout.textview_autosize_maxlines, null);
        textView.setLineSpacing(5, 5);

        final AppCompatTextViewAutoSizeHelper helper =
                new AppCompatTextViewAutoSizeHelper(textView);
        helper.initTempTextPaint(100);

        final String text = mActivity.getResources().getString(R.string.sample_text1);
        StaticLayout staticLayout = helper.createLayout(text, ALIGN_NORMAL, 100, 1);

        final Field spacingMultField = TextView.class.getDeclaredField("mSpacingMult");
        spacingMultField.setAccessible(true);
        final float spacingMultReference = (float) spacingMultField.get(textView);
        final float spacingMultActual = staticLayout.getSpacingMultiplier();
        assertEquals(spacingMultReference, spacingMultActual, 0f);

        final Field spacingAddField = TextView.class.getDeclaredField("mSpacingAdd");
        spacingAddField.setAccessible(true);
        final float spacingAddReference = (float) spacingAddField.get(textView);
        final float spacingAddActual = staticLayout.getSpacingAdd();
        assertEquals(spacingAddReference, spacingAddActual, 0f);

    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN)
    public void testSpacingIsSet() {
        final AppCompatTextView textView = (AppCompatTextView) mActivity
                .getLayoutInflater().inflate(R.layout.textview_autosize_maxlines,  null);
        textView.setLineSpacing(5, 5);

        final AppCompatTextViewAutoSizeHelper helper =
                new AppCompatTextViewAutoSizeHelper(textView);
        helper.initTempTextPaint(100);

        final String text = mActivity.getResources().getString(R.string.sample_text1);
        StaticLayout staticLayout = helper.createLayout(text, ALIGN_NORMAL, 100, 1);

        assertEquals(textView.getLineSpacingMultiplier(), staticLayout.getSpacingMultiplier(), 0f);
        assertEquals(textView.getLineSpacingExtra(), staticLayout.getSpacingAdd(), 0f);
    }

    public static class CustomTextViewWithTransformationMethod extends AppCompatTextView {
        public CustomTextViewWithTransformationMethod(Context context) {
            super(context);
            init();
        }

        public CustomTextViewWithTransformationMethod(Context context,
                @Nullable AttributeSet attrs) {
            super(context, attrs);
            init();
        }

        public CustomTextViewWithTransformationMethod(Context context,
                @Nullable AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
            init();
        }

        private void init() {
            setTransformationMethod(new SingleLineTransformationMethod());
        }
    }
}
