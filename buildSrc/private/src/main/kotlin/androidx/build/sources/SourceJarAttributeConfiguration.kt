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

package androidx.build.sources

import org.gradle.api.Project
import org.gradle.api.attributes.AttributeCompatibilityRule
import org.gradle.api.attributes.AttributeDisambiguationRule
import org.gradle.api.attributes.CompatibilityCheckDetails
import org.gradle.api.attributes.MultipleCandidatesDetails
import org.gradle.api.attributes.Usage
import org.gradle.kotlin.dsl.named

/** Container for attribute resolution rules related to resolving source jars. */
internal object SourceJarAttributeConfiguration {
    /** Sets up [project] to use [SourceJarCompatibilityRule] and [SourceJarDisambiguationRule]. */
    fun register(project: Project) {
        project.dependencies.attributesSchema { schema ->
            schema.attribute(Usage.USAGE_ATTRIBUTE) { usage ->
                usage.disambiguationRules.add(SourceJarDisambiguationRule::class.java)
                usage.compatibilityRules.add(SourceJarCompatibilityRule::class.java)
            }
        }
    }

    /**
     * Rule that sets both the [Usage] values [MULTIPLATFORM_USAGE_NAME] and [Usage.JAVA_RUNTIME] to
     * be compatible with the [DOCS_SOURCE_JAR_USAGE_NAME] value.
     */
    class SourceJarCompatibilityRule : AttributeCompatibilityRule<Usage> {
        override fun execute(t: CompatibilityCheckDetails<Usage>) {
            if (t.consumerValue?.name == DOCS_SOURCE_JAR_USAGE_NAME) {
                when (t.producerValue?.name) {
                    MULTIPLATFORM_USAGE_NAME,
                    Usage.JAVA_RUNTIME -> t.compatible()
                }
            }
        }
    }

    /**
     * Rule to pick between multiple matching [Usage] values when the requested value is
     * [DOCS_SOURCE_JAR_USAGE_NAME].
     *
     * A KMP project will have source jars with both [MULTIPLATFORM_USAGE_NAME] and
     * [Usage.JAVA_RUNTIME] values for [Usage]. The multiplatform variant is preferred if it exists.
     */
    class SourceJarDisambiguationRule : AttributeDisambiguationRule<Usage> {
        override fun execute(t: MultipleCandidatesDetails<Usage>) {
            if (t.consumerValue?.name == DOCS_SOURCE_JAR_USAGE_NAME) {
                val closestMatch =
                    t.candidateValues.firstOrNull { it.name == MULTIPLATFORM_USAGE_NAME }
                        ?: t.candidateValues.firstOrNull { it.name == Usage.JAVA_RUNTIME }
                if (closestMatch != null) {
                    t.closestMatch(closestMatch)
                }
            }
        }
    }

    private const val MULTIPLATFORM_USAGE_NAME = "androidx-multiplatform-docs"

    /** The [Usage] value for a KMP source jar created by the AndroidX build. */
    val Project.multiplatformUsage
        get() = objects.named<Usage>(MULTIPLATFORM_USAGE_NAME)

    private const val DOCS_SOURCE_JAR_USAGE_NAME = "androidx-docs-source-jar"

    /** A [Usage] value intended to resolve either the KMP or regular source jar of a project. */
    internal val Project.docsSourceJarUsage
        get() = objects.named<Usage>(DOCS_SOURCE_JAR_USAGE_NAME)
}
