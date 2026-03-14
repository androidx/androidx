import androidx.room3.RoomDatabase
import androidx.room3.RoomRawQuery
import androidx.room3.util.getColumnIndexOrThrow
import androidx.room3.util.performSuspending
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.prepare
import androidx.sqlite.step
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room3.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
internal class MyDao_Impl(
  __db: RoomDatabase,
) : MyDao {
  private val __db: RoomDatabase

  private val __lettersReturnTypeConverter: LettersReturnTypeConverter =
      LettersReturnTypeConverter()
  init {
    this.__db = __db
  }

  public override suspend fun getA(): A<MyEntity> {
    val _sql: String = "SELECT * FROM MyEntity"
    return __lettersReturnTypeConverter.convertA(listOf("MyEntity"), __db) {
      performSuspending(__db, true, false) { _connection ->
        val _stmt: SQLiteStatement = _connection.prepare(_sql)
        try {
          val _columnIndexOfPk: Int = getColumnIndexOrThrow(_stmt, "pk")
          val _result: MyEntity
          if (_stmt.step()) {
            val _tmpPk: Int
            _tmpPk = _stmt.getLong(_columnIndexOfPk).toInt()
            _result = MyEntity(_tmpPk)
          } else {
            error("The query result was empty, but expected a single row to return a NON-NULL object of type 'MyEntity'.")
          }
          _result
        } finally {
          _stmt.close()
        }
      }
    }
  }

  public override suspend fun getB(): B<MyEntity> {
    val _sql: String = "SELECT * FROM MyEntity"
    val _rawQuery: RoomRawQuery = RoomRawQuery(_sql)
    return __lettersReturnTypeConverter.convertB(__db, _rawQuery) { _converterQuery ->
      performSuspending(__db, true, false) { _connection ->
        val _stmt: SQLiteStatement = _connection.prepare(_converterQuery.sql)
        try {
          _converterQuery.getBindingFunction().invoke(_stmt)
          val _columnIndexOfPk: Int = getColumnIndexOrThrow(_stmt, "pk")
          val _result: MyEntity
          if (_stmt.step()) {
            val _tmpPk: Int
            _tmpPk = _stmt.getLong(_columnIndexOfPk).toInt()
            _result = MyEntity(_tmpPk)
          } else {
            error("The query result was empty, but expected a single row to return a NON-NULL object of type 'MyEntity'.")
          }
          _result
        } finally {
          _stmt.close()
        }
      }
    }
  }

  public override suspend fun getC(): C<MyEntity> {
    val _sql: String = "SELECT * FROM MyEntity"
    return __lettersReturnTypeConverter.convertC(false, __db, arrayOf("MyEntity")) {
      performSuspending(__db, true, false) { _connection ->
        val _stmt: SQLiteStatement = _connection.prepare(_sql)
        try {
          val _columnIndexOfPk: Int = getColumnIndexOrThrow(_stmt, "pk")
          val _result: MyEntity
          if (_stmt.step()) {
            val _tmpPk: Int
            _tmpPk = _stmt.getLong(_columnIndexOfPk).toInt()
            _result = MyEntity(_tmpPk)
          } else {
            error("The query result was empty, but expected a single row to return a NON-NULL object of type 'MyEntity'.")
          }
          _result
        } finally {
          _stmt.close()
        }
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
