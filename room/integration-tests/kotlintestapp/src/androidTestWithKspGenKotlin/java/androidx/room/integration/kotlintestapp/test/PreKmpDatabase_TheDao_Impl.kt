@file:Suppress("DEPRECATION", "ktlint") // Due to old entity adapter usage.

package androidx.room.integration.kotlintestapp.test

import android.database.Cursor
import androidx.room.EntityInsertionAdapter
import androidx.room.RoomDatabase
import androidx.room.RoomSQLiteQuery
import androidx.room.RoomSQLiteQuery.Companion.acquire
import androidx.room.util.getColumnIndexOrThrow
import androidx.sqlite.db.SupportSQLiteStatement
import java.lang.Class
import java.util.ArrayList
import javax.`annotation`.processing.Generated
import kotlin.ByteArray
import kotlin.Int
import kotlin.Lazy
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.jvm.JvmStatic

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(
    names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "RestrictedApiAndroidX"]
)
public class PreKmpDatabase_TheDao_Impl(
    __db: RoomDatabase,
) : PreKmpDatabase.TheDao {
    private val __db: RoomDatabase

    private val __insertionAdapterOfTheEntity: EntityInsertionAdapter<PreKmpDatabase.TheEntity>

    private val __theConverter: Lazy<PreKmpDatabase.TheConverter> = lazy {
        checkNotNull(__db.getTypeConverter(PreKmpDatabase.TheConverter::class.java))
    }

    init {
        this.__db = __db
        this.__insertionAdapterOfTheEntity =
            object : EntityInsertionAdapter<PreKmpDatabase.TheEntity>(__db) {
                protected override fun createQuery(): String =
                    "INSERT OR ABORT INTO `TheEntity` (`id`,`text`,`custom`) VALUES (?,?,?)"

                protected override fun bind(
                    statement: SupportSQLiteStatement,
                    entity: PreKmpDatabase.TheEntity
                ) {
                    statement.bindLong(1, entity.id)
                    statement.bindString(2, entity.text)
                    val _tmp: ByteArray = __theConverter().fromCustomData(entity.custom)
                    statement.bindBlob(3, _tmp)
                }
            }
    }

    public override fun insert(it: PreKmpDatabase.TheEntity) {
        __db.assertNotSuspendingTransaction()
        __db.beginTransaction()
        try {
            __insertionAdapterOfTheEntity.insert(it)
            __db.setTransactionSuccessful()
        } finally {
            __db.endTransaction()
        }
    }

    public override fun query(): List<PreKmpDatabase.TheEntity> {
        val _sql: String = "SELECT * FROM TheEntity"
        val _statement: RoomSQLiteQuery = acquire(_sql, 0)
        __db.assertNotSuspendingTransaction()
        val _cursor: Cursor = androidx.room.util.query(__db, _statement, false, null)
        try {
            val _cursorIndexOfId: Int = getColumnIndexOrThrow(_cursor, "id")
            val _cursorIndexOfText: Int = getColumnIndexOrThrow(_cursor, "text")
            val _cursorIndexOfCustom: Int = getColumnIndexOrThrow(_cursor, "custom")
            val _result: MutableList<PreKmpDatabase.TheEntity> =
                ArrayList<PreKmpDatabase.TheEntity>(_cursor.getCount())
            while (_cursor.moveToNext()) {
                val _item: PreKmpDatabase.TheEntity
                val _tmpId: Long
                _tmpId = _cursor.getLong(_cursorIndexOfId)
                val _tmpText: String
                _tmpText = _cursor.getString(_cursorIndexOfText)
                val _tmpCustom: PreKmpDatabase.CustomData
                val _tmp: ByteArray
                _tmp = _cursor.getBlob(_cursorIndexOfCustom)
                _tmpCustom = __theConverter().toCustomData(_tmp)
                _item = PreKmpDatabase.TheEntity(_tmpId, _tmpText, _tmpCustom)
                _result.add(_item)
            }
            return _result
        } finally {
            _cursor.close()
            _statement.release()
        }
    }

    private fun __theConverter(): PreKmpDatabase.TheConverter = __theConverter.value

    public companion object {
        @JvmStatic
        public fun getRequiredConverters(): List<Class<*>> =
            listOf(PreKmpDatabase.TheConverter::class.java)
    }
}
