/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.runtime

import androidx.annotation.RestrictTo

/**
 * TypeHolder is used to decouple the dependency to rendering runtime. Pass the rendering runtime
 * object to rendering runtime through the SceneCore with TypeHolder.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public sealed class TypeHolder<T>(public val value: T, public val type: Class<*>) {
    public companion object {
        /** If cast is success, return the typed holder. Otherwise, return null. */
        @JvmStatic
        public fun <T : Any, U : TypeHolder<T>> safeCast(
            anyTypeHolder: TypeHolder<*>,
            targetType: Class<T>,
        ): U? {
            if (anyTypeHolder.type == targetType) {
                // We checked the type above, so this cast is safe. Suppress the warning.
                @Suppress("UNCHECKED_CAST") val typedHolder = anyTypeHolder as U
                return typedHolder
            } else {
                return null
            }
        }

        /** If cast is success, return the typed holder. Otherwise, throw ClassCastException. */
        @JvmStatic
        public fun <T : Any> assertCast(
            anyTypeHolder: TypeHolder<*>,
            targetType: Class<T>,
        ): TypeHolder<T> {
            if (anyTypeHolder.type == targetType) {
                // We checked the type above, so this cast is safe. Suppress the warning.
                @Suppress("UNCHECKED_CAST") val typedHolder = anyTypeHolder as TypeHolder<T>
                return typedHolder
            } else {
                throw ClassCastException(
                    "Expected TypeHolder<${targetType.simpleName}>, " +
                        "but received ${anyTypeHolder.type}"
                )
            }
        }

        /**
         * If cast is success, return the value of the holder. Otherwise, throw ClassCastException.
         */
        @JvmStatic
        public fun <T : Any> assertGetValue(anyTypeHolder: TypeHolder<*>, targetType: Class<T>): T {
            val typedHolder = assertCast(anyTypeHolder, targetType)
            return typedHolder.value
        }
    }
}

/**
 * NodeHolder is used to decouple the dependency to rendering runtime. Pass the rendering runtime
 * object to rendering runtime through the SceneCore with NodeHolder.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class NodeHolder<T>(node: T, type: Class<*>) : TypeHolder<T>(node, type)

/**
 * SubspaceNodeHolder is used to decouple the dependency to rendering runtime. Pass the rendering
 * runtime object to rendering runtime through the JXR SDK with SubspaceNodeHolder.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class SubspaceNodeHolder<T>(subspaceNode: T, type: Class<*>) :
    TypeHolder<T>(subspaceNode, type)

/**
 * SpatialStateHolder is used to decouple the dependency to rendering runtime. Pass the rendering
 * runtime SpatialState to rendering runtime through the JxrPlatformAdapter with SpatialStateHolder.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class SpatialStateHolder<T>(state: T, type: Class<*>) : TypeHolder<T>(state, type)
