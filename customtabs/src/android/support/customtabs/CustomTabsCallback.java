/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.support.customtabs;

import android.os.Bundle;

/**
 * A callback class for custom tabs client to get messages regarding events in their custom tabs.
 */
public class CustomTabsCallback {
    /**
     * Sent when the tab has started loading a page.
     */
    public static final int NAVIGATION_STARTED = 1;

    /**
     * Sent when the tab has finished loading a page.
     */
    public static final int NAVIGATION_FINISHED = 2;

    /**
     * Sent when the tab couldn't finish loading due to a failure.
     */
    public static final int NAVIGATION_FAILED = 3;

    /**
     * Sent when loading was aborted by a user action before it finishes like clicking on a link
     * or refreshing the page.
     */
    public static final int NAVIGATION_ABORTED = 4;

    /**
     * To be called when a navigation event happens.
     *
     * @param navigationEvent The code corresponding to the navigation event.
     * @param extras Reserved for future use.
     */
    public void onNavigationEvent(int navigationEvent, Bundle extras) {}

    /**
     * Unsupported callbacks that may be provided by the implementation.
     *
     * <p>
     * <strong>Note:</strong>Clients should <strong>never</strong> rely on this callback to be
     * called and/or to have a defined behavior, as it is entirely implementation-defined and not
     * supported.
     *
     * <p> This can be used by implementations to add extra callbacks, for testing or experimental
     * purposes.
     *
     * @param callbackName Name of the extra callback.
     * @param args Arguments for the calback
     */
    public void extraCallback(String callbackName, Bundle args) {}
}
