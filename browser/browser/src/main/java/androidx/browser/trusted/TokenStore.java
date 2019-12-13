/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.browser.trusted;

import androidx.annotation.BinderThread;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

/**
 * Users should implement this interface to persist the given {@link Token} (across app
 * restarts).
 * Once implemented, they should override {@link TrustedWebActivityService#getTokenStore()} to
 * return an instance.
 * Finally, they should execute the following code themselves to set the verified provider:
 * <pre>
 * {@code
 * TokenStore tokenStore  = ... // Instantiate the implemented class.
 * String packageName = ... // Package name of the Trusted Web Activity provider.
 *
 * tokenStore.store(Token.create(packageName, getPackageManager());
 * }
 * </pre>
 *
 * There is only a single {@link Token} stored at a time.
 *
 * Note that {@link #load} will be called by {@link TrustedWebActivityService} on a binder thread.
 * Whereas {@link #store} can be called by the user on whichever thread they like.
 */
public interface TokenStore {
    /**
     * This method should persist the given {@link Token}.
     * Subsequent calls will overwrite the previously given {@link Token}.
     *
     * @param token The token to persist. It may be {@code null} to clear the storage.
     */
    @WorkerThread
    void store(@Nullable Token token);

    /**
     * This method returns the {@link Token} previously persisted by a call to {@link #store}.
     * @return The previously persisted {@link Token}, or {@code null} if none exist.
     *
     * This method will be called on a binder thread by {@link TrustedWebActivityService}.
     */
    @BinderThread
    @Nullable
    Token load();
}
