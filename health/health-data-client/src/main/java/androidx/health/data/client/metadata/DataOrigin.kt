/*
 * Copyright (C) 2022 The Android Open Source Project
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
package androidx.health.data.client.metadata

import androidx.health.data.client.records.Record

/**
 * Specifies the original source of any [Record]: application that inserted it and device on which
 * the data was generated.
 *
 * @property packageName auto-populated by Health Platform at insertion time of the client package
 * name.
 */
public class DataOrigin(public val packageName: String) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DataOrigin) return false

        if (packageName != other.packageName) return false

        return true
    }

    override fun hashCode(): Int {
        return packageName.hashCode()
    }
}
