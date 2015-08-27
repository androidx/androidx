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

import android.support.v17.leanback.R;
import android.support.v17.leanback.graphics.ColorOverlayDimmer;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.animation.TimeAnimator;
import android.content.res.Resources;
import static android.support.v17.leanback.widget.FocusHighlight.ZOOM_FACTOR_NONE;
import static android.support.v17.leanback.widget.FocusHighlight.ZOOM_FACTOR_SMALL;
import static android.support.v17.leanback.widget.FocusHighlight.ZOOM_FACTOR_XSMALL;
import static android.support.v17.leanback.widget.FocusHighlight.ZOOM_FACTOR_MEDIUM;
import static android.support.v17.leanback.widget.FocusHighlight.ZOOM_FACTOR_LARGE;

/**
 * Sets up the highlighting behavior when an item gains focus.
 */
public class FocusHighlightHelper {

    static boolean isValidZoomIndex(int zoomIndex) {
        return zoomIndex == ZOOM_FACTOR_NONE || getResId(zoomIndex) > 0;
    }

    private static int getResId(int zoomIndex) {
        switch (zoomIndex) {
            case ZOOM_FACTOR_SMALL:
                return R.fraction.lb_focus_zoom_factor_small;
            case ZOOM_FACTOR_XSMALL:
                return R.fraction.lb_focus_zoom_factor_xsmall;
            case ZOOM_FACTOR_MEDIUM:
                return R.fraction.lb_focus_zoom_factor_medium;
            case ZOOM_FACTOR_LARGE:
                return R.fraction.lb_focus_zoom_factor_large;
            default:
                return 0;
        }
    }


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
        private final ColorOverlayDimmer mDimmer;

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

        FocusAnimator(View view, float scale, boolean useDimmer, int duration) {
            mView = view;
            mDuration = duration;
            mScaleDiff = scale - 1f;
            if (view instanceof ShadowOverlayContainer) {
                mWrapper = (ShadowOverlayContainer) view;
            } else {
                mWrapper = null;
            }
            mAnimator.setTimeListener(this);
            if (useDimmer) {
                mDimmer = ColorOverlayDimmer.createDefault(view.getContext());
            } else {
                mDimmer = null;
            }
        }

        void setFocusLevel(float level) {
            mFocusLevel = level;
            float scale = 1f + mScaleDiff * level;
            mView.setScaleX(scale);
            mView.setScaleY(scale);
            if (mWrapper != null) {
                mWrapper.setShadowFocusLevel(level);
            } else {
                ShadowOverlayHelper.setNoneWrapperShadowFocusLevel(mView, level);
            }
            if (mDimmer != null) {
                mDimmer.setActiveLevel(level);
                int color = mDimmer.getPaint().getColor();
                if (mWrapper != null) {
                    mWrapper.setOverlayColor(color);
                } else {
                    ShadowOverlayHelper.setNoneWrapperOverlayColor(mView, color);
                }
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

    static class BrowseItemFocusHighlight implements FocusHighlightHandler {
        private static final int DURATION_MS = 150;

        private int mScaleIndex;
        private final boolean mUseDimmer;

        BrowseItemFocusHighlight(int zoomIndex, boolean useDimmer) {
            if (!isValidZoomIndex(zoomIndex)) {
                throw new IllegalArgumentException("Unhandled zoom index");
            }
            mScaleIndex = zoomIndex;
            mUseDimmer = useDimmer;
        }

        private float getScale(Resources res) {
            return mScaleIndex == ZOOM_FACTOR_NONE ? 1f :
                    res.getFraction(getResId(mScaleIndex), 1, 1);
        }

        @Override
        public void onItemFocused(View view, boolean hasFocus) {
            view.setSelected(hasFocus);
            getOrCreateAnimator(view).animateFocus(hasFocus, false);
        }

        @Override
        public void onInitializeView(View view) {
            getOrCreateAnimator(view).animateFocus(false, true);
        }

        private FocusAnimator getOrCreateAnimator(View view) {
            FocusAnimator animator = (FocusAnimator) view.getTag(R.id.lb_focus_animator);
            if (animator == null) {
                animator = new FocusAnimator(
                        view, getScale(view.getResources()), mUseDimmer, DURATION_MS);
                view.setTag(R.id.lb_focus_animator, animator);
            }
            return animator;
        }

    }

    /**
     * Sets up the focus highlight behavior of a focused item in browse list row.
     * @param zoomIndex One of {@link FocusHighlight#ZOOM_FACTOR_SMALL}
     * {@link FocusHighlight#ZOOM_FACTOR_XSMALL}
     * {@link FocusHighlight#ZOOM_FACTOR_MEDIUM}
     * {@link FocusHighlight#ZOOM_FACTOR_LARGE}
     * {@link FocusHighlight#ZOOM_FACTOR_NONE}.
     * @param useDimmer Allow dimming browse item when unselected.
     * @param adapter  adapter of the list row.
     */
    public static void setupBrowseItemFocusHighlight(ItemBridgeAdapter adapter, int zoomIndex,
            boolean useDimmer) {
        adapter.setFocusHighlight(new BrowseItemFocusHighlight(zoomIndex, useDimmer));
    }

    /**
     * Sets up the focus highlight behavior of a focused item in header list.
     * @param gridView  the header list.
     */
    public static void setupHeaderItemFocusHighlight(VerticalGridView gridView) {
        if (gridView.getAdapter() instanceof ItemBridgeAdapter) {
            ((ItemBridgeAdapter) gridView.getAdapter())
                    .setFocusHighlight(new HeaderItemFocusHighlight(gridView));
        }
    }

    static class HeaderItemFocusHighlight implements FocusHighlightHandler {
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
                super(view, scale, false, duration);
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

        @Override
        public void onInitializeView(View view) {
        }

    }
}
