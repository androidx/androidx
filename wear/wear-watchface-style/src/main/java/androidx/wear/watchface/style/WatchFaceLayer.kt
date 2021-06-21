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

package androidx.wear.watchface.style

/** Describes part of watchface. Used as a parameter for rendering. */
public enum class WatchFaceLayer {
    /** The watch excluding complications and anything that may render on top of complications. */
    BASE,

    /** The watch face complications. */
    COMPLICATIONS,

    /** Any parts of the watch that may render on top of complications, e.g. watch hands. */
    COMPLICATIONS_OVERLAY;

    public companion object {
        /** A [Set] of all [WatchFaceLayer]s. */
        @JvmField
        public val ALL_WATCH_FACE_LAYERS: Set<WatchFaceLayer> = values().toSet()
    }
}