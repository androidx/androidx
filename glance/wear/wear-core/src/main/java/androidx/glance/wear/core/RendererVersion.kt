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

package androidx.glance.wear.core

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.annotation.IntRange
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting

/**
 * The version information of the renderer supported by the Host.
 *
 * @property major Major version. Incremented on breaking changes (i.e. compatibility is not
 *   guaranteed across major versions).
 * @property minor Minor version. Incremented on non-breaking changes (e.g. feature additions).
 *   Anything consuming a payload can safely consume anything with a lower minor version.
 * @property revision Revision version. Incremented on non-breaking changes.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class RendererVersion(
    @IntRange(from = 1) public val major: Int = DEFAULT_RENDERER_VERSION_MAJOR,
    @IntRange(from = 0) public val minor: Int = DEFAULT_RENDERER_VERSION_MINOR,
    @IntRange(from = 0) public val revision: Int = DEFAULT_RENDERER_VERSION_REVISION,
) : Comparable<RendererVersion> {

    public override fun compareTo(other: RendererVersion): Int =
        compareValuesBy(this, other, { it.major }, { it.minor }, { it.revision })

    public companion object {
        /**
         * The default major version, describing the renderer Host offering initial RemoteCompose
         * support.
         */
        public const val DEFAULT_RENDERER_VERSION_MAJOR: Int = 1

        /**
         * The default minor version, describing the renderer Host offering initial RemoteCompose
         * support.
         */
        public const val DEFAULT_RENDERER_VERSION_MINOR: Int = 6

        /**
         * The default revision version, describing the renderer Host offering initial RemoteCompose
         * support.
         */
        public const val DEFAULT_RENDERER_VERSION_REVISION: Int = 0

        /**
         * Resolves the [RendererVersion] supported by the Wear OS Host by parsing the version name
         * of the ProtoLayout renderer package.
         *
         * If the package is not installed or parsing fails, it will fallback to the default version
         * (`1.000`).
         *
         * @param context The Android Context.
         * @return The resolved [RendererVersion].
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun fromPlHostPackage(context: Context): RendererVersion {
            try {
                val packageInfo =
                    context.packageManager.getPackageInfo(PL_RENDERER_HOST_PACKAGE, /* flags= */ 0)

                val versionName: String? = packageInfo.versionName
                if (versionName.isNullOrEmpty()) return PL_RENDERER_INITIAL_VERSION

                val parts: List<String> = versionName.split(".")
                if (parts.size < 2) return PL_RENDERER_INITIAL_VERSION

                val major =
                    parts[0].toIntOrNull()?.takeIf { it >= 1 } ?: return PL_RENDERER_INITIAL_VERSION

                val minor =
                    parts[1].toIntOrNull()?.takeIf { it >= 0 } ?: return PL_RENDERER_INITIAL_VERSION
                val revision =
                    if (parts.size >= 3) {
                        parts[2].toIntOrNull()?.takeIf { it >= 0 }
                            ?: return PL_RENDERER_INITIAL_VERSION
                    } else {
                        0
                    }

                return RendererVersion(major, minor, revision)
            } catch (e: PackageManager.NameNotFoundException) {
                Log.w(TAG, "ProtoLayout renderer package not installed", e)
            } catch (e: Exception) {
                Log.w(TAG, "Unexpected parsing error", e)
            }
            return PL_RENDERER_INITIAL_VERSION
        }

        @VisibleForTesting
        internal const val PL_RENDERER_HOST_PACKAGE: String =
            "com.google.android.wearable.protolayout.renderer"
        @VisibleForTesting internal val PL_RENDERER_INITIAL_VERSION = RendererVersion(1, 0, 0)
        private const val TAG = "RendererVersion"
    }
}
