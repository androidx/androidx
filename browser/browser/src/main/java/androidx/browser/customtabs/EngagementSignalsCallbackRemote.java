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
import android.os.IBinder;
import android.os.RemoteException;
import android.support.customtabs.IEngagementSignalsCallback;
import android.util.Log;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * Remote class used to execute callbacks from a binder of {@link EngagementSignalsCallback}. This
 * is a thin wrapper around {@link IEngagementSignalsCallback} that is passed to the Custom Tabs
 * implementation for the calls across process boundaries.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
/* package */ final class EngagementSignalsCallbackRemote implements EngagementSignalsCallback {
    private static final String TAG = "EngagementSigsCallbkRmt";

    private final IEngagementSignalsCallback mCallbackBinder;

    private EngagementSignalsCallbackRemote(@NonNull IEngagementSignalsCallback callbackBinder) {
        mCallbackBinder = callbackBinder;
    }

    /**
     * Creates an {@link EngagementSignalsCallback} from a binder.
     */
    @NonNull
    static EngagementSignalsCallbackRemote fromBinder(@NonNull IBinder binder) {
        IEngagementSignalsCallback callbackBinder =
                IEngagementSignalsCallback.Stub.asInterface(binder);
        return new EngagementSignalsCallbackRemote(callbackBinder);
    }

    /**
     * Called when a user scrolls the tab.
     *
     * @param isDirectionUp False when the user scrolls farther down the page, and true when the
     *                      user scrolls back up toward the top of the page.
     * @param extras Reserved for future use.
     */
    @Override
    public void onVerticalScrollEvent(boolean isDirectionUp, @NonNull Bundle extras) {
        try {
            mCallbackBinder.onVerticalScrollEvent(isDirectionUp, extras);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException during IEngagementSignalsCallback transaction");
        }
    }

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
    @Override
    public void onGreatestScrollPercentageIncreased(
            @IntRange(from = 1, to = 100) int scrollPercentage, @NonNull Bundle extras) {
        try {
            mCallbackBinder.onGreatestScrollPercentageIncreased(scrollPercentage, extras);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException during IEngagementSignalsCallback transaction");
        }
    }

    /**
     * Called when a {@link CustomTabsSession} is ending or when no further Engagement Signals
     * callbacks are expected to report whether any user action has occurred during the session.
     *
     * @param didUserInteract Whether the user has interacted with the page in any way, e.g.
     *                        scrolling.
     * @param extras Reserved for future use.
     */
    @Override
    public void onSessionEnded(boolean didUserInteract, @NonNull Bundle extras) {
        try {
            mCallbackBinder.onSessionEnded(didUserInteract, extras);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException during IEngagementSignalsCallback transaction");
        }
    }
}
