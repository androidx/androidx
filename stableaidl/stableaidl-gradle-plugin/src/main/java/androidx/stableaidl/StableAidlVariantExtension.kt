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

package androidx.stableaidl

import com.android.build.api.dsl.CommonExtension
import com.android.build.api.variant.VariantExtension
import com.android.build.api.variant.VariantExtensionConfig
import java.io.Serializable
import javax.inject.Inject
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Property

/** Stable AIDL properties scoped to the AGP variant object. */
@Suppress("UnstableApiUsage") // for VariantExtensionConfig
abstract class StableAidlVariantExtension
@Inject
constructor(
    config: VariantExtensionConfig<*>,
    project: Project,
) : VariantExtension, Serializable {
    abstract val version: Property<Int>

    init {
        version.set(
            config.buildTypeExtension(StableAidlBuildTypeDslExtension::class.java).version
                ?: (project.extensions.getByType(CommonExtension::class.java) as ExtensionAware)
                    .extensions
                    .findByType(StableAidlProjectDslExtension::class.java)
                    ?.version
                ?: throw GradleException("Must declare a Stable AIDL version")
        )
    }
}
