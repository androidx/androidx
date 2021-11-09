package androidx.sqlite.inspection.test

import com.squareup.sqldelight.Query
import com.squareup.sqldelight.Transacter
import kotlin.Any
import kotlin.Long
import kotlin.String

interface TestEntityQueries : Transacter {
    fun <T : Any> selectAll(mapper: (id: Long, value: String) -> T): Query<T>

    fun selectAll(): Query<TestEntity>

    fun insertOrReplace(value: String)
}
