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

package androidx.navigation3.scene

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigationevent.ExperimentalNavigationEventApi
import androidx.navigationevent.NavigationEventHistory
import androidx.navigationevent.NavigationEventInfo

/**
 * [NavMetadataKey] for defining a localized or dynamic title for a navigation scene or entry.
 *
 * The associated lambda is invoked in a `@Composable` context during navigation state resolution.
 */
public object TitleMetadataKey : NavMetadataKey<@Composable () -> String>

/**
 * [NavMetadataKey] for defining a URL or location path representation for a navigation scene or
 * entry.
 *
 * The associated lambda is invoked in a `@Composable` context during navigation state resolution.
 */
public object UrlMetadataKey : NavMetadataKey<@Composable () -> String>

/**
 * Represents a snapshot of the visible destinations in a navigation container.
 *
 * This class provides the necessary context for building animations during navigation gestures,
 * like predictive back. It's a simple data holder that feeds into the [NavigationEventHistory].
 *
 * Note: [title] and [url] are retrieved from [Scene.metadata], which by default only includes the
 * metadata of the last [NavEntry] in [Scene.entries]. For example, if a scene renders
 * `listOf(entryA, entryB)`, [title] and [url] will be populated only if `entryB` contains the
 * metadata.
 *
 * @property scene The scene whose state is used by the NavigationEvent
 * @property title The resolved window or destination title associated with [scene], or `null` if
 *   unspecified.
 * @property url The resolved URL or path representation associated with [scene], or `null` if
 *   unspecified.
 */
@Immutable
@OptIn(ExperimentalNavigationEventApi::class)
public class SceneInfo<T : Any>(
    public val scene: Scene<T>,
    public override val title: String? = null,
    public override val url: String? = null,
) : NavigationEventInfo() {

    /**
     * Binary-compatible constructor for callers compiled against versions without [title] and
     * [url].
     */
    public constructor(scene: Scene<T>) : this(scene = scene, title = null, url = null)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as SceneInfo<*>

        return scene == other.scene && title == other.title && url == other.url
    }

    override fun hashCode(): Int {
        var result = scene.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + url.hashCode()
        return result
    }

    override fun toString(): String {
        return "SceneInfo(scene=$scene, title=$title, url=$url)"
    }
}
