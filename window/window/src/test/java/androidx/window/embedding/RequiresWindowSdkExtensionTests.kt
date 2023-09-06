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

import android.app.ActivityOptions
import android.content.Context
import android.os.Binder
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.window.RequiresWindowSdkExtension
import androidx.window.WindowSdkExtensions
import androidx.window.WindowSdkExtensionsRule
import androidx.window.core.ConsumerAdapter
import androidx.window.core.PredicateAdapter
import androidx.window.embedding.EmbeddingAdapter.Companion.INVALID_ACTIVITY_STACK_TOKEN
import androidx.window.embedding.EmbeddingAdapter.Companion.INVALID_SPLIT_INFO_TOKEN
import androidx.window.extensions.core.util.function.Function
import androidx.window.extensions.embedding.ActivityEmbeddingComponent
import androidx.window.extensions.embedding.SplitAttributes as OemSplitAttributes
import androidx.window.extensions.embedding.SplitAttributesCalculatorParams as OemSplitAttributesCalculatorParams
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Verifies the behavior of [RequiresWindowSdkExtension]
 * - If the [WindowSdkExtensions.extensionVersion] is greater than or equal to the minimum required
 *   version denoted in [RequiresWindowSdkExtension.version], the denoted API must be called
 *   successfully
 * - Otherwise, [UnsupportedOperationException] must be thrown.
 */
@RequiresApi(Build.VERSION_CODES.M) // To call ActivityOptions.makeBasic()
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
    private lateinit var activityOptions: ActivityOptions

    private lateinit var mockAnnotations: AutoCloseable
    private lateinit var embeddingCompat: EmbeddingCompat

    @Before
    fun setUp() {
        mockAnnotations = MockitoAnnotations.openMocks(this)
        embeddingCompat = EmbeddingCompat(
            embeddingExtension,
            EmbeddingAdapter(PredicateAdapter(classLoader)),
            ConsumerAdapter(classLoader),
            applicationContext
        )

        doReturn(activityOptions).whenever(embeddingExtension).setLaunchingActivityStack(
            activityOptions,
            INVALID_ACTIVITY_STACK_TOKEN
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
            embeddingCompat.setLaunchingActivityStack(activityOptions, Binder())
        }
        verify(embeddingExtension, never()).setLaunchingActivityStack(any(), any())

        assertThrows(UnsupportedOperationException::class.java) {
            embeddingCompat.finishActivityStacks(emptySet())
        }
        verify(embeddingExtension, never()).finishActivityStacks(any())

        assertThrows(UnsupportedOperationException::class.java) {
            embeddingCompat.updateSplitAttributes(TEST_SPLIT_INFO, TEST_SPLIT_ATTRIBUTES)
        }
        verify(embeddingExtension, never()).updateSplitAttributes(any(), any())

        assertThrows(UnsupportedOperationException::class.java) {
            embeddingCompat.invalidateTopVisibleSplitAttributes()
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
            embeddingCompat.setLaunchingActivityStack(activityOptions, INVALID_ACTIVITY_STACK_TOKEN)
        }
        verify(embeddingExtension, never()).setLaunchingActivityStack(any(), any())

        assertThrows(UnsupportedOperationException::class.java) {
            embeddingCompat.finishActivityStacks(emptySet())
        }
        verify(embeddingExtension, never()).finishActivityStacks(any())

        assertThrows(UnsupportedOperationException::class.java) {
            embeddingCompat.updateSplitAttributes(TEST_SPLIT_INFO, TEST_SPLIT_ATTRIBUTES)
        }
        verify(embeddingExtension, never()).updateSplitAttributes(any(), any())

        assertThrows(UnsupportedOperationException::class.java) {
            embeddingCompat.invalidateTopVisibleSplitAttributes()
        }
        verify(embeddingExtension, never()).invalidateTopVisibleSplitAttributes()
    }

    @Test
    fun testVendorApiLevel3() {
        testRule.overrideExtensionVersion(3)

        embeddingCompat.setSplitAttributesCalculator { _ -> TEST_SPLIT_ATTRIBUTES }
        verify(embeddingExtension).setSplitAttributesCalculator(
            any<Function<OemSplitAttributesCalculatorParams, OemSplitAttributes>>()
        )

        embeddingCompat.clearSplitAttributesCalculator()
        verify(embeddingExtension).clearSplitAttributesCalculator()

        embeddingCompat.setLaunchingActivityStack(activityOptions, INVALID_ACTIVITY_STACK_TOKEN)

        verify(embeddingExtension).setLaunchingActivityStack(
            activityOptions,
            INVALID_ACTIVITY_STACK_TOKEN
        )

        embeddingCompat.finishActivityStacks(emptySet())
        verify(embeddingExtension).finishActivityStacks(emptySet())

        embeddingCompat.updateSplitAttributes(TEST_SPLIT_INFO, TEST_SPLIT_ATTRIBUTES)
        verify(embeddingExtension).updateSplitAttributes(
            INVALID_SPLIT_INFO_TOKEN,
            OemSplitAttributes.Builder().build()
        )

        embeddingCompat.invalidateTopVisibleSplitAttributes()
        verify(embeddingExtension).invalidateTopVisibleSplitAttributes()
    }

    companion object {
        private val TEST_SPLIT_INFO = SplitInfo(
            ActivityStack(emptyList(), isEmpty = true, INVALID_ACTIVITY_STACK_TOKEN),
            ActivityStack(emptyList(), isEmpty = true, INVALID_ACTIVITY_STACK_TOKEN),
            SplitAttributes.Builder().build(),
            INVALID_SPLIT_INFO_TOKEN,
        )

        private val TEST_SPLIT_ATTRIBUTES = SplitAttributes.Builder().build()
    }
}
