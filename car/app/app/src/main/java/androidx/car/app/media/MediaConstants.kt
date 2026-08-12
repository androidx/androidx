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

package androidx.car.app.media

object MediaConstants {
    /**
     * Intent Action sent to an application indicating that the playback screen should be opened if
     * they have one, or the most relevant screen otherwise.
     *
     * Starting in Car API 9, media apps must handle this action in `onNewIntent` (or intent
     * handling) to route the user to their playback view. For more details, see
     * [androidx.car.app.media.model.MediaPlaybackTemplate].
     */
    const val ACTION_SHOW_MEDIA_PLAYBACK = "androidx.car.app.media.action.SHOW_MEDIA_PLAYBACK"
}
