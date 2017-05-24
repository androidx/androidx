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

package android.support.navigation.app.nav;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.XmlRes;
import android.text.TextUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * NavController manages app navigation within a host.
 *
 * <p>A host is a single context or container for navigation, e.g. a {@link NavHostFragment}.
 * Navigation hosts are responsible for {@link #saveState() saving} and
 * {@link #restoreState(Bundle) restoring} their controller's state. Apps will generally obtain
 * a controller directly from a host, or by using one of the utility methods on the
 * {@link Navigation} class rather than create a controller directly.</p>
 *
 * <p>Navigation flows and destinations are determined by the
 * {@link NavGraph navigation graph} owned by the controller. These graphs are typically
 * {@link #getNavInflater() inflated} from an Android resource, but, like views, they can also
 * be constructed or combined programmatically or for the case of dynamic navigation structure.
 * (For example, if the navigation structure of the application is determined by live data obtained'
 * from a remote server.)</p>
 */
public class NavController implements NavigatorProvider {
    private static final String KEY_GRAPH_ID = "android-support-nav:controller:graphId";
    private static final String KEY_BACK_STACK_IDS = "android-support-nav:controller:backStackIds";

    private Context mContext;
    private NavInflater mInflater;
    private NavGraph mGraph;
    private int mGraphId;

    private final HashMap<String, Navigator> mNavigators = new HashMap<>();
    private final Deque<NavDestination> mBackStack = new ArrayDeque<>();

    private final Navigator.OnNavigatorNavigatedListener mOnNavigatedListener =
            new Navigator.OnNavigatorNavigatedListener() {
                @Override
                public void onNavigatorNavigated(Navigator navigator, @IdRes int destId,
                        @Navigator.BackStackEffect int backStackEffect) {
                    if (destId != 0) {
                        NavDestination newDest = findDestination(destId);
                        if (newDest == null) {
                            throw new IllegalArgumentException("Navigator " + navigator
                                    + " reported navigation to unknown destination id "
                                    + mContext.getResources().getResourceName(destId));
                        }
                        switch (backStackEffect) {
                            case Navigator.BACK_STACK_DESTINATION_POPPED:
                                while (!mBackStack.isEmpty()
                                        && mBackStack.peekLast().getId() != destId) {
                                    mBackStack.removeLast();
                                }
                                break;
                            case Navigator.BACK_STACK_DESTINATION_ADDED:
                                mBackStack.add(newDest);
                                break;
                            case Navigator.BACK_STACK_UNCHANGED:
                                // Don't update the back stack and don't dispatchOnNavigated
                                return;
                        }
                        dispatchOnNavigated(newDest);
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
        void onNavigated(NavController controller, NavDestination destination);
    }

    /**
     * Constructs a new controller for a given {@link Context}. Controllers should not be
     * used outside of their context and retain a hard reference to the context supplied.
     * If you need a global controller, pass {@link Context#getApplicationContext()}.
     *
     * <p>Apps should generally not construct controllers, instead obtain a relevant controller
     * directly from a navigation host such as
     * {@link NavHostFragment#getNavController() NavHostFragment} or by using one of
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
    public NavController(Context context) {
        mContext = context;
        addNavigator(new NavGraphNavigator(mContext));
        addNavigator(new ActivityNavigator(mContext));
    }

    /**
     * Constructs a new controller for a given {@link Activity}.
     *
     * <p>Apps should generally not construct controllers, instead obtain a relevant controller
     * directly from a navigation host such as
     * {@link NavHostFragment#getNavController() NavHostFragment} or by using one of
     * the utility methods on the {@link Navigation} class.</p>
     *
     * @param activity activity for this controller
     */
    public NavController(Activity activity) {
        mContext = activity;
        addNavigator(new NavGraphNavigator(mContext));
        addNavigator(new ActivityNavigator(activity));
    }

    /**
     * Adds an {@link OnNavigatedListener} to this controller to receive events when
     * the controller navigates to a new destination.
     *
     * <p>The current destination, if any, will be immediately sent to your listener.</p>
     *
     * @param listener the listener to receive events
     */
    public void addOnNavigatedListener(OnNavigatedListener listener) {
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
    public void removeOnNavigatedListener(OnNavigatedListener listener) {
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
            throw new IllegalArgumentException("NavController back stack is empty");
        }
        boolean popped = false;
        while (!mBackStack.isEmpty()) {
            popped = mBackStack.removeLast().getNavigator().popBackStack();
            if (popped) {
                break;
            }
        }
        return popped;
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
            throw new IllegalArgumentException("NavController back stack is empty");
        }
        ArrayList<NavDestination> destinationsToRemove = new ArrayList<>();
        Iterator<NavDestination> iterator = mBackStack.descendingIterator();
        while (iterator.hasNext()) {
            NavDestination destination = iterator.next();
            if (inclusive || destination.getId() != destinationId) {
                destinationsToRemove.add(destination);
            }
            if (destination.getId() == destinationId) {
                break;
            }
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
     * task where the user clicked the link. In this case the current activity will be
     * {@link Activity#finish() finished} and the user will be taken to an appropriate
     * destination in this app on its own task.</p>
     *
     * @return true if navigation was successful, false otherwise
     */
    public boolean navigateUp() {
        // TODO check current task; if we're in one from a viewer context on another task, jump.
        return popBackStack();
    }

    void dispatchOnNavigated(NavDestination destination) {
        for (OnNavigatedListener listener : mOnNavigatedListeners) {
            listener.onNavigated(this, destination);
        }
    }

    @Override
    public Navigator getNavigator(Class<? extends Navigator> navigatorClass) {
        Navigator.Name annotation = navigatorClass.getAnnotation(Navigator.Name.class);
        String name = annotation != null ? annotation.value() : null;
        if (TextUtils.isEmpty(name)) {
            throw new IllegalArgumentException("No @Navigator.Name annotation found for "
                    + navigatorClass.getSimpleName());
        }

        return getNavigator(name);
    }

    @Override
    public Navigator getNavigator(String name) {
        if (TextUtils.isEmpty(name)) {
            throw new IllegalArgumentException("navigator name cannot be null");
        }

        return mNavigators.get(name);
    }

    @Override
    public void addNavigator(Navigator navigator) {
        Navigator.Name annotation = navigator.getClass().getAnnotation(Navigator.Name.class);
        String name = annotation != null ? annotation.value() : null;
        if (TextUtils.isEmpty(name)) {
            throw new IllegalArgumentException("No @Navigator.Name annotation found for "
                    + navigator.getClass().getSimpleName());
        }

        addNavigator(name, navigator);
    }

    @Override
    public void addNavigator(String name, Navigator navigator) {
        if (TextUtils.isEmpty(name)) {
            throw new IllegalArgumentException("navigator name cannot be null");
        }

        navigator.addOnNavigatorNavigatedListener(mOnNavigatedListener);
        mNavigators.put(name, navigator);
    }

    /**
     * Removes a registered {@link Navigator} by name.
     *
     * @param name name of the navigator to remove
     */
    public void removeNavigator(String name) {
        final Navigator removed = mNavigators.remove(name);
        if (removed != null) {
            removed.removeOnNavigatorNavigatedListener(mOnNavigatedListener);
        }
    }

    /**
     * Sets the {@link NavGraph navigation graph} as specified in the application manifest.
     *
     * <p>Applications may declare a graph resource in their manifest instead of declaring
     * or passing this data to each host or controller:</p>
     *
     * <pre class="prettyprint">
     *     <meta-data android:name="android.nav.graph" android:resource="@xml/my_nav_graph" />
     * </pre>
     *
     * <p>The inflated graph can be retrieved via {@link #getGraph()}.</p>
     *
     * @see NavInflater#METADATA_KEY_GRAPH
     * @see NavInflater#inflateMetadataGraph()
     * @see #getGraph
     */
    public void setMetadataGraph() {
        setGraph(mInflater.inflateMetadataGraph());
    }

    /**
     * Returns the {@link NavInflater inflater} for this controller.
     *
     * @return inflater for loading navigation resources
     */
    public NavInflater getNavInflater() {
        if (mInflater == null) {
            mInflater = new NavInflater(mContext, this);
        }
        return mInflater;
    }

    /**
     * Sets the {@link NavGraph navigation graph} to the specified resource.
     * Any current navigation graph data will be replaced.
     *
     * <p>The inflated graph can be retrieved via {@link #getGraph()}.</p>
     *
     * @param resid resource id of the navigation graph to inflate
     *
     * @see #getNavInflater()
     * @see #setGraph(NavGraph)
     * @see #getGraph
     */
    public void setGraph(@XmlRes int resid) {
        mGraph = getNavInflater().inflate(resid);
        mGraphId = resid;
        onGraphCreated();
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
    public void setGraph(NavGraph graph) {
        mGraph = graph;
        mGraphId = 0;
        onGraphCreated();
    }

    private void onGraphCreated() {
        // Navigate to the first destination in the graph
        if (mGraph != null && mBackStack.isEmpty()) {
            mGraph.navigate(null, null);
        }
    }

    /**
     * Gets the topmost navigation graph associated with this NavController.
     *
     * @see #setGraph(int)
     * @see #setGraph(NavGraph)
     * @see #setMetadataGraph()
     */
    public NavGraph getGraph() {
        return mGraph;
    }

    /**
     * Gets the current destination.
     */
    public NavDestination getCurrentDestination() {
        return mBackStack.peekLast();
    }

    private NavDestination findDestination(@IdRes int destinationId) {
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
     * Navigate directly to a destination.
     *
     * <p>Requests navigation to the given destination id from the current navigation graph.
     * Apps should generally prefer {@link #navigate(int) navigating by action} when possible.</p>
     *
     * @param resid destination id to navigate to
     */
    public final void navigateTo(@IdRes int resid) {
        navigateTo(resid, null);
    }

    /**
     * Navigate directly to a destination.
     *
     * <p>Requests navigation to the given destination id from the current navigation graph.
     * Apps should generally prefer {@link #navigate(int) navigating by action} when possible.</p>
     *
     * @param resid destination id to navigate to
     * @param args arguments to pass to the destination
     */
    public final void navigateTo(@IdRes int resid, Bundle args) {
        navigateTo(resid, args, null);
    }

    /**
     * Navigate directly to a destination.
     *
     * <p>Requests navigation to the given destination id from the current navigation graph.
     * Apps should generally prefer {@link #navigate(int) navigating by action} when possible.</p>
     *
     * @param resid destination id to navigate to
     * @param args arguments to pass to the destination
     * @param navOptions special options for this navigation operation
     */
    public void navigateTo(@IdRes int resid, Bundle args, NavOptions navOptions) {
        NavDestination node = findDestination(resid);
        if (node == null) {
            final String dest = mContext.getResources().getResourceName(resid);
            throw new IllegalArgumentException("navigation destination " + dest
                    + " is unknown to this NavController");
        }
        if (navOptions != null && navOptions.getPopUpTo() != 0) {
            popBackStack(navOptions.getPopUpTo(), navOptions.isPopUpToInclusive());
        }
        node.navigate(args, navOptions);
    }

    /**
     * Navigate via an action defined on the current destination.
     *
     * <p>Requests navigation to the given {@link NavDestination#getAction(int) action},
     * appropriate for the current location, e.g. "next" or "home."</p>
     *
     * @param action navigation action to invoke
     */
    public void navigate(@IdRes int action) {
        navigate(action, null);
    }

    /**
     * Navigate via an action defined on the current destination.
     *
     * <p>Requests navigation to the given {@link NavDestination#getAction(int) action},
     * appropriate for the current location, e.g. "next" or "home."</p>
     *
     * @param action navigation action to invoke
     * @param args arguments to pass to the destination
     */
    public void navigate(@IdRes int action, Bundle args) {
        navigate(action, args, null);
    }

    /**
     * Navigate via an action defined on the current destination.
     *
     * <p>Requests navigation to the given {@link NavDestination#getAction(int) action},
     * appropriate for the current location, e.g. "next" or "home."</p>
     *
     * @param action navigation action to invoke
     * @param args arguments to pass to the destination
     * @param navOptions special options for this navigation operation. This will be used instead
     *                   of any NavOptions attached to the action.
     */
    public void navigate(@IdRes int action, Bundle args, NavOptions navOptions) {
        NavDestination currentNode = mBackStack.isEmpty() ? mGraph : mBackStack.peekLast();
        if (currentNode == null) {
            throw new IllegalStateException("no current navigation node");
        }
        final NavAction navAction = currentNode.getAction(action);
        if (navAction == null) {
            final Resources res = mContext.getResources();
            throw new IllegalStateException("no destination defined from "
                    + res.getResourceName(currentNode.getId())
                    + " for action " + res.getResourceName(action));
        }
        NavOptions options = navOptions != null ? navOptions : navAction.getNavOptions();
        if (navAction.getDestinationId() == 0 && options != null && options.getPopUpTo() != 0) {
            // Allow actions to leave out a destinationId only if they have a non-zero popUpTo
            popBackStack(options.getPopUpTo(), options.isPopUpToInclusive());
        } else {
            navigateTo(navAction.getDestinationId(), args, options);
        }
    }

    /**
     * Saves all navigation controller state to a Bundle.
     *
     * <p>State may be restored from a bundle returned from this method by calling
     * {@link #restoreState(Bundle)}. Saving controller state is the responsibility
     * of a navigation host, e.g. {@link NavHostFragment}.</p>
     *
     * @return saved state for this controller
     */
    public Bundle saveState() {
        Bundle b = null;
        if (mGraphId != 0) {
            b = new Bundle();
            b.putInt(KEY_GRAPH_ID, mGraphId);
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
     * Restores all navigation controller state from a bundle.
     *
     * <p>State may be saved to a bundle by calling {@link #saveState()}.
     * Restoring controller state is the responsibility of a navigation host,
     * e.g. {@link NavHostFragment}.</p>
     *
     * @param navState state bundle to restore
     */
    public void restoreState(Bundle navState) {
        if (navState == null) {
            return;
        }

        mGraphId = navState.getInt(KEY_GRAPH_ID);
        if (mGraphId != 0) {
            mGraph = getNavInflater().inflate(mGraphId);
        }

        // Restore the current location first, or onGraphCreated will perform navigation
        // if there are no nodes on the back stack.
        final int[] backStack = navState.getIntArray(KEY_BACK_STACK_IDS);
        if (backStack != null) {
            for (int destinationId : backStack) {
                NavDestination node = findDestination(destinationId);
                if (node == null) {
                    throw new IllegalStateException("unknown destination during restore: "
                            + mContext.getResources().getResourceName(destinationId));
                }
                mBackStack.add(node);
            }
        }
        onGraphCreated();
    }
}
