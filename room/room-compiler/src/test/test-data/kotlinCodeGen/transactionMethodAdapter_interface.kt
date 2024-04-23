import androidx.room.RoomDatabase
import androidx.room.util.performBlocking
import androidx.room.util.performInTransactionSuspending
import javax.`annotation`.processing.Generated
import kotlin.Function0
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.coroutines.SuspendFunction0
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class MyDao_Impl(
  __db: RoomDatabase,
) : MyDao {
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

  public override fun concreteWithReturn(): String = performBlocking(__db, false, true) { _ ->
    super@MyDao_Impl.concreteWithReturn()
  }

  public override fun concreteWithParamsAndReturn(text: String, num: Long): String =
      performBlocking(__db, false, true) { _ ->
    super@MyDao_Impl.concreteWithParamsAndReturn(text, num)
  }

  public override fun concreteWithFunctionalParam(block: Function0<Unit>): Unit =
      performBlocking(__db, false, true) { _ ->
    super@MyDao_Impl.concreteWithFunctionalParam(block)
  }

  public override suspend fun suspendConcrete(): Unit = performInTransactionSuspending(__db) {
    super@MyDao_Impl.suspendConcrete()
  }

  public override suspend fun suspendConcreteWithReturn(): String =
      performInTransactionSuspending(__db) {
    super@MyDao_Impl.suspendConcreteWithReturn()
  }

  public override suspend
      fun suspendConcreteWithSuspendFunctionalParam(block: SuspendFunction0<Unit>): Unit =
      performInTransactionSuspending(__db) {
    super@MyDao_Impl.suspendConcreteWithSuspendFunctionalParam(block)
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
