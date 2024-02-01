import androidx.room.EntityInsertionAdapter
import androidx.room.RoomDatabase
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performReadBlocking
import androidx.sqlite.db.SupportSQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Int
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

  private val __insertionAdapterOfMyEntity: EntityInsertionAdapter<MyEntity>
  init {
    this.__db = __db
    this.__insertionAdapterOfMyEntity = object : EntityInsertionAdapter<MyEntity>(__db) {
      protected override fun createQuery(): String =
          "INSERT OR ABORT INTO `MyEntity` (`pk`,`boolean`,`nullableBoolean`) VALUES (?,?,?)"

      protected override fun bind(statement: SupportSQLiteStatement, entity: MyEntity) {
        statement.bindLong(1, entity.pk.toLong())
        val _tmp: Int = if (entity.boolean) 1 else 0
        statement.bindLong(2, _tmp.toLong())
        val _tmpNullableBoolean: Boolean? = entity.nullableBoolean
        val _tmp_1: Int? = _tmpNullableBoolean?.let { if (it) 1 else 0 }
        if (_tmp_1 == null) {
          statement.bindNull(3)
        } else {
          statement.bindLong(3, _tmp_1.toLong())
        }
      }
    }
  }

  public override fun addEntity(item: MyEntity) {
    __db.assertNotSuspendingTransaction()
    __db.beginTransaction()
    try {
      __insertionAdapterOfMyEntity.insert(item)
      __db.setTransactionSuccessful()
    } finally {
      __db.endTransaction()
    }
  }

  public override fun getEntity(): MyEntity {
    val _sql: String = "SELECT * FROM MyEntity"
    return performReadBlocking(__db, _sql) { _stmt ->
      val _cursorIndexOfPk: Int = getColumnIndexOrThrow(_stmt, "pk")
      val _cursorIndexOfBoolean: Int = getColumnIndexOrThrow(_stmt, "boolean")
      val _cursorIndexOfNullableBoolean: Int = getColumnIndexOrThrow(_stmt, "nullableBoolean")
      val _result: MyEntity
      if (_stmt.step()) {
        val _tmpPk: Int
        _tmpPk = _stmt.getLong(_cursorIndexOfPk).toInt()
        val _tmpBoolean: Boolean
        val _tmp: Int
        _tmp = _stmt.getLong(_cursorIndexOfBoolean).toInt()
        _tmpBoolean = _tmp != 0
        val _tmpNullableBoolean: Boolean?
        val _tmp_1: Int?
        if (_stmt.isNull(_cursorIndexOfNullableBoolean)) {
          _tmp_1 = null
        } else {
          _tmp_1 = _stmt.getLong(_cursorIndexOfNullableBoolean).toInt()
        }
        _tmpNullableBoolean = _tmp_1?.let { it != 0 }
        _result = MyEntity(_tmpPk,_tmpBoolean,_tmpNullableBoolean)
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
