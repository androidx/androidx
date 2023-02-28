import android.database.Cursor
import androidx.paging.ListenableFuturePagingSource
import androidx.paging.PagingSource
import androidx.paging.rxjava2.RxPagingSource
import androidx.room.RoomDatabase
import androidx.room.RoomSQLiteQuery
import androidx.room.RoomSQLiteQuery.Companion.acquire
import androidx.room.paging.LimitOffsetPagingSource
import androidx.room.paging.guava.LimitOffsetListenableFuturePagingSource
import androidx.room.paging.rxjava2.LimitOffsetRxPagingSource
import java.lang.Class
import java.util.ArrayList
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.jvm.JvmStatic

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION"])
public class MyDao_Impl(
    __db: RoomDatabase,
) : MyDao() {
    private val __db: RoomDatabase
    init {
        this.__db = __db
    }

    public override fun getAllIds(): PagingSource<Int, MyEntity> {
        val _sql: String = "SELECT pk FROM MyEntity"
        val _statement: RoomSQLiteQuery = acquire(_sql, 0)
        return object : LimitOffsetPagingSource<MyEntity>(_statement, __db, "MyEntity") {
            protected override fun convertRows(cursor: Cursor): List<MyEntity> {
                val _cursorIndexOfPk: Int = 0
                val _result: MutableList<MyEntity> = ArrayList<MyEntity>(cursor.getCount())
                while (cursor.moveToNext()) {
                    val _item: MyEntity
                    val _tmpPk: Int
                    _tmpPk = cursor.getInt(_cursorIndexOfPk)
                    _item = MyEntity(_tmpPk)
                    _result.add(_item)
                }
                return _result
            }
        }
    }

    public override fun getAllIdsRx2(): RxPagingSource<Int, MyEntity> {
        val _sql: String = "SELECT pk FROM MyEntity"
        val _statement: RoomSQLiteQuery = acquire(_sql, 0)
        return object : LimitOffsetRxPagingSource<MyEntity>(_statement, __db, "MyEntity") {
            protected override fun convertRows(cursor: Cursor): List<MyEntity> {
                val _cursorIndexOfPk: Int = 0
                val _result: MutableList<MyEntity> = ArrayList<MyEntity>(cursor.getCount())
                while (cursor.moveToNext()) {
                    val _item: MyEntity
                    val _tmpPk: Int
                    _tmpPk = cursor.getInt(_cursorIndexOfPk)
                    _item = MyEntity(_tmpPk)
                    _result.add(_item)
                }
                return _result
            }
        }
    }

    public override fun getAllIdsRx3(): androidx.paging.rxjava3.RxPagingSource<Int, MyEntity> {
        val _sql: String = "SELECT pk FROM MyEntity"
        val _statement: RoomSQLiteQuery = acquire(_sql, 0)
        return object : androidx.room.paging.rxjava3.LimitOffsetRxPagingSource<MyEntity>(_statement,
            __db, "MyEntity") {
            protected override fun convertRows(cursor: Cursor): List<MyEntity> {
                val _cursorIndexOfPk: Int = 0
                val _result: MutableList<MyEntity> = ArrayList<MyEntity>(cursor.getCount())
                while (cursor.moveToNext()) {
                    val _item: MyEntity
                    val _tmpPk: Int
                    _tmpPk = cursor.getInt(_cursorIndexOfPk)
                    _item = MyEntity(_tmpPk)
                    _result.add(_item)
                }
                return _result
            }
        }
    }

    public override fun getAllIdsGuava(): ListenableFuturePagingSource<Int, MyEntity> {
        val _sql: String = "SELECT pk FROM MyEntity"
        val _statement: RoomSQLiteQuery = acquire(_sql, 0)
        return object : LimitOffsetListenableFuturePagingSource<MyEntity>(_statement, __db, "MyEntity")
        {
            protected override fun convertRows(cursor: Cursor): List<MyEntity> {
                val _cursorIndexOfPk: Int = 0
                val _result: MutableList<MyEntity> = ArrayList<MyEntity>(cursor.getCount())
                while (cursor.moveToNext()) {
                    val _item: MyEntity
                    val _tmpPk: Int
                    _tmpPk = cursor.getInt(_cursorIndexOfPk)
                    _item = MyEntity(_tmpPk)
                    _result.add(_item)
                }
                return _result
            }
        }
    }

    public companion object {
        @JvmStatic
        public fun getRequiredConverters(): List<Class<*>> = emptyList()
    }
}