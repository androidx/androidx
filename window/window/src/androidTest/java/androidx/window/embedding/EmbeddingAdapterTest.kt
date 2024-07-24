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
import androidx.window.core.PredicateAdapter
import androidx.window.embedding.DividerAttributes.DragRange.SplitRatioDragRange
import androidx.window.embedding.DividerAttributes.DraggableDividerAttributes
import androidx.window.embedding.DividerAttributes.FixedDividerAttributes
import androidx.window.embedding.SplitAttributes.SplitType
import androidx.window.embedding.SplitAttributes.SplitType.Companion.SPLIT_TYPE_HINGE
import androidx.window.extensions.embedding.ActivityStack as OEMActivityStack
import androidx.window.extensions.embedding.ActivityStack.Token as OEMActivityStackToken
import androidx.window.extensions.embedding.AnimationBackground as OEMEmbeddingAnimationBackground
import androidx.window.extensions.embedding.DividerAttributes as OEMDividerAttributes
import androidx.window.extensions.embedding.SplitAttributes as OEMSplitAttributes
import androidx.window.extensions.embedding.SplitAttributes.LayoutDirection.TOP_TO_BOTTOM
import androidx.window.extensions.embedding.SplitAttributes.SplitType.RatioSplitType
import androidx.window.extensions.embedding.SplitInfo as OEMSplitInfo
import androidx.window.extensions.embedding.SplitInfo.Token as OEMSplitInfoToken
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
        adapter =
            EmbeddingBackend::class.java.classLoader?.let { loader ->
                EmbeddingAdapter(PredicateAdapter(loader))
            }!!
    }

    @Test
    fun testTranslateSplitInfoWithDefaultAttrsWithApiLevel2() {
        WindowTestUtils.assumeAtLeastVendorApiLevel(2)
        WindowTestUtils.assumeBeforeVendorApiLevel(3)

        val oemSplitInfo = createTestOEMSplitInfo(OEMSplitAttributes.Builder().build())
        val expectedSplitInfo =
            SplitInfo(
                ActivityStack(emptyList(), isEmpty = true),
                ActivityStack(emptyList(), isEmpty = true),
                SplitAttributes.Builder()
                    .setSplitType(SplitType.SPLIT_TYPE_EQUAL)
                    .setLayoutDirection(SplitAttributes.LayoutDirection.LOCALE)
                    .build(),
            )
        assertEquals(listOf(expectedSplitInfo), adapter.translate(listOf(oemSplitInfo)))
    }

    @Suppress("DEPRECATION")
    @Test
    fun testTranslateSplitInfoWithDefaultAttrsWithApiLevel3() {
        WindowTestUtils.assumeAtLeastVendorApiLevel(3)
        WindowTestUtils.assumeBeforeVendorApiLevel(5)

        val oemSplitInfo =
            createTestOEMSplitInfo(
                OEMSplitAttributes.Builder().build(),
                testBinder = Binder(),
            )
        val expectedSplitInfo =
            SplitInfo(
                ActivityStack(emptyList(), isEmpty = true),
                ActivityStack(emptyList(), isEmpty = true),
                SplitAttributes.Builder()
                    .setSplitType(SplitType.SPLIT_TYPE_EQUAL)
                    .setLayoutDirection(SplitAttributes.LayoutDirection.LOCALE)
                    .build(),
                oemSplitInfo.token,
            )
        assertEquals(listOf(expectedSplitInfo), adapter.translate(listOf(oemSplitInfo)))
    }

    @Test
    fun testTranslateSplitInfoWithDefaultAttrsWithApiLevel5() {
        WindowTestUtils.assumeAtLeastVendorApiLevel(5)

        val oemSplitInfo =
            createTestOEMSplitInfo(
                createTestOEMActivityStack(OEMActivityStackToken.createFromBinder(Binder())),
                createTestOEMActivityStack(OEMActivityStackToken.createFromBinder(Binder())),
                OEMSplitAttributes.Builder().build(),
                testToken = OEMSplitInfoToken.createFromBinder(Binder()),
            )
        val expectedSplitInfo =
            SplitInfo(
                ActivityStack(
                    ArrayList(),
                    isEmpty = true,
                    oemSplitInfo.primaryActivityStack.activityStackToken,
                ),
                ActivityStack(
                    ArrayList(),
                    isEmpty = true,
                    oemSplitInfo.secondaryActivityStack.activityStackToken,
                ),
                SplitAttributes.Builder()
                    .setSplitType(SplitType.SPLIT_TYPE_EQUAL)
                    .setLayoutDirection(SplitAttributes.LayoutDirection.LOCALE)
                    .build(),
                oemSplitInfo.splitInfoToken,
            )
        assertEquals(listOf(expectedSplitInfo), adapter.translate(listOf(oemSplitInfo)))
    }

    @Test
    fun testTranslateSplitInfoWithExpandingContainersWithApiLevel2() {
        WindowTestUtils.assumeAtLeastVendorApiLevel(2)
        WindowTestUtils.assumeBeforeVendorApiLevel(3)

        val oemSplitInfo =
            createTestOEMSplitInfo(
                OEMSplitAttributes.Builder()
                    .setSplitType(OEMSplitAttributes.SplitType.ExpandContainersSplitType())
                    .build()
            )
        val expectedSplitInfo =
            SplitInfo(
                ActivityStack(emptyList(), isEmpty = true),
                ActivityStack(emptyList(), isEmpty = true),
                SplitAttributes.Builder()
                    .setSplitType(SplitType.SPLIT_TYPE_EXPAND)
                    .setLayoutDirection(SplitAttributes.LayoutDirection.LOCALE)
                    .build(),
            )
        assertEquals(listOf(expectedSplitInfo), adapter.translate(listOf(oemSplitInfo)))
    }

    @Suppress("DEPRECATION")
    @Test
    fun testTranslateSplitInfoWithExpandingContainersWithApiLevel3() {
        WindowTestUtils.assumeAtLeastVendorApiLevel(3)
        WindowTestUtils.assumeBeforeVendorApiLevel(5)

        val oemSplitInfo =
            createTestOEMSplitInfo(
                OEMSplitAttributes.Builder()
                    .setSplitType(OEMSplitAttributes.SplitType.ExpandContainersSplitType())
                    .build(),
                testBinder = Binder(),
            )
        val expectedSplitInfo =
            SplitInfo(
                ActivityStack(ArrayList(), isEmpty = true),
                ActivityStack(emptyList(), isEmpty = true),
                SplitAttributes.Builder()
                    .setSplitType(SplitType.SPLIT_TYPE_EXPAND)
                    .setLayoutDirection(SplitAttributes.LayoutDirection.LOCALE)
                    .build(),
                oemSplitInfo.token,
            )
        assertEquals(listOf(expectedSplitInfo), adapter.translate(listOf(oemSplitInfo)))
    }

    @Test
    fun testTranslateSplitInfoWithExpandingContainersWithApiLevel5() {
        WindowTestUtils.assumeAtLeastVendorApiLevel(5)

        val oemSplitInfo =
            createTestOEMSplitInfo(
                createTestOEMActivityStack(OEMActivityStackToken.createFromBinder(Binder())),
                createTestOEMActivityStack(OEMActivityStackToken.createFromBinder(Binder())),
                OEMSplitAttributes.Builder()
                    .setSplitType(OEMSplitAttributes.SplitType.ExpandContainersSplitType())
                    .build(),
                testToken = OEMSplitInfoToken.createFromBinder(Binder()),
            )
        val expectedSplitInfo =
            SplitInfo(
                ActivityStack(
                    ArrayList(),
                    isEmpty = true,
                    oemSplitInfo.primaryActivityStack.activityStackToken
                ),
                ActivityStack(
                    ArrayList(),
                    isEmpty = true,
                    oemSplitInfo.secondaryActivityStack.activityStackToken
                ),
                SplitAttributes.Builder()
                    .setSplitType(SplitType.SPLIT_TYPE_EXPAND)
                    .setLayoutDirection(SplitAttributes.LayoutDirection.LOCALE)
                    .build(),
                oemSplitInfo.splitInfoToken,
            )
        assertEquals(listOf(expectedSplitInfo), adapter.translate(listOf(oemSplitInfo)))
    }

    @Suppress("DEPRECATION")
    @Test
    fun testTranslateSplitInfoWithApiLevel1() {
        WindowTestUtils.assumeBeforeVendorApiLevel(2)

        val activityStack = createTestOEMActivityStack(ArrayList(), true)
        val expectedSplitRatio = 0.3f
        val oemSplitInfo =
            mock<OEMSplitInfo>().apply {
                whenever(primaryActivityStack).thenReturn(activityStack)
                whenever(secondaryActivityStack).thenReturn(activityStack)
                whenever(splitRatio).thenReturn(expectedSplitRatio)
            }

        val expectedSplitInfo =
            SplitInfo(
                ActivityStack(emptyList(), isEmpty = true),
                ActivityStack(emptyList(), isEmpty = true),
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

        val oemSplitInfo =
            createTestOEMSplitInfo(
                OEMSplitAttributes.Builder()
                    .setSplitType(OEMSplitAttributes.SplitType.HingeSplitType(RatioSplitType(0.5f)))
                    .setLayoutDirection(TOP_TO_BOTTOM)
                    .build(),
            )
        val expectedSplitInfo =
            SplitInfo(
                ActivityStack(emptyList(), isEmpty = true),
                ActivityStack(emptyList(), isEmpty = true),
                SplitAttributes.Builder()
                    .setSplitType(SPLIT_TYPE_HINGE)
                    .setLayoutDirection(SplitAttributes.LayoutDirection.TOP_TO_BOTTOM)
                    .build(),
            )
        assertEquals(listOf(expectedSplitInfo), adapter.translate(listOf(oemSplitInfo)))
    }

    @Suppress("DEPRECATION")
    @Test
    fun testTranslateSplitInfoWithApiLevel3() {
        WindowTestUtils.assumeAtLeastVendorApiLevel(3)
        WindowTestUtils.assumeBeforeVendorApiLevel(5)

        val oemSplitInfo =
            createTestOEMSplitInfo(
                OEMSplitAttributes.Builder()
                    .setSplitType(OEMSplitAttributes.SplitType.HingeSplitType(RatioSplitType(0.5f)))
                    .setLayoutDirection(TOP_TO_BOTTOM)
                    .build(),
                testBinder = Binder()
            )
        val expectedSplitInfo =
            SplitInfo(
                ActivityStack(ArrayList(), isEmpty = true),
                ActivityStack(ArrayList(), isEmpty = true),
                SplitAttributes.Builder()
                    .setSplitType(SPLIT_TYPE_HINGE)
                    .setLayoutDirection(SplitAttributes.LayoutDirection.TOP_TO_BOTTOM)
                    .build(),
                oemSplitInfo.token,
            )
        assertEquals(listOf(expectedSplitInfo), adapter.translate(listOf(oemSplitInfo)))
    }

    @Test
    fun testTranslateSplitInfoWithApiLevel5() {
        WindowTestUtils.assumeAtLeastVendorApiLevel(5)

        val oemSplitInfo =
            createTestOEMSplitInfo(
                createTestOEMActivityStack(OEMActivityStackToken.createFromBinder(Binder())),
                createTestOEMActivityStack(OEMActivityStackToken.createFromBinder(Binder())),
                OEMSplitAttributes.Builder()
                    .setSplitType(OEMSplitAttributes.SplitType.HingeSplitType(RatioSplitType(0.5f)))
                    .setLayoutDirection(TOP_TO_BOTTOM)
                    .build(),
                testToken = OEMSplitInfoToken.createFromBinder(Binder())
            )
        val expectedSplitInfo =
            SplitInfo(
                ActivityStack(
                    emptyList(),
                    isEmpty = true,
                    oemSplitInfo.primaryActivityStack.activityStackToken,
                ),
                ActivityStack(
                    emptyList(),
                    isEmpty = true,
                    oemSplitInfo.secondaryActivityStack.activityStackToken,
                ),
                SplitAttributes.Builder()
                    .setSplitType(SPLIT_TYPE_HINGE)
                    .setLayoutDirection(SplitAttributes.LayoutDirection.TOP_TO_BOTTOM)
                    .build(),
                token = oemSplitInfo.splitInfoToken,
            )
        assertEquals(listOf(expectedSplitInfo), adapter.translate(listOf(oemSplitInfo)))
    }

    @Test
    fun testTranslateAnimationBackgroundWithApiLevel5() {
        WindowTestUtils.assumeAtLeastVendorApiLevel(5)

        val colorBackground = EmbeddingAnimationBackground.createColorBackground(Color.BLUE)
        val splitAttributesWithColorBackground =
            SplitAttributes.Builder().setAnimationBackground(colorBackground).build()
        val splitAttributesWithDefaultBackground =
            SplitAttributes.Builder()
                .setAnimationBackground(EmbeddingAnimationBackground.DEFAULT)
                .build()

        val extensionsColorBackground =
            OEMEmbeddingAnimationBackground.createColorBackground(Color.BLUE)
        val extensionsSplitAttributesWithColorBackground =
            OEMSplitAttributes.Builder().setAnimationBackground(extensionsColorBackground).build()
        val extensionsSplitAttributesWithDefaultBackground =
            OEMSplitAttributes.Builder()
                .setAnimationBackground(
                    OEMEmbeddingAnimationBackground.ANIMATION_BACKGROUND_DEFAULT
                )
                .build()

        // Translate from Window to Extensions
        assertEquals(
            extensionsSplitAttributesWithColorBackground,
            adapter.translateSplitAttributes(splitAttributesWithColorBackground)
        )
        assertEquals(
            extensionsSplitAttributesWithDefaultBackground,
            adapter.translateSplitAttributes(splitAttributesWithDefaultBackground)
        )

        // Translate from Extensions to Window
        assertEquals(
            splitAttributesWithColorBackground,
            adapter.translate(extensionsSplitAttributesWithColorBackground)
        )
        assertEquals(
            splitAttributesWithDefaultBackground,
            adapter.translate(extensionsSplitAttributesWithDefaultBackground)
        )
    }

    @Test
    fun testTranslateAnimationBackgroundBeforeApiLevel5() {
        WindowTestUtils.assumeAtLeastVendorApiLevel(2)
        WindowTestUtils.assumeBeforeVendorApiLevel(5)

        val colorBackground = EmbeddingAnimationBackground.createColorBackground(Color.BLUE)
        val splitAttributesWithColorBackground =
            SplitAttributes.Builder().setAnimationBackground(colorBackground).build()
        val splitAttributesWithDefaultBackground =
            SplitAttributes.Builder()
                .setAnimationBackground(EmbeddingAnimationBackground.DEFAULT)
                .build()

        // No difference after translate before API level 5
        assertEquals(
            adapter.translateSplitAttributes(splitAttributesWithColorBackground),
            adapter.translateSplitAttributes(splitAttributesWithDefaultBackground)
        )
    }

    @OptIn(androidx.window.core.ExperimentalWindowApi::class)
    @Test
    fun testTranslateEmbeddingConfigurationToWindowAttributes() {
        WindowTestUtils.assumeAtLeastVendorApiLevel(5)

        val dimAreaBehavior = EmbeddingConfiguration.DimAreaBehavior.ON_TASK
        adapter.embeddingConfiguration = EmbeddingConfiguration(dimAreaBehavior)
        val oemSplitAttributes = adapter.translateSplitAttributes(SplitAttributes.Builder().build())

        assertEquals(dimAreaBehavior.value, oemSplitAttributes.windowAttributes.dimAreaBehavior)
    }

    @Test
    fun testTranslateDividerAttributes_draggable() {
        WindowTestUtils.assumeAtLeastVendorApiLevel(6)
        val dividerAttributes =
            DraggableDividerAttributes.Builder()
                .setWidthDp(20)
                .setDragRange(SplitRatioDragRange(0.3f, 0.7f))
                .setColor(Color.GRAY)
                .build()
        val oemDividerAttributes =
            OEMDividerAttributes.Builder(OEMDividerAttributes.DIVIDER_TYPE_DRAGGABLE)
                .setWidthDp(20)
                .setPrimaryMinRatio(0.3f)
                .setPrimaryMaxRatio(0.7f)
                .setDividerColor(Color.GRAY)
                .build()

        assertEquals(oemDividerAttributes, adapter.translateDividerAttributes(dividerAttributes))
        assertEquals(dividerAttributes, adapter.translateDividerAttributes(oemDividerAttributes))
    }

    @Test
    fun testTranslateDividerAttributes_fixed() {
        WindowTestUtils.assumeAtLeastVendorApiLevel(6)
        val dividerAttributes =
            FixedDividerAttributes.Builder().setWidthDp(20).setColor(Color.GRAY).build()
        val oemDividerAttributes =
            OEMDividerAttributes.Builder(OEMDividerAttributes.DIVIDER_TYPE_FIXED)
                .setWidthDp(20)
                .setDividerColor(Color.GRAY)
                .build()

        assertEquals(oemDividerAttributes, adapter.translateDividerAttributes(dividerAttributes))
        assertEquals(dividerAttributes, adapter.translateDividerAttributes(oemDividerAttributes))
    }

    @Test
    fun testTranslateDividerAttributes_noDivider() {
        WindowTestUtils.assumeAtLeastVendorApiLevel(6)
        val dividerAttributes = DividerAttributes.NO_DIVIDER
        val oemDividerAttributes = null

        assertEquals(oemDividerAttributes, adapter.translateDividerAttributes(dividerAttributes))
        assertEquals(dividerAttributes, adapter.translateDividerAttributes(oemDividerAttributes))
    }

    private fun createTestOEMSplitInfo(
        testSplitAttributes: OEMSplitAttributes,
        testBinder: IBinder? = null,
        testToken: OEMSplitInfo.Token? = null,
    ): OEMSplitInfo =
        createTestOEMSplitInfo(
            createTestOEMActivityStack(),
            createTestOEMActivityStack(),
            testSplitAttributes,
            testBinder,
            testToken,
        )

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
            if (extensionVersion >= 2) {
                whenever(splitAttributes).thenReturn(testSplitAttributes)
            }
            when (extensionVersion) {
                in 3..4 -> whenever(token).thenReturn(testBinder)
                in 5..Int.MAX_VALUE -> whenever(splitInfoToken).thenReturn(testToken)
            }
        }
    }

    private fun createTestOEMActivityStack(
        testToken: OEMActivityStackToken? = null
    ): OEMActivityStack =
        createTestOEMActivityStack(
            emptyList(),
            testIsEmpty = true,
            testToken,
        )

    private fun createTestOEMActivityStack(
        testActivities: List<Activity>,
        testIsEmpty: Boolean,
        testToken: OEMActivityStackToken? = null,
    ): OEMActivityStack {
        return mock<OEMActivityStack>().apply {
            whenever(activities).thenReturn(testActivities)
            whenever(isEmpty).thenReturn(testIsEmpty)
            if (extensionVersion >= 5) {
                whenever(activityStackToken).thenReturn(testToken)
            }
        }
    }
}
