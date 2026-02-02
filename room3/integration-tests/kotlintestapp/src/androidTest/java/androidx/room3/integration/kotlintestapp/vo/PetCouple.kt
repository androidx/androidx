/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.room3.integration.kotlintestapp.vo

import androidx.room3.Embedded
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import androidx.room3.RoomWarnings

@Entity
@Suppress(RoomWarnings.PRIMARY_KEY_FROM_EMBEDDED_IS_DROPPED)
data class PetCouple(
    @PrimaryKey val id: String,
    @Embedded(prefix = "male_") val male: Pet,
    @Embedded(prefix = "female_") val female: Pet?,
)
