import androidx.room.EntityInsertionAdapter
import androidx.room.RoomDatabase
import androidx.room.util.convertByteToUUID
import androidx.room.util.convertUUIDToByte
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performReadBlocking
import androidx.sqlite.db.SupportSQLiteStatement
import java.util.UUID
import javax.`annotation`.processing.Generated
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
          "INSERT OR ABORT INTO `MyEntity` (`pk`,`uuid`,`nullableUuid`) VALUES (?,?,?)"

      protected override fun bind(statement: SupportSQLiteStatement, entity: MyEntity) {
        statement.bindLong(1, entity.pk.toLong())
        statement.bindBlob(2, convertUUIDToByte(entity.uuid))
        val _tmpNullableUuid: UUID? = entity.nullableUuid
        if (_tmpNullableUuid == null) {
          statement.bindNull(3)
        } else {
          statement.bindBlob(3, convertUUIDToByte(_tmpNullableUuid))
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
      val _cursorIndexOfUuid: Int = getColumnIndexOrThrow(_stmt, "uuid")
      val _cursorIndexOfNullableUuid: Int = getColumnIndexOrThrow(_stmt, "nullableUuid")
      val _result: MyEntity
      if (_stmt.step()) {
        val _tmpPk: Int
        _tmpPk = _stmt.getLong(_cursorIndexOfPk).toInt()
        val _tmpUuid: UUID
        _tmpUuid = convertByteToUUID(_stmt.getBlob(_cursorIndexOfUuid))
        val _tmpNullableUuid: UUID?
        if (_stmt.isNull(_cursorIndexOfNullableUuid)) {
          _tmpNullableUuid = null
        } else {
          _tmpNullableUuid = convertByteToUUID(_stmt.getBlob(_cursorIndexOfNullableUuid))
        }
        _result = MyEntity(_tmpPk,_tmpUuid,_tmpNullableUuid)
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
