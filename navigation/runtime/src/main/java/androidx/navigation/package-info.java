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
 *     <li>{@link androidx.navigation.NavGraph}: A navigation graph encapsulates a set
 *     of {@link androidx.navigation.NavDestination destinations}. It can be created by
 *     inflating a navigation XML file, by constructing it programmatically,
 *     or a combination of the two.
 *     </li>
 *     <li>{@link androidx.navigation.NavController}: This is the main entry
 *     point for interacting with the Navigation Graph, translating calls to
 *     {@link androidx.navigation.NavController#navigate(int)},
 *     {@link androidx.navigation.NavController#popBackStack()}, and
 *     {@link androidx.navigation.NavController#navigateUp()} into the appropriate operations.
 *     </li>
 *     <li>{@link androidx.navigation.NavHost}: The container that hosts a
 *     {@link androidx.navigation.NavController} and provides support for one or more specific
 *     types of {@link androidx.navigation.NavDestination destinations}. For example,
 *     {@link androidx.navigation.fragment.NavHostFragment} allows you to use
 *     {@link androidx.navigation.fragment.FragmentNavigator.Destination fragment destinations}.
 *     </li>
 * </ul>
 * Below is an example of working with a NavController.
 * <pre class="prettyprint">
 * // File: HomeFragment.java
 * public void onViewCreated(View view, {@literal @}Nullable Bundle savedInstanceState) {
 *   // For example purposes, assume our layout created in onCreateView has a Button
 *   // that should navigate the user to a destination
 *   Button b = view.findViewById(R.id.view_details);
 *
 *   b.setOnClickListener(v -> {
 *       // Retrieve the NavController from any View within a NavHost
 *       NavController navController = Navigation.findNavController(v);
 *       navController.navigate(R.id.details));
 *   }
 *
 *   // Or use the convenience method in Navigation to combine the previous steps
 *   b.setOnClickListener(Navigation.createNavigateOnClickListener(R.id.details));
 * }
 * </pre>
 * <p>
 * Please see the documentation of individual classes for details.
 */
package androidx.navigation;
