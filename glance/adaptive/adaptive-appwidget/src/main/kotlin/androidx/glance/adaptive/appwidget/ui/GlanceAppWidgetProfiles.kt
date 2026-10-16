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

package androidx.glance.adaptive.appwidget.ui

import androidx.compose.remote.creation.compose.capture.createProfile
import androidx.compose.remote.creation.profile.Profile

/** Defines RemoteCompose [Profile]s for Glance Adaptive AppWidgets. */
internal object GlanceAppWidgetProfiles {
    // Forked from androidx.compose.remote.core.RcProfiles.PROFILE_WIDGETS
    internal const val PROFILE_WIDGETS: Int = 0x100

    /** Document API level supported by the Android 17 (API 37) platform widget player. */
    internal const val WIDGETS_V7_DOC_API_LEVEL: Int = 7

    /** A [Profile] for Android 17 (API 37) AppWidgets (`WIDGETS_V7`). */
    val widgetsV7: Profile by
        lazy(mode = LazyThreadSafetyMode.PUBLICATION) {
            createProfile(
                docApiLevel = WIDGETS_V7_DOC_API_LEVEL,
                profileFlags = PROFILE_WIDGETS,
            )
        }
}
