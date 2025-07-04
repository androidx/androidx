/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.select;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Rect;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.RestrictTo;
import androidx.pdf.R;
import androidx.pdf.models.PageSelection;
import androidx.pdf.models.SelectionBoundary;
import androidx.pdf.util.Preconditions;
import androidx.pdf.viewer.PaginatedView;
import androidx.pdf.widget.ZoomView;

import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Objects;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class SelectionActionMode {
    private static final String TAG = "SelectionActionMode";
    private final Context mContext;
    private final ZoomView mZoomView;
    private final PaginatedView mPaginatedView;
    private final SelectionModel<PageSelection> mSelectionModel;
    private final Object mSelectionObserverKey;
    private final ActionMode.Callback2 mCallback;
    private ActionMode mActionMode;
    private PageSelection mCurrentSelection;

    private final String mKeyCopiedText = "PdfCopiedText";

    public SelectionActionMode(@NonNull Context context, @NonNull PaginatedView paginatedView,
            @NonNull ZoomView zoomView, @NonNull SelectionModel<PageSelection> selectionModel) {
        Preconditions.checkNotNull(context, "Context should not be null");
        Preconditions.checkNotNull(paginatedView, "paginatedView should not be null");
        Preconditions.checkNotNull(paginatedView, "selectionModel should not be null");
        Preconditions.checkNotNull(paginatedView, "callback should not be null");
        this.mContext = context;
        this.mPaginatedView = paginatedView;
        this.mZoomView = zoomView;
        this.mSelectionModel = selectionModel;
        this.mCallback = new SelectionCallback();

        mSelectionObserverKey = selectionModel.selection().addObserver(
                (oldValue, newValue) -> {
                    mCurrentSelection = newValue;
                    if (newValue == null) {
                        stopActionMode();
                    } else if (oldValue == null) {
                        startActionMode();
                    } else {
                        if (!oldValue.getStart().equals(newValue.getStart())
                                && !oldValue.getStop().equals(newValue.getStop())) {
                            resume();
                        }
                    }
                });
    }

    /**
     *
     */
    public void destroy() {
        mSelectionModel.selection().removeObserver(mSelectionObserverKey);
    }

    /** Start this action mode - updates the menu, ensures it is visible. */
    private void startActionMode() {
        mZoomView.post(() -> {
            mZoomView.startActionMode(mCallback, ActionMode.TYPE_FLOATING);
        });
    }

    /** Resumes the context menu **/
    public void resume() {
        if (mCurrentSelection != null) {
            startActionMode();
        }
    }

    /** Stop this action mode. */
    public void stopActionMode() {
        if (mActionMode != null) {
            mActionMode.finish();
            mActionMode = null;
        }
    }

    private void copyToClipboard(String text) {
        try {
            ClipboardManager clipboard = mContext.getSystemService(ClipboardManager.class);
            ClipData clip = ClipData.newPlainText(mKeyCopiedText, text);
            clipboard.setPrimaryClip(clip);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    private class SelectionCallback extends ActionMode.Callback2 {
        /** Called when the action mode is created. */
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mActionMode = mode;

            // Inflate the menu resource providing context menu items.
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.context_menu, menu);
            return true;

        }

        /**
         * Called each time the action mode is shown. Always called after onCreateActionMode(),
         * and might be called multiple times if the mode is invalidated.
         */
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false; // Return false as nothing needs to be done.
        }

        /** Called when the user selects a contextual menu item. */
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getItemId() == R.id.action_selectAll) {
                mSelectionModel.updateSelectionAsync(SelectionBoundary.PAGE_START,
                        SelectionBoundary.PAGE_END);
            } else if (item.getItemId() == R.id.action_copy) {
                copyToClipboard(mSelectionModel.getText());
                mSelectionModel.setSelection(null);
            }
            return true;

        }

        /** Called when the user exits the action mode. */
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mode = null;
        }

        /**
         * Called when an ActionMode needs to be positioned on screen, potentially occluding
         * view content.
         */
        @Override
        public void onGetContentRect(ActionMode mode, View view, Rect outRect) {
            Rect bounds = chooseContentLocationForMenu();
            outRect.set(Math.round(mZoomView.toZoomViewX(bounds.left)),
                    Math.round(mZoomView.toZoomViewY(bounds.top)),
                    Math.round(mZoomView.toZoomViewX(bounds.right)),
                    Math.round(mZoomView.toZoomViewY(bounds.bottom)));
        }

        /**
         * Determines the ideal location of the selection menu based on the visible area of the PDF
         * and the bounds of the current selection.
         *
         * In order of preference, this will return:
         * a) The first selection boundary, if it's visible
         * b) The last selection boundary, if the first is not visible
         * c) The middle of the visible area, if neither the first nor the last selection boundary
         * are visible
         *
         * @return the ideal location for the selection menu in content coordinates, as a
         * {@link Rect}. These coordinates should be adjusted for the current zoom / scroll level
         * before using them in any {@link View}
         */
        private Rect chooseContentLocationForMenu() {
            PageSelection selection = Objects.requireNonNull(mSelectionModel.mSelection.get());
            int selectionPage = selection.getPage();
            Rect pageLocation = mPaginatedView.getModel().getPageLocation(selectionPage,
                    mPaginatedView.getViewArea());

            List<Rect> selectionBounds = selection.getRects();
            // The selection bounds are defined in page coordinates, we need their bounds in the
            // overall PDF.
            Rect firstSelectionBounds = toContentRect(selectionBounds.get(0), pageLocation);
            Rect lastSelectionBounds = toContentRect(
                    selectionBounds.get(selectionBounds.size() - 1), pageLocation);

            Rect visibleArea = mZoomView.getVisibleAreaInContentCoords();
            if (firstSelectionBounds.intersect(visibleArea)) {
                return firstSelectionBounds;
            } else if (lastSelectionBounds.intersect(visibleArea)) {
                return lastSelectionBounds;
            } else {
                // If neither the beginning nor the end of the current selection are visible,
                // center the menu in View
                int centerX = visibleArea.centerX();
                int centerY = visibleArea.centerY();
                return new Rect(centerX - 1, centerY - 1, centerX + 1, centerY + 1);
            }
        }

        /**
         * Converts a {@link Rect} in PDF page coordinate space to a Rect in PDF coordinate space
         *
         * @param pageRect     A {@link Rect} describing a location within a PDF page
         * @param pageLocation A {@link Rect} describing the location of a page within a PDF
         * @return A {@link Rect} describing {@code pageRect}'s location in the overall PDF
         */
        private Rect toContentRect(Rect pageRect, Rect pageLocation) {
            Rect out = new Rect(pageRect);
            out.offset(pageLocation.left, pageLocation.top);
            return out;
        }
    }
}

