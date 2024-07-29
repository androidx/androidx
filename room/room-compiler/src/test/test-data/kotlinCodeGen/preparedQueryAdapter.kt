import androidx.room.RoomDatabase
import androidx.room.util.appendPlaceholders
import androidx.room.util.getLastInsertedRowId
import androidx.room.util.getTotalChangedRows
import androidx.room.util.performBlocking
import androidx.sqlite.SQLiteStatement
import java.lang.Void
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
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

  public override fun insertEntity(id: Long) {
    val _sql: String = "INSERT INTO MyEntity (id) VALUES (?)"
    return performBlocking(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun insertEntityReturnLong(id: Long): Long {
    val _sql: String = "INSERT INTO MyEntity (id) VALUES (?)"
    return performBlocking(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        _stmt.step()
        getLastInsertedRowId(_connection)
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun insertEntityReturnVoid(id: Long): Void? {
    val _sql: String = "INSERT INTO MyEntity (id) VALUES (?)"
    return performBlocking(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        _stmt.step()
        null
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun updateEntity(text: String) {
    val _sql: String = "UPDATE MyEntity SET text = ?"
    return performBlocking(__db, false, true) { _connection ->
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

  public override fun updateEntityReturnInt(id: Long, text: String): Int {
    val _sql: String = "UPDATE MyEntity SET text = ? WHERE id = ?"
    return performBlocking(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, text)
        _argIndex = 2
        _stmt.bindLong(_argIndex, id)
        _stmt.step()
        getTotalChangedRows(_connection)
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun deleteEntity() {
    val _sql: String = "DELETE FROM MyEntity"
    return performBlocking(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun deleteEntityReturnInt(): Int {
    val _sql: String = "DELETE FROM MyEntity"
    return performBlocking(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
        getTotalChangedRows(_connection)
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun deleteEntitiesIn(ids: List<Long>) {
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("DELETE FROM MyEntity WHERE id IN (")
    val _inputSize: Int = ids.size
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    return performBlocking(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        for (_item: Long in ids) {
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
