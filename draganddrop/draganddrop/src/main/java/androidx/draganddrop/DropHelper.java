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
 * Helper class used to configure {@link View}s to receive data dropped by a drag and drop
 * operation. Includes support for content insertion using an
 * {@link OnReceiveContentListener OnReceiveContentListener}. Adds highlighting during the drag
 * interaction to indicate to the user where the drop action can successfully take place.
 *
 * <p>To ensure that drop target highlighting and text data handling work correctly, all
 * {@link EditText} elements in the drop target view's descendant tree (that is, any
 * {@code EditText} elements contained within the drop target) must be provided as arguments to a
 * call to {@link DropHelper.Options.Builder#addInnerEditTexts(EditText...)}. Otherwise, an
 * {@code EditText} within the target will steal the focus during the drag and drop operation,
 * possibly causing undesired highlighting behavior.
 *
 * <p>Also, if the user is dragging text data and URI data in the drag and drop {@link ClipData},
 * one of the {@code EditText} elements in the drop target is automatically chosen to handle the
 * text data. See {@link DropHelper.Options.Builder#addInnerEditTexts(EditText...)} for the order of
 * precedence in selecting the {@code EditText} that handles the text data.
 *
 * <p>This helper attaches an {@link OnReceiveContentListener OnReceiveContentListener} to drop
 * targets and configures drop targets to listen for drag and drop events (see
 * {@link #configureView(Activity, View, String[], OnReceiveContentListener) configureView}). Do not
 * attach an {@link OnDragListener OnDragListener} or additional {@code OnReceiveContentLister} to
 * drop targets when using {@link DropHelper}.
 *
 * <p><b>Note:</b> This class requires Android API level 24 or higher.
 *
 * @see <a href="https://developer.android.com/guide/topics/ui/drag-drop">Drag and drop</a>
 * @see <a href="https://developer.android.com/guide/topics/large-screens/multi-window-support#dnd">
 *     Multi-window support</a>
 */
@RequiresApi(Build.VERSION_CODES.N)
public final class DropHelper {

    private static final String TAG = "DropHelper";

    private DropHelper() {}

    /**
     * Configures a {@code View} for drag and drop operations, including the highlighting that
     * indicates the view is a drop target. Sets a listener that enables the view to handle dropped
     * data.
     * <p>
     * Same as <code>{@link #configureView(Activity, View, String[], Options,
     * OnReceiveContentListener)}</code> but with default configuration options.
     * <p>
     * <b>Note:</b> If the drop target contains {@link EditText} elements, you must use
     * {@link #configureView(Activity, View, String[], Options, OnReceiveContentListener)}. The
     * {@code Options} argument enables you to specify a list of the {@code EditText} elements
     * (see {@link Options.Builder#addInnerEditTexts(EditText...)}).
     *
     * @param activity The current {@code Activity} (used for URI permissions).
     * @param dropTarget A {@code View} that accepts the drag and drop data.
     * @param mimeTypes The MIME types the drop target can accept from the dropped data.
     * @param onReceiveContentListener A listener that handles the dropped data.
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
     * Configures a {@code View} for drag and drop operations, including the highlighting that
     * indicates the view is a drop target. Sets a listener that enables the view to handle dropped
     * data.
     * <p>
     * If the drop target's view hierarchy contains any {@code EditText} elements, they all must be
     * specified in {@code options} (see {@link Options.Builder#addInnerEditTexts(EditText...)}).
     * <p>
     * View highlighting occurs for a drag action only if a MIME type in the
     * {@link android.content.ClipDescription ClipDescription} matches a MIME type provided in
     * {@code mimeTypes}; wildcards are allowed (for example, "image/*"). A drop can be executed
     * and passed on to the {@code OnReceiveContentListener} even if the MIME type is not matched.
     * <p>
     * See {@link DropHelper} for more information.
     *
     * @param activity The current {@code Activity} (used for URI permissions).
     * @param dropTarget A {@code View} that accepts the drag and drop data.
     * @param mimeTypes The MIME types the drop target can accept from the dropped data.
     * @param options Configuration options for the drop target (see {@link DropHelper.Options}).
     * @param onReceiveContentListener A listener that handles the dropped data.
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
        highlighterBuilder.shouldAcceptDragsWithLocalState(
                options.shouldAcceptDragsWithLocalState());
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S || view instanceof AppCompatEditText) {
            // In AppCompatEditText, or in S+, the OnReceiveContentListener will handle the drop.
            // We just need to add highlighting.
            view.setOnDragListener(highlighter::onDrag);
        } else {
            // Otherwise, trigger the OnReceiveContentListener from an OnDragListener.
            view.setOnDragListener(createHighlightingOnDragListener(highlighter, activity));
        }
    }

    /*
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

    /*
     * Creates an OnDragListener that performs highlighting and delegates to an inner EditText.
     */
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

    /**
     * Options for configuring drop targets specified by {@link DropHelper}.
     */
    @RequiresApi(Build.VERSION_CODES.N)
    public static final class Options {
        private final @ColorInt int mHighlightColor;
        private final boolean mHighlightColorHasBeenSupplied;
        private final int mHighlightCornerRadiusPx;
        private final boolean mHighlightCornerRadiusPxHasBeenSupplied;
        private final boolean mAcceptDragsWithLocalState;
        private final @NonNull List<EditText> mInnerEditTexts;

        Options(
                @ColorInt int highlightColor,
                boolean highlightColorHasBeenSupplied,
                int highlightCornerRadiusPx,
                boolean highlightCornerRadiusPxHasBeenSupplied,
                boolean acceptDragsWithLocalState,
                @Nullable List<EditText> innerEditTexts) {
            this.mHighlightColor = highlightColor;
            this.mHighlightColorHasBeenSupplied = highlightColorHasBeenSupplied;
            this.mHighlightCornerRadiusPx = highlightCornerRadiusPx;
            this.mHighlightCornerRadiusPxHasBeenSupplied = highlightCornerRadiusPxHasBeenSupplied;
            this.mAcceptDragsWithLocalState = acceptDragsWithLocalState;
            this.mInnerEditTexts =
                    innerEditTexts != null ? new ArrayList<>(innerEditTexts) : new ArrayList<>();
        }

        /**
         * Returns the color used to highlight the drop target.
         *
         * @return The drop target highlight color.
         * @see #hasHighlightColor()
         */
        public @ColorInt int getHighlightColor() {
            return mHighlightColor;
        }

        /**
         * Indicates whether or not a drop target highlight color has been set. If not, a default
         * is used.
         *
         * @return True if a highlight color has been set, false otherwise.
         */
        public boolean hasHighlightColor() {
            return mHighlightColorHasBeenSupplied;
        }

        /**
         * Returns the corner radius of the drop target highlighting.
         *
         * @return The drop target highlighting corner radius.
         * @see #hasHighlightCornerRadiusPx()
         */
        public int getHighlightCornerRadiusPx() {
            return mHighlightCornerRadiusPx;
        }

        /**
         * Indicates whether or not a corner radius has been set for the drop target highlighting.
         * If not, a default is used.
         *
         * @return True if a corner radius has been set, false otherwise.
         */
        public boolean hasHighlightCornerRadiusPx() {
            return mHighlightCornerRadiusPxHasBeenSupplied;
        }

        /**
         * Indicates whether or not we should respond to drag events when the drag operation
         * contains a {@link DragEvent#getLocalState() localState}. Setting localState is only
         * possible when the drag operation originated from this Activity.
         *
         * @return True if drag events will be accepted when the localState is non-null.
         */
        public boolean shouldAcceptDragsWithLocalState() {
            return mAcceptDragsWithLocalState;
        }

        /**
         * Returns a list of the {@link EditText} elements contained in the drop target view
         * hierarchy. A list of {@code EditText} elements is supplied when building this
         * {@link DropHelper.Options} instance (see
         * {@link Builder#addInnerEditTexts(EditText...)}).
         *
         * @return The list of drop target {@code EditText} elements.
         */
        public @NonNull List<EditText> getInnerEditTexts() {
            return Collections.unmodifiableList(mInnerEditTexts);
        }

        /**
         * Builder for constructing a {@link DropHelper.Options} instance.
         */
        @RequiresApi(Build.VERSION_CODES.N)
        public static final class Builder {
            private @ColorInt int mHighlightColor;
            private boolean mHighlightColorHasBeenSupplied = false;
            private int mHighlightCornerRadiusPx;
            private boolean mHighlightCornerRadiusPxHasBeenSupplied = false;
            private boolean mAcceptDragsWithLocalState = false;
            private @Nullable List<EditText> mInnerEditTexts;

            /**
             * Builds a new {@link DropHelper.Options} instance.
             *
             * @return A new {@link DropHelper.Options} instance.
             */
            public @NonNull Options build() {
                return new Options(
                        mHighlightColor,
                        mHighlightColorHasBeenSupplied,
                        mHighlightCornerRadiusPx,
                        mHighlightCornerRadiusPxHasBeenSupplied,
                        mAcceptDragsWithLocalState,
                        mInnerEditTexts);
            }

            /**
             * Enables you to specify the {@link EditText} elements contained within the drop
             * target. To ensure proper drop target highlighting, all {@code EditText} elements in
             * the drop target view hierarchy must be included in a call to this method. Otherwise,
             * an {@code EditText} within the target, rather than the target view itself, acquires
             * focus during the drag and drop operation.
             * <p>
             * If the user is dragging text data and URI data in the drag and drop
             * {@link ClipData}, one of the {@code EditText} elements in the drop target is
             * selected to handle the text data. Selection is based on the following order of
             * precedence:
             * <ol>
             *     <li>The {@code EditText} (if any) on which the {@code ClipData} was dropped
             *     <li>The {@code EditText} (if any) that contains the text cursor (caret)
             *     <li>The first {@code EditText} provided in {@code editTexts}
             * </ol>
             * <p>
             * To set the default {@code EditText}, make it the first argument of the
             * {@code editTexts} parameter. For example, if your drop target handles images and
             * contains two editable text fields, T1 and T2, make T2 the default by calling
             * <code>addInnerEditTexts(T2, T1)</code>.
             * <p>
             * <b>Note:</b> Behavior is undefined if {@code EditText}s are added to or removed
             * from the drop target after this method has been called.
             * <p>
             * See {@link DropHelper} for more information.
             *
             * @param editTexts The {@code EditText} elements contained in the drop target.
             * @return This {@link DropHelper.Options.Builder} instance.
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
             * Sets the color of the drop target highlight. The highlight is shown during a drag
             * and drop operation when data is dragged over the drop target and a MIME type in the
             * {@link android.content.ClipDescription ClipDescription} matches a MIME type provided
             * to
             * {@link DropHelper#configureView(Activity, View, String[], OnReceiveContentListener)
             * DropHelper#configureView}.
             * <p>
             * <b>Note:</b> Opacity, if provided, is ignored.
             *
             * @param highlightColor The highlight color.
             * @return This {@link DropHelper.Options.Builder} instance.
             */
            public @NonNull Options.Builder setHighlightColor(@ColorInt int highlightColor) {
                this.mHighlightColor = highlightColor;
                this.mHighlightColorHasBeenSupplied = true;
                return this;
            }

            /**
             * Sets the corner radius of the drop target highlight. The highlight is shown during
             * a drag and drop operation when data is dragged over the drop target and a MIME type
             * in the {@link android.content.ClipDescription ClipDescription} matches a MIME type
             * provided to
             * {@link DropHelper#configureView(Activity, View, String[], OnReceiveContentListener)
             * DropHelper#configureView}.
             *
             * @param highlightCornerRadiusPx The highlight corner radius in pixels.
             * @return This {@link DropHelper.Options.Builder} instance.
             */
            public @NonNull Options.Builder setHighlightCornerRadiusPx(
                    int highlightCornerRadiusPx) {
                this.mHighlightCornerRadiusPx = highlightCornerRadiusPx;
                this.mHighlightCornerRadiusPxHasBeenSupplied = true;
                return this;
            }

            /**
             * Sets whether or not we should respond to drag events when the drag operation contains
             * a {@link DragEvent#getLocalState() localState}. Setting localState is only possible
             * when the drag operation originated from this Activity.
             *
             * <p>
             * By default, this is false.
             *
             * <p>
             * Note that to elicit the default behavior of ignoring drags from the same Activity as
             * the drop target, the localState supplied when starting the drag (via
             * {@link View#startDragAndDrop(ClipData, View.DragShadowBuilder, Object, int)} or using
             * <a href="https://developer.android.com/reference/androidx/core/view/DragStartHelper">DragStartHelper</a>
             * must be set to non-null.
             *
             * @param acceptDragsWithLocalState Whether or not to accept drag events with non-null
             *                                  localState.
             * @return This {@link DropHelper.Options.Builder} instance.
             */
            public @NonNull Options.Builder setAcceptDragsWithLocalState(
                    boolean acceptDragsWithLocalState) {
                this.mAcceptDragsWithLocalState = acceptDragsWithLocalState;
                return this;
            }
        }
    }
}
