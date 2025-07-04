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

package androidx.browser.customtabs;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Represents the data passed by a browser when a custom content action is triggered
 * from a Custom Tab.
 * When you add custom content actions using
 * {@link CustomTabsIntent.Builder#addCustomContentAction(CustomContentAction)},
 * the browser sends the {@link android.app.PendingIntent} associated with the selected action
 * when the user triggers it. The browser includes specific extras in the {@link Intent}
 * carried by the {@link android.app.PendingIntent}.
 * This class provides a convenient way to access these extras, such as the current URL,
 * the type of content interacted with, the ID of the triggered action, and potentially
 * details about the specific element (like an image or link).
 */
@ExperimentalCustomContentAction
public final class ContentActionSelectedData {

    private final Intent mIntent;

    private ContentActionSelectedData(@NonNull Intent intent) {
        mIntent = intent;
    }

    /**
     * Creates a {@link ContentActionSelectedData} instance from the given {@link Intent}.
     *
     * @param intent The intent received from the browser's custom content action.
     *               This intent is expected to contain the relevant extras.
     * @return A {@link ContentActionSelectedData} instance, or {@code null} if the
     * provided intent is null.
     */
    public static @Nullable ContentActionSelectedData fromIntent(@Nullable Intent intent) {
        if (intent == null) {
            return null;
        }
        return new ContentActionSelectedData(intent);
    }

    /**
     * Gets the URL of the current web page in the Custom Tab when the action was triggered.
     * This is retrieved from {@link Intent#getData()}.
     *
     * @return The current URL as a {@link Uri}.
     */
    public @NonNull Uri getPageUrl() {
        return mIntent.getData();
    }

    /**
     * Gets the ID of the specific {@link CustomContentAction} that was triggered by the user.
     *
     * @return The integer ID of the triggered action. Returns -1 if the extra is not present,
     * indicating an issue or that the ID was not properly sent.
     * @see CustomTabsIntent#EXTRA_TRIGGERED_CUSTOM_CONTENT_ACTION_ID
     */
    public int getTriggeredActionId() {
        // Default to -1, a common indicator for "not found" or "invalid".
        return mIntent.getIntExtra(CustomTabsIntent.EXTRA_TRIGGERED_CUSTOM_CONTENT_ACTION_ID, -1);
    }

    /**
     * Gets the specific type of content that was interacted with (e.g., via long-press)
     * to trigger the custom content action.
     *
     * @return The {@link CustomTabsIntent.ContentTargetType} integer value (e.g.,
     * {@link CustomTabsIntent#CONTENT_TARGET_TYPE_IMAGE},
     * {@link CustomTabsIntent#CONTENT_TARGET_TYPE_LINK}).
     * Returns 0 if the extra is not present, which is not a valid target type and
     * indicates the data was not sent or is malformed.
     * @see CustomTabsIntent#EXTRA_CLICKED_CONTENT_TARGET_TYPE
     */
    public @CustomTabsIntent.ContentTargetType int getClickedContentTargetType() {
        // Default to 0, which isn't a valid ContentTargetType.
        return mIntent.getIntExtra(CustomTabsIntent.EXTRA_CLICKED_CONTENT_TARGET_TYPE, 0);
    }

    /**
     * Gets the URL of the clicked image, if the interacted element was an image and the
     * browser supports providing this information.
     *
     * @return The image URL as a String, or {@code null} if not present or not applicable.
     * @see CustomTabsIntent#EXTRA_CONTEXT_IMAGE_URL
     */
    public @Nullable String getImageUrl() {
        return mIntent.getStringExtra(CustomTabsIntent.EXTRA_CONTEXT_IMAGE_URL);
    }

    /**
     * Gets a {@link Uri} pointing to the data of the clicked image, if the interacted
     * element was an image and the browser supports providing this information.
     * <p>
     * <b>Note:</b> The returned {@code Uri} points to a temporary file managed by the
     * browser. Its contents should be copied to your app's persistent storage immediately
     * if you need to access it later, as the file may be deleted. Do not store the Uri itself
     * for long-term use.
     * Please refer to https://developer.android
     * .com/training/secure-file-sharing/share-file#GrantPermissions
     * for more information.
     *
     * @return A {@link Uri} for the image data, or {@code null} if not present or
     * Android version is below Tiramisu.
     * @see CustomTabsIntent#EXTRA_CONTEXT_IMAGE_DATA_URI
     */
    public @Nullable Uri getImageDataUri() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return mIntent.getParcelableExtra(CustomTabsIntent.EXTRA_CONTEXT_IMAGE_DATA_URI,
                    Uri.class);
        }
        return null;
    }

    /**
     * Gets the alt text of the clicked element (e.g., an image or link), if available.
     * <p>
     * <b>Note:</b> The browser determines the length of the returned text, so it may be truncated.
     * As such, the returned value is not guaranteed to be the complete, original text.
     *
     * @return The alt text as a String, or {@code null} if not present or not applicable.
     * @see CustomTabsIntent#EXTRA_CONTEXT_IMAGE_ALT_TEXT
     */
    public @Nullable String getImageAltText() {
        return mIntent.getStringExtra(CustomTabsIntent.EXTRA_CONTEXT_IMAGE_ALT_TEXT);
    }

    /**
     * Gets the URL of the clicked link, if the interacted element was a link
     * (i.e., {@link #getClickedContentTargetType()} returns
     * {@link CustomTabsIntent#CONTENT_TARGET_TYPE_LINK}) and the browser supports
     * providing this information.
     *
     * @return The link URL as a String, or {@code null} if not present or not applicable.
     * @see CustomTabsIntent#EXTRA_CONTEXT_LINK_URL
     */
    public @Nullable String getLinkUrl() {
        return mIntent.getStringExtra(CustomTabsIntent.EXTRA_CONTEXT_LINK_URL);
    }

    /**
     * Gets the visible text content of the clicked link, if the interacted element was a link
     * and the browser provides this information.
     *
     * @return The link text as a String, or {@code null} if not present or not applicable.
     * @see CustomTabsIntent#EXTRA_CONTEXT_LINK_TEXT
     */
    public @Nullable String getLinkText() {
        return mIntent.getStringExtra(CustomTabsIntent.EXTRA_CONTEXT_LINK_TEXT);
    }

    /**
     * Returns the underlying {@link Intent} object that this data was parsed from.
     * This can be useful for accessing any additional, non-standard extras that might
     * have been included by the browser.
     *
     * @return The original {@link Intent}.
     */
    public @NonNull Intent getIntent() {
        return mIntent;
    }
}
