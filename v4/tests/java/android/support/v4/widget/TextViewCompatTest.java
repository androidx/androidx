/*
 * Copyright (C) 2015 The Android Open Source Project
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


package android.support.v4.widget;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.app.Instrumentation;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Looper;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.test.suitebuilder.annotation.SmallTest;
import android.support.annotation.LayoutRes;
import android.support.test.InstrumentationRegistry;
import android.support.v4.test.R;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.TextViewCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class TextViewCompatTest extends ActivityInstrumentationTestCase2<TestActivity> {
    private static final String TAG = "TextViewCompatTest";

    private TextView mTextView;

    public TextViewCompatTest() {
        super("android.support.v4.widget", TestActivity.class);
    }

    @Override
    public void tearDown() throws Exception {
        if (mTextView != null) {
            removeTextView();
        }

        getInstrumentation().waitForIdleSync();
        super.tearDown();
    }

    private boolean isMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    private void removeTextView() {
        if (mTextView == null) {
            return;
        }
        if (!isMainThread()) {
            getInstrumentation().waitForIdleSync();
        }
        try {
            runTestOnUiThread(new Runnable() {
                @Override
                public void run() {
                    getActivity().mContainer.removeAllViews();
                }
            });
        } catch (Throwable throwable) {
            Log.e(TAG, "", throwable);
        }
        mTextView = null;
    }

    private void createAndAddTextView() {
        final TestActivity activity = getActivity();
        mTextView = new TextView(activity);
        activity.mContainer.addView(mTextView);
    }

    @UiThreadTest
    @SmallTest
    public void testMaxLines() throws Throwable {
        createAndAddTextView();
        final int maxLinesCount = 4;
        mTextView.setMaxLines(maxLinesCount);

        assertEquals("Empty view: Max lines must match", TextViewCompat.getMaxLines(mTextView),
                maxLinesCount);

        mTextView.setText(R.string.test_text_short);
        assertEquals("Short text: Max lines must match", TextViewCompat.getMaxLines(mTextView),
                maxLinesCount);

        mTextView.setText(R.string.test_text_medium);
        assertEquals("Medium text: Max lines must match", TextViewCompat.getMaxLines(mTextView),
                maxLinesCount);

        mTextView.setText(R.string.test_text_long);
        assertEquals("Long text: Max lines must match", TextViewCompat.getMaxLines(mTextView),
                maxLinesCount);
    }

    @UiThreadTest
    @SmallTest
    public void testMinLines() throws Throwable {
        createAndAddTextView();
        final int minLinesCount = 3;
        mTextView.setMinLines(minLinesCount);

        assertEquals("Empty view: Min lines must match", TextViewCompat.getMinLines(mTextView),
                minLinesCount);

        mTextView.setText(R.string.test_text_short);
        assertEquals("Short text: Min lines must match", TextViewCompat.getMinLines(mTextView),
                minLinesCount);

        mTextView.setText(R.string.test_text_medium);
        assertEquals("Medium text: Min lines must match", TextViewCompat.getMinLines(mTextView),
                minLinesCount);

        mTextView.setText(R.string.test_text_long);
        assertEquals("Long text: Min lines must match", TextViewCompat.getMinLines(mTextView),
                minLinesCount);
    }

    @UiThreadTest
    @SmallTest
    public void testStyle() throws Throwable {
        createAndAddTextView();

        TextViewCompat.setTextAppearance(mTextView, R.style.TextMediumStyle);

        final Resources res = getActivity().getResources();
        assertEquals("Styled text view: style", mTextView.getTypeface().getStyle(),
                Typeface.ITALIC);
        assertEquals("Styled text view: color", mTextView.getTextColors().getDefaultColor(),
                res.getColor(R.color.text_color));
        assertEquals("Styled text view: size", mTextView.getTextSize(),
                (float) res.getDimensionPixelSize(R.dimen.text_medium_size));
    }

    @UiThreadTest
    @SmallTest
    public void testCompoundDrawablesRelative() throws Throwable {
        createAndAddTextView();

        final Drawable drawableStart = new ColorDrawable(0xFFFF0000);
        drawableStart.setBounds(0, 0, 20, 20);
        final Drawable drawableTop = new ColorDrawable(0xFF00FF00);
        drawableTop.setBounds(0, 0, 20, 20);
        final Drawable drawableEnd = new ColorDrawable(0xFF0000FF);
        drawableEnd.setBounds(0, 0, 20, 20);

        mTextView.setText(R.string.test_text_medium);
        TextViewCompat.setCompoundDrawablesRelative(mTextView, drawableStart, drawableTop,
                drawableEnd, null);

        // Explicitly measure and layout the text view so that the core TextView updates its
        // internal tracking of where each drawable is positioned relative to the text.
        final DisplayMetrics metrics = getActivity().getResources().getDisplayMetrics();
        int textViewWidthPx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 200, metrics);
        int textViewHeightPx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 60, metrics);
        mTextView.measure(
                View.MeasureSpec.makeMeasureSpec(textViewWidthPx, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(textViewHeightPx, View.MeasureSpec.EXACTLY));
        mTextView.layout(0, 0, textViewWidthPx, textViewHeightPx);

        final Drawable[] drawablesAbsolute = mTextView.getCompoundDrawables();
        assertEquals("Compound drawable: left", drawablesAbsolute[0], drawableStart);
        assertEquals("Compound drawable: top", drawablesAbsolute[1], drawableTop);
        assertEquals("Compound drawable: right", drawablesAbsolute[2], drawableEnd);
        assertNull("Compound drawable: bottom", drawablesAbsolute[3]);
    }

    @UiThreadTest
    @SmallTest
    public void testCompoundDrawablesRelativeRtl() throws Throwable {
        createAndAddTextView();

        ViewCompat.setLayoutDirection(mTextView, ViewCompat.LAYOUT_DIRECTION_RTL);

        final Drawable drawableStart = new ColorDrawable(0xFFFF0000);
        drawableStart.setBounds(0, 0, 20, 20);
        final Drawable drawableTop = new ColorDrawable(0xFF00FF00);
        drawableTop.setBounds(0, 0, 20, 20);
        final Drawable drawableEnd = new ColorDrawable(0xFF0000FF);
        drawableEnd.setBounds(0, 0, 20, 20);

        mTextView.setText(R.string.test_text_medium);
        TextViewCompat.setCompoundDrawablesRelative(mTextView, drawableStart, drawableTop,
                drawableEnd, null);

        // Explicitly measure and layout the text view so that the core TextView updates its
        // internal tracking of where each drawable is positioned relative to the text.
        final DisplayMetrics metrics = getActivity().getResources().getDisplayMetrics();
        int textViewWidthPx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 200, metrics);
        int textViewHeightPx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 60, metrics);
        mTextView.measure(
                View.MeasureSpec.makeMeasureSpec(textViewWidthPx, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(textViewHeightPx, View.MeasureSpec.EXACTLY));
        mTextView.layout(0, 0, textViewWidthPx, textViewHeightPx);

        // Check to see whether our text view is under RTL mode
        if (ViewCompat.getLayoutDirection(mTextView) != ViewCompat.LAYOUT_DIRECTION_RTL) {
            // This will happen on v17- devices
            return;
        }

        final Drawable[] drawablesAbsolute = mTextView.getCompoundDrawables();
        // End drawable should be returned as left
        assertEquals("Compound drawable: left", drawablesAbsolute[0], drawableEnd);
        assertEquals("Compound drawable: top", drawablesAbsolute[1], drawableTop);
        // Start drawable should be returned as right
        assertEquals("Compound drawable: right", drawablesAbsolute[2], drawableStart);
        assertNull("Compound drawable: bottom", drawablesAbsolute[3]);
    }
}
