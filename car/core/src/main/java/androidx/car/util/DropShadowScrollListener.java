/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.car.util;

import android.animation.ValueAnimator;
import android.util.Log;
import android.view.View;

import androidx.annotation.RestrictTo;
import androidx.car.R;
import androidx.car.widget.PagedListView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * A helper class that that listens for scrolls on a {@link PagedListView} and adds a drop shadow
 * to a view that is passed to it if the {@code PagedListView} has been scrolled to a position that
 * is not the top.
 *
 * <p>Note: this class expects that the {@code PagedListView} it is attached to is using a
 * {@link LinearLayoutManager}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class DropShadowScrollListener extends RecyclerView.OnScrollListener {
    private static final String TAG = "DropShadowScrollListener";
    private static final int ANIMATION_DURATION_MS = 100;

    private final View mElevationView;
    private final float mElevation;

    public DropShadowScrollListener(View elevationView) {
        mElevationView = elevationView;
        mElevation = elevationView.getResources().getDimension(
               R.dimen.car_list_dialog_title_elevation);
    }

    @Override
    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);

        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        if (layoutManager == null) {
            return;
        }

        // This class is only used internally for lists that are expected to be using a
        // LinearLayoutManager.
        if (!(layoutManager instanceof LinearLayoutManager)) {
            Log.e(TAG, "Using a LayoutManager that is not a LinearLayoutManager. Class is: "
                    + layoutManager);
            return;
        }

        // If we're at the top, then remove the elevation. Otherwise, add it.
        if (((LinearLayoutManager) layoutManager).findFirstCompletelyVisibleItemPosition() == 0) {
            removeElevationWithAnimation();
        } else if (mElevationView.getElevation() != mElevation) {
            // Note that elevation can be added without any animation because the list
            // scroll will hide the fact that it pops in.
            mElevationView.setElevation(mElevation);
        }
    }

    /** Animates the removal of elevation from {@link #mElevationView}. */
    private void removeElevationWithAnimation() {
        ValueAnimator elevationAnimator =
                ValueAnimator.ofFloat(mElevationView.getElevation(), 0f);
        elevationAnimator
                .setDuration(ANIMATION_DURATION_MS)
                .addUpdateListener(animation -> mElevationView.setElevation(
                        (float) animation.getAnimatedValue()));
        elevationAnimator.start();
    }

}
