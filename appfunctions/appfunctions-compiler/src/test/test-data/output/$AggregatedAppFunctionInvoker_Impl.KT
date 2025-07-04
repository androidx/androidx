package androidx.appfunctions.service.`internal`

import com.testdata.`$AllPrimitiveInputFunctions_AppFunctionInvoker`
import com.testdata.`$SimpleFunction_AppFunctionInvoker`
import javax.`annotation`.processing.Generated
import kotlin.collections.List

@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$AggregatedAppFunctionInvoker_Impl` : AggregatedAppFunctionInvoker() {
  override val invokers: List<AppFunctionInvoker> = listOf(
    `$AllPrimitiveInputFunctions_AppFunctionInvoker`(),
    `$SimpleFunction_AppFunctionInvoker`(),
  )
}
