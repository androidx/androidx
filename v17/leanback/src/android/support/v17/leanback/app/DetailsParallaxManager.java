/*
 * Copyright (C) 2015 The Android Open Source Project
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
package android.support.v17.leanback.app;

import android.support.v17.leanback.R;
import android.support.v17.leanback.widget.Parallax;
import android.support.v17.leanback.widget.ParallaxRecyclerViewSource;
import android.support.v7.widget.RecyclerView;

/**
 * Class in charge of managing the {@link Parallax} object for {@link DetailsFragment}. This
 * can be shared for creating both parallax effect and video animations when transitioning to/from
 * half/full screen.
 * @hide
 */
public class DetailsParallaxManager {
    final ParallaxRecyclerViewSource mParallaxSource;
    final ParallaxRecyclerViewSource.ChildPositionProperty mFrameTop;
    final ParallaxRecyclerViewSource.ChildPositionProperty mFrameBottom;
    final Parallax mParallax;

    public DetailsParallaxManager() {
        mParallaxSource = new ParallaxRecyclerViewSource();

        // track the top edge of details_frame of first item of adapter
        mFrameTop = mParallaxSource
                .addProperty("frameTop")
                .adapterPosition(0)
                .viewId(R.id.details_frame);

        // track the bottom edge of details_frame of first item of adapter
        mFrameBottom = mParallaxSource
                .addProperty("frameBottom")
                .adapterPosition(0)
                .viewId(R.id.details_frame)
                .fraction(1.0f);

        mParallax = new Parallax();
        mParallax.setSource(mParallaxSource);
    }

    /**
     * Returns the {@link Parallax} instance.
     */
    public Parallax getParallax() {
        return mParallax;
    }

    public RecyclerView getRecyclerView() {
        return mParallaxSource.getRecyclerView();
    }

    /**
     * Set the RecyclerView to register onScrollListener.
     * @param recyclerView
     */
    public void setRecyclerView(RecyclerView recyclerView) {
        mParallaxSource.setRecyclerView(recyclerView);
    }

    /**
     * Returns the top of the details overview row. This is tracked for implementing the
     * parallax effect.
     */
    public ParallaxRecyclerViewSource.ChildPositionProperty getFrameTop() {
        return mFrameTop;
    }

    /**
     * Returns the bottom of the details overview row. This is tracked for implementing the
     * parallax effect.
     */
    public ParallaxRecyclerViewSource.ChildPositionProperty getFrameBottom() {
        return mFrameBottom;
    }
}
