/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.browser.auth;

import android.os.Bundle;

import androidx.browser.customtabs.CustomTabsCallback;
import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsService;

import org.jspecify.annotations.NonNull;

import java.util.concurrent.Executor;

/**
 * A callback class for Auth Tab clients to get messages regarding events in their Auth Tabs. In the
 * implementation, all callbacks are sent to the {@link Executor} provided by the client to
 * {@link CustomTabsClient#newAuthTabSession} or its UI thread if one is not provided.
 */
public interface AuthTabCallback {
    /**
     * To be called when a navigation event happens.
     *
     * @param navigationEvent The code corresponding to the navigation event.
     * @param extras          Reserved for future use.
     */
    void onNavigationEvent(@CustomTabsCallback.NavigationEvent int navigationEvent,
            @NonNull Bundle extras);

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
     * @param args         Arguments for the callback
     */
    void onExtraCallback(@NonNull String callbackName, @NonNull Bundle args);

    /**
     * The same as {@link #onExtraCallback}, except that this method allows the Auth Tab provider to
     * return a result.
     *
     * A return value of {@link Bundle#EMPTY} will be used to signify that the client does not know
     * how to handle the callback.
     *
     * As optional best practices, {@link CustomTabsService#KEY_SUCCESS} could be use to identify
     * that callback was *successfully* handled. For example, when returning a message with result:
     * <pre><code>
     *     Bundle result = new Bundle();
     *     result.putString("message", message);
     *     if (success)
     *         result.putBoolean(CustomTabsService#KEY_SUCCESS, true);
     *     return result;
     * </code></pre>
     * The caller side:
     * <pre><code>
     *     Bundle result = extraCallbackWithResult(callbackName, args);
     *     if (result.getBoolean(CustomTabsService#KEY_SUCCESS)) {
     *         // callback was successfully handled
     *     }
     * </code></pre>
     */
    @NonNull
    Bundle onExtraCallbackWithResult(@NonNull String callbackName, @NonNull Bundle args);

    /**
     * Called when the browser process finished warming up initiated by
     * {@link CustomTabsClient#warmup()}.
     *
     * @param extras Reserved for future use.
     */
    void onWarmupCompleted(@NonNull Bundle extras);
}
