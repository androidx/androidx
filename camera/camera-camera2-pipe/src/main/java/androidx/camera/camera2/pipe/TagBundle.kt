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

package androidx.camera.camera2.pipe

import androidx.annotation.RestrictTo
import androidx.camera.common.Metadata

/**
 * An immutable set of tags that decides which frames a [FrameBuffer] receives.
 *
 * Use [TagBundle] as the value type of a [Metadata.Key], for example
 * `Metadata.Key<TagBundle<String>>`. Only keys of this type are treated as tags; a key whose value
 * is a plain [Set] is a regular parameter.
 * - In the parameters of [FrameGraph.captureWith], a [TagBundle] lists the tags that a frame must
 *   carry to be added to the buffer. The tag bundles of all attached buffers are merged into the
 *   repeating request.
 * - In [Request.extras], a [TagBundle] lists the tags of the frames produced by that request.
 *
 * Use [tagsOf] to create a [TagBundle] with a single tag (`tagsOf("a")`) or several tags
 * (`tagsOf("a", "b")`).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public interface TagBundle<out T> : Set<T>

/** Returns a [TagBundle] that contains [tags]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun <T> tagsOf(vararg tags: T): TagBundle<T> = TagBundleImpl(tags.toSet())

/** Returns a [TagBundle] that contains [tags]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun <T> Iterable<T>.toTagBundle(): TagBundle<T> =
    this as? TagBundle<T> ?: TagBundleImpl(toSet())

/** Returns true if this is a [Metadata.Key] whose type is a [TagBundle]. */
internal fun Any.isTagKey(): Boolean =
    this is Metadata.Key<*> && TagBundle::class.java.isAssignableFrom(type)

private class TagBundleImpl<T>(private val tags: Set<T>) : TagBundle<T>, Set<T> by tags {
    override fun equals(other: Any?): Boolean = tags == other

    override fun hashCode(): Int = tags.hashCode()

    override fun toString(): String = "TagBundle$tags"
}
