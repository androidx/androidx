import androidx.room3.EntityInsertAdapter
import androidx.room3.RoomDatabase
import androidx.room3.util.convertByteToUUID
import androidx.room3.util.convertUUIDToByte
import androidx.room3.util.getColumnIndexOrThrow
import androidx.room3.util.performBlocking
import androidx.sqlite.SQLiteStatement
import java.util.UUID
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.reflect.KClass
import kotlin.uuid.Uuid

@Generated(value = ["androidx.room3.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL", "OPT_IN_USAGE_ERROR", "OPT_IN_USAGE", "MemberExtensionConflict", "CAN_BE_VAL"])
internal class MyDao_Impl(
  __db: RoomDatabase,
) : MyDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfMyEntity: EntityInsertAdapter<MyEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfMyEntity = object : EntityInsertAdapter<MyEntity>() {
      protected override fun createQuery(): String = "INSERT OR ABORT INTO `MyEntity` (`pk`,`uuid`,`nullableUuid`,`uuidKt`,`nullableUuidKt`) VALUES (?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: MyEntity) {
        statement.bindLong(1, entity.pk.toLong())
        statement.bindBlob(2, convertUUIDToByte(entity.uuid))
        val _tmpNullableUuid: UUID? = entity.nullableUuid
        if (_tmpNullableUuid == null) {
          statement.bindNull(3)
        } else {
          statement.bindBlob(3, convertUUIDToByte(_tmpNullableUuid))
        }
        statement.bindBlob(4, entity.uuidKt.toByteArray())
        val _tmpNullableUuidKt: Uuid? = entity.nullableUuidKt
        if (_tmpNullableUuidKt == null) {
          statement.bindNull(5)
        } else {
          statement.bindBlob(5, _tmpNullableUuidKt.toByteArray())
        }
      }
    }
  }

  public override fun addEntity(item: MyEntity): Unit = performBlocking(__db, false, true) { _connection ->
    __insertAdapterOfMyEntity.insert(_connection, item)
  }

  public override fun getEntity(): MyEntity {
    val _sql: String = "SELECT * FROM MyEntity"
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfPk: Int = getColumnIndexOrThrow(_stmt, "pk")
        val _columnIndexOfUuid: Int = getColumnIndexOrThrow(_stmt, "uuid")
        val _columnIndexOfNullableUuid: Int = getColumnIndexOrThrow(_stmt, "nullableUuid")
        val _columnIndexOfUuidKt: Int = getColumnIndexOrThrow(_stmt, "uuidKt")
        val _columnIndexOfNullableUuidKt: Int = getColumnIndexOrThrow(_stmt, "nullableUuidKt")
        val _result: MyEntity
        if (_stmt.step()) {
          val _tmpPk: Int
          _tmpPk = _stmt.getLong(_columnIndexOfPk).toInt()
          val _tmpUuid: UUID
          _tmpUuid = convertByteToUUID(_stmt.getBlob(_columnIndexOfUuid))
          val _tmpNullableUuid: UUID?
          if (_stmt.isNull(_columnIndexOfNullableUuid)) {
            _tmpNullableUuid = null
          } else {
            _tmpNullableUuid = convertByteToUUID(_stmt.getBlob(_columnIndexOfNullableUuid))
          }
          val _tmpUuidKt: Uuid
          _tmpUuidKt = Uuid.fromByteArray(_stmt.getBlob(_columnIndexOfUuidKt))
          val _tmpNullableUuidKt: Uuid?
          if (_stmt.isNull(_columnIndexOfNullableUuidKt)) {
            _tmpNullableUuidKt = null
          } else {
            _tmpNullableUuidKt = Uuid.fromByteArray(_stmt.getBlob(_columnIndexOfNullableUuidKt))
          }
          _result = MyEntity(_tmpPk,_tmpUuid,_tmpNullableUuid,_tmpUuidKt,_tmpNullableUuidKt)
        } else {
          error("The query result was empty, but expected a single row to return a NON-NULL object of type 'MyEntity'.")
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredColumnConverters(): List<KClass<*>> = emptyList()

    public fun getRequiredDaoReturnTypeConverters(): List<KClass<*>> = emptyList()
  }
}
