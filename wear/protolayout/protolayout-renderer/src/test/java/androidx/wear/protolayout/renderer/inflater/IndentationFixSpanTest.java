/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.protolayout.renderer.inflater;

import static androidx.wear.protolayout.renderer.inflater.IndentationFixSpan.ELLIPSIS_CHAR;
import static androidx.wear.protolayout.renderer.inflater.IndentationFixSpan.calculatePadding;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Layout;
import android.text.Layout.Alignment;
import android.text.StaticLayout;
import android.text.TextPaint;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.common.truth.Expect;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Objects;

/**
 * Tests that the actual padding is correctly calculated. The translation of canvas is tested with
 * screenshot tests.
 */
@RunWith(AndroidJUnit4.class)
public class IndentationFixSpanTest {

    private static final String TEST_TEXT = "Test";
    private static final int DEFAULT_ELLIPSIZE_START = 100;
    private static final TestPaint PAINT = new TestPaint();

    @Rule public final Expect expect = Expect.create();

    @Test
    public void test_givenLayout_correctObjectIsUsed() {
        StaticLayout layout = mock(StaticLayout.class);
        IndentationFixSpan span = new IndentationFixSpan(layout);
        Layout givenLayout = mock(Layout.class);

        span.drawLeadingMargin(
                mock(Canvas.class),
                mock(Paint.class),
                /* x= */ 0,
                /* dir= */ 0,
                /* top= */ 0,
                /* baseline= */ 0,
                /* bottom= */ 0,
                "Text",
                /* start= */ 0,
                /* end= */ 0,
                false,
                givenLayout);

        verifyNoInteractions(givenLayout);

        verify(layout).getLineCount();
        verify(layout).getLineForOffset(/* offset= */ 0);
    }

    @Test
    public void test_calculatedPadding_onRtl_centerAlign_correctValue() {
        TestLayoutRtl layout =
                new TestLayoutRtl(
                        TEST_TEXT,
                        /* width= */ 288,
                        Alignment.ALIGN_CENTER,
                        /* mainLineIndex= */ 2);

        // On ellipsized line there is padding.
        expect.that(calculatePadding(PAINT, DEFAULT_ELLIPSIZE_START, layout)).isEqualTo(-8.5f);
    }

    @Test
    public void test_calculatedPadding_onRtl_normalAlign_correctValue() {
        TestLayoutRtl layout =
                new TestLayoutRtl(
                        TEST_TEXT,
                        /* width= */ 288,
                        Alignment.ALIGN_NORMAL,
                        /* mainLineIndex= */ 2);

        // On ellipsized line there is padding.
        expect.that(calculatePadding(PAINT, DEFAULT_ELLIPSIZE_START, layout)).isEqualTo(-9f);
    }

    @Test
    public void test_calculatedPadding_onLtr_centerAlign_correctValue() {
        TestLayoutLtr layout =
                new TestLayoutLtr(
                        TEST_TEXT,
                        /* width= */ 300,
                        Alignment.ALIGN_CENTER,
                        /* mainLineIndex= */ 2);

        // On ellipsized line there is padding.
        expect.that(calculatePadding(PAINT, DEFAULT_ELLIPSIZE_START, layout)).isEqualTo(13f);
    }

    @Test
    public void test_calculatedPadding_onLtr_normalAlign_correctValue() {
        TestLayoutLtr layout =
                new TestLayoutLtr(
                        TEST_TEXT,
                        /* width= */ 300,
                        Alignment.ALIGN_NORMAL,
                        /* mainLineIndex= */ 2);

        // On ellipsized line there is padding.
        expect.that(calculatePadding(PAINT, DEFAULT_ELLIPSIZE_START, layout)).isEqualTo(19f);
    }

    @Test
    public void test_calculatePadding_lastLineNotEllipsize_returnsZero() {
        // Number of lines so that notEllipsizedLineIndex is the last one.
        TestLayoutLtr layout =
                new TestLayoutLtr(
                        TEST_TEXT,
                        /* width= */ 300,
                        Alignment.ALIGN_CENTER,
                        /* mainLineIndex= */ 2);
        // But not ellipsized.
        layout.removeEllipsisCount();

        // On not ellipsized line there is no padding.
        expect.that(calculatePadding(PAINT, DEFAULT_ELLIPSIZE_START, layout)).isEqualTo(0);
    }

    @Test
    public void test_calculatePadding_notLastLine_returnsZero() {
        // Number of lines so that notEllipsizedLineIndex is the last one.
        TestLayoutLtr layout =
                new TestLayoutLtr(
                        TEST_TEXT,
                        /* width= */ 300,
                        Alignment.ALIGN_CENTER,
                        /* mainLineIndex= */ 2);
        // Number of lines so that lineIndex is NOT the last one.
        layout.increaseLineCount();

        // On not last line there is no padding.
        expect.that(calculatePadding(PAINT, DEFAULT_ELLIPSIZE_START, layout)).isEqualTo(0);
    }

    private static class TestPaint extends TextPaint {

        @Override
        public float measureText(String text) {
            if (Objects.equals(text, ELLIPSIS_CHAR)) {
                return 23f;
            }
            return super.measureText(text);
        }
    }

    /**
     * Test only implementation of {@link Layout} with numbers so we can test padding correctly.
     */
    private abstract static class TestLayout extends Layout {

        private static final int DEFAULT_ELLIPSIS_COUNT = 3;
        protected final int mMainLineIndex;

        // Overridable values for the mainLineIndex.
        private int mLineCount;
        private int mEllipsisCount = DEFAULT_ELLIPSIS_COUNT;

        protected TestLayout(CharSequence text, int width, Alignment align, int mainLineIndex) {
            super(text, PAINT, width, align, /* spacingMult= */ 0, /* spacingAdd= */ 0);
            this.mMainLineIndex = mainLineIndex;
            mLineCount = mainLineIndex + 1;
        }

        void increaseLineCount() {
            mLineCount = mLineCount + 3;
        }

        void removeEllipsisCount() {
            this.mEllipsisCount = 0;
        }

        @Override
        public int getLineCount() {
            return mLineCount;
        }

        @Override
        public int getLineTop(int line) {
            // N/A
            return 0;
        }

        @Override
        public int getLineDescent(int line) {
            // N/A
            return 0;
        }

        @Override
        public int getLineStart(int line) {
            return 0;
        }

        @Override
        public boolean getLineContainsTab(int line) {
            // N/A
            return false;
        }

        @Override
        public Directions getLineDirections(int line) {
            // N/A
            return null;
        }

        @Override
        public int getTopPadding() {
            // N/A
            return 0;
        }

        @Override
        public int getBottomPadding() {
            // N/A
            return 0;
        }

        @Override
        public int getEllipsisCount(int line) {
            return line == mMainLineIndex ? /* non zero */ mEllipsisCount : 0;
        }

        @Override
        public int getLineForOffset(int offset) {
            return offset == DEFAULT_ELLIPSIZE_START ? mMainLineIndex : 0;
        }
    }

    /**
     * Specific implementation of {@link Layout} that returns numbers for LTR testing of padding.
     */
    private static class TestLayoutLtr extends TestLayout {

        protected TestLayoutLtr(CharSequence text, int width, Alignment align, int mainLineIndex) {
            super(text, width, align, mainLineIndex);
        }

        @Override
        public float getPrimaryHorizontal(int offset) {
            return 258f;
        }

        @Override
        public float getLineLeft(int line) {
            return line == mMainLineIndex ? -7f : super.getLineLeft(line);
        }

        @Override
        public int getEllipsisStart(int line) {
            return 20;
        }

        @Override
        public int getParagraphDirection(int line) {
            return Layout.DIR_LEFT_TO_RIGHT;
        }
    }

    /**
     * Specific implementation of {@link Layout} that returns numbers for RTL testing of padding.
     */
    private static class TestLayoutRtl extends TestLayout {

        protected TestLayoutRtl(CharSequence text, int width, Alignment align, int mainLineIndex) {
            super(text, width, align, mainLineIndex);
        }

        @Override
        public float getPrimaryHorizontal(int offset) {
            return 32f;
        }

        @Override
        public float getLineLeft(int line) {
            return line == mMainLineIndex ? -7f : super.getLineLeft(line);
        }

        @Override
        public int getEllipsisStart(int line) {
            return 20;
        }

        @Override
        public int getParagraphDirection(int line) {
            return Layout.DIR_RIGHT_TO_LEFT;
        }

        @Override
        public float getLineRight(int line) {
            return line == mMainLineIndex ? 296f : super.getLineRight(line);
        }
    }
}
