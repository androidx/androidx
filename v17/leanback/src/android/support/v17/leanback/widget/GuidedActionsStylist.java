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
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v17.leanback.R;
import android.support.v17.leanback.widget.VerticalGridView;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewPropertyAnimator;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.Checkable;
import android.widget.EditText;
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
 * <p>
 * In order to support editable actions, the view associated with guidedactions_item_title should
 * be a subclass of {@link android.widget.EditText}, and should satisfy the {@link
 * ImeKeyMonitor} interface.
 *
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedStepImeAppearingAnimation
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedStepImeDisappearingAnimation
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedActionsSelectorShowAnimation
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedActionsSelectorHideAnimation
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedActionsSelectorStyle
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedActionsListStyle
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedActionItemContainerStyle
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedActionItemCheckmarkStyle
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedActionItemIconStyle
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedActionItemContentStyle
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedActionItemTitleStyle
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedActionItemDescriptionStyle
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedActionItemChevronStyle
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedActionPressedAnimation
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedActionUnpressedAnimation
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedActionEnabledChevronAlpha
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedActionDisabledChevronAlpha
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedActionTitleMinLines
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedActionTitleMaxLines
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedActionDescriptionMinLines
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedActionVerticalPadding
 * @see android.R.styleable#Theme_listChoiceIndicatorSingle
 * @see android.R.styleable#Theme_listChoiceIndicatorMultiple
 * @see android.support.v17.leanback.app.GuidedStepFragment
 * @see GuidedAction
 */
public class GuidedActionsStylist implements FragmentAnimationProvider {

    /**
     * Default viewType that associated with default layout Id for the action item.
     * @see #getItemViewType(GuidedAction)
     * @see #onProvideItemLayoutId(int)
     * @see #onCreateViewHolder(ViewGroup, int)
     */
    public static final int VIEW_TYPE_DEFAULT = 0;

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
        private boolean mInEditing;
        private boolean mInEditingDescription;

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
         * Convenience method to return an editable version of the title, if possible,
         * or null if the title view isn't an EditText.
         */
        public EditText getEditableTitleView() {
            return (mTitleView instanceof EditText) ? (EditText)mTitleView : null;
        }

        /**
         * Returns the description view within this view holder's view.
         */
        public TextView getDescriptionView() {
            return mDescriptionView;
        }

        /**
         * Convenience method to return an editable version of the description, if possible,
         * or null if the description view isn't an EditText.
         */
        public EditText getEditableDescriptionView() {
            return (mDescriptionView instanceof EditText) ? (EditText)mDescriptionView : null;
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

        /**
         * Returns true if the TextView is in editing title or description, false otherwise.
         */
        public boolean isInEditing() {
            return mInEditing;
        }

        /**
         * Returns true if the TextView is in editing description, false otherwise.
         */
        public boolean isInEditingDescription() {
            return mInEditingDescription;
        }

        public View getEditingView() {
            if (mInEditing) {
                return mInEditingDescription ?  mDescriptionView : mTitleView;
            } else {
                return null;
            }
        }
    }

    private static String TAG = "GuidedActionsStylist";

    private ViewGroup mMainView;
    private VerticalGridView mActionsGridView;
    private View mBgView;
    private View mSelectorView;
    private View mContentView;
    private boolean mButtonActions;

    private Animator mSelectorAnimator;

    // Cached values from resources
    private float mEnabledTextAlpha;
    private float mDisabledTextAlpha;
    private float mEnabledDescriptionAlpha;
    private float mDisabledDescriptionAlpha;
    private float mEnabledChevronAlpha;
    private float mDisabledChevronAlpha;
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
        mMainView = (ViewGroup) inflater.inflate(onProvideLayoutId(), container, false);
        mContentView = mMainView.findViewById(R.id.guidedactions_content);
        mSelectorView = mMainView.findViewById(R.id.guidedactions_selector);
        mSelectorView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                    int oldLeft, int oldTop, int oldRight, int oldBottom) {
                updateSelectorView(false);
            }
        });
        mBgView = mMainView.findViewById(R.id.guidedactions_list_background);
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
                mActionsGridView.setOnScrollListener(new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                            if (mSelectorView.getAlpha() != 1f) {
                                updateSelectorView(true);
                            }
                        }
                    }
                });
            }
        }

        if (mSelectorView != null) {
            // ALlow focus to move to other views
            mActionsGridView.getViewTreeObserver().addOnGlobalFocusChangeListener(
                    mGlobalFocusChangeListener);
        }

        // Cache widths, chevron alpha values, max and min text lines, etc
        Context ctx = mMainView.getContext();
        TypedValue val = new TypedValue();
        mEnabledChevronAlpha = getFloat(ctx, val, R.attr.guidedActionEnabledChevronAlpha);
        mDisabledChevronAlpha = getFloat(ctx, val, R.attr.guidedActionDisabledChevronAlpha);
        mTitleMinLines = getInteger(ctx, val, R.attr.guidedActionTitleMinLines);
        mTitleMaxLines = getInteger(ctx, val, R.attr.guidedActionTitleMaxLines);
        mDescriptionMinLines = getInteger(ctx, val, R.attr.guidedActionDescriptionMinLines);
        mVerticalPadding = getDimension(ctx, val, R.attr.guidedActionVerticalPadding);
        mDisplayHeight = ((WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay().getHeight();

        mEnabledTextAlpha = Float.valueOf(ctx.getResources().getString(R.string
                .lb_guidedactions_item_unselected_text_alpha));
        mDisabledTextAlpha = Float.valueOf(ctx.getResources().getString(R.string
                .lb_guidedactions_item_disabled_text_alpha));
        mEnabledDescriptionAlpha = Float.valueOf(ctx.getResources().getString(R.string
                .lb_guidedactions_item_unselected_description_text_alpha));
        mDisabledDescriptionAlpha = Float.valueOf(ctx.getResources().getString(R.string
                .lb_guidedactions_item_disabled_description_text_alpha));
        return mMainView;
    }

    /**
     * Default implementation turns on background for actions and applies different Ids to views so
     * that GuidedStepFragment could run transitions against two action lists.  The method is called
     * by GuidedStepFragment, app may override this function when replacing default layout file
     * provided by {@link #onProvideLayoutId()}
     */
    public void setAsButtonActions() {
        mButtonActions = true;
        mMainView.setId(R.id.guidedactions_root2);
        ViewCompat.setTransitionName(mMainView, "guidedactions_root2");
        if (mActionsGridView != null) {
            mActionsGridView.setId(R.id.guidedactions_list2);
        }
        if (mSelectorView != null) {
            mSelectorView.setId(R.id.guidedactions_selector2);
            ViewCompat.setTransitionName(mSelectorView, "guidedactions_selector2");
        }
        if (mContentView != null) {
            mContentView.setId(R.id.guidedactions_content2);
            ViewCompat.setTransitionName(mContentView, "guidedactions_content2");
        }
        if (mBgView != null) {
            mBgView.setId(R.id.guidedactions_list_background2);
            ViewCompat.setTransitionName(mBgView, "guidedactions_list_background2");
            mBgView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Returns true if {@link #setAsButtonActions()} was called, false otherwise.
     * @return True if {@link #setAsButtonActions()} was called, false otherwise.
     */
    public boolean isButtonActions() {
        return mButtonActions;
    }

    final ViewTreeObserver.OnGlobalFocusChangeListener mGlobalFocusChangeListener =
            new ViewTreeObserver.OnGlobalFocusChangeListener() {

        @Override
        public void onGlobalFocusChanged(View oldFocus, View newFocus) {
            updateSelectorView(false);
        }
    };

    /**
     * Called when destroy the View created by GuidedActionsStylist.
     */
    public void onDestroyView() {
        if (mSelectorView != null) {
            mActionsGridView.getViewTreeObserver().removeOnGlobalFocusChangeListener(
                    mGlobalFocusChangeListener);
        }
        endSelectorAnimator();
        mActionsGridView = null;
        mSelectorView = null;
        mContentView = null;
        mBgView = null;
        mMainView = null;
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
     * Return view type of action, each different type can have differently associated layout Id.
     * Default implementation returns {@link #VIEW_TYPE_DEFAULT}.
     * @param action  The action object.
     * @return View type that used in {@link #onProvideItemLayoutId(int)}.
     */
    public int getItemViewType(GuidedAction action) {
        return VIEW_TYPE_DEFAULT;
    }

    /**
     * Provides the resource ID of the layout defining the view for an individual guided actions.
     * Subclasses may override to provide their own customized layouts. The base implementation
     * returns {@link android.support.v17.leanback.R.layout#lb_guidedactions_item}. If overridden,
     * the substituted layout should contain matching IDs for any views that should be managed by
     * the base class; this can be achieved by starting with a copy of the base layout file. Note
     * that in order for the item to support editing, the title view should both subclass {@link
     * android.widget.EditText} and implement {@link ImeKeyMonitor}; see {@link
     * GuidedActionEditText}.  To support different types of Layouts, override {@link
     * #onProvideItemLayoutId(int)}.
     * @return The resource ID of the layout to be inflated to define the view to display an
     * individual GuidedAction.
     */
    public int onProvideItemLayoutId() {
        return R.layout.lb_guidedactions_item;
    }

    /**
     * Provides the resource ID of the layout defining the view for an individual guided actions.
     * Subclasses may override to provide their own customized layouts. The base implementation
     * returns {@link android.support.v17.leanback.R.layout#lb_guidedactions_item}. If overridden,
     * the substituted layout should contain matching IDs for any views that should be managed by
     * the base class; this can be achieved by starting with a copy of the base layout file. Note
     * that in order for the item to support editing, the title view should both subclass {@link
     * android.widget.EditText} and implement {@link ImeKeyMonitor}; see {@link
     * GuidedActionEditText}.
     * @param viewType View type returned by {@link #getItemViewType(GuidedAction)}
     * @return The resource ID of the layout to be inflated to define the view to display an
     * individual GuidedAction.
     */
    public int onProvideItemLayoutId(int viewType) {
        if (viewType == VIEW_TYPE_DEFAULT) {
            return onProvideItemLayoutId();
        } else {
            throw new RuntimeException("ViewType " + viewType +
                    " not supported in GuidedActionsStylist");
        }
    }

    /**
     * Constructs a {@link ViewHolder} capable of representing {@link GuidedAction}s. Subclasses
     * may choose to return a subclass of ViewHolder.  To support different view types, override
     * {@link #onCreateViewHolder(ViewGroup, int)}
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
     * Constructs a {@link ViewHolder} capable of representing {@link GuidedAction}s. Subclasses
     * may choose to return a subclass of ViewHolder.
     * <p>
     * <i>Note: Should not actually add the created view to the parent; the caller will do
     * this.</i>
     * @param parent The view group to be used as the parent of the new view.
     * @param viewType The viewType returned by {@link #getItemViewType(GuidedAction)}
     * @return The view to be added to the caller's view hierarchy.
     */
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_DEFAULT) {
            return onCreateViewHolder(parent);
        }
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View v = inflater.inflate(onProvideItemLayoutId(viewType), parent, false);
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
            vh.mTitleView.setAlpha(action.isEnabled() ? mEnabledTextAlpha : mDisabledTextAlpha);
            vh.mTitleView.setFocusable(action.isEditable());
        }
        if (vh.mDescriptionView != null) {
            vh.mDescriptionView.setText(action.getDescription());
            vh.mDescriptionView.setVisibility(TextUtils.isEmpty(action.getDescription()) ?
                    View.GONE : View.VISIBLE);
            vh.mDescriptionView.setAlpha(action.isEnabled() ? mEnabledDescriptionAlpha :
                mDisabledDescriptionAlpha);
            vh.mDescriptionView.setFocusable(action.isDescriptionEditable());
        }
        // Clients might want the check mark view to be gone entirely, in which case, ignore it.
        if (vh.mCheckmarkView != null) {
            onBindCheckMarkView(vh, action);
        }

        if (vh.mChevronView != null) {
            onBindChevronView(vh, action);
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
        setEditingMode(vh, action, false);
        if (action.isFocusable()) {
            vh.view.setFocusable(true);
            if (vh.view instanceof ViewGroup) {
                ((ViewGroup) vh.view).setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
            }
        } else {
            vh.view.setFocusable(false);
            if (vh.view instanceof ViewGroup) {
                ((ViewGroup) vh.view).setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
            }
        }
        setupImeOptions(vh, action);
    }

    /**
     * Called by {@link #onBindViewHolder(ViewHolder, GuidedAction)} to setup IME options.  Default
     * implementation assigns {@link EditorInfo#IME_ACTION_DONE}.  Subclass may override.
     * @param vh The view holder to be associated with the given action.
     * @param action The guided action to be displayed by the view holder's view.
     */
    protected void setupImeOptions(ViewHolder vh, GuidedAction action) {
        setupNextImeOptions(vh.getEditableTitleView());
        setupNextImeOptions(vh.getEditableDescriptionView());
    }

    private void setupNextImeOptions(EditText edit) {
        if (edit != null) {
            edit.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        }
    }

    public void setEditingMode(ViewHolder vh, GuidedAction action, boolean editing) {
        if (editing != vh.mInEditing) {
            vh.mInEditing = editing;
            onEditingModeChange(vh, action, editing);
        }
    }

    protected void onEditingModeChange(ViewHolder vh, GuidedAction action, boolean editing) {
        TextView titleView = vh.getTitleView();
        TextView descriptionView = vh.getDescriptionView();
        if (editing) {
            CharSequence editTitle = action.getEditTitle();
            if (titleView != null && editTitle != null) {
                titleView.setText(editTitle);
            }
            CharSequence editDescription = action.getEditDescription();
            if (descriptionView != null && editDescription != null) {
                descriptionView.setText(editDescription);
            }
            if (action.isDescriptionEditable()) {
                if (descriptionView != null) {
                    descriptionView.setVisibility(View.VISIBLE);
                    descriptionView.setInputType(action.getDescriptionEditInputType());
                }
                vh.mInEditingDescription = true;
            } else {
                vh.mInEditingDescription = false;
                if (titleView != null) {
                    titleView.setInputType(action.getEditInputType());
                }
            }
        } else {
            if (titleView != null) {
                titleView.setText(action.getTitle());
            }
            if (descriptionView != null) {
                descriptionView.setText(action.getDescription());
            }
            if (vh.mInEditingDescription) {
                if (descriptionView != null) {
                    descriptionView.setVisibility(TextUtils.isEmpty(action.getDescription()) ?
                            View.GONE : View.VISIBLE);
                    descriptionView.setInputType(action.getDescriptionInputType());
                }
                vh.mInEditingDescription = false;
            } else {
                if (titleView != null) {
                    titleView.setInputType(action.getInputType());
                }
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
     * Resets the view holder's view to unpressed state.
     * @param vh The view holder associated with the relevant action.
     */
    public void onAnimateItemPressedCancelled(ViewHolder vh) {
        createAnimator(vh.view, R.attr.guidedActionUnpressedAnimation).end();
    }

    /**
     * Animates the view holder's view (or subviews thereof) when the action has had its check state
     * changed. Default implementation calls setChecked() if {@link ViewHolder#getCheckmarkView()}
     * is instance of {@link Checkable}.
     *
     * @param vh The view holder associated with the relevant action.
     * @param checked True if the action has become checked, false if it has become unchecked.
     * @see #onBindCheckMarkView(ViewHolder, GuidedAction)
     */
    public void onAnimateItemChecked(ViewHolder vh, boolean checked) {
        if (vh.mCheckmarkView instanceof Checkable) {
            ((Checkable) vh.mCheckmarkView).setChecked(checked);
        }
    }

    /**
     * Sets states of check mark view, called by {@link #onBindViewHolder(ViewHolder, GuidedAction)}
     * when action's checkset Id is other than {@link GuidedAction#NO_CHECK_SET}. Default
     * implementation assigns drawable loaded from theme attribute
     * {@link android.R.attr#listChoiceIndicatorMultiple} for checkbox or
     * {@link android.R.attr#listChoiceIndicatorSingle} for radio button. Subclass rarely needs
     * override the method, instead app can provide its own drawable that supports transition
     * animations, change theme attributes {@link android.R.attr#listChoiceIndicatorMultiple} and
     * {@link android.R.attr#listChoiceIndicatorSingle} in {android.support.v17.leanback.R.
     * styleable#LeanbackGuidedStepTheme}.
     *
     * @param vh The view holder associated with the relevant action.
     * @param action The GuidedAction object to bind to.
     * @see #onAnimateItemChecked(ViewHolder, boolean)
     */
    public void onBindCheckMarkView(ViewHolder vh, GuidedAction action) {
        if (action.getCheckSetId() != GuidedAction.NO_CHECK_SET) {
            vh.mCheckmarkView.setVisibility(View.VISIBLE);
            int attrId = action.getCheckSetId() == GuidedAction.CHECKBOX_CHECK_SET_ID ?
                    android.R.attr.listChoiceIndicatorMultiple :
                    android.R.attr.listChoiceIndicatorSingle;
            final Context context = vh.mCheckmarkView.getContext();
            Drawable drawable = null;
            TypedValue typedValue = new TypedValue();
            if (context.getTheme().resolveAttribute(attrId, typedValue, true)) {
                drawable = ContextCompat.getDrawable(context, typedValue.resourceId);
            }
            vh.mCheckmarkView.setImageDrawable(drawable);
            if (vh.mCheckmarkView instanceof Checkable) {
                ((Checkable) vh.mCheckmarkView).setChecked(action.isChecked());
            }
        } else {
            vh.mCheckmarkView.setVisibility(View.GONE);
        }
    }

    /**
     * Sets states of chevron view, called by {@link #onBindViewHolder(ViewHolder, GuidedAction)}.
     * Subclass may override.
     *
     * @param vh The view holder associated with the relevant action.
     * @param action The GuidedAction object to bind to.
     */
    public void onBindChevronView(ViewHolder vh, GuidedAction action) {
        vh.mChevronView.setVisibility(action.hasNext() ? View.VISIBLE : View.GONE);
        vh.mChevronView.setAlpha(action.isEnabled() ? mEnabledChevronAlpha :
                mDisabledChevronAlpha);
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
    public void onImeAppearing(@NonNull List<Animator> animators) {
        animators.add(createAnimator(mContentView, R.attr.guidedStepImeAppearingAnimation));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onImeDisappearing(@NonNull List<Animator> animators) {
        animators.add(createAnimator(mContentView, R.attr.guidedStepImeDisappearingAnimation));
    }

    /*
     * ==========================================
     * Private methods
     * ==========================================
     */

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

    private void endSelectorAnimator() {
        if (mSelectorAnimator != null) {
            mSelectorAnimator.end();
            mSelectorAnimator = null;
        }
    }

    private void updateSelectorView(boolean animate) {
        if (mActionsGridView == null || mSelectorView == null || mSelectorView.getHeight() <= 0) {
            return;
        }
        final View focusedChild = mActionsGridView.getFocusedChild();
        endSelectorAnimator();
        if (focusedChild == null || !mActionsGridView.hasFocus()
                || mActionsGridView.getScrollState() != RecyclerView.SCROLL_STATE_IDLE) {
            if (animate) {
                mSelectorAnimator = createAnimator(mSelectorView,
                        R.attr.guidedActionsSelectorHideAnimation);
                mSelectorAnimator.start();
            } else {
                mSelectorView.setAlpha(0f);
            }
        } else {
            final float scaleY = (float) focusedChild.getHeight() / mSelectorView.getHeight();
            Rect r = new Rect(0, 0, focusedChild.getWidth(), focusedChild.getHeight());
            mMainView.offsetDescendantRectToMyCoords(focusedChild, r);
            mMainView.offsetRectIntoDescendantCoords(mSelectorView, r);
            mSelectorView.setTranslationY(r.exactCenterY() - mSelectorView.getHeight() * 0.5f);
            if (animate) {
                mSelectorAnimator = createAnimator(mSelectorView,
                        R.attr.guidedActionsSelectorShowAnimation);
                ((ObjectAnimator) ((AnimatorSet) mSelectorAnimator).getChildAnimations().get(1))
                        .setFloatValues(scaleY);
                mSelectorAnimator.start();
            } else {
                mSelectorView.setAlpha(1f);
                mSelectorView.setScaleY(scaleY);
            }
        }
    }

}
