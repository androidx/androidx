import androidx.room.RoomDatabase
import androidx.room.util.performBlocking
import androidx.room.util.performInTransactionSuspending
import javax.`annotation`.processing.Generated
import kotlin.Long
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class MyDao_Impl(
  __db: RoomDatabase,
) : MyDao() {
  private val __db: RoomDatabase
  init {
    this.__db = __db
  }

  public override fun baseConcrete(): Unit = performBlocking(__db, false, true) { _ ->
    super@MyDao_Impl.baseConcrete()
  }

  public override suspend fun baseSuspendConcrete(): Unit = performInTransactionSuspending(__db) {
    super@MyDao_Impl.baseSuspendConcrete()
  }

  public override fun concrete(): Unit = performBlocking(__db, false, true) { _ ->
    super@MyDao_Impl.concrete()
  }

  internal override fun concreteInternal(): Unit = performBlocking(__db, false, true) { _ ->
    super@MyDao_Impl.concreteInternal()
  }

  public override suspend fun suspendConcrete(): Unit = performInTransactionSuspending(__db) {
    super@MyDao_Impl.suspendConcrete()
  }

  public override fun concreteWithVararg(vararg arr: Long): Unit = performBlocking(__db, false,
      true) { _ ->
    super@MyDao_Impl.concreteWithVararg(*arr)
  }

  public override suspend fun suspendConcreteWithVararg(vararg arr: Long): Unit =
      performInTransactionSuspending(__db) {
    super@MyDao_Impl.suspendConcreteWithVararg(*arr)
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
