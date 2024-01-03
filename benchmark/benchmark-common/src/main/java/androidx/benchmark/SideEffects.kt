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

package androidx.benchmark

import android.os.Build
import android.util.Log
import androidx.annotation.RestrictTo

/**
 * Disables the [packages] during the course of a benchmark thereby reducing the amount of noise.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DisablePackages(
    private val packages: List<String> = DEFAULT_PACKAGES_TO_DISABLE
) : SideEffect {
    override fun name(): String {
        return "DisablePackages"
    }

    override fun setup() {
        if (Build.VERSION.SDK_INT >= 21) {
            Log.d(BenchmarkState.TAG, "Disabling packages $packages")
            Shell.disablePackages(packages)
        }
    }

    override fun tearDown() {
        if (Build.VERSION.SDK_INT >= 21) {
            Log.d(BenchmarkState.TAG, "Re-enabling packages $packages")
            Shell.enablePackages(packages)
        }
    }

    companion object {
        // A list of packages to disable + "com.google.android.gms"
        // https://source.corp.google.com/piper///depot/google3/java/com/google/android/libraries/swpower/fixture/DisableModule.java
        internal val DEFAULT_PACKAGES_TO_DISABLE = listOf(
            "com.android.chrome",
            "com.android.phone",
            "com.android.ramdump",
            "com.android.vending",
            "com.google.android.apps.docs",
            "com.google.android.apps.gcs",
            "com.google.android.apps.internal.betterbug",
            "com.google.android.apps.maps",
            "com.google.android.apps.messaging",
            "com.google.android.apps.nbu.files",
            "com.google.android.apps.photos",
            "com.google.android.apps.scone",
            "com.google.android.apps.tips",
            "com.google.android.apps.turbo",
            "com.google.android.apps.tycho",
            "com.google.android.apps.work.clouddpc",
            "com.google.android.apps.youtube.music",
            "com.google.android.as",
            "com.google.android.calculator",
            "com.google.android.calendar",
            "com.google.android.configupdater",
            "com.google.android.contacts",
            "com.google.android.deskclock",
            "com.google.android.dialer",
            "com.google.android.googlequicksearchbox",
            "com.google.android.gm",
            "com.google.android.gms",
            "com.google.android.GoogleCamera",
            "com.google.android.ims",
            "com.google.android.inputmethod.latin",
            "com.google.android.marvin.talkback",
            "com.google.android.partnersetup",
            "com.google.android.settings.intelligence",
            "com.google.android.tts",
            "com.google.android.videos",
            "com.google.android.youtube",
            "com.google.android.videos",
            "com.google.android.volta"
        )
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DisableDexOpt : SideEffect {
    override fun name(): String {
        return "DisableDexOpt"
    }

    override fun setup() {
        // PGO was enabled on Android N
        if (Build.VERSION.SDK_INT >= 24) {
            Shell.disableBackgroundDexOpt()
        }
    }

    override fun tearDown() {
        if (Build.VERSION.SDK_INT >= 24) {
            Shell.enableBackgroundDexOpt()
        }
    }
}
