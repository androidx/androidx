import android.database.Cursor
import android.os.CancellationSignal
import androidx.room.CoroutinesRoom.Companion.execute
import androidx.room.RoomDatabase
import androidx.room.RoomSQLiteQuery
import androidx.room.RoomSQLiteQuery.Companion.acquire
import androidx.room.util.createCancellationSignal
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.query
import java.lang.Class
import java.util.concurrent.Callable
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.jvm.JvmStatic

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION"])
public class MyDao_Impl(
    __db: RoomDatabase,
) : MyDao {
    private val __db: RoomDatabase
    init {
        this.__db = __db
    }

    public override suspend fun getEntity(): MyEntity {
        val _sql: String = "SELECT * FROM MyEntity"
        val _statement: RoomSQLiteQuery = acquire(_sql, 0)
        val _cancellationSignal: CancellationSignal? = createCancellationSignal()
        return execute(__db, false, _cancellationSignal, object : Callable<MyEntity> {
            public override fun call(): MyEntity {
                val _cursor: Cursor = query(__db, _statement, false, null)
                try {
                    val _cursorIndexOfPk: Int = getColumnIndexOrThrow(_cursor, "pk")
                    val _result: MyEntity
                    if (_cursor.moveToFirst()) {
                        val _tmpPk: Int
                        _tmpPk = _cursor.getInt(_cursorIndexOfPk)
                        _result = MyEntity(_tmpPk)
                    } else {
                        error("The query result was empty, but expected a single row to return a NON-NULL object of type <MyEntity>.")
                    }
                    return _result
                } finally {
                    _cursor.close()
                    _statement.release()
                }
            }
        })
    }

    public companion object {
        @JvmStatic
        public fun getRequiredConverters(): List<Class<*>> = emptyList()
    }
}