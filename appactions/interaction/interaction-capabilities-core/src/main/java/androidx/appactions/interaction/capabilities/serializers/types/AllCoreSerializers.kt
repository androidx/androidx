// Copyright 2023 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package androidx.appactions.interaction.capabilities.serializers.types

import kotlin.jvm.JvmStatic

/**
 * Exposes static methods to access all serializers in interaction-capabilities-core.
 *
 * Not intended to be used directly but instead to be accessed via reflection by the serialization
 * runtime. In that sense, this object acts as a sort-of registry of serializers.
 *
 * Some of the static factory methods in this object may get pruned by r8/proguard based on what
 * types the application code explicitly references.
 *
 * Also see the proguard file for this lib for more context.
 */
public object AllCoreSerializers {
    @JvmStatic public fun getIntangibleSerializer(): IntangibleSerializer = IntangibleSerializer()

    @JvmStatic public fun getPersonSerializer(): PersonSerializer = PersonSerializer()

    @JvmStatic public fun getThingSerializer(): ThingSerializer = ThingSerializer()
}
