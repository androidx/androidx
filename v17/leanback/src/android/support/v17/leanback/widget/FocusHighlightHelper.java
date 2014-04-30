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

import android.graphics.drawable.TransitionDrawable;
import android.support.v17.leanback.R;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.animation.TimeAnimator;
import android.content.res.Resources;

import static android.support.v17.leanback.widget.FocusHighlight.ZOOM_FACTOR_NONE;
import static android.support.v17.leanback.widget.FocusHighlight.ZOOM_FACTOR_SMALL;
import static android.support.v17.leanback.widget.FocusHighlight.ZOOM_FACTOR_MEDIUM;
import static android.support.v17.leanback.widget.FocusHighlight.ZOOM_FACTOR_LARGE;


/**
 * Setup the behavior how to highlight when a item gains focus.
 */
public class FocusHighlightHelper {

    static class FocusAnimator implements TimeAnimator.TimeListener {
        private final View mView;
        private final int mDuration;
        private final ShadowOverlayContainer mWrapper;
        private final float mScaleDiff;
        private float mFocusLevel = 0f;
        private float mFocusLevelStart;
        private float mFocusLevelDelta;
        private final TimeAnimator mAnimator = new TimeAnimator();
        private final Interpolator mInterpolator = new AccelerateDecelerateInterpolator();

        void animateFocus(boolean select, boolean immediate) {
            endAnimation();
            final float end = select ? 1 : 0;
            if (immediate) {
                setFocusLevel(end);
            } else if (mFocusLevel != end) {
                mFocusLevelStart = mFocusLevel;
                mFocusLevelDelta = end - mFocusLevelStart;
                mAnimator.start();
            }
        }

        FocusAnimator(View view, float scale, int duration) {
            mView = view;
            mDuration = duration;
            mScaleDiff = scale - 1f;
            if (view instanceof ShadowOverlayContainer) {
                mWrapper = (ShadowOverlayContainer) view;
            } else {
                mWrapper = null;
            }
            mAnimator.setTimeListener(this);
        }

        void setFocusLevel(float level) {
            mFocusLevel = level;
            float scale = 1f + mScaleDiff * level;
            mView.setScaleX(scale);
            mView.setScaleY(scale);
            if (mWrapper != null) {
                mWrapper.setShadowFocusLevel(level);
            }
        }

        float getFocusLevel() {
            return mFocusLevel;
        }

        void endAnimation() {
            mAnimator.end();
        }

        @Override
        public void onTimeUpdate(TimeAnimator animation, long totalTime, long deltaTime) {
            float fraction;
            if (totalTime >= mDuration) {
                fraction = 1;
                mAnimator.end();
            } else {
                fraction = (float) (totalTime / (double) mDuration);
            }
            if (mInterpolator != null) {
                fraction = mInterpolator.getInterpolation(fraction);
            }
            setFocusLevel(mFocusLevelStart + fraction * mFocusLevelDelta);
        }
    }

    static class BrowseItemFocusHighlight implements FocusHighlight {
        private static final int DURATION_MS = 150;

        private static float[] sScaleFactor = new float[4];

        private int mScaleIndex;

        BrowseItemFocusHighlight(int zoomIndex) {
            mScaleIndex = (zoomIndex >= 0 && zoomIndex < sScaleFactor.length) ?
                    zoomIndex : ZOOM_FACTOR_MEDIUM;
        }

        private static void lazyInit(Resources resources) {
            if (sScaleFactor[ZOOM_FACTOR_NONE] == 0f) {
                sScaleFactor[ZOOM_FACTOR_NONE] = 1f;
                sScaleFactor[ZOOM_FACTOR_SMALL] =
                        resources.getFraction(R.fraction.lb_focus_zoom_factor_small, 1, 1);
                sScaleFactor[ZOOM_FACTOR_MEDIUM] =
                        resources.getFraction(R.fraction.lb_focus_zoom_factor_medium, 1, 1);
                sScaleFactor[ZOOM_FACTOR_LARGE] =
                        resources.getFraction(R.fraction.lb_focus_zoom_factor_large, 1, 1);
            }
        }

        private float getScale(View view) {
            lazyInit(view.getResources());
            return sScaleFactor[mScaleIndex];
        }

        private void viewFocused(View view, boolean hasFocus) {
            view.setSelected(hasFocus);
            FocusAnimator animator = (FocusAnimator) view.getTag(R.id.lb_focus_animator);
            if (animator == null) {
                animator = new FocusAnimator(view, getScale(view), DURATION_MS);
                view.setTag(R.id.lb_focus_animator, animator);
            }
            animator.animateFocus(hasFocus, false);
        }

        @Override
        public void onItemFocused(View view, boolean hasFocus) {
            viewFocused(view, hasFocus);
        }
    }

    private static ActionItemFocusHighlight sActionItemFocusHighlight =
            new ActionItemFocusHighlight();

    /**
     * Setup the focus highlight behavior of a focused item in browse list row.
     * @param adapter  adapter of the list row.
     */
    public static void setupBrowseItemFocusHighlight(ItemBridgeAdapter adapter, int zoomIndex) {
        adapter.setFocusHighlight(new BrowseItemFocusHighlight(zoomIndex));
    }

    /**
     * Setup the focus highlight behavior of a focused item in header list.
     * @param gridView  the header list.
     */
    public static void setupHeaderItemFocusHighlight(VerticalGridView gridView) {
        if (gridView.getAdapter() instanceof ItemBridgeAdapter) {
            ((ItemBridgeAdapter) gridView.getAdapter())
                    .setFocusHighlight(new HeaderItemFocusHighlight(gridView));
        }
    }

    /**
     * Setup the focus highlight behavior of a focused item in an action list.
     * @param adapter  adapter of the action list.
     */
    public static void setupActionItemFocusHighlight(ItemBridgeAdapter adapter) {
        adapter.setFocusHighlight(sActionItemFocusHighlight);
    }

    static class HeaderItemFocusHighlight implements FocusHighlight {
        private static boolean sInitialized;
        private static float sSelectScale;
        private static int sDuration;
        private BaseGridView mGridView;

        HeaderItemFocusHighlight(BaseGridView gridView) {
            mGridView = gridView;
            lazyInit(gridView.getContext().getResources());
        }

        private static void lazyInit(Resources res) {
            if (!sInitialized) {
                sSelectScale =
                        Float.parseFloat(res.getString(R.dimen.lb_browse_header_select_scale));
                sDuration =
                        Integer.parseInt(res.getString(R.dimen.lb_browse_header_select_duration));
                sInitialized = true;
            }
        }

        class HeaderFocusAnimator extends FocusAnimator {

            ItemBridgeAdapter.ViewHolder mViewHolder;
            HeaderFocusAnimator(View view, float scale, int duration) {
                super(view, scale, duration);
                mViewHolder = (ItemBridgeAdapter.ViewHolder) mGridView.getChildViewHolder(view);
            }

            @Override
            void setFocusLevel(float level) {
                Presenter presenter = mViewHolder.getPresenter();
                if (presenter instanceof RowHeaderPresenter) {
                    ((RowHeaderPresenter) presenter).setSelectLevel(
                            ((RowHeaderPresenter.ViewHolder) mViewHolder.getViewHolder()), level);
                }
                super.setFocusLevel(level);
            }

        }

        private void viewFocused(View view, boolean hasFocus) {
            view.setSelected(hasFocus);
            FocusAnimator animator = (FocusAnimator) view.getTag(R.id.lb_focus_animator);
            if (animator == null) {
                animator = new HeaderFocusAnimator(view, sSelectScale, sDuration);
                view.setTag(R.id.lb_focus_animator, animator);
            }
            animator.animateFocus(hasFocus, false);
        }

        @Override
        public void onItemFocused(View view, boolean hasFocus) {
            viewFocused(view, hasFocus);
        }
    }

    private static class ActionItemFocusHighlight implements FocusHighlight {
        private boolean mInitialized;
        private int mDuration;

        private void initializeDimensions(Resources res) {
            if (!mInitialized) {
                mDuration = Integer.parseInt(res.getString(R.dimen.lb_details_overview_action_select_duration));
            }
        }

        @Override
        public void onItemFocused(View view, boolean hasFocus) {
            initializeDimensions(view.getResources());
            TransitionDrawable td = (TransitionDrawable) view.getBackground();
            if (hasFocus) {
                td.startTransition(mDuration);
            } else {
                td.reverseTransition(mDuration);
            }
        }
    }
}
