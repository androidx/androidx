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

package androidx.wear.watchface.complications.rendering.utils;

import android.graphics.Rect;
import android.support.wearable.complications.ComplicationData;
import android.text.Layout;
import android.text.Layout.Alignment;
import android.view.Gravity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

/**
 * Common superclass for layout helpers. Layout helpers are used to calculate bounds for each
 * element depending on width, height of the complication and complication data.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class LayoutHelper {

    private final Rect mBounds = new Rect();
    private ComplicationData mComplicationData;

    /** Returns the complication bounds assuming it's positioned at (0,0). */
    public void getBounds(@NonNull Rect outRect) {
        outRect.set(mBounds);
    }

    /**
     * @param width The width to apply to the layout's bounds
     */
    public void setWidth(int width) {
        mBounds.right = width;
    }

    /**
     * @param height The height to apply to the layout's bounds
     */
    public void setHeight(int height) {
        mBounds.bottom = height;
    }

    /**
     * @param data The {@link ComplicationData} to associate with this {@link LayoutHelper}
     */
    public void setComplicationData(@Nullable ComplicationData data) {
        mComplicationData = data;
    }

    /** Updates layout helper with given parameters. */
    public void update(int width, int height, @Nullable ComplicationData data) {
        setWidth(width);
        setHeight(height);
        setComplicationData(data);
    }

    /**
     * @return The {@link ComplicationData} associated with this {@link LayoutHelper}
     */
    @Nullable
    public ComplicationData getComplicationData() {
        return mComplicationData;
    }

    /**
     * @param outRect {@link Rect} that receives the computed bounds for the icon
     */
    public void getIconBounds(@NonNull Rect outRect) {
        outRect.setEmpty();
    }

    /**
     * @param outRect {@link Rect} that receives the computed bounds for the small image
     */
    public void getSmallImageBounds(@NonNull Rect outRect) {
        outRect.setEmpty();
    }

    /**
     * @param outRect {@link Rect} that receives the computed bounds for the large image
     */
    public void getLargeImageBounds(@NonNull Rect outRect) {
        outRect.setEmpty();
    }

    /**
     * @param outRect {@link Rect} that receives the computed bounds for the ranged value
     */
    public void getRangedValueBounds(@NonNull Rect outRect) {
        outRect.setEmpty();
    }

    /**
     * @param outRect {@link Rect} that receives the computed bounds for the short title text
     */
    public void getShortTextBounds(@NonNull Rect outRect) {
        outRect.setEmpty();
    }

    /**
     * @return The {@link Layout.Alignment} to use with short text
     */
    @NonNull
    public Layout.Alignment getShortTextAlignment() {
        return Alignment.ALIGN_CENTER;
    }

    /**
     * @return Gravity to use with short text
     */
    public int getShortTextGravity() {
        return Gravity.CENTER;
    }

    /**
     * @param outRect {@link Rect} that receives the computed bounds for the short title text
     */
    public void getShortTitleBounds(@NonNull Rect outRect) {
        outRect.setEmpty();
    }

    /**
     * @return The {@link Layout.Alignment} to use with short title text
     */
    @NonNull
    public Layout.Alignment getShortTitleAlignment() {
        return Alignment.ALIGN_CENTER;
    }

    /**
     * @return Gravity to use with short title text
     */
    public int getShortTitleGravity() {
        return Gravity.CENTER;
    }

    /**
     * @param outRect {@link Rect}  that receives the computed bounds for the long text
     */
    public void getLongTextBounds(@NonNull Rect outRect) {
        outRect.setEmpty();
    }

    /**
     * @return The {@link Layout.Alignment} to use with long text
     */
    @NonNull
    public Layout.Alignment getLongTextAlignment() {
        return Alignment.ALIGN_CENTER;
    }

    /**
     * @return Gravity to use with long title text
     */
    public int getLongTextGravity() {
        return Gravity.CENTER;
    }

    /**
     * @param outRect {@link Rect}  that receives the computed bounds for the long title text
     */
    public void getLongTitleBounds(@NonNull Rect outRect) {
        outRect.setEmpty();
    }

    /**
     * @return The {@link Layout.Alignment} to use with long title text
     */
    @NonNull
    public Layout.Alignment getLongTitleAlignment() {
        return Alignment.ALIGN_CENTER;
    }

    /**
     * @return The gravity to use with Long Title Text
     */
    public int getLongTitleGravity() {
        return Gravity.CENTER;
    }
}
