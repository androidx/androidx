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

package androidx.core.telecom.reference.view

/**
 * Defines the navigation route constants used within the application.
 *
 * Using constants for routes helps prevent typos and makes navigation logic easier to manage.
 */
object NavRoutes {
    /** Route for the main Dialer screen. */
    const val DIALER = "dialer"
    /** Route for the Settings screen. */
    const val SETTINGS = "settings"
    /** Route for the In-Call screen displayed during an active call. */
    const val IN_CALL = "inCall"
}
