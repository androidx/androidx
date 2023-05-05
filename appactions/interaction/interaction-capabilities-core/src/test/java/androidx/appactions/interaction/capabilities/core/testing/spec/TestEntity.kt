/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.appactions.interaction.capabilities.core.testing.spec

import java.time.Duration
import java.time.ZonedDateTime

class TestEntity internal constructor(
    val id: String?,
    val name: String?,
    val duration: Duration?,
    val zonedDateTime: ZonedDateTime?,
    val enum: TestEnum?,
    val entity: TestEntity?
) {
    override fun toString(): String {
        return "TestEntity(identifier=$id, name=$name, duration=$duration, " +
            "zonedDateTime=$zonedDateTime, enum=$enum, entity=$entity)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TestEntity

        if (id != other.id) return false
        if (name != other.name) return false
        if (duration != other.duration) return false
        if (zonedDateTime != other.zonedDateTime) return false
        if (enum != other.enum) return false
        if (entity != other.entity) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result += 31 * name.hashCode()
        result += 31 * duration.hashCode()
        result += 31 * zonedDateTime.hashCode()
        result += 31 * enum.hashCode()
        result += 31 * entity.hashCode()
        return result
    }

    class Builder {
        private var id: String? = null
        private var name: String? = null
        private var duration: Duration? = null
        private var zonedDateTime: ZonedDateTime? = null
        private var enum: TestEnum? = null
        private var entity: TestEntity? = null

        fun setId(id: String): Builder = apply { this.id = id }
        fun setName(name: String): Builder = apply { this.name = name }
        fun setDuration(duration: Duration): Builder = apply { this.duration = duration }
        fun setZonedDateTime(zonedDateTime: ZonedDateTime): Builder = apply {
            this.zonedDateTime = zonedDateTime
        }
        fun setEnum(enum: TestEnum): Builder = apply { this.enum = enum }
        fun setEntity(entity: TestEntity): Builder = apply { this.entity = entity }

        fun build(): TestEntity = TestEntity(id, name, duration, zonedDateTime, enum, entity)
    }
}