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

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.test.filters.MediumTest;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.BaseInstrumentationTestCase;
import android.support.v7.appcompat.test.R;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.junit.Test;

@MediumTest
public class AppCompatTextViewAutoSizeTest extends
        BaseInstrumentationTestCase<AppCompatTextViewAutoSizeActivity> {

    public AppCompatTextViewAutoSizeTest() {
        super(AppCompatTextViewAutoSizeActivity.class);
    }

    @Test
    public void testAutoSize_notSupportedByEditText() throws Throwable {
        final TextView autoSizeEditText = prepareAndRetrieveAutoSizeTestData(
                R.id.edittext_autosize_uniform, true);
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
        getInstrumentation().waitForIdleSync();
        final float initialTextSize = autoSizeEditText.getTextSize();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                autoSizeEditText.setHeight(autoSizeEditText.getHeight() / 4);
            }
        });
        getInstrumentation().waitForIdleSync();

        assertEquals(initialTextSize, autoSizeEditText.getTextSize(), 0f);
    }

    @Test
    public void testAutoSizeCallers_setHeight() throws Throwable {
        final TextView autoSizeTextView = prepareAndRetrieveAutoSizeTestData(
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
        getInstrumentation().waitForIdleSync();
        final float initialTextSize = autoSizeTextView.getTextSize();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                autoSizeTextView.setHeight(autoSizeTextView.getHeight() / 4);
            }
        });
        getInstrumentation().waitForIdleSync();

        assertTrue(autoSizeTextView.getTextSize() < initialTextSize);
    }

    @Test
    public void testAutoSizeCallers_setCompoundDrawables() throws Throwable {
        final TextView autoSizeTextView = prepareAndRetrieveAutoSizeTestData(
                R.id.textview_autosize_uniform, false);
        final float initialTextSize = autoSizeTextView.getTextSize();
        final Drawable drawable = ResourcesCompat.getDrawable(getActivity().getResources(),
                R.drawable.test_drawable_red, null);
        drawable.setBounds(0, 0, autoSizeTextView.getWidth() / 3, autoSizeTextView.getHeight() / 3);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                autoSizeTextView.setCompoundDrawables(drawable, drawable, drawable, drawable);
            }
        });
        getInstrumentation().waitForIdleSync();

        assertTrue(autoSizeTextView.getTextSize() < initialTextSize);
    }

    @Test
    public void testAutoSizeCallers_setCompoundDrawablesRelative() throws Throwable {
        if (Build.VERSION.SDK_INT >= 17) {
            final TextView autoSizeTextView = prepareAndRetrieveAutoSizeTestData(
                    R.id.textview_autosize_uniform, false);
            final float initialTextSize = autoSizeTextView.getTextSize();
            final Drawable drawable = ResourcesCompat.getDrawable(getActivity().getResources(),
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
            getInstrumentation().waitForIdleSync();

            assertTrue(autoSizeTextView.getTextSize() < initialTextSize);
        }
    }

    @Test
    public void testAutoSizeCallers_setCompoundDrawablePadding() throws Throwable {
        final TextView autoSizeTextView = prepareAndRetrieveAutoSizeTestData(
                R.id.textview_autosize_uniform, false);
        // Prepare a larger layout in order not to hit the min value easily.
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                autoSizeTextView.setWidth(autoSizeTextView.getWidth() * 2);
                autoSizeTextView.setHeight(autoSizeTextView.getHeight() * 2);
            }
        });
        getInstrumentation().waitForIdleSync();
        // Setup the drawables before setting their padding in order to modify the available
        // space and trigger a resize.
        final Drawable drawable = ResourcesCompat.getDrawable(getActivity().getResources(),
                R.drawable.test_drawable_red, null);
        drawable.setBounds(0, 0, autoSizeTextView.getWidth() / 4, autoSizeTextView.getHeight() / 4);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                autoSizeTextView.setCompoundDrawables(
                        drawable, drawable, drawable, drawable);
            }
        });
        getInstrumentation().waitForIdleSync();
        final float initialTextSize = autoSizeTextView.getTextSize();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                autoSizeTextView.setCompoundDrawablePadding(
                        autoSizeTextView.getCompoundDrawablePadding() + 10);
            }
        });
        getInstrumentation().waitForIdleSync();

        assertTrue(autoSizeTextView.getTextSize() < initialTextSize);
    }

    @Test
    public void testAutoSizeCallers_setPadding() throws Throwable {
        final TextView autoSizeTextView = prepareAndRetrieveAutoSizeTestData(
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
        getInstrumentation().waitForIdleSync();

        assertTrue(autoSizeTextView.getTextSize() < initialTextSize);
    }

    @Test
    public void testAutoSizeCallers_setPaddingRelative() throws Throwable {
        if (Build.VERSION.SDK_INT >= 16) {
            final TextView autoSizeTextView = prepareAndRetrieveAutoSizeTestData(
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
            getInstrumentation().waitForIdleSync();

            assertTrue(autoSizeTextView.getTextSize() < initialTextSize);
        }
    }

    @Test
    public void testAutoSizeCallers_setTypeface() throws Throwable {
        final TextView autoSizeTextView = prepareAndRetrieveAutoSizeTestData(
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
        getInstrumentation().waitForIdleSync();
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
        getInstrumentation().waitForIdleSync();
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
        getInstrumentation().waitForIdleSync();

        assertEquals(changedTextSize, autoSizeTextView.getTextSize(), 0f);
    }

    @Test
    public void testAutoSizeCallers_setLetterSpacing() throws Throwable {
        if (Build.VERSION.SDK_INT >= 21) {
            final TextView autoSizeTextView = prepareAndRetrieveAutoSizeTestData(
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
            getInstrumentation().waitForIdleSync();
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
            getInstrumentation().waitForIdleSync();

            assertEquals(changedTextSize, autoSizeTextView.getTextSize(), 0f);
        }
    }

    @Test
    public void testAutoSizeCallers_setMaxHeight() throws Throwable {
        final TextView autoSizeTextView = prepareAndRetrieveAutoSizeTestData(
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
        getInstrumentation().waitForIdleSync();
        final float initialTextSize = autoSizeTextView.getTextSize();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                autoSizeTextView.setMaxHeight(
                        autoSizeTextView.getHeight() / 4);
            }
        });
        getInstrumentation().waitForIdleSync();

        assertTrue(autoSizeTextView.getTextSize() < initialTextSize);
    }

    @Test
    public void testAutoSizeCallers_setMaxWidth() throws Throwable {
        final TextView autoSizeTextView = prepareAndRetrieveAutoSizeTestData(
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
        getInstrumentation().waitForIdleSync();
        final float initialTextSize = autoSizeTextView.getTextSize();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                autoSizeTextView.setMaxWidth(
                        autoSizeTextView.getWidth() / 4);
            }
        });
        getInstrumentation().waitForIdleSync();

        assertTrue(autoSizeTextView.getTextSize() != initialTextSize);
    }

    @Test
    public void testAutoSizeCallers_setWidth() throws Throwable {
        final TextView autoSizeTextView = prepareAndRetrieveAutoSizeTestData(
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
        getInstrumentation().waitForIdleSync();

        final float initialTextSize = autoSizeTextView.getTextSize();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                autoSizeTextView.setWidth(
                        autoSizeTextView.getWidth() / 4);
            }
        });
        getInstrumentation().waitForIdleSync();

        assertTrue(autoSizeTextView.getTextSize() != initialTextSize);
    }

    @Test
    public void testAutoSizeCallers_setTextSizeIsNoOp() throws Throwable {
        final TextView autoSizeTextView = prepareAndRetrieveAutoSizeTestData(
                R.id.textview_autosize_uniform, false);
        final float initialTextSize = autoSizeTextView.getTextSize();

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                autoSizeTextView.setTextSize(initialTextSize + 123f);
            }
        });
        getInstrumentation().waitForIdleSync();

        assertEquals(initialTextSize, autoSizeTextView.getTextSize(), 0f);
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
    private TextView prepareAndRetrieveAutoSizeTestData(final int viewId,
            final boolean shouldWrapLayoutContent) throws Throwable {
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                LinearLayout ll = (LinearLayout) getActivity().findViewById(
                        android.support.v7.appcompat.test.R.id.layout_textviewtest);
                TextView targetedTextView = (TextView) getActivity().findViewById(viewId);
                ll.removeAllViews();
                ll.addView(targetedTextView);
            }
        });
        getInstrumentation().waitForIdleSync();

        final TextView textView = (TextView) getActivity().findViewById(viewId);
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
            getInstrumentation().waitForIdleSync();
        }

        return textView;
    }
}
