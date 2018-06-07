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
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NavigationRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.TaskStackBuilder;

import java.util.ArrayDeque;

/**
 * Class used to construct deep links to a particular destination in a {@link NavGraph}.
 *
 * <p>When this deep link is triggered:
 * <ol>
 *     <li>The task is cleared.</li>
 *     <li>The destination and all of its parents will be on the back stack.</li>
 *     <li>Calling {@link NavController#navigateUp()} will navigate to the parent of the
 *     destination.</li>
 * </ol></p>
 *
 * The parent of the destination is the {@link NavGraph#getStartDestination() start destination}
 * of the containing {@link NavGraph navigation graph}. In the cases where the destination is
 * the start destination of its containing navigation graph, the start destination of its
 * grandparent is used.
 * <p>
 * You can construct an instance directly with {@link #NavDeepLinkBuilder(Context)} or build one
 * using an existing {@link NavController} via {@link NavController#createDeepLink()}.
 */
public class NavDeepLinkBuilder {
    private final Context mContext;
    private final Intent mIntent;

    private NavGraph mGraph;
    private int mDestId;

    /**
     * Construct a new NavDeepLinkBuilder.
     *
     * If the context passed in here is not an {@link Activity}, this method will use
     * {@link android.content.pm.PackageManager#getLaunchIntentForPackage(String)} as the
     * default activity to launch, if available.
     *
     * @param context Context used to create deep links
     * @see #setComponentName
     */
    public NavDeepLinkBuilder(@NonNull Context context) {
        mContext = context;
        if (mContext instanceof Activity) {
            mIntent = new Intent(mContext, mContext.getClass());
        } else {
            Intent launchIntent = mContext.getPackageManager().getLaunchIntentForPackage(
                    mContext.getPackageName());
            mIntent = launchIntent != null ? launchIntent : new Intent();
        }
        mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    }

    /**
     * @see NavController#createDeepLink()
     */
    NavDeepLinkBuilder(@NonNull NavController navController) {
        this(navController.getContext());
        mGraph = navController.getGraph();
    }

    /**
     * Sets an explicit Activity to be started by the deep link created by this class.
     *
     * @param activityClass The Activity to start. This Activity should have a {@link NavController}
     *                      which uses the same {@link NavGraph} used to construct this
     *                      deep link.
     * @return this object for chaining
     */
    @NonNull
    public NavDeepLinkBuilder setComponentName(@NonNull Class<? extends Activity> activityClass) {
        return setComponentName(new ComponentName(mContext, activityClass));
    }

    /**
     * Sets an explicit Activity to be started by the deep link created by this class.
     *
     * @param componentName The Activity to start. This Activity should have a {@link NavController}
     *                      which uses the same {@link NavGraph} used to construct this
     *                      deep link.
     * @return this object for chaining
     */
    @NonNull
    public NavDeepLinkBuilder setComponentName(@NonNull ComponentName componentName) {
        mIntent.setComponent(componentName);
        return this;
    }

    /**
     * Sets the graph that contains the {@link #setDestination(int) deep link destination}.
     *
     * @param navGraphId ID of the {@link NavGraph} containing the deep link destination
     * @return this object for chaining
     */
    @NonNull
    public NavDeepLinkBuilder setGraph(@NavigationRes int navGraphId) {
        return setGraph(new NavInflater(mContext, new PermissiveNavigatorProvider(mContext))
                .inflate(navGraphId));
    }

    /**
     * Sets the graph that contains the {@link #setDestination(int) deep link destination}.
     *
     * @param navGraph The {@link NavGraph} containing the deep link destination
     * @return this object for chaining
     */
    @NonNull
    public NavDeepLinkBuilder setGraph(@NonNull NavGraph navGraph) {
        mGraph = navGraph;
        if (mDestId != 0) {
            fillInIntent();
        }
        return this;
    }

    /**
     * Sets the destination id to deep link to.
     *
     * @param destId destination ID to deep link to.
     * @return this object for chaining
     */
    @NonNull
    public NavDeepLinkBuilder setDestination(@IdRes int destId) {
        mDestId = destId;
        if (mGraph != null) {
            fillInIntent();
        }
        return this;
    }

    private void fillInIntent() {
        NavDestination node = null;
        ArrayDeque<NavDestination> possibleDestinations = new ArrayDeque<>();
        possibleDestinations.add(mGraph);
        while (!possibleDestinations.isEmpty() && node == null) {
            NavDestination destination = possibleDestinations.poll();
            if (destination.getId() == mDestId) {
                node = destination;
            } else if (destination instanceof NavGraph) {
                for (NavDestination child : (NavGraph) destination) {
                    possibleDestinations.add(child);
                }
            }
        }
        if (node == null) {
            final String dest = NavDestination.getDisplayName(mContext, mDestId);
            throw new IllegalArgumentException("navigation destination " + dest
                    + " is unknown to this NavController");
        }
        mIntent.putExtra(NavController.KEY_DEEP_LINK_IDS, node.buildDeepLinkIds());
    }

    /**
     * Set optional arguments to send onto the destination
     * @param args arguments to pass to the destination
     * @return this object for chaining
     */
    @NonNull
    public NavDeepLinkBuilder setArguments(@Nullable Bundle args) {
        mIntent.putExtra(NavController.KEY_DEEP_LINK_EXTRAS, args);
        return this;
    }

    /**
     * Construct the full {@link TaskStackBuilder task stack} needed to deep link to the given
     * destination.
     * <p>
     * You must have {@link #setGraph set a NavGraph} and {@link #setDestination set a destination}
     * before calling this method.
     * </p>
     *
     * @return a {@link TaskStackBuilder} which can be used to
     * {@link TaskStackBuilder#startActivities() send the deep link} or
     * {@link TaskStackBuilder#getPendingIntent(int, int) create a PendingIntent} to deep link to
     * the given destination.
     */
    @NonNull
    public TaskStackBuilder createTaskStackBuilder() {
        if (mIntent.getIntArrayExtra(NavController.KEY_DEEP_LINK_IDS) == null) {
            if (mGraph == null) {
                throw new IllegalStateException("You must call setGraph() "
                        + "before constructing the deep link");
            } else {
                throw new IllegalStateException("You must call setDestination() "
                        + "before constructing the deep link");
            }
        }
        // We create a copy of the Intent to ensure the Intent does not have itself
        // as an extra. This also prevents developers from modifying the internal Intent
        // via taskStackBuilder.editIntentAt()
        TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(mContext)
                .addNextIntentWithParentStack(new Intent(mIntent));
        for (int index = 0; index < taskStackBuilder.getIntentCount(); index++) {
            // Attach the original Intent to each Activity so that they can know
            // they were constructed in response to a deep link
            taskStackBuilder.editIntentAt(index)
                    .putExtra(NavController.KEY_DEEP_LINK_INTENT, mIntent);
        }
        return taskStackBuilder;
    }

    /**
     * Construct a {@link PendingIntent} to the {@link #setDestination(int) deep link destination}.
     * <p>
     * This constructs the entire {@link #createTaskStackBuilder() task stack} needed.
     * <p>
     * You must have {@link #setGraph set a NavGraph} and {@link #setDestination set a destination}
     * before calling this method.
     * </p>
     *
     * @return a PendingIntent constructed with
     * {@link TaskStackBuilder#getPendingIntent(int, int)} to deep link to the
     * given destination
     */
    @NonNull
    public PendingIntent createPendingIntent() {
        return createTaskStackBuilder()
                .getPendingIntent(mDestId, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * A {@link NavigatorProvider} that only parses the basics: {@link NavGraph navigation graphs}
     * and {@link NavDestination destinations}, effectively only getting the base destination
     * information.
     */
    @SuppressWarnings("unchecked")
    private static class PermissiveNavigatorProvider extends SimpleNavigatorProvider {
        /**
         * A Navigator that only parses the {@link NavDestination} attributes.
         */
        private final Navigator<NavDestination> mDestNavigator = new Navigator<NavDestination>() {
            @NonNull
            @Override
            public NavDestination createDestination() {
                return new NavDestination(this);
            }

            @Override
            public void navigate(@NonNull NavDestination destination, @Nullable Bundle args,
                    @Nullable NavOptions navOptions) {
                throw new IllegalStateException("navigate is not supported");
            }

            @Override
            public boolean popBackStack() {
                throw new IllegalStateException("popBackStack is not supported");
            }
        };

        PermissiveNavigatorProvider(Context context) {
            addNavigator(new NavGraphNavigator(context));
        }

        @NonNull
        @Override
        public Navigator<? extends NavDestination> getNavigator(@NonNull String name) {
            try {
                return super.getNavigator(name);
            } catch (IllegalStateException e) {
                return mDestNavigator;
            }
        }
    }
}
