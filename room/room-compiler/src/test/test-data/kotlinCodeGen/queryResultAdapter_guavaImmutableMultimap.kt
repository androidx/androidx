import android.database.Cursor
import androidx.room.RoomDatabase
import androidx.room.RoomSQLiteQuery
import androidx.room.RoomSQLiteQuery.Companion.acquire
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.query
import com.google.common.collect.ImmutableListMultimap
import com.google.common.collect.ImmutableSetMultimap
import java.lang.Class
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

    public override fun getArtistWithSongs(): ImmutableSetMultimap<Artist, Song> {
        val _sql: String = "SELECT * FROM Artist JOIN Song ON Artist.artistId = Song.artistKey"
        val _statement: RoomSQLiteQuery = acquire(_sql, 0)
        __db.assertNotSuspendingTransaction()
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
            val _cursorIndexOfArtistId: Int = getColumnIndexOrThrow(_cursor, "artistId")
            val _cursorIndexOfSongId: Int = getColumnIndexOrThrow(_cursor, "songId")
            val _cursorIndexOfArtistKey: Int = getColumnIndexOrThrow(_cursor, "artistKey")
            val _mapBuilder: ImmutableSetMultimap.Builder<Artist, Song> = ImmutableSetMultimap.builder()
            while (_cursor.moveToNext()) {
                val _key: Artist
                val _tmpArtistId: String
                _tmpArtistId = _cursor.getString(_cursorIndexOfArtistId)
                _key = Artist(_tmpArtistId)
                if (_cursor.isNull(_cursorIndexOfSongId) && _cursor.isNull(_cursorIndexOfArtistKey)) {
                    continue
                }
                val _value: Song
                val _tmpSongId: String
                _tmpSongId = _cursor.getString(_cursorIndexOfSongId)
                val _tmpArtistKey: String
                _tmpArtistKey = _cursor.getString(_cursorIndexOfArtistKey)
                _value = Song(_tmpSongId,_tmpArtistKey)
                _mapBuilder.put(_key, _value)
            }
            val _result: ImmutableSetMultimap<Artist, Song> = _mapBuilder.build()
            return _result
        } finally {
            _cursor.close()
            _statement.release()
        }
    }

    public override fun getArtistWithSongIds(): ImmutableListMultimap<Artist, Song> {
        val _sql: String = "SELECT * FROM Artist JOIN Song ON Artist.artistId = Song.artistKey"
        val _statement: RoomSQLiteQuery = acquire(_sql, 0)
        __db.assertNotSuspendingTransaction()
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
            val _cursorIndexOfArtistId: Int = getColumnIndexOrThrow(_cursor, "artistId")
            val _cursorIndexOfSongId: Int = getColumnIndexOrThrow(_cursor, "songId")
            val _cursorIndexOfArtistKey: Int = getColumnIndexOrThrow(_cursor, "artistKey")
            val _mapBuilder: ImmutableListMultimap.Builder<Artist, Song> = ImmutableListMultimap.builder()
            while (_cursor.moveToNext()) {
                val _key: Artist
                val _tmpArtistId: String
                _tmpArtistId = _cursor.getString(_cursorIndexOfArtistId)
                _key = Artist(_tmpArtistId)
                if (_cursor.isNull(_cursorIndexOfSongId) && _cursor.isNull(_cursorIndexOfArtistKey)) {
                    continue
                }
                val _value: Song
                val _tmpSongId: String
                _tmpSongId = _cursor.getString(_cursorIndexOfSongId)
                val _tmpArtistKey: String
                _tmpArtistKey = _cursor.getString(_cursorIndexOfArtistKey)
                _value = Song(_tmpSongId,_tmpArtistKey)
                _mapBuilder.put(_key, _value)
            }
            val _result: ImmutableListMultimap<Artist, Song> = _mapBuilder.build()
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