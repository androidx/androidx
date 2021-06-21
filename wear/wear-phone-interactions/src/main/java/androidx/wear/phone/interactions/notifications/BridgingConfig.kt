/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.phone.interactions.notifications

import android.content.Context
import android.os.Bundle
import java.util.Objects

/**
 * Bridging configuration to be specified at runtime, to set tags for notifications that are exempt
 * from the bridging mode. specifically, create a [BridgingConfig] object, add excluded tags, then
 * set it with [BridgingManager.setConfig]
 *
 * Specifying a bridging configuration at runtime overrides a bridging-related setting in the
 * Android manifest file.
 *
 */
public class BridgingConfig internal constructor(
    /** Name of the package of the current context */
    internal val packageName: String?,
    /** Whether notification bridging is enabled in the configuration. */
    public val isBridgingEnabled: Boolean,
    /**
     * The set of excluded tags in the configuration. The bridging mode for these tags is the
     * opposite of the default mode (returned by [isBridgingEnabled]).
     */
    public val excludedTags: MutableSet<String>?
) {
    internal companion object {
        private const val TAG = "BridgingConfig"

        private const val EXTRA_ORIGINAL_PACKAGE =
            "android.support.wearable.notifications.extra.originalPackage"
        private const val EXTRA_BRIDGING_ENABLED =
            "android.support.wearable.notifications.extra.bridgingEnabled"
        private const val EXTRA_EXCLUDED_TAGS =
            "android.support.wearable.notifications.extra.excludedTags"

        @JvmStatic
        internal fun fromBundle(bundle: Bundle): BridgingConfig =
            BridgingConfig(
                bundle.getString(EXTRA_ORIGINAL_PACKAGE),
                bundle.getBoolean(EXTRA_BRIDGING_ENABLED),
                bundle.getStringArrayList(EXTRA_EXCLUDED_TAGS)?.toSet() as MutableSet<String>?
            )
    }

    internal fun toBundle(context: Context): Bundle =
        Bundle().apply {
            putString(EXTRA_ORIGINAL_PACKAGE, context.packageName)
            putBoolean(EXTRA_BRIDGING_ENABLED, isBridgingEnabled)
            putStringArrayList(EXTRA_EXCLUDED_TAGS, excludedTags?.toList() as ArrayList<String>)
        }

    override fun equals(other: Any?): Boolean {
        if (other is BridgingConfig) {
            return other.isBridgingEnabled == isBridgingEnabled and
                (other.excludedTags == excludedTags) and
                (other.packageName == packageName)
        }

        return false
    }

    override fun hashCode(): Int = Objects.hash(packageName, isBridgingEnabled, excludedTags)

    override fun toString(): String {
        return "BridgingConfig{packageName='$packageName'" +
            ", isBridgingEnabled='$isBridgingEnabled'" +
            ", excludedTags=$excludedTags}"
    }

    /**
     * Builder for BridgingConfig. The set of excluded tags is empty, unless added with
     * [addExcludedTag] or [addExcludedTags].
     *
     * @param context   The [Context] of the application requesting a BridgingConfig change.
     * @param isBridgingEnabled Whether notification bridging is enabled in the configuration.
     */
    public class Builder(context: Context, private val isBridgingEnabled: Boolean) {
        private val packageName: String = context.packageName
        private val excludedTags: MutableSet<String> = HashSet()

        /**
         * Adds a tag for which the bridging mode is the opposite as the default mode.
         *
         * Examples:
         *
         * ```
         * new BridgingConfig.Builder(context, false)  // bridging disabled by default
         *   .addExcludedTag("foo")
         *   .addExcludedTag("bar")
         *   .build());
         * ```
         *
         * ```
         * new BridgingConfig.Builder(context, true)  // bridging enabled by default
         *   .addExcludedTag("foo")
         *   .addExcludedTag("bar")
         *   .build());
         * ```
         *
         * @param tag The tag to exclude from the default bridging mode.
         * @return The Builder instance.
         */
        public fun addExcludedTag(tag: String): Builder {
            excludedTags.add(tag)
            return this
        }

        /**
         * Sets a collection of tags for which the bridging mode is the opposite as the default mode.
         *
         * Examples:
         *
         * ```
         * new BridgingConfig.Builder(context, false)  // bridging disabled by default
         *   .addExcludedTags(Arrays.asList("foo", "bar", "baz"))
         *   .build());
         *```
         *
         * ```
         * new BridgingConfig.Builder(context, true)  // bridging enabled by default
         *   .addExcludedTags(Arrays.asList("foo", "bar", "baz"))
         *   .build());
         * }
         * ```
         *
         * @param tags The collection of tags to exclude from the default bridging mode.
         * @return The Builder instance.
         */
        @SuppressWarnings("MissingGetterMatchingBuilder")
        // no getter needed for the builder, getter is provided in BridgingConfig
        public fun addExcludedTags(tags: Collection<String>): Builder {
            excludedTags.addAll(tags)
            return this
        }

        /** Builds a BridgingConfig object. */
        public fun build(): BridgingConfig =
            BridgingConfig(packageName, isBridgingEnabled, excludedTags)
    }
}