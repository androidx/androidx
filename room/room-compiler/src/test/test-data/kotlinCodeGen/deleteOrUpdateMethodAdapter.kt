import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.RoomDatabase
import androidx.room.util.performBlocking
import androidx.sqlite.SQLiteStatement
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

  private val __deleteAdapterOfMyEntity: EntityDeleteOrUpdateAdapter<MyEntity>

  private val __updateAdapterOfMyEntity: EntityDeleteOrUpdateAdapter<MyEntity>
  init {
    this.__db = __db
    this.__deleteAdapterOfMyEntity = object : EntityDeleteOrUpdateAdapter<MyEntity>() {
      protected override fun createQuery(): String = "DELETE FROM `MyEntity` WHERE `pk` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: MyEntity) {
        statement.bindLong(1, entity.pk)
      }
    }
    this.__updateAdapterOfMyEntity = object : EntityDeleteOrUpdateAdapter<MyEntity>() {
      protected override fun createQuery(): String =
          "UPDATE OR ABORT `MyEntity` SET `pk` = ?,`data` = ? WHERE `pk` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: MyEntity) {
        statement.bindLong(1, entity.pk)
        statement.bindText(2, entity.data)
        statement.bindLong(3, entity.pk)
      }
    }
  }

  public override fun deleteEntity(item: MyEntity): Unit = performBlocking(__db, false, true) {
      _connection ->
    __deleteAdapterOfMyEntity.handle(_connection, item)
  }

  public override fun deleteEntityAndReturnCount(item: MyEntity): Int = performBlocking(__db, false,
      true) { _connection ->
    var _result: Int = 0
    _result += __deleteAdapterOfMyEntity.handle(_connection, item)
    _result
  }

  public override fun updateEntity(item: MyEntity): Unit = performBlocking(__db, false, true) {
      _connection ->
    __updateAdapterOfMyEntity.handle(_connection, item)
  }

  public override fun updateEntityAndReturnCount(item: MyEntity): Int = performBlocking(__db, false,
      true) { _connection ->
    var _result: Int = 0
    _result += __updateAdapterOfMyEntity.handle(_connection, item)
    _result
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
