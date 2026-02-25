import androidx.room3.RoomDatabase
import androidx.room3.RoomRawQuery
import androidx.room3.coroutines.createFlow
import androidx.room3.util.getColumnIndex
import androidx.room3.util.performBlocking
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.prepare
import androidx.sqlite.step
import javax.`annotation`.processing.Generated
import kotlin.Double
import kotlin.Float
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room3.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
internal class MyDao_Impl(
  __db: RoomDatabase,
) : MyDao {
  private val __db: RoomDatabase
  init {
    this.__db = __db
  }

  public override fun getEntitySupport(sql: RoomRawQuery): MyEntity {
    val _sql: String = sql.sql
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        sql.getBindingFunction().invoke(_stmt)
        val _result: MyEntity
        if (_stmt.step()) {
          _result = __entityStatementConverter_MyEntity(_stmt)
        } else {
          error("The query result was empty, but expected a single row to return a NON-NULL object of type 'MyEntity'.")
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getNullableEntitySupport(sql: RoomRawQuery): MyEntity? {
    val _sql: String = sql.sql
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        sql.getBindingFunction().invoke(_stmt)
        val _result: MyEntity?
        if (_stmt.step()) {
          _result = __entityStatementConverter_MyEntity(_stmt)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getEntitySupportFlow(sql: RoomRawQuery): Flow<MyEntity> {
    val _sql: String = sql.sql
    return createFlow(__db, false, arrayOf("MyEntity")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        sql.getBindingFunction().invoke(_stmt)
        val _result: MyEntity
        if (_stmt.step()) {
          _result = __entityStatementConverter_MyEntity(_stmt)
        } else {
          error("The query result was empty, but expected a single row to return a NON-NULL object of type 'MyEntity'.")
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  private fun __entityStatementConverter_MyEntity(statement: SQLiteStatement): MyEntity {
    val _entity: MyEntity
    val _columnIndexOfPk: Int = getColumnIndex(statement, "pk")
    val _columnIndexOfDoubleColumn: Int = getColumnIndex(statement, "doubleColumn")
    val _columnIndexOfFloatColumn: Int = getColumnIndex(statement, "floatColumn")
    val _tmpPk: Long
    if (_columnIndexOfPk == -1) {
      _tmpPk = 0
    } else {
      _tmpPk = statement.getLong(_columnIndexOfPk)
    }
    val _tmpDoubleColumn: Double
    if (_columnIndexOfDoubleColumn == -1) {
      _tmpDoubleColumn = 0.0
    } else {
      _tmpDoubleColumn = statement.getDouble(_columnIndexOfDoubleColumn)
    }
    val _tmpFloatColumn: Float
    if (_columnIndexOfFloatColumn == -1) {
      _tmpFloatColumn = 0f
    } else {
      _tmpFloatColumn = statement.getDouble(_columnIndexOfFloatColumn).toFloat()
    }
    _entity = MyEntity(_tmpPk,_tmpDoubleColumn,_tmpFloatColumn)
    return _entity
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
