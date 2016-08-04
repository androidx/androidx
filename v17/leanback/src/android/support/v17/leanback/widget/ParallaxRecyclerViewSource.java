/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import static android.support.v7.widget.RecyclerView.LayoutManager;
import static android.support.v7.widget.RecyclerView.OnScrollListener;
import static android.support.v7.widget.RecyclerView.ViewHolder;

/**
 * Implementation of {@link ParallaxSource} class for {@link RecyclerView}. This class
 * allows users to track position of specific views inside {@link RecyclerView} relative to
 * itself. @see {@link ChildPositionProperty} for details.
 */
public class ParallaxRecyclerViewSource extends
        ParallaxSource.IntSource<ParallaxRecyclerViewSource.ChildPositionProperty> {
    private final RecyclerView mRecylerView;
    private Listener mListener;
    private final boolean mIsVertical;

    /**
     * Subclass of {@link ParallaxSource.IntProperty}. Using this Property, users can track a
     * RecylerView child's position inside recyclerview. i.e.
     *
     * tracking_pos = view.top + fraction * view.height() + offset
     *
     * This way we can track top using fraction 0 and bottom using fraction 1.
     */
    public static final class ChildPositionProperty extends ParallaxSource.IntProperty {
        int mAdapterPosition;
        int mViewId;
        int mOffset;
        float mFraction;

        ChildPositionProperty(String name, int index) {
            super(name, index);
        }

        /**
         * Sets adapter position of the recyclerview child to track.
         *
         * @param adapterPosition Zero based position in adapter.
         * @return This ChildPositionProperty object.
         */
        public ChildPositionProperty adapterPosition(int adapterPosition) {
            mAdapterPosition = adapterPosition;
            return this;
        };

        /**
         * Sets view Id of a descendant of recyclerview child to track.
         *
         * @param viewId Id of a descendant of recyclerview child.
         * @return This ChildPositionProperty object.
         */
        public ChildPositionProperty viewId(int viewId) {
            mViewId = viewId;
            return this;
        }

        /**
         * Sets offset in pixels added to the view's start position.
         *
         * @param offset Offset in pixels added to the view's start position.
         * @return This ChildPositionProperty object.
         */
        public ChildPositionProperty offset(int offset) {
            mOffset = offset;
            return this;
        }

        /**
         * Sets fraction of size to be added to view's start position.  e.g. to track the
         * center position of the view, use fraction 0.5; to track the end position of the view
         * use fraction 1.
         *
         * @param fraction Fraction of size of the view.
         * @return This ChildPositionProperty object.
         */
        public ChildPositionProperty fraction(float fraction) {
            mFraction = fraction;
            return this;
        }

        /**
         * Returns adapter position of the recyclerview child to track.
         */
        public int getAdapterPosition() {
            return mAdapterPosition;
        }

        /**
         * Returns view Id of a descendant of recyclerview child to track.
         */
        public int getViewId() {
            return mViewId;
        }

        /**
         * Returns offset in pixels added to the view's start position.
         */
        public int getOffset() {
            return mOffset;
        }

        /**
         * Returns fraction of size to be added to view's start position.  e.g. to track the
         * center position of the view, use fraction 0.5; to track the end position of the view
         * use fraction 1.
         */
        public float getFraction() {
            return mFraction;
        }

        void updateValue(ParallaxRecyclerViewSource source) {
            RecyclerView recyclerView = source.mRecylerView;
            ViewHolder viewHolder
                    = recyclerView.findViewHolderForAdapterPosition(mAdapterPosition);
            if (viewHolder == null) {
                View firstChild = recyclerView.getChildAt(0);
                ViewHolder vh = recyclerView.findContainingViewHolder(
                        firstChild);
                int firstPosition = vh.getAdapterPosition();
                if (firstPosition < mAdapterPosition) {
                    source.setPropertyValue(getIndex(), IntProperty.UNKNOWN_AFTER);
                } else {
                    source.setPropertyValue(getIndex(), IntProperty.UNKNOWN_BEFORE);
                }
            } else {
                View trackingView = viewHolder.itemView.findViewById(mViewId);
                if (trackingView == null) {
                    return;
                }

                Rect rect = new Rect(
                        0, 0, trackingView.getWidth(), trackingView.getHeight());
                recyclerView.offsetDescendantRectToMyCoords(trackingView, rect);
                if (source.mIsVertical) {
                    source.setPropertyValue(getIndex(), rect.top + mOffset +
                            (int) (mFraction * rect.height()));
                } else {
                    source.setPropertyValue(getIndex(), rect.left + mOffset +
                            (int) (mFraction * rect.width()));
                }
            }
        }
    }

    @Override
    public ChildPositionProperty createProperty(String name, int index) {
        return new ChildPositionProperty(name, index);
    }

    @Override
    public void setListener(Listener listener) {
        mListener = listener;
    }

    @Override
    public int getMaxParentVisibleSize() {
        if (mRecylerView == null) {
            return 0;
        }
        return mIsVertical ? mRecylerView.getHeight() : mRecylerView.getWidth();
    }

    /**
     * Constructor.
     */
    public ParallaxRecyclerViewSource(RecyclerView parent) {
        this.mRecylerView = parent;
        LayoutManager.Properties properties = mRecylerView.getLayoutManager()
                .getProperties(mRecylerView.getContext(), null, 0, 0);
        mIsVertical = properties.orientation == RecyclerView.VERTICAL;
        setupScrollListener();
    }

    private void setupScrollListener() {
        mRecylerView.addOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                for (ChildPositionProperty prop: getProperties()) {
                    prop.updateValue(ParallaxRecyclerViewSource.this);
                }
                mListener.onPropertiesChanged(ParallaxRecyclerViewSource.this);
            }
        });
    }
}
