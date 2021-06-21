/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.room.compiler.processing

/**
 * Field in an [XTypeElement].
 */
interface XFieldElement : XVariableElement, XHasModifiers {
    /**
     * The element that declared this field.
     * For fields declared in classes, this will be an [XTypeElement].
     *
     * For fields declared as top level properties in Kotlin:
     *   * When running with KAPT, the value will be an [XTypeElement].
     *   * When running with KSP, if this property is coming from the classpath, the value will
     *   be an [XTypeElement].
     *   * When running with KSP, if this property is in source, the value will **NOT** be an
     *   [XTypeElement]. If you need the generated synthetic java class name, you can use
     *   [XMemberContainer.className] property.
     */
    val enclosingElement: XMemberContainer

    override val fallbackLocationText: String
        get() = "$name in ${enclosingElement.fallbackLocationText}"
}
