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
package android.arch.persistence.room.integration.kotlintestapp.test

import android.arch.persistence.room.integration.kotlintestapp.vo.DataClassFromDependency
import android.support.test.runner.AndroidJUnit4
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DependencyDaoTest : TestDatabaseTest() {
    @Test
    fun insertAndGet() {
        val dao = database.dependencyDao()
        val data = DataClassFromDependency(
                id = 3,
                name = "foo"
        )
        dao.insert(data)
        assertThat(dao.selectAll(), `is`(listOf(data)))
    }
}