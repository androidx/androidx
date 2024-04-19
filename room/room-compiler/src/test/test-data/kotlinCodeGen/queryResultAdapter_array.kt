import androidx.room.RoomDatabase
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performBlocking
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Array
import kotlin.Int
import kotlin.Long
import kotlin.LongArray
import kotlin.Short
import kotlin.ShortArray
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
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

  public override fun queryOfArray(): Array<MyEntity> {
    val _sql: String = "SELECT * FROM MyEntity"
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _cursorIndexOfPk: Int = getColumnIndexOrThrow(_stmt, "pk")
        val _cursorIndexOfOther: Int = getColumnIndexOrThrow(_stmt, "other")
        val _cursorIndexOfOther2: Int = getColumnIndexOrThrow(_stmt, "other2")
        val _listResult: MutableList<MyEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: MyEntity
          val _tmpPk: Int
          _tmpPk = _stmt.getLong(_cursorIndexOfPk).toInt()
          val _tmpOther: String
          _tmpOther = _stmt.getText(_cursorIndexOfOther)
          val _tmpOther2: Long
          _tmpOther2 = _stmt.getLong(_cursorIndexOfOther2)
          _item = MyEntity(_tmpPk,_tmpOther,_tmpOther2)
          _listResult.add(_item)
        }
        val _result: Array<MyEntity> = _listResult.toTypedArray()
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun queryOfNullableArray(): Array<MyEntity?> {
    val _sql: String = "SELECT * FROM MyEntity"
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _cursorIndexOfPk: Int = getColumnIndexOrThrow(_stmt, "pk")
        val _cursorIndexOfOther: Int = getColumnIndexOrThrow(_stmt, "other")
        val _cursorIndexOfOther2: Int = getColumnIndexOrThrow(_stmt, "other2")
        val _listResult: MutableList<MyEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: MyEntity
          val _tmpPk: Int
          _tmpPk = _stmt.getLong(_cursorIndexOfPk).toInt()
          val _tmpOther: String
          _tmpOther = _stmt.getText(_cursorIndexOfOther)
          val _tmpOther2: Long
          _tmpOther2 = _stmt.getLong(_cursorIndexOfOther2)
          _item = MyEntity(_tmpPk,_tmpOther,_tmpOther2)
          _listResult.add(_item)
        }
        val _result: Array<MyEntity?> = _listResult.toTypedArray()
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun queryOfArrayWithLong(): Array<Long> {
    val _sql: String = "SELECT pk FROM MyEntity"
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _listResult: MutableList<Long> = mutableListOf()
        while (_stmt.step()) {
          val _item: Long
          _item = _stmt.getLong(0)
          _listResult.add(_item)
        }
        val _result: Array<Long> = _listResult.toTypedArray()
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun queryOfArrayWithNullableLong(): Array<Long?> {
    val _sql: String = "SELECT pk FROM MyEntity"
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _listResult: MutableList<Long> = mutableListOf()
        while (_stmt.step()) {
          val _item: Long
          _item = _stmt.getLong(0)
          _listResult.add(_item)
        }
        val _result: Array<Long?> = _listResult.toTypedArray()
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun queryOfLongArray(): LongArray {
    val _sql: String = "SELECT pk FROM MyEntity"
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _listResult: MutableList<Long> = mutableListOf()
        while (_stmt.step()) {
          val _item: Long
          _item = _stmt.getLong(0)
          _listResult.add(_item)
        }
        val _result: LongArray = _listResult.toLongArray()
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun queryOfShortArray(): ShortArray {
    val _sql: String = "SELECT pk FROM MyEntity"
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _listResult: MutableList<Short> = mutableListOf()
        while (_stmt.step()) {
          val _item: Short
          _item = _stmt.getLong(0).toShort()
          _listResult.add(_item)
        }
        val _result: ShortArray = _listResult.toShortArray()
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
