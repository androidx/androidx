/*
 * Copyright (C) 2015 The Android Open Source Project
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

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v17.leanback.R;
import android.support.v17.leanback.widget.VerticalGridView;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.animation.DecelerateInterpolator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewPropertyAnimator;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * GuidedActionsStylist is used within a {@link android.support.v17.leanback.app.GuidedStepFragment}
 * to supply the right-side panel where users can take actions. It consists of a container for the
 * list of actions, and a stationary selector view that indicates visually the location of focus.
 * <p>
 * Many aspects of the base GuidedActionsStylist can be customized through theming; see the
 * theme attributes below. Note that these attributes are not set on individual elements in layout
 * XML, but instead would be set in a custom theme. See
 * <a href="http://developer.android.com/guide/topics/ui/themes.html">Styles and Themes</a>
 * for more information.
 * <p>
 * If these hooks are insufficient, this class may also be subclassed. Subclasses may wish to
 * override the {@link #onProvideLayoutId} method to change the layout used to display the
 * list container and selector, or the {@link #onProvideItemLayoutId} method to change the layout
 * used to display each action.
 * <p>
 * Note: If an alternate list layout is provided, the following view IDs must be supplied:
 * <ul>
 * <li>{@link android.support.v17.leanback.R.id#guidedactions_selector}</li>
 * <li>{@link android.support.v17.leanback.R.id#guidedactions_list}</li>
 * </ul><p>
 * These view IDs must be present in order for the stylist to function. The list ID must correspond
 * to a {@link VerticalGridView} or subclass.
 * <p>
 * If an alternate item layout is provided, the following view IDs should be used to refer to base
 * elements:
 * <ul>
 * <li>{@link android.support.v17.leanback.R.id#guidedactions_item_content}</li>
 * <li>{@link android.support.v17.leanback.R.id#guidedactions_item_title}</li>
 * <li>{@link android.support.v17.leanback.R.id#guidedactions_item_description}</li>
 * <li>{@link android.support.v17.leanback.R.id#guidedactions_item_icon}</li>
 * <li>{@link android.support.v17.leanback.R.id#guidedactions_item_checkmark}</li>
 * <li>{@link android.support.v17.leanback.R.id#guidedactions_item_chevron}</li>
 * </ul><p>
 * These view IDs are allowed to be missing, in which case the corresponding views in {@link
 * GuidedActionsStylist.ViewHolder} will be null.
 *
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedActionsEntryAnimation
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedActionsSelectorShowAnimation
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedActionsSelectorHideAnimation
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedActionsContainerStyle
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedActionsSelectorStyle
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedActionsListStyle
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedActionItemContainerStyle
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedActionItemCheckmarkStyle
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedActionItemIconStyle
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedActionItemContentStyle
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedActionItemTitleStyle
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedActionItemDescriptionStyle
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedActionItemChevronStyle
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedActionCheckedAnimation
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedActionUncheckedAnimation
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedActionPressedAnimation
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedActionUnpressedAnimation
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedActionEnabledChevronAlpha
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedActionDisabledChevronAlpha
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedActionContentWidth
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedActionContentWidthNoIcon
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedActionTitleMinLines
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedActionTitleMaxLines
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedActionDescriptionMinLines
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedActionVerticalPadding
 * @see android.support.v17.leanback.app.GuidedStepFragment
 * @see GuidedAction
 */
public class GuidedActionsStylist implements FragmentAnimationProvider {

    /**
     * ViewHolder caches information about the action item layouts' subviews. Subclasses of {@link
     * GuidedActionsStylist} may also wish to subclass this in order to add fields.
     * @see GuidedAction
     */
    public static class ViewHolder {

        public final View view;

        private View mContentView;
        private TextView mTitleView;
        private TextView mDescriptionView;
        private ImageView mIconView;
        private ImageView mCheckmarkView;
        private ImageView mChevronView;

        /**
         * Constructs an ViewHolder and caches the relevant subviews.
         */
        public ViewHolder(View v) {
            view = v;

            mContentView = v.findViewById(R.id.guidedactions_item_content);
            mTitleView = (TextView) v.findViewById(R.id.guidedactions_item_title);
            mDescriptionView = (TextView) v.findViewById(R.id.guidedactions_item_description);
            mIconView = (ImageView) v.findViewById(R.id.guidedactions_item_icon);
            mCheckmarkView = (ImageView) v.findViewById(R.id.guidedactions_item_checkmark);
            mChevronView = (ImageView) v.findViewById(R.id.guidedactions_item_chevron);
        }

        /**
         * Returns the content view within this view holder's view, where title and description are
         * shown.
         */
        public View getContentView() {
            return mContentView;
        }

        /**
         * Returns the title view within this view holder's view.
         */
        public TextView getTitleView() {
            return mTitleView;
        }

        /**
         * Returns the description view within this view holder's view.
         */
        public TextView getDescriptionView() {
            return mDescriptionView;
        }

        /**
         * Returns the icon view within this view holder's view.
         */
        public ImageView getIconView() {
            return mIconView;
        }

        /**
         * Returns the checkmark view within this view holder's view.
         */
        public ImageView getCheckmarkView() {
            return mCheckmarkView;
        }

        /**
         * Returns the chevron view within this view holder's view.
         */
        public ImageView getChevronView() {
            return mChevronView;
        }

    }

    private static String TAG = "GuidedActionsStylist";

    protected View mMainView;
    protected VerticalGridView mActionsGridView;
    protected View mSelectorView;

    // Cached values from resources
    private float mEnabledChevronAlpha;
    private float mDisabledChevronAlpha;
    private int mContentWidth;
    private int mContentWidthNoIcon;
    private int mTitleMinLines;
    private int mTitleMaxLines;
    private int mDescriptionMinLines;
    private int mVerticalPadding;
    private int mDisplayHeight;

    /**
     * Creates a view appropriate for displaying a list of GuidedActions, using the provided
     * inflater and container.
     * <p>
     * <i>Note: Does not actually add the created view to the container; the caller should do
     * this.</i>
     * @param inflater The layout inflater to be used when constructing the view.
     * @param container The view group to be passed in the call to
     * <code>LayoutInflater.inflate</code>.
     * @return The view to be added to the caller's view hierarchy.
     */
    public View onCreateView(LayoutInflater inflater, ViewGroup container) {
        mMainView = inflater.inflate(onProvideLayoutId(), container, false);
        mSelectorView = mMainView.findViewById(R.id.guidedactions_selector);
        if (mMainView instanceof VerticalGridView) {
            mActionsGridView = (VerticalGridView) mMainView;
        } else {
            mActionsGridView = (VerticalGridView) mMainView.findViewById(R.id.guidedactions_list);
            if (mActionsGridView == null) {
                throw new IllegalStateException("No ListView exists.");
            }
            mActionsGridView.setWindowAlignmentOffset(0);
            mActionsGridView.setWindowAlignmentOffsetPercent(50f);
            mActionsGridView.setWindowAlignment(VerticalGridView.WINDOW_ALIGN_NO_EDGE);
            if (mSelectorView != null) {
                mActionsGridView.setOnScrollListener(new
                        SelectorAnimator(mSelectorView, mActionsGridView));
            }
        }

        mActionsGridView.requestFocusFromTouch();

        if (mSelectorView != null) {
            // ALlow focus to move to other views
            mActionsGridView.getViewTreeObserver().addOnGlobalFocusChangeListener(
                    new ViewTreeObserver.OnGlobalFocusChangeListener() {
                        private boolean mChildFocused;

                        @Override
                        public void onGlobalFocusChanged(View oldFocus, View newFocus) {
                            View focusedChild = mActionsGridView.getFocusedChild();
                            if (focusedChild == null) {
                                mSelectorView.setVisibility(View.INVISIBLE);
                                mChildFocused = false;
                            } else if (!mChildFocused) {
                                mChildFocused = true;
                                mSelectorView.setVisibility(View.VISIBLE);
                                updateSelectorView(focusedChild);
                            }
                        }
                    });
        }

        // Cache widths, chevron alpha values, max and min text lines, etc
        Context ctx = mMainView.getContext();
        TypedValue val = new TypedValue();
        mEnabledChevronAlpha = getFloat(ctx, val, R.attr.guidedActionEnabledChevronAlpha);
        mDisabledChevronAlpha = getFloat(ctx, val, R.attr.guidedActionDisabledChevronAlpha);
        mContentWidth = getDimension(ctx, val, R.attr.guidedActionContentWidth);
        mContentWidthNoIcon = getDimension(ctx, val, R.attr.guidedActionContentWidthNoIcon);
        mTitleMinLines = getInteger(ctx, val, R.attr.guidedActionTitleMinLines);
        mTitleMaxLines = getInteger(ctx, val, R.attr.guidedActionTitleMaxLines);
        mDescriptionMinLines = getInteger(ctx, val, R.attr.guidedActionDescriptionMinLines);
        mVerticalPadding = getDimension(ctx, val, R.attr.guidedActionVerticalPadding);
        mDisplayHeight = ((WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay().getHeight();

        return mMainView;
    }

    /**
     * Returns the VerticalGridView that displays the list of GuidedActions.
     * @return The VerticalGridView for this presenter.
     */
    public VerticalGridView getActionsGridView() {
        return mActionsGridView;
    }

    /**
     * Provides the resource ID of the layout defining the host view for the list of guided actions.
     * Subclasses may override to provide their own customized layouts. The base implementation
     * returns {@link android.support.v17.leanback.R.layout#lb_guidedactions}. If overridden, the
     * substituted layout should contain matching IDs for any views that should be managed by the
     * base class; this can be achieved by starting with a copy of the base layout file.
     * @return The resource ID of the layout to be inflated to define the host view for the list
     * of GuidedActions.
     */
    public int onProvideLayoutId() {
        return R.layout.lb_guidedactions;
    }

    /**
     * Provides the resource ID of the layout defining the view for an individual guided actions.
     * Subclasses may override to provide their own customized layouts. The base implementation
     * returns {@link android.support.v17.leanback.R.layout#lb_guidedactions_item}. If overridden,
     * the substituted layout should contain matching IDs for any views that should be managed by
     * the base class; this can be achieved by starting with a copy of the base layout file.
     * @return The resource ID of the layout to be inflated to define the view to display an
     * individual GuidedAction.
     */
    public int onProvideItemLayoutId() {
        return R.layout.lb_guidedactions_item;
    }

    /**
     * Constructs a {@link ViewHolder} capable of representing {@link GuidedAction}s. Subclasses
     * may choose to return a subclass of ViewHolder.
     * <p>
     * <i>Note: Should not actually add the created view to the parent; the caller will do
     * this.</i>
     * @param parent The view group to be used as the parent of the new view.
     * @return The view to be added to the caller's view hierarchy.
     */
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View v = inflater.inflate(onProvideItemLayoutId(), parent, false);
        return new ViewHolder(v);
    }

    /**
     * Binds a {@link ViewHolder} to a particular {@link GuidedAction}.
     * @param vh The view holder to be associated with the given action.
     * @param action The guided action to be displayed by the view holder's view.
     * @return The view to be added to the caller's view hierarchy.
     */
    public void onBindViewHolder(ViewHolder vh, GuidedAction action) {

        if (vh.mTitleView != null) {
            vh.mTitleView.setText(action.getTitle());
        }
        if (vh.mDescriptionView != null) {
            vh.mDescriptionView.setText(action.getDescription());
            vh.mDescriptionView.setVisibility(TextUtils.isEmpty(action.getDescription()) ?
                    View.GONE : View.VISIBLE);
        }
        // Clients might want the check mark view to be gone entirely, in which case, ignore it.
        if (vh.mCheckmarkView != null && vh.mCheckmarkView.getVisibility() != View.GONE) {
            vh.mCheckmarkView.setVisibility(action.isChecked() ? View.VISIBLE : View.INVISIBLE);
        }

        if (vh.mContentView != null) {
            ViewGroup.LayoutParams contentLp = vh.mContentView.getLayoutParams();
            if (setIcon(vh.mIconView, action)) {
                contentLp.width = mContentWidth;
            } else {
                contentLp.width = mContentWidthNoIcon;
            }
            vh.mContentView.setLayoutParams(contentLp);
        }

        if (vh.mChevronView != null) {
            vh.mChevronView.setVisibility(action.hasNext() ? View.VISIBLE : View.INVISIBLE);
            vh.mChevronView.setAlpha(action.isEnabled() ? mEnabledChevronAlpha :
                    mDisabledChevronAlpha);
        }

        if (action.hasMultilineDescription()) {
            if (vh.mTitleView != null) {
                vh.mTitleView.setMaxLines(mTitleMaxLines);
                if (vh.mDescriptionView != null) {
                    vh.mDescriptionView.setMaxHeight(getDescriptionMaxHeight(vh.view.getContext(),
                            vh.mTitleView));
                }
            }
        } else {
            if (vh.mTitleView != null) {
                vh.mTitleView.setMaxLines(mTitleMinLines);
            }
            if (vh.mDescriptionView != null) {
                vh.mDescriptionView.setMaxLines(mDescriptionMinLines);
            }
        }
    }

    /**
     * Animates the view holder's view (or subviews thereof) when the action has had its focus
     * state changed.
     * @param vh The view holder associated with the relevant action.
     * @param focused True if the action has become focused, false if it has lost focus.
     */
    public void onAnimateItemFocused(ViewHolder vh, boolean focused) {
        // No animations for this, currently, because the animation is done on
        // mSelectorView
    }

    /**
     * Animates the view holder's view (or subviews thereof) when the action has had its press
     * state changed.
     * @param vh The view holder associated with the relevant action.
     * @param pressed True if the action has been pressed, false if it has been unpressed.
     */
    public void onAnimateItemPressed(ViewHolder vh, boolean pressed) {
        int attr = pressed ? R.attr.guidedActionPressedAnimation :
                R.attr.guidedActionUnpressedAnimation;
        createAnimator(vh.view, attr).start();
    }

    /**
     * Animates the view holder's view (or subviews thereof) when the action has had its check
     * state changed.
     * @param vh The view holder associated with the relevant action.
     * @param checked True if the action has become checked, false if it has become unchecked.
     */
    public void onAnimateItemChecked(ViewHolder vh, boolean checked) {
        final View checkView = vh.mCheckmarkView;
        if (checkView != null) {
            if (checked) {
                checkView.setVisibility(View.VISIBLE);
                createAnimator(checkView, R.attr.guidedActionCheckedAnimation).start();
            } else {
                Animator animator = createAnimator(checkView,
                        R.attr.guidedActionUncheckedAnimation);
                animator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        checkView.setVisibility(View.INVISIBLE);
                    }
                });
                animator.start();
            }
        }
    }

    /*
     * ==========================================
     * FragmentAnimationProvider overrides
     * ==========================================
     */

    /**
     * {@inheritDoc}
     */
    @Override
    public void onActivityEnter(@NonNull List<Animator> animators) {
        animators.add(createAnimator(mMainView, R.attr.guidedActionsEntryAnimation));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onActivityExit(@NonNull List<Animator> animators) {}

    /**
     * {@inheritDoc}
     */
    @Override
    public void onFragmentEnter(@NonNull List<Animator> animators) {
        animators.add(createAnimator(mActionsGridView, R.attr.guidedStepEntryAnimation));
        animators.add(createAnimator(mSelectorView, R.attr.guidedStepEntryAnimation));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onFragmentExit(@NonNull List<Animator> animators) {
        animators.add(createAnimator(mActionsGridView, R.attr.guidedStepExitAnimation));
        animators.add(createAnimator(mSelectorView, R.attr.guidedStepExitAnimation));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onFragmentReenter(@NonNull List<Animator> animators) {
        animators.add(createAnimator(mActionsGridView, R.attr.guidedStepReentryAnimation));
        animators.add(createAnimator(mSelectorView, R.attr.guidedStepReentryAnimation));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onFragmentReturn(@NonNull List<Animator> animators) {
        animators.add(createAnimator(mActionsGridView, R.attr.guidedStepReturnAnimation));
        animators.add(createAnimator(mSelectorView, R.attr.guidedStepReturnAnimation));
    }

    /*
     * ==========================================
     * Private methods
     * ==========================================
     */

    private void updateSelectorView(View focusedChild) {
        // Display the selector view.
        int height = focusedChild.getHeight();
        LayoutParams lp = mSelectorView.getLayoutParams();
        lp.height = height;
        mSelectorView.setLayoutParams(lp);
        mSelectorView.setAlpha(1f);
    }

    private float getFloat(Context ctx, TypedValue typedValue, int attrId) {
        ctx.getTheme().resolveAttribute(attrId, typedValue, true);
        // Android resources don't have a native float type, so we have to use strings.
        return Float.valueOf(ctx.getResources().getString(typedValue.resourceId));
    }

    private int getInteger(Context ctx, TypedValue typedValue, int attrId) {
        ctx.getTheme().resolveAttribute(attrId, typedValue, true);
        return ctx.getResources().getInteger(typedValue.resourceId);
    }

    private int getDimension(Context ctx, TypedValue typedValue, int attrId) {
        ctx.getTheme().resolveAttribute(attrId, typedValue, true);
        return ctx.getResources().getDimensionPixelSize(typedValue.resourceId);
    }

    private static Animator createAnimator(View v, int attrId) {
        Context ctx = v.getContext();
        TypedValue typedValue = new TypedValue();
        ctx.getTheme().resolveAttribute(attrId, typedValue, true);
        Animator animator = AnimatorInflater.loadAnimator(ctx, typedValue.resourceId);
        animator.setTarget(v);
        return animator;
    }

    private boolean setIcon(final ImageView iconView, GuidedAction action) {
        Drawable icon = null;
        if (iconView != null) {
            Context context = iconView.getContext();
            icon = action.getIcon();
            if (icon != null) {
                // setImageDrawable resets the drawable's level unless we set the view level first.
                iconView.setImageLevel(icon.getLevel());
                iconView.setImageDrawable(icon);
                iconView.setVisibility(View.VISIBLE);
            } else {
                iconView.setVisibility(View.GONE);
            }
        }
        return icon != null;
    }

    /**
     * @return the max height in pixels the description can be such that the
     *         action nicely takes up the entire screen.
     */
    private int getDescriptionMaxHeight(Context context, TextView title) {
        // The 2 multiplier on the title height calculation is a
        // conservative estimate for font padding which can not be
        // calculated at this stage since the view hasn't been rendered yet.
        return (int)(mDisplayHeight - 2*mVerticalPadding - 2*mTitleMaxLines*title.getLineHeight());
    }

    /**
     * SelectorAnimator
     * Controls animation for selected item backgrounds
     * TODO: Move into focus animation override?
     */
    private static class SelectorAnimator extends RecyclerView.OnScrollListener {

        private final View mSelectorView;
        private final ViewGroup mParentView;
        private volatile boolean mFadedOut = true;

        SelectorAnimator(View selectorView, ViewGroup parentView) {
            mSelectorView = selectorView;
            mParentView = parentView;
        }

        // We want to fade in the selector if we've stopped scrolling on it. If
        // we're scrolling, we want to ensure to dim the selector if we haven't
        // already. We dim the last highlighted view so that while a user is
        // scrolling, nothing is highlighted.
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            Animator animator = null;
            boolean fadingOut = false;
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                // The selector starts with a height of 0. In order to scale up from
                // 0, we first need the set the height to 1 and scale from there.
                View focusedChild = mParentView.getFocusedChild();
                if (focusedChild != null) {
                    int selectorHeight = mSelectorView.getHeight();
                    float scaleY = (float) focusedChild.getHeight() / selectorHeight;
                    AnimatorSet animators = (AnimatorSet)createAnimator(mSelectorView,
                            R.attr.guidedActionsSelectorShowAnimation);
                    if (mFadedOut) {
                        // selector is completely faded out, so we can just scale before fading in.
                        mSelectorView.setScaleY(scaleY);
                        animator = animators.getChildAnimations().get(0);
                    } else {
                        // selector is not faded out, so we must animate the scale as we fade in.
                        ((ObjectAnimator)animators.getChildAnimations().get(1))
                                .setFloatValues(scaleY);
                        animator = animators;
                    }
                }
            } else {
                animator = createAnimator(mSelectorView, R.attr.guidedActionsSelectorHideAnimation);
                fadingOut = true;
            }
            if (animator != null) {
                animator.addListener(new Listener(fadingOut));
                animator.start();
            }
        }

        /**
         * Sets {@link BaseScrollAdapterFragment#mFadedOut}
         * {@link BaseScrollAdapterFragment#mFadedOut} is true, iff
         * {@link BaseScrollAdapterFragment#mSelectorView} has an alpha of 0
         * (faded out). If false the view either has an alpha of 1 (visible) or
         * is in the process of animating.
         */
        private class Listener implements Animator.AnimatorListener {
            private boolean mFadingOut;
            private boolean mCanceled;

            public Listener(boolean fadingOut) {
                mFadingOut = fadingOut;
            }

            @Override
            public void onAnimationStart(Animator animation) {
                if (!mFadingOut) {
                    mFadedOut = false;
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!mCanceled && mFadingOut) {
                    mFadedOut = true;
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mCanceled = true;
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        }
    }

}
