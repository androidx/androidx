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

package androidx.glance.wear.core

import androidx.annotation.RestrictTo
import java.util.Objects

/**
 * An identifier for a widget instance.
 *
 * This is an id that is created by the system and passed to the widget provider. The id is unique
 * for each widget instance and is persistent across device reboots.
 *
 * @property namespace The namespace indicating the Host where the id is generated
 * @property id The integer id of this instance
 */
public class WidgetInstanceId(public val namespace: String, public val id: Int) {

    /** Returns a string representation of the id that can be used for logging and debugging. */
    public fun flattenToString(): String = "$namespace:$id"

    public override fun equals(other: Any?): Boolean =
        when {
            this === other -> true
            other !is WidgetInstanceId -> false
            else -> id == other.id && namespace == other.namespace
        }

    public override fun hashCode(): Int = Objects.hash(namespace, id)

    public override fun toString(): String {
        return "WidgetInstanceId(namespace=$namespace, instanceId=$id)"
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public companion object {
        /** The namespace used for widget instances in the carousel. */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public const val WIDGET_CAROUSEL_NAMESPACE: String = "tiles"
    }
}
