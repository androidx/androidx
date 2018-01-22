/*
 * Copyright (C) 2017 The Android Open Source Project
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
 * limitations under the License
 */

package android.support.tools.jetifier.core.transform.bytecode.asm

import android.support.tools.jetifier.core.rules.JavaType
import android.support.tools.jetifier.core.transform.bytecode.CoreRemapper
import org.objectweb.asm.commons.Remapper

/**
 * Extends [Remapper] to allow further customizations.
 */
class CustomRemapper(private val remapper: CoreRemapper) : Remapper() {

    override fun map(typeName: String): String {
        return remapper.rewriteType(JavaType(typeName)).fullName
    }

    override fun mapValue(value: Any?): Any? {
        val stringMaybe = value as? String
        if (stringMaybe == null) {
            return super.mapValue(value)
        }

        return remapper.rewriteString(stringMaybe)
    }
}