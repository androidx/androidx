/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.room.log

import androidx.room.vo.Warning
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.mock

@RunWith(JUnit4::class)
class RLogTest {

    val messager = mock(RLog.Messager::class.java)

    @Test
    fun testSafeFormat() {
        val logger = RLog(messager, emptySet(), null)

        // UnknownFormatConversionException
        logger.w(Warning.ALL, "bad query: 'SELECT '%q' as formatString FROM'")

        // IllegalFormatConversionException
        logger.w(Warning.ALL, "bad query: 'SELECT strftime('%d', mAge) as mDay, mName FROM'")
    }
}