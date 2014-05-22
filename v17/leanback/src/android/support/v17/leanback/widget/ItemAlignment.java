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

import static android.support.v7.widget.RecyclerView.HORIZONTAL;
import static android.support.v7.widget.RecyclerView.VERTICAL;
import static android.support.v17.leanback.widget.BaseGridView.ITEM_ALIGN_OFFSET_PERCENT_DISABLED;

import android.graphics.Rect;
import android.support.v17.leanback.widget.GridLayoutManager.LayoutParams;
import android.view.View;
import android.view.ViewGroup;

/**
 * Defines alignment position on two directions of an item view. Typically item
 * view alignment is at the center of the view. The class allows defining
 * alignment at left/right or fixed offset/percentage position; it also allows
 * using descendant view by id match.
 */
class ItemAlignment {

    final static class Axis {
        private int mOrientation;
        private int mOffset = 0;
        private float mOffsetPercent = 50;
        private int mViewId = 0;
        private boolean mOffsetWithPadding = false;
        private Rect mRect = new Rect();

        Axis(int orientation) {
            mOrientation = orientation;
        }

        public void setItemAlignmentOffset(int offset) {
            mOffset = offset;
        }

        public int getItemAlignmentOffset() {
            return mOffset;
        }

        public void setItemAlignmentOffsetWithPadding(boolean withPadding) {
            mOffsetWithPadding = withPadding;
        }

        public boolean isItemAlignmentOffsetWithPadding() {
            return mOffsetWithPadding;
        }

        public void setItemAlignmentOffsetPercent(float percent) {
            if ( (percent < 0 || percent > 100) &&
                    percent != ITEM_ALIGN_OFFSET_PERCENT_DISABLED) {
                throw new IllegalArgumentException();
            }
            mOffsetPercent = percent;
        }

        public float getItemAlignmentOffsetPercent() {
            return mOffsetPercent;
        }

        public void setItemAlignmentViewId(int viewId) {
            mViewId = viewId;
        }

        public int getItemAlignmentViewId() {
            return mViewId;
        }

        /**
         * get alignment position relative to optical left/top of itemView.
         */
        public int getAlignmentPosition(View itemView) {
            LayoutParams p = (LayoutParams) itemView.getLayoutParams();
            View view = itemView;
            if (mViewId != 0) {
                view = itemView.findViewById(mViewId);
                if (view == null) {
                    view = itemView;
                }
            }
            int alignPos;
            if (mOrientation == HORIZONTAL) {
                if (mOffset >= 0) {
                    alignPos = mOffset;
                    if (mOffsetWithPadding) {
                        alignPos += view.getPaddingLeft();
                    }
                } else {
                    alignPos = view == itemView ? p.getOpticalWidth(view) : view.getWidth()
                            + mOffset;
                    if (mOffsetWithPadding) {
                        alignPos -= view.getPaddingRight();
                    }
                }
                if (mOffsetPercent != ITEM_ALIGN_OFFSET_PERCENT_DISABLED) {
                    alignPos += ((view == itemView ? p.getOpticalWidth(view) : view.getWidth())
                            * mOffsetPercent) / 100f;
                }
                if (itemView != view) {
                    mRect.left = alignPos;
                    ((ViewGroup) itemView).offsetDescendantRectToMyCoords(view, mRect);
                    alignPos = mRect.left - p.getOpticalLeftInset();
                }
            } else {
                if (mOffset >= 0) {
                    alignPos = mOffset;
                    if (mOffsetWithPadding) {
                        alignPos += view.getPaddingTop();
                    }
                } else {
                    alignPos = view == itemView ? p.getOpticalHeight(view) : view.getHeight()
                            + mOffset;
                    if (mOffsetWithPadding) {
                        alignPos += view.getPaddingBottom();
                    }
                }
                if (mOffsetPercent != ITEM_ALIGN_OFFSET_PERCENT_DISABLED) {
                    alignPos += ((view == itemView ? p.getOpticalHeight(view) : view.getHeight())
                            * mOffsetPercent) / 100f;
                }
                if (itemView != view) {
                    mRect.top = alignPos;
                    ((ViewGroup) itemView).offsetDescendantRectToMyCoords(view, mRect);
                    alignPos = mRect.top - p.getOpticalTopInset();
                }
            }
            return alignPos;
        }
    }

    private int mOrientation = HORIZONTAL;

    final public Axis vertical = new Axis(VERTICAL);

    final public Axis horizontal = new Axis(HORIZONTAL);

    private Axis mMainAxis = horizontal;

    private Axis mSecondAxis = vertical;

    final public Axis mainAxis() {
        return mMainAxis;
    }

    final public Axis secondAxis() {
        return mSecondAxis;
    }

    final public void setOrientation(int orientation) {
        mOrientation = orientation;
        if (mOrientation == HORIZONTAL) {
            mMainAxis = horizontal;
            mSecondAxis = vertical;
        } else {
            mMainAxis = vertical;
            mSecondAxis = horizontal;
        }
    }

    final public int getOrientation() {
        return mOrientation;
    }


}
