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

package androidx.savedstate.serialization

import android.util.SparseArray
import androidx.savedstate.serialization.serializers.SizeFSerializer
import androidx.savedstate.serialization.serializers.SizeSerializer
import androidx.savedstate.serialization.serializers.SparseArraySerializer
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual

internal actual fun getDefaultSerializersModuleOnPlatform(): SerializersModule = SerializersModule {
    contextual(SizeSerializer)
    contextual(SizeFSerializer)
    contextual(SparseArray::class) { argSerializers ->
        SparseArraySerializer(argSerializers.first())
    }
}
