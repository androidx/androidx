/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.room.integration.kotlintestapp.testutil

import androidx.recyclerview.widget.DiffUtil
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class PagingEntity(@PrimaryKey val id: Int, val value: String = "item_$id") {
    companion object {
        val DIFF_CALLBACK =
            object : DiffUtil.ItemCallback<PagingEntity>() {
                override fun areItemsTheSame(
                    oldItem: PagingEntity,
                    newItem: PagingEntity
                ): Boolean {
                    return oldItem.id == newItem.id
                }

                override fun areContentsTheSame(
                    oldItem: PagingEntity,
                    newItem: PagingEntity
                ): Boolean {
                    return oldItem == newItem
                }
            }
    }
}
