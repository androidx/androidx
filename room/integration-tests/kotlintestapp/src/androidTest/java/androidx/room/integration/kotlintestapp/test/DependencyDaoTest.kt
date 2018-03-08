/*
 * Copyright (C) 2018 The Android Open Source Project
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
package androidx.room.integration.kotlintestapp.test

import android.os.Build
import android.support.test.filters.SdkSuppress
import android.support.test.runner.AndroidJUnit4
import androidx.room.integration.kotlintestapp.dao.DependencyDao
import androidx.room.integration.kotlintestapp.vo.DataClassFromDependency
import androidx.room.integration.kotlintestapp.vo.EmbeddedFromDependency
import androidx.room.integration.kotlintestapp.vo.PojoFromDependency
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DependencyDaoTest : TestDatabaseTest() {
    lateinit var dao: DependencyDao
    @Before
    fun init() {
        dao = database.dependencyDao()
    }

    @Test
    fun insertAndGet() {
        val data = insertSample(3)
        assertThat(dao.selectAll(), `is`(listOf(data)))
    }

    @Test
    fun insertAndGetByQuery() {
        val data = insertSample(3)
        assertThat(dao.findById(3), `is`(data))
        assertThat(dao.findById(5), `is`(nullValue()))
    }

    @Test
    fun insertAndGetByQuery_embedded() {
        val data = insertSample(3)
        assertThat(dao.findEmbedded(3), `is`(EmbeddedFromDependency(data)))
        assertThat(dao.findEmbedded(5), `is`(nullValue()))
    }

    @Test
    fun insertAndGetByQuery_pojo() {
        val data = insertSample(3)
        assertThat(dao.findPojo(3), `is`(PojoFromDependency(
                id = data.id,
                name = data.name)))
        assertThat(dao.findPojo(5), `is`(nullValue()))
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    @Test
    fun getRelation() {
        val foo1 = DataClassFromDependency(
                id = 3,
                name = "foo"
        )
        val foo2 = DataClassFromDependency(
                id = 4,
                name = "foo"
        )
        val bar = DataClassFromDependency(
                id = 5,
                name = "bar"
        )
        dao.insert(foo1, foo2, bar)
        val fooList = dao.relation("foo")
        assertThat(fooList.sharedName, `is`("foo"))
        assertThat(fooList, `is`(notNullValue()))
        assertThat(fooList.dataItems, `is`(listOf(foo1, foo2)))

        val barList = dao.relation("bar")
        assertThat(barList.sharedName, `is`("bar"))
        assertThat(barList, `is`(notNullValue()))
        assertThat(barList.dataItems, `is`(listOf(bar)))

        val bazList = dao.relation("baz")
        assertThat(bazList.sharedName, `is`("baz"))
        assertThat(bazList, `is`(notNullValue()))
        assertThat(bazList.dataItems, `is`(emptyList()))
    }

    private fun insertSample(id: Int): DataClassFromDependency {
        val data = DataClassFromDependency(
                id = id,
                name = "foo"
        )
        dao.insert(data)
        return data
    }
}
