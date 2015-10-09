/* This file is auto-generated from GuidedStepFragment.java.  DO NOT MODIFY. */

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
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v17.leanback.transition.TransitionHelper;
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
import android.view.Gravity;
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
 * A GuidedStepSupportFragment is used to guide the user through a decision or series of decisions.
 * It is composed of a guidance view on the left and a view on the right containing a list of
 * possible actions.
 * <p>
 * <h3>Basic Usage</h3>
 * <p>
 * Clients of GuidedStepSupportFragment must create a custom subclass to attach to their Activities.
 * This custom subclass provides the information necessary to construct the user interface and
 * respond to user actions. At a minimum, subclasses should override:
 * <ul>
 * <li>{@link #onCreateGuidance}, to provide instructions to the user</li>
 * <li>{@link #onCreateActions}, to provide a set of {@link GuidedAction}s the user can take</li>
 * <li>{@link #onGuidedActionClicked}, to respond to those actions</li>
 * </ul>
 * <p>
 * Clients use following helper functions to add GuidedStepSupportFragment to Activity or FragmentManager:
 * <ul>
 * <li>{@link #addAsRoot(FragmentActivity, GuidedStepSupportFragment, int)}, to be called during Activity onCreate,
 * adds GuidedStepSupportFragment as the first Fragment in activity.</li>
 * <li>{@link #add(FragmentManager, GuidedStepSupportFragment)} or {@link #add(FragmentManager,
 * GuidedStepSupportFragment, int)}, to add GuidedStepSupportFragment on top of existing Fragments or
 * replacing existing GuidedStepSupportFragment when moving forward to next step.</li>
 * </ul>
 * <h3>Theming and Stylists</h3>
 * <p>
 * GuidedStepSupportFragment delegates its visual styling to classes called stylists. The {@link
 * GuidanceStylist} is responsible for the left guidance view, while the {@link
 * GuidedActionsStylist} is responsible for the right actions view. The stylists use theme
 * attributes to derive values associated with the presentation, such as colors, animations, etc.
 * Most simple visual aspects of GuidanceStylist and GuidedActionsStylist can be customized
 * via theming; see their documentation for more information.
 * <p>
 * GuidedStepSupportFragments must have access to an appropriate theme in order for the stylists to
 * function properly.  Specifically, the fragment must receive {@link
 * android.support.v17.leanback.R.style#Theme_Leanback_GuidedStep}, or a theme whose parent is
 * is set to that theme. Themes can be provided in one of three ways:
 * <ul>
 * <li>The simplest way is to set the theme for the host Activity to the GuidedStep theme or a
 * theme that derives from it.</li>
 * <li>If the Activity already has a theme and setting its parent theme is inconvenient, the
 * existing Activity theme can have an entry added for the attribute {@link
 * android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedStepTheme}. If present,
 * this theme will be used by GuidedStepSupportFragment as an overlay to the Activity's theme.</li>
 * <li>Finally, custom subclasses of GuidedStepSupportFragment may provide a theme through the {@link
 * #onProvideTheme} method. This can be useful if a subclass is used across multiple
 * Activities.</li>
 * </ul>
 * <p>
 * If the theme is provided in multiple ways, the onProvideTheme override has priority, followed by
 * the Activty's theme.  (Themes whose parent theme is already set to the guided step theme do not
 * need to set the guidedStepTheme attribute; if set, it will be ignored.)
 * <p>
 * If themes do not provide enough customizability, the stylists themselves may be subclassed and
 * provided to the GuidedStepSupportFragment through the {@link #onCreateGuidanceStylist} and {@link
 * #onCreateActionsStylist} methods.  The stylists have simple hooks so that subclasses
 * may override layout files; subclasses may also have more complex logic to determine styling.
 * <p>
 * <h3>Guided sequences</h3>
 * <p>
 * GuidedStepSupportFragments can be grouped together to provide a guided sequence. GuidedStepSupportFragments
 * grouped as a sequence use custom animations provided by {@link GuidanceStylist} and
 * {@link GuidedActionsStylist} (or subclasses) during transitions between steps. Clients
 * should use {@link #add} to place subsequent GuidedFragments onto the fragment stack so that
 * custom animations are properly configured. (Custom animations are triggered automatically when
 * the fragment stack is subsequently popped by any normal mechanism.)
 * <p>
 * <i>Note: Currently GuidedStepSupportFragments grouped in this way must all be defined programmatically,
 * rather than in XML. This restriction may be removed in the future.</i>
 *
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedStepTheme
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedStepBackground
 * @see GuidanceStylist
 * @see GuidanceStylist.Guidance
 * @see GuidedAction
 * @see GuidedActionsStylist
 */
public class GuidedStepSupportFragment extends Fragment implements GuidedActionAdapter.ClickListener,
        GuidedActionAdapter.FocusListener {

    private static final String TAG_LEAN_BACK_ACTIONS_FRAGMENT = "leanBackGuidedStepSupportFragment";
    private static final String EXTRA_ACTION_SELECTED_INDEX = "selectedIndex";

    private static final boolean IS_FRAMEWORK_FRAGMENT = false;

    /**
     * Fragment argument name for UI style.  The argument value is persisted in fragment state.
     * The value is initially {@link #UI_STYLE_DEFAULT} and might be changed in one of the three
     * helper functions:
     * <ul>
     * <li>{@link #addAsRoot(FragmentActivity, GuidedStepSupportFragment, int)}</li>
     * <li>{@link #add(FragmentManager, GuidedStepSupportFragment)} or {@link #add(FragmentManager,
     * GuidedStepSupportFragment, int)}</li>
     * </ul>
     * <p>
     * Argument value can be either:
     * <ul>
     * <li>{@link #UI_STYLE_DEFAULT}</li>
     * <li>{@link #UI_STYLE_ENTRANCE}</li>
     * <li>{@link #UI_STYLE_ACTIVITY_ROOT}</li>
     * </ul>
     */
    public static final String EXTRA_UI_STYLE = "uiStyle";

    /**
     * Default value for argument {@link #EXTRA_UI_STYLE}.  The default value is assigned
     * in GuidedStepSupportFragment constructor.  This is the case that we use GuidedStepSupportFragment to
     * replace another existing GuidedStepSupportFragment when moving forward to next step. Default
     * behavior of this style is:
     * <ul>
     * <li> Enter transition slides in from END(right), exit transition slide out to START(left).
     * </li>
     * <li> No background, see {@link #onProvideBackgroundSupportFragment()}.</li>
     * </ul>
     */
    public static final int UI_STYLE_DEFAULT = 0;

    /**
     * One possible value of argument {@link #EXTRA_UI_STYLE}.  This is the case that we show
     * GuidedStepSupportFragment on top of other content.  The default behavior of this style:
     * <ul>
     * <li>Enter transition slides in from two sides, exit transition is inherited from
     * {@link #UI_STYLE_DEFAULT}.  Note: Changing exit transition by UI style is not working because
     * fragment transition asks for exit transition before UI style is restored in Fragment
     * .onCreate().</li>
     * <li> {@link #onProvideBackgroundSupportFragment()} will create {@link GuidedStepBackgroundSupportFragment}
     * to covering underneath content. The activity must provide a container to host background
     * fragment and override {@link #getContainerIdForBackground()}</li>
     * </ul>
     */
    public static final int UI_STYLE_ENTRANCE = 1;

    /**
     * One possible value of argument {@link #EXTRA_UI_STYLE}.  This is the case that we show first
     * GuidedStepSupportFragment in a separate activity.  The default behavior of this style:
     * <ul>
     * <li> Enter transition is assigned null (will rely on activity transition), exit transition is
     * same as {@link #UI_STYLE_DEFAULT}.  Note: Changing exit transition by UI style is not working
     * because fragment transition asks for exit transition before UI style is restored in
     * Fragment.onCreate().</li>
     * <li> No background, see {@link #onProvideBackgroundSupportFragment()}.
     * </ul>
     */
    public static final int UI_STYLE_ACTIVITY_ROOT = 2;

    private static final String TAG = "GuidedStepSupportFragment";
    private static final boolean DEBUG = false;

    private int mTheme;
    private ContextThemeWrapper mThemeWrapper;
    private GuidanceStylist mGuidanceStylist;
    private GuidedActionsStylist mActionsStylist;
    private GuidedActionAdapter mAdapter;
    private VerticalGridView mListView;
    private List<GuidedAction> mActions = new ArrayList<GuidedAction>();
    private int mSelectedIndex = -1;

    public GuidedStepSupportFragment() {
        // We need to supply the theme before any potential call to onInflate in order
        // for the defaulting to work properly.
        mTheme = onProvideTheme();
        mGuidanceStylist = onCreateGuidanceStylist();
        mActionsStylist = onCreateActionsStylist();
        onProvideFragmentTransitions();
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
     * Callback invoked when an action's title has been edited.
     */
    public void onGuidedActionEdited(GuidedAction action) {
    }

    /**
     * Adds the specified GuidedStepSupportFragment to the fragment stack, replacing any existing
     * GuidedStepSupportFragments in the stack, and configuring the fragment-to-fragment custom
     * transitions.  A backstack entry is added, so the fragment will be dismissed when BACK key
     * is pressed.
     * <li>If current fragment on stack is GuidedStepSupportFragment: assign {@link #UI_STYLE_DEFAULT}
     * <li>If current fragment on stack is not GuidedStepSupportFragment: assign {@link #UI_STYLE_ENTRANCE}
     * <p>
     * Note: currently fragments added using this method must be created programmatically rather
     * than via XML.
     * @param fragmentManager The FragmentManager to be used in the transaction.
     * @param fragment The GuidedStepSupportFragment to be inserted into the fragment stack.
     * @return The ID returned by the call FragmentTransaction.replace.
     */
    public static int add(FragmentManager fragmentManager, GuidedStepSupportFragment fragment) {
        return add(fragmentManager, fragment, android.R.id.content);
    }

    /**
     * Adds the specified GuidedStepSupportFragment to the fragment stack, replacing any existing
     * GuidedStepSupportFragments in the stack, and configuring the fragment-to-fragment custom
     * transitions.  A backstack entry is added, so the fragment will be dismissed when BACK key
     * is pressed.
     * <li>If current fragment on stack is GuidedStepSupportFragment: assign {@link #UI_STYLE_DEFAULT}
     * <li>If current fragment on stack is not GuidedStepSupportFragment: assign {@link #UI_STYLE_ENTRANCE}
     * <p>
     * Note: currently fragments added using this method must be created programmatically rather
     * than via XML.
     * @param fragmentManager The FragmentManager to be used in the transaction.
     * @param fragment The GuidedStepSupportFragment to be inserted into the fragment stack.
     * @param id The id of container to add GuidedStepSupportFragment, can be android.R.id.content.
     * @return The ID returned by the call FragmentTransaction.replace.
     */
    public static int add(FragmentManager fragmentManager, GuidedStepSupportFragment fragment, int id) {
        boolean inGuidedStep = getCurrentGuidedStepSupportFragment(fragmentManager) != null;
        if (IS_FRAMEWORK_FRAGMENT && Build.VERSION.SDK_INT >= 21 && Build.VERSION.SDK_INT < 23
                && !inGuidedStep && fragment.getContainerIdForBackground() != View.NO_ID) {
            // workaround b/22631964 for framework fragment
            fragmentManager.beginTransaction()
                .replace(id, new DummyFragment(), TAG_LEAN_BACK_ACTIONS_FRAGMENT)
                .replace(fragment.getContainerIdForBackground(), new DummyFragment())
                .commit();
        }
        FragmentTransaction ft = fragmentManager.beginTransaction();

        ft.addToBackStack(null);
        fragment.setUiStyle(inGuidedStep ? UI_STYLE_DEFAULT : UI_STYLE_ENTRANCE);
        initialBackground(fragment, id, ft);
        return ft.replace(id, fragment, TAG_LEAN_BACK_ACTIONS_FRAGMENT).commit();
    }

    /**
     * Adds the specified GuidedStepSupportFragment as content of Activity; no backstack entry is added so
     * the activity will be dismissed when BACK key is pressed.
     * {@link #UI_STYLE_ACTIVITY_ROOT} is assigned.
     *
     * Note: currently fragments added using this method must be created programmatically rather
     * than via XML.
     * @param activity The Activity to be used to insert GuidedstepFragment.
     * @param fragment The GuidedStepSupportFragment to be inserted into the fragment stack.
     * @param id The id of container to add GuidedStepSupportFragment, can be android.R.id.content.
     * @return The ID returned by the call FragmentTransaction.replace.
     */
    public static int addAsRoot(FragmentActivity activity, GuidedStepSupportFragment fragment, int id) {
        // Workaround b/23764120: call getDecorView() to force requestFeature of ActivityTransition.
        activity.getWindow().getDecorView();

        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        FragmentTransaction ft = fragmentManager.beginTransaction();
        fragment.setUiStyle(UI_STYLE_ACTIVITY_ROOT);
        initialBackground(fragment, id, ft);
        return ft.replace(id, fragment, TAG_LEAN_BACK_ACTIONS_FRAGMENT).commit();
    }

    static void initialBackground(GuidedStepSupportFragment fragment, int id, FragmentTransaction ft) {
        if (fragment.getContainerIdForBackground() != View.NO_ID) {
            Fragment backgroundFragment = fragment.onProvideBackgroundSupportFragment();
            if (backgroundFragment != null) {
                ft.replace(fragment.getContainerIdForBackground(), backgroundFragment);
            }
        }
    }

    /**
     * Returns the current GuidedStepSupportFragment on the fragment transaction stack.
     * @return The current GuidedStepSupportFragment, if any, on the fragment transaction stack.
     */
    public static GuidedStepSupportFragment getCurrentGuidedStepSupportFragment(FragmentManager fm) {
        Fragment f = fm.findFragmentByTag(TAG_LEAN_BACK_ACTIONS_FRAGMENT);
        if (f instanceof GuidedStepSupportFragment) {
            return (GuidedStepSupportFragment) f;
        }
        return null;
    }

    /**
     * @hide
     */
    public static class DummyFragment extends Fragment {

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            final View v = new View(inflater.getContext());
            v.setVisibility(View.GONE);
            return v;
        }
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
     * Called by Constructor to provide fragment transitions.  Default implementation creates
     * a short slide and fade transition in code for {@link #UI_STYLE_DEFAULT} for both enter and
     * exit transition.  When using style {@link #UI_STYLE_ENTRANCE}, enter transition is set
     * to slide from both sides.  When using style {@link #UI_STYLE_ACTIVITY_ROOT}, enter
     * transition is set to null and you should rely on activity transition.
     * <p>
     * Subclass may override and set its own fragment transition.  Note that because Context is not
     * available when onProvideFragmentTransitions() is called, subclass will need use a cached
     * static application context to load transition from xml.  Because the fragment view is
     * removed during fragment transition, in general app cannot use two Visibility transition
     * together.  Workaround is to create your own Visibility transition that controls multiple
     * animators (e.g. slide and fade animation in one Transition class).
     */
    protected void onProvideFragmentTransitions() {
        if (Build.VERSION.SDK_INT >= 21) {
            TransitionHelper helper = TransitionHelper.getInstance();
            if (getUiStyle() == UI_STYLE_DEFAULT) {
                Object enterTransition = helper.createFadeAndShortSlide(Gravity.END);
                helper.exclude(enterTransition, R.id.guidedactions_background, true);
                helper.exclude(enterTransition, R.id.guidedactions_selector, true);
                TransitionHelper.getInstance().setEnterTransition(this, enterTransition);
                Object exitTransition = helper.createFadeAndShortSlide(Gravity.START);
                helper.exclude(exitTransition, R.id.guidedactions_background, true);
                helper.exclude(exitTransition, R.id.guidedactions_selector, true);
                TransitionHelper.getInstance().setExitTransition(this, exitTransition);
            } else if (getUiStyle() == UI_STYLE_ENTRANCE) {
                Object enterTransition = helper.createFadeAndShortSlide(Gravity.END |
                        Gravity.START);
                helper.include(enterTransition, R.id.content_fragment);
                helper.include(enterTransition, R.id.action_fragment);
                TransitionHelper.getInstance().setEnterTransition(this, enterTransition);
                // exit transition is unchanged, same as UI_STYLE_DEFAULT
            } else if (getUiStyle() == UI_STYLE_ACTIVITY_ROOT) {
                // for Activity root, we dont need enter transition, use activity transition
                TransitionHelper.getInstance().setEnterTransition(this, null);
                // exit transition is unchanged, same as UI_STYLE_DEFAULT
            }
        }
    }

    /**
     * Default implementation of background for covering content below GuidedStepSupportFragment.
     * It uses current theme attribute guidedStepBackground which by default is read from
     * android:windowBackground.
     */
    public static class GuidedStepBackgroundSupportFragment extends Fragment {
        public GuidedStepBackgroundSupportFragment() {
            onProvideFragmentTransitions();
        }

        /**
         * Sets fragment transitions for GuidedStepBackgroundSupportFragment.  Can be overridden.
         */
        protected void onProvideFragmentTransitions() {
            if (Build.VERSION.SDK_INT >= 21) {
                TransitionHelper helper = TransitionHelper.getInstance();
                Object enterTransition = helper.createFadeTransition(
                        TransitionHelper.FADE_IN|TransitionHelper.FADE_OUT);
                TransitionHelper.getInstance().setEnterTransition(this, enterTransition);
            }
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            FragmentActivity activity = getActivity();
            Context themedContext = null;
            if (!isGuidedStepTheme(activity)) {
                // Look up the guidedStepTheme in the activity's currently specified theme.  If it
                // exists, replace the theme with its value.
                int resId = R.attr.guidedStepTheme;
                TypedValue typedValue = new TypedValue();
                boolean found = activity.getTheme().resolveAttribute(resId, typedValue, true);
                if (DEBUG) Log.v(TAG, "Found guided step theme reference? " + found);
                if (found) {
                    ContextThemeWrapper themeWrapper =
                            new ContextThemeWrapper(activity, typedValue.resourceId);
                    if (isGuidedStepTheme(themeWrapper)) {
                        themedContext = themeWrapper;
                    }
                }
                if (!found) {
                    Log.e(TAG, "GuidedStepSupportFragment does not have an appropriate theme set.");
                }
            }

            if (themedContext != null) {
                inflater = inflater.cloneInContext(themedContext);
            }

            return inflater.inflate(R.layout.lb_guidedstep_background, container, false);
        }
    }

    /**
     * Creates a background fragment for {@link #UI_STYLE_ENTRANCE}, returns null for other cases.
     * Subclass may override the default behavior, e.g. provide different backgrounds
     * for {@link #UI_STYLE_DEFAULT}.  Background fragment will be inserted in {@link
     * #getContainerIdForBackground()}.
     *
     * @return fragment that will be inserted below GuidedStepSupportFragment.
     */
    protected Fragment onProvideBackgroundSupportFragment() {
        if (getUiStyle() == UI_STYLE_ENTRANCE) {
            return new GuidedStepBackgroundSupportFragment();
        }
        return null;
    }

    /**
     * Returns container id for inserting {@link #onProvideBackgroundSupportFragment()}.  The id should be
     * different than container id for inserting GuidedStepSupportFragment.
     * Default value is {@link View#NO_ID}.  Subclass must override to host background fragment.
     * @return container id for inserting {@link #onProvideBackgroundSupportFragment()}
     */
    protected int getContainerIdForBackground() {
        return View.NO_ID;
    }


    /**
     * Set UI style to fragment arguments,  UI style cannot be changed after initialization.
     * @param style {@link #UI_STYLE_ACTIVITY_ROOT} {@link #UI_STYLE_DEFAULT} or
     * {@link #UI_STYLE_ENTRANCE}.
     */
    public void setUiStyle(int style) {
        int oldStyle = getUiStyle();
        Bundle arguments = getArguments();
        if (arguments == null) {
            arguments = new Bundle();
        }
        arguments.putInt(EXTRA_UI_STYLE, style);
        // call setArgument() will validate if the fragment is already added.
        setArguments(arguments);
        if (style != oldStyle) {
            onProvideFragmentTransitions();
        }
    }

    /**
     * Read UI style from fragment arguments.
     *
     * @return {@link #UI_STYLE_ACTIVITY_ROOT} {@link #UI_STYLE_DEFAULT} or
     * {@link #UI_STYLE_ENTRANCE}.
     */
    public int getUiStyle() {
        Bundle b = getArguments();
        if (b == null) return UI_STYLE_DEFAULT;
        return b.getInt(EXTRA_UI_STYLE, UI_STYLE_DEFAULT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DEBUG) Log.v(TAG, "onCreate");
        // Set correct transition from saved arguments.
        onProvideFragmentTransitions();
        Bundle state = (savedInstanceState != null) ? savedInstanceState : getArguments();
        if (state != null) {
            if (mSelectedIndex == -1) {
                mSelectedIndex = state.getInt(EXTRA_ACTION_SELECTED_INDEX, -1);
            }
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

        GuidedActionAdapter.EditListener editListener = new GuidedActionAdapter.EditListener() {
                @Override
                public void onGuidedActionEdited(GuidedAction action, boolean entering) {
                    runImeAnimations(entering);
                    if (!entering) {
                        GuidedStepSupportFragment.this.onGuidedActionEdited(action);
                    }
                }
        };

        mAdapter = new GuidedActionAdapter(mActions, this, this, editListener, mActionsStylist);

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
    }

    private static boolean isGuidedStepTheme(Context context) {
        int resId = R.attr.guidedStepThemeFlag;
        TypedValue typedValue = new TypedValue();
        boolean found = context.getTheme().resolveAttribute(resId, typedValue, true);
        if (DEBUG) Log.v(TAG, "Found guided step theme flag? " + found);
        return found && typedValue.type == TypedValue.TYPE_INT_BOOLEAN && typedValue.data != 0;
    }

    private void resolveTheme() {
        // Look up the guidedStepTheme in the currently specified theme.  If it exists,
        // replace the theme with its value.
        FragmentActivity activity = getActivity();
        if (mTheme == -1 && !isGuidedStepTheme(activity)) {
            // Look up the guidedStepTheme in the activity's currently specified theme.  If it
            // exists, replace the theme with its value.
            int resId = R.attr.guidedStepTheme;
            TypedValue typedValue = new TypedValue();
            boolean found = activity.getTheme().resolveAttribute(resId, typedValue, true);
            if (DEBUG) Log.v(TAG, "Found guided step theme reference? " + found);
            if (found) {
                ContextThemeWrapper themeWrapper =
                        new ContextThemeWrapper(activity, typedValue.resourceId);
                if (isGuidedStepTheme(themeWrapper)) {
                    mTheme = typedValue.resourceId;
                    mThemeWrapper = themeWrapper;
                } else {
                    found = false;
                    mThemeWrapper = null;
                }
            }
            if (!found) {
                Log.e(TAG, "GuidedStepSupportFragment does not have an appropriate theme set.");
            }
        } else if (mTheme != -1) {
            mThemeWrapper = new ContextThemeWrapper(activity, mTheme);
        }
    }

    private LayoutInflater getThemeInflater(LayoutInflater inflater) {
        if (mTheme == -1) {
            return inflater;
        } else {
            return inflater.cloneInContext(mThemeWrapper);
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

    private void runImeAnimations(boolean entering) {
        ArrayList<Animator> animators = new ArrayList<Animator>();
        if (entering) {
            mGuidanceStylist.onImeAppearing(animators);
            mActionsStylist.onImeAppearing(animators);
        } else {
            mGuidanceStylist.onImeDisappearing(animators);
            mActionsStylist.onImeDisappearing(animators);
        }
        AnimatorSet set = new AnimatorSet();
        set.playTogether(animators);
        set.start();
    }

}
