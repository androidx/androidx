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

package androidx.baselineprofile.gradle.configuration

import androidx.baselineprofile.gradle.configuration.attribute.BaselineProfilePluginAgpVersionAttr
import androidx.baselineprofile.gradle.configuration.attribute.BaselineProfilePluginVersionAttr
import androidx.baselineprofile.gradle.utils.ATTRIBUTE_BASELINE_PROFILE_PLUGIN_VERSION
import androidx.baselineprofile.gradle.utils.ATTRIBUTE_TARGET_JVM_ENVIRONMENT
import androidx.baselineprofile.gradle.utils.ATTRIBUTE_USAGE_BASELINE_PROFILE
import androidx.baselineprofile.gradle.utils.agpVersion
import androidx.baselineprofile.gradle.utils.camelCase
import com.android.build.api.AndroidPluginVersion
import com.android.build.api.attributes.BuildTypeAttr
import com.android.build.api.attributes.ProductFlavorAttr
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.java.TargetJvmEnvironment

/**
 * Helps creating configurations for the baseline profile plugin, handling the attributes used
 * between the different modules to exchange artifacts.
 */
internal class ConfigurationManager(private val project: Project) {

    private fun AttributeContainer.usage(value: String) {
        attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class.java, value))
    }

    private fun AttributeContainer.buildType(value: String) {
        attribute(BuildTypeAttr.ATTRIBUTE, project.objects.named(BuildTypeAttr::class.java, value))
    }

    private fun AttributeContainer.targetJvmEnvironment(value: String) {
        attribute(
            TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE,
            project.objects.named(TargetJvmEnvironment::class.java, value)
        )
    }

    private fun AttributeContainer.agpVersion(value: String) {
        attribute(
            BaselineProfilePluginAgpVersionAttr.ATTRIBUTE,
            project.objects.named(BaselineProfilePluginAgpVersionAttr::class.java, value)
        )
    }

    private fun AttributeContainer.baselineProfilePluginVersion(value: String) {
        attribute(
            BaselineProfilePluginVersionAttr.ATTRIBUTE,
            project.objects.named(BaselineProfilePluginVersionAttr::class.java, value)
        )
    }

    @Suppress("UnstableApiUsage")
    private fun AttributeContainer.productFlavors(productFlavors: List<Pair<String, String>>) {
        productFlavors.forEach { (flavorName, flavorValue) ->
            attribute(
                ProductFlavorAttr.of(flavorName),
                project.objects.named(ProductFlavorAttr::class.java, flavorValue)
            )
        }
    }

    fun maybeCreate(
        nameParts: List<String>,
        canBeResolved: Boolean,
        canBeConsumed: Boolean,
        extendFromConfigurations: List<Configuration>? = null,
        buildType: String?,
        productFlavors: List<Pair<String, String>>?,
        usage: String? = ATTRIBUTE_USAGE_BASELINE_PROFILE,
        targetJvmEnvironment: String? = ATTRIBUTE_TARGET_JVM_ENVIRONMENT,
        bpPluginVersion: String? = ATTRIBUTE_BASELINE_PROFILE_PLUGIN_VERSION,
        agpVersion: String? = project.agpVersion().versionString()
    ): Configuration {
        return project.configurations.maybeCreate(camelCase(*(nameParts.toTypedArray()))).apply {
            isCanBeResolved = canBeResolved
            isCanBeConsumed = canBeConsumed

            if (extendFromConfigurations != null) setExtendsFrom(extendFromConfigurations)

            attributes {
                if (buildType != null) it.buildType(buildType)
                if (productFlavors != null) it.productFlavors(productFlavors)
                if (usage != null) it.usage(usage)
                if (targetJvmEnvironment != null) it.targetJvmEnvironment(targetJvmEnvironment)
                if (agpVersion != null) it.agpVersion(agpVersion)
                if (bpPluginVersion != null) it.baselineProfilePluginVersion(bpPluginVersion)
            }
        }
    }

    private fun AndroidPluginVersion.versionString(): String {
        val preview =
            if (!previewType.isNullOrBlank()) {
                "-$previewType$preview"
            } else {
                ""
            }
        return "$major.$minor.$micro$preview"
    }
}
