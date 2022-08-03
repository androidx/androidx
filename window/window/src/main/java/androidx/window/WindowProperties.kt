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
 * Window related [android.content.pm.PackageManager.Property] tags for developers to define in app
 * AndroidManifest.
 */
object WindowProperties {
    /**
     * Application level [android.content.pm.PackageManager.Property] tag for developers to
     * provide consent for their app to allow OEMs to manually provide ActivityEmbedding split
     * rule configuration on behalf of the app.
     * <p>If `true`, the system can override the windowing behaviors for the app, such as
     * showing some activities side-by-side. In this case, it will report that ActivityEmbedding
     * APIs are disabled for the app to avoid conflict.
     * <p>If `false`, the system can't override the window behavior for the app. It should
     * be used if the app wants to provide their own ActivityEmbedding split rules, or if the
     * app wants to opt-out of system overrides for any other reason.
     * <p>Default is `false`.
     * <p>The system enforcement is added in Android 14, but some devices may start following the
     * requirement before that. The best practice for apps is to always explicitly set this
     * property in AndroidManifest instead of relying on the default value.
     * <p>Example usage:
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
}