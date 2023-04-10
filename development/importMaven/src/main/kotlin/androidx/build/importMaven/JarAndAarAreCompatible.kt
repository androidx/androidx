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

import org.gradle.api.artifacts.CacheableRule
import org.gradle.api.attributes.AttributeCompatibilityRule
import org.gradle.api.attributes.CompatibilityCheckDetails
import org.gradle.api.attributes.LibraryElements

/**
 * A [AttributeCompatibilityRule] that makes Gradle consider both aar and jar as compatible
 * artifacts.
 */
@CacheableRule
abstract class JarAndAarAreCompatible : AttributeCompatibilityRule<LibraryElements> {
    override fun execute(t: CompatibilityCheckDetails<LibraryElements>) {
        val consumer = t.consumerValue?.name ?: return
        val producer = t.producerValue?.name ?: return
        if (consumer.isAarOrJar() && producer.isAarOrJar()) {
            t.compatible()
        }
    }

    private fun String.isAarOrJar() = compareTo("jar", ignoreCase = true) == 0 ||
            compareTo("aar", ignoreCase = true) == 0
}
