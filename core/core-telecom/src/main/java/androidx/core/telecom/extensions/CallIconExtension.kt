/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.core.telecom.extensions

import android.net.Uri
import androidx.core.telecom.util.ExperimentalAppActions

/**
 * Adds support for displaying a call icon on remote surfaces for a given call.
 *
 * This interface allows a VoIP app to provide a URI representing the call icon, which can then be
 * displayed on remote surfaces (e.g., a connected car's display).
 *
 * **Important Manifest Declarations:**
 * 1. **`InCallService` Intent Filter:** VoIP apps **must** declare the following `<queries>` intent
 *    filter in their `AndroidManifest.xml` to allow remote surfaces to be discoverable when
 *    granting URI permissions: ```xml <queries> <intent> <action
 *    android:name="android.telecom.InCallService" /> </intent> </queries> ```
 * 2. **`FileProvider` Definition:** To securely share call icon images with remote surfaces, it is
 *    **strongly recommended** to use a `FileProvider`. A `FileProvider` generates `content://`
 *    URIs, which allow temporary, secure access to specific files without requiring broad file
 *    system permissions. Define a `FileProvider` in your `AndroidManifest.xml` similar to this: ```
 *    <provider android:name="androidx.core.content.FileProvider"
 *    android:authorities="androidx.core.telecom.test.fileprovider" android:exported="false"
 *    android:grantUriPermissions="true"> <meta-data
 *    android:name="android.support.FILE_PROVIDER_PATHS" android:resource="@xml/file_paths" />
 *    </provider> ``` with your app's unique authority.
 * 3. **`FileProvider` Configuration:** Create a `file_paths.xml` file in your `res/xml` directory
 *    (as referenced in the `meta-data` of the `FileProvider` definition) to specify the directories
 *    your app can share icons from: ``` <paths> <files-path name="my_images" path="images/" />
 *    </paths> ```
 *
 *    This example allows sharing files from the `images` subdirectory of your app's internal
 *    storage `files` directory. Adjust the `name` and `path` according to your needs.
 *
 * **Generating `content://` URIs:**
 *
 * In your app's code, use `FileProvider.getUriForFile()` to generate a secure `content://` URI for
 * your call icon file:
 * ```java
 * // Assuming you have a file named 'call_icon.png' in your app's internal storage
 * // directory, specifically within a subdirectory named 'images'
 * File iconFile = new File(getFilesDir(), "images/call_icon.png");
 *
 * Uri iconUri = FileProvider.getUriForFile(
 *         context,
 *         "androidx.core.telecom.test.fileprovider", // Your FileProvider authority
 *         iconFile
 * );
 * ```
 *
 * **Using the `content://` URI:**
 *
 * Pass the generated `content://` URI to the `addCallIconExtension` method when setting up your
 * call. Remote surfaces will then be able to securely access and display the icon.
 *
 * Failure to properly declare the `InCallService` intent filter or to use a `FileProvider` for icon
 * sharing will prevent remote surfaces from displaying the call icon.
 *
 * @see ExtensionInitializationScope.addCallIconExtension
 * @see [androidx.core.content.FileProvider]
 */
@ExperimentalAppActions
public interface CallIconExtension {

    /**
     * Updates the call icon displayed on remote surfaces.
     *
     * Call this function whenever the call icon changes. The provided `iconUri` will be used to
     * fetch and display the new icon.
     *
     * @param iconUri The `Uri` representing the new call icon. If the content of the URI has
     *   changed, but the URI hasn't, there is no need to call this API.
     */
    public suspend fun updateCallIconUri(iconUri: Uri)
}
