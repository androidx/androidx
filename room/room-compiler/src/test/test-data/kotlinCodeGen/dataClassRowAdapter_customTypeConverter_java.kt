import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performBlocking
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.Long
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
          "INSERT OR ABORT INTO `MyEntity` (`pk`,`data`) VALUES (?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: MyEntity) {
        val _tmp: Long? = FooConverter.toLong(entity.pk)
        if (_tmp == null) {
          statement.bindNull(1)
        } else {
          statement.bindLong(1, _tmp)
        }
        val _tmp_1: String? = FooConverter.toString(entity.data)
        if (_tmp_1 == null) {
          statement.bindNull(2)
        } else {
          statement.bindText(2, _tmp_1)
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
        val _columnIndexOfPk: Int = getColumnIndexOrThrow(_stmt, "pk")
        val _columnIndexOfData: Int = getColumnIndexOrThrow(_stmt, "data")
        val _result: MyEntity
        if (_stmt.step()) {
          val _tmpPk: Foo
          val _tmp: Long?
          if (_stmt.isNull(_columnIndexOfPk)) {
            _tmp = null
          } else {
            _tmp = _stmt.getLong(_columnIndexOfPk)
          }
          _tmpPk = FooConverter.fromLong(_tmp)
          val _tmpData: Bar
          val _tmp_1: String?
          if (_stmt.isNull(_columnIndexOfData)) {
            _tmp_1 = null
          } else {
            _tmp_1 = _stmt.getText(_columnIndexOfData)
          }
          _tmpData = FooConverter.fromString(_tmp_1)
          _result = MyEntity(_tmpPk,_tmpData)
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
