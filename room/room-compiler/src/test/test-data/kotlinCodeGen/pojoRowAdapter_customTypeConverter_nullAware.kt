import androidx.room.EntityInsertionAdapter
import androidx.room.RoomDatabase
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performReadBlocking
import androidx.sqlite.db.SupportSQLiteStatement
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
          "INSERT OR ABORT INTO `MyEntity` (`pk`,`foo`,`bar`) VALUES (?,?,?)"

      protected override fun bind(statement: SupportSQLiteStatement, entity: MyEntity) {
        statement.bindLong(1, entity.pk.toLong())
        val _tmp: String? = FooBarConverter.toString(entity.foo)
        if (_tmp == null) {
          statement.bindNull(2)
        } else {
          statement.bindString(2, _tmp)
        }
        val _tmp_1: Foo = FooBarConverter.toFoo(entity.bar)
        val _tmp_2: String? = FooBarConverter.toString(_tmp_1)
        if (_tmp_2 == null) {
          statement.bindNull(3)
        } else {
          statement.bindString(3, _tmp_2)
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
      val _cursorIndexOfFoo: Int = getColumnIndexOrThrow(_stmt, "foo")
      val _cursorIndexOfBar: Int = getColumnIndexOrThrow(_stmt, "bar")
      val _result: MyEntity
      if (_stmt.step()) {
        val _tmpPk: Int
        _tmpPk = _stmt.getLong(_cursorIndexOfPk).toInt()
        val _tmpFoo: Foo
        val _tmp: String?
        if (_stmt.isNull(_cursorIndexOfFoo)) {
          _tmp = null
        } else {
          _tmp = _stmt.getText(_cursorIndexOfFoo)
        }
        val _tmp_1: Foo? = FooBarConverter.fromString(_tmp)
        if (_tmp_1 == null) {
          error("Expected NON-NULL 'Foo', but it was NULL.")
        } else {
          _tmpFoo = _tmp_1
        }
        val _tmpBar: Bar
        val _tmp_2: String?
        if (_stmt.isNull(_cursorIndexOfBar)) {
          _tmp_2 = null
        } else {
          _tmp_2 = _stmt.getText(_cursorIndexOfBar)
        }
        val _tmp_3: Foo? = FooBarConverter.fromString(_tmp_2)
        val _tmp_4: Bar?
        if (_tmp_3 == null) {
          _tmp_4 = null
        } else {
          _tmp_4 = FooBarConverter.fromFoo(_tmp_3)
        }
        if (_tmp_4 == null) {
          error("Expected NON-NULL 'Bar', but it was NULL.")
        } else {
          _tmpBar = _tmp_4
        }
        _result = MyEntity(_tmpPk,_tmpFoo,_tmpBar)
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
