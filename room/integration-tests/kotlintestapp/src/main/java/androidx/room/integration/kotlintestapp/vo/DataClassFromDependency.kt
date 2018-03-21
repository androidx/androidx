/*
 * Copyright (C) 2018 The Android Open Source Project
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

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * used to test the case where kotlin classes from dependencies cannot be read properly.
 * Since the main db in this app is in the test module, the original classes serve as a dependency.
 */
@Entity
data class DataClassFromDependency(
        @PrimaryKey(autoGenerate = true)
        val id: Int,
        val name: String)