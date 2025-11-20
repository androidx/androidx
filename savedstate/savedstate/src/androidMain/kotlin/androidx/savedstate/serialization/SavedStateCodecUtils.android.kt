/*
 * Copyright 2024 The Android Open Source Project
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

import android.os.IBinder
import android.os.Parcelable
import androidx.savedstate.serialization.serializers.CharSequenceSerializer
import androidx.savedstate.serialization.serializers.DefaultParcelableSerializer
import androidx.savedstate.serialization.serializers.SparseArraySerializer
import java.io.Serializable as JavaSerializable
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.builtins.ArraySerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.nullable

internal val polymorphicCharSequenceDescriptor =
    PolymorphicSerializer(CharSequence::class).descriptor

internal val polymorphicParcelableDescriptor = PolymorphicSerializer(Parcelable::class).descriptor

internal val polymorphicJavaSerializableDescriptor =
    PolymorphicSerializer(JavaSerializable::class).descriptor

internal val polymorphicIBinderDescriptor = PolymorphicSerializer(IBinder::class).descriptor

@OptIn(ExperimentalSerializationApi::class)
internal val parcelableArrayDescriptor = ArraySerializer(DefaultParcelableSerializer).descriptor
@OptIn(ExperimentalSerializationApi::class)
internal val polymorphicParcelableArrayDescriptor =
    ArraySerializer(PolymorphicSerializer(Parcelable::class)).descriptor

internal val parcelableListDescriptor = ListSerializer(DefaultParcelableSerializer).descriptor
internal val polymorphicParcelableListDescriptor =
    ListSerializer(PolymorphicSerializer(Parcelable::class)).descriptor

@OptIn(ExperimentalSerializationApi::class)
internal val charSequenceArrayDescriptor = ArraySerializer(CharSequenceSerializer).descriptor
@OptIn(ExperimentalSerializationApi::class)
internal val polymorphicCharSequenceArrayDescriptor =
    ArraySerializer(PolymorphicSerializer(CharSequence::class)).descriptor

internal val charSequenceListDescriptor = ListSerializer(CharSequenceSerializer).descriptor
internal val polymorphicCharSequenceListDescriptor =
    ListSerializer(PolymorphicSerializer(CharSequence::class)).descriptor

internal val sparseParcelableArrayDescriptor =
    SparseArraySerializer(DefaultParcelableSerializer).descriptor
internal val polymorphicSparseParcelableArrayDescriptor =
    SparseArraySerializer(PolymorphicSerializer(Parcelable::class)).descriptor
internal val nullablePolymorphicSparseParcelableArrayDescriptor =
    SparseArraySerializer(PolymorphicSerializer(Parcelable::class).nullable).descriptor
