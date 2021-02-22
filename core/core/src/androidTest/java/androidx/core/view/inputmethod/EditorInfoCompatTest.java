/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.core.view.inputmethod;

import static androidx.core.view.inputmethod.EditorInfoTestUtils.createEditorInfoForTest;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyInt;

import android.os.Parcel;
import android.support.v4.BaseInstrumentationTestCase;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import androidx.annotation.Nullable;
import androidx.core.app.TestActivity;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;

import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(AndroidJUnit4.class)
@MediumTest
public class EditorInfoCompatTest extends BaseInstrumentationTestCase<TestActivity> {
    private static final int LONG_EXP_TEXT_LENGTH =
            EditorInfoCompat.MEMORY_EFFICIENT_TEXT_LENGTH * 2;

    public EditorInfoCompatTest() {
        super(TestActivity.class);
    }

    @Test
    public void testRoundTrip() {
        EditorInfo editorInfo1 = new EditorInfo();
        String[] mimeTypes1 = new String[]{"image/gif", "image/jpeg", "image/png"};
        EditorInfoCompat.setContentMimeTypes(editorInfo1, mimeTypes1);
        assertArrayEquals(EditorInfoCompat.getContentMimeTypes(editorInfo1), mimeTypes1);

        EditorInfo editorInfo2 = new EditorInfo();
        String[] mimeTypes2 = new String[]{"image/gif", "image/jpeg"};
        EditorInfoCompat.setContentMimeTypes(editorInfo2, mimeTypes2);
        assertArrayEquals(EditorInfoCompat.getContentMimeTypes(editorInfo2), mimeTypes2);

        EditorInfo editorInfo3 = new EditorInfo();
        String[] mimeTypes3 = new String[]{};
        EditorInfoCompat.setContentMimeTypes(editorInfo3, mimeTypes3);
        assertArrayEquals(EditorInfoCompat.getContentMimeTypes(editorInfo3), mimeTypes3);
    }

    @Test
    @SdkSuppress(maxSdkVersion = 24)
    public void testRoundTripSupportLibAndroidX100() {
        String[] mimeTypes = new String[]{"image/gif", "image/jpeg", "image/png"};
        assertArrayEquals(EditorInfoCompat.getContentMimeTypes(
                createEditorInfoForTest(mimeTypes, EditorInfoCompat.Protocol.AndroidX_1_0_0)),
                mimeTypes);
    }

    @Test
    @SdkSuppress(maxSdkVersion = 24)
    public void testRoundTripSupportLibAndroidX110() {
        String[] mimeTypes = new String[]{"image/gif", "image/jpeg", "image/png"};
        assertArrayEquals(EditorInfoCompat.getContentMimeTypes(
                createEditorInfoForTest(mimeTypes, EditorInfoCompat.Protocol.AndroidX_1_1_0)),
                mimeTypes);
    }

    @Test
    public void setInitialText_nullInputText_throwsException() {
        final CharSequence testText = null;
        final EditorInfo editorInfo = new EditorInfo();

        try {
            EditorInfoCompat.setInitialSurroundingText(editorInfo, testText);
            fail("setSurroundingText should not take null input");
        } catch (NullPointerException expected) {
            // Expected behavior, nothing to do.
        }
    }

    @Test
    public void setInitialText_cursorAtHead_dividesByCursorPosition() {
        final CharSequence testText = createTestText(EditorInfoCompat.MEMORY_EFFICIENT_TEXT_LENGTH);
        final EditorInfo editorInfo = new EditorInfo();
        final int selectionLength = 0;
        editorInfo.initialSelStart = 0;
        editorInfo.initialSelEnd = editorInfo.initialSelStart + selectionLength;
        final int expectedTextBeforeCursorLength = 0;
        final int expectedTextAfterCursorLength = testText.length();

        EditorInfoCompat.setInitialSurroundingText(editorInfo, testText);

        assertExpectedTextLength(editorInfo, expectedTextBeforeCursorLength, selectionLength,
                expectedTextAfterCursorLength);
    }

    @Test
    public void setInitialText_cursorAtTail_dividesByCursorPosition() {
        final CharSequence testText = createTestText(EditorInfoCompat.MEMORY_EFFICIENT_TEXT_LENGTH);
        final EditorInfo editorInfo = new EditorInfo();
        final int selectionLength = 0;
        editorInfo.initialSelStart = testText.length() - selectionLength;
        editorInfo.initialSelEnd = testText.length();
        final int expectedTextBeforeCursorLength = testText.length();
        final int expectedTextAfterCursorLength = 0;

        EditorInfoCompat.setInitialSurroundingText(editorInfo, testText);

        assertExpectedTextLength(editorInfo, expectedTextBeforeCursorLength, selectionLength,
                expectedTextAfterCursorLength);
    }

    @Test
    public void setInitialText_cursorAtMiddle_dividesByCursorPosition() {
        final CharSequence testText = createTestText(EditorInfoCompat.MEMORY_EFFICIENT_TEXT_LENGTH);
        final EditorInfo editorInfo = new EditorInfo();
        final int selectionLength = 2;
        editorInfo.initialSelStart = testText.length() / 2;
        editorInfo.initialSelEnd = editorInfo.initialSelStart + selectionLength;
        final int expectedTextBeforeCursorLength = editorInfo.initialSelStart;
        final int expectedTextAfterCursorLength = testText.length() - editorInfo.initialSelEnd;

        EditorInfoCompat.setInitialSurroundingText(editorInfo, testText);

        assertExpectedTextLength(editorInfo, expectedTextBeforeCursorLength, selectionLength,
                expectedTextAfterCursorLength);
    }

    @Test
    public void setInitialText_incorrectCursorOrder_correctsThenDivide() {
        final CharSequence testText = createTestText(EditorInfoCompat.MEMORY_EFFICIENT_TEXT_LENGTH);
        final EditorInfo editorInfo = new EditorInfo();
        final int selectionLength = 2;
        editorInfo.initialSelEnd = testText.length() / 2;
        editorInfo.initialSelStart = editorInfo.initialSelEnd + selectionLength;
        final int expectedTextBeforeCursorLength = testText.length() / 2;
        final int expectedTextAfterCursorLength = testText.length() - testText.length() / 2
                - selectionLength;

        EditorInfoCompat.setInitialSurroundingText(editorInfo, testText);

        assertExpectedTextLength(editorInfo, expectedTextBeforeCursorLength, selectionLength,
                expectedTextAfterCursorLength);
    }

    @Test
    public void setInitialText_invalidCursorPosition_returnsNull() {
        final CharSequence testText = createTestText(EditorInfoCompat.MEMORY_EFFICIENT_TEXT_LENGTH);
        final EditorInfo editorInfo = new EditorInfo();
        editorInfo.initialSelStart = -1;

        EditorInfoCompat.setInitialSurroundingText(editorInfo, testText);

        assertExpectedTextLength(editorInfo,
                /* expectBeforeCursorLength= */null,
                /* expectSelectionLength= */null,
                /* expectAfterCursorLength= */null);
    }

    @Test
    public void setOverSizeInitialText_cursorAtMiddle_dividesProportionately() {
        final CharSequence testText =
                createTestText(EditorInfoCompat.MEMORY_EFFICIENT_TEXT_LENGTH + 2);
        final EditorInfo editorInfo = new EditorInfo();
        final int selectionLength = 2;
        editorInfo.initialSelStart = testText.length() / 2;
        editorInfo.initialSelEnd = editorInfo.initialSelStart + selectionLength;
        final int expectedTextBeforeCursorLength = Math.min(editorInfo.initialSelStart,
                (int) (0.8 * (EditorInfoCompat.MEMORY_EFFICIENT_TEXT_LENGTH - selectionLength)));
        final int expectedTextAfterCursorLength = EditorInfoCompat.MEMORY_EFFICIENT_TEXT_LENGTH
                - expectedTextBeforeCursorLength - selectionLength;

        EditorInfoCompat.setInitialSurroundingText(editorInfo, testText);

        assertExpectedTextLength(editorInfo, expectedTextBeforeCursorLength, selectionLength,
                expectedTextAfterCursorLength);
    }

    @Test
    public void setOverSizeInitialText_overSizeSelection_dropsSelection() {
        final CharSequence testText =
                createTestText(EditorInfoCompat.MEMORY_EFFICIENT_TEXT_LENGTH + 2);
        final EditorInfo editorInfo = new EditorInfo();
        final int selectionLength = EditorInfoCompat.MAX_INITIAL_SELECTION_LENGTH + 1;
        editorInfo.initialSelStart = testText.length() / 2;
        editorInfo.initialSelEnd = editorInfo.initialSelStart + selectionLength;
        final int expectedTextBeforeCursorLength = Math.min(editorInfo.initialSelStart,
                (int) (0.8 * EditorInfoCompat.MEMORY_EFFICIENT_TEXT_LENGTH));
        final int expectedTextAfterCursorLength = testText.length() - editorInfo.initialSelEnd;

        EditorInfoCompat.setInitialSurroundingText(editorInfo, testText);

        assertExpectedTextLength(editorInfo, expectedTextBeforeCursorLength,
                /* expectSelectionLength= */null, expectedTextAfterCursorLength);
    }

    @Test
    public void setInitialSubText_trimmedSubText_dividesByOriginalCursorPosition() {
        final String prefixString = "prefix";
        final CharSequence subText = createTestText(EditorInfoCompat.MEMORY_EFFICIENT_TEXT_LENGTH);
        final CharSequence originalText = TextUtils.concat(prefixString, subText);
        final EditorInfo editorInfo = new EditorInfo();
        final int selLength = 2;
        editorInfo.initialSelStart = originalText.length() / 2;
        editorInfo.initialSelEnd = editorInfo.initialSelStart + selLength;
        final CharSequence expectedTextBeforeCursor = createExpectedText(/* startNumber= */0,
                editorInfo.initialSelStart - prefixString.length());
        final CharSequence expectedSelectedText = createExpectedText(
                editorInfo.initialSelStart - prefixString.length(), selLength);
        final CharSequence expectedTextAfterCursor = createExpectedText(
                editorInfo.initialSelEnd - prefixString.length(),
                originalText.length() - editorInfo.initialSelEnd);

        EditorInfoCompat.setInitialSurroundingSubText(editorInfo, subText, prefixString.length());

        assertTrue(TextUtils.equals(expectedTextBeforeCursor,
                EditorInfoCompat.getInitialTextBeforeCursor(editorInfo, LONG_EXP_TEXT_LENGTH,
                        anyInt())));
        assertTrue(TextUtils.equals(expectedSelectedText,
                EditorInfoCompat.getInitialSelectedText(editorInfo, anyInt())));
        assertTrue(TextUtils.equals(expectedTextAfterCursor,
                EditorInfoCompat.getInitialTextAfterCursor(editorInfo, LONG_EXP_TEXT_LENGTH,
                        anyInt())));
    }

    @Test
    public void surroundingTextRetrieval_writeToParcel_noException() {
        StringBuilder sb = new StringBuilder("abcdefg");
        Parcel parcel = Parcel.obtain();
        EditorInfo editorInfo = new EditorInfo();
        editorInfo.initialSelStart = 2;
        editorInfo.initialSelEnd = 5;
        editorInfo.inputType = EditorInfo.TYPE_CLASS_TEXT;

        EditorInfoCompat.setInitialSurroundingText(editorInfo, sb);
        sb.setLength(0);
        editorInfo.writeToParcel(parcel, 0);

        EditorInfoCompat.getInitialTextBeforeCursor(editorInfo, 60, 1);
    }

    private static void assertExpectedTextLength(EditorInfo editorInfo,
            @Nullable Integer expectBeforeCursorLength, @Nullable Integer expectSelectionLength,
            @Nullable Integer expectAfterCursorLength) {
        final CharSequence textBeforeCursor =
                EditorInfoCompat.getInitialTextBeforeCursor(editorInfo, LONG_EXP_TEXT_LENGTH,
                        InputConnection.GET_TEXT_WITH_STYLES);
        final CharSequence selectedText =
                EditorInfoCompat.getInitialSelectedText(editorInfo,
                        InputConnection.GET_TEXT_WITH_STYLES);
        final CharSequence textAfterCursor =
                EditorInfoCompat.getInitialTextAfterCursor(editorInfo, LONG_EXP_TEXT_LENGTH,
                        InputConnection.GET_TEXT_WITH_STYLES);

        if (expectBeforeCursorLength == null) {
            assertNull(textBeforeCursor);
        } else {
            assertEquals(expectBeforeCursorLength.intValue(), textBeforeCursor.length());
        }

        if (expectSelectionLength == null) {
            assertNull(selectedText);
        } else {
            assertEquals(expectSelectionLength.intValue(), selectedText.length());
        }

        if (expectAfterCursorLength == null) {
            assertNull(textAfterCursor);
        } else {
            assertEquals(expectAfterCursorLength.intValue(), textAfterCursor.length());
        }
    }

    private static CharSequence createTestText(int surroundingLength) {
        final SpannableStringBuilder builder = new SpannableStringBuilder();
        for (int i = 0; i < surroundingLength; i++) {
            builder.append(Integer.toString(i % 10));
        }
        return builder;
    }

    private static CharSequence createExpectedText(int startNumber, int length) {
        final SpannableStringBuilder builder = new SpannableStringBuilder();
        for (int i = startNumber; i < startNumber + length; i++) {
            builder.append(Integer.toString(i % 10));
        }
        return builder;
    }
}
