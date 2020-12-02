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

import android.content.ClipData;
import android.content.Context;
import android.os.Build;
import android.text.Editable;
import android.text.Selection;
import android.text.Spanned;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.Set;

/**
 * Base implementation of {@link RichContentReceiverCompat} for editable {@link TextView}
 * components.
 *
 * <p>This class handles insertion of text (plain text, styled text, HTML, etc) but not images or
 * other rich content. It should be used as a base class when implementing a custom
 * {@link RichContentReceiverCompat}, to provide consistent behavior for insertion of text while
 * implementing custom behavior for insertion of other content (images, etc).
 *
 * <p>See {@link RichContentReceiverCompat} for an example of how to implement a custom receiver.
 */
public abstract class TextViewRichContentReceiverCompat extends
        RichContentReceiverCompat<TextView> {

    private static final Set<String> MIME_TYPES_ALL_TEXT = Collections.singleton("text/*");

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public Set<String> getSupportedMimeTypes() {
        return MIME_TYPES_ALL_TEXT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onReceive(@NonNull TextView textView, @NonNull ClipData clip,
            @Source int source, @Flags int flags) {
        if (source == SOURCE_INPUT_METHOD && !supports(clip.getDescription())) {
            return false;
        }

        // The code here follows the platform logic in TextView:
        // https://cs.android.com/android/_/android/platform/frameworks/base/+/9fefb65aa9e7beae9ca8306b925b9fbfaeffecc9:core/java/android/widget/TextView.java;l=12644
        // In particular, multiple items within the given ClipData will trigger separate calls to
        // replace/insert. This is to preserve the platform behavior with respect to TextWatcher
        // notifications fired from SpannableStringBuilder when replace/insert is called.
        final Editable editable = (Editable) textView.getText();
        final Context context = textView.getContext();
        boolean didFirst = false;
        for (int i = 0; i < clip.getItemCount(); i++) {
            CharSequence paste;
            if ((flags & FLAG_CONVERT_TO_PLAIN_TEXT) != 0) {
                paste = clip.getItemAt(i).coerceToText(context);
                paste = (paste instanceof Spanned) ? paste.toString() : paste;
            } else {
                if (Build.VERSION.SDK_INT >= 16) {
                    paste = clip.getItemAt(i).coerceToStyledText(context);
                } else {
                    paste = clip.getItemAt(i).coerceToText(context);
                }
            }
            if (paste != null) {
                if (!didFirst) {
                    final int selStart = Selection.getSelectionStart(editable);
                    final int selEnd = Selection.getSelectionEnd(editable);
                    final int start = Math.max(0, Math.min(selStart, selEnd));
                    final int end = Math.max(0, Math.max(selStart, selEnd));
                    Selection.setSelection(editable, end);
                    editable.replace(start, end, paste);
                    didFirst = true;
                } else {
                    editable.insert(Selection.getSelectionEnd(editable), "\n");
                    editable.insert(Selection.getSelectionEnd(editable), paste);
                }
            }
        }
        return didFirst;
    }
}
