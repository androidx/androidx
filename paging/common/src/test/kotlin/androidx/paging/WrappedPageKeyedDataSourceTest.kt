/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.paging

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.test.assertTrue

@RunWith(JUnit4::class)
class WrappedPageKeyedDataSourceTest {

    @Test
    fun propagatesInvalidation() {
        val dataSource = WrapperPageKeyedDataSource(TestPageKeyedDataSource(listOf(0))) { it }

        var kotlinInvalidated = false
        dataSource.addInvalidatedCallback {
            kotlinInvalidated = true
        }
        var javaInvalidated = false
        dataSource.addInvalidatedCallback(object : DataSource.InvalidatedCallback {
            override fun onInvalidated() {
                javaInvalidated = true
            }
        })

        dataSource.invalidate()
        assertTrue { dataSource.isInvalid }
        assertTrue { kotlinInvalidated }
        assertTrue { javaInvalidated }
    }
}
