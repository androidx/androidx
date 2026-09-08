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

package androidx.webkit.test

import androidx.annotation.RestrictTo
import androidx.webkit.WebViewFeature
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.lang.Deprecated
import java.lang.reflect.Modifier.isFinal
import java.lang.reflect.Modifier.isPublic
import java.lang.reflect.Modifier.isStatic
import kotlin.test.Test
import net.bytebuddy.pool.TypePool
import org.junit.Assert.assertNotNull
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Tests that validate [WebViewFeature] string constants.
 *
 * Implemented as a unit test to allow ByteBuddy to read .class files and detect the [RestrictTo]
 * annotation.
 */
@RunWith(JUnit4::class)
class PublicFeatureAvailabilityTest {

    @Test
    fun checkAllPublicFeatureValuesAreDistinct() {
        val duplicateFeatureValues =
            WebViewFeature::class
                .java
                .declaredFields
                .asSequence()
                .filter {
                    isPublic(it.modifiers) &&
                        isStatic(it.modifiers) &&
                        isFinal(it.modifiers) &&
                        it.type == String::class.java
                }
                .map { it.get(null) as String }
                .groupingBy { it }
                .eachCount()
                .filter { it.value > 1 }
                .keys

        assertWithMessage("Duplicate WebViewFeature constant values found")
            .that(duplicateFeatureValues)
            .isEmpty()
    }

    /**
     * There has been several instances of a feature being released (i.e. having the [RestrictTo]
     * annotation removed) without the feature being supported by WebView. Most often, this is
     * because the DEV_SUFFIX is left behind in the Chromium repository.
     *
     * This test guards against that by forcing feature authors to link to the CL where they make
     * the feature available in WebView. It is not a perfect system, but a reminder to the CL
     * reviewers to check.
     */
    @Test
    fun testWebViewFeaturesAreSupported() {
        val pool = TypePool.Default.ofSystemLoader()
        val clazz = WebViewFeature::class.java
        val type = pool.describe(clazz.name).resolve()

        assertNotNull(type)
        val publicApiFields =
            type.declaredFields
                .asSequence()
                .filter {
                    it.isPublic &&
                        it.isStatic &&
                        it.isFinal &&
                        it.type.represents(String::class.java)
                }
                .filterNot { it.declaredAnnotations.isAnnotationPresent(Deprecated::class.java) }
                .filterNot { it.declaredAnnotations.isAnnotationPresent(RestrictTo::class.java) }
                .map { it.actualName }
                .toSet()
        assertThat(publicApiFields).isNotEmpty()

        val publicFieldValuesMissingMapping =
            publicApiFields
                .associateWith { clazz.getField(it).get(null) as String }
                .filterNot { it.value in EXCEPTION_FEATURE_CONSTANTS }
                .filterNot { it.value in PUBLIC_FEATURE_UNHIDE_CLS }
                .map { it.key }
        assertWithMessage(
                "All public feature constants must be linked to the Chromium CL that unhides it. Update the mapping in PublicFeatureAvailability.kt"
            )
            .that(publicFieldValuesMissingMapping)
            .isEmpty()
    }

    @Test
    fun checkAllLinksAreChromiumCls() {
        assertWithMessage(
                "All public features should link to a Chromium CL using short-link syntax"
            )
            .that(
                PUBLIC_FEATURE_UNHIDE_CLS.filterNot { it.value.startsWith("https://crrev.com/c/") }
            )
            .isEmpty()
    }

    @Test
    fun checkAllBugsAreChromiumBugs() {
        assertWithMessage("All exceptions should link to a Chromium bug using short-link syntax")
            .that(
                EXCEPTION_FEATURE_CONSTANTS.filterNot { it.value.startsWith("https://crbug.com/") }
            )
            .isEmpty()
    }

    companion object {
        /**
         * Feature constants that are public in AndroidX but still hidden in WebView.
         *
         * Feature constants should generally <em>not</em> be added here, and must link to bugs that
         * explain the circumstances and how the situation will be resolved.
         */
        private val EXCEPTION_FEATURE_CONSTANTS =
            mapOf(WebViewFeature.HYPERLINK_CONTEXT_MENU_ITEMS to "https://crbug.com/538133088")
    }
}
