import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performBlocking
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Byte
import kotlin.Char
import kotlin.Double
import kotlin.Float
import kotlin.Int
import kotlin.Long
import kotlin.Short
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class MyDao_Impl(
  __db: RoomDatabase,
) : MyDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfMyEntity: EntityInsertAdapter<MyEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfMyEntity = object : EntityInsertAdapter<MyEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR ABORT INTO `MyEntity` (`int`,`short`,`byte`,`long`,`char`,`float`,`double`) VALUES (?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: MyEntity) {
        val _tmpInt: Int? = entity.int
        if (_tmpInt == null) {
          statement.bindNull(1)
        } else {
          statement.bindLong(1, _tmpInt.toLong())
        }
        val _tmpShort: Short? = entity.short
        if (_tmpShort == null) {
          statement.bindNull(2)
        } else {
          statement.bindLong(2, _tmpShort.toLong())
        }
        val _tmpByte: Byte? = entity.byte
        if (_tmpByte == null) {
          statement.bindNull(3)
        } else {
          statement.bindLong(3, _tmpByte.toLong())
        }
        val _tmpLong: Long? = entity.long
        if (_tmpLong == null) {
          statement.bindNull(4)
        } else {
          statement.bindLong(4, _tmpLong)
        }
        val _tmpChar: Char? = entity.char
        if (_tmpChar == null) {
          statement.bindNull(5)
        } else {
          statement.bindLong(5, _tmpChar.toLong())
        }
        val _tmpFloat: Float? = entity.float
        if (_tmpFloat == null) {
          statement.bindNull(6)
        } else {
          statement.bindDouble(6, _tmpFloat.toDouble())
        }
        val _tmpDouble: Double? = entity.double
        if (_tmpDouble == null) {
          statement.bindNull(7)
        } else {
          statement.bindDouble(7, _tmpDouble)
        }
      }
    }
  }

  public override fun addEntity(item: MyEntity): Unit = performBlocking(__db, false, true) {
      _connection ->
    __insertAdapterOfMyEntity.insert(_connection, item)
  }

  public override fun getEntity(): MyEntity {
    val _sql: String = "SELECT * FROM MyEntity"
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _cursorIndexOfInt: Int = getColumnIndexOrThrow(_stmt, "int")
        val _cursorIndexOfShort: Int = getColumnIndexOrThrow(_stmt, "short")
        val _cursorIndexOfByte: Int = getColumnIndexOrThrow(_stmt, "byte")
        val _cursorIndexOfLong: Int = getColumnIndexOrThrow(_stmt, "long")
        val _cursorIndexOfChar: Int = getColumnIndexOrThrow(_stmt, "char")
        val _cursorIndexOfFloat: Int = getColumnIndexOrThrow(_stmt, "float")
        val _cursorIndexOfDouble: Int = getColumnIndexOrThrow(_stmt, "double")
        val _result: MyEntity
        if (_stmt.step()) {
          val _tmpInt: Int?
          if (_stmt.isNull(_cursorIndexOfInt)) {
            _tmpInt = null
          } else {
            _tmpInt = _stmt.getLong(_cursorIndexOfInt).toInt()
          }
          val _tmpShort: Short?
          if (_stmt.isNull(_cursorIndexOfShort)) {
            _tmpShort = null
          } else {
            _tmpShort = _stmt.getLong(_cursorIndexOfShort).toShort()
          }
          val _tmpByte: Byte?
          if (_stmt.isNull(_cursorIndexOfByte)) {
            _tmpByte = null
          } else {
            _tmpByte = _stmt.getLong(_cursorIndexOfByte).toByte()
          }
          val _tmpLong: Long?
          if (_stmt.isNull(_cursorIndexOfLong)) {
            _tmpLong = null
          } else {
            _tmpLong = _stmt.getLong(_cursorIndexOfLong)
          }
          val _tmpChar: Char?
          if (_stmt.isNull(_cursorIndexOfChar)) {
            _tmpChar = null
          } else {
            _tmpChar = _stmt.getLong(_cursorIndexOfChar).toChar()
          }
          val _tmpFloat: Float?
          if (_stmt.isNull(_cursorIndexOfFloat)) {
            _tmpFloat = null
          } else {
            _tmpFloat = _stmt.getDouble(_cursorIndexOfFloat).toFloat()
          }
          val _tmpDouble: Double?
          if (_stmt.isNull(_cursorIndexOfDouble)) {
            _tmpDouble = null
          } else {
            _tmpDouble = _stmt.getDouble(_cursorIndexOfDouble)
          }
          _result = MyEntity(_tmpInt,_tmpShort,_tmpByte,_tmpLong,_tmpChar,_tmpFloat,_tmpDouble)
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
