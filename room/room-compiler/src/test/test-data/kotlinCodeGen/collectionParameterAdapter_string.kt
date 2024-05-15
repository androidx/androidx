import androidx.room.RoomDatabase
import androidx.room.util.appendPlaceholders
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performBlocking
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Set
import kotlin.reflect.KClass
import kotlin.text.StringBuilder

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class MyDao_Impl(
  __db: RoomDatabase,
) : MyDao {
  private val __db: RoomDatabase
  init {
    this.__db = __db
  }

  public override fun listOfString(arg: List<String>): MyEntity {
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("SELECT * FROM MyEntity WHERE string IN (")
    val _inputSize: Int = arg.size
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        for (_item: String in arg) {
          _stmt.bindText(_argIndex, _item)
          _argIndex++
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

  public override fun nullableListOfString(arg: List<String>?): MyEntity {
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("SELECT * FROM MyEntity WHERE string IN (")
    val _inputSize: Int = if (arg == null) 1 else arg.size
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        if (arg == null) {
          _stmt.bindNull(_argIndex)
        } else {
          for (_item: String in arg) {
            _stmt.bindText(_argIndex, _item)
            _argIndex++
          }
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

  public override fun listOfNullableString(arg: List<String?>): MyEntity {
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("SELECT * FROM MyEntity WHERE string IN (")
    val _inputSize: Int = arg.size
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        for (_item: String? in arg) {
          if (_item == null) {
            _stmt.bindNull(_argIndex)
          } else {
            _stmt.bindText(_argIndex, _item)
          }
          _argIndex++
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

  public override fun setOfString(arg: Set<String>): MyEntity {
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("SELECT * FROM MyEntity WHERE string IN (")
    val _inputSize: Int = arg.size
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        for (_item: String in arg) {
          _stmt.bindText(_argIndex, _item)
          _argIndex++
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
