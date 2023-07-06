/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.core.i18n.messageformat_icu.simple

import androidx.core.i18n.DateTimeFormatter
import java.text.DateFormat
import java.text.FieldPosition
import java.text.Format
import java.text.ParseException
import java.text.ParsePosition
import java.util.Calendar
import java.util.Date
import java.util.Objects

/**
 * Decorator for [DateTimeFormatter], because [MessageFormat] expects formatters that
 * extend [java.text.Format].
 */
internal class DateTimeFormatterAsFormat(private val realFormatter: DateTimeFormatter) : Format() {
    override fun format(
        obj: Any,
        stringBuffer: StringBuffer,
        fieldPosition: FieldPosition
    ): StringBuffer {
        val result = when (obj) {
            is Date -> realFormatter.format(obj)
            is Calendar -> realFormatter.format(obj)
            is Long -> realFormatter.format(obj)
            else -> Objects.toString(obj)
        }
        return stringBuffer.append(result)
    }

    override fun parseObject(s: String, parsePosition: ParsePosition): Any {
        java.text.SimpleDateFormat.getDateInstance(DateFormat.LONG).parseObject("")
        throw ParseException("Parsing not implemented", 0)
    }
}
