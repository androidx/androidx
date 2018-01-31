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

import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.MethodRemapper

/**
 * Currently only adds field context awareness into [MethodRemapper]
 */
internal class CustomMethodRemapper(mv:MethodVisitor,
                                    private val customRemapper: CustomRemapper)
    : MethodRemapper(Opcodes.ASM5, mv, customRemapper) {

    override fun visitFieldInsn(opcode: Int, owner: String, name: String, desc: String?) {
        mv ?: return

        val field = customRemapper.mapField(owner, name)
        mv.visitFieldInsn(opcode, field.owner.fullName, field.name, remapper.mapDesc(desc))
    }
}