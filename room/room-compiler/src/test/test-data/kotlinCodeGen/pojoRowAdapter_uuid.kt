import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.util.convertByteToUUID
import androidx.room.util.convertUUIDToByte
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performBlocking
import androidx.sqlite.SQLiteStatement
import java.util.UUID
import javax.`annotation`.processing.Generated
import kotlin.Int
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
          "INSERT OR ABORT INTO `MyEntity` (`pk`,`uuid`,`nullableUuid`) VALUES (?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: MyEntity) {
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

  public override fun addEntity(item: MyEntity): Unit = performBlocking(__db, false, true) {
      _connection ->
    __insertAdapterOfMyEntity.insert(_connection, item)
  }

  public override fun getEntity(): MyEntity {
    val _sql: String = "SELECT * FROM MyEntity"
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
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
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
