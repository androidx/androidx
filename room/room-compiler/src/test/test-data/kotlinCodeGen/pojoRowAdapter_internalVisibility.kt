import androidx.room.EntityInsertionAdapter
import androidx.room.RoomDatabase
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performReadBlocking
import androidx.sqlite.db.SupportSQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.Long
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
          "INSERT OR ABORT INTO `MyEntity` (`pk`,`internalVal`,`internalVar`,`internalSetterVar`) VALUES (?,?,?,?)"

      protected override fun bind(statement: SupportSQLiteStatement, entity: MyEntity) {
        statement.bindLong(1, entity.pk.toLong())
        statement.bindLong(2, entity.internalVal)
        statement.bindLong(3, entity.internalVar)
        statement.bindLong(4, entity.internalSetterVar)
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
      val _cursorIndexOfInternalVal: Int = getColumnIndexOrThrow(_stmt, "internalVal")
      val _cursorIndexOfInternalVar: Int = getColumnIndexOrThrow(_stmt, "internalVar")
      val _cursorIndexOfInternalSetterVar: Int = getColumnIndexOrThrow(_stmt, "internalSetterVar")
      val _result: MyEntity
      if (_stmt.step()) {
        val _tmpPk: Int
        _tmpPk = _stmt.getLong(_cursorIndexOfPk).toInt()
        val _tmpInternalVal: Long
        _tmpInternalVal = _stmt.getLong(_cursorIndexOfInternalVal)
        _result = MyEntity(_tmpPk,_tmpInternalVal)
        _result.internalVar = _stmt.getLong(_cursorIndexOfInternalVar)
        _result.internalSetterVar = _stmt.getLong(_cursorIndexOfInternalSetterVar)
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
