import androidx.room.RoomDatabase
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performBlocking
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class MyDao_Impl(
  __db: RoomDatabase,
) : MyDao {
  private val __db: RoomDatabase
  init {
    this.__db = __db
  }

  public override fun stringParam(arg: String): MyEntity {
    val _sql: String = "SELECT * FROM MyEntity WHERE string = ?"
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, arg)
        val _cursorIndexOfString: Int = getColumnIndexOrThrow(_stmt, "string")
        val _result: MyEntity
        if (_stmt.step()) {
          val _tmpString: String
          _tmpString = _stmt.getText(_cursorIndexOfString)
          _result = MyEntity(_tmpString)
        } else {
          error("The query result was empty, but expected a single row to return a NON-NULL object of type <MyEntity>.")
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun nullableStringParam(arg: String?): MyEntity {
    val _sql: String = "SELECT * FROM MyEntity WHERE string = ?"
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        if (arg == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, arg)
        }
        val _cursorIndexOfString: Int = getColumnIndexOrThrow(_stmt, "string")
        val _result: MyEntity
        if (_stmt.step()) {
          val _tmpString: String
          _tmpString = _stmt.getText(_cursorIndexOfString)
          _result = MyEntity(_tmpString)
        } else {
          error("The query result was empty, but expected a single row to return a NON-NULL object of type <MyEntity>.")
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
