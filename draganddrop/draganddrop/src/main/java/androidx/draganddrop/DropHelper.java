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

package androidx.draganddrop;

import android.app.Activity;
import android.content.ClipData;
import android.os.Build;
import android.view.DragAndDropPermissions;
import android.view.DragEvent;
import android.view.View;
import android.view.View.OnDragListener;
import android.widget.EditText;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.core.view.ContentInfoCompat;
import androidx.core.view.OnReceiveContentListener;
import androidx.core.view.ViewCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class is used to configure {@code View}s to receive data dropped via drag-and-drop. It also
 * adds highlighting when the user is dragging, to indicate to the user where they can drop. It
 * will also add support for content insertion via all methods supported by
 * {@link OnReceiveContentListener}s, when supported.
 *
 * <p>All {@code EditText}(s) in the drop target View's descendant tree (i.e. any contained within
 * the drop target) <b>must</b> be provided via
 * {@link Options.Builder#addInnerEditTexts(EditText...)}. This is to ensure the
 * highlighting works correctly. Without this, the EditText will "steal" the focus during
 * the drag-and-drop operation, causing undesired highlighting behavior.
 *
 * <p>If the user is dragging text data in the DragEvent alongside URI data, one of the EditTexts
 * will be chosen to handle that additional text data. If the user has positioned the cursor in one
 * of the EditTexts, or drops directly on it, that will be correctly delegated to. If not, the first
 * one provided is the one that will be used as a default. For example, if your drop target handles
 * images, and contains two editable text fields, you should ensure the one that you want to receive
 * the additional text by default is provided first to
 * {@link Options.Builder#addInnerEditTexts(EditText...)}.
 *
 * <p>Under the hood, this attaches an {@link OnReceiveContentListener}. It will also attach an
 * {@link OnDragListener}. It is not recommended to attach either of these manually if using
 * {@link DropHelper}.
 *
 * <p>This requires Android N+.
 *
 * @see <a href="http://developer.android.com/guide/topics/ui/drag-drop">Drag and Drop</a>
 * @see <a href="http://developer.android.com/guide/topics/ui/multi-window">Multi-window</a>
 */
@RequiresApi(Build.VERSION_CODES.N)
public final class DropHelper {

    private static final String TAG = "DropHelper";

    private DropHelper() {}

    /**
     * Same as
     * {@link #configureView(Activity, View, String[], Options, OnReceiveContentListener)}, but
     * with default options.
     */
    public static void configureView(
            @NonNull Activity activity,
            @NonNull View dropTarget,
            @NonNull String[] mimeTypes,
            @NonNull @SuppressWarnings("ExecutorRegistration")
                    OnReceiveContentListener onReceiveContentListener) {
        configureView(
                activity,
                dropTarget,
                mimeTypes,
                new Options.Builder().build(),
                onReceiveContentListener);
    }

    /**
     * Configures a View to receive content and highlight during drag and drop operations.
     *
     * <p>If there are any EditTexts contained in the drop target's hierarchy, they must all be
     * provided via {@code options}.
     *
     * <p>Highlighting will only occur for a particular drag action if it matches the MIME type
     * provided here, wildcards allowed (e.g. 'image/*'). A drop can be executed and will be
     * passed on to the onReceiveContentListener even if the MIME type is not matched.
     *
     * <p>See {@link DropHelper} for full instructions.
     *
     * @param activity The current {@code Activity}, used for URI permissions.
     * @param dropTarget The View that should become a drop target.
     * @param mimeTypes  The MIME types that can be accepted.
     * @param options Options for configuration.
     * @param onReceiveContentListener    The listener to handle dropped data.
     */
    public static void configureView(
            @NonNull Activity activity,
            @NonNull View dropTarget,
            @NonNull String[] mimeTypes,
            @NonNull Options options,
            @NonNull @SuppressWarnings("ExecutorRegistration")
                    OnReceiveContentListener onReceiveContentListener) {
        DropAffordanceHighlighter.Builder highlighterBuilder = DropAffordanceHighlighter.forView(
                dropTarget,
                clipDescription -> {
                    if (clipDescription == null) {
                        return false;
                    }
                    for (String mimeType : mimeTypes) {
                        if (clipDescription.hasMimeType(mimeType)) {
                            return true;
                        }
                    }
                    return false;
                });
        if (options.hasHighlightColor()) {
            highlighterBuilder.setHighlightColor(options.getHighlightColor());
        }
        if (options.hasHighlightCornerRadiusPx()) {
            highlighterBuilder.setHighlightCornerRadiusPx(options.getHighlightCornerRadiusPx());
        }
        DropAffordanceHighlighter highlighter = highlighterBuilder.build();
        List<EditText> innerEditTexts = options.getInnerEditTexts();
        if (!innerEditTexts.isEmpty()) {
            // Any inner EditTexts need to know how to handle the drop.
            for (EditText innerEditText : innerEditTexts) {
                setHighlightingAndHandling(innerEditText, mimeTypes, highlighter,
                        onReceiveContentListener, activity);
            }
            // When handling drops to the outer view, delegate to the correct inner EditText.
            dropTarget.setOnDragListener(createDelegatingHighlightingOnDragListener(
                    activity, highlighter, innerEditTexts));
        } else {
            // With no inner EditTexts, the main View can handle everything.
            setHighlightingAndHandling(
                    dropTarget, mimeTypes, highlighter, onReceiveContentListener, activity);
        }
    }

    private static void setHighlightingAndHandling(
            View view,
            String[] mimeTypes,
            DropAffordanceHighlighter highlighter,
            OnReceiveContentListener onReceiveContentListener,
            Activity activity) {
        ViewCompat.setOnReceiveContentListener(view, mimeTypes, onReceiveContentListener);
        if (view instanceof AppCompatEditText) {
            // In AppCompatEditText, the OnReceiveContentListener will handle the drop. We just
            // need to add highlighting.
            view.setOnDragListener(highlighter::onDrag);
        } else {
            // Otherwise, trigger the OnReceiveContentListener from an OnDragListener.
            view.setOnDragListener(createHighlightingOnDragListener(highlighter, activity));
        }
    }

    /**
     * Creates an OnDragListener that performs highlighting and triggers the
     * OnReceiveContentListener.
     */
    private static OnDragListener createHighlightingOnDragListener(
            DropAffordanceHighlighter highlighter,
            Activity activity) {
        return (v, dragEvent) -> {
            if (dragEvent.getAction() == DragEvent.ACTION_DROP) {
                ContentInfoCompat data = new ContentInfoCompat.Builder(
                        dragEvent.getClipData(), ContentInfoCompat.SOURCE_DRAG_AND_DROP).build();
                try {
                    requestPermissionsIfNeeded(activity, dragEvent);
                } catch (CouldNotObtainPermissionsException e) {
                    return false;
                }
                ViewCompat.performReceiveContent(v, data);
            }
            return highlighter.onDrag(v, dragEvent);
        };
    }

    private static void requestPermissionsIfNeeded(Activity activity, DragEvent dragEvent)
            throws CouldNotObtainPermissionsException {
        ClipData clipData = dragEvent.getClipData();
        if (clipData != null && hasUris(clipData)) {
            DragAndDropPermissions permissions = activity.requestDragAndDropPermissions(dragEvent);
            if (permissions == null) {
                throw new CouldNotObtainPermissionsException("Couldn't get DragAndDropPermissions");
            }
        }
    }

    private static class CouldNotObtainPermissionsException extends Exception {
        CouldNotObtainPermissionsException(String msg) {
            super(msg);
        }
    }

    private static boolean hasUris(ClipData clipData) {
        for (int i = 0; i < clipData.getItemCount(); i++) {
            if (clipData.getItemAt(i).getUri() != null) {
                return true;
            }
        }
        return false;
    }

    /** Creates an OnDragListener that performs highlighting and delegates to an inner EditText. */
    private static OnDragListener createDelegatingHighlightingOnDragListener(
            Activity activity, DropAffordanceHighlighter highlighter, List<EditText> editTexts) {
        return (v, dragEvent) -> {
            if (dragEvent.getAction() == DragEvent.ACTION_DROP) {
                ContentInfoCompat data = new ContentInfoCompat.Builder(
                        dragEvent.getClipData(), ContentInfoCompat.SOURCE_DRAG_AND_DROP).build();
                try {
                    requestPermissionsIfNeeded(activity, dragEvent);
                } catch (CouldNotObtainPermissionsException e) {
                    return false;
                }
                for (EditText editText : editTexts) {
                    if (editText.hasFocus()) {
                        ViewCompat.performReceiveContent(editText, data);
                        return true;
                    }
                }
                // If none had focus, default to the first one provided.
                ViewCompat.performReceiveContent(editTexts.get(0), data);
                return true;
            }
            return highlighter.onDrag(v, dragEvent);
        };
    }

    /** Options for configuring {@link DropHelper}. */
    @RequiresApi(Build.VERSION_CODES.N)
    public static final class Options {
        private final @ColorInt int mHighlightColor;
        private final boolean mHighlightColorHasBeenSupplied;
        private final int mHighlightCornerRadiusPx;
        private final boolean mHighlightCornerRadiusPxHasBeenSupplied;
        private final @NonNull List<EditText> mInnerEditTexts;

        Options(
                @ColorInt int highlightColor,
                boolean highlightColorHasBeenSupplied,
                int highlightCornerRadiusPx,
                boolean highlightCornerRadiusPxHasBeenSupplied,
                @Nullable List<EditText> innerEditTexts) {
            this.mHighlightColor = highlightColor;
            this.mHighlightColorHasBeenSupplied = highlightColorHasBeenSupplied;
            this.mHighlightCornerRadiusPx = highlightCornerRadiusPx;
            this.mHighlightCornerRadiusPxHasBeenSupplied = highlightCornerRadiusPxHasBeenSupplied;
            this.mInnerEditTexts =
                    innerEditTexts != null ? new ArrayList<>(innerEditTexts) : new ArrayList<>();
        }

        /** The color to use for highlighting, if set.
         *
         * @see #hasHighlightColor()
         */
        public @ColorInt int getHighlightColor() {
            return mHighlightColor;
        }

        /** Whether or not a highlight color has been set. If not, a default will be used. */
        public boolean hasHighlightColor() {
            return mHighlightColorHasBeenSupplied;
        }

        /** The desired corner radius, if set.
         *
         * @see #hasHighlightCornerRadiusPx()
         */
        public int getHighlightCornerRadiusPx() {
            return mHighlightCornerRadiusPx;
        }

        /** Whether or not a corner radius has been set. If not, a default will be used. */
        public boolean hasHighlightCornerRadiusPx() {
            return mHighlightCornerRadiusPxHasBeenSupplied;
        }

        /** The EditText instances supplied when constructing this instance. */
        public @NonNull List<EditText> getInnerEditTexts() {
            return Collections.unmodifiableList(mInnerEditTexts);
        }

        /** Builder for constructing {@link Options}. */
        @RequiresApi(Build.VERSION_CODES.N)
        public static final class Builder {
            private @ColorInt int mHighlightColor;
            private boolean mHighlightColorHasBeenSupplied = false;
            private int mHighlightCornerRadiusPx;
            private boolean mHighlightCornerRadiusPxHasBeenSupplied = false;
            private @Nullable List<EditText> mInnerEditTexts;

            /** Builds the {@link Options} instance. */
            public @NonNull Options build() {
                return new Options(
                        mHighlightColor,
                        mHighlightColorHasBeenSupplied,
                        mHighlightCornerRadiusPx,
                        mHighlightCornerRadiusPxHasBeenSupplied,
                        mInnerEditTexts);
            }

            /**
             * All {@code EditText}(s) in the drop target View's descendant tree (i.e. any contained
             * within the drop target) <b>must</b> be provided via this option.
             *
             * <p>If the user is dragging text data in the DragEvent alongside URI data, one of the
             * EditTexts will be chosen to handle that additional text data. If the user has
             * positioned the cursor in one of the EditTexts, or drops directly on it, that will
             * be correctly delegated to. If not, the first one provided is the one that will be
             * used as a default. For example, if your drop target handles images, and contains
             * two editable text fields, you should ensure the one that you want to receive the
             * additional text by default is provided first.
             *
             * <p>Behavior is undefined if EditTexts are added or removed after configuation.
             *
             * <p>See {@link DropHelper} for full instructions and explanation.
             */
            public @NonNull Options.Builder addInnerEditTexts(
                    @NonNull EditText... editTexts) {
                if (this.mInnerEditTexts == null) {
                    this.mInnerEditTexts = new ArrayList<>();
                }
                Collections.addAll(this.mInnerEditTexts, editTexts);
                return this;
            }

            /**
             * Sets the color of the highlight shown while a drag-and-drop operation is in progress.
             *
             * <p>Note that opacity, if provided, is ignored.
             */
            public @NonNull Options.Builder setHighlightColor(@ColorInt int highlightColor) {
                this.mHighlightColor = highlightColor;
                this.mHighlightColorHasBeenSupplied = true;
                return this;
            }

            /**
             * Sets the corner radius (px) of the highlight shown while a drag-and-drop operation is
             * in progress.
             */
            public @NonNull Options.Builder setHighlightCornerRadiusPx(
                    int highlightCornerRadiusPx) {
                this.mHighlightCornerRadiusPx = highlightCornerRadiusPx;
                this.mHighlightCornerRadiusPxHasBeenSupplied = true;
                return this;
            }
        }
    }
}
