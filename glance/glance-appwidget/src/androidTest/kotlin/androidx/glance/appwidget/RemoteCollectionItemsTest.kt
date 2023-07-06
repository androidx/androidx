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

package androidx.glance.appwidget

import android.content.Context
import android.widget.RemoteViews
import androidx.glance.appwidget.test.R
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Test

@SdkSuppress(maxSdkVersion = 29)
@MediumTest
class RemoteCollectionItemsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val packageName = context.packageName

    @Test
    fun testBuilder_empty() {
        val items = RemoteCollectionItems.Builder().build()

        assertThat(items.itemCount).isEqualTo(0)
        assertThat(items.viewTypeCount).isEqualTo(1)
        assertThat(items.hasStableIds()).isFalse()
    }

    @Test
    fun testBuilder_viewTypeCountUnspecified() {
        val firstItem = RemoteViews(packageName, R.layout.list_view_row)
        val secondItem = RemoteViews(packageName, R.layout.list_view_row_2)
        val items = RemoteCollectionItems.Builder()
            .setHasStableIds(true)
            .addItem(id = 3, firstItem)
            .addItem(id = 5, secondItem)
            .build()

        assertThat(items.itemCount).isEqualTo(2)
        assertThat(items.getItemId(0)).isEqualTo(3)
        assertThat(items.getItemId(1)).isEqualTo(5)
        assertThat(items.getItemView(0).layoutId).isEqualTo(R.layout.list_view_row)
        assertThat(items.getItemView(1).layoutId).isEqualTo(R.layout.list_view_row_2)
        assertThat(items.hasStableIds()).isTrue()
        // The view type count should be derived from the number of different layout ids if
        // unspecified.
        assertThat(items.viewTypeCount).isEqualTo(2)
    }

    @Test
    fun testBuilder_viewTypeCountSpecified() {
        val firstItem = RemoteViews(packageName, R.layout.list_view_row)
        val secondItem = RemoteViews(packageName, R.layout.list_view_row_2)
        val items = RemoteCollectionItems.Builder()
            .addItem(id = 3, firstItem)
            .addItem(id = 5, secondItem)
            .setViewTypeCount(15)
            .build()

        assertThat(items.viewTypeCount).isEqualTo(15)
    }

    @Test
    fun testBuilder_repeatedIdsAndLayouts() {
        val firstItem = RemoteViews(packageName, R.layout.list_view_row)
        val secondItem = RemoteViews(packageName, R.layout.list_view_row)
        val thirdItem = RemoteViews(packageName, R.layout.list_view_row)
        val items = RemoteCollectionItems.Builder()
            .setHasStableIds(false)
            .addItem(id = 42, firstItem)
            .addItem(id = 42, secondItem)
            .addItem(id = 42, thirdItem)
            .build()

        assertThat(items.itemCount).isEqualTo(3)
        assertThat(items.getItemId(0)).isEqualTo(42)
        assertThat(items.getItemId(1)).isEqualTo(42)
        assertThat(items.getItemId(2)).isEqualTo(42)
        assertThat(items.getItemView(0)).isSameInstanceAs(firstItem)
        assertThat(items.getItemView(1)).isSameInstanceAs(secondItem)
        assertThat(items.getItemView(2)).isSameInstanceAs(thirdItem)
        assertThat(items.hasStableIds()).isFalse()
        assertThat(items.viewTypeCount).isEqualTo(1)
    }

    @Test
    fun testBuilder_viewTypeCountLowerThanLayoutCount() {
        assertFailsWith(IllegalArgumentException::class) {
            RemoteCollectionItems.Builder()
                .setHasStableIds(true)
                .setViewTypeCount(1)
                .addItem(3, RemoteViews(packageName, R.layout.list_view_row))
                .addItem(5, RemoteViews(packageName, R.layout.list_view_row_2))
                .build()
        }
    }
}
