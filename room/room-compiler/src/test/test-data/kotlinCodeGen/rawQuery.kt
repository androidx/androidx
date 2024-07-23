import android.database.Cursor
import androidx.room.CoroutinesRoom
import androidx.room.RoomDatabase
import androidx.room.RoomRawQuery
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndex
import androidx.room.util.performBlocking
import androidx.room.util.query
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.db.SupportSQLiteQuery
import java.util.concurrent.Callable
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

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class MyDao_Impl(
  __db: RoomDatabase,
) : MyDao {
  private val __db: RoomDatabase
  init {
    this.__db = __db
  }

  public override fun getEntitySupport(sql: SupportSQLiteQuery): MyEntity {
    __db.assertNotSuspendingTransaction()
    val _cursor: Cursor = query(__db, sql, false, null)
    try {
      val _result: MyEntity
      if (_cursor.moveToFirst()) {
        _result = __entityCursorConverter_MyEntity(_cursor)
      } else {
        error("The query result was empty, but expected a single row to return a NON-NULL object of type <MyEntity>.")
      }
      return _result
    } finally {
      _cursor.close()
    }
  }

  public override fun getNullableEntitySupport(sql: SupportSQLiteQuery): MyEntity? {
    __db.assertNotSuspendingTransaction()
    val _cursor: Cursor = query(__db, sql, false, null)
    try {
      val _result: MyEntity?
      if (_cursor.moveToFirst()) {
        _result = __entityCursorConverter_MyEntity(_cursor)
      } else {
        _result = null
      }
      return _result
    } finally {
      _cursor.close()
    }
  }

  public override fun getEntitySupportFlow(sql: SupportSQLiteQuery): Flow<MyEntity> =
      CoroutinesRoom.createFlow(__db, false, arrayOf("MyEntity"), object : Callable<MyEntity> {
    public override fun call(): MyEntity {
      val _cursor: Cursor = query(__db, sql, false, null)
      try {
        val _result: MyEntity
        if (_cursor.moveToFirst()) {
          _result = __entityCursorConverter_MyEntity(_cursor)
        } else {
          error("The query result was empty, but expected a single row to return a NON-NULL object of type <MyEntity>.")
        }
        return _result
      } finally {
        _cursor.close()
      }
    }
  })

  public override fun getEntity(query: RoomRawQuery): MyEntity {
    val _sql: String = query.sql
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        query.getBindingFunction().invoke(_stmt)
        val _result: MyEntity
        if (_stmt.step()) {
          _result = __entityStatementConverter_MyEntity(_stmt)
        } else {
          error("The query result was empty, but expected a single row to return a NON-NULL object of type <MyEntity>.")
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getNullableEntity(query: RoomRawQuery): MyEntity? {
    val _sql: String = query.sql
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        query.getBindingFunction().invoke(_stmt)
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

  public override fun getEntityFlow(query: RoomRawQuery): Flow<MyEntity> {
    val _sql: String = query.sql
    return createFlow(__db, false, arrayOf("MyEntity")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        query.getBindingFunction().invoke(_stmt)
        val _result: MyEntity
        if (_stmt.step()) {
          _result = __entityStatementConverter_MyEntity(_stmt)
        } else {
          error("The query result was empty, but expected a single row to return a NON-NULL object of type <MyEntity>.")
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  private fun __entityCursorConverter_MyEntity(cursor: Cursor): MyEntity {
    val _entity: MyEntity
    val _cursorIndexOfPk: Int = getColumnIndex(cursor, "pk")
    val _cursorIndexOfDoubleColumn: Int = getColumnIndex(cursor, "doubleColumn")
    val _cursorIndexOfFloatColumn: Int = getColumnIndex(cursor, "floatColumn")
    val _tmpPk: Long
    if (_cursorIndexOfPk == -1) {
      _tmpPk = 0
    } else {
      _tmpPk = cursor.getLong(_cursorIndexOfPk)
    }
    val _tmpDoubleColumn: Double
    if (_cursorIndexOfDoubleColumn == -1) {
      _tmpDoubleColumn = 0.0
    } else {
      _tmpDoubleColumn = cursor.getDouble(_cursorIndexOfDoubleColumn)
    }
    val _tmpFloatColumn: Float
    if (_cursorIndexOfFloatColumn == -1) {
      _tmpFloatColumn = 0f
    } else {
      _tmpFloatColumn = cursor.getFloat(_cursorIndexOfFloatColumn)
    }
    _entity = MyEntity(_tmpPk,_tmpDoubleColumn,_tmpFloatColumn)
    return _entity
  }

  private fun __entityStatementConverter_MyEntity(statement: SQLiteStatement): MyEntity {
    val _entity: MyEntity
    val _cursorIndexOfPk: Int = getColumnIndex(statement, "pk")
    val _cursorIndexOfDoubleColumn: Int = getColumnIndex(statement, "doubleColumn")
    val _cursorIndexOfFloatColumn: Int = getColumnIndex(statement, "floatColumn")
    val _tmpPk: Long
    if (_cursorIndexOfPk == -1) {
      _tmpPk = 0
    } else {
      _tmpPk = statement.getLong(_cursorIndexOfPk)
    }
    val _tmpDoubleColumn: Double
    if (_cursorIndexOfDoubleColumn == -1) {
      _tmpDoubleColumn = 0.0
    } else {
      _tmpDoubleColumn = statement.getDouble(_cursorIndexOfDoubleColumn)
    }
    val _tmpFloatColumn: Float
    if (_cursorIndexOfFloatColumn == -1) {
      _tmpFloatColumn = 0f
    } else {
      _tmpFloatColumn = statement.getDouble(_cursorIndexOfFloatColumn).toFloat()
    }
    _entity = MyEntity(_tmpPk,_tmpDoubleColumn,_tmpFloatColumn)
    return _entity
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
