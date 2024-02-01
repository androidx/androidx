import androidx.room.EntityInsertionAdapter
import androidx.room.RoomDatabase
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performReadBlocking
import androidx.sqlite.db.SupportSQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.ByteArray
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
          "INSERT OR ABORT INTO `MyEntity` (`pk`,`byteArray`,`nullableByteArray`) VALUES (?,?,?)"

      protected override fun bind(statement: SupportSQLiteStatement, entity: MyEntity) {
        statement.bindLong(1, entity.pk.toLong())
        statement.bindBlob(2, entity.byteArray)
        val _tmpNullableByteArray: ByteArray? = entity.nullableByteArray
        if (_tmpNullableByteArray == null) {
          statement.bindNull(3)
        } else {
          statement.bindBlob(3, _tmpNullableByteArray)
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
      val _cursorIndexOfByteArray: Int = getColumnIndexOrThrow(_stmt, "byteArray")
      val _cursorIndexOfNullableByteArray: Int = getColumnIndexOrThrow(_stmt, "nullableByteArray")
      val _result: MyEntity
      if (_stmt.step()) {
        val _tmpPk: Int
        _tmpPk = _stmt.getLong(_cursorIndexOfPk).toInt()
        val _tmpByteArray: ByteArray
        _tmpByteArray = _stmt.getBlob(_cursorIndexOfByteArray)
        val _tmpNullableByteArray: ByteArray?
        if (_stmt.isNull(_cursorIndexOfNullableByteArray)) {
          _tmpNullableByteArray = null
        } else {
          _tmpNullableByteArray = _stmt.getBlob(_cursorIndexOfNullableByteArray)
        }
        _result = MyEntity(_tmpPk,_tmpByteArray,_tmpNullableByteArray)
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
