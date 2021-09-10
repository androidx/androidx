/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.car.app.navigation;

import androidx.car.app.CarContext;
import androidx.car.app.navigation.model.Trip;

/**
 * Callback for events from the {@link NavigationManager}.
 *
 * @see NavigationManager
 */
public interface NavigationManagerCallback {
    /**
     * Notifies the app to stop active navigation, which may occurs when another source such as the
     * car head unit starts navigating.
     *
     * <p>When receiving this callback, the app must stop all routing including navigation voice
     * guidance, routing-related notifications, and updating trip information via {@link
     * NavigationManager#updateTrip(Trip)}.
     */
    default void onStopNavigation() {
    }

    /**
     * Notifies the app that, from this point onwards, when the user chooses to navigate to a
     * destination, the app should start simulating a drive towards that destination.
     *
     * <p>This mode should remain active until {@link CarContext#finishCarApp()} is called.
     *
     * <p>This functionality is used to allow verifying the app's navigation capabilities without
     * being in an actual car.
     */
    default void onAutoDriveEnabled() {
    }
}
