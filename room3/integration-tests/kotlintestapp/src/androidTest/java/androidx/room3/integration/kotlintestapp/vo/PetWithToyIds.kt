/*
 * Copyright 2017 The Android Open Source Project
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
import androidx.room3.Ignore
import androidx.room3.Relation

class PetWithToyIds {
    @Embedded val pet: Pet?

    @Relation(
        parentColumn = "petId",
        entityColumn = "petId",
        projection = ["id"],
        entity = Toy::class,
    )
    var toyIds: List<Int>? = null

    // for the relation
    constructor(pet: Pet?) {
        this.pet = pet
    }

    @Ignore
    constructor(pet: Pet?, toyIds: List<Int>?) {
        this.pet = pet
        this.toyIds = toyIds
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as PetWithToyIds
        if (if (pet != null) pet != that.pet else that.pet != null) return false
        return if (toyIds != null) toyIds == that.toyIds else that.toyIds == null
    }

    override fun hashCode(): Int {
        var result = pet?.hashCode() ?: 0
        result = 31 * result + (toyIds?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return ("PetWithToyIds{pet=$pet, toyIds=$toyIds}")
    }
}
