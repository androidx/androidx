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

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.ClassRemapper

/**
 * Currently only adds field context awareness into [ClassRemapper] and substitutes the default
 * method remapper with [CustomMethodRemapper]
 */
class CustomClassRemapper(cv: ClassVisitor, private val customRemapper: CustomRemapper)
    : ClassRemapper(Opcodes.ASM5, cv, customRemapper) {

    override fun visitField(access: Int,
                            name: String,
                            desc: String?,
                            signature: String?,
                            value: Any?) : FieldVisitor? {
        cv ?: return null

        val field = customRemapper.mapField(className, name)
        val fieldVisitor = cv.visitField(
                access,
                field.name,
                remapper.mapDesc(desc),
                remapper.mapSignature(signature, true),
                remapper.mapValue(value))

        fieldVisitor ?: return null

        return createFieldRemapper(fieldVisitor)
    }

    override fun createMethodRemapper(mv: MethodVisitor) : MethodVisitor {
        return CustomMethodRemapper(mv, customRemapper)
    }
}
