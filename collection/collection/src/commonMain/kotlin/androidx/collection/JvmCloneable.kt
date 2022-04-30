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

package androidx.collection

/**
 * Placeholder interface that will be mapped to java.lang.Cloneable on JVM and a custom interface
 * in other platforms.
 */
internal expect interface CloneableKmp

/**
 * Placeholder class that serves as a parent for cloneable classes.
 * On JVM, this maps to `java.lang.Object` whereas on other platforms, it is just an empty class.
 *
 * Note that this class only exists to keep binary/behavior compatibility with the JVM artifact.
 * You should never directly use this class from your common code.
 *
 * @constructor Empty constructor to initialize the object.
 */
@Deprecated(
    message = """
        This class only exists for JVM binary compatibility, you should never use it directly.
    """,
    level = DeprecationLevel.WARNING
)
public expect abstract class JvmCloneableAny {
    protected constructor()

    /**
     * Stub implementation for java.lang.object.clone.
     *
     * On JVM, this delegates to Object.clone.
     * On other platforms, it will return `null`.
     */
    public open fun clone(): Any?
}
