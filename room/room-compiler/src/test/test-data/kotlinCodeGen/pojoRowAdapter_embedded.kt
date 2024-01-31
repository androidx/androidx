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
          "INSERT OR ABORT INTO `MyEntity` (`pk`,`numberData`,`stringData`,`nullablenumberData`,`nullablestringData`) VALUES (?,?,?,?,?)"

      protected override fun bind(statement: SupportSQLiteStatement, entity: MyEntity) {
        statement.bindLong(1, entity.pk.toLong())
        val _tmpFoo: Foo = entity.foo
        statement.bindLong(2, _tmpFoo.numberData)
        statement.bindString(3, _tmpFoo.stringData)
        val _tmpNullableFoo: Foo? = entity.nullableFoo
        if (_tmpNullableFoo != null) {
          statement.bindLong(4, _tmpNullableFoo.numberData)
          statement.bindString(5, _tmpNullableFoo.stringData)
        } else {
          statement.bindNull(4)
          statement.bindNull(5)
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
      val _cursorIndexOfNumberData: Int = getColumnIndexOrThrow(_stmt, "numberData")
      val _cursorIndexOfStringData: Int = getColumnIndexOrThrow(_stmt, "stringData")
      val _cursorIndexOfNumberData_1: Int = getColumnIndexOrThrow(_stmt, "nullablenumberData")
      val _cursorIndexOfStringData_1: Int = getColumnIndexOrThrow(_stmt, "nullablestringData")
      val _result: MyEntity
      if (_stmt.step()) {
        val _tmpPk: Int
        _tmpPk = _stmt.getLong(_cursorIndexOfPk).toInt()
        val _tmpFoo: Foo
        val _tmpNumberData: Long
        _tmpNumberData = _stmt.getLong(_cursorIndexOfNumberData)
        val _tmpStringData: String
        _tmpStringData = _stmt.getText(_cursorIndexOfStringData)
        _tmpFoo = Foo(_tmpNumberData,_tmpStringData)
        val _tmpNullableFoo: Foo?
        if (!(_stmt.isNull(_cursorIndexOfNumberData_1) &&
            _stmt.isNull(_cursorIndexOfStringData_1))) {
          val _tmpNumberData_1: Long
          _tmpNumberData_1 = _stmt.getLong(_cursorIndexOfNumberData_1)
          val _tmpStringData_1: String
          _tmpStringData_1 = _stmt.getText(_cursorIndexOfStringData_1)
          _tmpNullableFoo = Foo(_tmpNumberData_1,_tmpStringData_1)
        } else {
          _tmpNullableFoo = null
        }
        _result = MyEntity(_tmpPk,_tmpFoo,_tmpNullableFoo)
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
