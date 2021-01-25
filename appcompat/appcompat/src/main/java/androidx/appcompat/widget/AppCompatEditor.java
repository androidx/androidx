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

import static androidx.core.view.ContentInfoCompat.SOURCE_DRAG_AND_DROP;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build;
import android.text.Selection;
import android.text.Spannable;
import android.util.Log;
import android.view.DragEvent;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.view.ContentInfoCompat;
import androidx.core.view.ViewCompat;

class AppCompatEditor {
    private static final String LOG_TAG = "AppCompatEditor";

    @NonNull
    private final TextView mTextView;

    AppCompatEditor(@NonNull TextView textView) {
        mTextView = textView;
    }

    public boolean onDragEvent(@NonNull DragEvent event) {
        if (Build.VERSION.SDK_INT < 24
                || event.getAction() != DragEvent.ACTION_DROP
                || event.getLocalState() != null
                || ViewCompat.getOnReceiveContentMimeTypes(mTextView) == null) {
            return false;
        }
        // We make a best effort to find the activity for this view by unwrapping the
        // context (common case). If we are not able to find it, we skip calling the
        // OnReceiveContentListener and delegate to the default behavior. Apps can always implement
        // custom drop handling by overriding onDragEvent() or setting an OnDragListener.
        final Activity activity = getActivity();
        if (activity == null) {
            Log.i(LOG_TAG, "No activity so not calling performReceiveContent: " + mTextView);
            return false;
        }
        return Api24Impl.onDrop(event, mTextView, activity);
    }

    @Nullable
    private Activity getActivity() {
        Context context = mTextView.getContext();
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    @RequiresApi(24) // For Activity.requestDragAndDropPermissions()
    private static final class Api24Impl {
        private Api24Impl() {}

        static boolean onDrop(@NonNull DragEvent event,
                @NonNull TextView textView, @NonNull Activity activity) {
            final int offset = textView.getOffsetForPosition(event.getX(), event.getY());
            activity.requestDragAndDropPermissions(event);
            textView.beginBatchEdit();
            try {
                Selection.setSelection((Spannable) textView.getText(), offset);
                final ClipData clip = event.getClipData();
                final ContentInfoCompat payload =
                        new ContentInfoCompat.Builder(clip, SOURCE_DRAG_AND_DROP).build();
                ViewCompat.performReceiveContent(textView, payload);
            } finally {
                textView.endBatchEdit();
            }
            return true;
        }
    }
}
