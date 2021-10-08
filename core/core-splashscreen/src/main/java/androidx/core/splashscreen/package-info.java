/*
 * Copyright 2021 The Android Open Source Project
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
 * This Splash Screen library provides compatibility support for the
 * <code>android.window.SplashScreen</code> APIs down to API 21, with support of the splash
 * screen icon from API 23.
 * <p>
 * It is composed of a compatibility theme
 * {@link androidx.core.splashscreen.R.style#Theme_SplashScreen}
 * that needs to be set as the starting theme of the activity and a programmatic API in
 * {@link androidx.core.splashscreen.SplashScreen}.
 * <p>
 * To use it, the theme of the launching Activity must inherit from
 * <code>Theme.SplashScreen</code>
 * <p>
 * <i>AndroidManifest.xml:</i>
 * <pre class="prettyprint">
 *     &lt;manifest...>
 *         &lt;application>
 *         &lt;activity>
 *              android:name=".MainActivity"
 *              android:theme="@style/Theme.App.Starting"/&gt;
 *      &lt;/manifest>
 * </pre>
 * <i>res/values/styles.xml:</i>
 * <pre class="prettyprint">
 * &lt;resources>
 *     &lt;style name="Theme.App" parent="...">
 *     ...
 *     &lt;/style>
 *
 *    &lt;style name="Theme.App.Starting" parent="Theme.SplashScreen">
 *        &lt;item name="windowSplashScreenBackground">@color/splashScreenBackground&lt;/item>
 *        &lt;item name="windowSplashScreenAnimatedIcon">@drawable/splashscreen_icon&lt;/item>
 *        &lt;item name="windowSplashScreenAnimationDuration">2000&lt;/item>
 *        &lt;item name="postSplashScreenTheme">@style/Theme.App&lt;/item>
 * &lt;/resources>
 * </pre>
 *
 * <i>MainActivity.java:</i>
 * <pre class="prettyprint">
 *     class MainActivity : Activity {
 *         fun onCreate() {
 *             super.onCreate()
 *             val splashScreen = installSplashScreen()
 *
 *             // Set the content view right after installing the splash screen
 *             setContentView(R.layout.main_activity)
 *         }
 *     }
 * </pre>
 */
package androidx.core.splashscreen;
