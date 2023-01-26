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

package androidx.car.app.messaging;

import androidx.car.app.annotations.ExperimentalCarApi;

/** Constants related to messaging in Android Auto */
@ExperimentalCarApi
public class MessagingServiceConstants {
    /**
     * Used to declare Android Auto messaging support within an app's manifest
     *
     * <p> Specifically, this bit should be added to an {@link android.app.IntentService} as the
     * {@code IntentFilter}'s action. When declared, apps will show up in Android Auto's app
     * launcher with the "default" / "built-in" in-car messaging experience.
     */
    public static final String ACTION_HANDLE_CAR_MESSAGING =
            "androidx.car.app.messaging.action.HANDLE_CAR_MESSAGING";

    // Do not instantiate
    private MessagingServiceConstants() {}
}
