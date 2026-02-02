import androidx.room3.EntityDeleteOrUpdateAdapter
import androidx.room3.EntityInsertAdapter
import androidx.room3.EntityUpsertAdapter
import androidx.room3.RoomDatabase
import androidx.room3.util.performBlocking
import androidx.sqlite.SQLiteStatement
import java.lang.Void
import javax.`annotation`.processing.Generated
import kotlin.Array
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.reflect.KClass

@Generated(value = ["androidx.room3.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
internal class MyDao_Impl(
  __db: RoomDatabase,
) : MyDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfMyEntity: EntityInsertAdapter<MyEntity>

  private val __upsertAdapterOfMyEntity: EntityUpsertAdapter<MyEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfMyEntity = object : EntityInsertAdapter<MyEntity>() {
      protected override fun createQuery(): String = "INSERT OR ABORT INTO `MyEntity` (`pk`,`data`) VALUES (?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: MyEntity) {
        statement.bindLong(1, entity.pk)
        statement.bindText(2, entity.data)
      }
    }
    this.__upsertAdapterOfMyEntity = EntityUpsertAdapter<MyEntity>(object : EntityInsertAdapter<MyEntity>() {
      protected override fun createQuery(): String = "INSERT INTO `MyEntity` (`pk`,`data`) VALUES (?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: MyEntity) {
        statement.bindLong(1, entity.pk)
        statement.bindText(2, entity.data)
      }
    }, object : EntityDeleteOrUpdateAdapter<MyEntity>() {
      protected override fun createQuery(): String = "UPDATE `MyEntity` SET `pk` = ?,`data` = ? WHERE `pk` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: MyEntity) {
        statement.bindLong(1, entity.pk)
        statement.bindText(2, entity.data)
        statement.bindLong(3, entity.pk)
      }
    })
  }

  public override fun insertEntity(item: MyEntity): Unit = performBlocking(__db, false, true) { _connection ->
    __insertAdapterOfMyEntity.insert(_connection, item)
  }

  public override fun insertEntityAndReturnVoid(item: MyEntity): Void? = performBlocking(__db, false, true) { _connection ->
    __insertAdapterOfMyEntity.insert(_connection, item)
    null
  }

  public override fun insertEntityAndReturnRowId(item: MyEntity): Long = performBlocking(__db, false, true) { _connection ->
    val _result: Long = __insertAdapterOfMyEntity.insertAndReturnId(_connection, item)
    _result
  }

  public override fun insertEntityListAndReturnRowIds(items: List<MyEntity>): List<Long> = performBlocking(__db, false, true) { _connection ->
    val _result: List<Long> = __insertAdapterOfMyEntity.insertAndReturnIdsList(_connection, items)
    _result
  }

  public override fun insertEntityListAndReturnMutableRowIds(items: List<MyEntity>): MutableList<Long> = performBlocking(__db, false, true) { _connection ->
    val _result: MutableList<Long> = __insertAdapterOfMyEntity.insertAndReturnIdsList(_connection, items).toMutableList()
    _result
  }

  public override fun upsertEntity(item: MyEntity): Unit = performBlocking(__db, false, true) { _connection ->
    __upsertAdapterOfMyEntity.upsert(_connection, item)
  }

  public override fun upsertEntityAndReturnVoid(item: MyEntity): Void? = performBlocking(__db, false, true) { _connection ->
    __upsertAdapterOfMyEntity.upsert(_connection, item)
    null
  }

  public override fun upsertEntityAndReturnRowId(item: MyEntity): Long = performBlocking(__db, false, true) { _connection ->
    val _result: Long = __upsertAdapterOfMyEntity.upsertAndReturnId(_connection, item)
    _result
  }

  public override fun upsertEntityListAndReturnRowIds(items: List<MyEntity>): List<Long> = performBlocking(__db, false, true) { _connection ->
    val _result: List<Long> = __upsertAdapterOfMyEntity.upsertAndReturnIdsList(_connection, items)
    _result
  }

  public override fun upsertEntityListAndReturnMutableRowIds(items: List<MyEntity>): MutableList<Long> = performBlocking(__db, false, true) { _connection ->
    val _result: MutableList<Long> = __upsertAdapterOfMyEntity.upsertAndReturnIdsList(_connection, items).toMutableList()
    _result
  }

  public override fun upsertEntityListAndReturnRowIdsArray(items: List<MyEntity>): Array<Long> = performBlocking(__db, false, true) { _connection ->
    val _result: Array<Long> = (__upsertAdapterOfMyEntity.upsertAndReturnIdsArrayBox(_connection, items)) as Array<Long>
    _result
  }

  public override fun upsertEntityListAndReturnRowIdsOutArray(items: List<MyEntity>): Array<out Long> = performBlocking(__db, false, true) { _connection ->
    val _result: Array<out Long> = __upsertAdapterOfMyEntity.upsertAndReturnIdsArrayBox(_connection, items)
    _result
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
