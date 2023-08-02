import android.database.Cursor
import androidx.paging.DataSource
import androidx.room.RoomDatabase
import androidx.room.RoomSQLiteQuery
import androidx.room.RoomSQLiteQuery.Companion.acquire
import androidx.room.paging.LimitOffsetDataSource
import androidx.room.util.getColumnIndexOrThrow
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
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION"])
public class MyDao_Impl(
    __db: RoomDatabase,
) : MyDao() {
    private val __db: RoomDatabase
    init {
        this.__db = __db
    }

    public override fun getDataSourceFactory(): DataSource.Factory<Int, MyEntity> {
        val _sql: String = "SELECT * from MyEntity"
        val _statement: RoomSQLiteQuery = acquire(_sql, 0)
        return object : DataSource.Factory<Int, MyEntity>() {
            public override fun create(): LimitOffsetDataSource<MyEntity> = object :
                LimitOffsetDataSource<MyEntity>(__db, _statement, false, true, "MyEntity") {
                protected override fun convertRows(cursor: Cursor): List<MyEntity> {
                    val _cursorIndexOfPk: Int = getColumnIndexOrThrow(cursor, "pk")
                    val _cursorIndexOfOther: Int = getColumnIndexOrThrow(cursor, "other")
                    val _res: MutableList<MyEntity> = ArrayList<MyEntity>(cursor.getCount())
                    while (cursor.moveToNext()) {
                        val _item: MyEntity
                        val _tmpPk: Int
                        _tmpPk = cursor.getInt(_cursorIndexOfPk)
                        val _tmpOther: String
                        _tmpOther = cursor.getString(_cursorIndexOfOther)
                        _item = MyEntity(_tmpPk,_tmpOther)
                        _res.add(_item)
                    }
                    return _res
                }
            }
        }
    }

    public companion object {
        @JvmStatic
        public fun getRequiredConverters(): List<Class<*>> = emptyList()
    }
}