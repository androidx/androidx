/*
 * Copyright 2021 The Android Open Source Project
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
import static androidx.core.view.inputmethod.InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build;
import android.os.Bundle;
import android.text.Selection;
import android.text.Spannable;
import android.util.Log;
import android.view.DragEvent;
import android.view.View;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputContentInfo;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.view.ContentInfoCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.inputmethod.InputConnectionCompat;
import androidx.core.view.inputmethod.InputContentInfoCompat;

/**
 * Common code for handling content via {@link ViewCompat#performReceiveContent}.
 */
final class AppCompatReceiveContentHelper {
    private AppCompatReceiveContentHelper() {}

    private static final String LOG_TAG = "ReceiveContent";

    /**
     * If the menu action is either "Paste" or "Paste as plain text" and the view has a
     * {@link androidx.core.view.OnReceiveContentListener}, use the listener to handle the paste.
     *
     * @return true if the action was handled; false otherwise
     */
    static boolean maybeHandleMenuActionViaPerformReceiveContent(@NonNull TextView view,
            int menuItemId) {
        if (!(menuItemId == android.R.id.paste || menuItemId == android.R.id.pasteAsPlainText)
                || ViewCompat.getOnReceiveContentMimeTypes(view) == null) {
            return false;
        }
        ClipboardManager cm = (ClipboardManager) view.getContext().getSystemService(
                Context.CLIPBOARD_SERVICE);
        ClipData clip = (cm == null) ? null : cm.getPrimaryClip();
        if (clip != null && clip.getItemCount() > 0) {
            ContentInfoCompat payload = new ContentInfoCompat.Builder(clip, SOURCE_CLIPBOARD)
                    .setFlags((menuItemId == android.R.id.paste) ? 0 : FLAG_CONVERT_TO_PLAIN_TEXT)
                    .build();
            ViewCompat.performReceiveContent(view, payload);
        }
        return true;
    }

    /**
     * If the given view has a {@link androidx.core.view.OnReceiveContentListener}, try to handle
     * drag-and-drop via the listener.
     *
     * @return true if the event was handled; false otherwise
     */
    static boolean maybeHandleDragEventViaPerformReceiveContent(@NonNull View view,
            @NonNull DragEvent event) {
        if (Build.VERSION.SDK_INT < 24
                || event.getLocalState() != null
                || ViewCompat.getOnReceiveContentMimeTypes(view) == null) {
            return false;
        }
        // We make a best effort to find the activity for this view by unwrapping the context.
        // If we are not able to find it, we can't provide default drag-and-drop handling via
        // OnReceiveContentListener. If that happens an app can still implement custom handling
        // using an OnDragListener or by overriding onDragEvent().
        final Activity activity = tryGetActivity(view);
        if (activity == null) {
            Log.i(LOG_TAG, "Can't handle drop: no activity: view=" + view);
            return false;
        }
        if (event.getAction() == DragEvent.ACTION_DRAG_STARTED) {
            // We need onDragEvent to return true for ACTION_DRAG_STARTED in order to be notified
            // of further drag events for the current drag action. TextView has the appropriate
            // logic to return true for ACTION_DRAG_STARTED if the TextView is editable. Other
            // widgets don't have default handling for drag-and-drop, so we return true ourselves
            // here.
            return !(view instanceof TextView);
        }
        if (event.getAction() == DragEvent.ACTION_DROP) {
            return (view instanceof TextView)
                    ? OnDropApi24Impl.onDropForTextView(event, (TextView) view, activity)
                    : OnDropApi24Impl.onDropForView(event, view, activity);
        }
        return false;
    }

    @RequiresApi(24) // For Activity.requestDragAndDropPermissions()
    private static final class OnDropApi24Impl {
        private OnDropApi24Impl() {}

        static boolean onDropForTextView(@NonNull DragEvent event, @NonNull TextView view,
                @NonNull Activity activity) {
            activity.requestDragAndDropPermissions(event);
            final int offset = view.getOffsetForPosition(event.getX(), event.getY());
            view.beginBatchEdit();
            try {
                Selection.setSelection((Spannable) view.getText(), offset);
                final ContentInfoCompat payload = new ContentInfoCompat.Builder(
                        event.getClipData(), SOURCE_DRAG_AND_DROP).build();
                ViewCompat.performReceiveContent(view, payload);
            } finally {
                view.endBatchEdit();
            }
            return true;
        }

        static boolean onDropForView(@NonNull DragEvent event, @NonNull View view,
                @NonNull Activity activity) {
            activity.requestDragAndDropPermissions(event);
            final ContentInfoCompat payload = new ContentInfoCompat.Builder(
                    event.getClipData(), SOURCE_DRAG_AND_DROP).build();
            ViewCompat.performReceiveContent(view, payload);
            return true;
        }
    }

    /**
     * Attempts to find the activity for the given view by unwrapping the view's context. This is
     * a "best effort" approach that's not guaranteed to get the activity, since a view's context
     * is not necessarily an activity.
     *
     * @param view The target view.
     * @return The activity if found; null otherwise.
     */
    @Nullable
    static Activity tryGetActivity(@NonNull View view) {
        Context context = view.getContext();
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    /**
     * Creates an {@link InputConnectionCompat.OnCommitContentListener} that uses
     * {@link ViewCompat#performReceiveContent} to insert content. The listener returned by this
     * function should be passed to {@link InputConnectionCompat#createWrapper} when creating the
     * {@link InputConnection} in {@link View#onCreateInputConnection}.
     */
    // TODO(b/178324480): Make this a public API on InputConnectionCompat
    @NonNull
    static InputConnectionCompat.OnCommitContentListener createOnCommitContentListener(
            @NonNull final View view) {
        return new InputConnectionCompat.OnCommitContentListener() {
            @Override
            public boolean onCommitContent(InputContentInfoCompat inputContentInfo, int flags,
                    Bundle opts) {
                Bundle extras = opts;
                if (Build.VERSION.SDK_INT >= 25
                        && (flags & INPUT_CONTENT_GRANT_READ_URI_PERMISSION) != 0) {
                    try {
                        inputContentInfo.requestPermission();
                    } catch (Exception e) {
                        Log.w(LOG_TAG,
                                "Can't insert content from IME; requestPermission() failed", e);
                        return false;
                    }
                    // Permissions granted above are revoked automatically by the platform when the
                    // corresponding InputContentInfo object is garbage collected. To prevent
                    // this from happening prematurely (before the receiving app has had a chance
                    // to process the content), we set the InputContentInfo object into the
                    // extras of the payload passed to OnReceiveContentListener.
                    InputContentInfo inputContentInfoFmk =
                            (InputContentInfo) inputContentInfo.unwrap();
                    extras = (opts == null) ? new Bundle() : new Bundle(opts);
                    extras.putParcelable(EXTRA_INPUT_CONTENT_INFO, inputContentInfoFmk);
                }
                ClipData clip = new ClipData(inputContentInfo.getDescription(),
                        new ClipData.Item(inputContentInfo.getContentUri()));
                ContentInfoCompat payload = new ContentInfoCompat.Builder(clip, SOURCE_INPUT_METHOD)
                        .setLinkUri(inputContentInfo.getLinkUri())
                        .setExtras(extras)
                        .build();
                return ViewCompat.performReceiveContent(view, payload) == null;
            }
        };
    }

    /**
     * Key for extras in {@link ContentInfoCompat}, to hold the {@link InputContentInfo} object
     * passed by the IME. Apps should not access/read this object; it is only set in the extras
     * in order to prevent premature garbage collection of {@link InputContentInfo} which in
     * turn causes premature revocation of URI permissions.
     */
    private static final String EXTRA_INPUT_CONTENT_INFO =
            "androidx.core.view.extra.INPUT_CONTENT_INFO";
}
