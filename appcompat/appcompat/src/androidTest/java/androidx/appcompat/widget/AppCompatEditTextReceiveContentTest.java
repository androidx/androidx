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

import static androidx.core.view.ContentInfoCompat.FLAG_CONVERT_TO_PLAIN_TEXT;
import static androidx.core.view.ContentInfoCompat.SOURCE_CLIPBOARD;
import static androidx.core.view.ContentInfoCompat.SOURCE_DRAG_AND_DROP;
import static androidx.core.view.ContentInfoCompat.SOURCE_INPUT_METHOD;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.SpannableStringBuilder;
import android.view.DragEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputContentInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.test.R;
import androidx.core.util.ObjectsCompat;
import androidx.core.view.ContentInfoCompat;
import androidx.core.view.OnReceiveContentListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.inputmethod.EditorInfoCompat;
import androidx.core.view.inputmethod.InputConnectionCompat;
import androidx.core.view.inputmethod.InputContentInfoCompat;
import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.rule.ActivityTestRule;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class AppCompatEditTextReceiveContentTest {
    private static final String[] MIME_TYPES_IMAGES = new String[] {"image/*"};
    private static final Uri SAMPLE_CONTENT_URI = Uri.parse("content://com.example/path");

    @Rule
    public final ActivityTestRule<AppCompatEditTextReceiveContentActivity> mActivityTestRule =
            new ActivityTestRule<>(AppCompatEditTextReceiveContentActivity.class);

    private Context mContext;
    private AppCompatEditText mEditText;
    private OnReceiveContentListener mMockReceiver;
    private ClipboardManager mClipboardManager;

    @SuppressWarnings("unchecked")
    @UiThreadTest
    @Before
    public void before() {
        AppCompatActivity activity = mActivityTestRule.getActivity();
        mContext = activity;
        mEditText = activity.findViewById(R.id.edit_text);

        mMockReceiver = Mockito.mock(OnReceiveContentListener.class);

        mClipboardManager = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);

        // Clear the clipboard
        if (Build.VERSION.SDK_INT >= 28) {
            mClipboardManager.clearPrimaryClip();
        } else {
            mClipboardManager.setPrimaryClip(ClipData.newPlainText("", ""));
        }
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
        String[] mimeTypes = new String[] {"image/*", "video/mp4"};
        ViewCompat.setOnReceiveContentListener(mEditText, mimeTypes, mMockReceiver);

        // Call onCreateInputConnection() and assert that contentMimeTypes uses the receiver's MIME
        // types.
        EditorInfo editorInfo = new EditorInfo();
        InputConnection ic = mEditText.onCreateInputConnection(editorInfo);
        assertThat(ic).isNotNull();
        verifyZeroInteractions(mMockReceiver);
        assertThat(EditorInfoCompat.getContentMimeTypes(editorInfo)).isEqualTo(mimeTypes);
    }

    // ============================================================================================
    // Tests to verify that the listener is invoked for all the appropriate user interactions:
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
        ViewCompat.setOnReceiveContentListener(mEditText, MIME_TYPES_IMAGES, mMockReceiver);

        // Trigger the "Paste" action and assert that the custom receiver was executed.
        triggerContextMenuAction(android.R.id.paste);
        verify(mMockReceiver, times(1)).onReceiveContent(
                eq(mEditText), payloadEq(clip, SOURCE_CLIPBOARD, 0));
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
        ViewCompat.setOnReceiveContentListener(mEditText, MIME_TYPES_IMAGES, mMockReceiver);

        // Trigger the "Paste" action and assert the boolean it returns.
        ContentInfoCompat toReturn = new ContentInfoCompat.Builder(clip, SOURCE_CLIPBOARD).build();
        when(mMockReceiver.onReceiveContent(eq(mEditText), any(ContentInfoCompat.class)))
                .thenReturn(toReturn);
        boolean result = triggerContextMenuAction(android.R.id.paste);
        assertThat(result).isTrue();

        when(mMockReceiver.onReceiveContent(eq(mEditText), any(ContentInfoCompat.class)))
                .thenReturn(null);
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
        String[] mimeTypes = new String[] {"image/*"};
        ViewCompat.setOnReceiveContentListener(mEditText, mimeTypes, mMockReceiver);

        // Trigger the "Paste" action and assert that the custom receiver was executed. This
        // confirms that the receiver is invoked (give a chance to handle the content via some
        // fallback) even if the MIME type of the content is not one of the receiver's supported
        // MIME types.
        triggerContextMenuAction(android.R.id.paste);
        verify(mMockReceiver, times(1)).onReceiveContent(
                eq(mEditText), payloadEq(clip, SOURCE_CLIPBOARD, 0));
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
        ViewCompat.setOnReceiveContentListener(mEditText, MIME_TYPES_IMAGES, mMockReceiver);

        // Trigger the "Paste as plain text" action and assert that the custom receiver was
        // executed.
        triggerContextMenuAction(android.R.id.pasteAsPlainText);
        verify(mMockReceiver, times(1)).onReceiveContent(
                eq(mEditText), payloadEq(clip, SOURCE_CLIPBOARD, FLAG_CONVERT_TO_PLAIN_TEXT));
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
        ViewCompat.setOnReceiveContentListener(mEditText, MIME_TYPES_IMAGES, mMockReceiver);

        // Trigger the "Paste as plain text" action and assert that the custom receiver was
        // executed. This confirms that the receiver is invoked (given a chance to handle the
        // content via some fallback) even if the MIME type of the content is not one of the
        // receiver's supported MIME types.
        triggerContextMenuAction(android.R.id.pasteAsPlainText);
        verify(mMockReceiver, times(1)).onReceiveContent(
                eq(mEditText), payloadEq(clip, SOURCE_CLIPBOARD, FLAG_CONVERT_TO_PLAIN_TEXT));
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

        // Setup: Configure the receiver to a custom impl.
        ViewCompat.setOnReceiveContentListener(mEditText, MIME_TYPES_IMAGES, mMockReceiver);

        // Trigger the IME's commitContent() call and assert that the custom receiver was executed.
        triggerImeCommitContentViaCompat("image/png");
        ClipData clip = ClipData.newRawUri("", SAMPLE_CONTENT_URI);
        verify(mMockReceiver, times(1)).onReceiveContent(
                eq(mEditText), payloadEq(clip, SOURCE_INPUT_METHOD, 0));
        verifyNoMoreInteractions(mMockReceiver);
    }

    @UiThreadTest
    @Test
    public void testImeCommitContent_withReceiver_resultBoolean() throws Exception {
        setTextAndCursor("xz", 1);

        // Setup: Configure the receiver to a custom impl.
        ViewCompat.setOnReceiveContentListener(mEditText, MIME_TYPES_IMAGES, mMockReceiver);

        // Trigger the IME's commitContent() call and assert the boolean value it returns.
        when(mMockReceiver.onReceiveContent(eq(mEditText), any(ContentInfoCompat.class)))
                .thenReturn(null);
        boolean result1 = triggerImeCommitContentViaCompat("image/png");
        ClipData clip = ClipData.newRawUri("", SAMPLE_CONTENT_URI);
        ContentInfoCompat payloadToReturn =
                new ContentInfoCompat.Builder(clip, SOURCE_INPUT_METHOD).build();
        when(mMockReceiver.onReceiveContent(eq(mEditText), any(ContentInfoCompat.class)))
                .thenReturn(payloadToReturn);
        boolean result2 = triggerImeCommitContentViaCompat("image/png");
        verify(mMockReceiver, times(2)).onReceiveContent(
                eq(mEditText), payloadEq(clip, SOURCE_INPUT_METHOD, 0));
        if (Build.VERSION.SDK_INT >= 25) {
            // On SDK >= 25, the boolean result depends on the return value from the receiver.
            assertThat(result1).isTrue();
            assertThat(result2).isFalse();
        } else {
            // On SDK <= 24, commitContent() is handled via InputConnection.performPrivateCommand().
            // This ends up returning true whenever the command is sent, regardless of the return
            // value of the underlying operation.
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

        // Setup: Configure the receiver to a custom impl.
        ViewCompat.setOnReceiveContentListener(mEditText, MIME_TYPES_IMAGES, mMockReceiver);

        // Trigger the IME's commitContent() call via the support lib and assert that the custom
        // receiver was executed. This confirms that the receiver is invoked (give a chance to
        // handle the content via some fallback) even if the MIME type of the content is not one
        // of the receiver's declared MIME types.
        triggerImeCommitContentViaCompat("video/mp4");
        ClipData clip = ClipData.newRawUri("", SAMPLE_CONTENT_URI);
        verify(mMockReceiver, times(1)).onReceiveContent(
                eq(mEditText), payloadEq(clip, SOURCE_INPUT_METHOD, 0));
        verifyNoMoreInteractions(mMockReceiver);
    }

    @SdkSuppress(minSdkVersion = 25) // InputConnection.commitContent() was added in SDK 25.
    @UiThreadTest
    @Test
    public void testImeCommitContent_direct_withReceiver_unsupportedMimeType() throws Exception {
        setTextAndCursor("xz", 1);

        // Setup: Configure the receiver to a custom impl.
        ViewCompat.setOnReceiveContentListener(mEditText, MIME_TYPES_IMAGES, mMockReceiver);

        // Trigger the IME's commitContent() call and assert that the custom receiver was executed.
        triggerImeCommitContentDirect("video/mp4");
        ClipData clip = ClipData.newRawUri("", SAMPLE_CONTENT_URI);
        verify(mMockReceiver, times(1)).onReceiveContent(
                eq(mEditText), payloadEq(clip, SOURCE_INPUT_METHOD, 0));
        verifyNoMoreInteractions(mMockReceiver);
    }

    @UiThreadTest
    @Test
    public void testImeCommitContent_linkUri() throws Exception {
        setTextAndCursor("xz", 1);

        // Setup: Configure the receiver to a mock impl.
        ViewCompat.setOnReceiveContentListener(mEditText, MIME_TYPES_IMAGES, mMockReceiver);

        // Trigger the IME's commitContent() call with a linkUri and assert receiver extras.
        Uri sampleLinkUri = Uri.parse("http://example.com");
        triggerImeCommitContentViaCompat("image/png", sampleLinkUri, null);
        ClipData clip = ClipData.newRawUri("expected", SAMPLE_CONTENT_URI);
        verify(mMockReceiver, times(1)).onReceiveContent(
                eq(mEditText),
                payloadEq(clip, SOURCE_INPUT_METHOD, 0, sampleLinkUri, null));
    }

    @UiThreadTest
    @Test
    public void testImeCommitContent_opts() throws Exception {
        setTextAndCursor("xz", 1);

        // Setup: Configure the receiver to a mock impl.
        ViewCompat.setOnReceiveContentListener(mEditText, MIME_TYPES_IMAGES, mMockReceiver);

        // Trigger the IME's commitContent() call with opts and assert receiver extras.
        String sampleOptValue = "sampleOptValue";
        triggerImeCommitContentViaCompat("image/png", null, sampleOptValue);
        ClipData clip = ClipData.newRawUri("expected", SAMPLE_CONTENT_URI);
        verify(mMockReceiver, times(1)).onReceiveContent(
                eq(mEditText),
                payloadEq(clip, SOURCE_INPUT_METHOD, 0, null, sampleOptValue));
    }

    @UiThreadTest
    @Test
    public void testImeCommitContent_linkUriAndOpts() throws Exception {
        setTextAndCursor("xz", 1);

        // Setup: Configure the receiver to a mock impl.
        ViewCompat.setOnReceiveContentListener(mEditText, MIME_TYPES_IMAGES, mMockReceiver);

        // Trigger the IME's commitContent() call with a linkUri & opts and assert receiver extras.
        Uri sampleLinkUri = Uri.parse("http://example.com");
        String sampleOptValue = "sampleOptValue";
        triggerImeCommitContentViaCompat("image/png", sampleLinkUri, sampleOptValue);
        ClipData clip = ClipData.newRawUri("expected", SAMPLE_CONTENT_URI);
        verify(mMockReceiver, times(1)).onReceiveContent(
                eq(mEditText),
                payloadEq(clip, SOURCE_INPUT_METHOD, 0, sampleLinkUri, sampleOptValue));
    }

    @UiThreadTest
    @Test
    public void testDragAndDrop_noReceiver() throws Exception {
        setTextAndCursor("xz", 1);

        ClipData clip = ClipData.newPlainText("test", "a");
        clip.addItem(new ClipData.Item("b"));
        boolean result = triggerDropEvent(clip);

        assertThat(result).isTrue();
        if (Build.VERSION.SDK_INT <= 20) {
            // The platform code on Android K and earlier had logic to insert a space before and
            // after the inserted content (if no space was already present). See
            // https://cs.android.com/android/platform/superproject/+/android-4.4.4_r2:frameworks/base/core/java/android/widget/TextView.java;l=8526,8527,8528,8545,8546
            assertTextAndCursorPosition("ab xz", 2);
        } else if (Build.VERSION.SDK_INT <= 30) {
            assertTextAndCursorPosition("abxz", 2);
        } else {
            assertTextAndCursorPosition("a\nbxz", 3);
        }
    }

    @UiThreadTest
    @Test
    public void testDragAndDrop_withReceiver() throws Exception {
        setTextAndCursor("xz", 1);

        ViewCompat.setOnReceiveContentListener(mEditText, MIME_TYPES_IMAGES, mMockReceiver);

        ClipData clip = ClipData.newPlainText("test", "a");
        clip.addItem(new ClipData.Item("b"));
        boolean result = triggerDropEvent(clip);

        assertThat(result).isTrue();
        if (Build.VERSION.SDK_INT >= 24) {
            verify(mMockReceiver, times(1)).onReceiveContent(
                    eq(mEditText), payloadEq(clip, SOURCE_DRAG_AND_DROP, 0));
            verifyNoMoreInteractions(mMockReceiver);
            // Note: The cursor is moved to the location of the drop before calling the receiver.
            assertTextAndCursorPosition("xz", 0);
        } else {
            if (Build.VERSION.SDK_INT <= 20) {
                // The platform code on Android K and earlier had logic to insert a space before and
                // after the inserted content (if no space was already present). See
                // https://cs.android.com/android/platform/superproject/+/android-4.4.4_r2:frameworks/base/core/java/android/widget/TextView.java;l=8526,8527,8528,8545,8546
                assertTextAndCursorPosition("ab xz", 2);
            } else {
                assertTextAndCursorPosition("abxz", 2);
            }
        }
    }

    private boolean triggerContextMenuAction(final int actionId) {
        return mEditText.onTextContextMenuItem(actionId);
    }

    private boolean triggerImeCommitContentViaCompat(String mimeType) {
        return triggerImeCommitContentViaCompat(mimeType, null, null);
    }

    private boolean triggerImeCommitContentViaCompat(String mimeType, Uri linkUri, String extra) {
        final InputContentInfoCompat contentInfo = new InputContentInfoCompat(
                SAMPLE_CONTENT_URI,
                new ClipDescription("from test", new String[]{mimeType}),
                linkUri);
        final Bundle opts;
        if (extra == null) {
            opts = null;
        } else {
            opts = new Bundle();
            opts.putString(PayloadArgumentMatcher.EXTRA_KEY, extra);
        }
        EditorInfo editorInfo = new EditorInfo();
        InputConnection ic = mEditText.onCreateInputConnection(editorInfo);
        int flags = (Build.VERSION.SDK_INT >= 25)
                ? InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION : 0;
        return InputConnectionCompat.commitContent(ic, editorInfo, contentInfo, flags, opts);
    }

    private boolean triggerImeCommitContentDirect(String mimeType) {
        final InputContentInfo contentInfo = new InputContentInfo(
                SAMPLE_CONTENT_URI,
                new ClipDescription("from test", new String[]{mimeType}),
                null);
        EditorInfo editorInfo = new EditorInfo();
        InputConnection ic = mEditText.onCreateInputConnection(editorInfo);
        int flags = (Build.VERSION.SDK_INT >= 25)
                ? InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION : 0;
        return ic.commitContent(contentInfo, flags, null);
    }

    private boolean triggerDropEvent(ClipData clip) {
        DragEvent dropEvent = createDragEvent(DragEvent.ACTION_DROP, mEditText.getX(),
                mEditText.getY(), clip);
        return mEditText.onDragEvent(dropEvent);
    }

    private static DragEvent createDragEvent(int action, float x, float y, ClipData clip) {
        DragEvent dragEvent = mock(DragEvent.class);
        when(dragEvent.getAction()).thenReturn(action);
        when(dragEvent.getX()).thenReturn(x);
        when(dragEvent.getY()).thenReturn(y);
        when(dragEvent.getClipData()).thenReturn(clip);
        return dragEvent;
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

    private static ContentInfoCompat payloadEq(@NonNull ClipData clip, int source, int flags) {
        return argThat(new PayloadArgumentMatcher(clip, source, flags, null, null));
    }

    private static ContentInfoCompat payloadEq(@NonNull ClipData clip, int source, int flags,
            @Nullable Uri linkUri, @Nullable String extra) {
        return argThat(new PayloadArgumentMatcher(clip, source, flags, linkUri, extra));
    }

    private static class PayloadArgumentMatcher implements ArgumentMatcher<ContentInfoCompat> {
        public static final String EXTRA_KEY = "testExtra";

        @NonNull
        private final ClipData mClip;
        private final int mSource;
        private final int mFlags;
        @Nullable
        private final Uri mLinkUri;
        @Nullable
        private final String mExtraValue;

        private PayloadArgumentMatcher(@NonNull ClipData clip, int source, int flags,
                @Nullable Uri linkUri, @Nullable String extraValue) {
            mClip = clip;
            mSource = source;
            mFlags = flags;
            mLinkUri = linkUri;
            mExtraValue = extraValue;
        }

        @NonNull
        @Override
        public String toString() {
            return "[" + "clip=" + mClip + ", source=" + mSource + ", flags=" + mFlags
                    + ", linkUri=" + mLinkUri + ", extraValue=" + mExtraValue + "]";
        }

        @Override
        public boolean matches(ContentInfoCompat actual) {
            ClipData.Item expectedItem = mClip.getItemAt(0);
            ClipData.Item actualItem = actual.getClip().getItemAt(0);
            return ObjectsCompat.equals(expectedItem.getText(), actualItem.getText())
                    && ObjectsCompat.equals(expectedItem.getUri(), actualItem.getUri())
                    && mSource == actual.getSource()
                    && mFlags == actual.getFlags()
                    && ObjectsCompat.equals(mLinkUri, actual.getLinkUri())
                    && extrasMatch(actual.getExtras());
        }

        private boolean extrasMatch(Bundle actualExtras) {
            if (mSource == SOURCE_INPUT_METHOD && Build.VERSION.SDK_INT >= 25
                    && Build.VERSION.SDK_INT <= 30) {
                // On SDK >= 25 and <= 30, when inserting from the keyboard, the InputContentInfo
                // object passed from the IME should be set in the extras. This is needed in order
                // to prevent premature release of URI permissions. Passing this object via the
                // extras is not needed on other SDKs: on  SDK < 25, the IME code handles URI
                // permissions differently (expects the IME to grant URI permissions), and on
                // SDK > 30, this is handled by the platform implementation of the API.
                if (actualExtras == null) {
                    return false;
                }
                Parcelable actualInputContentInfoExtra = actualExtras.getParcelable(
                        "androidx.core.view.extra.INPUT_CONTENT_INFO");
                if (!(actualInputContentInfoExtra instanceof InputContentInfo)) {
                    return false;
                }
            } else if (mExtraValue == null) {
                return actualExtras == null;
            }
            String actualExtraValue = actualExtras.getString(EXTRA_KEY);
            return ObjectsCompat.equals(mExtraValue, actualExtraValue);
        }
    }
}
