/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.navigation;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NavigationRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.util.Pair;
import android.util.Log;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * NavController manages app navigation within a {@link NavHost}.
 *
 * <p>Apps will generally obtain a controller directly from a host, or by using one of the utility
 * methods on the {@link Navigation} class rather than create a controller directly.</p>
 *
 * <p>Navigation flows and destinations are determined by the
 * {@link NavGraph navigation graph} owned by the controller. These graphs are typically
 * {@link #getNavInflater() inflated} from an Android resource, but, like views, they can also
 * be constructed or combined programmatically or for the case of dynamic navigation structure.
 * (For example, if the navigation structure of the application is determined by live data obtained'
 * from a remote server.)</p>
 */
public class NavController {
    private static final String TAG = "NavController";
    private static final String KEY_NAVIGATOR_STATE =
            "android-support-nav:controller:navigatorState";
    private static final String KEY_NAVIGATOR_STATE_NAMES =
            "android-support-nav:controller:navigatorState:names";
    private static final String KEY_BACK_STACK_IDS = "android-support-nav:controller:backStackIds";
    static final String KEY_DEEP_LINK_IDS = "android-support-nav:controller:deepLinkIds";
    static final String KEY_DEEP_LINK_EXTRAS =
            "android-support-nav:controller:deepLinkExtras";
    /**
     * The {@link Intent} that triggered a deep link to the current destination.
     */
    public static final @NonNull String KEY_DEEP_LINK_INTENT =
            "android-support-nav:controller:deepLinkIntent";

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final Context mContext;
    private Activity mActivity;
    private NavInflater mInflater;
    private NavGraph mGraph;
    private Bundle mNavigatorStateToRestore;
    private int[] mBackStackToRestore;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final Deque<NavDestination> mBackStack = new ArrayDeque<>();

    private final SimpleNavigatorProvider mNavigatorProvider = new SimpleNavigatorProvider() {
        @Nullable
        @Override
        public Navigator<? extends NavDestination> addNavigator(@NonNull String name,
                @NonNull Navigator<? extends NavDestination> navigator) {
            Navigator<? extends NavDestination> previousNavigator =
                    super.addNavigator(name, navigator);
            if (previousNavigator != navigator) {
                if (previousNavigator != null) {
                    previousNavigator.removeOnNavigatorNavigatedListener(mOnNavigatedListener);
                }
                navigator.addOnNavigatorNavigatedListener(mOnNavigatedListener);
            }
            return previousNavigator;
        }
    };

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final Navigator.OnNavigatorNavigatedListener mOnNavigatedListener =
            new Navigator.OnNavigatorNavigatedListener() {
                @Override
                public void onNavigatorNavigated(@NonNull Navigator navigator, @IdRes int destId,
                        @Navigator.BackStackEffect int backStackEffect) {
                    switch (backStackEffect) {
                        case Navigator.BACK_STACK_DESTINATION_POPPED:
                            // Find what destination just got popped
                            NavDestination lastFromNavigator = null;
                            Iterator<NavDestination> iterator = mBackStack.descendingIterator();
                            while (iterator.hasNext()) {
                                NavDestination destination = iterator.next();
                                if (destination.getNavigator() == navigator) {
                                    lastFromNavigator = destination;
                                    break;
                                }
                            }
                            if (lastFromNavigator == null) {
                                throw new IllegalArgumentException("Navigator " + navigator
                                        + " reported pop but did not have any destinations"
                                        + " on the NavController back stack");
                            }
                            // Pop all intervening destinations from other Navigators off the
                            // back stack
                            popBackStack(lastFromNavigator.getId(), false);
                            // Now record the pop operation that we were sent
                            if (!mBackStack.isEmpty()) {
                                mBackStack.removeLast();
                            }
                            // We never want to leave NavGraphs on the top of the stack
                            while (!mBackStack.isEmpty()
                                    && mBackStack.peekLast() instanceof NavGraph) {
                                popBackStack();
                            }
                            if (!mBackStack.isEmpty()) {
                                dispatchOnNavigated(mBackStack.peekLast());
                            }
                            break;
                        case Navigator.BACK_STACK_DESTINATION_ADDED:
                            NavDestination newDest = findDestination(destId);
                            if (newDest == null) {
                                throw new IllegalArgumentException("Navigator " + navigator
                                        + " reported navigation to unknown destination id "
                                        + NavDestination.getDisplayName(mContext, destId));
                            }
                            mBackStack.add(newDest);
                            dispatchOnNavigated(newDest);
                            break;
                        case Navigator.BACK_STACK_UNCHANGED:
                            break;
                    }
                }
            };

    private final CopyOnWriteArrayList<OnNavigatedListener> mOnNavigatedListeners =
            new CopyOnWriteArrayList<>();

    /**
     * OnNavigatorNavigatedListener receives a callback when the associated controller
     * navigates to a new destination.
     */
    public interface OnNavigatedListener {
        /**
         * onNavigatorNavigated is called when the controller navigates to a new destination.
         * This navigation may be to a destination that has not been seen before, or one that
         * was previously on the back stack. This method is called after navigation is complete,
         * but associated transitions may still be playing.
         *
         * @param controller the controller that navigated
         * @param destination the new destination
         */
        void onNavigated(@NonNull NavController controller, @NonNull NavDestination destination);
    }

    /**
     * Constructs a new controller for a given {@link Context}. Controllers should not be
     * used outside of their context and retain a hard reference to the context supplied.
     * If you need a global controller, pass {@link Context#getApplicationContext()}.
     *
     * <p>Apps should generally not construct controllers, instead obtain a relevant controller
     * directly from a navigation host via {@link NavHost#getNavController()} or by using one of
     * the utility methods on the {@link Navigation} class.</p>
     *
     * <p>Note that controllers that are not constructed with an {@link Activity} context
     * (or a wrapped activity context) will only be able to navigate to
     * {@link android.content.Intent#FLAG_ACTIVITY_NEW_TASK new tasks} or
     * {@link android.content.Intent#FLAG_ACTIVITY_NEW_DOCUMENT new document tasks} when
     * navigating to new activities.</p>
     *
     * @param context context for this controller
     */
    public NavController(@NonNull Context context) {
        mContext = context;
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                mActivity = (Activity) context;
                break;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        mNavigatorProvider.addNavigator(new NavGraphNavigator(mContext));
        mNavigatorProvider.addNavigator(new ActivityNavigator(mContext));
    }

    @NonNull
    Context getContext() {
        return mContext;
    }

    /**
     * Retrieve the NavController's {@link NavigatorProvider}. All {@link Navigator Navigators} used
     * to construct the {@link NavGraph navigation graph} for this nav controller should be added
     * to this navigator provider before the graph is constructed.
     * <p>
     * Generally, the Navigators are set for you by the {@link NavHost} hosting this NavController
     * and you do not need to manually interact with the navigator provider.
     * </p>
     * @return The {@link NavigatorProvider} used by this NavController.
     */
    @NonNull
    public NavigatorProvider getNavigatorProvider() {
        return mNavigatorProvider;
    }

    /**
     * Adds an {@link OnNavigatedListener} to this controller to receive events when
     * the controller navigates to a new destination.
     *
     * <p>The current destination, if any, will be immediately sent to your listener.</p>
     *
     * @param listener the listener to receive events
     */
    public void addOnNavigatedListener(@NonNull OnNavigatedListener listener) {
        // Inform the new listener of our current state, if any
        if (!mBackStack.isEmpty()) {
            listener.onNavigated(this, mBackStack.peekLast());
        }
        mOnNavigatedListeners.add(listener);
    }

    /**
     * Removes an {@link OnNavigatedListener} from this controller. It will no longer
     * receive navigation events.
     *
     * @param listener the listener to remove
     */
    public void removeOnNavigatedListener(@NonNull OnNavigatedListener listener) {
        mOnNavigatedListeners.remove(listener);
    }

    /**
     * Attempts to pop the controller's back stack. Analogous to when the user presses
     * the system {@link android.view.KeyEvent#KEYCODE_BACK Back} button when the associated
     * navigation host has focus.
     *
     * @return true if the stack was popped, false otherwise
     */
    public boolean popBackStack() {
        if (mBackStack.isEmpty()) {
            // Nothing to pop if the back stack is empty
            return false;
        }
        // Pop just the current destination off the stack
        return popBackStack(getCurrentDestination().getId(), true);
    }


    /**
     * Attempts to pop the controller's back stack back to a specific destination.
     *
     * @param destinationId The topmost destination to retain
     * @param inclusive Whether the given destination should also be popped.
     *
     * @return true if the stack was popped at least once, false otherwise
     */
    public boolean popBackStack(@IdRes int destinationId, boolean inclusive) {
        if (mBackStack.isEmpty()) {
            // Nothing to pop if the back stack is empty
            return false;
        }
        ArrayList<NavDestination> destinationsToRemove = new ArrayList<>();
        Iterator<NavDestination> iterator = mBackStack.descendingIterator();
        boolean foundDestination = false;
        while (iterator.hasNext()) {
            NavDestination destination = iterator.next();
            if (inclusive || destination.getId() != destinationId) {
                destinationsToRemove.add(destination);
            }
            if (destination.getId() == destinationId) {
                foundDestination = true;
                break;
            }
        }
        if (!foundDestination) {
            // We were passed a destinationId that doesn't exist on our back stack.
            // Better to ignore the popBackStack than accidentally popping the entire stack
            String destinationName = NavDestination.getDisplayName(mContext, destinationId);
            Log.i(TAG, "Ignoring popBackStack to destination " + destinationName
                    + " as it was not found on the current back stack");
            return false;
        }
        boolean popped = false;
        iterator = destinationsToRemove.iterator();
        while (iterator.hasNext()) {
            NavDestination destination = iterator.next();
            // Skip destinations already removed by a previous popBackStack operation
            while (!mBackStack.isEmpty() && mBackStack.peekLast().getId() != destination.getId()) {
                if (iterator.hasNext()) {
                    destination = iterator.next();
                } else {
                    destination = null;
                    break;
                }
            }
            if (destination != null) {
                popped = destination.getNavigator().popBackStack() || popped;
            }
        }
        return popped;
    }

    /**
     * Attempts to navigate up in the navigation hierarchy. Suitable for when the
     * user presses the "Up" button marked with a left (or start)-facing arrow in the upper left
     * (or starting) corner of the app UI.
     *
     * <p>The intended behavior of Up differs from {@link #popBackStack() Back} when the user
     * did not reach the current destination from the application's own task. e.g. if the user
     * is viewing a document or link in the current app in an activity hosted on another app's
     * task where the user clicked the link. In this case the current activity (determined by the
     * context used to create this NavController) will be {@link Activity#finish() finished} and
     * the user will be taken to an appropriate destination in this app on its own task.</p>
     *
     * @return true if navigation was successful, false otherwise
     */
    public boolean navigateUp() {
        if (mBackStack.size() == 1) {
            // If there's only one entry, then we've deep linked into a specific destination
            // on another task so we need to find the parent and start our task from there
            NavDestination currentDestination = getCurrentDestination();
            int destId = currentDestination.getId();
            NavGraph parent = currentDestination.getParent();
            while (parent != null) {
                if (parent.getStartDestination() != destId) {
                    TaskStackBuilder parentIntents = new NavDeepLinkBuilder(NavController.this)
                            .setDestination(parent.getId())
                            .createTaskStackBuilder();
                    parentIntents.startActivities();
                    if (mActivity != null) {
                        mActivity.finish();
                    }
                    return true;
                }
                destId = parent.getId();
                parent = parent.getParent();
            }
            // We're already at the startDestination of the graph so there's no 'Up' to go to
            return false;
        } else {
            return popBackStack();
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void dispatchOnNavigated(NavDestination destination) {
        for (OnNavigatedListener listener : mOnNavigatedListeners) {
            listener.onNavigated(this, destination);
        }
    }

    /**
     * Returns the {@link NavInflater inflater} for this controller.
     *
     * @return inflater for loading navigation resources
     */
    @NonNull
    public NavInflater getNavInflater() {
        if (mInflater == null) {
            mInflater = new NavInflater(mContext, mNavigatorProvider);
        }
        return mInflater;
    }

    /**
     * Sets the {@link NavGraph navigation graph} to the specified resource.
     * Any current navigation graph data will be replaced.
     *
     * <p>The inflated graph can be retrieved via {@link #getGraph()}.</p>
     *
     * @param graphResId resource id of the navigation graph to inflate
     *
     * @see #getNavInflater()
     * @see #setGraph(NavGraph)
     * @see #getGraph
     */
    public void setGraph(@NavigationRes int graphResId) {
        setGraph(graphResId, null);
    }

    /**
     * Sets the {@link NavGraph navigation graph} to the specified resource.
     * Any current navigation graph data will be replaced.
     *
     * <p>The inflated graph can be retrieved via {@link #getGraph()}.</p>
     *
     * @param graphResId resource id of the navigation graph to inflate
     * @param startDestinationArgs arguments to send to the start destination of the graph
     *
     * @see #getNavInflater()
     * @see #setGraph(NavGraph, Bundle)
     * @see #getGraph
     */
    public void setGraph(@NavigationRes int graphResId, @Nullable Bundle startDestinationArgs) {
        mGraph = getNavInflater().inflate(graphResId);
        onGraphCreated(startDestinationArgs);
    }

    /**
     * Sets the {@link NavGraph navigation graph} to the specified graph.
     * Any current navigation graph data will be replaced.
     *
     * <p>The graph can be retrieved later via {@link #getGraph()}.</p>
     *
     * @param graph graph to set
     * @see #setGraph(int)
     * @see #getGraph
     */
    public void setGraph(@NonNull NavGraph graph) {
        setGraph(graph, null);
    }

    /**
     * Sets the {@link NavGraph navigation graph} to the specified graph.
     * Any current navigation graph data will be replaced.
     *
     * <p>The graph can be retrieved later via {@link #getGraph()}.</p>
     *
     * @param graph graph to set
     * @see #setGraph(int, Bundle)
     * @see #getGraph
     */
    public void setGraph(@NonNull NavGraph graph, @Nullable Bundle startDestinationArgs) {
        mGraph = graph;
        onGraphCreated(startDestinationArgs);
    }

    private void onGraphCreated(@Nullable Bundle startDestinationArgs) {
        if (mNavigatorStateToRestore != null) {
            ArrayList<String> navigatorNames = mNavigatorStateToRestore.getStringArrayList(
                    KEY_NAVIGATOR_STATE_NAMES);
            if (navigatorNames != null) {
                for (String name : navigatorNames) {
                    Navigator navigator = mNavigatorProvider.getNavigator(name);
                    Bundle bundle = mNavigatorStateToRestore.getBundle(name);
                    if (bundle != null) {
                        navigator.onRestoreState(bundle);
                    }
                }
            }
        }
        if (mBackStackToRestore != null) {
            for (int destinationId : mBackStackToRestore) {
                NavDestination node = findDestination(destinationId);
                if (node == null) {
                    throw new IllegalStateException("unknown destination during restore: "
                            + mContext.getResources().getResourceName(destinationId));
                }
                mBackStack.add(node);
            }
            mBackStackToRestore = null;
        }
        if (mGraph != null && mBackStack.isEmpty()) {
            boolean deepLinked = mActivity != null && onHandleDeepLink(mActivity.getIntent());
            if (!deepLinked) {
                // Navigate to the first destination in the graph
                // if we haven't deep linked to a destination
                mGraph.navigate(startDestinationArgs, null, null);
            }
        }
    }

    /**
     * Checks the given Intent for a Navigation deep link and navigates to the deep link if present.
     * This is called automatically for you the first time you set the graph if you've passed in an
     * {@link Activity} as the context when constructing this NavController, but should be manually
     * called if your Activity receives new Intents in {@link Activity#onNewIntent(Intent)}.
     * <p>
     * The types of Intents that are supported include:
     * <ul>
     *     <ol>Intents created by {@link NavDeepLinkBuilder} or
     *     {@link #createDeepLink()}. This assumes that the current graph shares
     *     the same hierarchy to get to the deep linked destination as when the deep link was
     *     constructed.</ol>
     *     <ol>Intents that include a {@link Intent#getData() data Uri}. This Uri will be checked
     *     against the Uri patterns added via {@link NavDestination#addDeepLink(String)}.</ol>
     * </ul>
     * <p>The {@link #getGraph() navigation graph} should be set before calling this method.</p>
     * @param intent The Intent that may contain a valid deep link
     * @return True if the navigation controller found a valid deep link and navigated to it.
     * @see NavDestination#addDeepLink(String)
     */
    public boolean onHandleDeepLink(@Nullable Intent intent) {
        if (intent == null) {
            return false;
        }
        Bundle extras = intent.getExtras();
        int[] deepLink = extras != null ? extras.getIntArray(KEY_DEEP_LINK_IDS) : null;
        Bundle bundle = new Bundle();
        Bundle deepLinkExtras = extras != null ? extras.getBundle(KEY_DEEP_LINK_EXTRAS) : null;
        if (deepLinkExtras != null) {
            bundle.putAll(deepLinkExtras);
        }
        if ((deepLink == null || deepLink.length == 0) && intent.getData() != null) {
            Pair<NavDestination, Bundle> matchingDeepLink = mGraph.matchDeepLink(intent.getData());
            if (matchingDeepLink != null) {
                deepLink = matchingDeepLink.first.buildDeepLinkIds();
                bundle.putAll(matchingDeepLink.second);
            }
        }
        if (deepLink == null || deepLink.length == 0) {
            return false;
        }
        bundle.putParcelable(KEY_DEEP_LINK_INTENT, intent);
        int flags = intent.getFlags();
        if ((flags & Intent.FLAG_ACTIVITY_NEW_TASK) != 0
                && (flags & Intent.FLAG_ACTIVITY_CLEAR_TASK) == 0) {
            // Someone called us with NEW_TASK, but we don't know what state our whole
            // task stack is in, so we need to manually restart the whole stack to
            // ensure we're in a predictably good state.
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            TaskStackBuilder taskStackBuilder = TaskStackBuilder
                    .create(mContext)
                    .addNextIntentWithParentStack(intent);
            taskStackBuilder.startActivities();
            if (mActivity != null) {
                mActivity.finish();
            }
            return true;
        }
        if ((flags & Intent.FLAG_ACTIVITY_NEW_TASK) != 0) {
            // Start with a cleared task starting at our root when we're on our own task
            if (!mBackStack.isEmpty()) {
                navigate(mGraph.getStartDestination(), bundle, new NavOptions.Builder()
                        .setPopUpTo(mGraph.getId(), true)
                        .setEnterAnim(0).setExitAnim(0).build());
            }
            int index = 0;
            while (index < deepLink.length) {
                int destinationId = deepLink[index++];
                NavDestination node = findDestination(destinationId);
                if (node == null) {
                    throw new IllegalStateException("unknown destination during deep link: "
                            + NavDestination.getDisplayName(mContext, destinationId));
                }
                node.navigate(bundle,
                        new NavOptions.Builder().setEnterAnim(0).setExitAnim(0).build(), null);
            }
            return true;
        }
        // Assume we're on another apps' task and only start the final destination
        NavGraph graph = mGraph;
        for (int i = 0; i < deepLink.length; i++) {
            int destinationId = deepLink[i];
            NavDestination node = i == 0 ? mGraph : graph.findNode(destinationId);
            if (node == null) {
                throw new IllegalStateException("unknown destination during deep link: "
                        + NavDestination.getDisplayName(mContext, destinationId));
            }
            if (i != deepLink.length - 1) {
                // We're not at the final NavDestination yet, so keep going through the chain
                graph = (NavGraph) node;
            } else {
                // Navigate to the last NavDestination, clearing any existing destinations
                node.navigate(bundle, new NavOptions.Builder()
                        .setPopUpTo(mGraph.getId(), true)
                        .setEnterAnim(0).setExitAnim(0).build(), null);
            }
        }
        return true;
    }

    /**
     * Gets the topmost navigation graph associated with this NavController.
     *
     * @see #setGraph(int)
     * @see #setGraph(NavGraph)
     * @throws IllegalStateException if called before <code>setGraph()</code>.
     */
    @NonNull
    public NavGraph getGraph() {
        if (mGraph == null) {
            throw new IllegalStateException("You must call setGraph() before calling getGraph()");
        }
        return mGraph;
    }

    /**
     * Gets the current destination.
     */
    @Nullable
    public NavDestination getCurrentDestination() {
        return mBackStack.peekLast();
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    NavDestination findDestination(@IdRes int destinationId) {
        if (mGraph == null) {
            return null;
        }
        if (mGraph.getId() == destinationId) {
            return mGraph;
        }
        NavDestination currentNode = mBackStack.isEmpty() ? mGraph : mBackStack.peekLast();
        NavGraph currentGraph = currentNode instanceof NavGraph
                ? (NavGraph) currentNode
                : currentNode.getParent();
        return currentGraph.findNode(destinationId);
    }

    /**
     * Navigate to a destination from the current navigation graph. This supports both navigating
     * via an {@link NavDestination#getAction(int) action} and directly navigating to a destination.
     *
     * @param resId an {@link NavDestination#getAction(int) action} id or a destination id to
     *              navigate to
     */
    public final void navigate(@IdRes int resId) {
        navigate(resId, null);
    }

    /**
     * Navigate to a destination from the current navigation graph. This supports both navigating
     * via an {@link NavDestination#getAction(int) action} and directly navigating to a destination.
     *
     * @param resId an {@link NavDestination#getAction(int) action} id or a destination id to
     *              navigate to
     * @param args arguments to pass to the destination
     */
    public final void navigate(@IdRes int resId, @Nullable Bundle args) {
        navigate(resId, args, null);
    }

    /**
     * Navigate to a destination from the current navigation graph. This supports both navigating
     * via an {@link NavDestination#getAction(int) action} and directly navigating to a destination.
     *
     * @param resId an {@link NavDestination#getAction(int) action} id or a destination id to
     *              navigate to
     * @param args arguments to pass to the destination
     * @param navOptions special options for this navigation operation
     */
    @SuppressWarnings("deprecation")
    public void navigate(@IdRes int resId, @Nullable Bundle args, @Nullable NavOptions navOptions) {
        navigate(resId, args, navOptions, null);
    }

    /**
     * Navigate to a destination from the current navigation graph. This supports both navigating
     * via an {@link NavDestination#getAction(int) action} and directly navigating to a destination.
     *
     * @param resId an {@link NavDestination#getAction(int) action} id or a destination id to
     *              navigate to
     * @param args arguments to pass to the destination
     * @param navOptions special options for this navigation operation
     * @param navigatorExtras extras to pass to the Navigator
     */
    @SuppressWarnings("deprecation")
    public void navigate(@IdRes int resId, @Nullable Bundle args, @Nullable NavOptions navOptions,
            @Nullable Navigator.Extras navigatorExtras) {
        NavDestination currentNode = mBackStack.isEmpty() ? mGraph : mBackStack.peekLast();
        if (currentNode == null) {
            throw new IllegalStateException("no current navigation node");
        }
        @IdRes int destId = resId;
        final NavAction navAction = currentNode.getAction(resId);
        if (navAction != null) {
            if (navOptions == null) {
                navOptions = navAction.getNavOptions();
            }
            destId = navAction.getDestinationId();
        }
        if (destId == 0 && navOptions != null && navOptions.getPopUpTo() != 0) {
            popBackStack(navOptions.getPopUpTo(), navOptions.isPopUpToInclusive());
            return;
        }

        if (destId == 0) {
            throw new IllegalArgumentException("Destination id == 0 can only be used"
                    + " in conjunction with navOptions.popUpTo != 0");
        }

        NavDestination node = findDestination(destId);
        if (node == null) {
            final String dest = NavDestination.getDisplayName(mContext, destId);
            throw new IllegalArgumentException("navigation destination " + dest
                    + (navAction != null
                    ? " referenced from action " + NavDestination.getDisplayName(mContext, resId)
                    : "")
                    + " is unknown to this NavController");
        }
        if (navOptions != null) {
            if (navOptions.getPopUpTo() != 0) {
                popBackStack(navOptions.getPopUpTo(), navOptions.isPopUpToInclusive());
            }
        }
        node.navigate(args, navOptions, navigatorExtras);
    }

    /**
     * Navigate via the given {@link NavDirections}
     *
     * @param directions directions that describe this navigation operation
     */
    public void navigate(@NonNull NavDirections directions) {
        navigate(directions.getActionId(), directions.getArguments());
    }

    /**
     * Navigate via the given {@link NavDirections}
     *
     * @param directions directions that describe this navigation operation
     * @param navOptions special options for this navigation operation
     */
    public void navigate(@NonNull NavDirections directions, @Nullable NavOptions navOptions) {
        navigate(directions.getActionId(), directions.getArguments(), navOptions);
    }

    /**
     * Navigate via the given {@link NavDirections}
     *
     * @param directions directions that describe this navigation operation
     * @param navigatorExtras extras to pass to the {@link Navigator}
     */
    public void navigate(@NonNull NavDirections directions,
            @NonNull Navigator.Extras navigatorExtras) {
        navigate(directions.getActionId(), directions.getArguments(), null, navigatorExtras);
    }

    /**
     * Create a deep link to a destination within this NavController.
     *
     * @return a {@link NavDeepLinkBuilder} suitable for constructing a deep link
     */
    @NonNull
    public NavDeepLinkBuilder createDeepLink() {
        return new NavDeepLinkBuilder(this);
    }

    /**
     * Saves all navigation controller state to a Bundle.
     *
     * <p>State may be restored from a bundle returned from this method by calling
     * {@link #restoreState(Bundle)}. Saving controller state is the responsibility
     * of a {@link NavHost}.</p>
     *
     * @return saved state for this controller
     */
    @Nullable
    public Bundle saveState() {
        Bundle b = null;
        ArrayList<String> navigatorNames = new ArrayList<>();
        Bundle navigatorState = new Bundle();
        for (Map.Entry<String, Navigator<? extends NavDestination>> entry :
                mNavigatorProvider.getNavigators().entrySet()) {
            String name = entry.getKey();
            Bundle savedState = entry.getValue().onSaveState();
            if (savedState != null) {
                navigatorNames.add(name);
                navigatorState.putBundle(name, savedState);
            }
        }
        if (!navigatorNames.isEmpty()) {
            b = new Bundle();
            navigatorState.putStringArrayList(KEY_NAVIGATOR_STATE_NAMES, navigatorNames);
            b.putBundle(KEY_NAVIGATOR_STATE, navigatorState);
        }
        if (!mBackStack.isEmpty()) {
            if (b == null) {
                b = new Bundle();
            }
            int[] backStack = new int[mBackStack.size()];
            int index = 0;
            for (NavDestination destination : mBackStack) {
                backStack[index++] = destination.getId();
            }
            b.putIntArray(KEY_BACK_STACK_IDS, backStack);
        }
        return b;
    }

    /**
     * Restores all navigation controller state from a bundle. This should be called before any
     * call to {@link #setGraph}.
     *
     * <p>State may be saved to a bundle by calling {@link #saveState()}.
     * Restoring controller state is the responsibility of a {@link NavHost}.</p>
     *
     * @param navState state bundle to restore
     */
    public void restoreState(@Nullable Bundle navState) {
        if (navState == null) {
            return;
        }

        mNavigatorStateToRestore = navState.getBundle(KEY_NAVIGATOR_STATE);
        mBackStackToRestore = navState.getIntArray(KEY_BACK_STACK_IDS);
    }
}
