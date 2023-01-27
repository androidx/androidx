import android.database.Cursor
import android.os.CancellationSignal
import androidx.room.EntityDeletionOrUpdateAdapter
import androidx.room.EntityInsertionAdapter
import androidx.room.EntityUpsertionAdapter
import androidx.room.RoomDatabase
import androidx.room.RoomSQLiteQuery
import androidx.room.RoomSQLiteQuery.Companion.acquire
import androidx.room.guava.GuavaRoom
import androidx.room.util.appendPlaceholders
import androidx.room.util.createCancellationSignal
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.newStringBuilder
import androidx.room.util.query
import androidx.sqlite.db.SupportSQLiteStatement
import com.google.common.util.concurrent.ListenableFuture
import java.lang.Class
import java.lang.StringBuilder
import java.util.concurrent.Callable
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.Long
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

    private val __insertionAdapterOfMyEntity: EntityInsertionAdapter<MyEntity>

    private val __deletionAdapterOfMyEntity: EntityDeletionOrUpdateAdapter<MyEntity>

    private val __updateAdapterOfMyEntity: EntityDeletionOrUpdateAdapter<MyEntity>

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
        this.__deletionAdapterOfMyEntity = object : EntityDeletionOrUpdateAdapter<MyEntity>(__db) {
            protected override fun createQuery(): String = "DELETE FROM `MyEntity` WHERE `pk` = ?"

            protected override fun bind(statement: SupportSQLiteStatement, entity: MyEntity) {
                statement.bindLong(1, entity.pk.toLong())
            }
        }
        this.__updateAdapterOfMyEntity = object : EntityDeletionOrUpdateAdapter<MyEntity>(__db) {
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
        ListenableFuture<List<Long>> = GuavaRoom.createListenableFuture(__db, true, object :
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
        GuavaRoom.createListenableFuture(__db, true, object : Callable<Int> {
            public override fun call(): Int {
                var _total: Int = 0
                __db.beginTransaction()
                try {
                    _total += __deletionAdapterOfMyEntity.handle(entity)
                    __db.setTransactionSuccessful()
                    return _total
                } finally {
                    __db.endTransaction()
                }
            }
        })

    public override fun updateListenableFuture(entity: MyEntity): ListenableFuture<Int> =
        GuavaRoom.createListenableFuture(__db, true, object : Callable<Int> {
            public override fun call(): Int {
                var _total: Int = 0
                __db.beginTransaction()
                try {
                    _total += __updateAdapterOfMyEntity.handle(entity)
                    __db.setTransactionSuccessful()
                    return _total
                } finally {
                    __db.endTransaction()
                }
            }
        })

    public override fun upsertListenableFuture(vararg entities: MyEntity):
        ListenableFuture<List<Long>> = GuavaRoom.createListenableFuture(__db, true, object :
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
        val _stringBuilder: StringBuilder = newStringBuilder()
        _stringBuilder.append("SELECT * FROM MyEntity WHERE pk IN (")
        val _inputSize: Int = arg.size
        appendPlaceholders(_stringBuilder, _inputSize)
        _stringBuilder.append(")")
        val _sql: String = _stringBuilder.toString()
        val _argCount: Int = 0 + _inputSize
        val _statement: RoomSQLiteQuery = acquire(_sql, _argCount)
        var _argIndex: Int = 1
        for (_item: String? in arg) {
            if (_item == null) {
                _statement.bindNull(_argIndex)
            } else {
                _statement.bindString(_argIndex, _item)
            }
            _argIndex++
        }
        val _cancellationSignal: CancellationSignal? = createCancellationSignal()
        return GuavaRoom.createListenableFuture(__db, false, object : Callable<MyEntity> {
            public override fun call(): MyEntity {
                val _cursor: Cursor = query(__db, _statement, false, _cancellationSignal)
                try {
                    val _cursorIndexOfPk: Int = getColumnIndexOrThrow(_cursor, "pk")
                    val _cursorIndexOfOther: Int = getColumnIndexOrThrow(_cursor, "other")
                    val _result: MyEntity
                    if (_cursor.moveToFirst()) {
                        val _tmpPk: Int
                        _tmpPk = _cursor.getInt(_cursorIndexOfPk)
                        val _tmpOther: String
                        _tmpOther = _cursor.getString(_cursorIndexOfOther)
                        _result = MyEntity(_tmpPk,_tmpOther)
                    } else {
                        error("The query result was empty, but expected a single row to return a NON-NULL object of type <MyEntity>.")
                    }
                    return _result
                } finally {
                    _cursor.close()
                }
            }
        }, _statement, true, _cancellationSignal)
    }

    public override fun getListenableFutureNullable(vararg arg: String?):
        ListenableFuture<MyEntity?> {
        val _stringBuilder: StringBuilder = newStringBuilder()
        _stringBuilder.append("SELECT * FROM MyEntity WHERE pk IN (")
        val _inputSize: Int = arg.size
        appendPlaceholders(_stringBuilder, _inputSize)
        _stringBuilder.append(")")
        val _sql: String = _stringBuilder.toString()
        val _argCount: Int = 0 + _inputSize
        val _statement: RoomSQLiteQuery = acquire(_sql, _argCount)
        var _argIndex: Int = 1
        for (_item: String? in arg) {
            if (_item == null) {
                _statement.bindNull(_argIndex)
            } else {
                _statement.bindString(_argIndex, _item)
            }
            _argIndex++
        }
        val _cancellationSignal: CancellationSignal? = createCancellationSignal()
        return GuavaRoom.createListenableFuture(__db, false, object : Callable<MyEntity?> {
            public override fun call(): MyEntity? {
                val _cursor: Cursor = query(__db, _statement, false, _cancellationSignal)
                try {
                    val _cursorIndexOfPk: Int = getColumnIndexOrThrow(_cursor, "pk")
                    val _cursorIndexOfOther: Int = getColumnIndexOrThrow(_cursor, "other")
                    val _result: MyEntity?
                    if (_cursor.moveToFirst()) {
                        val _tmpPk: Int
                        _tmpPk = _cursor.getInt(_cursorIndexOfPk)
                        val _tmpOther: String
                        _tmpOther = _cursor.getString(_cursorIndexOfOther)
                        _result = MyEntity(_tmpPk,_tmpOther)
                    } else {
                        _result = null
                    }
                    return _result
                } finally {
                    _cursor.close()
                }
            }
        }, _statement, true, _cancellationSignal)
    }

    public companion object {
        @JvmStatic
        public fun getRequiredConverters(): List<Class<*>> = emptyList()
    }
}