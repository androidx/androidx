package androidx.sqlite.inspection.test

import kotlin.Long
import kotlin.String

interface TestEntity {
    val id: Long

    val value: String

    data class Impl(
        override val id: Long,
        override val value: String
    ) : TestEntity {
        override fun toString(): String = """
    |TestEntity.Impl [
    |  id: $id
    |  value: $value
    |]
    """.trimMargin()
    }
}
