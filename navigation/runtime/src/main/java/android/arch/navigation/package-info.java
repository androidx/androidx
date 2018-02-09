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

/**
 * Navigation is a framework for navigating between 'destinations' within an Android
 * application that provides a consistent API whether destinations are implemented as
 * {@link android.support.v4.app.Fragment Fragments}, {@link android.app.Activity Activities}, or
 * other components.
 * <p>
 * There are 3 major components in Navigation.
 * <ul>
 *     <li>{@link android.arch.navigation.NavGraph}: A navigation graph encapsulates a set
 *     of {@link android.arch.navigation.NavDestination destinations}. It can be created by
 *     inflating a navigation XML file, by constructing it programmatically,
 *     or a combination of the two.
 *     </li>
 *     <li>{@link android.arch.navigation.NavController}: This is the main entry
 *     point for interacting with the Navigation Graph, translating calls to
 *     {@link android.arch.navigation.NavController#navigate(int)},
 *     {@link android.arch.navigation.NavController#popBackStack()}, and
 *     {@link android.arch.navigation.NavController#navigateUp()} into the appropriate operations.
 *     </li>
 *     <li>{@link android.arch.navigation.NavHostFragment}: The NavHostFragment provides a
 *     {@link android.arch.navigation.NavController} that supports
 *     {@link android.arch.navigation.FragmentNavigator.Destination fragment destinations}.
 *     </li>
 * </ul>
 * Below is a minimal implementation.
 * <pre class="prettyprint">
 * // File: res/xml/main_navigation.xml
 * &lt;navigation xmlns:android="http://schemas.android.com/apk/res/android"
 *     xmlns:app="http://schemas.android.com/apk/res-auto"
 *     app:startDestination="{@literal @}+id/home_fragment"&gt;
 *   &lt;fragment android:id="{@literal @}+id/home_fragment"
 *       android:name="com.example.HomeFragment"&gt;
 *     &lt;action android:id="{@literal @}+id/details"
 *       app:destination="{@literal @}+id/details_fragment" /&gt;
 *   &lt;fragment /&gt;
 *   &lt;fragment android:id="{@literal @}+id/details_fragment"
 *       android:name="com.example.DetailsFragment"/&gt;
 * &lt;navigation /&gt;
 *
 * // File: activity_main.xml
 * &lt;fragment
 *   android:id="{@literal @}+id/my_nav_host_fragment"
 *   android:layout_width="match_parent"
 *   android:layout_height="match_parent"
 *   android:name="android.arch.navigation.NavHostFragment"
 *   app:navGraph="{@literal @}xml/main_navigation"
 *   app:defaultNavHost="true"
 * /&gt;
 *
 * // File: HomeFragment.java
 * public void onViewCreated(View view, {@literal @}Nullable Bundle savedInstanceState) {
 *   // For example purposes, assume our layout created in onCreateView has a Button
 *   // that should navigate the user to a destination
 *   Button b = view.findViewById(R.id.view_details);
 *
 *   // Retrieve the NavController from any Fragment created by a NavHostFragment by passing in
 *   // this
 *   final NavController navController = Navigation.findNavController(this);
 *   // Alternatively, you can use findNavController(view) with any View within the NavHostFragment
 *
 *   // And set the listener
 *   b.setOnClickListener(() -%gt; navController.navigate(R.id.details));
 *
 *   // Or use the convenience method in Navigation to combine all of the previous steps
 *   b.setOnClickListener(Navigation.createNavigateOnClickListener(R.id.details));
 * }
 * </pre>
 * <p>
 * Please see the documentation of individual classes for details.
 */
package android.arch.navigation;
