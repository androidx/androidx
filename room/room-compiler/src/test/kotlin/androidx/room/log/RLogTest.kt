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

import androidx.room.compiler.processing.XAnnotation
import androidx.room.compiler.processing.XAnnotationValue
import androidx.room.compiler.processing.XElement
import androidx.room.compiler.processing.XMessager
import androidx.room.vo.Warning
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import javax.tools.Diagnostic

@RunWith(JUnit4::class)
class RLogTest {
    @Test
    fun testSafeFormat() {
        val messager = object : XMessager() {
            override fun onPrintMessage(
                kind: Diagnostic.Kind,
                msg: String,
                element: XElement?,
                annotation: XAnnotation?,
                annotationValue: XAnnotationValue?
            ) {}
        }
        val logger = RLog(messager, emptySet(), null)

        // UnknownFormatConversionException
        logger.w(Warning.ALL, "bad query: 'SELECT '%q' as formatString FROM'")

        // IllegalFormatConversionException
        logger.w(Warning.ALL, "bad query: 'SELECT strftime('%d', mAge) as mDay, mName FROM'")
    }
}