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
package androidx.core.i18n

import android.content.Context
import androidx.core.i18n.messageformat_icu.simple.MessageFormat
import java.util.Locale

public class MessageFormat private constructor() {
    public companion object {
        /**
         * Formats a message pattern string with a variable number of name/value pair arguments.
         * Creates an ICU MessageFormat for the locale and pattern, and formats with the arguments.
         *
         * @param context Android context object. Used to retrieve user preferences.
         * @param locale Locale for number formatting and plural selection etc.
         * @param msg an ICU-MessageFormat-syntax string
         * @param namedArguments map of argument name to argument value
         */
        @JvmStatic
        @JvmOverloads
        public fun format(
            context: Context,
            locale: Locale = Locale.getDefault(),
            msg: String,
            namedArguments: Map<String, Any>
        ): String {
            val result: StringBuffer = StringBuffer()
            return MessageFormat(context, msg, locale)
                .format(namedArguments, result, null)
                .toString()
        }

        /**
         * Formats a message pattern from Android resource for the default locale with a variable
         * number of name/value pair arguments. Creates an ICU MessageFormat for Locale.getDefault()
         * and pattern, and formats with the arguments.
         *
         * @param context Android context object
         * @param id Android string resource ID representing ICU-MessageFormat-syntax string
         * @param namedArguments map of argument name to argument value
         */
        @JvmStatic
        public fun format(context: Context, id: Int, namedArguments: Map<String, Any>): String {
            return format(
                context,
                Locale.getDefault(),
                context.resources.getString(id),
                namedArguments
            )
        }
    }
}
