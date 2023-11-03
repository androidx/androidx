/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.inspection.framework

import java.lang.reflect.Method

val Method.signature: String
    get() {
        val sb = StringBuilder()
        sb.append(name).append('(')
        parameterTypes.forEach {
            sb.append(it.signature)
        }
        sb.append(')').append(returnType.signature)
        return sb.toString()
    }

val <T> Class<T>.signature: String
    get() {
        if (isPrimitive) {
            return when (this) {
                java.lang.Boolean.TYPE -> "Z"
                java.lang.Byte.TYPE -> "B"
                Character.TYPE -> "C"
                java.lang.Short.TYPE -> "S"
                Integer.TYPE -> "I"
                java.lang.Long.TYPE -> "J"
                java.lang.Float.TYPE -> "F"
                java.lang.Double.TYPE -> "D"
                Void.TYPE -> "V"
                else -> error("Unknown primitive type")
            }
        }
        return "L${name.replace('.', '/')};"
    }
