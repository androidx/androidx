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

import android.support.tools.jetifier.core.rules.JavaField
import android.support.tools.jetifier.core.rules.JavaType
import android.support.tools.jetifier.core.transform.bytecode.CoreRemapper
import org.objectweb.asm.commons.Remapper

/**
 * Extends [Remapper] with a capability to rewrite field names together with their owner.
 */
class CustomRemapper(val remapperImpl: CoreRemapper) : Remapper() {

    override fun map(typeName: String): String {
        return remapperImpl.rewriteType(JavaType(typeName)).fullName
    }

    fun mapField(ownerName: String, fieldName: String): JavaField {
        return remapperImpl.rewriteField(JavaField(ownerName, fieldName))
    }

    override fun mapFieldName(owner: String?, name: String, desc: String?): String {
        throw RuntimeException("This should not be called")
    }
}