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

package androidx.build.importMaven

import org.gradle.api.artifacts.ComponentMetadataContext
import org.gradle.api.artifacts.ComponentMetadataRule

/**
 * Some artifacts don't get resolved via configuration attributes.
 * e.g. sources:
 *   https://github.com/gradle/gradle/commit/94b266dc50b5fd7dd5460d6d32ff66eab3740627
 *
 * We use this ComponentMetadataRule to add them.
 */
class CustomMetadataRules : ComponentMetadataRule {
    override fun execute(context: ComponentMetadataContext) {
        val id = context.details.id
        context.details.allVariants { variantMetadata ->
            variantMetadata.withFiles {
                // sources do not always get resolved for nested dependencies
                it.addFile("${id.name}-${id.version}-sources.jar")
                // if it does not have gradle metadata, we might miss aar; add it
                it.addFile("${id.name}-${id.version}.aar")
            }
        }
    }
}