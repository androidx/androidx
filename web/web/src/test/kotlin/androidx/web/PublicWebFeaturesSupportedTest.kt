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

package androidx.web

import androidx.annotation.RestrictTo
import com.google.common.truth.Truth.assertWithMessage
import net.bytebuddy.description.field.FieldDescription
import net.bytebuddy.pool.TypePool
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Validates that any feature in WebFeature.kt made public (without a @RestrictTo annotation) is
 * properly mapped to a corresponding Chromium CL review link.
 */
@RunWith(JUnit4::class)
class PublicWebFeaturesSupportedTest {

    companion object {
        private val publicFeatures =
            mapOf<String, String>(
                // Example: "NEW_FEATURE" to "https://crrev.com/c/1234abcd"
            )

        private val FieldDescription.InDefinedShape.isPublicStaticFinalString: Boolean
            get() = isPublic && isStatic && isFinal && type.represents(String::class.java)

        private val FieldDescription.InDefinedShape.isRestricted: Boolean
            get() = declaredAnnotations.isAnnotationPresent(RestrictTo::class.java)
    }

    @Test
    fun testPublicFeaturesHaveChromiumLinks() {
        val pool = TypePool.Default.ofSystemLoader()
        val type = pool.describe(WebFeature::class.java.name).resolve()

        val parsedFeatures =
            type.declaredFields
                .asSequence()
                .filter { it.isPublicStaticFinalString }
                .filterNot { it.isRestricted }
                .map { it.name }
                .toSet()

        assertWithMessage("Public features missing a Chromium CL link in publicFeatures")
            .that(parsedFeatures - publicFeatures.keys)
            .isEmpty()

        assertWithMessage("Obsolete features in publicFeatures not found in WebFeature.kt")
            .that(publicFeatures.keys - parsedFeatures)
            .isEmpty()
    }
}
