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
package androidx.room.integration.kotlintestapp.vo

import androidx.room.Embedded
import androidx.room.Ignore
import androidx.room.Relation

class PetWithToyIds {
    @Embedded
    val mPet: Pet?

    @Relation(
        parentColumn = "mPetId",
        entityColumn = "mPetId",
        projection = ["mId"],
        entity = Toy::class
    )
    var mToyIds: List<Int>? = null

    // for the relation
    constructor(pet: Pet?) {
        this.mPet = pet
    }

    @Ignore
    constructor(pet: Pet?, toyIds: List<Int>?) {
        this.mPet = pet
        this.mToyIds = toyIds
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as PetWithToyIds
        if (if (mPet != null) !mPet.equals(that.mPet) else that.mPet != null) return false
        return if (mToyIds != null) mToyIds == that.mToyIds else that.mToyIds == null
    }

    override fun hashCode(): Int {
        var result = mPet?.hashCode() ?: 0
        result = 31 * result + if (mToyIds != null) mToyIds.hashCode() else 0
        return result
    }

    override fun toString(): String {
        return ("PetWithToyIds{" +
            "pet=" + mPet +
            ", toyIds=" + mToyIds +
            '}')
    }
}
