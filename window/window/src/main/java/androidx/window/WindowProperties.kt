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
}