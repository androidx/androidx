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

import androidx.window.extensions.embedding.ActivityStack as OEMActivityStack
import androidx.window.extensions.embedding.SplitAttributes as OEMSplitAttributes
import androidx.window.extensions.embedding.SplitInfo as OEMSplitInfo
import android.app.Activity
import android.graphics.Color
import android.os.Binder
import android.os.IBinder
import androidx.window.WindowTestUtils
import androidx.window.core.ExtensionsUtil
import androidx.window.core.PredicateAdapter
import androidx.window.embedding.EmbeddingAdapter.Companion.INVALID_ACTIVITY_STACK_TOKEN
import androidx.window.embedding.EmbeddingAdapter.Companion.INVALID_SPLIT_INFO_TOKEN
import androidx.window.embedding.SplitAttributes.SplitType
import androidx.window.extensions.WindowExtensions
import com.nhaarman.mockitokotlin2.doReturn
import kotlin.test.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

/** Tests for [EmbeddingAdapter] */
class EmbeddingAdapterTest {
    private lateinit var adapter: EmbeddingAdapter

    @Before
    fun setUp() {
        adapter = EmbeddingBackend::class.java.classLoader?.let { loader ->
            EmbeddingAdapter(PredicateAdapter(loader))
        }!!
    }

    @Test
    fun testTranslateSplitInfoWithDefaultAttrs() {
        WindowTestUtils.assumeAtLeastVendorApiLevel(WindowExtensions.VENDOR_API_LEVEL_2)

        val oemSplitInfo = createTestOEMSplitInfo(
            createTestOEMActivityStack(ArrayList(), true),
            createTestOEMActivityStack(ArrayList(), true),
            OEMSplitAttributes.Builder().build(),
        )
        val expectedSplitInfo = SplitInfo(
            ActivityStack(ArrayList(), isEmpty = true, INVALID_ACTIVITY_STACK_TOKEN),
            ActivityStack(ArrayList(), isEmpty = true, INVALID_ACTIVITY_STACK_TOKEN),
            SplitAttributes.Builder()
                .setSplitType(SplitType.splitEqually())
                .setLayoutDirection(SplitAttributes.LayoutDirection.LOCALE)
                .setAnimationBackgroundColor(0)
                .build(),
            INVALID_SPLIT_INFO_TOKEN,
        )
        assertEquals(listOf(expectedSplitInfo), adapter.translate(listOf(oemSplitInfo)))
    }

    @Test
    fun testTranslateSplitInfoWithExpandingContainers() {
        WindowTestUtils.assumeAtLeastVendorApiLevel(WindowExtensions.VENDOR_API_LEVEL_2)

        val oemSplitInfo = createTestOEMSplitInfo(
            createTestOEMActivityStack(ArrayList(), true),
            createTestOEMActivityStack(ArrayList(), true),
            OEMSplitAttributes.Builder()
                .setSplitType(OEMSplitAttributes.SplitType.ExpandContainersSplitType())
                .build(),
        )
        val expectedSplitInfo = SplitInfo(
            ActivityStack(ArrayList(), isEmpty = true, INVALID_ACTIVITY_STACK_TOKEN),
            ActivityStack(ArrayList(), isEmpty = true, INVALID_ACTIVITY_STACK_TOKEN),
            SplitAttributes.Builder()
                .setSplitType(SplitType.expandContainers())
                .setLayoutDirection(SplitAttributes.LayoutDirection.LOCALE)
                .build(),
            INVALID_SPLIT_INFO_TOKEN,
        )
        assertEquals(listOf(expectedSplitInfo), adapter.translate(listOf(oemSplitInfo)))
    }

    @Suppress("DEPRECATION")
    @Test
    fun testTranslateSplitInfoWithApiLevel1() {
        WindowTestUtils.assumeBeforeVendorApiLevel(WindowExtensions.VENDOR_API_LEVEL_2)

        val activityStack = createTestOEMActivityStack(ArrayList(), true)
        val expectedSplitRatio = 0.3f
        val oemSplitInfo = mock(OEMSplitInfo::class.java)
        doReturn(activityStack).`when`(oemSplitInfo).primaryActivityStack
        doReturn(activityStack).`when`(oemSplitInfo).secondaryActivityStack
        doReturn(expectedSplitRatio).`when`(oemSplitInfo).splitRatio

        val expectedSplitInfo = SplitInfo(
            ActivityStack(ArrayList(), isEmpty = true, INVALID_ACTIVITY_STACK_TOKEN),
            ActivityStack(ArrayList(), isEmpty = true, INVALID_ACTIVITY_STACK_TOKEN),
            SplitAttributes.Builder()
                .setSplitType(SplitType.ratio(expectedSplitRatio))
                // OEMSplitInfo with Vendor API level 1 doesn't provide layoutDirection.
                .setLayoutDirection(SplitAttributes.LayoutDirection.LOCALE)
                .build(),
            INVALID_SPLIT_INFO_TOKEN,
        )
        assertEquals(listOf(expectedSplitInfo), adapter.translate(listOf(oemSplitInfo)))
    }

    @Test
    fun testTranslateSplitInfoWithApiLevel2() {
        WindowTestUtils.assumeAtLeastVendorApiLevel(WindowExtensions.VENDOR_API_LEVEL_2)

        val oemSplitInfo = createTestOEMSplitInfo(
            createTestOEMActivityStack(ArrayList(), true),
            createTestOEMActivityStack(ArrayList(), true),
            OEMSplitAttributes.Builder()
                .setSplitType(
                    OEMSplitAttributes.SplitType.HingeSplitType(
                        OEMSplitAttributes.SplitType.RatioSplitType(0.3f)
                    )
                ).setLayoutDirection(OEMSplitAttributes.LayoutDirection.TOP_TO_BOTTOM)
                .setAnimationBackgroundColor(Color.YELLOW)
                .build(),
        )
        val expectedSplitInfo = SplitInfo(
            ActivityStack(ArrayList(), isEmpty = true, INVALID_ACTIVITY_STACK_TOKEN),
            ActivityStack(ArrayList(), isEmpty = true, INVALID_ACTIVITY_STACK_TOKEN),
            SplitAttributes.Builder()
                .setSplitType(SplitType.splitByHinge(SplitType.ratio(0.3f)))
                .setLayoutDirection(SplitAttributes.LayoutDirection.TOP_TO_BOTTOM)
                .setAnimationBackgroundColor(Color.YELLOW)
                .build(),
            INVALID_SPLIT_INFO_TOKEN,
        )
        assertEquals(listOf(expectedSplitInfo), adapter.translate(listOf(oemSplitInfo)))
    }

    @Test
    fun testTranslateSplitInfoWithApiLevel3() {
        WindowTestUtils.assumeAtLeastVendorApiLevel(WindowExtensions.VENDOR_API_LEVEL_3)
        val testStackToken = Binder()
        val testSplitInfoToken = Binder()
        val oemSplitInfo = createTestOEMSplitInfo(
            createTestOEMActivityStack(ArrayList(), true, testStackToken),
            createTestOEMActivityStack(ArrayList(), true, testStackToken),
            OEMSplitAttributes.Builder()
                .setSplitType(
                    OEMSplitAttributes.SplitType.HingeSplitType(
                        OEMSplitAttributes.SplitType.RatioSplitType(0.3f)
                    )
                ).setLayoutDirection(OEMSplitAttributes.LayoutDirection.TOP_TO_BOTTOM)
                .setAnimationBackgroundColor(Color.YELLOW)
                .build(),
            testSplitInfoToken,
        )
        val expectedSplitInfo = SplitInfo(
            ActivityStack(ArrayList(), isEmpty = true, testStackToken),
            ActivityStack(ArrayList(), isEmpty = true, testStackToken),
            SplitAttributes.Builder()
                .setSplitType(SplitType.splitByHinge(SplitType.ratio(0.3f)))
                .setLayoutDirection(SplitAttributes.LayoutDirection.TOP_TO_BOTTOM)
                .setAnimationBackgroundColor(Color.YELLOW)
                .build(),
            testSplitInfoToken,
        )
        assertEquals(listOf(expectedSplitInfo), adapter.translate(listOf(oemSplitInfo)))
    }

    private fun createTestOEMSplitInfo(
        primaryActivityStack: OEMActivityStack,
        secondaryActivityStack: OEMActivityStack,
        splitAttributes: OEMSplitAttributes,
        token: IBinder = INVALID_SPLIT_INFO_TOKEN,
    ): OEMSplitInfo {
        val oemSplitInfo = mock(OEMSplitInfo::class.java)
        doReturn(primaryActivityStack).`when`(oemSplitInfo).primaryActivityStack
        doReturn(secondaryActivityStack).`when`(oemSplitInfo).secondaryActivityStack
        if (ExtensionsUtil.safeVendorApiLevel >= WindowExtensions.VENDOR_API_LEVEL_2) {
            doReturn(splitAttributes).`when`(oemSplitInfo).splitAttributes
        }
        if (ExtensionsUtil.safeVendorApiLevel >= WindowExtensions.VENDOR_API_LEVEL_3) {
            doReturn(token).`when`(oemSplitInfo).token
        }
        return oemSplitInfo
    }

    private fun createTestOEMActivityStack(
        activities: List<Activity>,
        isEmpty: Boolean,
        token: IBinder = INVALID_ACTIVITY_STACK_TOKEN,
    ): OEMActivityStack {
        val activityStack = mock(OEMActivityStack::class.java)
        doReturn(activities).`when`(activityStack).activities
        doReturn(isEmpty).`when`(activityStack).isEmpty
        if (ExtensionsUtil.safeVendorApiLevel >= WindowExtensions.VENDOR_API_LEVEL_3) {
            doReturn(token).`when`(activityStack).token
        }
        return activityStack
    }
}