import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.appendPlaceholders
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.getLastInsertedRowId
import androidx.room.util.getTotalChangedRows
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlin.text.StringBuilder
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

  public override fun getFlow(vararg arg: String?): Flow<MyEntity> {
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("SELECT * FROM MyEntity WHERE pk IN (")
    val _inputSize: Int = arg.size
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    return createFlow(__db, false, arrayOf("MyEntity")) { _connection ->
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
        val _cursorIndexOfPk: Int = getColumnIndexOrThrow(_stmt, "pk")
        val _cursorIndexOfOther: Int = getColumnIndexOrThrow(_stmt, "other")
        val _result: MyEntity
        if (_stmt.step()) {
          val _tmpPk: Int
          _tmpPk = _stmt.getLong(_cursorIndexOfPk).toInt()
          val _tmpOther: String
          _tmpOther = _stmt.getText(_cursorIndexOfOther)
          _result = MyEntity(_tmpPk,_tmpOther)
        } else {
          error("The query result was empty, but expected a single row to return a NON-NULL object of type <MyEntity>.")
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getFlowNullable(vararg arg: String?): Flow<MyEntity?> {
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("SELECT * FROM MyEntity WHERE pk IN (")
    val _inputSize: Int = arg.size
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    return createFlow(__db, false, arrayOf("MyEntity")) { _connection ->
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
        val _cursorIndexOfPk: Int = getColumnIndexOrThrow(_stmt, "pk")
        val _cursorIndexOfOther: Int = getColumnIndexOrThrow(_stmt, "other")
        val _result: MyEntity?
        if (_stmt.step()) {
          val _tmpPk: Int
          _tmpPk = _stmt.getLong(_cursorIndexOfPk).toInt()
          val _tmpOther: String
          _tmpOther = _stmt.getText(_cursorIndexOfOther)
          _result = MyEntity(_tmpPk,_tmpOther)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getSuspendList(vararg arg: String?): List<MyEntity> {
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("SELECT * FROM MyEntity WHERE pk IN (")
    val _inputSize: Int = arg.size
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    return performSuspending(__db, true, false) { _connection ->
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
        val _cursorIndexOfPk: Int = getColumnIndexOrThrow(_stmt, "pk")
        val _cursorIndexOfOther: Int = getColumnIndexOrThrow(_stmt, "other")
        val _result: MutableList<MyEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item_1: MyEntity
          val _tmpPk: Int
          _tmpPk = _stmt.getLong(_cursorIndexOfPk).toInt()
          val _tmpOther: String
          _tmpOther = _stmt.getText(_cursorIndexOfOther)
          _item_1 = MyEntity(_tmpPk,_tmpOther)
          _result.add(_item_1)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getCount(): Int {
    val _sql: String = "SELECT count(*) FROM MyEntity"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: Int
        if (_stmt.step()) {
          val _tmp: Int
          _tmp = _stmt.getLong(0).toInt()
          _result = _tmp
        } else {
          _result = 0
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun insertEntity(pk: Long) {
    val _sql: String = "INSERT INTO MyEntity (pk) VALUES (?)"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, pk)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun insertEntityReturnLong(pk: Long): Long {
    val _sql: String = "INSERT INTO MyEntity (pk) VALUES (?)"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, pk)
        _stmt.step()
        getLastInsertedRowId(_connection)
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun updateEntity(text: String) {
    val _sql: String = "UPDATE MyEntity SET other = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, text)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun updateEntityReturnInt(pk: Long, text: String): Int {
    val _sql: String = "UPDATE MyEntity SET other = ? WHERE pk = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, text)
        _argIndex = 2
        _stmt.bindLong(_argIndex, pk)
        _stmt.step()
        getTotalChangedRows(_connection)
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteEntity() {
    val _sql: String = "DELETE FROM MyEntity"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteEntityReturnInt(): Int {
    val _sql: String = "DELETE FROM MyEntity"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
        getTotalChangedRows(_connection)
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteEntitiesIn(pks: List<Long>) {
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("DELETE FROM MyEntity WHERE pk IN (")
    val _inputSize: Int = pks.size
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        for (_item: Long in pks) {
          _stmt.bindLong(_argIndex, _item)
          _argIndex++
        }
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
