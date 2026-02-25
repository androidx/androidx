import androidx.paging.ListenableFuturePagingSource
import androidx.paging.PagingSource
import androidx.paging.rxjava3.RxPagingSource
import androidx.room3.RoomDatabase
import androidx.room3.RoomRawQuery
import androidx.room3.paging.LimitOffsetPagingSource
import androidx.room3.paging.guava.LimitOffsetListenableFuturePagingSource
import androidx.room3.paging.rxjava3.LimitOffsetRxPagingSource
import androidx.room3.util.getColumnIndexOrThrow
import androidx.room3.util.performSuspending
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.prepare
import androidx.sqlite.step
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room3.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
internal class MyDao_Impl(
  __db: RoomDatabase,
) : MyDao() {
  private val __db: RoomDatabase
  init {
    this.__db = __db
  }

  public override fun getAllIds(): PagingSource<Int, MyEntity> {
    val _sql: String = "SELECT pk FROM MyEntity"
    val _rawQuery: RoomRawQuery = RoomRawQuery(_sql)
    return object : LimitOffsetPagingSource<MyEntity>(_rawQuery, __db, "MyEntity") {
      protected override suspend fun convertRows(limitOffsetQuery: RoomRawQuery, itemCount: Int): List<MyEntity> = performSuspending(__db, true, false) { _connection ->
        val _stmt: SQLiteStatement = _connection.prepare(limitOffsetQuery.sql)
        limitOffsetQuery.getBindingFunction().invoke(_stmt)
        try {
          val _columnIndexOfPk: Int = 0
          val _result: MutableList<MyEntity> = mutableListOf()
          while (_stmt.step()) {
            val _item: MyEntity
            val _tmpPk: String
            _tmpPk = _stmt.getText(_columnIndexOfPk)
            _item = MyEntity(_tmpPk)
            _result.add(_item)
          }
          _result
        } finally {
          _stmt.close()
        }
      }
    }
  }

  public override fun getAllIdsWithArgs(gt: Long): PagingSource<Int, MyEntity> {
    val _sql: String = "SELECT * FROM MyEntity WHERE pk > ? ORDER BY pk ASC"
    val _rawQuery: RoomRawQuery = RoomRawQuery(_sql) { _stmt ->
      var _argIndex: Int = 1
      _stmt.bindLong(_argIndex, gt)
    }
    return object : LimitOffsetPagingSource<MyEntity>(_rawQuery, __db, "MyEntity") {
      protected override suspend fun convertRows(limitOffsetQuery: RoomRawQuery, itemCount: Int): List<MyEntity> = performSuspending(__db, true, false) { _connection ->
        val _stmt: SQLiteStatement = _connection.prepare(limitOffsetQuery.sql)
        limitOffsetQuery.getBindingFunction().invoke(_stmt)
        try {
          val _columnIndexOfPk: Int = getColumnIndexOrThrow(_stmt, "pk")
          val _result: MutableList<MyEntity> = mutableListOf()
          while (_stmt.step()) {
            val _item: MyEntity
            val _tmpPk: String
            _tmpPk = _stmt.getText(_columnIndexOfPk)
            _item = MyEntity(_tmpPk)
            _result.add(_item)
          }
          _result
        } finally {
          _stmt.close()
        }
      }
    }
  }

  public override fun getAllIdsRx3(): RxPagingSource<Int, MyEntity> {
    val _sql: String = "SELECT pk FROM MyEntity"
    val _rawQuery: RoomRawQuery = RoomRawQuery(_sql)
    return object : LimitOffsetRxPagingSource<MyEntity>(_rawQuery, __db, "MyEntity") {
      protected override suspend fun convertRows(limitOffsetQuery: RoomRawQuery, itemCount: Int): List<MyEntity> = performSuspending(__db, true, false) { _connection ->
        val _stmt: SQLiteStatement = _connection.prepare(limitOffsetQuery.sql)
        limitOffsetQuery.getBindingFunction().invoke(_stmt)
        try {
          val _columnIndexOfPk: Int = 0
          val _result: MutableList<MyEntity> = mutableListOf()
          while (_stmt.step()) {
            val _item: MyEntity
            val _tmpPk: String
            _tmpPk = _stmt.getText(_columnIndexOfPk)
            _item = MyEntity(_tmpPk)
            _result.add(_item)
          }
          _result
        } finally {
          _stmt.close()
        }
      }
    }
  }

  public override fun getAllIdsGuava(): ListenableFuturePagingSource<Int, MyEntity> {
    val _sql: String = "SELECT pk FROM MyEntity"
    val _rawQuery: RoomRawQuery = RoomRawQuery(_sql)
    return object : LimitOffsetListenableFuturePagingSource<MyEntity>(_rawQuery, __db, "MyEntity") {
      protected override suspend fun convertRows(limitOffsetQuery: RoomRawQuery, itemCount: Int): List<MyEntity> = performSuspending(__db, true, false) { _connection ->
        val _stmt: SQLiteStatement = _connection.prepare(limitOffsetQuery.sql)
        limitOffsetQuery.getBindingFunction().invoke(_stmt)
        try {
          val _columnIndexOfPk: Int = 0
          val _result: MutableList<MyEntity> = mutableListOf()
          while (_stmt.step()) {
            val _item: MyEntity
            val _tmpPk: String
            _tmpPk = _stmt.getText(_columnIndexOfPk)
            _item = MyEntity(_tmpPk)
            _result.add(_item)
          }
          _result
        } finally {
          _stmt.close()
        }
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
