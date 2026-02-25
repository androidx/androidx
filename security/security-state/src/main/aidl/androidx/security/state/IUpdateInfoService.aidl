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

package androidx.security.state;

import androidx.security.state.UpdateCheckResult;

/**
 * Interface for a service that provides information about available security updates.
 *
 * <p>Trusted system applications (such as the System Updater or Google Play Store)
 * implement this interface to expose pending update information to the
 * {@link androidx.security.state.SecurityPatchState} library.
 *
 * <p>Host applications must declare a service that handles the
 * {@code "androidx.security.state.provider.UPDATE_INFO_SERVICE"} action.
 */
@JavaPassthrough(annotation="@androidx.annotation.RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)")
interface IUpdateInfoService {

    /**
     * Retrieves available updates and the time they were last synchronized.
     *
     * <p>This method returns the latest available update information currently known
     * to the provider.
     *
     * <p>The result includes a timestamp indicating when the data was last synchronized
     * with the backend. Clients should inspect this timestamp to determine if the
     * data is fresh enough for their needs.
     *
     * @return An {@link UpdateCheckResult} containing the list of available updates and the
     *         timestamp of the last successful synchronization with the backend.
     */
    UpdateCheckResult listAvailableUpdates();
}