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

package androidx.benchmark.macro

class BaselineProfileConfig
internal constructor(
    private val outputFilePrefix: String?,
    private val packageName: String,
    private val profileBlock: MacrobenchmarkScope.() -> Unit,
    private val maxIterations: Int = 15,
    private val stableIterations: Int = 3,
    private val includeInStartupProfile: Boolean = false,
    private val strictStability: Boolean = false,
    private val filterPredicate: ((String) -> Boolean) = { true },
) {

    /**
     * @return An optional file name prefix used when creating the output file with the contents of
     *   the human readable baseline profile. For example: `outputFilePrefix-baseline-prof.txt`
     */
    fun getOutputFilePrefix(): String? = outputFilePrefix

    /** @return The Package name of the app for which profiles are to be generated. */
    fun getPackageName(): String = packageName

    /** @return the he critical user journey. */
    fun getProfileBlock(): MacrobenchmarkScope.() -> Unit = profileBlock

    /** @return the maximum number of iterations to run when collecting profiles. */
    fun getMaxIterations(): Int = maxIterations

    /**
     * @return the minimum number of iterations to observe as stable before assuming stability, and
     *   completing profile generation.
     */
    fun getStableIterations(): Int = stableIterations

    /**
     * Determines whether the generated profile should be also used as a startup profile. A startup
     * profile is utilized during the build process in order to determine which classes are needed
     * in the primary dex to optimize the startup time. This flag should be used only for startup
     * flows, such as main application startup pre and post login or other entry points of the app.
     * Note that methods collected in a startup profiles are also utilized for baseline profiles.
     */
    fun isIncludeInStartupProfile(): Boolean = includeInStartupProfile

    /** @return `true` iff we enforce that the generated profile was stable */
    fun isStrictStability(): Boolean = strictStability

    /** @return the function used to filter individual rules / lines of the baseline profile. */
    fun getFilterPredicate(): (String) -> Boolean = filterPredicate

    /** Can be used to build a [androidx.benchmark.macro.BaselineProfileConfig] instance. */
    class Builder
    public constructor(
        private val packageName: String,
        private val profileBlock: MacrobenchmarkScope.() -> Unit,
    ) {
        private var outputFilePrefix: String? = null
        private var maxIterations: Int = 15
        private var stableIterations: Int = 3
        private var includeInStartupProfile: Boolean = false
        private var strictStability: Boolean = false
        private var filterPredicate: ((String) -> Boolean) = { true }

        /**
         * Sets the optional file name prefix used when creating the output file with the contents
         * of the human readable baseline profile. For example: `outputFilePrefix-baseline-prof.txt`
         *
         * @return The [androidx.benchmark.macro.BaselineProfileConfig.Builder] instance for
         *   chaining.
         */
        fun setOutputFilePrefix(outputFilePrefix: String): Builder {
            this.outputFilePrefix = outputFilePrefix
            return this
        }

        /**
         * Overrides the maximum number of iterations to run when collecting profiles.
         *
         * @return The [androidx.benchmark.macro.BaselineProfileConfig.Builder] instance for
         *   chaining.
         */
        fun setMaxIterations(maxIterations: Int): Builder {
            this.maxIterations = maxIterations
            return this
        }

        /**
         * Overrides the minimum number of iterations to observe as stable before assuming
         * stability, and completing profile generation.
         *
         * @return The [androidx.benchmark.macro.BaselineProfileConfig.Builder] instance for
         *   chaining.
         */
        fun setStableIterations(stableIterations: Int): Builder {
            this.stableIterations = stableIterations
            return this
        }

        /**
         * Overrides the bit that determines whether the generated profile should be also used as a
         * startup profile. A startup profile is utilized during the build process in order to
         * determine which classes are needed in the primary dex to optimize the startup time. This
         * flag should be used only for startup flows, such as main application startup pre and post
         * login or other entry points of the app. Note that methods collected in a startup profiles
         * are also utilized for baseline profiles.
         *
         * @return The [androidx.benchmark.macro.BaselineProfileConfig.Builder] instance for
         *   chaining.
         */
        fun setIncludeInStartupProfile(includeInStartupProfile: Boolean): Builder {
            this.includeInStartupProfile = includeInStartupProfile
            return this
        }

        /**
         * Overrides the bit that enforces if the generated profile was stable
         *
         * @return The [androidx.benchmark.macro.BaselineProfileConfig.Builder] instance for
         *   chaining.
         */
        fun setStrictStability(strictStability: Boolean): Builder {
            this.strictStability = strictStability
            return this
        }

        /**
         * Overrides the function used to filter individual rules / lines of the baseline profile.
         * By default, no filters are applied. Note that this works only when the target
         * application's code is not obfuscated.
         *
         * @return The [androidx.benchmark.macro.BaselineProfileConfig.Builder] instance for
         *   chaining.
         */
        fun setFilterPredicate(filterPredicate: (String) -> Boolean): Builder {
            this.filterPredicate = filterPredicate
            return this
        }

        /** @return the [androidx.benchmark.macro.BaselineProfileConfig] instance. */
        fun build(): BaselineProfileConfig {
            return BaselineProfileConfig(
                outputFilePrefix = outputFilePrefix,
                packageName = packageName,
                profileBlock = profileBlock,
                maxIterations = maxIterations,
                stableIterations = stableIterations,
                includeInStartupProfile = includeInStartupProfile,
                strictStability = strictStability,
                filterPredicate = filterPredicate,
            )
        }
    }
}
