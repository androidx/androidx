import android.database.Cursor
import androidx.room.RoomDatabase
import androidx.room.RoomSQLiteQuery
import androidx.room.RoomSQLiteQuery.Companion.acquire
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.query
import java.lang.Class
import java.util.ArrayList
import java.util.LinkedHashMap
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
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

    public override fun getSongsWithArtist(): Map<Song, Artist> {
        val _sql: String = "SELECT * FROM Song JOIN Artist ON Song.artistKey = Artist.artistId"
        val _statement: RoomSQLiteQuery = acquire(_sql, 0)
        __db.assertNotSuspendingTransaction()
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
            val _cursorIndexOfSongId: Int = getColumnIndexOrThrow(_cursor, "songId")
            val _cursorIndexOfArtistKey: Int = getColumnIndexOrThrow(_cursor, "artistKey")
            val _cursorIndexOfArtistId: Int = getColumnIndexOrThrow(_cursor, "artistId")
            val _result: MutableMap<Song, Artist> = LinkedHashMap<Song, Artist>()
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
                if (!_result.containsKey(_key)) {
                    _result.put(_key, _value)
                }
            }
            return _result
        } finally {
            _cursor.close()
            _statement.release()
        }
    }

    public override fun getArtistWithSongs(): Map<Artist, List<Song>> {
        val _sql: String = "SELECT * FROM Artist JOIN Song ON Artist.artistId = Song.artistKey"
        val _statement: RoomSQLiteQuery = acquire(_sql, 0)
        __db.assertNotSuspendingTransaction()
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
            val _cursorIndexOfArtistId: Int = getColumnIndexOrThrow(_cursor, "artistId")
            val _cursorIndexOfSongId: Int = getColumnIndexOrThrow(_cursor, "songId")
            val _cursorIndexOfArtistKey: Int = getColumnIndexOrThrow(_cursor, "artistKey")
            val _result: MutableMap<Artist, MutableList<Song>> =
                LinkedHashMap<Artist, MutableList<Song>>()
            while (_cursor.moveToNext()) {
                val _key: Artist
                val _tmpArtistId: String
                _tmpArtistId = _cursor.getString(_cursorIndexOfArtistId)
                _key = Artist(_tmpArtistId)
                val _values: MutableList<Song>
                if (_result.containsKey(_key)) {
                    _values = _result.getValue(_key)
                } else {
                    _values = ArrayList<Song>()
                    _result.put(_key, _values)
                }
                if (_cursor.isNull(_cursorIndexOfSongId) && _cursor.isNull(_cursorIndexOfArtistKey)) {
                    continue
                }
                val _value: Song
                val _tmpSongId: String
                _tmpSongId = _cursor.getString(_cursorIndexOfSongId)
                val _tmpArtistKey: String
                _tmpArtistKey = _cursor.getString(_cursorIndexOfArtistKey)
                _value = Song(_tmpSongId,_tmpArtistKey)
                _values.add(_value)
            }
            return _result
        } finally {
            _cursor.close()
            _statement.release()
        }
    }

    public override fun getArtistSongCount(): Map<Artist, Int> {
        val _sql: String =
            "SELECT Artist.*, COUNT(songId) as songCount FROM Artist JOIN Song ON Artist.artistId = Song.artistKey GROUP BY artistId"
        val _statement: RoomSQLiteQuery = acquire(_sql, 0)
        __db.assertNotSuspendingTransaction()
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
            val _cursorIndexOfArtistId: Int = getColumnIndexOrThrow(_cursor, "artistId")
            val _columnIndexOfSongCount: Int = getColumnIndexOrThrow(_cursor, "songCount")
            val _result: MutableMap<Artist, Int> = LinkedHashMap<Artist, Int>()
            while (_cursor.moveToNext()) {
                val _key: Artist
                val _tmpArtistId: String
                _tmpArtistId = _cursor.getString(_cursorIndexOfArtistId)
                _key = Artist(_tmpArtistId)
                if (_cursor.isNull(_columnIndexOfSongCount)) {
                    error("The column(s) of the map value object of type 'Int' are NULL but the map's value type argument expect it to be NON-NULL")
                }
                val _value: Int
                val _tmp: Int
                _tmp = _cursor.getInt(_columnIndexOfSongCount)
                _value = _tmp
                if (!_result.containsKey(_key)) {
                    _result.put(_key, _value)
                }
            }
            return _result
        } finally {
            _cursor.close()
            _statement.release()
        }
    }

    public override fun getArtistWithSongIds(): Map<Artist, List<String>> {
        val _sql: String = "SELECT * FROM Artist JOIN Song ON Artist.artistId = Song.artistKey"
        val _statement: RoomSQLiteQuery = acquire(_sql, 0)
        __db.assertNotSuspendingTransaction()
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
            val _cursorIndexOfArtistId: Int = getColumnIndexOrThrow(_cursor, "artistId")
            val _columnIndexOfSongId: Int = getColumnIndexOrThrow(_cursor, "songId")
            val _result: MutableMap<Artist, MutableList<String>> =
                LinkedHashMap<Artist, MutableList<String>>()
            while (_cursor.moveToNext()) {
                val _key: Artist
                val _tmpArtistId: String
                _tmpArtistId = _cursor.getString(_cursorIndexOfArtistId)
                _key = Artist(_tmpArtistId)
                val _values: MutableList<String>
                if (_result.containsKey(_key)) {
                    _values = _result.getValue(_key)
                } else {
                    _values = ArrayList<String>()
                    _result.put(_key, _values)
                }
                if (_cursor.isNull(_columnIndexOfSongId)) {
                    continue
                }
                val _value: String
                _value = _cursor.getString(_columnIndexOfSongId)
                _values.add(_value)
            }
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