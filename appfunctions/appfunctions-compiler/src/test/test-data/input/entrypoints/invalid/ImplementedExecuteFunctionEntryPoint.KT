package com.testdata

import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.ExecuteAppFunctionRequest
import androidx.appfunctions.ExecuteAppFunctionResponse
import androidx.appfunctions.AppFunctionServiceEntryPoint
import androidx.appfunctions.AppFunction
import java.util.function.Consumer
import android.os.CancellationSignal

@AppFunctionServiceEntryPoint("MySimpleService", "app_functions_v2.xml")
abstract class ImplementedExecuteFunctionEntryPoint: AppFunctionService() {
    @AppFunction fun simpleFunction() {}

    override fun onExecuteFunction(
        request: ExecuteAppFunctionRequest,
        cancellationSignal: CancellationSignal,
        callback: Consumer<ExecuteAppFunctionResponse>
    ) {
      throw NotImplementedError()
    }
}
