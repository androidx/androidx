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

package androidx.pdf.viewer;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

/**
 * Base class for views that will base their display on the {@link PaginationModel}.
 *
 * <p>Provides consistent {@link #onMeasure(int, int)} and {@link #onLayout(boolean, int, int, int,
 * int)} behavior and requests a layout each time a new page is added to the model.
 *
 * <p>Subclasses must implement {@link #layoutChild(int)} in order to position their actual views.
 * Subclasses can override {@link #onViewAreaChanged()} if they need to perform updates when this
 * happens.
 *
 * <p>Padding will not be acknowledged. If views must implement padding they need to measure
 * themselves but should be aware they will diverge from the coordinates of other views using the
 * {@link PaginationModel}.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public abstract class AbstractPaginatedView extends ViewGroup implements PaginationModelObserver {

    @Nullable
    private PaginationModel mModel;

    public AbstractPaginatedView(@NonNull Context context) {
        super(context);
    }

    public AbstractPaginatedView(@NonNull Context context, @NonNull AttributeSet attrs) {
        super(context, attrs);
    }

    public AbstractPaginatedView(@NonNull Context context, @NonNull AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setModel(@Nullable PaginationModel model) {
        this.mModel = model;
    }

    // This class does not produce a model but rather renders a model generated elsewhere to a view.
    // Any classes wishing to obtain the model should do so from the owner/manager.
    @Nullable
    public PaginationModel getModel() {
        return mModel;
    }

    protected boolean isInitialized() {
        return mModel != null;
    }

    /**
     * Measures this view in relation to the {@link #mModel} then asks all child views to measure
     * themselves.
     *
     * <p>If the {@link #mModel} is not initialized, this view has nothing to display and will
     * measure (0, 0). Otherwise, view will measure ({@link #mModel}'s width, {@link #mModel}'s
     * estimated height).
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = 0;
        int estimatedHeight = 0;

        if (isInitialized()) {
            width = mModel.getWidth();
            estimatedHeight = mModel.getEstimatedFullHeight();
        }

        setMeasuredDimension(width, estimatedHeight);
        measureChildren(widthMeasureSpec, heightMeasureSpec);
    }

    /**
     * Provides consistent layout behavior for subclasses.
     *
     * <p>Does not perform a layout if there aren't any child views. Otherwise asks the
     * subclasses to
     * layout each child by index.
     */
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int count = getChildCount();
        if (count == 0) {
            return;
        }

        for (int i = 0; i < count; i++) {
            layoutChild(i);
        }
    }

    /**
     * Lays out the child at {@code index}.
     *
     * <p>Subclasses should use the {@link #mModel} to determine top and bottom values.
     */
    protected abstract void layoutChild(int index);

    /** Requests a layout because this view has to grow now to accommodate the new page(s). */
    @Override
    public void onPageAdded() {
        requestLayout();
    }

    /**
     * Implementation of PaginationModelObserver, is no-op at this level.
     *
     * <p>Will be called each time the viewArea of the model is changed. Should be overridden by any
     * subclasses that wish to perform actions when this occurs.
     */
    @Override
    public void onViewAreaChanged() {
    }
}
