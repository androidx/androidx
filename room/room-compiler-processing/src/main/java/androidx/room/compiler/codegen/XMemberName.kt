/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.room.compiler.codegen

import com.squareup.kotlinpoet.MemberName.Companion.member

/**
 * Represents the name of a member (such as a function or a property).
 *
 * @property enclosingClassName The enclosing class name or null if this is a package member,
 * such as a top-level function.
 */
class XMemberName internal constructor(
    val enclosingClassName: XClassName?,
    internal val java: JCodeBlock,
    internal val kotlin: KMemberName
) {
    val simpleName = kotlin.simpleName

    companion object {
        /**
         * Creates a [XMemberName] that is contained by the receiving class's companion object.
         *
         * @param isJvmStatic if the companion object member is annotated with [JvmStatic] or not.
         * This will cause generated code to be slightly different in Java since the member can be
         * referenced without the companion object's `INSTANCE`.
         */
        fun XClassName.companionMember(
            simpleName: String,
            isJvmStatic: Boolean = false
        ): XMemberName {
            return XMemberName(
                enclosingClassName = this,
                java = if (isJvmStatic) {
                    JCodeBlock.of("$T.$L", this.java, simpleName)
                } else {
                    JCodeBlock.of("$T.INSTANCE.$L", this.java.nestedClass("Companion"), simpleName)
                },
                kotlin = this.kotlin.nestedClass("Companion").member(simpleName)
            )
        }

        /**
         * Creates a [XMemberName] is that is contained by the receiving class's package, i.e.
         * a top-level function or property.
         *
         * @see [androidx.room.compiler.processing.XMemberContainer.asClassName]
         */
        fun XClassName.packageMember(simpleName: String): XMemberName {
            return XMemberName(
                enclosingClassName = null,
                java = JCodeBlock.of("$T.$L", this.java, simpleName),
                kotlin = KMemberName(this.packageName, simpleName)
            )
        }
    }
}
