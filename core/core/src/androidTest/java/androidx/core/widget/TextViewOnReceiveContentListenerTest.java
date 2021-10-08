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

package androidx.core.widget;

import static androidx.core.view.ContentInfoCompat.FLAG_CONVERT_TO_PLAIN_TEXT;
import static androidx.core.view.ContentInfoCompat.SOURCE_CLIPBOARD;
import static androidx.core.view.ContentInfoCompat.SOURCE_DRAG_AND_DROP;
import static androidx.core.view.ContentInfoCompat.SOURCE_INPUT_METHOD;

import static com.google.common.truth.Truth.assertThat;

import android.content.ClipData;
import android.net.Uri;
import android.text.Selection;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.UnderlineSpan;
import android.widget.EditText;

import androidx.core.test.R;
import androidx.core.view.ContentInfoCompat;
import androidx.core.view.ContentInfoCompat.Flags;
import androidx.core.view.ContentInfoCompat.Source;
import androidx.core.view.OnReceiveContentListener;
import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.rule.ActivityTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class TextViewOnReceiveContentListenerTest {

    @Rule
    public final ActivityTestRule<ReceiveContentTestActivity> mActivityTestRule =
            new ActivityTestRule<>(ReceiveContentTestActivity.class);

    private EditText mEditText;
    private TextViewOnReceiveContentListener mReceiver;

    @Before
    public void before() {
        ReceiveContentTestActivity activity = mActivityTestRule.getActivity();
        mEditText = activity.findViewById(R.id.edit_text);
        mReceiver = new TextViewOnReceiveContentListener();
    }

    @UiThreadTest
    @Test
    public void testOnReceive_text() throws Exception {
        setTextAndCursor("xz", 1);

        ClipData clip = ClipData.newPlainText("test", "y");
        boolean result = onReceive(mReceiver, clip, SOURCE_CLIPBOARD, 0);

        assertThat(result).isTrue();
        assertTextAndCursorPosition("xyz", 2);
    }

    @UiThreadTest
    @Test
    public void testOnReceive_styledText() throws Exception {
        setTextAndCursor("xz", 1);

        UnderlineSpan underlineSpan = new UnderlineSpan();
        SpannableStringBuilder ssb = new SpannableStringBuilder("hi world");
        ssb.setSpan(underlineSpan, 3, 7, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ClipData clip = ClipData.newPlainText("test", ssb);

        boolean result = onReceive(mReceiver, clip, SOURCE_CLIPBOARD, 0);

        assertThat(result).isTrue();
        assertTextAndCursorPosition("xhi worldz", 9);
        int spanStart = mEditText.getText().getSpanStart(underlineSpan);
        assertThat(spanStart).isEqualTo(4);
    }

    @UiThreadTest
    @Test
    public void testOnReceive_text_convertToPlainText() throws Exception {
        setTextAndCursor("xz", 1);

        ClipData clip = ClipData.newPlainText("test", "y");
        boolean result = onReceive(mReceiver, clip, SOURCE_CLIPBOARD, FLAG_CONVERT_TO_PLAIN_TEXT);

        assertThat(result).isTrue();
        assertTextAndCursorPosition("xyz", 2);
    }

    @UiThreadTest
    @Test
    public void testOnReceive_styledText_convertToPlainText() throws Exception {
        setTextAndCursor("xz", 1);

        UnderlineSpan underlineSpan = new UnderlineSpan();
        SpannableStringBuilder ssb = new SpannableStringBuilder("hi world");
        ssb.setSpan(underlineSpan, 3, 7, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ClipData clip = ClipData.newPlainText("test", ssb);

        boolean result = onReceive(mReceiver, clip, SOURCE_CLIPBOARD, FLAG_CONVERT_TO_PLAIN_TEXT);

        assertThat(result).isTrue();
        assertTextAndCursorPosition("xhi worldz", 9);
        int spanStart = mEditText.getText().getSpanStart(underlineSpan);
        assertThat(spanStart).isEqualTo(-1);
    }

    @SdkSuppress(minSdkVersion = 16) // Passing HTML into a ClipData.Item was added in SDK 16.
    @UiThreadTest
    @Test
    public void testOnReceive_html() throws Exception {
        setTextAndCursor("xz", 1);

        ClipData clip = ClipData.newHtmlText("test", "*y*", "<b>y</b>");
        boolean result = onReceive(mReceiver, clip, SOURCE_CLIPBOARD, 0);

        assertThat(result).isTrue();
        assertTextAndCursorPosition("xyz", 2);
    }

    @SdkSuppress(minSdkVersion = 16) // Passing HTML into a ClipData.Item was added in SDK 16.
    @UiThreadTest
    @Test
    public void testOnReceive_html_convertToPlainText() throws Exception {
        setTextAndCursor("xz", 1);

        ClipData clip = ClipData.newHtmlText("test", "*y*", "<b>y</b>");
        boolean result = onReceive(mReceiver, clip, SOURCE_CLIPBOARD, FLAG_CONVERT_TO_PLAIN_TEXT);

        assertThat(result).isTrue();
        assertTextAndCursorPosition("x*y*z", 4);
    }

    @UiThreadTest
    @Test
    public void testOnReceive_multipleItemsInClipData() throws Exception {
        setTextAndCursor("xz", 1);

        ClipData clip = ClipData.newPlainText("test", "ONE");
        clip.addItem(new ClipData.Item("TWO"));
        clip.addItem(new ClipData.Item("THREE"));

        // Verify the resulting text when pasting a clip that contains multiple text items.
        boolean result = onReceive(mReceiver, clip, SOURCE_CLIPBOARD, 0);
        assertThat(result).isTrue();
        assertTextAndCursorPosition("xONE\nTWO\nTHREEz", 14);

        // Verify the resulting text when inserting the same clip via drag-and-drop. The result
        // should be the same as when pasting from the clipboard.
        setTextAndCursor("xz", 1);
        result = onReceive(mReceiver, clip, SOURCE_DRAG_AND_DROP, 0);
        assertThat(result).isTrue();
        assertTextAndCursorPosition("xONE\nTWO\nTHREEz", 14);
    }

    @UiThreadTest
    @Test
    public void testOnReceive_noSelectionPriorToPaste() throws Exception {
        // Set the text and then clear the selection (ie, ensure that nothing is selected and
        // that the cursor is not present).
        setTextAndCursor("xz", 0);
        Selection.removeSelection(mEditText.getText());
        assertTextAndCursorPosition("xz", -1);

        // Pasting should still work (should just insert the text at the beginning).
        ClipData clip = ClipData.newPlainText("test", "y");
        boolean result = onReceive(mReceiver, clip, SOURCE_CLIPBOARD, 0);

        assertThat(result).isTrue();
        assertTextAndCursorPosition("yxz", 1);
    }

    @UiThreadTest
    @Test
    public void testOnReceive_selectionStartAndEndSwapped() throws Exception {
        // Set the text and then set the selection such that "end" is before "start".
        setTextAndCursor("hey", 0);
        Selection.setSelection(mEditText.getText(), 3, 1);
        assertTextAndSelection("hey", 3, 1);

        // Pasting should still work (should still successfully overwrite the selection).
        ClipData clip = ClipData.newPlainText("test", "i");
        boolean result = onReceive(mReceiver, clip, SOURCE_CLIPBOARD, 0);

        assertThat(result).isTrue();
        assertTextAndCursorPosition("hi", 2);
    }

    @SdkSuppress(minSdkVersion = 16) // Passing HTML into a ClipData.Item was added in SDK 16.
    @UiThreadTest
    @Test
    public void testOnReceive_unsupportedMimeType_viaMenu() throws Exception {
        setTextAndCursor("xz", 1);

        ClipData clip = new ClipData("test", new String[]{"video/mp4"},
                new ClipData.Item("text", "html", null, Uri.parse("content://com.example/path")));
        boolean result = onReceive(mReceiver, clip, SOURCE_CLIPBOARD, 0);

        assertThat(result).isTrue();
        assertTextAndCursorPosition("xhtmlz", 5);
    }

    @SdkSuppress(minSdkVersion = 16) // Passing HTML into a ClipData.Item was added in SDK 16.
    @UiThreadTest
    @Test
    public void testOnReceive_unsupportedMimeType_viaMenu_convertToPlainText() throws Exception {
        setTextAndCursor("xz", 1);

        ClipData clip = new ClipData("test", new String[]{"video/mp4"},
                new ClipData.Item("text", "html", null, Uri.parse("content://com.example/path")));
        boolean result = onReceive(mReceiver, clip, SOURCE_CLIPBOARD, FLAG_CONVERT_TO_PLAIN_TEXT);

        assertThat(result).isTrue();
        assertTextAndCursorPosition("xtextz", 5);
    }

    @UiThreadTest
    @Test
    public void testOnReceive_unsupportedMimeType_viaInputMethod() throws Exception {
        setTextAndCursor("xz", 1);

        ClipData clip = new ClipData("test", new String[]{"video/mp4"},
                new ClipData.Item("text", null, Uri.parse("content://com.example/path")));
        boolean result = onReceive(mReceiver, clip, SOURCE_INPUT_METHOD, 0);

        assertThat(result).isFalse();
        assertTextAndCursorPosition("xz", 1);
    }

    @UiThreadTest
    @Test
    public void testOnReceive_dragAndDrop_text() throws Exception {
        setTextAndCursor("xz", 1);

        ClipData clip = ClipData.newPlainText("test", "y");
        boolean result = onReceive(mReceiver, clip, SOURCE_DRAG_AND_DROP, 0);

        assertThat(result).isTrue();
        assertTextAndCursorPosition("xyz", 2);
    }

    private boolean onReceive(final OnReceiveContentListener receiver, ClipData clip,
            @Source int source, @Flags int flags) {
        ContentInfoCompat payload = new ContentInfoCompat.Builder(clip, source)
                .setFlags(flags)
                .build();
        return receiver.onReceiveContent(mEditText, payload) == null;
    }

    private void setTextAndCursor(final String text, final int cursorPosition) {
        mEditText.requestFocus();
        SpannableStringBuilder ssb = new SpannableStringBuilder(text);
        mEditText.setText(ssb);
        mEditText.setSelection(cursorPosition);
        assertThat(mEditText.hasFocus()).isTrue();
        assertTextAndCursorPosition(text, cursorPosition);
    }

    private void assertTextAndCursorPosition(String expectedText, int cursorPosition) {
        assertTextAndSelection(expectedText, cursorPosition, cursorPosition);
    }

    private void assertTextAndSelection(String expectedText, int start, int end) {
        assertThat(mEditText.getText().toString()).isEqualTo(expectedText);
        assertThat(mEditText.getSelectionStart()).isEqualTo(start);
        assertThat(mEditText.getSelectionEnd()).isEqualTo(end);
    }
}
