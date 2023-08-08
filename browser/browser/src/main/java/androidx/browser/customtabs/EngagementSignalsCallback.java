/*
 * Copyright 2023 The Android Open Source Project
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

import android.os.Bundle;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

/**
 * A callback class for custom tabs clients to get messages regarding the user's engagement with the
 * webpage within their custom tabs. These methods may not be called for some webpages. In the
 * implementation, all callbacks are sent to the
 * {@link Executor} provided by the client or its UI thread if one is not provided.
 */
public interface EngagementSignalsCallback {
    /**
     * Called when a user scrolls the tab.
     *
     * @param isDirectionUp False when the user scrolls farther down the page, and true when the
     *                      user scrolls back up toward the top of the page.
     * @param extras Reserved for future use.
     */
    default void onVerticalScrollEvent(boolean isDirectionUp, @NonNull Bundle extras) {}

    /**
     * Called when a user has reached a greater scroll percentage on the page. The greatest scroll
     * percentage is reset if the user navigates to a different page. If the current page's total
     * height changes, this method will be called again only if the scroll progress reaches a
     * higher percentage based on the new and current height of the page.
     *
     * @param scrollPercentage An integer indicating the percent of scrollable progress the user has
     *                         made down the current page.
     * @param extras Reserved for future use.
     */
    default void onGreatestScrollPercentageIncreased(
            @IntRange(from = 1, to = 100) int scrollPercentage, @NonNull Bundle extras) {}

    /**
     * Called when a {@link CustomTabsSession} is ending or when no further Engagement Signals
     * callbacks are expected to report whether any user action has occurred during the session.
     *
     * @param didUserInteract Whether the user has interacted with the page in any way, e.g.
     *                        scrolling.
     * @param extras Reserved for future use.
     */
    default void onSessionEnded(boolean didUserInteract, @NonNull Bundle extras) {}
}
