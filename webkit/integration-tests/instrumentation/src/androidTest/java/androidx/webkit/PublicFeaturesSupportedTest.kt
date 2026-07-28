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

package androidx.webkit

import androidx.annotation.RestrictTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.webkit.internal.ApiFeature
import androidx.webkit.internal.StartupApiFeature
import androidx.webkit.internal.WebViewFeatureInternal
import com.google.common.truth.Truth.assertWithMessage
import java.lang.Deprecated
import java.lang.reflect.Field
import java.lang.reflect.Modifier.isFinal
import java.lang.reflect.Modifier.isPublic
import java.lang.reflect.Modifier.isStatic
import kotlin.reflect.KClass
import org.chromium.support_lib_boundary.util.Features
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests to prevent accidentally releasing APIs that are not supported by WebView.
 *
 * Only runs on SDK 29+ because we don't install WebView updates on lower SDK devices
 */
@SmallTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 29)
class PublicFeaturesSupportedTest {

    @Before
    fun setUp() {
        // We need to touch the WebViewFeatureInternal to get the classloader to
        // initialize the arrays of [ApiFeature] and [StartupApiFeature].
        // Otherwise, the call to .values() will return an empty set.
        // It does not matter which field we touch. The important part is that the
        // WebViewFeatureInternal class is loaded.
        assertNotNull(WebViewFeatureInternal.PROXY_OVERRIDE)
    }

    /**
     * Asserts that all string constants in [WebViewFeature] are mapped to a WebView boundary
     * feature constant through either [ApiFeature] or [StartupApiFeature].
     */
    @Test
    fun testAllPublicFeaturesAreMapped() {
        val mappedFeatures =
            ApiFeature.values().map { it.publicFeatureName }.toSet() +
                StartupApiFeature.values().map { it.publicFeatureName }.toSet()

        val declaredFeatures =
            WebViewFeature::class.getPublicStaticFinalStrings()
                .filterNot(::isDeprecated)
                .map(::fieldToStringValue)
                .toSet()

        val unmappedFeatures = declaredFeatures - mappedFeatures
        assertWithMessage("These feature strings are not mapped").that(unmappedFeatures).isEmpty()
    }

    /**
     * Test that asserts that all unhidden constants on [WebViewFeature] are supported by the test
     * runner WebView without DEV_SUFFIX.
     */
    @Test
    fun testAllVisibleApiFeaturesAreSupported() {
        // Create a mapping from public feature values to boundary interface values.
        // This filter implicitly filters out any constants for startup features, since they are not
        // part of the ApiFeature "enum".
        val boundaryInterfaceValueByPublicValue =
            ApiFeature.values().associate {
                it.publicFeatureName to it.internalFeatureValueForTesting
            }

        // Features marked as deprecated in the boundary interface are expected to not be supported.
        val deprecatedFeatures =
            Features::class.getPublicStaticFinalStrings()
                .filter(::isDeprecated)
                .map(::fieldToStringValue)
                .toSet()

        val expectedSupportedFeatures =
            WebViewFeature::class.getPublicStaticFinalStrings()
                .filterNot(::isHidden)
                // Get the actual values and filter out any exceptions.
                .map(::fieldToStringValue)
                .filterNotIn(EXEMPT_PUBLIC_FEATURES)
                // Convert to the boundary interface values and remove any values
                // that are marked as @Deprecated.
                .mapNotNull { boundaryInterfaceValueByPublicValue[it] }
                .filterNotIn(deprecatedFeatures)
                .toSet()

        val actuallySupportedFeatures = ApiFeature.getWebViewApkFeaturesForTesting()
        val missingFeatures = expectedSupportedFeatures - actuallySupportedFeatures
        assertWithMessage("These feature strings are not supported by WebView")
            .that(missingFeatures)
            .isEmpty()
    }

    /**
     * Asserts that all feature constants for startup features are supported by the installed
     * WebView.
     */
    @Test
    fun testAllPublicStartupFeaturesAreSupported() {
        val startupFeatureByPublicFeatureName =
            StartupApiFeature.values().associateBy { it.publicFeatureName }

        val unsupportedFeatures =
            WebViewFeature::class.getPublicStaticFinalStrings()
                .filterNot(::isHidden)
                // Get the actual values and filter out any exceptions.
                .map(::fieldToStringValue)
                .filterNotIn(EXEMPT_PUBLIC_FEATURES)
                // Convert to StartupFeatures, and check they are supported
                .mapNotNull { startupFeatureByPublicFeatureName[it] }
                .filterNot { it.isSupportedByWebView(ApplicationProvider.getApplicationContext()) }
                .map { it.internalFeatureValueForTesting }
                .toSet()

        assertWithMessage("These Startup Features are not supported by WebView")
            .that(unsupportedFeatures)
            .isEmpty()
    }

    /**
     * Extension method for classes that return all `public static final String` fields as a
     * [Sequence].
     */
    private fun KClass<*>.getPublicStaticFinalStrings(): Sequence<Field> =
        this.java.declaredFields.asSequence().filter {
            isPublic(it.modifiers) &&
                isStatic(it.modifiers) &&
                isFinal(it.modifiers) &&
                it.type == String::class.java
        }

    private fun isDeprecated(field: Field): Boolean =
        field.isAnnotationPresent(Deprecated::class.java)

    private fun isHidden(field: Field): Boolean = field.isAnnotationPresent(RestrictTo::class.java)

    /** Maps a static [Field] to the field value as a [String]. */
    private fun fieldToStringValue(field: Field): String = field.get(null) as String

    /** Retain elements in the sequence that are not in the provided [filterSet]. */
    private fun <T : Any> Sequence<T>.filterNotIn(filterSet: Set<T>): Sequence<T> =
        this.filterNot(filterSet::contains)

    companion object {
        /**
         * This set contains features that are unhidden despite not being supported by the WebView
         * used by test runners.
         *
         * Features may only be added to this set after consultation with library owners.
         */
        private val EXEMPT_PUBLIC_FEATURES =
            setOf(
                // TODO(https://crbug.com/538133088): Under investigation.
                WebViewFeature.HYPERLINK_CONTEXT_MENU_ITEMS,
                // TODO(http://b/397385172): This feature is conditionally enabled for now.
                WebViewFeature.ENQUEUE_PRECONNECT,
                // Landed in https://crrev.com/c/8135521. Remove this line once it's been dropped.
                WebViewFeature.PREFETCH_CACHE_V1,
                // Landed in https://crrev.com/c/8135521. Remove this line once it's been dropped.
                WebViewFeature.SET_MAX_PRERENDERS_V1,
            )
    }
}
