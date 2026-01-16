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

package androidx.pdf.ink

import android.graphics.RectF
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class PageInfoProviderImplTest {

    private lateinit var pageInfoProvider: PageInfoProviderImpl

    @Before
    fun setUp() {
        pageInfoProvider = PageInfoProviderImpl()
    }

    @Test
    fun getPageInfoFromViewCoordinates_pointInsidePage_returnsCorrectPageInfo() {
        val pageNum = 0
        val pageBounds = RectF(100f, 100f, 300f, 500f)
        pageInfoProvider.pageLocations.put(pageNum, pageBounds)
        pageInfoProvider.zoom = 2f

        val result = pageInfoProvider.getPageInfoFromViewCoordinates(150f, 250f)

        assertThat(result).isNotNull()
        assertThat(result?.pageNum).isEqualTo(pageNum)
        assertThat(result?.pageBounds).isEqualTo(pageBounds)

        // Verify pageToViewTransform
        val pointOnPage = floatArrayOf(0f, 0f)
        result?.pageToViewTransform?.mapPoints(pointOnPage)
        assertThat(pointOnPage[0]).isEqualTo(100f)
        assertThat(pointOnPage[1]).isEqualTo(100f)

        // Verify viewToPageTransform
        val pointInView = floatArrayOf(150f, 250f)
        result?.viewToPageTransform?.mapPoints(pointInView)
        assertThat(pointInView[0]).isEqualTo(25f)
        assertThat(pointInView[1]).isEqualTo(75f)
    }

    @Test
    fun getPageInfoFromViewCoordinates_pointOutsidePage_returnsNull() {
        val pageBounds = RectF(100f, 100f, 300f, 500f)
        pageInfoProvider.pageLocations.put(0, pageBounds)
        pageInfoProvider.zoom = 1f

        val result = pageInfoProvider.getPageInfoFromViewCoordinates(50f, 250f)

        assertThat(result).isNull()
    }

    @Test
    fun getPageInfoFromViewCoordinates_pointOnPageEdge_returnsPageInfo() {
        val pageBounds = RectF(100f, 100f, 300f, 500f)
        pageInfoProvider.pageLocations.put(0, pageBounds)
        pageInfoProvider.zoom = 1f

        val result = pageInfoProvider.getPageInfoFromViewCoordinates(100f, 100f)

        assertThat(result).isNotNull()
        assertThat(result?.pageNum).isEqualTo(0)
    }

    @Test
    fun getPageInfoFromViewCoordinates_multiplePages_returnsCorrectPage() {
        val pageBounds1 = RectF(0f, 0f, 200f, 400f)
        val pageBounds2 = RectF(0f, 400f, 200f, 800f)
        pageInfoProvider.pageLocations.apply {
            put(0, pageBounds1)
            put(1, pageBounds2)
        }
        pageInfoProvider.zoom = 1f

        val result = pageInfoProvider.getPageInfoFromViewCoordinates(100f, 500f)

        assertThat(result).isNotNull()
        assertThat(result?.pageNum).isEqualTo(1)
        assertThat(result?.pageBounds).isEqualTo(pageBounds2)
    }

    @Test
    fun getPageInfoFromViewCoordinates_noPages_returnsNull() {
        val result = pageInfoProvider.getPageInfoFromViewCoordinates(100f, 100f)
        assertThat(result).isNull()
    }
}
