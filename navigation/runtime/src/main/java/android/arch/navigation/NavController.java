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

package android.arch.navigation;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
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
    private static final String KEY_DEEP_LINK_IDS = "android-support-nav:controller:deepLinkIds";
    private static final String KEY_DEEP_LINK_EXTRAS =
            "android-support-nav:controller:deepLinkExtras";
    /**
     * The {@link Intent} that triggered a deep link to the current destination.
     */
    public static final String KEY_DEEP_LINK_INTENT =
            "android-support-nav:controller:deepLinkIntent";

    private Context mContext;
    private Activity mActivity;
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
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                mActivity = (Activity) context;
                break;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        addNavigator(new NavGraphNavigator(mContext));
        addNavigator(new ActivityNavigator(mContext));
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
        if (mBackStack.size() == 1) {
            // If there's only one entry, then we've deep linked into a specific destination
            // so we should build the parent destination to provide the proper Up behavior
            NavDestination currentDestination = getCurrentDestination();
            int destId = currentDestination.getId();
            NavGraph parent = currentDestination.getParent();
            while (parent != null) {
                if (parent.getStartDestination() != destId) {
                    navigate(parent.getStartDestination(), null,
                            new NavOptions.Builder()
                                    .setClearTask(true)
                                    .build());
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
        setGraph(getNavInflater().inflateMetadataGraph());
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
        if (mGraph != null && mBackStack.isEmpty()) {
            boolean deepLinked = mActivity != null && onHandleDeepLink(mActivity.getIntent());
            if (!deepLinked) {
                // Navigate to the first destination in the graph
                // if we haven't deep linked to a destination
                mGraph.navigate(null, null);
            }
        }
    }

    /**
     * Checks the given Intent for a Navigation deep link created by
     * {@link #createDeepLink(int, Bundle)} or {@link #createDeepLinkIntent(int, Bundle)} and
     * navigates to the deep link if present. This is called automatically for you the first time
     * you set the graph, but should be manually called if your Activity receives new Intents in
     * {@link Activity#onNewIntent(Intent)}.
     *
     * <p>The {@link #getGraph() navigation graph} should be set before calling this method.</p>
     * @param intent The Intent that may contain
     * @return True if the navigation controller found a valid deep link and navigated to it.
     */
    public boolean onHandleDeepLink(Intent intent) {
        if (intent == null || intent.getExtras() == null) {
            return false;
        }
        Bundle extras = intent.getExtras();
        final int[] deepLink = extras.getIntArray(KEY_DEEP_LINK_IDS);
        if (deepLink == null || deepLink.length == 0) {
            return false;
        }
        NavGraph graph = mGraph;
        for (int i = 0; i < deepLink.length; i++) {
            int destinationId = deepLink[i];
            NavDestination node = i == 0 ? mGraph : graph.findNode(destinationId);
            if (node == null) {
                throw new IllegalStateException("unknown destination during deep link: "
                        + mContext.getResources().getResourceName(destinationId));
            }
            if (i != deepLink.length - 1) {
                // We're not at the final NavDestination yet, so keep going through the chain
                graph = (NavGraph) node;
            } else {
                // Clear the back stack, then navigate to the last NavDestination
                while (!mBackStack.isEmpty()) {
                    mBackStack.peekLast().getNavigator().popBackStack();
                }
                Bundle bundle = extras.getBundle(KEY_DEEP_LINK_EXTRAS);
                if (bundle == null) {
                    bundle = new Bundle();
                }
                bundle.putParcelable(KEY_DEEP_LINK_INTENT, intent);
                node.navigate(bundle, null);
            }
        }
        return true;
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
    public final void navigate(@IdRes int resId, Bundle args) {
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
    public void navigate(@IdRes int resId, Bundle args, NavOptions navOptions) {
        NavDestination currentNode = mBackStack.isEmpty() ? mGraph : mBackStack.peekLast();
        if (currentNode == null) {
            throw new IllegalStateException("no current navigation node");
        }
        @IdRes int destId = resId;
        final NavAction navAction = currentNode.getAction(resId);
        if (navAction != null) {
            NavOptions options = navOptions != null ? navOptions : navAction.getNavOptions();
            if (navAction.getDestinationId() == 0 && options != null && options.getPopUpTo() != 0) {
                // Allow actions to leave out a destinationId only if they have a non-zero popUpTo
                popBackStack(options.getPopUpTo(), options.isPopUpToInclusive());
                return;
            } else {
                destId = navAction.getDestinationId();
            }
        }
        NavDestination node = findDestination(destId);
        if (node == null) {
            final String dest = mContext.getResources().getResourceName(destId);
            throw new IllegalArgumentException("navigation destination " + dest
                    + (navAction != null
                    ? " referenced from action " + mContext.getResources().getResourceName(resId)
                    : "")
                    + " is unknown to this NavController");
        }
        if (navOptions != null) {
            if (navOptions.shouldClearTask()) {
                popBackStack(0, true);
            } else if (navOptions.getPopUpTo() != 0) {
                popBackStack(navOptions.getPopUpTo(), navOptions.isPopUpToInclusive());
            }
        }
        node.navigate(args, navOptions);
    }

    /**
     * Construct a PendingIntent that will deep link to the given destination.
     *
     * <p>When this deep link is triggered:
     * <ol>
     *     <li>The task is cleared.</li>
     *     <li>The only entry on the back stack is the given destination.</li>
     *     <li>Calling {@link #navigateUp()} will navigate to the parent of the destination.</li>
     * </ol></p>
     *
     * The parent of the destination is the {@link NavGraph#getStartDestination() start destination}
     * of the containing {@link NavGraph navigation graph}. In the cases where the destination is
     * the start destination of its containing navigation graph, the start destination of its
     * grandparent is used.
     *
     * @param destId destination id to deep link to
     * @return a PendingIntent constructed with
     * {@link PendingIntent#getActivity(Context, int, Intent, int)} to deep link to the
     * given destination
     */
    public PendingIntent createDeepLink(@IdRes int destId) {
        return createDeepLink(destId, null);
    }


    /**
     * Construct a PendingIntent that will deep link to the given destination.
     *
     * <p>When this deep link is triggered:
     * <ol>
     *     <li>The task is cleared.</li>
     *     <li>The only entry on the back stack is the given destination.</li>
     *     <li>Calling {@link #navigateUp()} will navigate to the parent of the destination.</li>
     * </ol></p>
     *
     * The parent of the destination is the {@link NavGraph#getStartDestination() start destination}
     * of the containing {@link NavGraph navigation graph}. In the cases where the destination is
     * the start destination of its containing navigation graph, the start destination of its
     * grandparent is used.
     *
     * @param destId destination id to deep link to
     * @param args arguments to pass to the destination
     * @return a PendingIntent constructed with
     * {@link PendingIntent#getActivity(Context, int, Intent, int)} to deep link to the
     * given destination
     */
    public PendingIntent createDeepLink(@IdRes int destId, @Nullable Bundle args) {
        Intent intent = createDeepLinkIntent(destId, args);
        return PendingIntent.getActivity(mContext, destId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }
    /**
     * Construct an Intent that will deep link to the given destination.
     *
     * <p>When this deep link is triggered:
     * <ol>
     *     <li>The task is cleared.</li>
     *     <li>The only entry on the back stack is the given destination.</li>
     *     <li>Calling {@link #navigateUp()} will navigate to the parent of the destination.</li>
     * </ol></p>
     *
     * The parent of the destination is the {@link NavGraph#getStartDestination() start destination}
     * of the containing {@link NavGraph navigation graph}. In the cases where the destination is
     * the start destination of its containing navigation graph, the start destination of its
     * grandparent is used.
     *
     * @param destId destination id to deep link to
     * @param args arguments to pass to the destination
     * @return an Intent which can be used with {@link Context#startActivity(Intent)} or
     * {@link android.app.PendingIntent#getActivity(Context, int, Intent, int)} to deep link to
     * the given destination.
     */
    public Intent createDeepLinkIntent(@IdRes int destId, @Nullable Bundle args) {
        Intent intent = new Intent();
        if (mActivity != null) {
            intent.setClassName(mContext, mActivity.getClass().getName());
        } else {
            intent = mContext.getPackageManager().getLaunchIntentForPackage(
                    mContext.getPackageName());
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        NavDestination node = findDestination(destId);
        if (node == null) {
            final String dest = mContext.getResources().getResourceName(destId);
            throw new IllegalArgumentException("navigation destination " + dest
                    + " is unknown to this NavController");
        }
        ArrayDeque<NavDestination> hierarchy = new ArrayDeque<>();
        hierarchy.add(node);
        while (hierarchy.peekFirst().getParent() != null) {
            hierarchy.addFirst(hierarchy.peekFirst().getParent());
        }
        int[] deepLinkIds = new int[hierarchy.size()];
        int index = 0;
        for (NavDestination destination : hierarchy) {
            deepLinkIds[index++] = destination.getId();
        }
        intent.putExtra(KEY_DEEP_LINK_IDS, deepLinkIds);
        if (args != null) {
            intent.putExtra(KEY_DEEP_LINK_EXTRAS, args);
        }
        return intent;
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
