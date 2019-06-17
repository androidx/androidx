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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Instrumentation;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.TextUtils;
import android.text.method.TransformationMethod;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.appcompat.test.R;
import androidx.appcompat.testutils.BaseTestActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.widget.AutoSizeableTextView;
import androidx.core.widget.TextViewCompat;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Base class for testing auto-size-text enabled views in appcompat-v7 that implement the
 * <code>AutoSizeableTextView</code> interface. Extensions of this class run all tests
 * from here and can add test cases specific to the functionality they add to the relevant
 * base view class.
 */
public abstract class AppCompatBaseAutoSizeTest<A extends BaseTestActivity,
        T extends TextView & AutoSizeableTextView> {

    @Rule
    public final ActivityTestRule<A> mActivityTestRule;

    protected A mActivity;
    protected Instrumentation mInstrumentation;
    protected ViewGroup mContainer;

    public AppCompatBaseAutoSizeTest(Class<A> clazz) {
        mActivityTestRule = new ActivityTestRule<A>(clazz);
    }

    @Before
    public void setup() {
        mInstrumentation = InstrumentationRegistry.getInstrumentation();
        mActivity = mActivityTestRule.getActivity();
        mContainer = mActivity.findViewById(R.id.container);
    }

    @Test
    @MediumTest
    @SdkSuppress(minSdkVersion = 16)
    // public TextView#getMaxLines only introduced in API 16.
    public void testAutoSizeCallers_setMaxLines() throws Throwable {
        final T autoSizeView = prepareAndRetrieveAutoSizeTestData(R.id.view_autosize_uniform,
                false);
        // Configure layout params and auto-size both in pixels to dodge flakiness on different
        // devices.
        final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                500, 500);
        final String text = "one two three four five six seven eight nine ten";
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                autoSizeView.setLayoutParams(layoutParams);
                ((AutoSizeableTextView) autoSizeView).setAutoSizeTextTypeUniformWithConfiguration(
                        1 /* autoSizeMinTextSize */,
                        5000 /* autoSizeMaxTextSize */,
                        1 /* autoSizeStepGranularity */,
                        TypedValue.COMPLEX_UNIT_PX);
                autoSizeView.setText(text);
            }
        });
        mInstrumentation.waitForIdleSync();

        float initialSize = 0;
        for (int i = 1; i < 10; i++) {
            final int maxLines = i;
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    autoSizeView.setMaxLines(maxLines);
                }
            });
            mInstrumentation.waitForIdleSync();
            float expectedSmallerSize = autoSizeView.getTextSize();
            if (i == 1) {
                initialSize = expectedSmallerSize;
            }

            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    autoSizeView.setMaxLines(maxLines + 1);
                }
            });
            mInstrumentation.waitForIdleSync();
            assertTrue(expectedSmallerSize <= autoSizeView.getTextSize());
        }
        assertTrue(initialSize < autoSizeView.getTextSize());

        initialSize = 999999;
        for (int i = 10; i > 1; i--) {
            final int maxLines = i;
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    autoSizeView.setMaxLines(maxLines);
                }
            });
            mInstrumentation.waitForIdleSync();
            float expectedLargerSize = autoSizeView.getTextSize();
            if (i == 10) {
                initialSize = expectedLargerSize;
            }

            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    autoSizeView.setMaxLines(maxLines - 1);
                }
            });
            mInstrumentation.waitForIdleSync();
            assertTrue(expectedLargerSize >= autoSizeView.getTextSize());
        }
        assertTrue(initialSize > autoSizeView.getTextSize());
    }

    @Test
    @MediumTest
    public void testAutoSizeUniform_autoSizeCalledWhenTypeChanged() throws Throwable {
        final T view = mContainer.findViewById(R.id.view_text);

        // Make sure we pick an already inflated non auto-sized text view.
        assertEquals(TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE,
                ((AutoSizeableTextView) view).getAutoSizeTextType());
        // Set the text size to a very low value in order to prepare for auto-size.
        final int customTextSize = 3;
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX, customTextSize);
            }
        });
        mInstrumentation.waitForIdleSync();
        assertEquals(customTextSize, view.getTextSize(), 0f);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((AutoSizeableTextView) view).setAutoSizeTextTypeWithDefaults(
                        TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM);
            }
        });
        mInstrumentation.waitForIdleSync();
        // The size of the text should have changed.
        assertNotEquals(customTextSize, view.getTextSize(), 0f);
    }

    @Test
    @MediumTest
    public void testAutoSizeCallers_setText() throws Throwable {
        final T autoSizeView = prepareAndRetrieveAutoSizeTestData(R.id.view_autosize_uniform,
                false);

        // Configure layout params and auto-size both in pixels to dodge flakiness on different
        // devices.
        final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                500, 500);
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                autoSizeView.setLayoutParams(layoutParams);
                ((AutoSizeableTextView) autoSizeView).setAutoSizeTextTypeUniformWithConfiguration(
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
                    autoSizeView.setText(textToSet.toString());
                }
            });

            mInstrumentation.waitForIdleSync();
            float expectedLargerSize = autoSizeView.getTextSize();
            if (i == 0) {
                initialSize = expectedLargerSize;
            }

            textToSet.append(initialText);
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    autoSizeView.setText(textToSet.toString());
                }
            });
            mInstrumentation.waitForIdleSync();
            assertTrue(expectedLargerSize >= autoSizeView.getTextSize());
        }
        assertTrue(initialSize > autoSizeView.getTextSize());

        initialSize = 9999999;
        // As we remove characters the text size expands.
        for (int i = 9; i >= 0; i--) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    autoSizeView.setText(textToSet.toString());
                }
            });
            mInstrumentation.waitForIdleSync();
            float expectedSmallerSize = autoSizeView.getTextSize();
            if (i == 0) {
                initialSize = expectedSmallerSize;
            }

            textToSet.replace((textToSet.length() - initialText.length()),
                    textToSet.length(), "");
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    autoSizeView.setText(textToSet.toString());
                }
            });
            mInstrumentation.waitForIdleSync();

            assertTrue(autoSizeView.getTextSize() >= expectedSmallerSize);
        }
        assertTrue(autoSizeView.getTextSize() > initialSize);
    }

    @Test
    @MediumTest
    public void testAutoSizeUniform_equivalentConfigurations() throws Throwable {
        final DisplayMetrics dm = mActivity.getResources().getDisplayMetrics();
        final int minTextSize = 10;
        final int maxTextSize = 20;
        final int granularity = 2;
        final int unit = TypedValue.COMPLEX_UNIT_SP;

        final T granularityView = getNewAutoSizeViewInstance();
        ((AutoSizeableTextView) granularityView).setAutoSizeTextTypeUniformWithConfiguration(
                minTextSize, maxTextSize, granularity, unit);

        final T presetView = getNewAutoSizeViewInstance();
        ((AutoSizeableTextView) presetView).setAutoSizeTextTypeUniformWithPresetSizes(
                new int[]{minTextSize, 12, 14, 16, 18, maxTextSize}, unit);

        // The TextViews have been configured differently but the end result should be nearly
        // identical.
        final int expectedAutoSizeType = AppCompatTextView.AUTO_SIZE_TEXT_TYPE_UNIFORM;
        assertEquals(expectedAutoSizeType,
                ((AutoSizeableTextView) granularityView).getAutoSizeTextType());
        assertEquals(expectedAutoSizeType,
                ((AutoSizeableTextView) presetView).getAutoSizeTextType());

        final int expectedMinTextSizeInPx = Math.round(
                TypedValue.applyDimension(unit, minTextSize, dm));
        assertEquals(expectedMinTextSizeInPx,
                ((AutoSizeableTextView) granularityView).getAutoSizeMinTextSize());
        assertEquals(expectedMinTextSizeInPx,
                ((AutoSizeableTextView) presetView).getAutoSizeMinTextSize());

        final int expectedMaxTextSizeInPx = Math.round(
                TypedValue.applyDimension(unit, maxTextSize, dm));
        assertEquals(expectedMaxTextSizeInPx,
                ((AutoSizeableTextView) granularityView).getAutoSizeMaxTextSize());
        assertEquals(expectedMaxTextSizeInPx,
                ((AutoSizeableTextView) presetView).getAutoSizeMaxTextSize());

        // Configured with granularity.
        assertEquals(Math.round(TypedValue.applyDimension(unit, granularity, dm)),
                ((AutoSizeableTextView) granularityView).getAutoSizeStepGranularity());
        // Configured with preset values, there is no granularity.
        assertEquals(-1,
                ((AutoSizeableTextView) presetView).getAutoSizeStepGranularity());

        // Both TextViews generate exactly the same sizes in pixels to choose from when auto-sizing.
        assertArrayEquals(
                ((AutoSizeableTextView) granularityView).getAutoSizeTextAvailableSizes(),
                ((AutoSizeableTextView) presetView).getAutoSizeTextAvailableSizes());

        final String someText = "This is a string";
        final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                500, 500);
        // Configure identically and attach to layout.
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                granularityView.setLayoutParams(layoutParams);
                presetView.setLayoutParams(layoutParams);

                LinearLayout ll = mActivity.findViewById(R.id.container);
                ll.removeAllViews();
                ll.addView(granularityView);
                ll.addView(presetView);

                granularityView.setText(someText);
                presetView.setText(someText);
            }
        });
        mInstrumentation.waitForIdleSync();

        assertEquals(granularityView.getTextSize(), presetView.getTextSize(), 0f);
    }

    @Test
    @MediumTest
    public void testAutoSizeCallers_setHeight() throws Throwable {
        final T autoSizeView = prepareAndRetrieveAutoSizeTestData(
                R.id.view_autosize_uniform, true);
        // Do not force exact height only.
        final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                200,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                autoSizeView.setLayoutParams(layoutParams);
            }
        });
        mInstrumentation.waitForIdleSync();
        final float initialTextSize = autoSizeView.getTextSize();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                autoSizeView.setHeight(autoSizeView.getHeight() / 4);
            }
        });
        mInstrumentation.waitForIdleSync();

        assertTrue(autoSizeView.getTextSize() < initialTextSize);
    }

    @Test
    @MediumTest
    public void testAutoSizeCallers_setTransformationMethod() throws Throwable {
        final T autoSizeView = prepareAndRetrieveAutoSizeTestData(R.id.view_autosize_uniform,
                false);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                autoSizeView.setTransformationMethod(null);
            }
        });
        mInstrumentation.waitForIdleSync();
        final float initialTextSize = autoSizeView.getTextSize();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                autoSizeView.setTransformationMethod(new DoubleTextTransformationMethod());
            }
        });
        mInstrumentation.waitForIdleSync();

        assertTrue(autoSizeView.getTextSize() < initialTextSize);
    }

    @Test
    @MediumTest
    public void testAutoSizeCallers_setCompoundDrawables() throws Throwable {
        final T autoSizeView = prepareAndRetrieveAutoSizeTestData(R.id.view_autosize_uniform,
                false);
        final float initialTextSize = autoSizeView.getTextSize();
        final Drawable drawable = ResourcesCompat.getDrawable(mActivity.getResources(),
                R.drawable.test_drawable_red, null);
        drawable.setBounds(0, 0, autoSizeView.getWidth() / 3, autoSizeView.getHeight() / 3);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                autoSizeView.setCompoundDrawables(drawable, drawable, drawable, drawable);
            }
        });
        mInstrumentation.waitForIdleSync();

        assertTrue(autoSizeView.getTextSize() < initialTextSize);
    }

    @Test
    @MediumTest
    public void testAutoSizeCallers_setCompoundDrawablesRelative() throws Throwable {
        if (Build.VERSION.SDK_INT >= 17) {
            final T autoSizeView = prepareAndRetrieveAutoSizeTestData(
                    R.id.view_autosize_uniform, false);
            final float initialTextSize = autoSizeView.getTextSize();
            final Drawable drawable = ResourcesCompat.getDrawable(mActivity.getResources(),
                    R.drawable.test_drawable_red, null);
            drawable.setBounds(0, 0, autoSizeView.getWidth() / 3,
                    autoSizeView.getHeight() / 3);
            mActivityTestRule.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (Build.VERSION.SDK_INT >= 17) {
                        autoSizeView.setCompoundDrawablesRelative(
                                drawable, drawable, drawable, drawable);
                    }
                }
            });
            mInstrumentation.waitForIdleSync();

            assertTrue(autoSizeView.getTextSize() < initialTextSize);
        }
    }

    @Test
    @MediumTest
    public void testAutoSizeCallers_setCompoundDrawablePadding() throws Throwable {
        final T autoSizeView = prepareAndRetrieveAutoSizeTestData(R.id.view_autosize_uniform,
                false);
        // Prepare a larger layout in order not to hit the min value easily.
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                autoSizeView.setWidth(autoSizeView.getWidth() * 2);
                autoSizeView.setHeight(autoSizeView.getHeight() * 2);
            }
        });
        mInstrumentation.waitForIdleSync();
        // Setup the drawables before setting their padding in order to modify the available
        // space and trigger a resize.
        final Drawable drawable = ResourcesCompat.getDrawable(mActivity.getResources(),
                R.drawable.test_drawable_red, null);
        drawable.setBounds(0, 0, autoSizeView.getWidth() / 4, autoSizeView.getHeight() / 4);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                autoSizeView.setCompoundDrawables(
                        drawable, drawable, drawable, drawable);
            }
        });
        mInstrumentation.waitForIdleSync();
        final float initialTextSize = autoSizeView.getTextSize();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                autoSizeView.setCompoundDrawablePadding(
                        autoSizeView.getCompoundDrawablePadding() + 10);
            }
        });
        mInstrumentation.waitForIdleSync();

        assertTrue(autoSizeView.getTextSize() < initialTextSize);
    }

    @Test
    @MediumTest
    public void testAutoSizeCallers_setPadding() throws Throwable {
        final T autoSizeView = prepareAndRetrieveAutoSizeTestData(R.id.view_autosize_uniform,
                false);
        final float initialTextSize = autoSizeView.getTextSize();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                autoSizeView.setPadding(
                        autoSizeView.getWidth() / 3, autoSizeView.getHeight() / 3,
                        autoSizeView.getWidth() / 3, autoSizeView.getHeight() / 3);
            }
        });
        mInstrumentation.waitForIdleSync();

        assertTrue(autoSizeView.getTextSize() < initialTextSize);
    }

    @Test
    @MediumTest
    public void testAutoSizeCallers_setPaddingRelative() throws Throwable {
        if (Build.VERSION.SDK_INT >= 16) {
            final T autoSizeView = prepareAndRetrieveAutoSizeTestData(
                    R.id.view_autosize_uniform, false);
            final float initialTextSize = autoSizeView.getTextSize();

            mActivityTestRule.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (Build.VERSION.SDK_INT > 16) {
                        autoSizeView.setPaddingRelative(
                                autoSizeView.getWidth() / 3, autoSizeView.getHeight() / 3,
                                autoSizeView.getWidth() / 3, autoSizeView.getHeight() / 3);
                    }
                }
            });
            mInstrumentation.waitForIdleSync();

            assertTrue(autoSizeView.getTextSize() < initialTextSize);
        }
    }

    @Test
    @MediumTest
    public void testAutoSizeCallers_setTypeface() throws Throwable {
        final T autoSizeView = prepareAndRetrieveAutoSizeTestData(R.id.view_autosize_uniform,
                false);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                autoSizeView.setText("The typeface change needs a bit more text then "
                        + "the default used for this batch of tests in order to get to resize text."
                        + " The resize function is always called but even with different typefaces "
                        + "there may not be a need to resize text because it just fits. The longer "
                        + "the text, the higher the chance for a resize. And here is yet another "
                        + "sentence to make sure this test is not flaky. Not flaky at all.");
            }
        });
        mInstrumentation.waitForIdleSync();
        final float initialTextSize = autoSizeView.getTextSize();

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Typeface differentTypeface = Typeface.MONOSPACE;
                if (autoSizeView.getTypeface() == Typeface.MONOSPACE) {
                    differentTypeface = Typeface.SANS_SERIF;
                }
                autoSizeView.setTypeface(differentTypeface);
            }
        });
        mInstrumentation.waitForIdleSync();
        final float changedTextSize = autoSizeView.getTextSize();

        // Don't really know if it is larger or smaller (depends on the typeface chosen above),
        // but it should definitely have changed.
        assertNotEquals(initialTextSize, changedTextSize, 0f);

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                autoSizeView.setTypeface(autoSizeView.getTypeface());
            }
        });
        mInstrumentation.waitForIdleSync();

        assertEquals(changedTextSize, autoSizeView.getTextSize(), 0f);
    }

    @Test
    @MediumTest
    public void testAutoSizeCallers_setLetterSpacing() throws Throwable {
        if (Build.VERSION.SDK_INT >= 21) {
            final T autoSizeView = prepareAndRetrieveAutoSizeTestData(
                    R.id.view_autosize_uniform, false);
            final float initialTextSize = autoSizeView.getTextSize();

            mActivityTestRule.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (Build.VERSION.SDK_INT >= 21) {
                        autoSizeView.setLetterSpacing(
                                autoSizeView.getLetterSpacing() * 1.5f + 4.5f);
                    }
                }
            });
            mInstrumentation.waitForIdleSync();
            final float changedTextSize = autoSizeView.getTextSize();

            assertTrue(changedTextSize < initialTextSize);

            mActivityTestRule.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (Build.VERSION.SDK_INT >= 21) {
                        autoSizeView.setLetterSpacing(autoSizeView.getLetterSpacing());
                    }
                }
            });
            mInstrumentation.waitForIdleSync();

            assertEquals(changedTextSize, autoSizeView.getTextSize(), 0f);
        }
    }

    @Test
    @MediumTest
    public void testAutoSizeCallers_setMaxHeight() throws Throwable {
        final T autoSizeView = prepareAndRetrieveAutoSizeTestData(R.id.view_autosize_uniform,
                true);
        // Do not force exact height only.
        final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                200,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                autoSizeView.setLayoutParams(layoutParams);
            }
        });
        mInstrumentation.waitForIdleSync();
        final float initialTextSize = autoSizeView.getTextSize();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                autoSizeView.setMaxHeight(autoSizeView.getHeight() / 4);
            }
        });
        mInstrumentation.waitForIdleSync();

        assertTrue(autoSizeView.getTextSize() < initialTextSize);
    }

    @Test
    @MediumTest
    public void testAutoSizeCallers_setMaxWidth() throws Throwable {
        final T autoSizeView = prepareAndRetrieveAutoSizeTestData(R.id.view_autosize_uniform,
                true);
        // Do not force exact width only.
        final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                200);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                autoSizeView.setLayoutParams(layoutParams);
            }
        });
        mInstrumentation.waitForIdleSync();
        final float initialTextSize = autoSizeView.getTextSize();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                autoSizeView.setMaxWidth(autoSizeView.getWidth() / 4);
            }
        });
        mInstrumentation.waitForIdleSync();

        assertTrue(autoSizeView.getTextSize() != initialTextSize);
    }

    @Test
    @MediumTest
    public void testAutoSizeCallers_setWidth() throws Throwable {
        final T autoSizeView = prepareAndRetrieveAutoSizeTestData(R.id.view_autosize_uniform,
                true);
        // Do not force exact width only.
        final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                200);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                autoSizeView.setLayoutParams(layoutParams);
            }
        });
        mInstrumentation.waitForIdleSync();

        final float initialTextSize = autoSizeView.getTextSize();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                autoSizeView.setWidth(autoSizeView.getWidth() / 4);
            }
        });
        mInstrumentation.waitForIdleSync();

        assertTrue(autoSizeView.getTextSize() != initialTextSize);
    }

    @Test
    @MediumTest
    public void testAutoSizeCallers_setTextSizeIsNoOp() throws Throwable {
        final T autoSizeView = prepareAndRetrieveAutoSizeTestData(R.id.view_autosize_uniform,
                false);
        final float initialTextSize = autoSizeView.getTextSize();

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                autoSizeView.setTextSize(initialTextSize + 123f);
            }
        });
        mInstrumentation.waitForIdleSync();

        assertEquals(initialTextSize, autoSizeView.getTextSize(), 0f);
    }

    @Test
    @MediumTest
    public void testAutoSizeCallers_setHorizontallyScrolling() throws Throwable {
        final T autoSizeView = prepareAndRetrieveAutoSizeTestData(R.id.view_autosize_uniform,
                false);
        // Horizontal scrolling is expected to be deactivated for this test.
        final float initialTextSize = autoSizeView.getTextSize();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                autoSizeView.setHorizontallyScrolling(true);
            }
        });
        mInstrumentation.waitForIdleSync();
        assertTrue(autoSizeView.getTextSize() > initialTextSize);

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                autoSizeView.setHorizontallyScrolling(false);
            }
        });
        mInstrumentation.waitForIdleSync();
        assertEquals(initialTextSize, autoSizeView.getTextSize(), 0f);
    }

    @Test
    @MediumTest
    public void testAutoSize_setEllipsize() throws Throwable {
        final T autoSizeView = mActivity.findViewById(R.id.view_autosize_uniform_predef_sizes);
        final int initialAutoSizeType = ((AutoSizeableTextView) autoSizeView).getAutoSizeTextType();
        final int initialMinTextSize =
                ((AutoSizeableTextView) autoSizeView).getAutoSizeMinTextSize();
        final int initialMaxTextSize =
                ((AutoSizeableTextView) autoSizeView).getAutoSizeMaxTextSize();
        final int initialAutoSizeGranularity =
                ((AutoSizeableTextView) autoSizeView).getAutoSizeStepGranularity();
        final int initialSizes =
                ((AutoSizeableTextView) autoSizeView).getAutoSizeTextAvailableSizes().length;

        assertEquals(null, autoSizeView.getEllipsize());
        // Verify styled attributes.
        assertEquals(TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM, initialAutoSizeType);
        assertNotEquals(-1, initialMinTextSize);
        assertNotEquals(-1, initialMaxTextSize);
        // Because this TextView has been configured to use predefined sizes.
        assertEquals(-1, initialAutoSizeGranularity);
        assertNotEquals(0, initialSizes);

        final TextUtils.TruncateAt newEllipsizeValue = TextUtils.TruncateAt.END;
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                autoSizeView.setEllipsize(newEllipsizeValue);
            }
        });
        mInstrumentation.waitForIdleSync();
        assertEquals(newEllipsizeValue, autoSizeView.getEllipsize());
        // Beside the ellipsis no auto-size attribute has changed.
        assertEquals(initialAutoSizeType,
                ((AutoSizeableTextView) autoSizeView).getAutoSizeTextType());
        assertEquals(initialMinTextSize,
                ((AutoSizeableTextView) autoSizeView).getAutoSizeMinTextSize());
        assertEquals(initialMaxTextSize,
                ((AutoSizeableTextView) autoSizeView).getAutoSizeMaxTextSize());
        assertEquals(initialAutoSizeGranularity,
                ((AutoSizeableTextView) autoSizeView).getAutoSizeStepGranularity());
        assertEquals(initialSizes,
                ((AutoSizeableTextView) autoSizeView).getAutoSizeTextAvailableSizes().length);
    }

    @Test
    @MediumTest
    public void testEllipsize_setAutoSize() throws Throwable {
        final T view = mActivity.findViewById(R.id.view_text);
        final TextUtils.TruncateAt newEllipsizeValue = TextUtils.TruncateAt.END;
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                view.setEllipsize(newEllipsizeValue);
            }
        });
        mInstrumentation.waitForIdleSync();
        assertEquals(newEllipsizeValue, view.getEllipsize());
        assertEquals(TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE,
                ((AutoSizeableTextView) view).getAutoSizeTextType());
        assertEquals(-1, ((AutoSizeableTextView) view).getAutoSizeMinTextSize());
        assertEquals(-1, ((AutoSizeableTextView) view).getAutoSizeMaxTextSize());
        assertEquals(-1, ((AutoSizeableTextView) view).getAutoSizeStepGranularity());
        assertEquals(0,
                ((AutoSizeableTextView) view).getAutoSizeTextAvailableSizes().length);

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((AutoSizeableTextView) view).setAutoSizeTextTypeWithDefaults(
                        TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM);
            }
        });
        mInstrumentation.waitForIdleSync();
        assertEquals(newEllipsizeValue, view.getEllipsize());
        assertEquals(TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM,
                ((AutoSizeableTextView) view).getAutoSizeTextType());
        // The auto-size defaults have been used.
        assertNotEquals(-1, ((AutoSizeableTextView) view).getAutoSizeMinTextSize());
        assertNotEquals(-1, ((AutoSizeableTextView) view).getAutoSizeMaxTextSize());
        assertNotEquals(-1, ((AutoSizeableTextView) view).getAutoSizeStepGranularity());
        assertNotEquals(0,
                ((AutoSizeableTextView) view).getAutoSizeTextAvailableSizes().length);
    }

    @Test
    @MediumTest
    public void testAutoSizeUniform_obtainStyledAttributesUsingPredefinedSizes() {
        DisplayMetrics m = mActivity.getResources().getDisplayMetrics();
        final T autoSizeViewUniform = mActivity.findViewById(
                R.id.view_autosize_uniform_predef_sizes);

        // In arrays.xml predefined the step sizes as: 5px, 11dip, 19sp, 29pt, 43mm and 53in.
        // TypedValue can not use the math library and instead rounds the value by adding
        // 0.5f when obtaining styled attributes. Check TypedValue#complexToDimensionPixelSize(...)
        int[] expectedSizesInPx = new int[] {
                Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, 5f, m)),
                Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 11f, m)),
                Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 19f, m)),
                Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PT, 29f, m)),
                Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_MM, 43f, m)),
                Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_IN, 53f, m))};

        boolean containsValueFromExpectedSizes = false;
        final int textSize = (int) autoSizeViewUniform.getTextSize();
        for (int i = 0; i < expectedSizesInPx.length; i++) {
            if (expectedSizesInPx[i] == textSize) {
                containsValueFromExpectedSizes = true;
                break;
            }
        }
        assertTrue(containsValueFromExpectedSizes);
    }

    @Test
    @MediumTest
    public void testAutoSizeUniform_obtainStyledAttributesPredefinedSizesFiltering() {
        T autoSizeViewUniform = mActivity.findViewById(
                R.id.view_autosize_uniform_predef_sizes_redundant_values);

        // In arrays.xml predefined the step sizes as: 40px, 10px, 10px, 10px, 0dp.
        final int[] expectedSizes = new int[] {10, 40};
        assertArrayEquals(expectedSizes,
                ((AutoSizeableTextView) autoSizeViewUniform).getAutoSizeTextAvailableSizes());
    }

    @Test
    @MediumTest
    public void testAutoSizeUniform_predefinedSizesFilteringAndSorting() throws Throwable {
        final T view = mActivity.findViewById(R.id.view_text);
        assertEquals(TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE,
                ((AutoSizeableTextView) view).getAutoSizeTextType());

        final int[] predefinedSizes = new int[] {400, 0, 10, 40, 10, 10, 0, 0};
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((AutoSizeableTextView) view).setAutoSizeTextTypeUniformWithPresetSizes(
                        predefinedSizes, TypedValue.COMPLEX_UNIT_PX);
            }
        });
        mInstrumentation.waitForIdleSync();
        assertArrayEquals(new int[] {10, 40, 400},
                ((AutoSizeableTextView) view).getAutoSizeTextAvailableSizes());
    }

    @Test(expected = NullPointerException.class)
    @SmallTest
    public void testAutoSizeUniform_predefinedSizesNullArray() throws Throwable {
        final T view = mActivity.findViewById(R.id.view_text);
        assertEquals(TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE,
                ((AutoSizeableTextView) view).getAutoSizeTextType());

        final int[] predefinedSizes = null;
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((AutoSizeableTextView) view).setAutoSizeTextTypeUniformWithPresetSizes(
                        predefinedSizes, TypedValue.COMPLEX_UNIT_PX);
            }
        });
        mInstrumentation.waitForIdleSync();
    }

    @Test
    @MediumTest
    public void testAutoSizeUniform_predefinedSizesEmptyArray() throws Throwable {
        final T view = mActivity.findViewById(R.id.view_text);
        assertEquals(TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE,
                ((AutoSizeableTextView) view).getAutoSizeTextType());

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((AutoSizeableTextView) view).setAutoSizeTextTypeWithDefaults(
                        TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM);
            }
        });
        mInstrumentation.waitForIdleSync();

        final int[] defaultSizes = ((AutoSizeableTextView) view).getAutoSizeTextAvailableSizes();
        assertNotNull(defaultSizes);
        assertTrue(defaultSizes.length > 0);

        final int[] predefinedSizes = new int[0];
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((AutoSizeableTextView) view).setAutoSizeTextTypeUniformWithPresetSizes(
                        predefinedSizes, TypedValue.COMPLEX_UNIT_PX);
            }
        });
        mInstrumentation.waitForIdleSync();

        final int[] newSizes = ((AutoSizeableTextView) view).getAutoSizeTextAvailableSizes();
        assertNotNull(defaultSizes);
        assertArrayEquals(defaultSizes, newSizes);
    }

    @Test
    @MediumTest
    public void testAutoSizeUniform_buildsSizes() throws Throwable {
        final T autoSizeViewUniform = mActivity.findViewById(R.id.view_autosize_uniform);

        // Verify that the interval limits are both included.
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((AutoSizeableTextView) autoSizeViewUniform)
                        .setAutoSizeTextTypeUniformWithConfiguration(10, 20, 2,
                                TypedValue.COMPLEX_UNIT_PX);
            }
        });
        mInstrumentation.waitForIdleSync();
        assertArrayEquals(
                new int[] {10, 12, 14, 16, 18, 20},
                ((AutoSizeableTextView) autoSizeViewUniform).getAutoSizeTextAvailableSizes());

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((AutoSizeableTextView) autoSizeViewUniform)
                        .setAutoSizeTextTypeUniformWithConfiguration(
                                ((AutoSizeableTextView) autoSizeViewUniform)
                                        .getAutoSizeMinTextSize(),
                                19,
                                ((AutoSizeableTextView) autoSizeViewUniform)
                                        .getAutoSizeStepGranularity(),
                                TypedValue.COMPLEX_UNIT_PX);
            }
        });
        mInstrumentation.waitForIdleSync();
        assertArrayEquals(
                new int[] {10, 12, 14, 16, 18},
                ((AutoSizeableTextView) autoSizeViewUniform).getAutoSizeTextAvailableSizes());

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((AutoSizeableTextView) autoSizeViewUniform)
                        .setAutoSizeTextTypeUniformWithConfiguration(
                                ((AutoSizeableTextView) autoSizeViewUniform)
                                        .getAutoSizeMinTextSize(),
                                21,
                                ((AutoSizeableTextView) autoSizeViewUniform)
                                        .getAutoSizeStepGranularity(),
                                TypedValue.COMPLEX_UNIT_PX);
            }
        });
        mInstrumentation.waitForIdleSync();
        assertArrayEquals(
                new int[] {10, 12, 14, 16, 18, 20},
                ((AutoSizeableTextView) autoSizeViewUniform).getAutoSizeTextAvailableSizes());
    }

    @Test
    @MediumTest
    public void testAutoSizeUniform_getSetAutoSizeTextDefaults() {
        final T autoSizeView = getNewAutoSizeViewInstance();
        assertEquals(TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE,
                ((AutoSizeableTextView) autoSizeView).getAutoSizeTextType());
        // Min/Max/Granularity values for auto-sizing are 0 because they are not used.
        assertEquals(-1, ((AutoSizeableTextView) autoSizeView).getAutoSizeMinTextSize());
        assertEquals(-1, ((AutoSizeableTextView) autoSizeView).getAutoSizeMaxTextSize());
        assertEquals(-1,
                ((AutoSizeableTextView) autoSizeView).getAutoSizeStepGranularity());

        ((AutoSizeableTextView) autoSizeView).setAutoSizeTextTypeWithDefaults(
                TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM);
        assertEquals(TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM,
                ((AutoSizeableTextView) autoSizeView).getAutoSizeTextType());
        // Min/Max default values for auto-sizing XY have been loaded.
        final int minSize = ((AutoSizeableTextView) autoSizeView).getAutoSizeMinTextSize();
        final int maxSize = ((AutoSizeableTextView) autoSizeView).getAutoSizeMaxTextSize();
        assertTrue(0 < minSize);
        assertTrue(minSize < maxSize);
        assertNotEquals(0,
                ((AutoSizeableTextView) autoSizeView).getAutoSizeStepGranularity());

        ((AutoSizeableTextView) autoSizeView)
                .setAutoSizeTextTypeWithDefaults(TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE);
        assertEquals(TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE,
                ((AutoSizeableTextView) autoSizeView).getAutoSizeTextType());
        // Min/Max values for auto-sizing XY have been cleared.
        assertEquals(-1, ((AutoSizeableTextView) autoSizeView).getAutoSizeMinTextSize());
        assertEquals(-1, ((AutoSizeableTextView) autoSizeView).getAutoSizeMaxTextSize());
        assertEquals(-1,
                ((AutoSizeableTextView) autoSizeView).getAutoSizeStepGranularity());
    }

    @Test
    @MediumTest
    public void testAutoSizeUniform_getSetAutoSizeStepGranularity() {
        final T autoSizeView = getNewAutoSizeViewInstance();
        assertEquals(TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE,
                ((AutoSizeableTextView) autoSizeView).getAutoSizeTextType());
        final int initialValue = -1;
        assertEquals(initialValue,
                ((AutoSizeableTextView) autoSizeView).getAutoSizeStepGranularity());

        ((AutoSizeableTextView) autoSizeView).setAutoSizeTextTypeWithDefaults(
                TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM);
        assertEquals(TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM,
                ((AutoSizeableTextView) autoSizeView).getAutoSizeTextType());
        final int defaultValue = 1; // 1px.
        // If the auto-size type is AUTO_SIZE_TEXT_TYPE_UNIFORM then it means autoSizeView went
        // through the auto-size setup and given that 0 is an invalid value it changed it to the
        // default.
        assertEquals(defaultValue,
                ((AutoSizeableTextView) autoSizeView).getAutoSizeStepGranularity());

        final int newValue = 33;
        ((AutoSizeableTextView) autoSizeView).setAutoSizeTextTypeUniformWithConfiguration(
                ((AutoSizeableTextView) autoSizeView).getAutoSizeMinTextSize(),
                ((AutoSizeableTextView) autoSizeView).getAutoSizeMaxTextSize(),
                newValue,
                TypedValue.COMPLEX_UNIT_PX);
        assertEquals(newValue, ((AutoSizeableTextView) autoSizeView).getAutoSizeStepGranularity());
    }

    @Test
    @MediumTest
    public void testAutoSizeUniform_getSetAutoSizeMinTextSize() {
        final T autoSizeView = getNewAutoSizeViewInstance();
        ((AutoSizeableTextView) autoSizeView).setAutoSizeTextTypeWithDefaults(
                TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM);
        assertEquals(TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM,
                ((AutoSizeableTextView) autoSizeView).getAutoSizeTextType());
        final int minSize = ((AutoSizeableTextView) autoSizeView).getAutoSizeMinTextSize();
        assertNotEquals(0, minSize);
        final int maxSize = ((AutoSizeableTextView) autoSizeView).getAutoSizeMaxTextSize();
        assertNotEquals(0, maxSize);

        // This is just a test check to verify the next assertions. If this fails it is a problem
        // of this test setup (we need at least 2 units).
        assertTrue((maxSize - minSize) > 1);
        final int newMinSize = maxSize - 1;
        ((AutoSizeableTextView) autoSizeView).setAutoSizeTextTypeUniformWithConfiguration(
                newMinSize,
                ((AutoSizeableTextView) autoSizeView).getAutoSizeMaxTextSize(),
                ((AutoSizeableTextView) autoSizeView).getAutoSizeStepGranularity(),
                TypedValue.COMPLEX_UNIT_PX);

        assertEquals(newMinSize, ((AutoSizeableTextView) autoSizeView).getAutoSizeMinTextSize());
        // Max size has not changed.
        assertEquals(maxSize, ((AutoSizeableTextView) autoSizeView).getAutoSizeMaxTextSize());

        ((AutoSizeableTextView) autoSizeView).setAutoSizeTextTypeUniformWithConfiguration(
                newMinSize,
                newMinSize + 10,
                ((AutoSizeableTextView) autoSizeView).getAutoSizeStepGranularity(),
                TypedValue.COMPLEX_UNIT_SP);

        // It does not matter which unit has been used to set the min size, the getter always
        // returns it in pixels.
        assertEquals(Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                newMinSize, mActivity.getResources().getDisplayMetrics())),
                ((AutoSizeableTextView) autoSizeView).getAutoSizeMinTextSize());
    }

    @Test(expected = IllegalArgumentException.class)
    @SmallTest
    public void testAutoSizeUniform_throwsException_whenMaxLessThanMin() {
        final T autoSizeView = getNewAutoSizeViewInstance();
        ((AutoSizeableTextView) autoSizeView).setAutoSizeTextTypeUniformWithConfiguration(
                10, 9, 1, TypedValue.COMPLEX_UNIT_SP);
    }

    @Test(expected = IllegalArgumentException.class)
    @SmallTest
    public void testAutoSizeUniform_throwsException_minLessThanZero() {
        final T autoSizeView = getNewAutoSizeViewInstance();
        ((AutoSizeableTextView) autoSizeView).setAutoSizeTextTypeUniformWithConfiguration(
                -1, 9, 1, TypedValue.COMPLEX_UNIT_SP);
    }

    @Test(expected = IllegalArgumentException.class)
    @SmallTest
    public void testAutoSizeUniform_throwsException_maxLessThanZero() {
        final T autoSizeView = getNewAutoSizeViewInstance();
        ((AutoSizeableTextView) autoSizeView).setAutoSizeTextTypeUniformWithConfiguration(
                10, -1, 1, TypedValue.COMPLEX_UNIT_SP);
    }

    @Test(expected = IllegalArgumentException.class)
    @SmallTest
    public void testAutoSizeUniform_throwsException_granularityLessThanZero() {
        final T autoSizeView = getNewAutoSizeViewInstance();
        ((AutoSizeableTextView) autoSizeView).setAutoSizeTextTypeUniformWithConfiguration(
                10, 20, -1, TypedValue.COMPLEX_UNIT_SP);
    }

    @Test
    @MediumTest
    public void testAutoSizeUniform_getSetAutoSizeMaxTextSize() {
        final T autoSizeView = getNewAutoSizeViewInstance();
        ((AutoSizeableTextView) autoSizeView).setAutoSizeTextTypeWithDefaults(
                TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM);
        assertEquals(TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM,
                ((AutoSizeableTextView) autoSizeView).getAutoSizeTextType());
        final int minSize = ((AutoSizeableTextView) autoSizeView).getAutoSizeMinTextSize();
        assertNotEquals(0, minSize);
        final int maxSize = ((AutoSizeableTextView) autoSizeView).getAutoSizeMaxTextSize();
        assertNotEquals(0, maxSize);

        final int newMaxSize = maxSize + 11;
        ((AutoSizeableTextView) autoSizeView).setAutoSizeTextTypeUniformWithConfiguration(
                ((AutoSizeableTextView) autoSizeView).getAutoSizeMinTextSize(),
                newMaxSize,
                ((AutoSizeableTextView) autoSizeView).getAutoSizeStepGranularity(),
                TypedValue.COMPLEX_UNIT_PX);

        assertEquals(newMaxSize, ((AutoSizeableTextView) autoSizeView).getAutoSizeMaxTextSize());
        // Min size has not changed.
        assertEquals(minSize, ((AutoSizeableTextView) autoSizeView).getAutoSizeMinTextSize());
        ((AutoSizeableTextView) autoSizeView).setAutoSizeTextTypeUniformWithConfiguration(
                ((AutoSizeableTextView) autoSizeView).getAutoSizeMinTextSize(),
                newMaxSize,
                ((AutoSizeableTextView) autoSizeView).getAutoSizeStepGranularity(),
                TypedValue.COMPLEX_UNIT_SP);
        // It does not matter which unit has been used to set the max size, the getter always
        // returns it in pixels.
        assertEquals(Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                newMaxSize, mActivity.getResources().getDisplayMetrics())),
                ((AutoSizeableTextView) autoSizeView).getAutoSizeMaxTextSize());
    }

    @Test
    @MediumTest
    public void testAutoSizeCallers_setTextSizeChangesSizeWhenAutoSizeDisabled() throws Throwable {
        final T autoSizeView = prepareAndRetrieveAutoSizeTestData(R.id.view_autosize_uniform,
                false);
        // Disable auto-size.
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((AutoSizeableTextView) autoSizeView).setAutoSizeTextTypeWithDefaults(
                        TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE);
            }
        });
        mInstrumentation.waitForIdleSync();

        final float newTextSizeInPx = autoSizeView.getTextSize() + 10f;
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                autoSizeView.setTextSize(TypedValue.COMPLEX_UNIT_PX, newTextSizeInPx);
            }
        });
        mInstrumentation.waitForIdleSync();

        assertEquals(newTextSizeInPx, autoSizeView.getTextSize(), 0f);
    }

    /**
     * Some View attributes require non-fixed width and/or layout height. This function removes
     * all other existing views from the layout leaving only one auto-size TextView (for exercising
     * the auto-size behavior) which has been set up to suit the test needs.
     *
     * @param viewId The id of the view to prepare.
     * @param shouldWrapLayoutContent Specifies if the layout params should wrap content
     *
     * @return a View configured for auto size tests.
     */
    private T prepareAndRetrieveAutoSizeTestData(final @IdRes int viewId,
            final boolean shouldWrapLayoutContent) throws Throwable {
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                LinearLayout ll = mActivity.findViewById(R.id.container);
                T targetedView = mActivity.findViewById(viewId);
                ll.removeAllViews();
                ll.addView(targetedView);
            }
        });
        mInstrumentation.waitForIdleSync();

        final T view = mActivity.findViewById(viewId);
        if (shouldWrapLayoutContent) {
            // Do not force exact width or height.
            final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            mActivityTestRule.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    view.setLayoutParams(layoutParams);
                }
            });
            mInstrumentation.waitForIdleSync();
        }

        return view;
    }

    /* Transformation method which duplicates text. */
    private final class DoubleTextTransformationMethod implements TransformationMethod {
        DoubleTextTransformationMethod() {
            /* Nothing to do. */
        }

        @Override
        public CharSequence getTransformation(CharSequence source, View view) {
            return new StringBuilder().append(source).append(source).toString();
        }

        @Override
        public void onFocusChanged(View view, CharSequence sourceText, boolean focused,
                int direction, Rect previouslyFocusedRect) {
            /* Nothing to do. */
        }
    }

    // Returns a new instance of the auto-sizable view for mActivity.
    protected abstract T getNewAutoSizeViewInstance();
}
