/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.room3.compiler.processing

/**
 * Configuration class for XProcessingEnv where certain behaviors might be modified.
 *
 * See documentation for details.
 *
 * To create an instance from Java, use the provided [Builder] class. To create an instance from
 * Kotlin, you can either use the provided [Builder] or just [copy] an existing configuration.
 *
 * If you are using XProcessing Testing library, you can set an implementation of
 * `XProcessingEnvironmentTestConfigProvider` via a service configuration to load your default
 * configuration in `runProcessorTest` calls.
 */
class XProcessingEnvConfig
private constructor(
    /**
     * When set to `true`, XProcessingEnv will hide all methods that have invalid source names in
     * Java (i.e. cannot be called from generated Java sources).
     *
     * Doing this resolution is expensive (requires type resolution) hence it is set to `false` by
     * default.
     *
     * Note that, due to KAPT stubs, this is not 100% consistent between KAPT and KSP when set to
     * `false`. Since KAPT generates stubs, it automatically removes methods that have invalid JVM
     * names.
     */
    val excludeMethodsWithInvalidJvmSourceNames: Boolean = false,

    /**
     * When set to `true`, [XBasicAnnotationProcessor] will not validate annotated elements in the
     * round before passing them to the various [XProcessingStep]s. Enabling this options
     * essentially disabled the built-in element deferring mechanism offered by XProcessing.
     *
     * This option can be useful for processor with a custom and more precise validation as the
     * built-in validation can be too broad.
     */
    val disableAnnotatedElementValidation: Boolean = false,

    /**
     * When set to `true`, [XFieldElement.getAllAnnotations] and similar APIs will include
     * annotations in the owner [XPropertyElement].
     *
     * Historically property annotations where included in fields but that doesn't match the
     * language model so this option is useful for backwards compatible behavior as processors
     * migrate.
     */
    val includePropertyAnnotationsInFields: Boolean = false,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is XProcessingEnvConfig) return false

        if (
            excludeMethodsWithInvalidJvmSourceNames != other.excludeMethodsWithInvalidJvmSourceNames
        ) {
            return false
        }
        if (disableAnnotatedElementValidation != other.disableAnnotatedElementValidation) {
            return false
        }
        if (includePropertyAnnotationsInFields != other.includePropertyAnnotationsInFields) {
            return false
        }

        return true
    }

    override fun hashCode(): Int {
        var result = excludeMethodsWithInvalidJvmSourceNames.hashCode()
        result = 31 * result + disableAnnotatedElementValidation.hashCode()
        result = 31 * result + includePropertyAnnotationsInFields.hashCode()
        return result
    }

    override fun toString(): String {
        return "XProcessingEnvConfig(" +
            "excludeMethodsWithInvalidJvmSourceNames=$excludeMethodsWithInvalidJvmSourceNames, " +
            "disableAnnotatedElementValidation=$disableAnnotatedElementValidation" +
            "includePropertyAnnotationsInFields=$includePropertyAnnotationsInFields" +
            ")"
    }

    fun copy(
        excludeMethodsWithInvalidJvmSourceNames: Boolean =
            this.excludeMethodsWithInvalidJvmSourceNames,
        disableAnnotatedElementValidation: Boolean = this.disableAnnotatedElementValidation,
        includePropertyAnnotationsInFields: Boolean = this.includePropertyAnnotationsInFields,
    ) =
        XProcessingEnvConfig(
            excludeMethodsWithInvalidJvmSourceNames = excludeMethodsWithInvalidJvmSourceNames,
            disableAnnotatedElementValidation = disableAnnotatedElementValidation,
            includePropertyAnnotationsInFields = includePropertyAnnotationsInFields,
        )

    fun toBuilder() = Builder(this)

    class Builder(baseline: XProcessingEnvConfig = XProcessingEnvConfig()) {
        private var instance = baseline

        /** @see XProcessingEnvConfig.excludeMethodsWithInvalidJvmSourceNames for docs. */
        fun excludeMethodsWithInvalidJvmSourceNames(value: Boolean) = apply {
            instance = instance.copy(excludeMethodsWithInvalidJvmSourceNames = value)
        }

        /** @see XProcessingEnvConfig.disableAnnotatedElementValidation for docs. */
        fun disableAnnotatedElementValidation(value: Boolean) = apply {
            instance = instance.copy(disableAnnotatedElementValidation = value)
        }

        /** @see XProcessingEnvConfig.includePropertyAnnotationsInFields for docs. */
        fun includePropertyAnnotationsInFields(value: Boolean) = apply {
            instance = instance.copy(includePropertyAnnotationsInFields = value)
        }

        fun build(): XProcessingEnvConfig {
            return instance
        }
    }

    companion object {
        /** Default configuration for XProcessingEnv */
        val DEFAULT = Builder().build()
    }
}
