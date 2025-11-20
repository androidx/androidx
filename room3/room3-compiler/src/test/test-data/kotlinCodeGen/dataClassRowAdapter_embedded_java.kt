import androidx.room3.EntityInsertAdapter
import androidx.room3.RoomDatabase
import androidx.room3.util.getColumnIndexOrThrow
import androidx.room3.util.performBlocking
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.reflect.KClass

@Generated(value = ["androidx.room3.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
internal class MyDao_Impl(
  __db: RoomDatabase,
) : MyDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfMyEntity: EntityInsertAdapter<MyEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfMyEntity = object : EntityInsertAdapter<MyEntity>() {
      protected override fun createQuery(): String = "INSERT OR ABORT INTO `MyEntity` (`mKey`,`numberData`,`stringData`,`nullablenumberData`,`nullablestringData`) VALUES (?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: MyEntity) {
        statement.bindLong(1, entity.mKey.toLong())
        val _tmpMFoo: Foo? = entity.getFoo()
        if (_tmpMFoo != null) {
          statement.bindLong(2, _tmpMFoo.numberData)
          statement.bindText(3, _tmpMFoo.stringData)
        } else {
          statement.bindNull(2)
          statement.bindNull(3)
        }
        val _tmpMNullableFoo: Foo? = entity.getNullableFoo()
        if (_tmpMNullableFoo != null) {
          statement.bindLong(4, _tmpMNullableFoo.numberData)
          statement.bindText(5, _tmpMNullableFoo.stringData)
        } else {
          statement.bindNull(4)
          statement.bindNull(5)
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
        val _columnIndexOfMKey: Int = getColumnIndexOrThrow(_stmt, "mKey")
        val _columnIndexOfNumberData: Int = getColumnIndexOrThrow(_stmt, "numberData")
        val _columnIndexOfStringData: Int = getColumnIndexOrThrow(_stmt, "stringData")
        val _columnIndexOfNumberData_1: Int = getColumnIndexOrThrow(_stmt, "nullablenumberData")
        val _columnIndexOfStringData_1: Int = getColumnIndexOrThrow(_stmt, "nullablestringData")
        val _result: MyEntity
        if (_stmt.step()) {
          val _tmpMKey: Int
          _tmpMKey = _stmt.getLong(_columnIndexOfMKey).toInt()
          val _tmpMFoo: Foo?
          if (!(_stmt.isNull(_columnIndexOfNumberData) && _stmt.isNull(_columnIndexOfStringData))) {
            val _tmpNumberData: Long
            _tmpNumberData = _stmt.getLong(_columnIndexOfNumberData)
            val _tmpStringData: String
            _tmpStringData = _stmt.getText(_columnIndexOfStringData)
            _tmpMFoo = Foo(_tmpNumberData,_tmpStringData)
          } else {
            _tmpMFoo = null
          }
          val _tmpMNullableFoo: Foo?
          if (!(_stmt.isNull(_columnIndexOfNumberData_1) && _stmt.isNull(_columnIndexOfStringData_1))) {
            val _tmpNumberData_1: Long
            _tmpNumberData_1 = _stmt.getLong(_columnIndexOfNumberData_1)
            val _tmpStringData_1: String
            _tmpStringData_1 = _stmt.getText(_columnIndexOfStringData_1)
            _tmpMNullableFoo = Foo(_tmpNumberData_1,_tmpStringData_1)
          } else {
            _tmpMNullableFoo = null
          }
          _result = MyEntity(_tmpMKey)
          _result.setFoo(_tmpMFoo)
          _result.setNullableFoo(_tmpMNullableFoo)
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
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
