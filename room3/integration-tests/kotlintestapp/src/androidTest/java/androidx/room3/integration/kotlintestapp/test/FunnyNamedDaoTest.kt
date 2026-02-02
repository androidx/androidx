/*
 * Copyright (C) 2017 The Android Open Source Project
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
import androidx.room3.integration.kotlintestapp.vo.FunnyNamedEntity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class FunnyNamedDaoTest : TestDatabaseTest() {

    @Test
    fun readWrite() {
        val entity = FunnyNamedEntity(1, "a")
        database.funnyNamedDao().insert(entity)
        val loaded = database.funnyNamedDao().load(1)
        assertThat(loaded).isEqualTo(entity)
    }

    @Test
    fun update() {
        val entity = FunnyNamedEntity(1, "a")
        database.funnyNamedDao().insert(entity)
        entity.value = "b"
        database.funnyNamedDao().update(entity)
        val loaded = database.funnyNamedDao().load(1)
        assertThat(loaded!!.value).isEqualTo("b")
    }

    @Test
    fun delete() {
        val entity = FunnyNamedEntity(1, "a")
        database.funnyNamedDao().insert(entity)
        assertThat(database.funnyNamedDao().load(1)).isNotNull()
        database.funnyNamedDao().delete(entity)
        assertThat(database.funnyNamedDao().load(1)).isNull()
    }
}
