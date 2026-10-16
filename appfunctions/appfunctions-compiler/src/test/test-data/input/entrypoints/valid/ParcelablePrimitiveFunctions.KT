package com.testdata

import android.app.PendingIntent
import android.content.Intent
import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint

@AppFunctionServiceEntryPoint("ParcelablePrimitiveFunctionsService", "app_functions_v2")
abstract class ParcelablePrimitiveFunctions : AppFunctionService() {
    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun pendingIntentInput(pendingIntent: PendingIntent) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun listPendingIntentInput(
        listPendingIntent: List<PendingIntent>
    ) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun pendingIntentOutput(): PendingIntent {
        val intent = Intent()
        return PendingIntent.getActivity(
            this,
            1,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
    }


    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun listPendingIntentOutput(): List<PendingIntent> {
        val intent = Intent()
        return listOf(
            PendingIntent.getActivity(
                this,
                1,
                intent,
                PendingIntent.FLAG_IMMUTABLE
            )
        )
    }
}
