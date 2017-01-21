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
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.annotation.XmlRes;
import android.support.v4.app.Fragment;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.android.support.navigation.R;

/**
 * NavHostFragment provides an area within your layout for self-contained navigation to occur.
 *
 * <p>NavHostFragment is intended to be used as the content area within a layout resource
 * defining your app's chrome around it, e.g.:</p>
 *
 * <pre class="prettyprint">
 *     <android.support.v4.widget.DrawerLayout
 *             xmlns:android="http://schemas.android.com/apk/res/android"
 *             xmlns:app="http://schemas.android.com/apk/res-auto"
 *             android:layout_width="match_parent"
 *             android:layout_height="match_parent">
 *         <fragment
 *                 android:layout_width="match_parent"
 *                 android:layout_height="match_parent"
 *                 android:id="@+id/my_nav_host_fragment"
 *                 android:name="android.support.navigation.app.nav.NavHostFragment"
 *                 app:navGraph="@xml/nav_sample"
 *                 app:startDestination="@+id/launcher_home"
 *                 app:defaultNavHost="true" />
 *         <android.support.design.widget.NavigationView
 *                 android:layout_width="wrap_content"
 *                 android:layout_height="match_parent"
 *                 android:layout_gravity="start"/>
 *     </android.support.v4.widget.DrawerLayout>
 * </pre>
 *
 * <p>Each NavHostFragment has a {@link NavController} that defines valid navigation within
 * the navigation host. This includes the {@link NavGraph navigation graph} as well as navigation
 * state such as current location and back stack that will be saved and restored along with the
 * NavHostFragment itself.</p>
 *
 * <p>NavHostFragments register their navigation controller at the root of their view subtree
 * such that any descendant can obtain the controller instance through the {@link Navigation}
 * helper class's methods such as {@link Navigation#findController(View)}. View event listener
 * implementations such as {@link android.view.View.OnClickListener} within navigation destination
 * fragments can use these helpers to navigate based on user interaction without creating a tight
 * coupling to the navigation host.</p>
 */
public class NavHostFragment extends Fragment {
    private static final String KEY_GRAPH_ID = "android-support-nav:fragment:graphId";
    private static final String KEY_START_DEST_ID = "android-support-nav:fragment:startDestId";
    private static final String KEY_NAV_CONTROLLER_STATE =
            "android-support-nav:fragment:navControllerState";
    private static final String KEY_DEFAULT_NAV_HOST = "android-support-nav:fragment:defaultHost";

    private NavController mNavController;

    // State that will be saved and restored
    private boolean mDefaultNavHost;

    /**
     * Create a new NavHostFragment instance with an inflated {@link NavGraph} resource.
     *
     * @param graphRes resource id of the navigation graph to inflate
     * @return a new NavHostFragment instance
     */
    public static NavHostFragment create(@XmlRes int graphRes) {
        return create(graphRes, 0);
    }

    /**
     * Create a new NavHostFragment instance with an inflated {@link NavGraph} resource
     * and a starting destination id.
     *
     * @param graphRes resource id of the navigation graph to inflate
     * @param startDestinationRes id of the initial destination
     * @return a new NavHostFragment instance
     */
    public static NavHostFragment create(@XmlRes int graphRes, @IdRes int startDestinationRes) {
        Bundle b = null;
        if (graphRes != 0) {
            b = new Bundle();
            b.putInt(KEY_GRAPH_ID, graphRes);
        }
        if (startDestinationRes != 0) {
            if (b == null) {
                b = new Bundle();
            }
            b.putInt(KEY_START_DEST_ID, startDestinationRes);
        }

        final NavHostFragment result = new NavHostFragment();
        if (b != null) {
            result.setArguments(b);
        }
        return result;
    }

    /**
     * Returns the {@link NavController navigation controller} for this navigation host.
     * This method will return null until this host fragment's {@link #onCreate(Bundle)}
     * has been called and it has had an opportunity to restore from a previous instance state.
     *
     * @return this host's navigation controller
     */
    @Nullable
    public NavController getNavController() {
        return mNavController;
    }

    /**
     * Set a {@link NavGraph} for this navigation host's {@link NavController} by resource id.
     * The existing graph will be replaced.
     *
     * @param graphRes resource id of the navigation graph to inflate
     */
    public void setGraph(@XmlRes int graphRes) {
        if (mNavController == null) {
            Bundle args = getArguments();
            if (args == null) {
                args = new Bundle();
            }
            args.putInt(KEY_GRAPH_ID, graphRes);
            setArguments(args);
        } else {
            mNavController.setGraph(graphRes);
        }
    }

    /**
     * Set a starting destination id for this navigation host.
     * If this host has not navigated to a destination yet, the host will navigate to the start
     * destination. The initial navigation to the starting destination is not considered part of the
     * {@link #getNavController() host controller's} back stack.
     *
     * @param destRes id of the initial destination
     */
    public void setStartDestination(@IdRes int destRes) {
        if (mNavController == null) {
            Bundle args = getArguments();
            if (args == null) {
                args = new Bundle();
            }
            args.putInt(KEY_START_DEST_ID, destRes);
            setArguments(args);
        } else {
            mNavController.setStartDestination(destRes);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        // TODO This feature should probably be a first-class feature of the Fragment system,
        // but it can stay here until we can add the necessary attr resources to
        // the fragment lib.
        if (mDefaultNavHost) {
            getFragmentManager().beginTransaction().setPrimaryNavigationFragment(this).commit();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Context context = getContext();
        final Activity activity = getActivity();

        if (activity != null) {
            mNavController = new NavController(activity);
        } else {
            mNavController = new NavController(context);
        }
        mNavController.addNavigator(FragmentNavigator.NAME,
                new FragmentNavigator(context, getChildFragmentManager(), getId()));

        Bundle navState = null;
        if (savedInstanceState != null) {
            navState = savedInstanceState.getBundle(KEY_NAV_CONTROLLER_STATE);
            if (savedInstanceState.getBoolean(KEY_DEFAULT_NAV_HOST, false)) {
                mDefaultNavHost = true;
                getFragmentManager().beginTransaction().setPrimaryNavigationFragment(this).commit();
            }
        }

        if (navState != null) {
            // Navigation controller state overrides arguments
            mNavController.restoreState(navState);
        } else {
            final Bundle args = getArguments();
            final int graphid = args.getInt(KEY_GRAPH_ID);
            final int destid = args.getInt(KEY_START_DEST_ID);
            if (graphid != 0) {
                mNavController.setGraph(graphid);
            } else {
                mNavController.addMetadataGraph();
            }
            if (destid != 0) {
                mNavController.setStartDestination(destid);
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return new FrameLayout(inflater.getContext());
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (!(view instanceof ViewGroup)) {
            throw new IllegalStateException("created host view " + view + " is not a ViewGroup");
        }
        Navigation.setViewNavController(view, mNavController);
    }

    @Override
    public void onInflate(Context context, AttributeSet attrs, Bundle savedInstanceState) {
        super.onInflate(context, attrs, savedInstanceState);

        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.NavHostFragment);
        final int graphid = a.getResourceId(R.styleable.NavHostFragment_navGraph, 0);
        final int destid = a.getResourceId(R.styleable.NavHostFragment_startDestination, 0);
        final boolean defaultHost = a.getBoolean(R.styleable.NavHostFragment_defaultNavHost, false);

        if (graphid != 0) {
            setGraph(graphid);
        }
        if (destid != 0) {
            setStartDestination(destid);
        }
        if (defaultHost) {
            mDefaultNavHost = true;
            if (isAdded()) {
                getFragmentManager().beginTransaction().setPrimaryNavigationFragment(this).commit();
            }
        }
        a.recycle();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Bundle navState = mNavController.saveState();
        if (navState != null) {
            outState.putBundle(KEY_NAV_CONTROLLER_STATE, navState);
        }
        if (mDefaultNavHost) {
            outState.putBoolean(KEY_DEFAULT_NAV_HOST, true);
        }
    }
}
