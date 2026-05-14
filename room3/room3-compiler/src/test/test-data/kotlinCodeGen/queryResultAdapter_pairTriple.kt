import androidx.room3.RoomDatabase
import androidx.room3.util.performBlocking
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Int
import kotlin.Pair
import kotlin.String
import kotlin.Suppress
import kotlin.Triple
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room3.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL", "MemberExtensionConflict"])
internal class MyDao_Impl(
  __db: RoomDatabase,
) : MyDao {
  private val __db: RoomDatabase
  init {
    this.__db = __db
  }

  public override fun getPair(): Pair<String, Int> {
    val _sql: String = "SELECT 'Tom', 1 FROM MyEntity LIMIT 1"
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: Pair<String, Int>
        if (_stmt.step()) {
          val _value0: String
          _value0 = _stmt.getText(0)
          val _value1: Int
          _value1 = _stmt.getLong(1).toInt()
          _result = Pair(_value0, _value1)
        } else {
          error("The query result was empty, but expected a single row to return a NON-NULL object of type 'kotlin.Pair<kotlin.String, kotlin.Int>'.")
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getPairNullable(): Pair<String, Int>? {
    val _sql: String = "SELECT 'Tom', 1 FROM MyEntity LIMIT 1"
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: Pair<String, Int>?
        if (_stmt.step()) {
          val _value0: String
          _value0 = _stmt.getText(0)
          val _value1: Int
          _value1 = _stmt.getLong(1).toInt()
          _result = Pair(_value0, _value1)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getPairList(): List<Pair<String, Int>> {
    val _sql: String = "SELECT 'Tom', 1 FROM MyEntity"
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: MutableList<Pair<String, Int>> = mutableListOf()
        while (_stmt.step()) {
          val _item: Pair<String, Int>
          val _value0: String
          _value0 = _stmt.getText(0)
          val _value1: Int
          _value1 = _stmt.getLong(1).toInt()
          _item = Pair(_value0, _value1)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getPairListNullableTypeArgs(): List<Pair<String?, Int>> {
    val _sql: String = "SELECT 'Tom', 1 FROM MyEntity"
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: MutableList<Pair<String?, Int>> = mutableListOf()
        while (_stmt.step()) {
          val _item: Pair<String?, Int>
          val _value0: String?
          if (_stmt.isNull(0)) {
            _value0 = null
          } else {
            _value0 = _stmt.getText(0)
          }
          val _value1: Int
          _value1 = _stmt.getLong(1).toInt()
          _item = Pair(_value0, _value1)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getTriple(): Triple<String, Int, Boolean> {
    val _sql: String = "SELECT 'Tom', 1, 0 FROM MyEntity LIMIT 1"
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: Triple<String, Int, Boolean>
        if (_stmt.step()) {
          val _value0: String
          _value0 = _stmt.getText(0)
          val _value1: Int
          _value1 = _stmt.getLong(1).toInt()
          val _value2: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(2).toInt()
          _value2 = _tmp != 0
          _result = Triple(_value0, _value1, _value2)
        } else {
          error("The query result was empty, but expected a single row to return a NON-NULL object of type 'kotlin.Triple<kotlin.String, kotlin.Int, kotlin.Boolean>'.")
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
