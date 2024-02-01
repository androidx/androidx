import androidx.room.RoomDatabase
import androidx.room.util.appendPlaceholders
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.newStringBuilder
import androidx.room.util.performReadBlocking
import java.lang.StringBuilder
import javax.`annotation`.processing.Generated
import kotlin.Array
import kotlin.Int
import kotlin.IntArray
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.jvm.JvmStatic
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION"])
public class MyDao_Impl(
  __db: RoomDatabase,
) : MyDao {
  private val __db: RoomDatabase
  init {
    this.__db = __db
  }

  public override fun arrayOfString(arg: Array<String>): MyEntity {
    val _stringBuilder: StringBuilder = newStringBuilder()
    _stringBuilder.append("SELECT * FROM MyEntity WHERE id IN (")
    val _inputSize: Int = arg.size
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    return performReadBlocking(__db, _sql) { _stmt ->
      var _argIndex: Int = 1
      for (_item: String in arg) {
        _stmt.bindText(_argIndex, _item)
        _argIndex++
      }
      val _cursorIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
      val _result: MyEntity
      if (_stmt.step()) {
        val _tmpId: String
        _tmpId = _stmt.getText(_cursorIndexOfId)
        _result = MyEntity(_tmpId)
      } else {
        error("The query result was empty, but expected a single row to return a NON-NULL object of type <MyEntity>.")
      }
      _result
    }
  }

  public override fun nullableArrayOfString(arg: Array<String>?): MyEntity {
    val _stringBuilder: StringBuilder = newStringBuilder()
    _stringBuilder.append("SELECT * FROM MyEntity WHERE id IN (")
    val _inputSize: Int = if (arg == null) 1 else arg.size
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    return performReadBlocking(__db, _sql) { _stmt ->
      var _argIndex: Int = 1
      if (arg == null) {
        _stmt.bindNull(_argIndex)
      } else {
        for (_item: String in arg) {
          _stmt.bindText(_argIndex, _item)
          _argIndex++
        }
      }
      val _cursorIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
      val _result: MyEntity
      if (_stmt.step()) {
        val _tmpId: String
        _tmpId = _stmt.getText(_cursorIndexOfId)
        _result = MyEntity(_tmpId)
      } else {
        error("The query result was empty, but expected a single row to return a NON-NULL object of type <MyEntity>.")
      }
      _result
    }
  }

  public override fun arrayOfNullableString(arg: Array<String?>): MyEntity {
    val _stringBuilder: StringBuilder = newStringBuilder()
    _stringBuilder.append("SELECT * FROM MyEntity WHERE id IN (")
    val _inputSize: Int = arg.size
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    return performReadBlocking(__db, _sql) { _stmt ->
      var _argIndex: Int = 1
      for (_item: String? in arg) {
        if (_item == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, _item)
        }
        _argIndex++
      }
      val _cursorIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
      val _result: MyEntity
      if (_stmt.step()) {
        val _tmpId: String
        _tmpId = _stmt.getText(_cursorIndexOfId)
        _result = MyEntity(_tmpId)
      } else {
        error("The query result was empty, but expected a single row to return a NON-NULL object of type <MyEntity>.")
      }
      _result
    }
  }

  public override fun varargOfString(vararg arg: String): MyEntity {
    val _stringBuilder: StringBuilder = newStringBuilder()
    _stringBuilder.append("SELECT * FROM MyEntity WHERE id IN (")
    val _inputSize: Int = arg.size
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    return performReadBlocking(__db, _sql) { _stmt ->
      var _argIndex: Int = 1
      for (_item: String in arg) {
        _stmt.bindText(_argIndex, _item)
        _argIndex++
      }
      val _cursorIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
      val _result: MyEntity
      if (_stmt.step()) {
        val _tmpId: String
        _tmpId = _stmt.getText(_cursorIndexOfId)
        _result = MyEntity(_tmpId)
      } else {
        error("The query result was empty, but expected a single row to return a NON-NULL object of type <MyEntity>.")
      }
      _result
    }
  }

  public override fun varargOfNullableString(vararg arg: String?): MyEntity {
    val _stringBuilder: StringBuilder = newStringBuilder()
    _stringBuilder.append("SELECT * FROM MyEntity WHERE id IN (")
    val _inputSize: Int = arg.size
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    return performReadBlocking(__db, _sql) { _stmt ->
      var _argIndex: Int = 1
      for (_item: String? in arg) {
        if (_item == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, _item)
        }
        _argIndex++
      }
      val _cursorIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
      val _result: MyEntity
      if (_stmt.step()) {
        val _tmpId: String
        _tmpId = _stmt.getText(_cursorIndexOfId)
        _result = MyEntity(_tmpId)
      } else {
        error("The query result was empty, but expected a single row to return a NON-NULL object of type <MyEntity>.")
      }
      _result
    }
  }

  public override fun primitiveIntArray(arg: IntArray): MyEntity {
    val _stringBuilder: StringBuilder = newStringBuilder()
    _stringBuilder.append("SELECT * FROM MyEntity WHERE id IN (")
    val _inputSize: Int = arg.size
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    return performReadBlocking(__db, _sql) { _stmt ->
      var _argIndex: Int = 1
      for (_item: Int in arg) {
        _stmt.bindLong(_argIndex, _item.toLong())
        _argIndex++
      }
      val _cursorIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
      val _result: MyEntity
      if (_stmt.step()) {
        val _tmpId: String
        _tmpId = _stmt.getText(_cursorIndexOfId)
        _result = MyEntity(_tmpId)
      } else {
        error("The query result was empty, but expected a single row to return a NON-NULL object of type <MyEntity>.")
      }
      _result
    }
  }

  public override fun nullablePrimitiveIntArray(arg: IntArray?): MyEntity {
    val _stringBuilder: StringBuilder = newStringBuilder()
    _stringBuilder.append("SELECT * FROM MyEntity WHERE id IN (")
    val _inputSize: Int = if (arg == null) 1 else arg.size
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    return performReadBlocking(__db, _sql) { _stmt ->
      var _argIndex: Int = 1
      if (arg == null) {
        _stmt.bindNull(_argIndex)
      } else {
        for (_item: Int in arg) {
          _stmt.bindLong(_argIndex, _item.toLong())
          _argIndex++
        }
      }
      val _cursorIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
      val _result: MyEntity
      if (_stmt.step()) {
        val _tmpId: String
        _tmpId = _stmt.getText(_cursorIndexOfId)
        _result = MyEntity(_tmpId)
      } else {
        error("The query result was empty, but expected a single row to return a NON-NULL object of type <MyEntity>.")
      }
      _result
    }
  }

  public companion object {
    @JvmStatic
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
