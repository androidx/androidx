import androidx.room.EntityInsertionAdapter
import androidx.room.RoomDatabase
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performBlocking
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.db.SupportSQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.IllegalArgumentException
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION"])
public class MyDao_Impl(
  __db: RoomDatabase,
) : MyDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfMyEntity: EntityInsertionAdapter<MyEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfMyEntity = object : EntityInsertionAdapter<MyEntity>(__db) {
      protected override fun createQuery(): String =
          "INSERT OR ABORT INTO `MyEntity` (`pk`,`enum`,`nullableEnum`) VALUES (?,?,?)"

      protected override fun bind(statement: SupportSQLiteStatement, entity: MyEntity) {
        statement.bindLong(1, entity.pk.toLong())
        statement.bindString(2, __Fruit_enumToString(entity.enum))
        val _tmpNullableEnum: Fruit? = entity.nullableEnum
        if (_tmpNullableEnum == null) {
          statement.bindNull(3)
        } else {
          statement.bindString(3, __Fruit_enumToString(_tmpNullableEnum))
        }
      }
    }
  }

  public override fun addEntity(item: MyEntity) {
    __db.assertNotSuspendingTransaction()
    __db.beginTransaction()
    try {
      __insertAdapterOfMyEntity.insert(item)
      __db.setTransactionSuccessful()
    } finally {
      __db.endTransaction()
    }
  }

  public override fun getEntity(): MyEntity {
    val _sql: String = "SELECT * FROM MyEntity"
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _cursorIndexOfPk: Int = getColumnIndexOrThrow(_stmt, "pk")
        val _cursorIndexOfEnum: Int = getColumnIndexOrThrow(_stmt, "enum")
        val _cursorIndexOfNullableEnum: Int = getColumnIndexOrThrow(_stmt, "nullableEnum")
        val _result: MyEntity
        if (_stmt.step()) {
          val _tmpPk: Int
          _tmpPk = _stmt.getLong(_cursorIndexOfPk).toInt()
          val _tmpEnum: Fruit
          _tmpEnum = __Fruit_stringToEnum(_stmt.getText(_cursorIndexOfEnum))
          val _tmpNullableEnum: Fruit?
          if (_stmt.isNull(_cursorIndexOfNullableEnum)) {
            _tmpNullableEnum = null
          } else {
            _tmpNullableEnum = __Fruit_stringToEnum(_stmt.getText(_cursorIndexOfNullableEnum))
          }
          _result = MyEntity(_tmpPk,_tmpEnum,_tmpNullableEnum)
        } else {
          error("The query result was empty, but expected a single row to return a NON-NULL object of type <MyEntity>.")
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  private fun __Fruit_enumToString(_value: Fruit): String = when (_value) {
    Fruit.APPLE -> "APPLE"
    Fruit.BANANA -> "BANANA"
  }

  private fun __Fruit_stringToEnum(_value: String): Fruit = when (_value) {
    "APPLE" -> Fruit.APPLE
    "BANANA" -> Fruit.BANANA
    else -> throw IllegalArgumentException("Can't convert value to enum, unknown value: " + _value)
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
