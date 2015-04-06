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
package android.support.v17.leanback.app;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v17.leanback.animation.UntargetableAnimatorSet;
import android.support.v17.leanback.R;
import android.support.v17.leanback.widget.GuidanceStylist;
import android.support.v17.leanback.widget.GuidanceStylist.Guidance;
import android.support.v17.leanback.widget.GuidedAction;
import android.support.v17.leanback.widget.GuidedActionsStylist;
import android.support.v17.leanback.widget.VerticalGridView;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * A GuidedStepFragment is used to guide the user through a decision or series of decisions.
 * It is composed of a guidance view on the left and a view on the right containing a list of
 * possible actions.
 * <p>
 * <h3>Basic Usage</h3>
 * <p>
 * Clients of GuidedStepFragment typically create a custom subclass to attach to their Activities.
 * This custom subclass provides the information necessary to construct the user interface and
 * respond to user actions. At a minimum, subclasses should override:
 * <ul>
 * <li>{@link #onCreateGuidance}, to provide instructions to the user</li>
 * <li>{@link #onCreateActions}, to provide a set of {@link GuidedAction}s the user can take</li>
 * <li>{@link #onGuidedActionClicked}, to respond to those actions</li>
 * </ul>
 * <p>
 * <h3>Theming and Stylists</h3>
 * <p>
 * GuidedStepFragment delegates its visual styling to classes called stylists. The {@link
 * GuidanceStylist} is responsible for the left guidance view, while the {@link
 * GuidedActionsStylist} is responsible for the right actions view. The stylists use theme
 * attributes to derive values associated with the presentation, such as colors, animations, etc.
 * Most simple visual aspects of GuidanceStylist and GuidedActionsStylist can be customized
 * via theming; see their documentation for more information.
 * <p>
 * GuidedStepFragments must have access to an appropriate theme in order for the stylists to
 * function properly.  Specifically, the fragment must receive {@link
 * android.support.v17.leanback.R.style#Theme_Leanback_GuidedStep}, or a theme whose parent is
 * is set to that theme. Themes can be provided in one of three ways:
 * <ul>
 * <li>The simplest way is to set the theme for the host Activity to the GuidedStep theme or a
 * theme that derives from it.</li>
 * <li>If the Activity already has a theme and setting its parent theme is inconvenient, the
 * existing Activity theme can have an entry added for the attribute {@link
 * android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedStepTheme}. If present,
 * this theme will be used by GuidedStepFragment as an overlay to the Activity's theme.</li>
 * <li>Finally, custom subclasses of GuidedStepFragment may provide a theme through the {@link
 * #onProvideTheme} method. This can be useful if a subclass is used across multiple
 * Activities.</li>
 * </ul>
 * <p>
 * If the theme is provided in multiple ways, the onProvideTheme override has priority, followed by
 * the Activty's theme.  (Themes whose parent theme is already set to the guided step theme do not
 * need to set the guidedStepTheme attribute; if set, it will be ignored.)
 * <p>
 * If themes do not provide enough customizability, the stylists themselves may be subclassed and
 * provided to the GuidedStepFragment through the {@link #onCreateGuidanceStylist} and {@link
 * #onCreateActionsStylist} methods.  The stylists have simple hooks so that subclasses
 * may override layout files; subclasses may also have more complex logic to determine styling.
 * <p>
 * <h3>Guided sequences</h3>
 * <p>
 * GuidedStepFragments can be grouped together to provide a guided sequence. GuidedStepFragments
 * grouped as a sequence use custom animations provided by {@link GuidanceStylist} and
 * {@link GuidedActionsStylist} (or subclasses) during transitions between steps. Clients
 * should use {@link #add} to place subsequent GuidedFragments onto the fragment stack so that
 * custom animations are properly configured. (Custom animations are triggered automatically when
 * the fragment stack is subsequently popped by any normal mechanism.)
 * <p>
 * <i>Note: Currently GuidedStepFragments grouped in this way must all be defined programmatically,
 * rather than in XML. This restriction may be removed in the future.</i>
 * <p>
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedStepTheme
 * @see GuidanceStylist
 * @see GuidanceStylist.Guidance
 * @see GuidedAction
 * @see GuidedActionsStylist
 */
public class GuidedStepFragment extends Fragment implements GuidedActionAdapter.ClickListener,
        GuidedActionAdapter.FocusListener {

    private static final String TAG_LEAN_BACK_ACTIONS_FRAGMENT = "leanBackGuidedStepFragment";
    private static final String EXTRA_ACTION_SELECTED_INDEX = "selectedIndex";
    private static final String EXTRA_ACTION_ENTRY_TRANSITION_ENABLED = "entryTransitionEnabled";
    private static final String EXTRA_ENTRY_TRANSITION_PERFORMED = "entryTransitionPerformed";
    private static final String TAG = "GuidedStepFragment";
    private static final boolean DEBUG = true;
    private static final int ANIMATION_FRAGMENT_ENTER = 1;
    private static final int ANIMATION_FRAGMENT_EXIT = 2;
    private static final int ANIMATION_FRAGMENT_ENTER_POP = 3;
    private static final int ANIMATION_FRAGMENT_EXIT_POP = 4;

    private int mTheme;
    private GuidanceStylist mGuidanceStylist;
    private GuidedActionsStylist mActionsStylist;
    private GuidedActionAdapter mAdapter;
    private VerticalGridView mListView;
    private List<GuidedAction> mActions = new ArrayList<GuidedAction>();
    private int mSelectedIndex = -1;
    private boolean mEntryTransitionPerformed;
    private boolean mEntryTransitionEnabled = true;

    public GuidedStepFragment() {
        // We need to supply the theme before any potential call to onInflate in order
        // for the defaulting to work properly.
        mTheme = onProvideTheme();
        mGuidanceStylist = onCreateGuidanceStylist();
        mActionsStylist = onCreateActionsStylist();
    }

    /**
     * Creates the presenter used to style the guidance panel. The default implementation returns
     * a basic GuidanceStylist.
     * @return The GuidanceStylist used in this fragment.
     */
    public GuidanceStylist onCreateGuidanceStylist() {
        return new GuidanceStylist();
    }

    /**
     * Creates the presenter used to style the guided actions panel. The default implementation
     * returns a basic GuidedActionsStylist.
     * @return The GuidedActionsStylist used in this fragment.
     */
    public GuidedActionsStylist onCreateActionsStylist() {
        return new GuidedActionsStylist();
    }

    /**
     * Returns the theme used for styling the fragment. The default returns -1, indicating that the
     * host Activity's theme should be used.
     * @return The theme resource ID of the theme to use in this fragment, or -1 to use the
     * host Activity's theme.
     */
    public int onProvideTheme() {
        return -1;
    }

    /**
     * Returns the information required to provide guidance to the user. This hook is called during
     * {@link #onCreateView}.  May be overridden to return a custom subclass of {@link
     * GuidanceStylist.Guidance} for use in a subclass of {@link GuidanceStylist}. The default
     * returns a Guidance object with empty fields; subclasses should override.
     * @param savedInstanceState The saved instance state from onCreateView.
     * @return The Guidance object representing the information used to guide the user.
     */
    public @NonNull Guidance onCreateGuidance(Bundle savedInstanceState) {
        return new Guidance("", "", "", null);
    }

    /**
     * Fills out the set of actions available to the user. This hook is called during {@link
     * #onCreate}. The default leaves the list of actions empty; subclasses should override.
     * @param actions A non-null, empty list ready to be populated.
     * @param savedInstanceState The saved instance state from onCreate.
     */
    public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
    }

    /**
     * Callback invoked when an action is taken by the user. Subclasses should override in
     * order to act on the user's decisions.
     * @param action The chosen action.
     */
    @Override
    public void onGuidedActionClicked(GuidedAction action) {
    }

    /**
     * Callback invoked when an action is focused (made to be the current selection) by the user.
     */
    @Override
    public void onGuidedActionFocused(GuidedAction action) {
    }

    /**
     * Adds the specified GuidedStepFragment to the fragment stack, replacing any existing
     * GuidedStepFragments in the stack, and configuring the fragment-to-fragment custom animations.
     * <p>
     * Note: currently fragments added using this method must be created programmatically rather
     * than via XML.
     * @param fragmentManager The FragmentManager to be used in the transaction.
     * @param fragment The GuidedStepFragment to be inserted into the fragment stack.
     * @return The ID returned by the call FragmentTransaction.replace.
     */
    public static int add(FragmentManager fragmentManager, GuidedStepFragment fragment) {
        return add(fragmentManager, fragment, android.R.id.content);
    }

    // Note, this method used to be public, but I haven't found a good way for a client
    // to specify an id.
    private static int add(FragmentManager fm, GuidedStepFragment f, int id) {
        boolean inGuidedStep = getCurrentGuidedStepFragment(fm) != null;
        FragmentTransaction ft = fm.beginTransaction();

        if (inGuidedStep) {
            ft.setCustomAnimations(ANIMATION_FRAGMENT_ENTER,
                    ANIMATION_FRAGMENT_EXIT, ANIMATION_FRAGMENT_ENTER_POP,
                    ANIMATION_FRAGMENT_EXIT_POP);
            ft.addToBackStack(null);
        }
        return ft.replace(id, f, TAG_LEAN_BACK_ACTIONS_FRAGMENT).commit();
    }

    /**
     * Returns the current GuidedStepFragment on the fragment transaction stack.
     * @return The current GuidedStepFragment, if any, on the fragment transaction stack.
     */
    public static GuidedStepFragment getCurrentGuidedStepFragment(FragmentManager fm) {
        Fragment f = fm.findFragmentByTag(TAG_LEAN_BACK_ACTIONS_FRAGMENT);
        if (f instanceof GuidedStepFragment) {
            return (GuidedStepFragment) f;
        }
        return null;
    }

    /**
     * Returns the GuidanceStylist that displays guidance information for the user.
     * @return The GuidanceStylist for this fragment.
     */
    public GuidanceStylist getGuidanceStylist() {
        return mGuidanceStylist;
    }

    /**
     * Returns the GuidedActionsStylist that displays the actions the user may take.
     * @return The GuidedActionsStylist for this fragment.
     */
    public GuidedActionsStylist getGuidedActionsStylist() {
        return mActionsStylist;
    }

    /**
     * Returns the list of GuidedActions that the user may take in this fragment.
     * @return The list of GuidedActions for this fragment.
     */
    public List<GuidedAction> getActions() {
        return mActions;
    }

    /**
     * Sets the list of GuidedActions that the user may take in this fragment.
     * @param actions The list of GuidedActions for this fragment.
     */
    public void setActions(List<GuidedAction> actions) {
        mActions = actions;
        if (mAdapter != null) {
            mAdapter.setActions(mActions);
        }
    }

    /**
     * Returns the view corresponding to the action at the indicated position in the list of
     * actions for this fragment.
     * @param position The integer position of the action of interest.
     * @return The View corresponding to the action at the indicated position, or null if that
     * action is not currently onscreen.
     */
    public View getActionItemView(int position) {
        return mListView.findViewHolderForPosition(position).itemView;
    }

    /**
     * Scrolls the action list to the position indicated, selecting that action's view.
     * @param position The integer position of the action of interest.
     */
    public void setSelectedActionPosition(int position) {
        mListView.setSelectedPosition(position);
    }

    /**
     * Returns the position if the currently selected GuidedAction.
     * @return position The integer position of the currently selected action.
     */
    public int getSelectedActionPosition() {
        return mListView.getSelectedPosition();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DEBUG) Log.v(TAG, "onCreate");
        Bundle state = (savedInstanceState != null) ? savedInstanceState : getArguments();
        if (state != null) {
            if (mSelectedIndex == -1) {
                mSelectedIndex = state.getInt(EXTRA_ACTION_SELECTED_INDEX, -1);
            }
            mEntryTransitionEnabled = state.getBoolean(EXTRA_ACTION_ENTRY_TRANSITION_ENABLED, true);
            mEntryTransitionPerformed = state.getBoolean(EXTRA_ENTRY_TRANSITION_PERFORMED, false);
        }
        mActions.clear();
        onCreateActions(mActions, savedInstanceState);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        if (DEBUG) Log.v(TAG, "onCreateView");

        resolveTheme();
        inflater = getThemeInflater(inflater);

        View v = inflater.inflate(R.layout.lb_guidedstep_fragment, container, false);
        ViewGroup guidanceContainer = (ViewGroup) v.findViewById(R.id.content_fragment);
        ViewGroup actionContainer = (ViewGroup) v.findViewById(R.id.action_fragment);

        Guidance guidance = onCreateGuidance(savedInstanceState);
        View guidanceView = mGuidanceStylist.onCreateView(inflater, guidanceContainer, guidance);
        guidanceContainer.addView(guidanceView);

        View actionsView = mActionsStylist.onCreateView(inflater, actionContainer);
        actionContainer.addView(actionsView);

        mAdapter = new GuidedActionAdapter(mActions, this, this, mActionsStylist);

        mListView = mActionsStylist.getActionsGridView();
        mListView.setAdapter(mAdapter);
        int pos = (mSelectedIndex >= 0 && mSelectedIndex < mActions.size()) ?
                mSelectedIndex : getFirstCheckedAction();
        mListView.setSelectedPosition(pos);

        return v;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(EXTRA_ACTION_SELECTED_INDEX,
                (mListView != null) ? getSelectedActionPosition() : mSelectedIndex);
        outState.putBoolean(EXTRA_ACTION_ENTRY_TRANSITION_ENABLED, mEntryTransitionEnabled);
        outState.putBoolean(EXTRA_ENTRY_TRANSITION_PERFORMED, mEntryTransitionPerformed);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStart() {
        if (DEBUG) Log.v(TAG, "onStart");
        super.onStart();
        if (isEntryTransitionEnabled() && !mEntryTransitionPerformed) {
            mEntryTransitionPerformed = true;
            performEntryTransition();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Animator onCreateAnimator(int transit, boolean enter, int nextAnim) {
        if (DEBUG) Log.v(TAG, "onCreateAnimator: " + transit + " " + enter + " " + nextAnim);
        View mainView = getView();

        ArrayList<Animator> animators = new ArrayList<Animator>();
        switch (nextAnim) {
            case ANIMATION_FRAGMENT_ENTER:
                mGuidanceStylist.onFragmentEnter(animators);
                mActionsStylist.onFragmentEnter(animators);
                break;
            case ANIMATION_FRAGMENT_EXIT:
                mGuidanceStylist.onFragmentExit(animators);
                mActionsStylist.onFragmentExit(animators);
                break;
            case ANIMATION_FRAGMENT_ENTER_POP:
                mGuidanceStylist.onFragmentReenter(animators);
                mActionsStylist.onFragmentReenter(animators);
                break;
            case ANIMATION_FRAGMENT_EXIT_POP:
                mGuidanceStylist.onFragmentReturn(animators);
                mActionsStylist.onFragmentReturn(animators);
                break;
            default:
                return super.onCreateAnimator(transit, enter, nextAnim);
        }

        mEntryTransitionPerformed = true;
        return createDummyAnimator(mainView, animators);
    }

    /**
     * Returns whether entry transitions are enabled for this fragment.
     * @return Whether entry transitions are enabled for this fragment.
     */
    protected boolean isEntryTransitionEnabled() {
        return mEntryTransitionEnabled;
    }

    /**
     * Sets whether entry transitions are enabled for this fragment.
     * @param enabled Whether to enable entry transitions for this fragment.
     */
    protected void setEntryTransitionEnabled(boolean enabled) {
        mEntryTransitionEnabled = enabled;
    }

    private boolean isGuidedStepTheme(Context context) {
        int resId = R.attr.guidedStepThemeFlag;
        TypedValue typedValue = new TypedValue();
        boolean found = context.getTheme().resolveAttribute(resId, typedValue, true);
        if (DEBUG) Log.v(TAG, "Found guided step theme flag? " + found);
        return found && typedValue.type == TypedValue.TYPE_INT_BOOLEAN && typedValue.data != 0;
    }

    private void resolveTheme() {
        boolean hasThemeReference = true;
        // Look up the guidedStepTheme in the currently specified theme.  If it exists,
        // replace the theme with its value.
        Activity activity = getActivity();
        if (mTheme == -1 && !isGuidedStepTheme(activity)) {
            // Look up the guidedStepTheme in the activity's currently specified theme.  If it
            // exists, replace the theme with its value.
            int resId = R.attr.guidedStepTheme;
            TypedValue typedValue = new TypedValue();
            boolean found = activity.getTheme().resolveAttribute(resId, typedValue, true);
            if (DEBUG) Log.v(TAG, "Found guided step theme reference? " + found);
            if (found) {
                if (isGuidedStepTheme(new ContextThemeWrapper(activity, typedValue.resourceId))) {
                    mTheme = typedValue.resourceId;
                } else {
                    found = false;
                }
            }
            if (!found) {
                Log.e(TAG, "GuidedStepFragment does not have an appropriate theme set.");
            }
        }
    }

    private LayoutInflater getThemeInflater(LayoutInflater inflater) {
        if (mTheme == -1) {
            return inflater;
        } else {
            Context ctw = new ContextThemeWrapper(getActivity(), mTheme);
            return inflater.cloneInContext(ctw);
        }
    }

    private int getFirstCheckedAction() {
        for (int i = 0, size = mActions.size(); i < size; i++) {
            if (mActions.get(i).isChecked()) {
                return i;
            }
        }
        return 0;
    }

    private void performEntryTransition() {
        if (DEBUG) Log.v(TAG, "performEntryTransition");
        final View mainView = getView();

        mainView.setVisibility(View.INVISIBLE);

        ArrayList<Animator> animators = new ArrayList<Animator>();
        mGuidanceStylist.onActivityEnter(animators);
        mActionsStylist.onActivityEnter(animators);

        final Animator animator = createDummyAnimator(mainView, animators);

        // We need to defer the animation until the first layout has occurred, as we don't yet
        // know the final locations of views.
        mainView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        mainView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        if (!isAdded()) {
                            // We have been detached before this could run,
                            // so just bail
                            return;
                        }

                        mainView.setVisibility(View.VISIBLE);
                        animator.start();
                    }
                });
    }

    private Animator createDummyAnimator(final View v, ArrayList<Animator> animators) {
        final AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(animators);
        return new UntargetableAnimatorSet(animatorSet);
    }

}
