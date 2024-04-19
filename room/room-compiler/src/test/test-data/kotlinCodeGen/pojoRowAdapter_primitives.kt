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
        statement.bindLong(1, entity.int.toLong())
        statement.bindLong(2, entity.short.toLong())
        statement.bindLong(3, entity.byte.toLong())
        statement.bindLong(4, entity.long)
        statement.bindLong(5, entity.char.toLong())
        statement.bindDouble(6, entity.float.toDouble())
        statement.bindDouble(7, entity.double)
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
          val _tmpInt: Int
          _tmpInt = _stmt.getLong(_cursorIndexOfInt).toInt()
          val _tmpShort: Short
          _tmpShort = _stmt.getLong(_cursorIndexOfShort).toShort()
          val _tmpByte: Byte
          _tmpByte = _stmt.getLong(_cursorIndexOfByte).toByte()
          val _tmpLong: Long
          _tmpLong = _stmt.getLong(_cursorIndexOfLong)
          val _tmpChar: Char
          _tmpChar = _stmt.getLong(_cursorIndexOfChar).toChar()
          val _tmpFloat: Float
          _tmpFloat = _stmt.getDouble(_cursorIndexOfFloat).toFloat()
          val _tmpDouble: Double
          _tmpDouble = _stmt.getDouble(_cursorIndexOfDouble)
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
