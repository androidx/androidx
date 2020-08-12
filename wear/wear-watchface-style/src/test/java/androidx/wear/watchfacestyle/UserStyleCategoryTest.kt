/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.wear.watchfacestyle

import android.graphics.drawable.Icon
import android.os.Bundle
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.model.FrameworkMethod
import org.robolectric.RobolectricTestRunner
import org.robolectric.internal.bytecode.InstrumentationConfiguration

// Without this we get test failures with an error:
// "failed to access class kotlin.jvm.internal.DefaultConstructorMarker".
class WatchFaceServiceTestRunner(testClass: Class<*>) : RobolectricTestRunner(testClass) {
    override fun createClassLoaderConfig(method: FrameworkMethod): InstrumentationConfiguration =
        InstrumentationConfiguration.Builder(super.createClassLoaderConfig(method))
            .doNotInstrumentPackage("androidx.wear.watchfacestyle")
            .build()
}

@RunWith(WatchFaceServiceTestRunner::class)
class UserStyleCategoryTest {

    private val icon1 = Icon.createWithContentUri("icon1")
    private val icon2 = Icon.createWithContentUri("icon2")
    private val icon3 = Icon.createWithContentUri("icon3")
    private val icon4 = Icon.createWithContentUri("icon4")
    private val option1 = ListViewUserStyleCategory.ListViewOption("1", "one", icon1)
    private val option2 = ListViewUserStyleCategory.ListViewOption("2", "two", icon2)
    private val option3 = ListViewUserStyleCategory.ListViewOption("3", "three", icon3)
    private val option4 = ListViewUserStyleCategory.ListViewOption("4", "four", icon4)

    @Test
    fun bundleAndUnbundleStyleCategoryAndOption() {
        val categoryIcon = Icon.createWithContentUri("categoryIcon")
        val styleCategory = ListViewUserStyleCategory(
            "id", "displayName", "description", categoryIcon, listOf(option1, option2, option3)
        )

        val bundle = Bundle()
        styleCategory.writeToBundle(bundle)

        val unbundled = UserStyleCategory.createFromBundle(bundle)
        assert(unbundled is ListViewUserStyleCategory)

        assertThat(unbundled.id).isEqualTo("id")
        assertThat(unbundled.displayName).isEqualTo("displayName")
        assertThat(unbundled.description).isEqualTo("description")
        assertThat(unbundled.icon!!.uri.toString()).isEqualTo("categoryIcon")
        val optionArray =
            unbundled.options.filterIsInstance<ListViewUserStyleCategory.ListViewOption>()
                .toTypedArray()
        assertThat(optionArray.size).isEqualTo(3)
        assertThat(optionArray[0].id).isEqualTo("1")
        assertThat(optionArray[0].displayName).isEqualTo("one")
        assertThat(optionArray[0].icon!!.uri.toString()).isEqualTo("icon1")
        assertThat(optionArray[1].id).isEqualTo("2")
        assertThat(optionArray[1].displayName).isEqualTo("two")
        assertThat(optionArray[1].icon!!.uri.toString()).isEqualTo("icon2")
        assertThat(optionArray[2].id).isEqualTo("3")
        assertThat(optionArray[2].displayName).isEqualTo("three")
        assertThat(optionArray[2].icon!!.uri.toString()).isEqualTo("icon3")
    }

    @Test
    fun bundleAndUnbundleOptionList() {
        val bundle = Bundle()
        UserStyleCategory.writeOptionListToBundle(listOf(option1, option2, option3), bundle)

        val unbundled = UserStyleCategory.readOptionsListFromBundle(bundle)
        val optionArray = unbundled.filterIsInstance<ListViewUserStyleCategory.ListViewOption>()
                .toTypedArray()

        assertThat(optionArray.size).isEqualTo(3)
        assertThat(optionArray[0].id).isEqualTo("1")
        assertThat(optionArray[0].displayName).isEqualTo("one")
        assertThat(optionArray[0].icon!!.uri.toString()).isEqualTo("icon1")
        assertThat(optionArray[1].id).isEqualTo("2")
        assertThat(optionArray[1].displayName).isEqualTo("two")
        assertThat(optionArray[1].icon!!.uri.toString()).isEqualTo("icon2")
        assertThat(optionArray[2].id).isEqualTo("3")
        assertThat(optionArray[2].displayName).isEqualTo("three")
        assertThat(optionArray[2].icon!!.uri.toString()).isEqualTo("icon3")
    }

    @Test
    fun bundleAndUnbundleStyleCategoryList() {
        val categoryIcon1 = Icon.createWithContentUri("categoryIcon1")
        val categoryIcon2 = Icon.createWithContentUri("categoryIcon2")
        val styleCategory1 = ListViewUserStyleCategory(
            "id1", "displayName1", "description1", categoryIcon1, listOf(option1, option2)
        )
        val styleCategory2 = ListViewUserStyleCategory(
            "id2", "displayName2", "description2", categoryIcon2, listOf(option3, option4)
        )

        val bundles = UserStyleCategory.userStyleCategoryListToBundles(
            listOf(styleCategory1, styleCategory2)
        )

        val unbundled = UserStyleCategory.bundlesToUserStyleCategoryLists(bundles)

        assert(unbundled[0] is ListViewUserStyleCategory)
        assertThat(unbundled[0].id).isEqualTo("id1")
        assertThat(unbundled[0].displayName).isEqualTo("displayName1")
        assertThat(unbundled[0].description).isEqualTo("description1")
        assertThat(unbundled[0].icon!!.uri.toString()).isEqualTo("categoryIcon1")
        val optionArray1 =
            unbundled[0].options.filterIsInstance<ListViewUserStyleCategory.ListViewOption>()
                .toTypedArray()
        assertThat(optionArray1.size).isEqualTo(2)
        assertThat(optionArray1[0].id).isEqualTo("1")
        assertThat(optionArray1[0].displayName).isEqualTo("one")
        assertThat(optionArray1[0].icon!!.uri.toString()).isEqualTo("icon1")
        assertThat(optionArray1[1].id).isEqualTo("2")
        assertThat(optionArray1[1].displayName).isEqualTo("two")
        assertThat(optionArray1[1].icon!!.uri.toString()).isEqualTo("icon2")

        assert(unbundled[1] is ListViewUserStyleCategory)
        assertThat(unbundled[1].id).isEqualTo("id2")
        assertThat(unbundled[1].displayName).isEqualTo("displayName2")
        assertThat(unbundled[1].description).isEqualTo("description2")
        assertThat(unbundled[1].icon!!.uri.toString()).isEqualTo("categoryIcon2")
        val optionArray2 =
            unbundled[1].options.filterIsInstance<ListViewUserStyleCategory.ListViewOption>()
                .toTypedArray()
        assertThat(optionArray2.size).isEqualTo(2)
        assertThat(optionArray2[0].id).isEqualTo("3")
        assertThat(optionArray2[0].displayName).isEqualTo("three")
        assertThat(optionArray2[0].icon!!.uri.toString()).isEqualTo("icon3")
        assertThat(optionArray2[1].id).isEqualTo("4")
        assertThat(optionArray2[1].displayName).isEqualTo("four")
        assertThat(optionArray2[1].icon!!.uri.toString()).isEqualTo("icon4")
    }

    @Test
    fun bundleAndUnbundleStyleMap() {
        val categoryIcon1 = Icon.createWithContentUri("categoryIcon1")
        val categoryIcon2 = Icon.createWithContentUri("categoryIcon2")
        val styleCategory1 = ListViewUserStyleCategory(
            "id1", "displayName1", "description1", categoryIcon1, listOf(option1, option2)
        )
        val styleCategory2 = ListViewUserStyleCategory(
            "id2", "displayName2", "description2", categoryIcon2, listOf(option3, option4)
        )
        val schema = listOf(styleCategory1, styleCategory2)
        val styleMap = mapOf(
            styleCategory1 as UserStyleCategory to option2 as UserStyleCategory.Option,
            styleCategory2 as UserStyleCategory to option3 as UserStyleCategory.Option
        )
        val bundle = UserStyleCategory.styleMapToBundle(styleMap)

        val unbundled = UserStyleCategory.bundleToStyleMap(bundle, schema)
        assertThat(unbundled.size).isEqualTo(2)
        assertThat(unbundled[styleCategory1]!!.id).isEqualTo(option2.id)
        assertThat(unbundled[styleCategory2]!!.id).isEqualTo(option3.id)
    }
}