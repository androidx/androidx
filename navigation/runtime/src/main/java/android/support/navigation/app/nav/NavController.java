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

import java.util.ArrayList;
import java.util.HashMap;
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
    /**
     * Metadata key for defining an app's default navigation graph.
     *
     * <p>Applications may declare a graph resource in their manifest instead of declaring
     * or passing this data to each host or controller:</p>
     *
     * <pre class="prettyprint">
     *     <meta-data android:name="android.nav.graph" android:resource="@xml/my_nav_graph" />
     * </pre>
     *
     * <p>A graph resource declared in this manner can be inflated into a controller by calling
     * {@link #addMetadataGraph()}. Navigation host implementations should do this automatically
     * if no navigation resource is otherwise supplied during host configuration.</p>
     */
    public static final String METADATA_KEY_GRAPH = "android.nav.graph";

    private static final String KEY_GRAPH_ID = "android-support-nav:controller:graphId";
    private static final String KEY_START_DEST_ID = "android-support-nav:controller:startDestId";
    private static final String KEY_CUR_DEST_ID = "android-support-nav:controller:curDestId";

    private Context mContext;
    private NavInflater mInflater;
    private NavGraph mGraph;
    private int mGraphId;
    private NavDestination mCurrentNode;
    private int mStartDestId;

    private final HashMap<String, Navigator> mNavigators = new HashMap<>();
    private final ArrayList<Navigator> mNavigatorBackStack = new ArrayList<>();

    private final Navigator.OnNavigatorNavigatedListener mOnNavigatedListener =
            new Navigator.OnNavigatorNavigatedListener() {
                @Override
                public void onNavigatorNavigated(Navigator navigator, @IdRes int destId,
                                                 boolean isBackStackEmpty) {
                    if (destId != 0) {
                        NavDestination newDest = mGraph.findNode(destId);
                        if (newDest == null) {
                            throw new IllegalArgumentException("Navigator " + navigator
                                    + " reported navigation to unknown destination id "
                                    + mContext.getResources().getResourceName(destId));
                        }
                        mCurrentNode = newDest;
                        dispatchOnNavigated(newDest);
                    }
                    if (isBackStackEmpty) {
                onNavigatorBackStackEmpty(navigator);
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
        final ActivityNavigator activityNavigator = new ActivityNavigator(mContext);
        mNavigators.put(ActivityNavigator.NAME, activityNavigator);
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
        final ActivityNavigator activityNavigator = new ActivityNavigator(activity);
        mNavigators.put(ActivityNavigator.NAME, activityNavigator);

        // The host activity is always at the root.
        mNavigatorBackStack.add(activityNavigator);
    }

    /**
     * Adds an {@link OnNavigatedListener} to this controller to receive events when
     * the controller navigates to a new destination.
     *
     * @param listener the listener to receive events
     */
    public void addOnNavigatedListener(OnNavigatedListener listener) {
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
        if (mNavigatorBackStack.isEmpty()) {
            throw new IllegalArgumentException("NavController back stack is empty");
        }
        return mNavigatorBackStack.get(mNavigatorBackStack.size() - 1).popBackStack();
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
    public Navigator getNavigator(String name) {
        if (TextUtils.isEmpty(name)) {
            throw new IllegalArgumentException("navigator name cannot be null");
        }

        return mNavigators.get(name);
    }

    @Override
    public void addNavigator(String name, Navigator navigator) {
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
     * Add a {@link NavGraph navigation graph} as specified in the application manifest.
     *
     * <p>Applications may declare a graph resource in their manifest instead of declaring
     * or passing this data to each host or controller:</p>
     *
     * <pre class="prettyprint">
     *     <meta-data android:name="android.nav.graph" android:resource="@xml/my_nav_graph" />
     * </pre>
     *
     * @see #METADATA_KEY_GRAPH
     */
    public void addMetadataGraph() {
        final Bundle metaData = mContext.getApplicationInfo().metaData;
        if (metaData != null) {
            final int resid = metaData.getInt(METADATA_KEY_GRAPH);
            if (resid != 0) {
                addGraph(resid);
            }
        }
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
     * @param resid resource id of the navigation graph to inflate
     *
     * @see #getNavInflater()
     * @see #setGraph(NavGraph)
     */
    public void setGraph(@XmlRes int resid) {
        mGraph = getNavInflater().inflate(resid);
        mGraphId = resid;
    }

    /**
     * Sets the {@link NavGraph navigation graph} to the specified resource.
     * Any current navigation graph data will be replaced.
     *
     * @param graph graph to set
     * @see #setGraph(int)
     */
    public void setGraph(NavGraph graph) {
        mGraph = graph;
        mGraphId = 0;
    }

    /**
     * Adds the contents of a navigation resource to the current navigation graph.
     *
     * @param resid resource id of the navigation graph to inflate
     *
     * @see #getNavInflater()
     * @see #setGraph(NavGraph)
     */
    public void addGraph(@XmlRes int resid) {
        NavInflater inflater = getNavInflater();
        NavGraph newGraph = inflater.inflate(resid);
        if (mGraph != null) {
            mGraph.addAll(newGraph);
            mGraphId = 0;
        } else {
            mGraph = newGraph;
            mGraphId = resid;
        }
    }

    /**
     * Adds the contents of a navigation resource to the current navigation graph.
     *
     * @param graph graph to merge into this controller's graph
     */
    public void addGraph(NavGraph graph) {
        if (mGraph == null) {
            mGraph = new NavGraph();
        }
        mGraph.addAll(graph);
        mGraphId = 0;
    }

    /**
     * Sets the starting navigation destination for this controller.
     *
     * @param resid destination id to set
     */
    public void setStartDestination(@IdRes int resid) {
        mStartDestId = resid;
        if (mCurrentNode == null) {
            navigateTo(resid);
        }
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
        NavDestination node = mGraph.findNode(resid);
        if (node == null) {
            final String dest = mContext.getResources().getResourceName(resid);
            throw new IllegalArgumentException("navigation destination " + dest
                    + " is unknown to this NavController");
        }
        node.navigate(args, navOptions);
    }

    /**
     * Navigate via an action defined on the current destination.
     *
     * <p>Requests navigation to the given {@link NavDestination#getActionDestination(int) action},
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
     * <p>Requests navigation to the given {@link NavDestination#getActionDestination(int) action},
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
     * <p>Requests navigation to the given {@link NavDestination#getActionDestination(int) action},
     * appropriate for the current location, e.g. "next" or "home."</p>
     *
     * @param action navigation action to invoke
     * @param args arguments to pass to the destination
     * @param navOptions special options for this navigation operation
     */
    public void navigate(@IdRes int action, Bundle args, NavOptions navOptions) {
        if (mCurrentNode == null) {
            throw new IllegalStateException("no current navigation node");
        }
        final int dest = mCurrentNode.getActionDestination(action);
        if (dest == 0) {
            final Resources res = mContext.getResources();
            throw new IllegalStateException("no destination defined from "
                    + res.getResourceName(mCurrentNode.getId())
                    + " for action " + res.getResourceName(action));
        }
        navigateTo(dest, args, navOptions);
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
        if (mStartDestId != 0) {
            if (b == null) {
                b = new Bundle();
            }
            b.putInt(KEY_START_DEST_ID, mStartDestId);
        }
        if (mCurrentNode != null) {
            if (b == null) {
                b = new Bundle();
            }
            b.putInt(KEY_CUR_DEST_ID, mCurrentNode.getId());
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
            setGraph(mGraphId);
        }
        mStartDestId = navState.getInt(KEY_START_DEST_ID);

        // Restore the current location first, or setStartDestination will perform navigation
        // if mCurrentNode is null.
        final int loc = navState.getInt(KEY_CUR_DEST_ID);
        if (loc != 0) {
            NavDestination node = mGraph.findNode(loc);
            if (node == null) {
                throw new IllegalStateException("unknown current destination during restore: "
                        + mContext.getResources().getResourceName(loc));
            }
            mCurrentNode = node;
        }
        if (mStartDestId != 0) {
            setStartDestination(mStartDestId);
        }
    }

    void onNavigatorBackStackEmpty(Navigator emptyNavigator) {
        if (mNavigatorBackStack.isEmpty()) {
            throw new IllegalStateException("Navigator " + emptyNavigator
                    + " reported empty, but this NavController has no back stack!");
        }
        // If a navigator's back stack is empty, it can't have any presence in our
        // navigator stack either. Remove all instances of it.
        for (int i = mNavigatorBackStack.size() - 1; i >= 0; i--) {
            final Navigator n = mNavigatorBackStack.get(i);
            if (n == emptyNavigator) {
                mNavigatorBackStack.remove(i);
            }
        }
    }
}
