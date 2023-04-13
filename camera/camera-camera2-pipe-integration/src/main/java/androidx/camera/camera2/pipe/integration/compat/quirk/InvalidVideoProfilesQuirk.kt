/*
 * Copyright 2023 The Android Open Source Project
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

@file:RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java

package androidx.camera.camera2.pipe.integration.compat.quirk

import android.annotation.SuppressLint
import android.media.EncoderProfiles
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.camera.core.impl.Quirk

/**
 * Quirk denoting the video profile list returns by [EncoderProfiles] is invalid.
 *
 * QuirkSummary
 * - Bug Id: 267727595
 * - Description: When using [EncoderProfiles] on TP1A or TD1A builds of Android API 33,
 *   [EncoderProfiles.getVideoProfiles] returns a list with size one, but the single value in the
 *   list is null. This is not the expected behavior, and makes [EncoderProfiles] lack of video
 *   information.
 * - Device(s): Pixel 4 and above pixel devices with TP1A or TD1A builds (API 33).
 *
 * TODO: enable CameraXQuirksClassDetector lint check when kotlin is supported.
 */
@SuppressLint("CameraXQuirksClassDetector")
class InvalidVideoProfilesQuirk : Quirk {

    companion object {
        private val AFFECTED_MODELS: List<String> = listOf(
            "pixel 4",
            "pixel 4a",
            "pixel 4a (5g)",
            "pixel 4 xl",
            "pixel 5",
            "pixel 5a",
            "pixel 6",
            "pixel 6a",
            "pixel 6 pro",
            "pixel 7",
            "pixel 7 pro"
        )

        fun isEnabled(): Boolean {
            return isAffectedModel() && isAffectedBuild()
        }

        private fun isAffectedModel(): Boolean {
            return AFFECTED_MODELS.contains(
                Build.MODEL.lowercase()
            )
        }

        private fun isAffectedBuild(): Boolean {
            return isTp1aBuild() || isTd1aBuild()
        }

        private fun isTp1aBuild(): Boolean {
            return Build.ID.startsWith("TP1A")
        }

        private fun isTd1aBuild(): Boolean {
            return Build.ID.startsWith("TD1A")
        }
    }
}