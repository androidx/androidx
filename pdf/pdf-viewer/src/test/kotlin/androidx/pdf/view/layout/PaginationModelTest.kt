/*
 * Copyright 2025 The Android Open Source Project
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
package androidx.pdf.view.layout

import android.graphics.Point
import android.os.Parcel
import com.google.common.truth.Truth.assertThat
import kotlin.math.max
import kotlin.random.Random
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class PaginationModelTest {
    private val NUM_PAGES = 250
    private lateinit var paginationModel: PaginationModel

    @Before
    fun setup() {
        paginationModel = PaginationModel(numPages = NUM_PAGES)
    }

    @Test
    fun invalidConstructorArguments() {
        assertThrows(IllegalArgumentException::class.java) { PaginationModel(numPages = -1) }
    }

    @Test
    fun propertyDefaults_withNoPagesAdded() {
        assertThat(paginationModel.reach).isEqualTo(-1)
    }

    @Test
    fun propertyValues_withSomePagesAdded() {
        var totalHeight = 0
        var maxWidth = 0
        val rng = Random(System.currentTimeMillis())
        val knownPages = NUM_PAGES / 2

        for (i in 0 until knownPages) {
            val pageSize = Point(rng.nextInt(50, 100), rng.nextInt(100, 200))
            maxWidth = max(maxWidth, pageSize.x)
            totalHeight += pageSize.y
            paginationModel.addPage(i, pageSize)
        }

        assertThat(paginationModel.reach).isEqualTo(knownPages - 1)
    }

    @Test
    fun propertyValues_withAllPagesAdded() {
        var totalHeight = 0
        var maxWidth = 0
        val rng = Random(System.currentTimeMillis())
        for (i in 0 until NUM_PAGES) {
            val pageSize = Point(rng.nextInt(50, 100), rng.nextInt(100, 200))
            maxWidth = max(maxWidth, pageSize.x)
            totalHeight += pageSize.y
            paginationModel.addPage(i, pageSize)
        }

        assertThat(paginationModel.reach).isEqualTo(NUM_PAGES - 1)
    }

    @Test
    fun rejectInvalidPage() {
        assertThrows(IllegalArgumentException::class.java) {
            paginationModel.addPage(0, Point(100, -1))
        }
        assertThrows(IllegalArgumentException::class.java) {
            paginationModel.addPage(0, Point(-1, 100))
        }
        assertThrows(IllegalArgumentException::class.java) {
            paginationModel.addPage(-1, Point(100, 200))
        }
        assertThrows(IllegalArgumentException::class.java) {
            paginationModel.addPage(NUM_PAGES + 10, Point(100, 200))
        }
    }

    @Test
    fun getPageSize() {
        val sizeRng = Random(System.currentTimeMillis())
        val sizes =
            List(size = 3) { _ -> Point(sizeRng.nextInt(100, 200), sizeRng.nextInt(100, 200)) }

        sizes.forEachIndexed { pageNum, size -> paginationModel.addPage(pageNum, size) }

        sizes.forEachIndexed { pageNum, size ->
            assertThat(paginationModel.getPageSize(pageNum)).isEqualTo(size)
        }
    }

    @Test
    fun getPageSize_invalidPageNum() {
        assertThrows(IllegalArgumentException::class.java) { paginationModel.getPageSize(-1) }
        assertThrows(IllegalArgumentException::class.java) {
            paginationModel.getPageSize(NUM_PAGES + 10)
        }
    }

    @Test
    fun parcelable() {
        val sizeRng = Random(System.currentTimeMillis())
        val sizes =
            List(size = 3) { _ -> Point(sizeRng.nextInt(100, 200), sizeRng.nextInt(100, 200)) }
        sizes.forEachIndexed { pageNum, size -> paginationModel.addPage(pageNum, size) }

        val parcel = Parcel.obtain()
        paginationModel.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)
        val newPaginationModel = PaginationModel.CREATOR.createFromParcel(parcel)

        assertThat(newPaginationModel.reach).isEqualTo(paginationModel.reach)

        for (i in 0 until paginationModel.reach) {
            assertThat(newPaginationModel.getPageSize(i)).isEqualTo(paginationModel.getPageSize(i))
        }
    }
}
