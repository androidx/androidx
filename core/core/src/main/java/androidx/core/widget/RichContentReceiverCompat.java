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
import android.content.ClipDescription;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.view.inputmethod.EditorInfoCompat;
import androidx.core.view.inputmethod.InputConnectionCompat;
import androidx.core.view.inputmethod.InputContentInfoCompat;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;

/**
 * Callback for apps to implement handling for insertion of rich content. "Rich content" here refers
 * to both text and non-text content: plain text, styled text, HTML, images, videos, audio files,
 * etc.
 *
 * <p>This callback can be attached to different types of UI components. For editable
 * {@link android.widget.TextView} components, implementations should typically extend from
 * {@link TextViewRichContentReceiverCompat}.
 *
 * <p>Example implementation:<br>
 * <pre class="prettyprint">
 *   public class MyRichContentReceiver extends TextViewRichContentReceiverCompat {
 *
 *       private static final Set&lt;String&gt; SUPPORTED_MIME_TYPES = Collections.unmodifiableSet(
 *           Set.of("text/*", "image/gif", "image/png", "image/jpg"));
 *
 *       &#64;NonNull
 *       &#64;Override
 *       public Set&lt;String&gt; getSupportedMimeTypes() {
 *           return SUPPORTED_MIME_TYPES;
 *       }
 *
 *       &#64;Override
 *       public boolean onReceive(@NonNull TextView textView, @NonNull ClipData clip,
 *               int source, int flags) {
 *         if (clip.getDescription().hasMimeType("image/*")) {
 *             return receiveImage(textView, clip);
 *         }
 *         return super.onReceive(textView, clip, source);
 *       }
 *
 *       private boolean receiveImage(@NonNull TextView textView, @NonNull ClipData clip) {
 *           // ... app-specific logic to handle the content URI in the clip ...
 *       }
 *   }
 * </pre>
 *
 * @param <T> The type of {@link View} with which this receiver can be associated.
 */
public abstract class RichContentReceiverCompat<T extends View> {
    private static final String TAG = "RichContentReceiver";

    /**
     * Specifies the UI through which content is being inserted.
     */
    @IntDef(value = {SOURCE_CLIPBOARD, SOURCE_INPUT_METHOD})
    @Retention(RetentionPolicy.SOURCE)
    @interface Source {}

    /**
     * Specifies that the operation was triggered by a paste from the clipboard (e.g. "Paste" or
     * "Paste as plain text" action in the insertion/selection menu).
     */
    public static final int SOURCE_CLIPBOARD = 0;

    /**
     * Specifies that the operation was triggered from the soft keyboard (also known as input method
     * editor or IME). See https://developer.android.com/guide/topics/text/image-keyboard for more
     * info.
     */
    public static final int SOURCE_INPUT_METHOD = 1;

    /**
     * Flags to configure the insertion behavior.
     */
    @IntDef(flag = true, value = {FLAG_CONVERT_TO_PLAIN_TEXT})
    @Retention(RetentionPolicy.SOURCE)
    @interface Flags {}

    /**
     * Flag for {@link #onReceive} requesting that the content should be converted to plain text
     * prior to inserting.
     */
    public static final int FLAG_CONVERT_TO_PLAIN_TEXT = 1 << 0;

    /**
     * Insert the given clip.
     *
     * <p>For a UI component where this callback is set, this function will be invoked in the
     * following scenarios:
     * <ol>
     *     <li>Paste from the clipboard (e.g. "Paste" or "Paste as plain text" action in the
     *     insertion/selection menu)
     *     <li>Content insertion from the keyboard ({@link InputConnection#commitContent})
     * </ol>
     *
     * <p>For text, if the view has a selection, the selection should be overwritten by the
     * clip; if there's no selection, this method should insert the content at the current
     * cursor position.
     *
     * <p>For rich content (e.g. an image), this function may insert the content inline, or it may
     * add the content as an attachment (could potentially go into a completely separate view).
     *
     * <p>This function may be invoked with a clip whose MIME type is not in the list of supported
     * types returned by {@link #getSupportedMimeTypes()}. This provides the opportunity to
     * implement custom fallback logic if desired.
     *
     * @param view   The view where the content insertion was requested.
     * @param clip   The clip to insert.
     * @param source The trigger of the operation.
     * @param flags  Optional flags to configure the insertion behavior. Use 0 for default
     *               behavior. See {@code FLAG_} constants on this class for other options.
     * @return Returns true if the clip was inserted.
     */
    public abstract boolean onReceive(@NonNull T view, @NonNull ClipData clip, @Source int source,
            @Flags int flags);

    /**
     * Returns the MIME types that can be handled by this callback.
     *
     * <p>Different platform features (e.g. pasting from the clipboard, inserting stickers from the
     * keyboard, etc) may use this function to conditionally alter their behavior. For example, the
     * keyboard may choose to hide its UI for inserting GIFs if the input field that has focus has
     * a {@link RichContentReceiverCompat} set and the MIME types returned from this function
     * don't include "image/gif".
     *
     * @return An immutable set with the MIME types supported by this callback. The returned
     * MIME types may contain wildcards such as "text/*", "image/*", etc.
     */
    @NonNull
    public abstract Set<String> getSupportedMimeTypes();

    /**
     * Returns true if the MIME type of the given clip is {@link #getSupportedMimeTypes() supported}
     * by this receiver.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public final boolean supports(@NonNull ClipDescription description) {
        for (String supportedMimeType : getSupportedMimeTypes()) {
            if (description.hasMimeType(supportedMimeType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Populates {@code outAttrs.contentMimeTypes} with the supported MIME types of this receiver.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public final void populateEditorInfoContentMimeTypes(@Nullable InputConnection ic,
            @Nullable EditorInfo outAttrs) {
        if (ic == null || outAttrs == null) {
            return;
        }
        String[] mimeTypes = getSupportedMimeTypes().toArray(new String[0]);
        EditorInfoCompat.setContentMimeTypes(outAttrs, mimeTypes);
    }

    /**
     * Creates an {@link InputConnectionCompat.OnCommitContentListener} that uses this receiver
     * to insert content. The object returned by this function should be passed to
     * {@link InputConnectionCompat#createWrapper} when creating the {@link InputConnection} in
     * {@link View#onCreateInputConnection}.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @NonNull
    public final InputConnectionCompat.OnCommitContentListener buildOnCommitContentListener(
            @NonNull final T view) {
        return new InputConnectionCompat.OnCommitContentListener() {
            @Override
            public boolean onCommitContent(InputContentInfoCompat content, int flags,
                    Bundle opts) {
                ClipDescription description = content.getDescription();
                if ((flags & InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION) != 0) {
                    try {
                        content.requestPermission();
                    } catch (Exception e) {
                        Log.w(TAG, "Can't insert from IME; requestPermission() failed: " + e);
                        return false; // Can't insert the content if we don't have permission
                    }
                }
                ClipData clip = new ClipData(description,
                        new ClipData.Item(content.getContentUri()));
                return onReceive(view, clip, SOURCE_INPUT_METHOD, 0);
            }
        };
    }
}
