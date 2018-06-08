/*
 * Copyright 2018 The Android Open Source Project
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
 * The {@link androidx.navigation.fragment.NavHostFragment} provides a
 * {@link androidx.navigation.NavHost} suitable for using
 * {@link android.support.v4.app.Fragment Fragments} as destinations in your navigation graphs via
 * &lt;fragment%gt; elements. Navigating to a Fragment will replace the contents of the
 * NavHostFragment.
 * <p>
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
 *   &lt;/fragment&gt;
 *   &lt;fragment android:id="{@literal @}+id/details_fragment"
 *       android:name="com.example.DetailsFragment"/&gt;
 * &lt;navigation /&gt;
 *
 * // File: activity_main.xml
 * &lt;fragment
 *   android:id="{@literal @}+id/my_nav_host_fragment"
 *   android:layout_width="match_parent"
 *   android:layout_height="match_parent"
 *   android:name="androidx.navigation.fragment.NavHostFragment"
 *   app:navGraph="{@literal @}xml/main_navigation"
 *   app:defaultNavHost="true"
 * /&gt;
 *
 * // File: HomeFragment.java
 * public void onViewCreated(View view, {@literal @}Nullable Bundle savedInstanceState) {
 *   // For example purposes, assume our layout created in onCreateView has a Button
 *   // that should navigate the user to a destination
 *   Button button = view.findViewById(R.id.view_details);
 *
 *   // Retrieve the NavController from any Fragment created by a NavHostFragment by passing in
 *   // this
 *   final NavController navController = NavHostFragment.findNavController(this);
 *   // Alternatively, retrieve the NavController from any View within the NavHostFragment
 *   final NavController viewNavController = Navigation.findNavController(button);
 *
 *   // And set the listener
 *   button.setOnClickListener(() -> navController.navigate(R.id.details));
 *
 *   // Or use the convenience method in Navigation to combine all of the previous steps
 *   button.setOnClickListener(Navigation.createNavigateOnClickListener(R.id.details));
 * }
 * </pre>
 */
package androidx.navigation.fragment;
