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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;
import static androidx.core.view.ContentInfoCompat.FLAG_CONVERT_TO_PLAIN_TEXT;
import static androidx.core.view.ContentInfoCompat.SOURCE_INPUT_METHOD;

import android.content.ClipData;
import android.content.Context;
import android.os.Build;
import android.text.Editable;
import android.text.Selection;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.view.ContentInfoCompat;
import androidx.core.view.ContentInfoCompat.Flags;
import androidx.core.view.ContentInfoCompat.Source;
import androidx.core.view.OnReceiveContentListener;

/**
 * Default implementation inserting content into editable {@link TextView} components. This class
 * handles insertion of text (plain text, styled text, HTML, etc) but not images or other content.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
public final class TextViewOnReceiveContentListener implements OnReceiveContentListener {
    private static final String LOG_TAG = "ReceiveContent";

    @Nullable
    @Override
    public ContentInfoCompat onReceiveContent(@NonNull View view,
            @NonNull ContentInfoCompat payload) {
        if (Log.isLoggable(LOG_TAG, Log.DEBUG)) {
            Log.d(LOG_TAG, "onReceive: " + payload);
        }
        final @Source int source = payload.getSource();
        if (source == SOURCE_INPUT_METHOD) {
            // InputConnection.commitContent() should only be used for non-text input which is not
            // supported by the default implementation.
            return payload;
        }

        // The code here follows the platform logic in TextView:
        // https://cs.android.com/android/_/android/platform/frameworks/base/+/9fefb65aa9e7beae9ca8306b925b9fbfaeffecc9:core/java/android/widget/TextView.java;l=12644
        // In particular, multiple items within the given ClipData will trigger separate calls to
        // replace/insert. This is to preserve the platform behavior with respect to TextWatcher
        // notifications fired from SpannableStringBuilder when replace/insert is called.
        final ClipData clip = payload.getClip();
        final @Flags int flags = payload.getFlags();
        final TextView textView = (TextView) view;
        final Editable editable = (Editable) textView.getText();
        final Context context = textView.getContext();
        boolean didFirst = false;
        for (int i = 0; i < clip.getItemCount(); i++) {
            CharSequence itemText = coerceToText(context, clip.getItemAt(i), flags);
            if (itemText != null) {
                if (!didFirst) {
                    replaceSelection(editable, itemText);
                    didFirst = true;
                } else {
                    editable.insert(Selection.getSelectionEnd(editable), "\n");
                    editable.insert(Selection.getSelectionEnd(editable), itemText);
                }
            }
        }
        return null;
    }

    private static CharSequence coerceToText(@NonNull Context context, @NonNull ClipData.Item item,
            @Flags int flags) {
        if (Build.VERSION.SDK_INT >= 16) {
            return Api16Impl.coerce(context, item, flags);
        } else {
            return ApiImpl.coerce(context, item, flags);
        }
    }

    private static void replaceSelection(@NonNull Editable editable,
            @NonNull CharSequence replacement) {
        final int selStart = Selection.getSelectionStart(editable);
        final int selEnd = Selection.getSelectionEnd(editable);
        final int start = Math.max(0, Math.min(selStart, selEnd));
        final int end = Math.max(0, Math.max(selStart, selEnd));
        Selection.setSelection(editable, end);
        editable.replace(start, end, replacement);
    }

    @RequiresApi(16) // For ClipData.Item.coerceToStyledText()
    private static final class Api16Impl {
        private Api16Impl() {}

        static CharSequence coerce(@NonNull Context context, @NonNull ClipData.Item item,
                @Flags int flags) {
            if ((flags & FLAG_CONVERT_TO_PLAIN_TEXT) != 0) {
                CharSequence text = item.coerceToText(context);
                return (text instanceof Spanned) ? text.toString() : text;
            } else {
                return item.coerceToStyledText(context);
            }
        }
    }

    private static final class ApiImpl {
        private ApiImpl() {}

        static CharSequence coerce(@NonNull Context context, @NonNull ClipData.Item item,
                @Flags int flags) {
            CharSequence text = item.coerceToText(context);
            if ((flags & FLAG_CONVERT_TO_PLAIN_TEXT) != 0 && text instanceof Spanned) {
                text = text.toString();
            }
            return text;
        }
    }
}
