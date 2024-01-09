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

package androidx.window.embedding

import android.content.Context
import android.os.Bundle
import android.os.IBinder
import androidx.window.RequiresWindowSdkExtension
import androidx.window.WindowSdkExtensions
import androidx.window.WindowSdkExtensionsRule
import androidx.window.core.ConsumerAdapter
import androidx.window.core.PredicateAdapter
import androidx.window.embedding.EmbeddingAdapter.Companion.INVALID_SPLIT_INFO_TOKEN
import androidx.window.extensions.core.util.function.Function
import androidx.window.extensions.embedding.ActivityEmbeddingComponent
import androidx.window.extensions.embedding.ActivityEmbeddingOptionsProperties
import androidx.window.extensions.embedding.ActivityStack.Token as ActivityStackToken
import androidx.window.extensions.embedding.SplitAttributes as OemSplitAttributes
import androidx.window.extensions.embedding.SplitAttributesCalculatorParams as OemSplitAttributesCalculatorParams
import androidx.window.extensions.embedding.SplitInfo.Token as SplitInfoToken
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

/**
 * Verifies the behavior of [RequiresWindowSdkExtension]
 * - If the [WindowSdkExtensions.extensionVersion] is greater than or equal to the minimum required
 *   version denoted in [RequiresWindowSdkExtension.version], the denoted API must be called
 *   successfully
 * - Otherwise, [UnsupportedOperationException] must be thrown.
 */
@Suppress("Deprecation")
class RequiresWindowSdkExtensionTests {

    @get:Rule
    val testRule = WindowSdkExtensionsRule()

    @Mock
    private lateinit var embeddingExtension: ActivityEmbeddingComponent
    @Mock
    private lateinit var classLoader: ClassLoader
    @Mock
    private lateinit var applicationContext: Context
    @Mock
    private lateinit var options: Bundle

    private lateinit var mockAnnotations: AutoCloseable
    private lateinit var embeddingCompat: EmbeddingCompat
    private lateinit var activityStack: ActivityStack

    private val activityStackToken = ActivityStackToken.INVALID_ACTIVITY_STACK_TOKEN

    @Before
    fun setUp() {
        mockAnnotations = MockitoAnnotations.openMocks(this)
        activityStack = ActivityStack(emptyList(), isEmpty = true, activityStackToken)
        embeddingCompat = EmbeddingCompat(
            embeddingExtension,
            EmbeddingAdapter(PredicateAdapter(classLoader)),
            ConsumerAdapter(classLoader),
            applicationContext
        )
    }

    @After
    fun tearDown() {
        mockAnnotations.close()
    }

    @Test
    fun testVendorApiLevel1() {
        testRule.overrideExtensionVersion(1)

        assertThrows(UnsupportedOperationException::class.java) {
            embeddingCompat.setSplitAttributesCalculator { _ -> TEST_SPLIT_ATTRIBUTES }
        }
        verify(embeddingExtension, never()).setSplitAttributesCalculator(any())

        assertThrows(UnsupportedOperationException::class.java) {
            embeddingCompat.clearSplitAttributesCalculator()
        }
        verify(embeddingExtension, never()).clearSplitAttributesCalculator()

        assertThrows(UnsupportedOperationException::class.java) {
            embeddingCompat.setLaunchingActivityStack(options, activityStack)
        }
        verify(options, never()).putBinder(any(), any())

        assertThrows(UnsupportedOperationException::class.java) {
            embeddingCompat.finishActivityStacks(emptySet())
        }
        verify(embeddingExtension, never()).finishActivityStacksWithTokens(
            any<Set<ActivityStackToken>>()
        )

        assertThrows(UnsupportedOperationException::class.java) {
            embeddingCompat.updateSplitAttributes(TEST_SPLIT_INFO, TEST_SPLIT_ATTRIBUTES)
        }
        verify(embeddingExtension, never()).updateSplitAttributes(any<IBinder>(), any())

        assertThrows(UnsupportedOperationException::class.java) {
            embeddingCompat.invalidateVisibleActivityStacks()
        }
        verify(embeddingExtension, never()).invalidateTopVisibleSplitAttributes()
    }

    @Test
    fun testVendorApiLevel2() {
        testRule.overrideExtensionVersion(2)

        embeddingCompat.setSplitAttributesCalculator { _ -> TEST_SPLIT_ATTRIBUTES }
        verify(embeddingExtension).setSplitAttributesCalculator(
            any<Function<OemSplitAttributesCalculatorParams, OemSplitAttributes>>()
        )

        embeddingCompat.clearSplitAttributesCalculator()
        verify(embeddingExtension).clearSplitAttributesCalculator()

        assertThrows(UnsupportedOperationException::class.java) {
            embeddingCompat.setLaunchingActivityStack(options, activityStack)
        }
        verify(options, never()).putBundle(any(), any())

        assertThrows(UnsupportedOperationException::class.java) {
            embeddingCompat.finishActivityStacks(emptySet())
        }
        verify(embeddingExtension, never()).finishActivityStacksWithTokens(
            any<Set<ActivityStackToken>>()
        )

        assertThrows(UnsupportedOperationException::class.java) {
            embeddingCompat.updateSplitAttributes(TEST_SPLIT_INFO, TEST_SPLIT_ATTRIBUTES)
        }
        verify(embeddingExtension, never()).updateSplitAttributes(any<IBinder>(), any())

        assertThrows(UnsupportedOperationException::class.java) {
            embeddingCompat.invalidateVisibleActivityStacks()
        }
        verify(embeddingExtension, never()).invalidateTopVisibleSplitAttributes()
    }

    @Test
    fun testVendorApiLevel3() {
        testRule.overrideExtensionVersion(3)

        val splitInfo = SplitInfo(
            ActivityStack(emptyList(), isEmpty = true),
            ActivityStack(emptyList(), isEmpty = true),
            SplitAttributes.Builder().build(),
            binder = INVALID_SPLIT_INFO_TOKEN,
        )

        embeddingCompat.setSplitAttributesCalculator { _ -> TEST_SPLIT_ATTRIBUTES }
        verify(embeddingExtension).setSplitAttributesCalculator(
            any<Function<OemSplitAttributesCalculatorParams, OemSplitAttributes>>()
        )

        embeddingCompat.clearSplitAttributesCalculator()
        verify(embeddingExtension).clearSplitAttributesCalculator()

        assertThrows(UnsupportedOperationException::class.java) {
            embeddingCompat.setLaunchingActivityStack(options, activityStack)
        }
        verify(options, never()).putBundle(any(), any())

        assertThrows(UnsupportedOperationException::class.java) {
            embeddingCompat.finishActivityStacks(emptySet())
        }
        verify(embeddingExtension, never()).finishActivityStacksWithTokens(
            any<Set<ActivityStackToken>>()
        )

        embeddingCompat.updateSplitAttributes(splitInfo, TEST_SPLIT_ATTRIBUTES)
        verify(embeddingExtension).updateSplitAttributes(
            splitInfo.getBinder(),
            OemSplitAttributes.Builder().build()
        )

        embeddingCompat.invalidateVisibleActivityStacks()
        verify(embeddingExtension).invalidateTopVisibleSplitAttributes()
    }

    @Test
    fun testVendorApiLevel4() {
        testRule.overrideExtensionVersion(4)

        val splitInfo = SplitInfo(
            ActivityStack(emptyList(), isEmpty = true),
            ActivityStack(emptyList(), isEmpty = true),
            SplitAttributes.Builder().build(),
            binder = INVALID_SPLIT_INFO_TOKEN,
        )

        embeddingCompat.setSplitAttributesCalculator { _ -> TEST_SPLIT_ATTRIBUTES }
        verify(embeddingExtension).setSplitAttributesCalculator(
            any<Function<OemSplitAttributesCalculatorParams, OemSplitAttributes>>()
        )

        embeddingCompat.clearSplitAttributesCalculator()
        verify(embeddingExtension).clearSplitAttributesCalculator()

        assertThrows(UnsupportedOperationException::class.java) {
            embeddingCompat.setLaunchingActivityStack(options, activityStack)
        }
        verify(options, never()).putBinder(any(), any())

        assertThrows(UnsupportedOperationException::class.java) {
            embeddingCompat.finishActivityStacks(emptySet())
        }
        verify(embeddingExtension, never()).finishActivityStacksWithTokens(
            any<Set<ActivityStackToken>>()
        )

        embeddingCompat.updateSplitAttributes(splitInfo, TEST_SPLIT_ATTRIBUTES)
        verify(embeddingExtension).updateSplitAttributes(
            splitInfo.getBinder(),
            OemSplitAttributes.Builder().build()
        )

        embeddingCompat.invalidateVisibleActivityStacks()
        verify(embeddingExtension).invalidateTopVisibleSplitAttributes()
    }

    @Test
    fun testVendorApiLevel5() {
        testRule.overrideExtensionVersion(5)

        val splitInfo = SplitInfo(
            ActivityStack(emptyList(), isEmpty = true),
            ActivityStack(emptyList(), isEmpty = true),
            SplitAttributes.Builder().build(),
            token = SplitInfoToken.createFromBinder(INVALID_SPLIT_INFO_TOKEN),
        )

        embeddingCompat.setSplitAttributesCalculator { _ -> TEST_SPLIT_ATTRIBUTES }
        verify(embeddingExtension).setSplitAttributesCalculator(
            any<Function<OemSplitAttributesCalculatorParams, OemSplitAttributes>>()
        )

        embeddingCompat.clearSplitAttributesCalculator()
        verify(embeddingExtension).clearSplitAttributesCalculator()

        embeddingCompat.setLaunchingActivityStack(options, activityStack)
        verify(options).putBundle(
            eq(ActivityEmbeddingOptionsProperties.KEY_ACTIVITY_STACK_TOKEN),
            any()
        )

        embeddingCompat.finishActivityStacks(emptySet())
        verify(embeddingExtension).finishActivityStacksWithTokens(emptySet())

        embeddingCompat.updateSplitAttributes(splitInfo, TEST_SPLIT_ATTRIBUTES)
        verify(embeddingExtension).updateSplitAttributes(
            splitInfo.getToken(),
            OemSplitAttributes.Builder().build()
        )

        embeddingCompat.invalidateVisibleActivityStacks()
        verify(embeddingExtension).invalidateTopVisibleSplitAttributes()
    }

    companion object {
        private val TEST_SPLIT_INFO = SplitInfo(
            ActivityStack(emptyList(), isEmpty = true),
            ActivityStack(emptyList(), isEmpty = true),
            SplitAttributes.Builder().build(),
        )

        private val TEST_SPLIT_ATTRIBUTES = SplitAttributes.Builder().build()
    }
}
