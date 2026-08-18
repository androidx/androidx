/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.glance.wear.lint

import com.android.ide.common.rendering.api.ResourceNamespace
import com.android.ide.common.resources.ResourceRepository
import com.android.resources.ResourceType
import com.android.resources.ResourceUrl

internal sealed interface ResolvedContainerType {
    data object TileCompat : ResolvedContainerType

    data object Large : ResolvedContainerType

    data object Small : ResolvedContainerType

    data class Unrecognized(val rawValue: String) : ResolvedContainerType
}

internal object WearWidgetContainerTypeResolver {
    private const val VALUE_CONTAINER_TYPE_TILE_COMPAT_STRING = "TILE_COMPAT"
    private const val VALUE_CONTAINER_TYPE_TILE_COMPAT_INT = "0"

    private const val VALUE_CONTAINER_TYPE_LARGE_STRING = "LARGE"
    private const val VALUE_CONTAINER_TYPE_LARGE_INT = "1"

    private const val VALUE_CONTAINER_TYPE_SMALL_STRING = "SMALL"
    private const val VALUE_CONTAINER_TYPE_SMALL_INT = "2"

    fun resolve(resourceRepository: ResourceRepository?, rawType: String): ResolvedContainerType {
        var current = rawType
        val visited = mutableSetOf<String>()

        while (visited.add(current)) {
            val resUrl = ResourceUrl.parse(current)
            if (
                resUrl != null &&
                    (resUrl.type == ResourceType.INTEGER || resUrl.type == ResourceType.STRING)
            ) {
                val items =
                    resourceRepository?.getResources(
                        ResourceNamespace.RES_AUTO,
                        resUrl.type,
                        resUrl.name,
                    )
                val resolvedValue =
                    items?.firstOrNull { it.configuration.isDefault }?.resourceValue?.value
                        ?: items?.firstOrNull()?.resourceValue?.value
                if (resolvedValue != null) {
                    current = resolvedValue
                    continue
                }
                return when (resUrl.name) {
                    "glance_wear_container_type_large" -> ResolvedContainerType.Large
                    "glance_wear_container_type_small" -> ResolvedContainerType.Small
                    else -> ResolvedContainerType.Unrecognized(rawType)
                }
            }

            return when {
                current.equals(VALUE_CONTAINER_TYPE_TILE_COMPAT_STRING, ignoreCase = true) ||
                    current == VALUE_CONTAINER_TYPE_TILE_COMPAT_INT ->
                    ResolvedContainerType.TileCompat

                current.equals(VALUE_CONTAINER_TYPE_LARGE_STRING, ignoreCase = true) ||
                    current == VALUE_CONTAINER_TYPE_LARGE_INT -> ResolvedContainerType.Large

                current.equals(VALUE_CONTAINER_TYPE_SMALL_STRING, ignoreCase = true) ||
                    current == VALUE_CONTAINER_TYPE_SMALL_INT -> ResolvedContainerType.Small

                else -> ResolvedContainerType.Unrecognized(rawType)
            }
        }

        return ResolvedContainerType.Unrecognized(rawType)
    }
}
