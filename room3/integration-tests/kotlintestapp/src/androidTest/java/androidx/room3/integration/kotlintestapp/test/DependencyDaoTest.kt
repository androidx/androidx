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
package androidx.room3.integration.kotlintestapp.test

import androidx.kruth.assertThat
import androidx.room3.integration.kotlintestapp.dao.DependencyDao
import androidx.room3.integration.kotlintestapp.vo.DataClassFromDependency
import androidx.room3.integration.kotlintestapp.vo.EmbeddedFromDependency
import androidx.room3.integration.kotlintestapp.vo.PojoFromDependency
import androidx.test.filters.SmallTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@SmallTest
@RunWith(Parameterized::class)
class DependencyDaoTest(useDriver: UseDriver) : TestDatabaseTest(useDriver) {

    private companion object {
        @JvmStatic
        @Parameters(name = "useDriver={0}")
        fun parameters() = arrayOf(UseDriver.BUNDLED, UseDriver.ANDROID)
    }

    lateinit var dao: DependencyDao

    @Before
    fun init() {
        dao = database.dependencyDao()
    }

    @Test
    fun insertAndGet() {
        val data = insertSample(3)
        assertThat(dao.selectAll()).containsExactly(data)
    }

    @Test
    fun insertAndGetByQuery() {
        val data = insertSample(3)
        assertThat(dao.findById(3)).isEqualTo(data)
        assertThat(dao.findById(5)).isNull()
    }

    @Test
    fun insertAndGetByQuery_embedded() {
        val data = insertSample(3)
        assertThat(dao.findEmbedded(3)).isEqualTo(EmbeddedFromDependency(data))
        assertThat(dao.findEmbedded(5)).isNull()
    }

    @Test
    fun insertAndGetByQuery_pojo() {
        val data = insertSample(3)
        assertThat(dao.findPojo(3)).isEqualTo(PojoFromDependency(id = data.id, name = data.name))
        assertThat(dao.findPojo(5)).isNull()
    }

    @Test
    fun getRelation() {
        val foo1 = DataClassFromDependency(id = 3, name = "foo")
        val foo2 = DataClassFromDependency(id = 4, name = "foo")
        val bar = DataClassFromDependency(id = 5, name = "bar")
        dao.insert(foo1, foo2, bar)
        val fooList = dao.relation("foo")
        assertThat(fooList).isNotNull()
        assertThat(fooList?.sharedName).isEqualTo("foo")
        assertThat(fooList?.dataItems).containsExactly(foo1, foo2)

        val barList = dao.relation("bar")
        assertThat(barList).isNotNull()
        assertThat(barList?.sharedName).isEqualTo("bar")
        assertThat(barList?.dataItems).containsExactly(bar)

        val bazList = dao.relation("baz")
        assertThat(bazList).isNotNull()
        assertThat(bazList?.sharedName).isEqualTo("baz")
        assertThat(bazList?.dataItems).isEmpty()
    }

    private fun insertSample(id: Int): DataClassFromDependency {
        val data = DataClassFromDependency(id = id, name = "foo")
        dao.insert(data)
        return data
    }
}
