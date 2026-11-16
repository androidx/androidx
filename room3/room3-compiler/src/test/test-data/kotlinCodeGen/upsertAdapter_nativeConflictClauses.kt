import androidx.room3.EntityDeleteOrUpdateAdapter
import androidx.room3.EntityInsertAdapter
import androidx.room3.EntityUpsertAdapter
import androidx.room3.RoomDatabase
import androidx.room3.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.reflect.KClass

@Generated(value = ["androidx.room3.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL", "OPT_IN_USAGE_ERROR", "OPT_IN_USAGE", "MemberExtensionConflict", "CAN_BE_VAL"])
internal class MyDao_Impl(
  __db: RoomDatabase,
) : MyDao {
  private val __db: RoomDatabase

  private val __upsertAdapterOfMyEntity: EntityUpsertAdapter<MyEntity>

  private val __upsertAdapterOfCompositeEntity: EntityUpsertAdapter<CompositeEntity>

  private val __upsertAdapterOfPkOnlyEntity: EntityUpsertAdapter<PkOnlyEntity>

  private val __upsertAdapterOfTitleOnlyAsAutoGenEntity: EntityUpsertAdapter<TitleOnly>

  private val __upsertAdapterOfFtsEntityItem: EntityUpsertAdapter<FtsEntityItem>
  init {
    this.__db = __db
    this.__upsertAdapterOfMyEntity = EntityUpsertAdapter<MyEntity>(object : EntityInsertAdapter<MyEntity>() {
      protected override fun createQuery(): String = "INSERT INTO `MyEntity` (`pk`,`data`) VALUES (?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: MyEntity) {
        statement.bindLong(1, entity.pk.toLong())
        statement.bindText(2, entity.data)
      }
    }, object : EntityDeleteOrUpdateAdapter<MyEntity>() {
      protected override fun createQuery(): String = "UPDATE `MyEntity` SET `pk` = ?,`data` = ? WHERE `pk` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: MyEntity) {
        statement.bindLong(1, entity.pk.toLong())
        statement.bindText(2, entity.data)
        statement.bindLong(3, entity.pk.toLong())
      }
    }, "INSERT INTO `MyEntity` (`pk`,`data`) VALUES (?,?) ON CONFLICT (`pk`) DO UPDATE SET `data` = excluded.`data`")
    this.__upsertAdapterOfCompositeEntity = EntityUpsertAdapter<CompositeEntity>(object : EntityInsertAdapter<CompositeEntity>() {
      protected override fun createQuery(): String = "INSERT INTO `CompositeEntity` (`pk1`,`pk2`,`value`) VALUES (?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: CompositeEntity) {
        statement.bindLong(1, entity.pk1.toLong())
        statement.bindText(2, entity.pk2)
        statement.bindLong(3, entity.value.toLong())
      }
    }, object : EntityDeleteOrUpdateAdapter<CompositeEntity>() {
      protected override fun createQuery(): String = "UPDATE `CompositeEntity` SET `pk1` = ?,`pk2` = ?,`value` = ? WHERE `pk1` = ? AND `pk2` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: CompositeEntity) {
        statement.bindLong(1, entity.pk1.toLong())
        statement.bindText(2, entity.pk2)
        statement.bindLong(3, entity.value.toLong())
        statement.bindLong(4, entity.pk1.toLong())
        statement.bindText(5, entity.pk2)
      }
    }, "INSERT INTO `CompositeEntity` (`pk1`,`pk2`,`value`) VALUES (?,?,?) ON CONFLICT (`pk1`,`pk2`) DO UPDATE SET `value` = excluded.`value`")
    this.__upsertAdapterOfPkOnlyEntity = EntityUpsertAdapter<PkOnlyEntity>(object : EntityInsertAdapter<PkOnlyEntity>() {
      protected override fun createQuery(): String = "INSERT INTO `PkOnlyEntity` (`a`,`b`) VALUES (?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: PkOnlyEntity) {
        statement.bindLong(1, entity.a)
        statement.bindLong(2, entity.b)
      }
    }, object : EntityDeleteOrUpdateAdapter<PkOnlyEntity>() {
      protected override fun createQuery(): String = "UPDATE `PkOnlyEntity` SET `a` = ?,`b` = ? WHERE `a` = ? AND `b` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: PkOnlyEntity) {
        statement.bindLong(1, entity.a)
        statement.bindLong(2, entity.b)
        statement.bindLong(3, entity.a)
        statement.bindLong(4, entity.b)
      }
    }, "INSERT INTO `PkOnlyEntity` (`a`,`b`) VALUES (?,?) ON CONFLICT (`a`,`b`) DO NOTHING")
    this.__upsertAdapterOfTitleOnlyAsAutoGenEntity = EntityUpsertAdapter<TitleOnly>(object : EntityInsertAdapter<TitleOnly>() {
      protected override fun createQuery(): String = "INSERT INTO `AutoGenEntity` (`title`) VALUES (?)"

      protected override fun bind(statement: SQLiteStatement, entity: TitleOnly) {
        statement.bindText(1, entity.title)
      }
    }, object : EntityDeleteOrUpdateAdapter<TitleOnly>() {
      protected override fun createQuery(): String = "UPDATE `AutoGenEntity` SET `title` = ? WHERE "

      protected override fun bind(statement: SQLiteStatement, entity: TitleOnly) {
        statement.bindText(1, entity.title)
      }
    }, null)
    this.__upsertAdapterOfFtsEntityItem = EntityUpsertAdapter<FtsEntityItem>(object : EntityInsertAdapter<FtsEntityItem>() {
      protected override fun createQuery(): String = "INSERT INTO `FtsEntityItem` (`rowid`,`body`) VALUES (nullif(?, 0),?)"

      protected override fun bind(statement: SQLiteStatement, entity: FtsEntityItem) {
        statement.bindLong(1, entity.rowId.toLong())
        statement.bindText(2, entity.body)
      }
    }, object : EntityDeleteOrUpdateAdapter<FtsEntityItem>() {
      protected override fun createQuery(): String = "UPDATE `FtsEntityItem` SET `rowid` = ?,`body` = ? WHERE `rowid` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: FtsEntityItem) {
        statement.bindLong(1, entity.rowId.toLong())
        statement.bindText(2, entity.body)
        statement.bindLong(3, entity.rowId.toLong())
      }
    }, null)
  }

  public override suspend fun upsertStandard(item: MyEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __upsertAdapterOfMyEntity.upsert(_connection, item)
  }

  public override suspend fun upsertComposite(item: CompositeEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __upsertAdapterOfCompositeEntity.upsert(_connection, item)
  }

  public override suspend fun upsertPkOnly(item: PkOnlyEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __upsertAdapterOfPkOnlyEntity.upsert(_connection, item)
  }

  public override suspend fun upsertPartialWithoutPk(item: TitleOnly): Unit = performSuspending(__db, false, true) { _connection ->
    __upsertAdapterOfTitleOnlyAsAutoGenEntity.upsert(_connection, item)
  }

  public override suspend fun upsertFts(item: FtsEntityItem): Unit = performSuspending(__db, false, true) { _connection ->
    __upsertAdapterOfFtsEntityItem.upsert(_connection, item)
  }

  public companion object {
    public fun getRequiredColumnConverters(): List<KClass<*>> = emptyList()

    public fun getRequiredDaoReturnTypeConverters(): List<KClass<*>> = emptyList()
  }
}
