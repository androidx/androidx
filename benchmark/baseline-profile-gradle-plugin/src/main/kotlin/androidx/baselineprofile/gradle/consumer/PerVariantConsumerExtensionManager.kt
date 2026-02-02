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

package androidx.baselineprofile.gradle.consumer

import com.android.build.api.variant.Variant
import org.gradle.api.GradleException
import org.gradle.api.Project

/**
 * The [BaselineProfileConsumerPlugin] supports per variant configuration, according to values
 * expressed in [BaselineProfileVariantConfiguration]. The correct value for a property is
 * determined considering the concept of override or merge. When a property is evaluated considering
 * the override, the variants are evaluated in this order: `variantName`, `buildType` or
 * `productFlavor` and `main`. The first variant configuration to define the property is used to
 * return that property. For lists only, a property can be evaluated also merging all the variant
 * configurations. This is the case for dependencies for example, so that when accessing custom
 * dependencies for variant `freeRelease` the returned list contains the dependencies for
 * `freeRelease`, `free`, `release` and `main` (global ones).
 */
internal class PerVariantConsumerExtensionManager(
    private val extension: BaselineProfileConsumerExtension,
) {

    fun variant(variant: Variant) = VariantConfigurationProxy(variant = variant, ext = extension)

    internal class VariantConfigurationProxy
    internal constructor(
        private val variant: Variant,
        private val ext: BaselineProfileConsumerExtension,
    ) {

        val filterRules: List<Pair<RuleType, String>>
            get() = getMergedListForVariant(variant) { filters.rules }

        val dependencies: List<Project>
            get() = getMergedListForVariant(variant) { dependencies }

        val baselineProfileRulesRewrite: Boolean?
            get() = getOverriddenValueForVariantAllowNull(variant) { baselineProfileRulesRewrite }

        val dexLayoutOptimization: Boolean?
            get() = getOverriddenValueForVariantAllowNull(variant) { dexLayoutOptimization }

        val saveInSrc: Boolean
            get() = getOverriddenValueForVariant(variant) { saveInSrc }

        val automaticGenerationDuringBuild: Boolean
            get() = getOverriddenValueForVariant(variant) { automaticGenerationDuringBuild }

        val baselineProfileOutputDir: String
            get() = getOverriddenValueForVariant(variant) { baselineProfileOutputDir }

        val mergeIntoMain: Boolean?
            get() = getOverriddenValueForVariantAllowNull(variant) { mergeIntoMain }

        private fun <T> getMergedListForVariant(
            variant: Variant,
            getter: BaselineProfileVariantConfigurationImpl.() -> List<T>
        ): List<T> {
            return listOfNotNull(
                    "main",
                    variant.flavorName,
                    *variant.productFlavors.map { it.second }.toTypedArray(),
                    variant.buildType,
                    variant.name
                )
                .mapNotNull { ext.variants.findByName(it) }
                .map { getter.invoke(it) }
                .flatten()
        }

        private fun <T> getOverriddenValueForVariantAllowNull(
            variant: Variant,
            getter: BaselineProfileVariantConfigurationImpl.() -> T
        ): T? {
            // Here we select a setting for the given variant. [BaselineProfileVariantConfiguration]
            // are evaluated in the following order: variant, flavor, build type, `main`.
            // If a property is found it will return it. Note that `main` should have all the
            // defaults set so this method never returns a nullable value and should always return.

            val definedProperties =
                listOfNotNull(
                        variant.name,
                        *variant.productFlavors.map { it.second }.toTypedArray(),
                        variant.flavorName,
                        variant.buildType,
                        "main"
                    )
                    .mapNotNull {
                        val variantConfig = ext.variants.findByName(it) ?: return@mapNotNull null
                        return@mapNotNull Pair(it, getter.invoke(variantConfig))
                    }
                    .filter { it.second != null }

            // This is a case where the property is defined in both build type and flavor.
            // In this case it should fail because the result is ambiguous.
            val propMap = definedProperties.toMap()
            if (
                variant.flavorName in propMap &&
                    variant.buildType in propMap &&
                    propMap[variant.flavorName] != propMap[variant.buildType]
            ) {
                throw GradleException(
                    """
            The per-variant configuration for baseline profiles is ambiguous. This happens when
            that the same property has been defined in both a build type and a flavor.

            For example:

            baselineProfiles {
                variants {
                    free {
                        saveInSrc = true
                    }
                    release {
                        saveInSrc = false
                    }
                }
            }

            In this case for `freeRelease` it's not possible to determine the exact value of the
            property. Please specify either the build type or the flavor.
            """
                        .trimIndent()
                )
            }

            return definedProperties.firstOrNull()?.second
        }

        private fun <T> getOverriddenValueForVariant(
            variant: Variant,
            default: T? = null,
            getter: BaselineProfileVariantConfigurationImpl.() -> T?
        ): T {
            val value = getOverriddenValueForVariantAllowNull(variant, getter)
            if (value != null) return value
            if (default != null) return default

            // This should never happen. It means the extension is missing a default property and
            // n default was specified when accessing this value. This cannot happen because of the
            // user configuration.
            throw GradleException("The required property does not have a default.")
        }
    }
}
