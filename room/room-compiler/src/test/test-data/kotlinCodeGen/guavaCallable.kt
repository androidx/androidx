import androidx.room.EntityDeletionOrUpdateAdapter
import androidx.room.EntityInsertionAdapter
import androidx.room.EntityUpsertionAdapter
import androidx.room.RoomDatabase
import androidx.room.guava.createListenableFuture
import androidx.room.util.appendPlaceholders
import androidx.room.util.getColumnIndexOrThrow
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.db.SupportSQLiteStatement
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Callable
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.reflect.KClass
import kotlin.text.StringBuilder

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class MyDao_Impl(
  __db: RoomDatabase,
) : MyDao {
  private val __db: RoomDatabase

  private val __insertionAdapterOfMyEntity: EntityInsertionAdapter<MyEntity>

  private val __deleteCompatAdapterOfMyEntity: EntityDeletionOrUpdateAdapter<MyEntity>

  private val __updateCompatAdapterOfMyEntity: EntityDeletionOrUpdateAdapter<MyEntity>

  private val __upsertionAdapterOfMyEntity: EntityUpsertionAdapter<MyEntity>
  init {
    this.__db = __db
    this.__insertionAdapterOfMyEntity = object : EntityInsertionAdapter<MyEntity>(__db) {
      protected override fun createQuery(): String =
          "INSERT OR ABORT INTO `MyEntity` (`pk`,`other`) VALUES (?,?)"

      protected override fun bind(statement: SupportSQLiteStatement, entity: MyEntity) {
        statement.bindLong(1, entity.pk.toLong())
        statement.bindString(2, entity.other)
      }
    }
    this.__deleteCompatAdapterOfMyEntity = object : EntityDeletionOrUpdateAdapter<MyEntity>(__db) {
      protected override fun createQuery(): String = "DELETE FROM `MyEntity` WHERE `pk` = ?"

      protected override fun bind(statement: SupportSQLiteStatement, entity: MyEntity) {
        statement.bindLong(1, entity.pk.toLong())
      }
    }
    this.__updateCompatAdapterOfMyEntity = object : EntityDeletionOrUpdateAdapter<MyEntity>(__db) {
      protected override fun createQuery(): String =
          "UPDATE OR ABORT `MyEntity` SET `pk` = ?,`other` = ? WHERE `pk` = ?"

      protected override fun bind(statement: SupportSQLiteStatement, entity: MyEntity) {
        statement.bindLong(1, entity.pk.toLong())
        statement.bindString(2, entity.other)
        statement.bindLong(3, entity.pk.toLong())
      }
    }
    this.__upsertionAdapterOfMyEntity = EntityUpsertionAdapter<MyEntity>(object :
        EntityInsertionAdapter<MyEntity>(__db) {
      protected override fun createQuery(): String =
          "INSERT INTO `MyEntity` (`pk`,`other`) VALUES (?,?)"

      protected override fun bind(statement: SupportSQLiteStatement, entity: MyEntity) {
        statement.bindLong(1, entity.pk.toLong())
        statement.bindString(2, entity.other)
      }
    }, object : EntityDeletionOrUpdateAdapter<MyEntity>(__db) {
      protected override fun createQuery(): String =
          "UPDATE `MyEntity` SET `pk` = ?,`other` = ? WHERE `pk` = ?"

      protected override fun bind(statement: SupportSQLiteStatement, entity: MyEntity) {
        statement.bindLong(1, entity.pk.toLong())
        statement.bindString(2, entity.other)
        statement.bindLong(3, entity.pk.toLong())
      }
    })
  }

  public override fun insertListenableFuture(vararg entities: MyEntity):
      ListenableFuture<List<Long>> = createListenableFuture(__db, true, object :
      Callable<List<Long>> {
    public override fun call(): List<Long> {
      __db.beginTransaction()
      try {
        val _result: List<Long> = __insertionAdapterOfMyEntity.insertAndReturnIdsList(entities)
        __db.setTransactionSuccessful()
        return _result
      } finally {
        __db.endTransaction()
      }
    }
  })

  public override fun deleteListenableFuture(entity: MyEntity): ListenableFuture<Int> =
      createListenableFuture(__db, true, object : Callable<Int> {
    public override fun call(): Int {
      var _total: Int = 0
      __db.beginTransaction()
      try {
        _total += __deleteCompatAdapterOfMyEntity.handle(entity)
        __db.setTransactionSuccessful()
        return _total
      } finally {
        __db.endTransaction()
      }
    }
  })

  public override fun updateListenableFuture(entity: MyEntity): ListenableFuture<Int> =
      createListenableFuture(__db, true, object : Callable<Int> {
    public override fun call(): Int {
      var _total: Int = 0
      __db.beginTransaction()
      try {
        _total += __updateCompatAdapterOfMyEntity.handle(entity)
        __db.setTransactionSuccessful()
        return _total
      } finally {
        __db.endTransaction()
      }
    }
  })

  public override fun upsertListenableFuture(vararg entities: MyEntity):
      ListenableFuture<List<Long>> = createListenableFuture(__db, true, object :
      Callable<List<Long>> {
    public override fun call(): List<Long> {
      __db.beginTransaction()
      try {
        val _result: List<Long> = __upsertionAdapterOfMyEntity.upsertAndReturnIdsList(entities)
        __db.setTransactionSuccessful()
        return _result
      } finally {
        __db.endTransaction()
      }
    }
  })

  public override fun getListenableFuture(vararg arg: String?): ListenableFuture<MyEntity> {
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("SELECT * FROM MyEntity WHERE pk IN (")
    val _inputSize: Int = arg.size
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    return createListenableFuture(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        for (_item: String? in arg) {
          if (_item == null) {
            _stmt.bindNull(_argIndex)
          } else {
            _stmt.bindText(_argIndex, _item)
          }
          _argIndex++
        }
        val _cursorIndexOfPk: Int = getColumnIndexOrThrow(_stmt, "pk")
        val _cursorIndexOfOther: Int = getColumnIndexOrThrow(_stmt, "other")
        val _result: MyEntity
        if (_stmt.step()) {
          val _tmpPk: Int
          _tmpPk = _stmt.getLong(_cursorIndexOfPk).toInt()
          val _tmpOther: String
          _tmpOther = _stmt.getText(_cursorIndexOfOther)
          _result = MyEntity(_tmpPk,_tmpOther)
        } else {
          error("The query result was empty, but expected a single row to return a NON-NULL object of type <MyEntity>.")
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getListenableFutureNullable(vararg arg: String?):
      ListenableFuture<MyEntity?> {
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("SELECT * FROM MyEntity WHERE pk IN (")
    val _inputSize: Int = arg.size
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    return createListenableFuture(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        for (_item: String? in arg) {
          if (_item == null) {
            _stmt.bindNull(_argIndex)
          } else {
            _stmt.bindText(_argIndex, _item)
          }
          _argIndex++
        }
        val _cursorIndexOfPk: Int = getColumnIndexOrThrow(_stmt, "pk")
        val _cursorIndexOfOther: Int = getColumnIndexOrThrow(_stmt, "other")
        val _result: MyEntity?
        if (_stmt.step()) {
          val _tmpPk: Int
          _tmpPk = _stmt.getLong(_cursorIndexOfPk).toInt()
          val _tmpOther: String
          _tmpOther = _stmt.getText(_cursorIndexOfOther)
          _result = MyEntity(_tmpPk,_tmpOther)
        } else {
          _result = null
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
