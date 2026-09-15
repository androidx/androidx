package com.testdata

import android.app.PendingIntent
import android.content.Intent
import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionContext

class ParcelablePrimitiveFunctions {
    @AppFunctionDeclaration
    fun pendingIntentInput(appFunctionContext: AppFunctionContext, pendingIntent: PendingIntent) {}

    @AppFunctionDeclaration
    fun listPendingIntentInput(
        appFunctionContext: AppFunctionContext,
        listPendingIntent: List<PendingIntent>
    ) {}

    @AppFunctionDeclaration
    fun pendingIntentOutput(appFunctionContext: AppFunctionContext): PendingIntent {
        val intent = Intent()
        return PendingIntent.getActivity(
            appFunctionContext.context,
            1,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
    }


    @AppFunctionDeclaration
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
