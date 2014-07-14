/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package android.support.v17.leanback.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An overview row for a details fragment. This row consists of an image, a
 * description view, and optionally a series of {@link Action}s that can be taken for
 * the item.
 */
public class DetailsOverviewRow extends Row {

    private Object mItem;
    private Drawable mImageDrawable;
    private ArrayList<Action> mActions = new ArrayList<Action>();
    private boolean mImageScaleUpAllowed = true;

    /**
     * Constructor for a DetailsOverviewRow.
     *
     * @param item The main item for the details page.
     */
    public DetailsOverviewRow(Object item) {
        super(null);
        mItem = item;
        verify();
    }

    /**
     * Gets the main item for the details page.
     */
    public final Object getItem() {
        return mItem;
    }

    /**
     * Sets a drawable as the image of this details overview.
     *
     * @param drawable The drawable to set.
     */
    public final void setImageDrawable(Drawable drawable) {
        mImageDrawable = drawable;
    }

    /**
     * Sets a Bitmap as the image of this details overview.
     *
     * @param context The context to retrieve display metrics from.
     * @param bm The bitmap to set.
     */
    public final void setImageBitmap(Context context, Bitmap bm) {
        mImageDrawable = new BitmapDrawable(context.getResources(), bm);
    }

    /**
     * Gets the image drawable of this details overview.
     *
     * @return The overview's image drawable, or null if no drawable has been
     *         assigned.
     */
    public final Drawable getImageDrawable() {
        return mImageDrawable;
    }

    /**
     * Allows or disallows scaling up of images.
     * Images will always be scaled down if necessary.
     */
    public void setImageScaleUpAllowed(boolean allowed) {
        mImageScaleUpAllowed = allowed;
    }

    /**
     * Returns true if the image may be scaled up; false otherwise.
     */
    public boolean isImageScaleUpAllowed() {
        return mImageScaleUpAllowed;
    }

    /**
     * Add an Action to the overview.
     *
     * @param action The Action to add.
     */
    public final void addAction(Action action) {
        mActions.add(action);
    }

    /**
     * Add an Action to the overview at the specified position.
     *
     * @param pos The position to insert the Action.
     * @param action The Action to add.
     */
    public final void addAction(int pos, Action action) {
        mActions.add(pos, action);
    }

    /**
     * Remove the given Action from the overview.
     *
     * @param action The Action to remove.
     * @return true if the overview contained the specified Action.
     */
    public final boolean removeAction(Action action) {
        return mActions.remove(action);
    }

    /**
     * Gets a read-only view of the list of Actions of this details overview.
     *
     * @return An unmodifiable view of the list of Actions.
     */
    public final List<Action> getActions() {
        return Collections.unmodifiableList(mActions);
    }

    private void verify() {
        if (mItem == null) {
            throw new IllegalArgumentException("Object cannot be null");
        }
    }
}
