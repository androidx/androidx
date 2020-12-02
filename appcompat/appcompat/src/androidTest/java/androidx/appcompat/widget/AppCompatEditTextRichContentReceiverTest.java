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

package androidx.appcompat.widget;

import static androidx.core.widget.RichContentReceiverCompat.FLAG_CONVERT_TO_PLAIN_TEXT;
import static androidx.core.widget.RichContentReceiverCompat.SOURCE_CLIPBOARD;
import static androidx.core.widget.RichContentReceiverCompat.SOURCE_INPUT_METHOD;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.text.SpannableStringBuilder;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputContentInfo;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.test.R;
import androidx.core.view.inputmethod.EditorInfoCompat;
import androidx.core.view.inputmethod.InputConnectionCompat;
import androidx.core.view.inputmethod.InputContentInfoCompat;
import androidx.core.widget.RichContentReceiverCompat;
import androidx.core.widget.TextViewRichContentReceiverCompat;
import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.rule.ActivityTestRule;

import com.google.common.collect.ImmutableSet;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

import java.util.Set;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class AppCompatEditTextRichContentReceiverTest {
    private static final Set<String> ALL_TEXT_AND_IMAGE_MIME_TYPES = ImmutableSet.of(
            "text/*", "image/*");

    @Rule
    public final ActivityTestRule<AppCompatEditTextRichContentReceiverActivity> mActivityTestRule =
            new ActivityTestRule<>(AppCompatEditTextRichContentReceiverActivity.class);

    private Context mContext;
    private AppCompatEditText mEditText;
    private RichContentReceiverCompat<TextView> mMockReceiver;
    private ClipboardManager mClipboardManager;

    @UiThreadTest
    @Before
    public void before() {
        AppCompatActivity activity = mActivityTestRule.getActivity();
        mContext = activity;
        mEditText = activity.findViewById(R.id.edit_text_default_values);

        mMockReceiver = Mockito.mock(RichContentReceiverCompat.class);

        mClipboardManager = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);

        // Clear the clipboard
        if (Build.VERSION.SDK_INT >= 28) {
            mClipboardManager.clearPrimaryClip();
        } else {
            mClipboardManager.setPrimaryClip(ClipData.newPlainText("", ""));
        }
    }

    // ============================================================================================
    // Tests to verify APIs/accessors/defaults related to RichContentReceiver.
    // ============================================================================================

    @UiThreadTest
    @Test
    public void testGetAndSetRichContentReceiverCompat() throws Exception {
        // Verify that by default the getter returns null.
        assertThat(mEditText.getRichContentReceiverCompat()).isNull();

        // Verify that after setting a custom receiver, the getter returns it.
        TextViewRichContentReceiverCompat receiver = new TextViewRichContentReceiverCompat() {};
        mEditText.setRichContentReceiverCompat(receiver);
        assertThat(mEditText.getRichContentReceiverCompat()).isSameInstanceAs(receiver);

        // Verify that the receiver can be reset by passing null.
        mEditText.setRichContentReceiverCompat(null);
        assertThat(mEditText.getRichContentReceiverCompat()).isNull();
    }

    @UiThreadTest
    @Test
    public void testOnCreateInputConnection_nullEditorInfo() throws Exception {
        setTextAndCursor("xz", 1);
        try {
            mEditText.onCreateInputConnection(null);
            Assert.fail("Expected NullPointerException");
        } catch (NullPointerException expected) {
        }
    }

    @UiThreadTest
    @Test
    public void testOnCreateInputConnection_noReceiver() throws Exception {
        setTextAndCursor("xz", 1);

        // Call onCreateInputConnection() and assert that contentMimeTypes is not set.
        EditorInfo editorInfo = new EditorInfo();
        InputConnection ic = mEditText.onCreateInputConnection(editorInfo);
        assertThat(ic).isNotNull();
        assertThat(EditorInfoCompat.getContentMimeTypes(editorInfo)).isEqualTo(new String[0]);
    }

    @UiThreadTest
    @Test
    public void testOnCreateInputConnection_withReceiver() throws Exception {
        setTextAndCursor("xz", 1);

        // Setup: Configure the receiver to a custom impl.
        Set<String> receiverMimeTypes = ImmutableSet.of("text/plain", "image/png", "video/mp4");
        when(mMockReceiver.getSupportedMimeTypes()).thenReturn(receiverMimeTypes);
        mEditText.setRichContentReceiverCompat(mMockReceiver);

        // Call onCreateInputConnection() and assert that contentMimeTypes is set from the receiver.
        EditorInfo editorInfo = new EditorInfo();
        InputConnection ic = mEditText.onCreateInputConnection(editorInfo);
        assertThat(ic).isNotNull();
        verify(mMockReceiver, times(1)).getSupportedMimeTypes();
        verifyNoMoreInteractions(mMockReceiver);
        assertThat(EditorInfoCompat.getContentMimeTypes(editorInfo))
                .isEqualTo(receiverMimeTypes.toArray(new String[0]));
    }

    // ============================================================================================
    // Tests to verify that the receiver callback is invoked for all the appropriate user
    // interactions:
    // * Paste from clipboard ("Paste" and "Paste as plain text" actions)
    // * Content insertion from IME
    // ============================================================================================

    @UiThreadTest
    @Test
    public void testPaste_noReceiver() throws Exception {
        setTextAndCursor("xz", 1);

        // Setup: Copy text to the clipboard.
        ClipData clip = ClipData.newPlainText("test", "y");
        clip = copyToClipboard(clip);

        // Trigger the "Paste" action. This should execute the platform paste handling, so the
        // content should be inserted according to whatever behavior is implemented in the OS
        // version that's running.
        boolean result = triggerContextMenuAction(android.R.id.paste);
        assertThat(result).isTrue();
        if (Build.VERSION.SDK_INT <= 20) {
            // The platform code on Android K and earlier had logic to insert a space before and
            // after the pasted content (if no space was already present). See
            // https://cs.android.com/android/platform/superproject/+/android-4.4.4_r2:frameworks/base/core/java/android/widget/TextView.java;l=8526,8527,8528,8545,8546
            assertTextAndCursorPosition("x y z", 3);
        } else {
            assertTextAndCursorPosition("xyz", 2);
        }
    }

    @UiThreadTest
    @Test
    public void testPaste_withReceiver() throws Exception {
        setTextAndCursor("xz", 1);

        // Setup: Copy text to the clipboard.
        ClipData clip = ClipData.newPlainText("test", "y");
        clip = copyToClipboard(clip);

        // Setup: Configure to use the mock receiver.
        mEditText.setRichContentReceiverCompat(mMockReceiver);

        // Trigger the "Paste" action and assert that the custom receiver was executed.
        triggerContextMenuAction(android.R.id.paste);
        verify(mMockReceiver, times(1)).onReceive(
                eq(mEditText), clipEq(clip), eq(SOURCE_CLIPBOARD), eq(0));
        verifyNoMoreInteractions(mMockReceiver);
    }

    @UiThreadTest
    @Test
    public void testPaste_withReceiver_resultBoolean() throws Exception {
        setTextAndCursor("xz", 1);

        // Setup: Copy text to the clipboard.
        ClipData clip = ClipData.newPlainText("test", "y");
        clip = copyToClipboard(clip);

        // Setup: Configure to use the mock receiver.
        mEditText.setRichContentReceiverCompat(mMockReceiver);

        // Trigger the "Paste" action and assert that the boolean result is true regardless of
        // the receiver's return value.
        when(mMockReceiver.onReceive(eq(mEditText), eq(clip), eq(SOURCE_CLIPBOARD),
                eq(FLAG_CONVERT_TO_PLAIN_TEXT))).thenReturn(true);
        boolean result = triggerContextMenuAction(android.R.id.paste);
        assertThat(result).isTrue();

        when(mMockReceiver.onReceive(eq(mEditText), eq(clip), eq(SOURCE_CLIPBOARD),
                eq(FLAG_CONVERT_TO_PLAIN_TEXT))).thenReturn(false);
        result = triggerContextMenuAction(android.R.id.paste);
        assertThat(result).isTrue();
    }

    @UiThreadTest
    @Test
    public void testPaste_withReceiver_unsupportedMimeType() throws Exception {
        setTextAndCursor("xz", 1);

        // Setup: Copy a URI to the clipboard with a MIME type that's not supported by the receiver.
        ClipData clip = new ClipData("test", new String[]{"video/mp4"},
                new ClipData.Item("text", null, Uri.parse("content://com.example/path")));
        clip = copyToClipboard(clip);

        // Setup: Configure to use the mock receiver.
        mEditText.setRichContentReceiverCompat(mMockReceiver);

        // Trigger the "Paste" action and assert that the custom receiver was executed. This
        // confirms that the receiver is invoked (give a chance to handle the content via some
        // fallback) even if the MIME type of the content is not one of the receiver's supported
        // MIME types.
        triggerContextMenuAction(android.R.id.paste);
        verify(mMockReceiver, times(1)).onReceive(
                eq(mEditText), clipEq(clip), eq(SOURCE_CLIPBOARD), eq(0));
        verifyNoMoreInteractions(mMockReceiver);
    }

    @SdkSuppress(minSdkVersion = 23) // The action "Paste as plain text" was added in SDK 23.
    @UiThreadTest
    @Test
    public void testPasteAsPlainText_noReceiver() throws Exception {
        setTextAndCursor("xz", 1);

        // Setup: Copy HTML to the clipboard.
        ClipData clip = ClipData.newHtmlText("test", "*y*", "<b>y</b>");
        clip = copyToClipboard(clip);

        // Trigger the "Paste as plain text" action. This should execute the platform paste
        // handling, so the content should be inserted according to whatever behavior is implemented
        // in the OS version that's running.
        boolean result = triggerContextMenuAction(android.R.id.pasteAsPlainText);
        assertThat(result).isTrue();
        assertTextAndCursorPosition("x*y*z", 4);
    }

    @UiThreadTest
    @Test
    public void testPasteAsPlainText_withReceiver() throws Exception {
        setTextAndCursor("xz", 1);

        // Setup: Copy text to the clipboard.
        ClipData clip = ClipData.newPlainText("test", "y");
        clip = copyToClipboard(clip);

        // Setup: Configure to use the mock receiver.
        mEditText.setRichContentReceiverCompat(mMockReceiver);

        // Trigger the "Paste as plain text" action and assert that the custom receiver was
        // executed.
        triggerContextMenuAction(android.R.id.pasteAsPlainText);
        verify(mMockReceiver, times(1)).onReceive(
                eq(mEditText), clipEq(clip),
                eq(SOURCE_CLIPBOARD), eq(FLAG_CONVERT_TO_PLAIN_TEXT));
        verifyNoMoreInteractions(mMockReceiver);
    }

    @UiThreadTest
    @Test
    public void testPasteAsPlainText_withReceiver_unsupportedMimeType() throws Exception {
        setTextAndCursor("xz", 1);

        // Setup: Copy a URI to the clipboard with a MIME type that's not supported by the receiver.
        ClipData clip = new ClipData("test", new String[]{"video/mp4"},
                new ClipData.Item("text", null, Uri.parse("content://com.example/path")));
        clip = copyToClipboard(clip);

        // Setup: Configure to use the mock receiver.
        mEditText.setRichContentReceiverCompat(mMockReceiver);

        // Trigger the "Paste as plain text" action and assert that the custom receiver was
        // executed. This confirms that the receiver is invoked (given a chance to handle the
        // content via some fallback) even if the MIME type of the content is not one of the
        // receiver's supported MIME types.
        triggerContextMenuAction(android.R.id.pasteAsPlainText);
        verify(mMockReceiver, times(1)).onReceive(
                eq(mEditText), clipEq(clip),
                eq(SOURCE_CLIPBOARD), eq(FLAG_CONVERT_TO_PLAIN_TEXT));
        verifyNoMoreInteractions(mMockReceiver);
    }

    @UiThreadTest
    @Test
    public void testImeCommitContent_noReceiver() throws Exception {
        setTextAndCursor("xz", 1);

        // Trigger the IME's commitContent() call and assert its outcome.
        boolean result = triggerImeCommitContentViaCompat("image/png");
        assertThat(result).isFalse();
        assertTextAndCursorPosition("xz", 1);
    }

    @UiThreadTest
    @Test
    public void testImeCommitContent_withReceiver() throws Exception {
        setTextAndCursor("xz", 1);

        // Setup: Configure the receiver to a custom impl that supports all text and images.
        when(mMockReceiver.getSupportedMimeTypes()).thenReturn(ALL_TEXT_AND_IMAGE_MIME_TYPES);
        mEditText.setRichContentReceiverCompat(mMockReceiver);

        // Trigger the IME's commitContent() call and assert that the custom receiver was executed.
        triggerImeCommitContentViaCompat("image/png");
        verify(mMockReceiver, times(1)).getSupportedMimeTypes();
        verify(mMockReceiver, times(1)).onReceive(
                eq(mEditText), any(ClipData.class), eq(SOURCE_INPUT_METHOD), eq(0));
        verifyNoMoreInteractions(mMockReceiver);
    }

    @UiThreadTest
    @Test
    public void testImeCommitContent_withReceiver_resultBoolean() throws Exception {
        setTextAndCursor("xz", 1);

        // Setup: Configure the receiver to a custom impl that supports all text and images.
        when(mMockReceiver.getSupportedMimeTypes()).thenReturn(ALL_TEXT_AND_IMAGE_MIME_TYPES);
        mEditText.setRichContentReceiverCompat(mMockReceiver);

        // Trigger the IME's commitContent() call, once when the mock receiver is configured to
        // return true and once when the mock receiver is configured to return false.
        when(mMockReceiver.onReceive(eq(mEditText), any(ClipData.class), eq(SOURCE_INPUT_METHOD),
                eq(0))).thenReturn(true);
        boolean result1 = triggerImeCommitContentViaCompat("image/png");
        when(mMockReceiver.onReceive(eq(mEditText), any(ClipData.class), eq(SOURCE_INPUT_METHOD),
                eq(0))).thenReturn(false);
        boolean result2 = triggerImeCommitContentViaCompat("image/png");
        verify(mMockReceiver, times(2)).onReceive(
                eq(mEditText), any(ClipData.class), eq(SOURCE_INPUT_METHOD), eq(0));
        if (Build.VERSION.SDK_INT >= 25) {
            // On SDK 25 and above, the boolean result should match the return value from the
            // receiver.
            assertThat(result1).isTrue();
            assertThat(result2).isFalse();
        } else {
            // On SDK 24 and below, commitContent() is handled via
            // InputConnection.performPrivateCommand(). This ends up returning true whenever the
            // command is sent, regardless of the return value of the underlying operation.
            // Relevant code links:
            // https://osscs.corp.google.com/androidx/platform/frameworks/support/+/androidx-master-dev:core/core/src/main/java/androidx/core/view/inputmethod/InputConnectionCompat.java;l=294;drc=0c365e84832f5ec5e393be28ab1c618eb18bab1e
            // https://cs.android.com/android/platform/superproject/+/android-7.0.0_r6:frameworks/base/core/java/com/android/internal/widget/EditableInputConnection.java;l=168
            assertThat(result1).isTrue();
            assertThat(result2).isTrue();
        }
    }

    @UiThreadTest
    @Test
    public void testImeCommitContent_withReceiver_unsupportedMimeType() throws Exception {
        setTextAndCursor("xz", 1);

        // Setup: Configure the receiver to a custom impl that supports all text and images.
        when(mMockReceiver.getSupportedMimeTypes()).thenReturn(ALL_TEXT_AND_IMAGE_MIME_TYPES);
        mEditText.setRichContentReceiverCompat(mMockReceiver);

        // Trigger the IME's commitContent() call and assert that the custom receiver was not
        // executed. This is because InputConnectionCompat.commitContent() checks the supported MIME
        // types before proceeding.
        triggerImeCommitContentViaCompat("video/mp4");
        verify(mMockReceiver, times(1)).getSupportedMimeTypes();
        verifyNoMoreInteractions(mMockReceiver);
    }

    @SdkSuppress(minSdkVersion = 25) // InputConnection.commitContent() was added in SDK 25.
    @UiThreadTest
    @Test
    public void testImeCommitContent_direct_withReceiver_unsupportedMimeType() throws Exception {
        setTextAndCursor("xz", 1);

        // Setup: Configure the receiver to a custom impl that supports all text and images.
        when(mMockReceiver.getSupportedMimeTypes()).thenReturn(ALL_TEXT_AND_IMAGE_MIME_TYPES);
        mEditText.setRichContentReceiverCompat(mMockReceiver);

        // Trigger the IME's commitContent() call and assert that the custom receiver was executed.
        triggerImeCommitContentDirect("video/mp4");
        verify(mMockReceiver, times(1)).getSupportedMimeTypes();
        verify(mMockReceiver, times(1)).onReceive(
                eq(mEditText), any(ClipData.class), eq(SOURCE_INPUT_METHOD), eq(0));
        verifyNoMoreInteractions(mMockReceiver);
    }

    private boolean triggerContextMenuAction(final int actionId) {
        return mEditText.onTextContextMenuItem(actionId);
    }

    private boolean triggerImeCommitContentViaCompat(String mimeType) {
        final InputContentInfoCompat contentInfo = new InputContentInfoCompat(
                Uri.parse("content://com.example/path"),
                new ClipDescription("from test", new String[]{mimeType}),
                Uri.parse("https://example.com"));
        EditorInfo editorInfo = new EditorInfo();
        InputConnection ic = mEditText.onCreateInputConnection(editorInfo);
        return InputConnectionCompat.commitContent(ic, editorInfo, contentInfo, 0, null);
    }

    private boolean triggerImeCommitContentDirect(String mimeType) {
        final InputContentInfo contentInfo = new InputContentInfo(
                Uri.parse("content://com.example/path"),
                new ClipDescription("from test", new String[]{mimeType}),
                Uri.parse("https://example.com"));
        EditorInfo editorInfo = new EditorInfo();
        InputConnection ic = mEditText.onCreateInputConnection(editorInfo);
        return ic.commitContent(contentInfo, 0, null);
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
        assertThat(mEditText.getText().toString()).isEqualTo(expectedText);
        assertThat(mEditText.getSelectionStart()).isEqualTo(cursorPosition);
        assertThat(mEditText.getSelectionEnd()).isEqualTo(cursorPosition);
    }

    private ClipData copyToClipboard(final ClipData clip) {
        mClipboardManager.setPrimaryClip(clip);
        ClipData primaryClip = mClipboardManager.getPrimaryClip();
        assertThat(primaryClip).isNotNull();
        return primaryClip;
    }

    private static ClipData clipEq(ClipData expected) {
        return argThat(new ClipDataArgumentMatcher(expected));
    }

    private static class ClipDataArgumentMatcher implements ArgumentMatcher<ClipData> {
        private final ClipData mExpected;

        private ClipDataArgumentMatcher(ClipData expected) {
            this.mExpected = expected;
        }

        @Override
        public boolean matches(ClipData actual) {
            return mExpected.getItemAt(0).getText().equals(actual.getItemAt(0).getText());
        }
    }
}
