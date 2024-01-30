import android.database.Cursor
import androidx.room.RoomDatabase
import androidx.room.RoomSQLiteQuery
import androidx.room.RoomSQLiteQuery.Companion.acquire
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.query
import com.google.common.collect.ImmutableMap
import java.lang.Class
import java.util.LinkedHashMap
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.MutableMap
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

    public override fun getSongsWithArtist(): ImmutableMap<Song, Artist> {
        val _sql: String = "SELECT * FROM Song JOIN Artist ON Song.artistKey = Artist.artistId"
        val _statement: RoomSQLiteQuery = acquire(_sql, 0)
        __db.assertNotSuspendingTransaction()
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
            val _cursorIndexOfSongId: Int = getColumnIndexOrThrow(_cursor, "songId")
            val _cursorIndexOfArtistKey: Int = getColumnIndexOrThrow(_cursor, "artistKey")
            val _cursorIndexOfArtistId: Int = getColumnIndexOrThrow(_cursor, "artistId")
            val _mapResult: MutableMap<Song, Artist> = LinkedHashMap<Song, Artist>()
            while (_cursor.moveToNext()) {
                val _key: Song
                val _tmpSongId: String
                _tmpSongId = _cursor.getString(_cursorIndexOfSongId)
                val _tmpArtistKey: String
                _tmpArtistKey = _cursor.getString(_cursorIndexOfArtistKey)
                _key = Song(_tmpSongId,_tmpArtistKey)
                if (_cursor.isNull(_cursorIndexOfArtistId)) {
                    error("The column(s) of the map value object of type 'Artist' are NULL but the map's value type argument expect it to be NON-NULL")
                }
                val _value: Artist
                val _tmpArtistId: String
                _tmpArtistId = _cursor.getString(_cursorIndexOfArtistId)
                _value = Artist(_tmpArtistId)
                if (!_mapResult.containsKey(_key)) {
                    _mapResult.put(_key, _value)
                }
            }
            val _result: ImmutableMap<Song, Artist> = ImmutableMap.copyOf(_mapResult)
            return _result
        } finally {
            _cursor.close()
            _statement.release()
        }
    }

    public companion object {
        @JvmStatic
        public fun getRequiredConverters(): List<Class<*>> = emptyList()
    }
}