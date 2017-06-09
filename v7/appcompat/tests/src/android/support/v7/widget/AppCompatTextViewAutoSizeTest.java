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
package android.support.v7.widget;

import static junit.framework.TestCase.assertEquals;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Instrumentation;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.appcompat.test.R;
import android.text.TextUtils;
import android.text.method.SingleLineTransformationMethod;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class AppCompatTextViewAutoSizeTest {
    @Rule
    public final ActivityTestRule<AppCompatTextViewAutoSizeActivity> mActivityTestRule;

    private Instrumentation mInstrumentation;
    private AppCompatTextViewAutoSizeActivity mActivity;

    public AppCompatTextViewAutoSizeTest() {
        mActivityTestRule = new ActivityTestRule<>(AppCompatTextViewAutoSizeActivity.class);
    }

    @Before
    public void setup() {
        mInstrumentation = InstrumentationRegistry.getInstrumentation();
        mActivity = mActivityTestRule.getActivity();
    }

    @Test
    public void testAutoSizeCallers_setText() throws Throwable {
        final AppCompatTextView autoSizeTextView = prepareAndRetrieveAutoSizeTestData(
                R.id.textview_autosize_uniform, false);

        // Configure layout params and auto-size both in pixels to dodge flakiness on different
        // devices.
        final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                500, 500);
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                autoSizeTextView.setLayoutParams(layoutParams);
                autoSizeTextView.setAutoSizeTextTypeUniformWithConfiguration(
                        1, 5000, 1, TypedValue.COMPLEX_UNIT_PX);
            }
        });
        mInstrumentation.waitForIdleSync();

        final String initialText = "13characters ";
        final StringBuilder textToSet = new StringBuilder().append(initialText);
        float initialSize = 0;

        // As we add characters the text size shrinks.
        for (int i = 0; i < 10; i++) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    autoSizeTextView.setText(textToSet.toString());
                }
            });

            mInstrumentation.waitForIdleSync();
            float expectedLargerSize = autoSizeTextView.getTextSize();
            if (i == 0) {
                initialSize = expectedLargerSize;
            }

            textToSet.append(initialText);
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    autoSizeTextView.setText(textToSet.toString());
                }
            });
            mInstrumentation.waitForIdleSync();
            assertTrue(expectedLargerSize >= autoSizeTextView.getTextSize());
        }
        assertTrue(initialSize > autoSizeTextView.getTextSize());

        initialSize = 9999999;
        // As we remove characters the text size expands.
        for (int i = 9; i >= 0; i--) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    autoSizeTextView.setText(textToSet.toString());
                }
            });
            mInstrumentation.waitForIdleSync();
            float expectedSmallerSize = autoSizeTextView.getTextSize();
            if (i == 0) {
                initialSize = expectedSmallerSize;
            }

            textToSet.replace((textToSet.length() - initialText.length()),
                    textToSet.length(), "");
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    autoSizeTextView.setText(textToSet.toString());
                }
            });
            mInstrumentation.waitForIdleSync();

            assertTrue(autoSizeTextView.getTextSize() >= expectedSmallerSize);
        }
        assertTrue(autoSizeTextView.getTextSize() > initialSize);
    }

    @Test
    public void testAutoSizeUniform_equivalentConfigurations() throws Throwable {
        final DisplayMetrics dm = mActivity.getResources().getDisplayMetrics();
        final int minTextSize = 10;
        final int maxTextSize = 20;
        final int granularity = 2;
        final int unit = TypedValue.COMPLEX_UNIT_SP;

        final AppCompatTextView granularityTextView = new AppCompatTextView(mActivity);
        granularityTextView.setAutoSizeTextTypeUniformWithConfiguration(
                minTextSize, maxTextSize, granularity, unit);

        final AppCompatTextView presetTextView = new AppCompatTextView(mActivity);
        presetTextView.setAutoSizeTextTypeUniformWithPresetSizes(
                new int[]{minTextSize, 12, 14, 16, 18, maxTextSize}, unit);

        // The TextViews have been configured differently but the end result should be nearly
        // identical.
        final int expectedAutoSizeType = AppCompatTextView.AUTO_SIZE_TEXT_TYPE_UNIFORM;
        assertEquals(expectedAutoSizeType, granularityTextView.getAutoSizeTextType());
        assertEquals(expectedAutoSizeType, presetTextView.getAutoSizeTextType());

        final int expectedMinTextSizeInPx = Math.round(
                TypedValue.applyDimension(unit, minTextSize, dm));
        assertEquals(expectedMinTextSizeInPx, granularityTextView.getAutoSizeMinTextSize());
        assertEquals(expectedMinTextSizeInPx, presetTextView.getAutoSizeMinTextSize());

        final int expectedMaxTextSizeInPx = Math.round(
                TypedValue.applyDimension(unit, maxTextSize, dm));
        assertEquals(expectedMaxTextSizeInPx, granularityTextView.getAutoSizeMaxTextSize());
        assertEquals(expectedMaxTextSizeInPx, presetTextView.getAutoSizeMaxTextSize());

        // Configured with granularity.
        assertEquals(Math.round(TypedValue.applyDimension(unit, granularity, dm)),
                granularityTextView.getAutoSizeStepGranularity());
        // Configured with preset values, there is no granularity.
        assertEquals(-1, presetTextView.getAutoSizeStepGranularity());

        // Both TextViews generate exactly the same sizes in pixels to choose from when auto-sizing.
        assertArrayEquals(
                granularityTextView.getAutoSizeTextAvailableSizes(),
                presetTextView.getAutoSizeTextAvailableSizes());

        final String someText = "This is a string";
        final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                500, 500);
        // Configure identically and attach to layout.
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                granularityTextView.setLayoutParams(layoutParams);
                presetTextView.setLayoutParams(layoutParams);

                LinearLayout ll = mActivity.findViewById(R.id.layout_textviewtest);
                ll.removeAllViews();
                ll.addView(granularityTextView);
                ll.addView(presetTextView);

                granularityTextView.setText(someText);
                presetTextView.setText(someText);
            }
        });
        mInstrumentation.waitForIdleSync();

        assertEquals(granularityTextView.getTextSize(), presetTextView.getTextSize(), 0f);
    }

    @Test
    public void testAutoSize_notSupportedByEditText() throws Throwable {
        final AppCompatEditText autoSizeEditText = (AppCompatEditText) mActivity.findViewById(
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
    public void testAutoSizeCallers_setHeight() throws Throwable {
        final AppCompatTextView autoSizeTextView = prepareAndRetrieveAutoSizeTestData(
                R.id.textview_autosize_uniform, true);
        // Do not force exact height only.
        final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                200,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                autoSizeTextView.setLayoutParams(layoutParams);
            }
        });
        mInstrumentation.waitForIdleSync();
        final float initialTextSize = autoSizeTextView.getTextSize();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                autoSizeTextView.setHeight(autoSizeTextView.getHeight() / 4);
            }
        });
        mInstrumentation.waitForIdleSync();

        assertTrue(autoSizeTextView.getTextSize() < initialTextSize);
    }

    @Test
    public void testAutoSizeCallers_setCompoundDrawables() throws Throwable {
        final AppCompatTextView autoSizeTextView = prepareAndRetrieveAutoSizeTestData(
                R.id.textview_autosize_uniform, false);
        final float initialTextSize = autoSizeTextView.getTextSize();
        final Drawable drawable = ResourcesCompat.getDrawable(mActivity.getResources(),
                R.drawable.test_drawable_red, null);
        drawable.setBounds(0, 0, autoSizeTextView.getWidth() / 3, autoSizeTextView.getHeight() / 3);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                autoSizeTextView.setCompoundDrawables(drawable, drawable, drawable, drawable);
            }
        });
        mInstrumentation.waitForIdleSync();

        assertTrue(autoSizeTextView.getTextSize() < initialTextSize);
    }

    @Test
    public void testAutoSizeCallers_setCompoundDrawablesRelative() throws Throwable {
        if (Build.VERSION.SDK_INT >= 17) {
            final AppCompatTextView autoSizeTextView = prepareAndRetrieveAutoSizeTestData(
                    R.id.textview_autosize_uniform, false);
            final float initialTextSize = autoSizeTextView.getTextSize();
            final Drawable drawable = ResourcesCompat.getDrawable(mActivity.getResources(),
                    R.drawable.test_drawable_red, null);
            drawable.setBounds(0, 0, autoSizeTextView.getWidth() / 3,
                    autoSizeTextView.getHeight() / 3);
            mActivityTestRule.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (Build.VERSION.SDK_INT >= 17) {
                        autoSizeTextView.setCompoundDrawablesRelative(
                                drawable, drawable, drawable, drawable);
                    }
                }
            });
            mInstrumentation.waitForIdleSync();

            assertTrue(autoSizeTextView.getTextSize() < initialTextSize);
        }
    }

    @Test
    public void testAutoSizeCallers_setCompoundDrawablePadding() throws Throwable {
        final AppCompatTextView autoSizeTextView = prepareAndRetrieveAutoSizeTestData(
                R.id.textview_autosize_uniform, false);
        // Prepare a larger layout in order not to hit the min value easily.
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                autoSizeTextView.setWidth(autoSizeTextView.getWidth() * 2);
                autoSizeTextView.setHeight(autoSizeTextView.getHeight() * 2);
            }
        });
        mInstrumentation.waitForIdleSync();
        // Setup the drawables before setting their padding in order to modify the available
        // space and trigger a resize.
        final Drawable drawable = ResourcesCompat.getDrawable(mActivity.getResources(),
                R.drawable.test_drawable_red, null);
        drawable.setBounds(0, 0, autoSizeTextView.getWidth() / 4, autoSizeTextView.getHeight() / 4);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                autoSizeTextView.setCompoundDrawables(
                        drawable, drawable, drawable, drawable);
            }
        });
        mInstrumentation.waitForIdleSync();
        final float initialTextSize = autoSizeTextView.getTextSize();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                autoSizeTextView.setCompoundDrawablePadding(
                        autoSizeTextView.getCompoundDrawablePadding() + 10);
            }
        });
        mInstrumentation.waitForIdleSync();

        assertTrue(autoSizeTextView.getTextSize() < initialTextSize);
    }

    @Test
    public void testAutoSizeCallers_setPadding() throws Throwable {
        final AppCompatTextView autoSizeTextView = prepareAndRetrieveAutoSizeTestData(
                R.id.textview_autosize_uniform, false);
        final float initialTextSize = autoSizeTextView.getTextSize();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                autoSizeTextView.setPadding(
                        autoSizeTextView.getWidth() / 3, autoSizeTextView.getHeight() / 3,
                        autoSizeTextView.getWidth() / 3, autoSizeTextView.getHeight() / 3);
            }
        });
        mInstrumentation.waitForIdleSync();

        assertTrue(autoSizeTextView.getTextSize() < initialTextSize);
    }

    @Test
    public void testAutoSizeCallers_setPaddingRelative() throws Throwable {
        if (Build.VERSION.SDK_INT >= 16) {
            final AppCompatTextView autoSizeTextView = prepareAndRetrieveAutoSizeTestData(
                    R.id.textview_autosize_uniform, false);
            final float initialTextSize = autoSizeTextView.getTextSize();

            mActivityTestRule.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (Build.VERSION.SDK_INT > 16) {
                        autoSizeTextView.setPaddingRelative(
                                autoSizeTextView.getWidth() / 3, autoSizeTextView.getHeight() / 3,
                                autoSizeTextView.getWidth() / 3, autoSizeTextView.getHeight() / 3);
                    }
                }
            });
            mInstrumentation.waitForIdleSync();

            assertTrue(autoSizeTextView.getTextSize() < initialTextSize);
        }
    }

    @Test
    public void testAutoSizeCallers_setTypeface() throws Throwable {
        final AppCompatTextView autoSizeTextView = prepareAndRetrieveAutoSizeTestData(
                R.id.textview_autosize_uniform, false);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                autoSizeTextView.setText("The typeface change needs a bit more text then "
                        + "the default used for this batch of tests in order to get to resize text."
                        + " The resize function is always called but even with different typefaces "
                        + "there may not be a need to resize text because it just fits. The longer "
                        + "the text, the higher the chance for a resize. And here is yet another "
                        + "sentence to make sure this test is not flaky. Not flaky at all.");
            }
        });
        mInstrumentation.waitForIdleSync();
        final float initialTextSize = autoSizeTextView.getTextSize();

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Typeface differentTypeface = Typeface.MONOSPACE;
                if (autoSizeTextView.getTypeface() == Typeface.MONOSPACE) {
                    differentTypeface = Typeface.SANS_SERIF;
                }
                autoSizeTextView.setTypeface(differentTypeface);
            }
        });
        mInstrumentation.waitForIdleSync();
        final float changedTextSize = autoSizeTextView.getTextSize();

        // Don't really know if it is larger or smaller (depends on the typeface chosen above),
        // but it should definitely have changed.
        assertNotEquals(initialTextSize, changedTextSize, 0f);

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                autoSizeTextView.setTypeface(autoSizeTextView.getTypeface());
            }
        });
        mInstrumentation.waitForIdleSync();

        assertEquals(changedTextSize, autoSizeTextView.getTextSize(), 0f);
    }

    @Test
    public void testAutoSizeCallers_setLetterSpacing() throws Throwable {
        if (Build.VERSION.SDK_INT >= 21) {
            final AppCompatTextView autoSizeTextView = prepareAndRetrieveAutoSizeTestData(
                    R.id.textview_autosize_uniform, false);
            final float initialTextSize = autoSizeTextView.getTextSize();

            mActivityTestRule.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (Build.VERSION.SDK_INT >= 21) {
                        autoSizeTextView.setLetterSpacing(
                                autoSizeTextView.getLetterSpacing() * 1.5f + 4.5f);
                    }
                }
            });
            mInstrumentation.waitForIdleSync();
            final float changedTextSize = autoSizeTextView.getTextSize();

            assertTrue(changedTextSize < initialTextSize);

            mActivityTestRule.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (Build.VERSION.SDK_INT >= 21) {
                        autoSizeTextView.setLetterSpacing(autoSizeTextView.getLetterSpacing());
                    }
                }
            });
            mInstrumentation.waitForIdleSync();

            assertEquals(changedTextSize, autoSizeTextView.getTextSize(), 0f);
        }
    }

    @Test
    public void testAutoSizeCallers_setMaxHeight() throws Throwable {
        final AppCompatTextView autoSizeTextView = prepareAndRetrieveAutoSizeTestData(
                R.id.textview_autosize_uniform, true);
        // Do not force exact height only.
        final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                200,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                autoSizeTextView.setLayoutParams(layoutParams);
            }
        });
        mInstrumentation.waitForIdleSync();
        final float initialTextSize = autoSizeTextView.getTextSize();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                autoSizeTextView.setMaxHeight(
                        autoSizeTextView.getHeight() / 4);
            }
        });
        mInstrumentation.waitForIdleSync();

        assertTrue(autoSizeTextView.getTextSize() < initialTextSize);
    }

    @Test
    public void testAutoSizeCallers_setMaxWidth() throws Throwable {
        final AppCompatTextView autoSizeTextView = prepareAndRetrieveAutoSizeTestData(
                R.id.textview_autosize_uniform, true);
        // Do not force exact width only.
        final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                200);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                autoSizeTextView.setLayoutParams(layoutParams);
            }
        });
        mInstrumentation.waitForIdleSync();
        final float initialTextSize = autoSizeTextView.getTextSize();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                autoSizeTextView.setMaxWidth(
                        autoSizeTextView.getWidth() / 4);
            }
        });
        mInstrumentation.waitForIdleSync();

        assertTrue(autoSizeTextView.getTextSize() != initialTextSize);
    }

    @Test
    public void testAutoSizeCallers_setWidth() throws Throwable {
        final AppCompatTextView autoSizeTextView = prepareAndRetrieveAutoSizeTestData(
                R.id.textview_autosize_uniform, true);
        // Do not force exact width only.
        final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                200);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                autoSizeTextView.setLayoutParams(layoutParams);
            }
        });
        mInstrumentation.waitForIdleSync();

        final float initialTextSize = autoSizeTextView.getTextSize();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                autoSizeTextView.setWidth(
                        autoSizeTextView.getWidth() / 4);
            }
        });
        mInstrumentation.waitForIdleSync();

        assertTrue(autoSizeTextView.getTextSize() != initialTextSize);
    }

    @Test
    public void testAutoSizeCallers_setTextSizeIsNoOp() throws Throwable {
        final AppCompatTextView autoSizeTextView = prepareAndRetrieveAutoSizeTestData(
                R.id.textview_autosize_uniform, false);
        final float initialTextSize = autoSizeTextView.getTextSize();

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                autoSizeTextView.setTextSize(initialTextSize + 123f);
            }
        });
        mInstrumentation.waitForIdleSync();

        assertEquals(initialTextSize, autoSizeTextView.getTextSize(), 0f);
    }

    @Test
    public void testAutoSizeCallers_setHorizontallyScrolling() throws Throwable {
        final AppCompatTextView autoSizeTextView = prepareAndRetrieveAutoSizeTestData(
                R.id.textview_autosize_uniform, false);
        // Horizontal scrolling is expected to be deactivated for this test.
        final float initialTextSize = autoSizeTextView.getTextSize();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                autoSizeTextView.setHorizontallyScrolling(true);
            }
        });
        mInstrumentation.waitForIdleSync();
        assertTrue(autoSizeTextView.getTextSize() > initialTextSize);

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                autoSizeTextView.setHorizontallyScrolling(false);
            }
        });
        mInstrumentation.waitForIdleSync();
        Assert.assertEquals(initialTextSize, autoSizeTextView.getTextSize(), 0f);
    }

    @Test
    public void testAutoSize_setEllipsize() throws Throwable {
        final AppCompatTextView textView = (AppCompatTextView) mActivity.findViewById(
                R.id.textview_autosize_uniform_predef_sizes);
        final int initialAutoSizeType = textView.getAutoSizeTextType();
        final int initialMinTextSize = textView.getAutoSizeMinTextSize();
        final int initialMaxTextSize = textView.getAutoSizeMaxTextSize();
        final int initialAutoSizeGranularity = textView.getAutoSizeStepGranularity();
        final int initialSizes = textView.getAutoSizeTextAvailableSizes().length;

        Assert.assertEquals(null, textView.getEllipsize());
        // Verify styled attributes.
        Assert.assertEquals(TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM, initialAutoSizeType);
        assertNotEquals(-1, initialMinTextSize);
        assertNotEquals(-1, initialMaxTextSize);
        // Because this TextView has been configured to use predefined sizes.
        Assert.assertEquals(-1, initialAutoSizeGranularity);
        assertNotEquals(0, initialSizes);

        final TextUtils.TruncateAt newEllipsizeValue = TextUtils.TruncateAt.END;
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.setEllipsize(newEllipsizeValue);
            }
        });
        mInstrumentation.waitForIdleSync();
        Assert.assertEquals(newEllipsizeValue, textView.getEllipsize());
        // Beside the ellipsis no auto-size attribute has changed.
        Assert.assertEquals(initialAutoSizeType, textView.getAutoSizeTextType());
        Assert.assertEquals(initialMinTextSize, textView.getAutoSizeMinTextSize());
        Assert.assertEquals(initialMaxTextSize, textView.getAutoSizeMaxTextSize());
        Assert.assertEquals(initialAutoSizeGranularity, textView.getAutoSizeStepGranularity());
        Assert.assertEquals(initialSizes, textView.getAutoSizeTextAvailableSizes().length);
    }

    @Test
    public void testEllipsize_setAutoSize() throws Throwable {
        final AppCompatTextView textView =
                (AppCompatTextView) mActivity.findViewById(R.id.textview_text);
        final TextUtils.TruncateAt newEllipsizeValue = TextUtils.TruncateAt.END;
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.setEllipsize(newEllipsizeValue);
            }
        });
        mInstrumentation.waitForIdleSync();
        Assert.assertEquals(newEllipsizeValue, textView.getEllipsize());
        Assert.assertEquals(TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE,
                textView.getAutoSizeTextType());
        Assert.assertEquals(-1, textView.getAutoSizeMinTextSize());
        Assert.assertEquals(-1, textView.getAutoSizeMaxTextSize());
        Assert.assertEquals(-1, textView.getAutoSizeStepGranularity());
        Assert.assertEquals(0, textView.getAutoSizeTextAvailableSizes().length);

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.setAutoSizeTextTypeWithDefaults(
                        TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM);
            }
        });
        mInstrumentation.waitForIdleSync();
        Assert.assertEquals(newEllipsizeValue, textView.getEllipsize());
        Assert.assertEquals(TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM,
                textView.getAutoSizeTextType());
        // The auto-size defaults have been used.
        assertNotEquals(-1, textView.getAutoSizeMinTextSize());
        assertNotEquals(-1, textView.getAutoSizeMaxTextSize());
        assertNotEquals(-1, textView.getAutoSizeStepGranularity());
        assertNotEquals(0, textView.getAutoSizeTextAvailableSizes().length);
    }

    @Test
    public void testAutoSizeUniform_obtainStyledAttributesUsingPredefinedSizes() {
        DisplayMetrics m = mActivity.getResources().getDisplayMetrics();
        final AppCompatTextView autoSizeTextViewUniform =
                (AppCompatTextView) mActivity.findViewById(
                        R.id.textview_autosize_uniform_predef_sizes);

        // In arrays.xml predefined the step sizes as: 5px, 11dip, 19sp, 29pt, 43mm and 53in.
        // TypedValue can not use the math library and instead rounds the value by adding
        // 0.5f when obtaining styled attributes. Check TypedValue#complexToDimensionPixelSize(...)
        int[] expectedSizesInPx = new int[] {
                (int) (0.5f + TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, 5f, m)),
                (int) (0.5f + TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 11f, m)),
                (int) (0.5f + TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 19f, m)),
                (int) (0.5f + TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PT, 29f, m)),
                (int) (0.5f + TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_MM, 43f, m)),
                (int) (0.5f + TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_IN, 53f, m))};

        boolean containsValueFromExpectedSizes = false;
        final int textSize = (int) autoSizeTextViewUniform.getTextSize();
        for (int i = 0; i < expectedSizesInPx.length; i++) {
            if (expectedSizesInPx[i] == textSize) {
                containsValueFromExpectedSizes = true;
                break;
            }
        }
        assertTrue(containsValueFromExpectedSizes);
    }

    @Test
    public void testAutoSizeUniform_obtainStyledAttributesPredefinedSizesFiltering() {
        AppCompatTextView autoSizeTextViewUniform = (AppCompatTextView) mActivity.findViewById(
                R.id.textview_autosize_uniform_predef_sizes_redundant_values);

        // In arrays.xml predefined the step sizes as: 40px, 10px, 10px, 10px, 0dp.
        final int[] expectedSizes = new int[] {10, 40};
        assertArrayEquals(expectedSizes, autoSizeTextViewUniform.getAutoSizeTextAvailableSizes());
    }

    @Test
    public void testAutoSizeUniform_predefinedSizesFilteringAndSorting() throws Throwable {
        final AppCompatTextView textView = (AppCompatTextView) mActivity.findViewById(
                R.id.textview_text);
        Assert.assertEquals(TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE,
                textView.getAutoSizeTextType());

        final int[] predefinedSizes = new int[] {400, 0, 10, 40, 10, 10, 0, 0};
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.setAutoSizeTextTypeUniformWithPresetSizes(
                        predefinedSizes, TypedValue.COMPLEX_UNIT_PX);
            }
        });
        mInstrumentation.waitForIdleSync();
        assertArrayEquals(new int[] {10, 40, 400}, textView.getAutoSizeTextAvailableSizes());
    }

    @Test(expected = NullPointerException.class)
    public void testAutoSizeUniform_predefinedSizesNullArray() throws Throwable {
        final AppCompatTextView textView = (AppCompatTextView) mActivity.findViewById(
                R.id.textview_text);
        Assert.assertEquals(TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE,
                textView.getAutoSizeTextType());

        final int[] predefinedSizes = null;
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.setAutoSizeTextTypeUniformWithPresetSizes(
                        predefinedSizes, TypedValue.COMPLEX_UNIT_PX);
            }
        });
        mInstrumentation.waitForIdleSync();
    }

    @Test
    public void testAutoSizeUniform_predefinedSizesEmptyArray() throws Throwable {
        final AppCompatTextView textView = (AppCompatTextView) mActivity.findViewById(
                R.id.textview_text);
        Assert.assertEquals(TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE,
                textView.getAutoSizeTextType());

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.setAutoSizeTextTypeWithDefaults(
                        TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM);
            }
        });
        mInstrumentation.waitForIdleSync();

        final int[] defaultSizes = textView.getAutoSizeTextAvailableSizes();
        assertNotNull(defaultSizes);
        assertTrue(defaultSizes.length > 0);

        final int[] predefinedSizes = new int[0];
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.setAutoSizeTextTypeUniformWithPresetSizes(
                        predefinedSizes, TypedValue.COMPLEX_UNIT_PX);
            }
        });
        mInstrumentation.waitForIdleSync();

        final int[] newSizes = textView.getAutoSizeTextAvailableSizes();
        assertNotNull(defaultSizes);
        assertArrayEquals(defaultSizes, newSizes);
    }

    @Test
    public void testAutoSizeUniform_buildsSizes() throws Throwable {
        final AppCompatTextView autoSizeTextViewUniform =
                (AppCompatTextView) mActivity.findViewById(
                        R.id.textview_autosize_uniform);

        // Verify that the interval limits are both included.
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                autoSizeTextViewUniform
                        .setAutoSizeTextTypeUniformWithConfiguration(10, 20, 2,
                                TypedValue.COMPLEX_UNIT_PX);
            }
        });
        mInstrumentation.waitForIdleSync();
        assertArrayEquals(
                new int[] {10, 12, 14, 16, 18, 20},
                autoSizeTextViewUniform.getAutoSizeTextAvailableSizes());

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                autoSizeTextViewUniform
                        .setAutoSizeTextTypeUniformWithConfiguration(
                                autoSizeTextViewUniform.getAutoSizeMinTextSize(),
                                19,
                                autoSizeTextViewUniform.getAutoSizeStepGranularity(),
                                TypedValue.COMPLEX_UNIT_PX);
            }
        });
        mInstrumentation.waitForIdleSync();
        assertArrayEquals(
                new int[] {10, 12, 14, 16, 18},
                autoSizeTextViewUniform.getAutoSizeTextAvailableSizes());

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                autoSizeTextViewUniform
                        .setAutoSizeTextTypeUniformWithConfiguration(
                                autoSizeTextViewUniform.getAutoSizeMinTextSize(),
                                21,
                                autoSizeTextViewUniform.getAutoSizeStepGranularity(),
                                TypedValue.COMPLEX_UNIT_PX);
            }
        });
        mInstrumentation.waitForIdleSync();
        assertArrayEquals(
                new int[] {10, 12, 14, 16, 18, 20},
                autoSizeTextViewUniform.getAutoSizeTextAvailableSizes());
    }

    @Test
    public void testAutoSizeUniform_getSetAutoSizeTextDefaults() {
        final AppCompatTextView textView = new AppCompatTextView(mActivity);
        Assert.assertEquals(TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE,
                textView.getAutoSizeTextType());
        // Min/Max/Granularity values for auto-sizing are 0 because they are not used.
        Assert.assertEquals(-1, textView.getAutoSizeMinTextSize());
        Assert.assertEquals(-1, textView.getAutoSizeMaxTextSize());
        Assert.assertEquals(-1, textView.getAutoSizeStepGranularity());

        textView.setAutoSizeTextTypeWithDefaults(TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM);
        Assert.assertEquals(TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM,
                textView.getAutoSizeTextType());
        // Min/Max default values for auto-sizing XY have been loaded.
        final int minSize = textView.getAutoSizeMinTextSize();
        final int maxSize = textView.getAutoSizeMaxTextSize();
        assertTrue(0 < minSize);
        assertTrue(minSize < maxSize);
        assertNotEquals(0, textView.getAutoSizeStepGranularity());

        textView.setAutoSizeTextTypeWithDefaults(TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE);
        Assert.assertEquals(TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE,
                textView.getAutoSizeTextType());
        // Min/Max values for auto-sizing XY have been cleared.
        Assert.assertEquals(-1, textView.getAutoSizeMinTextSize());
        Assert.assertEquals(-1, textView.getAutoSizeMaxTextSize());
        Assert.assertEquals(-1, textView.getAutoSizeStepGranularity());
    }

    @Test
    public void testAutoSizeUniform_getSetAutoSizeStepGranularity() {
        final AppCompatTextView textView = new AppCompatTextView(mActivity);
        Assert.assertEquals(TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE,
                textView.getAutoSizeTextType());
        final int initialValue = -1;
        Assert.assertEquals(initialValue, textView.getAutoSizeStepGranularity());

        textView.setAutoSizeTextTypeWithDefaults(TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM);
        Assert.assertEquals(TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM,
                textView.getAutoSizeTextType());
        final int defaultValue = 1; // 1px.
        // If the auto-size type is AUTO_SIZE_TEXT_TYPE_UNIFORM then it means textView went through
        // the auto-size setup and given that 0 is an invalid value it changed it to the default.
        Assert.assertEquals(defaultValue, textView.getAutoSizeStepGranularity());

        final int newValue = 33;
        textView.setAutoSizeTextTypeUniformWithConfiguration(
                textView.getAutoSizeMinTextSize(),
                textView.getAutoSizeMaxTextSize(),
                newValue,
                TypedValue.COMPLEX_UNIT_PX);
        Assert.assertEquals(newValue, textView.getAutoSizeStepGranularity());
    }

    @Test
    public void testAutoSizeUniform_getSetAutoSizeMinTextSize() {
        final AppCompatTextView textView = new AppCompatTextView(mActivity);
        textView.setAutoSizeTextTypeWithDefaults(TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM);
        Assert.assertEquals(TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM,
                textView.getAutoSizeTextType());
        final int minSize = textView.getAutoSizeMinTextSize();
        assertNotEquals(0, minSize);
        final int maxSize = textView.getAutoSizeMaxTextSize();
        assertNotEquals(0, maxSize);

        // This is just a test check to verify the next assertions. If this fails it is a problem
        // of this test setup (we need at least 2 units).
        assertTrue((maxSize - minSize) > 1);
        final int newMinSize = maxSize - 1;
        textView.setAutoSizeTextTypeUniformWithConfiguration(
                newMinSize,
                textView.getAutoSizeMaxTextSize(),
                textView.getAutoSizeStepGranularity(),
                TypedValue.COMPLEX_UNIT_PX);

        Assert.assertEquals(newMinSize, textView.getAutoSizeMinTextSize());
        // Max size has not changed.
        Assert.assertEquals(maxSize, textView.getAutoSizeMaxTextSize());

        textView.setAutoSizeTextTypeUniformWithConfiguration(
                newMinSize,
                newMinSize + 10,
                textView.getAutoSizeStepGranularity(),
                TypedValue.COMPLEX_UNIT_SP);

        // It does not matter which unit has been used to set the min size, the getter always
        // returns it in pixels.
        Assert.assertEquals(Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                newMinSize, mActivity.getResources().getDisplayMetrics())),
                        textView.getAutoSizeMinTextSize());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAutoSizeUniform_throwsException_whenMaxLessThanMin() {
        final AppCompatTextView textView = new AppCompatTextView(mActivity);
        textView.setAutoSizeTextTypeUniformWithConfiguration(
                10, 9, 1, TypedValue.COMPLEX_UNIT_SP);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAutoSizeUniform_throwsException_minLessThanZero() {
        final AppCompatTextView textView = new AppCompatTextView(mActivity);
        textView.setAutoSizeTextTypeUniformWithConfiguration(
                -1, 9, 1, TypedValue.COMPLEX_UNIT_SP);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAutoSizeUniform_throwsException_maxLessThanZero() {
        final AppCompatTextView textView = new AppCompatTextView(mActivity);
        textView.setAutoSizeTextTypeUniformWithConfiguration(
                10, -1, 1, TypedValue.COMPLEX_UNIT_SP);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAutoSizeUniform_throwsException_granularityLessThanZero() {
        final AppCompatTextView textView = new AppCompatTextView(mActivity);
        textView.setAutoSizeTextTypeUniformWithConfiguration(
                10, 20, -1, TypedValue.COMPLEX_UNIT_SP);
    }

    @Test
    public void testAutoSizeUniform_getSetAutoSizeMaxTextSize() {
        final AppCompatTextView textView = new AppCompatTextView(mActivity);
        textView.setAutoSizeTextTypeWithDefaults(TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM);
        Assert.assertEquals(TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM,
                textView.getAutoSizeTextType());
        final int minSize = textView.getAutoSizeMinTextSize();
        assertNotEquals(0, minSize);
        final int maxSize = textView.getAutoSizeMaxTextSize();
        assertNotEquals(0, maxSize);

        final int newMaxSize = maxSize + 11;
        textView.setAutoSizeTextTypeUniformWithConfiguration(
                textView.getAutoSizeMinTextSize(),
                newMaxSize,
                textView.getAutoSizeStepGranularity(),
                TypedValue.COMPLEX_UNIT_PX);

        Assert.assertEquals(newMaxSize, textView.getAutoSizeMaxTextSize());
        // Min size has not changed.
        Assert.assertEquals(minSize, textView.getAutoSizeMinTextSize());
        textView.setAutoSizeTextTypeUniformWithConfiguration(
                textView.getAutoSizeMinTextSize(),
                newMaxSize,
                textView.getAutoSizeStepGranularity(),
                TypedValue.COMPLEX_UNIT_SP);
        // It does not matter which unit has been used to set the max size, the getter always
        // returns it in pixels.
        Assert.assertEquals(Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                newMaxSize, mActivity.getResources().getDisplayMetrics())),
                        textView.getAutoSizeMaxTextSize());
    }

    @Test
    public void testAutoSizeUniform_autoSizeCalledWhenTypeChanged() throws Throwable {
        final AppCompatTextView textView = (AppCompatTextView) mActivity.findViewById(
                R.id.textview_text);
        // Make sure we pick an already inflated non auto-sized text view.
        Assert.assertEquals(TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE,
                textView.getAutoSizeTextType());
        // Set the text size to a very low value in order to prepare for auto-size.
        final int customTextSize = 3;
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, customTextSize);
            }
        });
        mInstrumentation.waitForIdleSync();
        Assert.assertEquals(customTextSize, textView.getTextSize(), 0f);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.setAutoSizeTextTypeWithDefaults(
                        TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM);
            }
        });
        mInstrumentation.waitForIdleSync();
        // The size of the text should have changed.
        assertNotEquals(customTextSize, textView.getTextSize(), 0f);
    }

    @Test
    public void testAutoSizeCallers_setTextSizeChangesSizeWhenAutoSizeDisabled() throws Throwable {
        final AppCompatTextView textView = (AppCompatTextView) prepareAndRetrieveAutoSizeTestData(
                R.id.textview_autosize_uniform, false);
        // Disable auto-size.
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.setAutoSizeTextTypeWithDefaults(TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE);
            }
        });
        mInstrumentation.waitForIdleSync();
        final float newTextSizeInPx = 123f;
        assertNotEquals(newTextSizeInPx, textView.getTextSize(), 0f);

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, newTextSizeInPx);
            }
        });
        mInstrumentation.waitForIdleSync();

        assertEquals(newTextSizeInPx, textView.getTextSize(), 0f);
    }

    /**
     * Some TextView attributes require non-fixed width and/or layout height. This function removes
     * all other existing views from the layout leaving only one auto-size TextView (for exercising
     * the auto-size behavior) which has been set up to suit the test needs.
     *
     * @param viewId The id of the view to prepare.
     * @param shouldWrapLayoutContent Specifies if the layout params should wrap content
     *
     * @return a TextView configured for auto size tests.
     */
    private AppCompatTextView prepareAndRetrieveAutoSizeTestData(final int viewId,
            final boolean shouldWrapLayoutContent) throws Throwable {
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                LinearLayout ll = (LinearLayout) mActivity.findViewById(
                        android.support.v7.appcompat.test.R.id.layout_textviewtest);
                AppCompatTextView targetedTextView =
                        (AppCompatTextView) mActivity.findViewById(viewId);
                ll.removeAllViews();
                ll.addView(targetedTextView);
            }
        });
        mInstrumentation.waitForIdleSync();

        final AppCompatTextView textView = mActivity.findViewById(viewId);
        if (shouldWrapLayoutContent) {
            // Do not force exact width or height.
            final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            mActivityTestRule.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    textView.setLayoutParams(layoutParams);
                }
            });
            mInstrumentation.waitForIdleSync();
        }

        return textView;
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
        assertEquals(TextView.AUTO_SIZE_TEXT_TYPE_UNIFORM, textView.getAutoSizeTextType());
        assertTrue(textView.getTransformationMethod() instanceof SingleLineTransformationMethod);
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
