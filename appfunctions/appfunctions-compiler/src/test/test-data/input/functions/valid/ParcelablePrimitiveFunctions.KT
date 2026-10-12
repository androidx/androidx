package com.testdata

import android.app.PendingIntent
import android.content.Intent
import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionContext

class ParcelablePrimitiveFunctions {
    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun pendingIntentInput(appFunctionContext: AppFunctionContext, pendingIntent: PendingIntent) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun listPendingIntentInput(
        appFunctionContext: AppFunctionContext,
        listPendingIntent: List<PendingIntent>
    ) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun pendingIntentOutput(appFunctionContext: AppFunctionContext): PendingIntent {
        val intent = Intent()
        return PendingIntent.getActivity(
            appFunctionContext.context,
            1,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
    }


    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun listPendingIntentOutput(appFunctionContext: AppFunctionContext): List<PendingIntent> {
        val intent = Intent()
        return listOf(
            PendingIntent.getActivity(
                appFunctionContext.context,
                1,
                intent,
                PendingIntent.FLAG_IMMUTABLE
            )
        )
    }
}
