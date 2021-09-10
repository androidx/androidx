/*
 * Copyright 2021 The Android Open Source Project
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

import com.squareup.javapoet.ClassName

/**
 * Common interface for elements that can contain methods and properties.
 *
 * This is especially important for handling top level methods / properties in KSP where the
 * synthetic container class does not exist
 */
interface XMemberContainer : XElement {
    /**
     * The JVM ClassName for this container.
     * For top level members of a Kotlin file, you can use this [className] for code generation.
     */
    val className: ClassName

    /**
     * The [XType] for the container if this is an [XTypeElement] otherwise `null` if a type
     * representing this container does not exist (e.g. a top level Kotlin source file)
     */
    val type: XType?
}
