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

    public override fun singleNested(): Map<Artist, Map<Album, List<Song>>> {
        val _sql: String =
            "SELECT * FROM Artist JOIN (Album JOIN Song ON Album.albumName = Song.album) ON Artist.artistName = Album.albumArtist"
        val _statement: RoomSQLiteQuery = acquire(_sql, 0)
        __db.assertNotSuspendingTransaction()
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
            val _cursorIndexOfArtistId: Int = getColumnIndexOrThrow(_cursor, "artistId")
            val _cursorIndexOfArtistName: Int = getColumnIndexOrThrow(_cursor, "artistName")
            val _cursorIndexOfAlbumId: Int = getColumnIndexOrThrow(_cursor, "albumId")
            val _cursorIndexOfAlbumName: Int = getColumnIndexOrThrow(_cursor, "albumName")
            val _cursorIndexOfAlbumArtist: Int = getColumnIndexOrThrow(_cursor, "albumArtist")
            val _cursorIndexOfSongId: Int = getColumnIndexOrThrow(_cursor, "songId")
            val _cursorIndexOfAlbum: Int = getColumnIndexOrThrow(_cursor, "album")
            val _cursorIndexOfSongArtist: Int = getColumnIndexOrThrow(_cursor, "songArtist")
            val _result: MutableMap<Artist, MutableMap<Album, MutableList<Song>>> =
                LinkedHashMap<Artist, MutableMap<Album, MutableList<Song>>>()
            while (_cursor.moveToNext()) {
                val _key: Artist
                val _tmpArtistId: String
                _tmpArtistId = _cursor.getString(_cursorIndexOfArtistId)
                val _tmpArtistName: String
                _tmpArtistName = _cursor.getString(_cursorIndexOfArtistName)
                _key = Artist(_tmpArtistId,_tmpArtistName)
                val _values: MutableMap<Album, MutableList<Song>>
                if (_result.containsKey(_key)) {
                    _values = _result.getValue(_key)
                } else {
                    _values = LinkedHashMap<Album, MutableList<Song>>()
                    _result.put(_key, _values)
                }
                if (_cursor.isNull(_cursorIndexOfAlbumId) && _cursor.isNull(_cursorIndexOfAlbumName) &&
                    _cursor.isNull(_cursorIndexOfAlbumArtist)) {
                    continue
                }
                val _key_1: Album
                val _tmpAlbumId: String
                _tmpAlbumId = _cursor.getString(_cursorIndexOfAlbumId)
                val _tmpAlbumName: String
                _tmpAlbumName = _cursor.getString(_cursorIndexOfAlbumName)
                val _tmpAlbumArtist: String
                _tmpAlbumArtist = _cursor.getString(_cursorIndexOfAlbumArtist)
                _key_1 = Album(_tmpAlbumId,_tmpAlbumName,_tmpAlbumArtist)
                val _values_1: MutableList<Song>
                if (_values.containsKey(_key_1)) {
                    _values_1 = _values.getValue(_key_1)
                } else {
                    _values_1 = ArrayList<Song>()
                    _values.put(_key_1, _values_1)
                }
                if (_cursor.isNull(_cursorIndexOfSongId) && _cursor.isNull(_cursorIndexOfAlbum) &&
                    _cursor.isNull(_cursorIndexOfSongArtist)) {
                    continue
                }
                val _value: Song
                val _tmpSongId: String
                _tmpSongId = _cursor.getString(_cursorIndexOfSongId)
                val _tmpAlbum: String
                _tmpAlbum = _cursor.getString(_cursorIndexOfAlbum)
                val _tmpSongArtist: String
                _tmpSongArtist = _cursor.getString(_cursorIndexOfSongArtist)
                _value = Song(_tmpSongId,_tmpAlbum,_tmpSongArtist)
                _values_1.add(_value)
            }
            return _result
        } finally {
            _cursor.close()
            _statement.release()
        }
    }

    public override fun doubleNested(): Map<Playlist, Map<Artist, Map<Album, List<Song>>>> {
        val _sql: String =
            "SELECT * FROM Playlist JOIN (Artist JOIN (Album JOIN Song ON Album.albumName = Song.album) ON Artist.artistName = Album.albumArtist)ON Playlist.playlistArtist = Artist.artistName"
        val _statement: RoomSQLiteQuery = acquire(_sql, 0)
        __db.assertNotSuspendingTransaction()
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
            val _cursorIndexOfPlaylistId: Int = getColumnIndexOrThrow(_cursor, "playlistId")
            val _cursorIndexOfPlaylistArtist: Int = getColumnIndexOrThrow(_cursor, "playlistArtist")
            val _cursorIndexOfArtistId: Int = getColumnIndexOrThrow(_cursor, "artistId")
            val _cursorIndexOfArtistName: Int = getColumnIndexOrThrow(_cursor, "artistName")
            val _cursorIndexOfAlbumId: Int = getColumnIndexOrThrow(_cursor, "albumId")
            val _cursorIndexOfAlbumName: Int = getColumnIndexOrThrow(_cursor, "albumName")
            val _cursorIndexOfAlbumArtist: Int = getColumnIndexOrThrow(_cursor, "albumArtist")
            val _cursorIndexOfSongId: Int = getColumnIndexOrThrow(_cursor, "songId")
            val _cursorIndexOfAlbum: Int = getColumnIndexOrThrow(_cursor, "album")
            val _cursorIndexOfSongArtist: Int = getColumnIndexOrThrow(_cursor, "songArtist")
            val _result: MutableMap<Playlist, MutableMap<Artist, MutableMap<Album, MutableList<Song>>>> =
                LinkedHashMap<Playlist, MutableMap<Artist, MutableMap<Album, MutableList<Song>>>>()
            while (_cursor.moveToNext()) {
                val _key: Playlist
                val _tmpPlaylistId: String
                _tmpPlaylistId = _cursor.getString(_cursorIndexOfPlaylistId)
                val _tmpPlaylistArtist: String
                _tmpPlaylistArtist = _cursor.getString(_cursorIndexOfPlaylistArtist)
                _key = Playlist(_tmpPlaylistId,_tmpPlaylistArtist)
                val _values: MutableMap<Artist, MutableMap<Album, MutableList<Song>>>
                if (_result.containsKey(_key)) {
                    _values = _result.getValue(_key)
                } else {
                    _values = LinkedHashMap<Artist, MutableMap<Album, MutableList<Song>>>()
                    _result.put(_key, _values)
                }
                if (_cursor.isNull(_cursorIndexOfArtistId) && _cursor.isNull(_cursorIndexOfArtistName)) {
                    continue
                }
                val _key_1: Artist
                val _tmpArtistId: String
                _tmpArtistId = _cursor.getString(_cursorIndexOfArtistId)
                val _tmpArtistName: String
                _tmpArtistName = _cursor.getString(_cursorIndexOfArtistName)
                _key_1 = Artist(_tmpArtistId,_tmpArtistName)
                val _values_1: MutableMap<Album, MutableList<Song>>
                if (_values.containsKey(_key_1)) {
                    _values_1 = _values.getValue(_key_1)
                } else {
                    _values_1 = LinkedHashMap<Album, MutableList<Song>>()
                    _values.put(_key_1, _values_1)
                }
                if (_cursor.isNull(_cursorIndexOfAlbumId) && _cursor.isNull(_cursorIndexOfAlbumName) &&
                    _cursor.isNull(_cursorIndexOfAlbumArtist)) {
                    continue
                }
                val _key_2: Album
                val _tmpAlbumId: String
                _tmpAlbumId = _cursor.getString(_cursorIndexOfAlbumId)
                val _tmpAlbumName: String
                _tmpAlbumName = _cursor.getString(_cursorIndexOfAlbumName)
                val _tmpAlbumArtist: String
                _tmpAlbumArtist = _cursor.getString(_cursorIndexOfAlbumArtist)
                _key_2 = Album(_tmpAlbumId,_tmpAlbumName,_tmpAlbumArtist)
                val _values_2: MutableList<Song>
                if (_values_1.containsKey(_key_2)) {
                    _values_2 = _values_1.getValue(_key_2)
                } else {
                    _values_2 = ArrayList<Song>()
                    _values_1.put(_key_2, _values_2)
                }
                if (_cursor.isNull(_cursorIndexOfSongId) && _cursor.isNull(_cursorIndexOfAlbum) &&
                    _cursor.isNull(_cursorIndexOfSongArtist)) {
                    continue
                }
                val _value: Song
                val _tmpSongId: String
                _tmpSongId = _cursor.getString(_cursorIndexOfSongId)
                val _tmpAlbum: String
                _tmpAlbum = _cursor.getString(_cursorIndexOfAlbum)
                val _tmpSongArtist: String
                _tmpSongArtist = _cursor.getString(_cursorIndexOfSongArtist)
                _value = Song(_tmpSongId,_tmpAlbum,_tmpSongArtist)
                _values_2.add(_value)
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