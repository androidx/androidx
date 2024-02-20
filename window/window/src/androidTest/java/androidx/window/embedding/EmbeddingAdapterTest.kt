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

package androidx.window.embedding

import android.app.Activity
import android.graphics.Color
import android.os.Binder
import android.os.IBinder
import androidx.window.WindowSdkExtensions
import androidx.window.WindowTestUtils
import androidx.window.core.ExtensionsUtil
import androidx.window.core.PredicateAdapter
import androidx.window.embedding.EmbeddingAdapter.Companion.INVALID_SPLIT_INFO_TOKEN
import androidx.window.embedding.SplitAttributes.SplitType
import androidx.window.embedding.SplitAttributes.SplitType.Companion.SPLIT_TYPE_HINGE
import androidx.window.extensions.embedding.ActivityStack as OEMActivityStack
import androidx.window.extensions.embedding.ActivityStack.Token as OEMActivityStackToken
import androidx.window.extensions.embedding.AnimationBackground as OEMEmbeddingAnimationBackground
import androidx.window.extensions.embedding.SplitAttributes as OEMSplitAttributes
import androidx.window.extensions.embedding.SplitAttributes.LayoutDirection.TOP_TO_BOTTOM
import androidx.window.extensions.embedding.SplitAttributes.SplitType.RatioSplitType
import androidx.window.extensions.embedding.SplitInfo as OEMSplitInfo
import androidx.window.extensions.embedding.SplitInfo.Token as OEMSplitInfoToken
import kotlin.test.Ignore
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/** Tests for [EmbeddingAdapter] */
class EmbeddingAdapterTest {
    private lateinit var adapter: EmbeddingAdapter

    private val extensionVersion = WindowSdkExtensions.getInstance().extensionVersion

    @Before
    fun setUp() {
        adapter = EmbeddingBackend::class.java.classLoader?.let { loader ->
            EmbeddingAdapter(PredicateAdapter(loader))
        }!!
    }

    @Test
    fun testTranslateSplitInfoWithDefaultAttrs() {
        WindowTestUtils.assumeAtLeastVendorApiLevel(2)
        WindowTestUtils.assumeBeforeVendorApiLevel(3)

        val oemSplitInfo = createTestOEMSplitInfo(
            createTestOEMActivityStack(ArrayList(), true),
            createTestOEMActivityStack(ArrayList(), true),
            OEMSplitAttributes.Builder().build(),
        )
        val expectedSplitInfo = SplitInfo(
            ActivityStack(
                ArrayList(),
                isEmpty = true,
                OEMActivityStackToken.INVALID_ACTIVITY_STACK_TOKEN
            ),
            ActivityStack(
                ArrayList(),
                isEmpty = true,
                OEMActivityStackToken.INVALID_ACTIVITY_STACK_TOKEN
            ),
            SplitAttributes.Builder()
                .setSplitType(SplitType.SPLIT_TYPE_EQUAL)
                .setLayoutDirection(SplitAttributes.LayoutDirection.LOCALE)
                .build(),
        )
        assertEquals(listOf(expectedSplitInfo), adapter.translate(listOf(oemSplitInfo)))
    }

    @Test
    fun testTranslateSplitInfoWithExpandingContainers() {
        WindowTestUtils.assumeAtLeastVendorApiLevel(2)
        WindowTestUtils.assumeBeforeVendorApiLevel(3)

        val oemSplitInfo = createTestOEMSplitInfo(
            createTestOEMActivityStack(ArrayList(), true),
            createTestOEMActivityStack(ArrayList(), true),
            OEMSplitAttributes.Builder()
                .setSplitType(OEMSplitAttributes.SplitType.ExpandContainersSplitType())
                .build(),
        )
        val expectedSplitInfo = SplitInfo(
            ActivityStack(
                ArrayList(),
                isEmpty = true,
                OEMActivityStackToken.INVALID_ACTIVITY_STACK_TOKEN
            ),
            ActivityStack(
                ArrayList(),
                isEmpty = true,
                OEMActivityStackToken.INVALID_ACTIVITY_STACK_TOKEN
            ),
            SplitAttributes.Builder()
                .setSplitType(SplitType.SPLIT_TYPE_EXPAND)
                .setLayoutDirection(SplitAttributes.LayoutDirection.LOCALE)
                .build(),
        )
        assertEquals(listOf(expectedSplitInfo), adapter.translate(listOf(oemSplitInfo)))
    }

    @Suppress("DEPRECATION")
    @Test
    fun testTranslateSplitInfoWithApiLevel1() {
        WindowTestUtils.assumeBeforeVendorApiLevel(2)

        val activityStack = createTestOEMActivityStack(ArrayList(), true)
        val expectedSplitRatio = 0.3f
        val oemSplitInfo = mock<OEMSplitInfo>().apply {
            whenever(primaryActivityStack).thenReturn(activityStack)
            whenever(secondaryActivityStack).thenReturn(activityStack)
            whenever(splitRatio).thenReturn(expectedSplitRatio)
        }

        val expectedSplitInfo = SplitInfo(
            ActivityStack(
                ArrayList(),
                isEmpty = true,
                OEMActivityStackToken.INVALID_ACTIVITY_STACK_TOKEN
            ),
            ActivityStack(
                ArrayList(),
                isEmpty = true,
                OEMActivityStackToken.INVALID_ACTIVITY_STACK_TOKEN
            ),
            SplitAttributes.Builder()
                .setSplitType(SplitType.ratio(expectedSplitRatio))
                // OEMSplitInfo with Vendor API level 1 doesn't provide layoutDirection.
                .setLayoutDirection(SplitAttributes.LayoutDirection.LOCALE)
                .build(),
        )
        assertEquals(listOf(expectedSplitInfo), adapter.translate(listOf(oemSplitInfo)))
    }

    @Test
    fun testTranslateSplitInfoWithApiLevel2() {
        WindowTestUtils.assumeAtLeastVendorApiLevel(2)
        WindowTestUtils.assumeBeforeVendorApiLevel(3)

        val oemSplitInfo = createTestOEMSplitInfo(
            createTestOEMActivityStack(ArrayList(), true),
            createTestOEMActivityStack(ArrayList(), true),
            OEMSplitAttributes.Builder()
                .setSplitType(OEMSplitAttributes.SplitType.HingeSplitType(RatioSplitType(0.5f)))
                .setLayoutDirection(TOP_TO_BOTTOM)
                .build(),
        )
        val expectedSplitInfo = SplitInfo(
            ActivityStack(
                ArrayList(),
                isEmpty = true,
                OEMActivityStackToken.INVALID_ACTIVITY_STACK_TOKEN
            ),
            ActivityStack(
                ArrayList(),
                isEmpty = true,
                OEMActivityStackToken.INVALID_ACTIVITY_STACK_TOKEN
            ),
            SplitAttributes.Builder()
                .setSplitType(SPLIT_TYPE_HINGE)
                .setLayoutDirection(SplitAttributes.LayoutDirection.TOP_TO_BOTTOM)
                .build(),
        )
        assertEquals(listOf(expectedSplitInfo), adapter.translate(listOf(oemSplitInfo)))
    }

    @Test
    fun testTranslateSplitInfoWithApiLevel3() {
        WindowTestUtils.assumeAtLeastVendorApiLevel(3)
        WindowTestUtils.assumeBeforeVendorApiLevel(5)

        val testSplitInfoToken = Binder()
        val oemSplitInfo = createTestOEMSplitInfo(
            createTestOEMActivityStack(ArrayList(), true),
            createTestOEMActivityStack(ArrayList(), true),
            OEMSplitAttributes.Builder()
                .setSplitType(OEMSplitAttributes.SplitType.HingeSplitType(RatioSplitType(0.5f)))
                .setLayoutDirection(TOP_TO_BOTTOM)
                .build(),
            testSplitInfoToken,
        )
        val expectedSplitInfo = SplitInfo(
            ActivityStack(ArrayList(), isEmpty = true),
            ActivityStack(ArrayList(), isEmpty = true),
            SplitAttributes.Builder()
                .setSplitType(SPLIT_TYPE_HINGE)
                .setLayoutDirection(SplitAttributes.LayoutDirection.TOP_TO_BOTTOM)
                .build(),
            testSplitInfoToken,
        )
        assertEquals(listOf(expectedSplitInfo), adapter.translate(listOf(oemSplitInfo)))
    }

    @Ignore // TODO(b/322056156): enable after the change is applied to platform prebuilt library.
    @Test
    fun testTranslateSplitInfoWithApiLevel5() {
        WindowTestUtils.assumeAtLeastVendorApiLevel(5)

        val oemSplitInfo = createTestOEMSplitInfo(
            createTestOEMActivityStack(
                emptyList(),
                true,
                OEMActivityStackToken.INVALID_ACTIVITY_STACK_TOKEN,
            ),
            createTestOEMActivityStack(
                emptyList(),
                true,
                OEMActivityStackToken.INVALID_ACTIVITY_STACK_TOKEN,
            ),
            OEMSplitAttributes.Builder()
                .setSplitType(OEMSplitAttributes.SplitType.HingeSplitType(RatioSplitType(0.5f)))
                .setLayoutDirection(TOP_TO_BOTTOM)
                .build(),
            testToken = OEMSplitInfoToken.createFromBinder(INVALID_SPLIT_INFO_TOKEN),
        )
        val expectedSplitInfo = SplitInfo(
            ActivityStack(
                emptyList(),
                isEmpty = true,
                OEMActivityStackToken.INVALID_ACTIVITY_STACK_TOKEN
            ),
            ActivityStack(
                emptyList(),
                isEmpty = true,
                OEMActivityStackToken.INVALID_ACTIVITY_STACK_TOKEN
            ),
            SplitAttributes.Builder()
                .setSplitType(SPLIT_TYPE_HINGE)
                .setLayoutDirection(SplitAttributes.LayoutDirection.TOP_TO_BOTTOM)
                .build(),
            token = OEMSplitInfoToken.createFromBinder(INVALID_SPLIT_INFO_TOKEN),
        )
        assertEquals(listOf(expectedSplitInfo), adapter.translate(listOf(oemSplitInfo)))
    }

    @Test
    fun testTranslateAnimationBackgroundWithApiLevel5() {
        WindowTestUtils.assumeAtLeastVendorApiLevel(5)

        val colorBackground = EmbeddingAnimationBackground.createColorBackground(Color.BLUE)
        val splitAttributesWithColorBackground = SplitAttributes.Builder()
            .setAnimationBackground(colorBackground)
            .build()
        val splitAttributesWithDefaultBackground = SplitAttributes.Builder()
            .setAnimationBackground(EmbeddingAnimationBackground.DEFAULT)
            .build()

        val extensionsColorBackground =
            OEMEmbeddingAnimationBackground.createColorBackground(Color.BLUE)
        val extensionsSplitAttributesWithColorBackground = OEMSplitAttributes.Builder()
            .setAnimationBackground(extensionsColorBackground)
            .build()
        val extensionsSplitAttributesWithDefaultBackground = OEMSplitAttributes.Builder()
            .setAnimationBackground(OEMEmbeddingAnimationBackground.ANIMATION_BACKGROUND_DEFAULT)
            .build()

        // Translate from Window to Extensions
        assertEquals(extensionsSplitAttributesWithColorBackground,
            adapter.translateSplitAttributes(splitAttributesWithColorBackground))
        assertEquals(extensionsSplitAttributesWithDefaultBackground,
            adapter.translateSplitAttributes(splitAttributesWithDefaultBackground))

        // Translate from Extensions to Window
        assertEquals(splitAttributesWithColorBackground,
            adapter.translate(extensionsSplitAttributesWithColorBackground))
        assertEquals(splitAttributesWithDefaultBackground,
            adapter.translate(extensionsSplitAttributesWithDefaultBackground))
    }

    @Test
    fun testTranslateAnimationBackgroundBeforeApiLevel5() {
        WindowTestUtils.assumeAtLeastVendorApiLevel(2)
        WindowTestUtils.assumeBeforeVendorApiLevel(5)

        val colorBackground = EmbeddingAnimationBackground.createColorBackground(Color.BLUE)
        val splitAttributesWithColorBackground = SplitAttributes.Builder()
            .setAnimationBackground(colorBackground)
            .build()
        val splitAttributesWithDefaultBackground = SplitAttributes.Builder()
            .setAnimationBackground(EmbeddingAnimationBackground.DEFAULT)
            .build()

        // No difference after translate before API level 5
        assertEquals(adapter.translateSplitAttributes(splitAttributesWithColorBackground),
            adapter.translateSplitAttributes(splitAttributesWithDefaultBackground))
    }

    @OptIn(androidx.window.core.ExperimentalWindowApi::class)
    @Test
    fun testTranslateEmbeddingConfigurationToWindowAttributes() {
        WindowTestUtils.assumeAtLeastVendorApiLevel(5)

        val dimArea = EmbeddingConfiguration.DimArea.ON_TASK
        adapter.embeddingConfiguration = EmbeddingConfiguration(dimArea)
        val oemSplitAttributes = adapter.translateSplitAttributes(SplitAttributes.Builder().build())

        assertEquals(dimArea.value, oemSplitAttributes.windowAttributes.dimAreaBehavior)
    }

    @Suppress("Deprecation") // Verify the behavior of version 3 and 4.
    private fun createTestOEMSplitInfo(
        testPrimaryActivityStack: OEMActivityStack,
        testSecondaryActivityStack: OEMActivityStack,
        testSplitAttributes: OEMSplitAttributes,
        testBinder: IBinder? = null,
        testToken: OEMSplitInfoToken? = null,
    ): OEMSplitInfo {
        return mock<OEMSplitInfo>().apply {
            whenever(primaryActivityStack).thenReturn(testPrimaryActivityStack)
            whenever(secondaryActivityStack).thenReturn(testSecondaryActivityStack)
            when (extensionVersion) {
                2 -> whenever(splitAttributes).thenReturn(testSplitAttributes)
                in 3..4 -> whenever(token).thenReturn(testBinder)
                in 5..Int.MAX_VALUE -> whenever(splitInfoToken).thenReturn(testToken)
            }
        }
    }

    private fun createTestOEMActivityStack(
        testActivities: List<Activity>,
        testIsEmpty: Boolean,
        testToken: OEMActivityStackToken = OEMActivityStackToken.INVALID_ACTIVITY_STACK_TOKEN,
    ): OEMActivityStack {
        return mock<OEMActivityStack>().apply {
            whenever(activities).thenReturn(testActivities)
            whenever(isEmpty).thenReturn(testIsEmpty)
            if (ExtensionsUtil.safeVendorApiLevel >= 5) {
                whenever(token).thenReturn(testToken)
            }
        }
    }
}
