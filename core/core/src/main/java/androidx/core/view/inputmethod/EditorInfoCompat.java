/*
 * Copyright (C) 2016 The Android Open Source Project
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

import static android.text.InputType.TYPE_CLASS_NUMBER;
import static android.text.InputType.TYPE_CLASS_TEXT;
import static android.text.InputType.TYPE_MASK_CLASS;
import static android.text.InputType.TYPE_MASK_VARIATION;
import static android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD;
import static android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD;
import static android.text.InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.core.util.Preconditions;

import java.lang.annotation.Retention;

/**
 * Helper for accessing features in {@link EditorInfo} in a backwards compatible fashion.
 */
@SuppressLint("PrivateConstructorForUtilityClass") // Already launched with public constructor
public final class EditorInfoCompat {

    /**
     * Flag of {@link EditorInfo#imeOptions}: used to request that the IME does not update any
     * personalized data such as typing history and personalized language model based on what the
     * user typed on this text editing object.  Typical use cases are:
     * <ul>
     *     <li>When the application is in a special mode, where user's activities are expected to be
     *     not recorded in the application's history.  Some web browsers and chat applications may
     *     have this kind of modes.</li>
     *     <li>When storing typing history does not make much sense.  Specifying this flag in typing
     *     games may help to avoid typing history from being filled up with words that the user is
     *     less likely to type in their daily life.  Another example is that when the application
     *     already knows that the expected input is not a valid word (e.g. a promotion code that is
     *     not a valid word in any natural language).</li>
     * </ul>
     *
     * <p>Applications need to be aware that the flag is not a guarantee, and some IMEs may not
     * respect it.</p>
     */
    public static final int IME_FLAG_NO_PERSONALIZED_LEARNING = 0x1000000;

    /**
     * Flag of {@link EditorInfo#imeOptions}: used to request an IME that is capable of inputting
     * ASCII characters.
     *
     * <p>The intention of this flag is to ensure that the user can type Roman alphabet characters
     * in a {@link android.widget.TextView}. It is typically used for an account ID or password
     * input.</p>
     *
     * <p>In many cases, IMEs are already able to input ASCII even without being told so (such IMEs
     * already respect this flag in a sense), but there are cases when this is not the default. For
     * instance, users of languages using a different script like Arabic, Greek, Hebrew or Russian
     * typically have a keyboard that can't input ASCII characters by default.</p>
     *
     * <p>Applications need to be aware that the flag is not a guarantee, and some IMEs may not
     * respect it. However, it is strongly recommended for IME authors to respect this flag
     * especially when their IME could end up with a state where only languages using non-ASCII are
     * enabled.</p>
     */
    public static final int IME_FLAG_FORCE_ASCII = 0x80000000;

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private static final String CONTENT_MIME_TYPES_KEY =
            "androidx.core.view.inputmethod.EditorInfoCompat.CONTENT_MIME_TYPES";
    private static final String CONTENT_MIME_TYPES_INTEROP_KEY =
            "android.support.v13.view.inputmethod.EditorInfoCompat.CONTENT_MIME_TYPES";
    private static final String CONTENT_SURROUNDING_TEXT_KEY =
            "androidx.core.view.inputmethod.EditorInfoCompat.CONTENT_SURROUNDING_TEXT";
    private static final String CONTENT_SELECTION_HEAD_KEY =
            "androidx.core.view.inputmethod.EditorInfoCompat.CONTENT_SELECTION_HEAD";
    private static final String CONTENT_SELECTION_END_KEY =
            "androidx.core.view.inputmethod.EditorInfoCompat.CONTENT_SELECTION_END";


    @Retention(SOURCE)
    @IntDef({Protocol.Unknown, Protocol.PlatformApi, Protocol.SupportLib, Protocol.AndroidX_1_0_0,
            Protocol.AndroidX_1_1_0})
    @interface Protocol {
        /** Platform API is not available. Backport protocol is also not detected. */
        int Unknown = 0;
        /** Uses platform API. */
        int PlatformApi = 1;
        /** Uses legacy backport protocol that was used by support lib. */
        int SupportLib = 2;
        /** Uses new backport protocol that was accidentally introduced in AndroidX 1.0.0. */
        int AndroidX_1_0_0 = 3;
        /** Uses new backport protocol that was introduced in AndroidX 1.1.0. */
        int AndroidX_1_1_0 = 4;
    }

    @IntDef({TrimPolicy.HEAD, TrimPolicy.TAIL})
    @Retention(SOURCE)
    @interface TrimPolicy {
        int HEAD = 0;
        int TAIL = 1;
    }

    /**
     * The maximum length of initialSurroundingText. When the input text from
     * {@code setInitialSurroundingText(CharSequence)} is longer than this, trimming shall be
     * performed to keep memory efficiency.
     */
    @VisibleForTesting
    static final int MEMORY_EFFICIENT_TEXT_LENGTH = 2048;

    /**
     * When the input text is longer than {@code #MEMORY_EFFICIENT_TEXT_LENGTH}, we start trimming
     * the input text into three parts: BeforeCursor, Selection, and AfterCursor. We don't want to
     * trim the Selection but we also don't want it consumes all available space. Therefore, the
     * maximum acceptable Selection length is half of {@code #MEMORY_EFFICIENT_TEXT_LENGTH}.
     */
    @VisibleForTesting
    static final int MAX_INITIAL_SELECTION_LENGTH =  MEMORY_EFFICIENT_TEXT_LENGTH / 2;

    /**
     * Sets MIME types that can be accepted by the target editor if the IME calls
     * {@link InputConnectionCompat#commitContent(InputConnection, EditorInfo,
     * InputContentInfoCompat, int, Bundle)}.
     *
     * @param editorInfo the editor with which we associate supported MIME types
     * @param contentMimeTypes an array of MIME types. {@code null} and an empty array means that
     *                         {@link InputConnectionCompat#commitContent(
     *                         InputConnection, EditorInfo, InputContentInfoCompat, int, Bundle)}
     *                         is not supported on this Editor
     */
    public static void setContentMimeTypes(@NonNull EditorInfo editorInfo,
            @Nullable String[] contentMimeTypes) {
        if (Build.VERSION.SDK_INT >= 25) {
            editorInfo.contentMimeTypes = contentMimeTypes;
        } else {
            if (editorInfo.extras == null) {
                editorInfo.extras = new Bundle();
            }
            editorInfo.extras.putStringArray(CONTENT_MIME_TYPES_KEY, contentMimeTypes);
            editorInfo.extras.putStringArray(CONTENT_MIME_TYPES_INTEROP_KEY, contentMimeTypes);
        }
    }

    /**
     * Gets MIME types that can be accepted by the target editor if the IME calls
     * {@link InputConnectionCompat#commitContent(InputConnection, EditorInfo,
     * InputContentInfoCompat, int, Bundle)}
     *
     * @param editorInfo the editor from which we get the MIME types
     * @return an array of MIME types. An empty array means that {@link
     * InputConnectionCompat#commitContent(InputConnection, EditorInfo, InputContentInfoCompat,
     * int, Bundle)} is not supported on this editor
     */
    @NonNull
    public static String[] getContentMimeTypes(@NonNull EditorInfo editorInfo) {
        if (Build.VERSION.SDK_INT >= 25) {
            final String[] result = editorInfo.contentMimeTypes;
            return result != null ? result : EMPTY_STRING_ARRAY;
        } else {
            if (editorInfo.extras == null) {
                return EMPTY_STRING_ARRAY;
            }
            String[] result = editorInfo.extras.getStringArray(CONTENT_MIME_TYPES_KEY);
            if (result == null) {
                result = editorInfo.extras.getStringArray(CONTENT_MIME_TYPES_INTEROP_KEY);
            }
            return result != null ? result : EMPTY_STRING_ARRAY;
        }
    }

    /**
     * Editors may use this method to provide initial input text to IMEs. As the surrounding text
     * could be used to provide various input assistance, we recommend editors to provide the
     * complete initial input text in its {@link View#onCreateInputConnection(EditorInfo)} callback.
     * The supplied text will then be processed to serve {@code #getInitialTextBeforeCursor},
     * {@code #getInitialSelectedText}, and {@code #getInitialTextBeforeCursor}. System is allowed
     * to trim {@code sourceText} for various reasons while keeping the most valuable data to IMEs.
     *
     * <p><strong>Editor authors: </strong>Providing the initial input text helps reducing IPC calls
     * for IMEs to provide many modern features right after the connection setup. We recommend
     * calling this method in your implementation.
     *
     * @param sourceText The complete input text.
     */
    public static void setInitialSurroundingText(@NonNull EditorInfo editorInfo,
            @NonNull CharSequence sourceText) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Api30Impl.setInitialSurroundingSubText(editorInfo, sourceText, /* subTextStart= */ 0);
        } else {
            setInitialSurroundingSubText(editorInfo, sourceText, /* subTextStart= */ 0);
        }
    }

    /**
     * Editors may use this method to provide initial input text to IMEs. As the surrounding text
     * could be used to provide various input assistance, we recommend editors to provide the
     * complete initial input text in its {@link View#onCreateInputConnection(EditorInfo)} callback.
     * When trimming the input text is needed, call this method instead of
     * {@code setInitialSurroundingText(CharSequence)} and provide the trimmed position info. Always
     * try to include the selected text within {@code subText} to give the system best flexibility
     * to choose where and how to trim {@code subText} when necessary.
     *
     * @param subText The input text. When it was trimmed, {@code subTextStart} must be provided
     *                correctly.
     * @param subTextStart  The position that the input text got trimmed. For example, when the
     *                      editor wants to trim out the first 10 chars, subTextStart should be 10.
     */
    public static void setInitialSurroundingSubText(@NonNull EditorInfo editorInfo,
            @NonNull CharSequence subText, int subTextStart) {
        Preconditions.checkNotNull(subText);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Api30Impl.setInitialSurroundingSubText(editorInfo, subText, subTextStart);
            return;
        }

        // Swap selection start and end if necessary.
        final int subTextSelStart = editorInfo.initialSelStart > editorInfo.initialSelEnd
                ? editorInfo.initialSelEnd - subTextStart :
                editorInfo.initialSelStart - subTextStart;
        final int subTextSelEnd = editorInfo.initialSelStart > editorInfo.initialSelEnd
                ? editorInfo.initialSelStart - subTextStart :
                editorInfo.initialSelEnd - subTextStart;

        final int subTextLength = subText.length();
        // Unknown or invalid selection.
        if (subTextStart < 0 || subTextSelStart < 0 || subTextSelEnd > subTextLength) {
            setSurroundingText(editorInfo, null, 0, 0);
            return;
        }

        // For privacy protection reason, we don't carry password inputs to IMEs.
        if (isPasswordInputType(editorInfo.inputType)) {
            setSurroundingText(editorInfo, null, 0, 0);
            return;
        }

        if (subTextLength <= MEMORY_EFFICIENT_TEXT_LENGTH) {
            setSurroundingText(editorInfo, subText, subTextSelStart, subTextSelEnd);
            return;
        }

        trimLongSurroundingText(editorInfo, subText, subTextSelStart, subTextSelEnd);
    }

    /**
     * Trims the initial surrounding text when it is over sized. Fundamental trimming rules are:
     * - The text before the cursor is the most important information to IMEs.
     * - The text after the cursor is the second important information to IMEs.
     * - The selected text is the least important information but it shall NEVER be truncated. When
     *    it is too long, just drop it.
     *<p><pre>
     * For example, the subText can be viewed as
     *     TextBeforeCursor + Selection + TextAfterCursor
     * The result could be
     *     1. (maybeTrimmedAtHead)TextBeforeCursor + Selection + TextAfterCursor(maybeTrimmedAtTail)
     *     2. (maybeTrimmedAtHead)TextBeforeCursor + TextAfterCursor(maybeTrimmedAtTail)</pre>
     *
     * @param subText The long text that needs to be trimmed.
     * @param selStart The text offset of the start of the selection.
     * @param selEnd The text offset of the end of the selection
     */
    private static void trimLongSurroundingText(EditorInfo editorInfo, CharSequence subText,
            int selStart, int selEnd) {
        final int sourceSelLength = selEnd - selStart;
        // When the selected text is too long, drop it.
        final int newSelLength = (sourceSelLength > MAX_INITIAL_SELECTION_LENGTH)
                ? 0 : sourceSelLength;

        // Distribute rest of length quota to TextBeforeCursor and TextAfterCursor in 4:1 ratio.
        final int subTextAfterCursorLength = subText.length() - selEnd;
        final int maxLengthMinusSelection = MEMORY_EFFICIENT_TEXT_LENGTH - newSelLength;
        final int possibleMaxBeforeCursorLength =
                Math.min(selStart, (int) (0.8 * maxLengthMinusSelection));
        int newAfterCursorLength = Math.min(subTextAfterCursorLength,
                maxLengthMinusSelection - possibleMaxBeforeCursorLength);
        int newBeforeCursorLength = Math.min(selStart,
                maxLengthMinusSelection - newAfterCursorLength);

        // As trimming may happen at the head of TextBeforeCursor, calculate new starting position.
        int newBeforeCursorHead = selStart - newBeforeCursorLength;

        // We don't want to cut surrogate pairs in the middle. Exam that at the new head and tail.
        if (isCutOnSurrogate(subText,
                selStart - newBeforeCursorLength, TrimPolicy.HEAD)) {
            newBeforeCursorHead = newBeforeCursorHead + 1;
            newBeforeCursorLength = newBeforeCursorLength - 1;
        }
        if (isCutOnSurrogate(subText,
                selEnd + newAfterCursorLength - 1, TrimPolicy.TAIL)) {
            newAfterCursorLength = newAfterCursorLength - 1;
        }

        // Now we know where to trim, compose the initialSurroundingText.
        final int newTextLength = newBeforeCursorLength + newSelLength + newAfterCursorLength;
        final CharSequence newInitialSurroundingText;
        if (newSelLength != sourceSelLength) {
            final CharSequence beforeCursor = subText.subSequence(newBeforeCursorHead,
                    newBeforeCursorHead + newBeforeCursorLength);
            final CharSequence afterCursor = subText.subSequence(selEnd,
                    selEnd + newAfterCursorLength);

            newInitialSurroundingText = TextUtils.concat(beforeCursor, afterCursor);
        } else {
            newInitialSurroundingText = subText
                    .subSequence(newBeforeCursorHead, newBeforeCursorHead + newTextLength);
        }

        // As trimming may happen at the head, adjust cursor position in the initialSurroundingText
        // obj.
        newBeforeCursorHead = 0;
        final int newSelHead = newBeforeCursorHead + newBeforeCursorLength;
        setSurroundingText(
                editorInfo, newInitialSurroundingText, newSelHead, newSelHead + newSelLength);
    }

    /**
     * Get <var>n</var> characters of text before the current cursor position. May be {@code null}
     * when the protocol is not supported.
     *
     * @param length The expected length of the text.
     * @param flags Supplies additional options controlling how the text is returned. May be
     * either 0 or {@link InputConnection#GET_TEXT_WITH_STYLES}.
     * @return the text before the cursor position; the length of the returned text might be less
     * than <var>n</var>. When there is no text before the cursor, an empty string will be returned.
     * It could also be {@code null} when the editor or system could not support this protocol.
     */
    @Nullable
    public static CharSequence getInitialTextBeforeCursor(@NonNull EditorInfo editorInfo,
            int length, int flags) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Api30Impl.getInitialTextBeforeCursor(editorInfo, length, flags);
        }

        if (editorInfo.extras == null) {
            return null;
        }

        final CharSequence surroundingText =
                editorInfo.extras.getCharSequence(CONTENT_SURROUNDING_TEXT_KEY);

        if (surroundingText == null) {
            return null;
        }

        final int selectionHead = editorInfo.extras.getInt(CONTENT_SELECTION_HEAD_KEY);
        final int textLength = Math.min(length, selectionHead);
        return ((flags & InputConnection.GET_TEXT_WITH_STYLES) != 0)
                ? surroundingText.subSequence(selectionHead - textLength, selectionHead)
                : TextUtils.substring(surroundingText, selectionHead - textLength,
                        selectionHead);
    }

    /**
     * Gets the selected text, if any. May be {@code null} when no text is selected or the selected
     * text is way too long.
     *
     * @param flags Supplies additional options controlling how the text is returned. May be
     * either 0 or {@link InputConnection#GET_TEXT_WITH_STYLES}.
     * @return the text that is currently selected, if any. It could be an empty string when there
     * is no text selected. When {@code null} is returned, the selected text might be too long or
     * this protocol is not supported.
     */
    @Nullable
    public static CharSequence getInitialSelectedText(@NonNull EditorInfo editorInfo, int flags) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Api30Impl.getInitialSelectedText(editorInfo, flags);
        }

        if (editorInfo.extras == null) {
            return null;
        }

        // Swap selection start and end if necessary.
        final int correctedTextSelStart = Math.min(editorInfo.initialSelStart,
                editorInfo.initialSelEnd);
        final int correctedTextSelEnd = Math.max(editorInfo.initialSelStart,
                editorInfo.initialSelEnd);

        final int selectionHead = editorInfo.extras.getInt(CONTENT_SELECTION_HEAD_KEY);
        final int selectionEnd = editorInfo.extras.getInt(CONTENT_SELECTION_END_KEY);
        final int sourceSelLength = correctedTextSelEnd - correctedTextSelStart;
        if (editorInfo.initialSelStart < 0 || editorInfo.initialSelEnd < 0
                || (selectionEnd - selectionHead) != sourceSelLength) {
            return null;
        }
        final CharSequence surroundingText =
                editorInfo.extras.getCharSequence(CONTENT_SURROUNDING_TEXT_KEY);

        if (surroundingText == null) {
            return null;
        }
        return ((flags & InputConnection.GET_TEXT_WITH_STYLES) != 0)
                ? surroundingText.subSequence(selectionHead, selectionEnd)
                : TextUtils.substring(surroundingText, selectionHead, selectionEnd);
    }

    /**
     * Get <var>n</var> characters of text after the current cursor position. May be {@code null}
     * when the protocol is not supported.
     *
     * @param length The expected length of the text.
     * @param flags Supplies additional options controlling how the text is returned. May be
     * either 0 or {@link InputConnection#GET_TEXT_WITH_STYLES}.
     * @return the text after the cursor position; the length of the returned text might be less
     * than <var>n</var>. When there is no text after the cursor, an empty string will be returned.
     * It could also be {@code null} when the editor or system could not support this protocol.
     */
    @Nullable
    public static CharSequence getInitialTextAfterCursor(@NonNull EditorInfo editorInfo, int length,
            int flags) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Api30Impl.getInitialTextAfterCursor(editorInfo, length, flags);
        }

        if (editorInfo.extras == null) {
            return null;
        }

        final CharSequence surroundingText =
                editorInfo.extras.getCharSequence(CONTENT_SURROUNDING_TEXT_KEY);

        if (surroundingText == null) {
            return null;
        }
        final int selectionEnd = editorInfo.extras.getInt(CONTENT_SELECTION_END_KEY);
        final int textLength = Math.min(length, surroundingText.length() - selectionEnd);
        return ((flags & InputConnection.GET_TEXT_WITH_STYLES) != 0)
                ? surroundingText.subSequence(selectionEnd, selectionEnd + textLength)
                : TextUtils.substring(surroundingText, selectionEnd, selectionEnd + textLength);
    }

    private static boolean isCutOnSurrogate(CharSequence sourceText, int cutPosition,
            @TrimPolicy int policy) {
        switch (policy) {
            case TrimPolicy.HEAD:
                return Character.isLowSurrogate(sourceText.charAt(cutPosition));
            case TrimPolicy.TAIL:
                return Character.isHighSurrogate(sourceText.charAt(cutPosition));
            default:
                return false;
        }
    }

    private static boolean isPasswordInputType(int inputType) {
        final int variation =
                inputType & (TYPE_MASK_CLASS | TYPE_MASK_VARIATION);
        return variation
                == (TYPE_CLASS_TEXT | TYPE_TEXT_VARIATION_PASSWORD)
                || variation
                == (TYPE_CLASS_TEXT | TYPE_TEXT_VARIATION_WEB_PASSWORD)
                || variation
                == (TYPE_CLASS_NUMBER | TYPE_NUMBER_VARIATION_PASSWORD);
    }

    private static void setSurroundingText(EditorInfo editorInfo,
            CharSequence surroundingText,
            int selectionHead, int selectionEnd) {
        if (editorInfo.extras == null) {
            editorInfo.extras = new Bundle();
        }

        CharSequence surroundingTextCopy = surroundingText != null
                ? new SpannableStringBuilder(surroundingText) : null;
        editorInfo.extras.putCharSequence(CONTENT_SURROUNDING_TEXT_KEY, surroundingTextCopy);
        editorInfo.extras.putInt(CONTENT_SELECTION_HEAD_KEY, selectionHead);
        editorInfo.extras.putInt(CONTENT_SELECTION_END_KEY, selectionEnd);
    }

    /**
     * Returns protocol version to work around an accidental internal key migration happened between
     * legacy support lib and AndroidX 1.0.0.
     *
     * @param editorInfo the editor from which we get the MIME types.
     * @return protocol number based on {@code editorInfo}.
     */
    @Protocol
    static int getProtocol(EditorInfo editorInfo) {
        if (Build.VERSION.SDK_INT >= 25) {
            return Protocol.PlatformApi;
        }
        if (editorInfo.extras == null) {
            return Protocol.Unknown;
        }
        final boolean hasNewKey = editorInfo.extras.containsKey(CONTENT_MIME_TYPES_KEY);
        final boolean hasOldKey = editorInfo.extras.containsKey(CONTENT_MIME_TYPES_INTEROP_KEY);
        if (hasNewKey && hasOldKey) {
            return Protocol.AndroidX_1_1_0;
        }
        if (hasNewKey) {
            return Protocol.AndroidX_1_0_0;
        }
        if (hasOldKey) {
            return Protocol.SupportLib;
        }
        return Protocol.Unknown;
    }

    /** @deprecated This type should not be instantiated as it contains only static methods. */
    @Deprecated
    public EditorInfoCompat() {
    }

    @RequiresApi(30)
    private static class Api30Impl {
        private Api30Impl() {}

        static void setInitialSurroundingSubText(@NonNull EditorInfo editorInfo,
                CharSequence sourceText, int subTextStart) {
            editorInfo.setInitialSurroundingSubText(sourceText, subTextStart);
        }

        static CharSequence getInitialTextBeforeCursor(@NonNull EditorInfo editorInfo,
                int length, int flags) {
            return editorInfo.getInitialTextBeforeCursor(length, flags);
        }

        static CharSequence getInitialSelectedText(@NonNull EditorInfo editorInfo, int flags) {
            return editorInfo.getInitialSelectedText(flags);
        }

        static CharSequence getInitialTextAfterCursor(@NonNull EditorInfo editorInfo, int length,
                int flags) {
            return editorInfo.getInitialTextAfterCursor(length, flags);
        }
    }
}
