import androidx.room.RoomDatabase
import androidx.room.withTransaction
import java.lang.Class
import javax.`annotation`.processing.Generated
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
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

    public override fun baseConcrete(): Unit {
        __db.beginTransaction()
        try {
            super@MyDao_Impl.baseConcrete()
            __db.setTransactionSuccessful()
        } finally {
            __db.endTransaction()
        }
    }

    public override suspend fun baseSuspendConcrete(): Unit {
        __db.withTransaction {
            super@MyDao_Impl.baseSuspendConcrete()
        }
    }

    public override fun concrete(): Unit {
        __db.beginTransaction()
        try {
            super@MyDao_Impl.concrete()
            __db.setTransactionSuccessful()
        } finally {
            __db.endTransaction()
        }
    }

    public override fun concreteInternal(): Unit {
        __db.beginTransaction()
        try {
            super@MyDao_Impl.concreteInternal()
            __db.setTransactionSuccessful()
        } finally {
            __db.endTransaction()
        }
    }

    public override suspend fun suspendConcrete(): Unit {
        __db.withTransaction {
            super@MyDao_Impl.suspendConcrete()
        }
    }

    public companion object {
        @JvmStatic
        public fun getRequiredConverters(): List<Class<*>> = emptyList()
    }
}