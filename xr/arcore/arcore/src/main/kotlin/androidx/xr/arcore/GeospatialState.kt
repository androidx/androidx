/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.arcore

/**
 * Describes the state of Geospatial. The State must be [RUNNING] to use Geospatial functionality.
 * If Geospatial has entered an error state other than [PAUSED], Geospatial must be disabled and
 * re-enabled to use Geospatial again.
 */
public class GeospatialState private constructor(internal val value: Int) {
    public companion object {
        /**
         * Geospatial is running and has not encountered an error. Functions to create anchors or
         * convert poses may still fail if Geospatial is not tracking.
         */
        @JvmField public val RUNNING: GeospatialState = GeospatialState(1)

        /**
         * Geospatial is not running. The Geospatial config must be enabled to use the Geospatial
         * APIs. After enablement, Geospatial will not immediately enter the RUNNING state.
         */
        @JvmField public val NOT_RUNNING: GeospatialState = GeospatialState(0)

        /**
         * Earth localization has encountered an internal error. The app should not attempt to
         * recover from this error. Please see the Android logs for additional information.
         */
        @JvmField public val ERROR_INTERNAL: GeospatialState = GeospatialState(-1)

        /**
         * The authorization provided by the application is not valid.
         * - The associated Google Cloud project may not have enabled the ARCore API.
         * - When using API key authentication, this will happen if the API key in the manifest is
         *   invalid or unauthorized. It may also fail if the API key is restricted to a set of apps
         *   not including the current one.
         * - When using keyless authentication, this may happen when no OAuth client has been
         *   created, or when the signing key and package name combination does not match the values
         *   used in the Google Cloud project. It may also fail if Google Play Services isn't
         *   installed, is too old, or is malfunctioning for some reason (e.g. killed due to memory
         *   pressure).
         */
        @JvmField public val ERROR_NOT_AUTHORIZED: GeospatialState = GeospatialState(-2)

        /**
         * The application has hit the rate limit for created Geospatial Sessions. The developer
         * should
         * [request additional quota](https://cloud.google.com/docs/quota#requesting_higher_quota)
         * for the ARCore API for their project from the Google Cloud Console.
         *
         * Sessions are limited per-minute and enabling may succeed if retried. The application can
         * disable and re-enable Geospatial to try again.
         */
        @JvmField public val ERROR_RESOURCE_EXHAUSTED: GeospatialState = GeospatialState(-3)

        /**
         * The Geospatial connection has been paused. The connection may resume, and does not
         * require action from the app. Tracked entities will enter the STOPPED state and must be
         * destroyed.
         */
        @JvmField public val PAUSED: GeospatialState = GeospatialState(2)
    }
}
