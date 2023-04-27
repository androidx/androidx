/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.window

/**
 * Window-related
 * [PackageManager.Property][android.content.pm.PackageManager.Property] tags
 * that can be defined in the app manifest file, `AndroidManifest.xml`.
 */
object WindowProperties {
    /**
     * Application-level
     * [PackageManager.Property][android.content.pm.PackageManager.Property] tag
     * that specifies whether OEMs are permitted to provide activity embedding
     * split-rule configurations on behalf of the app.
     *
     * If `true`, the system is permitted to override the app's windowing
     * behavior and implement activity embedding split rules, such as displaying
     * activities adjacent to each other. A system override informs the app that
     * the activity embedding APIs are disabled so the app will not provide its
     * own activity embedding rules, which would conflict with the system's
     * rules.
     *
     * If `false`, the system is not permitted to override the windowing
     * behavior of the app. Set the property to `false` if the app provides its
     * own activity embedding split rules, or if you want to prevent the system
     * override for any other reason.
     *
     * The default value is `false`.
     *
     * <p class="note"><b>Note:</b> Refusal to permit the system override is not
     * enforceable. OEMs can override the app's activity embedding
     * implementation whether or not this property is specified and set to
     * <code>false</code>. The property is, in effect, a hint to OEMs.</p>
     *
     * OEMs can implement activity embedding on any API level. The best practice
     * for apps is to always explicitly set this property in the app manifest
     * file regardless of targeted API level rather than rely on the default
     * value.
     *
     * **Syntax:**
     * <pre>
     * &lt;application&gt;
     *   &lt;property
     *     android:name="android.window.PROPERTY_ACTIVITY_EMBEDDING_ALLOW_SYSTEM_OVERRIDE"
     *     android:value="true|false"/&gt;
     * &lt;/application&gt;
     * </pre>
     */
    const val PROPERTY_ACTIVITY_EMBEDDING_ALLOW_SYSTEM_OVERRIDE =
        "android.window.PROPERTY_ACTIVITY_EMBEDDING_ALLOW_SYSTEM_OVERRIDE"

    /**
     * Application level
     * [PackageManager][android.content.pm.PackageManager.Property] tag
     * that an app must specify to inform the system that the app is ActivityEmbedding
     * split feature enabled. In other words, the ActivityEmbedding splits feature cannot be
     * used if the app has no property set.
     *
     * With this property, the system could provide custom behaviors for the apps that
     * have ActivityEmbedding split feature enabled. For example, the fixed-portrait orientation
     * requests of the activities could be ignored by the system in order to provide seamless
     * ActivityEmbedding split experiences while holding the large-screen devices in landscape mode.
     *
     * **Syntax:**
     * <pre>
     * &lt;application&gt;
     *   &lt;property
     *     android:name="android.window.PROPERTY_ACTIVITY_EMBEDDING_SPLITS_ENABLED"
     *     android:value="true|false"/&gt;
     * &lt;/application&gt;
     * </pre>
     */
    const val PROPERTY_ACTIVITY_EMBEDDING_SPLITS_ENABLED =
        "android.window.PROPERTY_ACTIVITY_EMBEDDING_SPLITS_ENABLED"

    /**
     * Application level
     * [PackageManager][android.content.pm.PackageManager.Property] tag
     * for an app to inform the system that the app can be opted-out from the compatibility
     * treatment that avoids [android.app.Activity.setRequestedOrientation] loops. The loop
     * can be trigerred when ignoreOrientationRequest display setting is
     * enabled on the device (enables compatibility mode for fixed orientation,
     * see [Enhanced letterboxing](https://developer.android.com/guide/practices/enhanced-letterboxing)
     * for more details). or by the landscape natural orientation of the device.
     *
     *
     * The system could ignore [android.app.Activity.setRequestedOrientation]
     * call from an app if both of the following conditions are true:
     *  * Activity has requested orientation more than 2 times within 1-second timer
     *  * Activity is not letterboxed for fixed orientation
     *
     * Setting this property to `false` informs the system that the app must be
     * opted-out from the compatibility treatment even if the device manufacturer has opted the app
     * into the treatment.
     *
     * Not setting this property at all, or setting this property to `true` has no effect.
     *
     * **Syntax:**
     * ```
     * <application>
     *   <property
     *     android:name="android.window.PROPERTY_COMPAT_ALLOW_IGNORING_ORIENTATION_REQUEST_WHEN_LOOP_DETECTED"
     *     android:value="false" />
     * </application>
     * ```
     */
    // TODO(b/274924641): Make property public
    const val PROPERTY_COMPAT_ALLOW_IGNORING_ORIENTATION_REQUEST_WHEN_LOOP_DETECTED =
        "android.window.PROPERTY_COMPAT_ALLOW_IGNORING_ORIENTATION_REQUEST_WHEN_LOOP_DETECTED"

    /**
     * Application level
     * [PackageManager][android.content.pm.PackageManager.Property] tag
     * for an app to inform the system that the app should be opted-out from the compatibility
     * override that changes the min aspect ratio.
     *
     * When this compat override is enabled the min aspect ratio given in the app's manifest can
     * be overridden by the device manufacturer using their discretion to improve display
     * compatibility unless the app's manifest value is higher. This treatment will also apply if
     * no min aspect ratio value is provided in the manifest. These treatments can apply only in
     * specific cases (e.g. device is in portrait) or each time the app is displayed on screen.
     *
     * Setting this property to `false` informs the system that the app must be
     * opted-out from the compatibility treatment even if the device manufacturer has opted the app
     * into the treatment.
     *
     * Not setting this property at all, or setting this property to `true` has no effect.
     *
     * **Syntax:**
     * ```
     * <application>
     *   <property
     *     android:name="android.window.PROPERTY_COMPAT_ALLOW_MIN_ASPECT_RATIO_OVERRIDE"
     *     android:value="false" />
     * </application>
     * ```
     */
    const val PROPERTY_COMPAT_ALLOW_MIN_ASPECT_RATIO_OVERRIDE =
        "android.window.PROPERTY_COMPAT_ALLOW_MIN_ASPECT_RATIO_OVERRIDE"
}