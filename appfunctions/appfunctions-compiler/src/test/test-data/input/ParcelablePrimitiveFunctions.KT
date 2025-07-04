package com.testdata

import android.app.PendingIntent
import android.content.Intent
import androidx.appfunctions.service.AppFunction
import androidx.appfunctions.AppFunctionContext

class ParcelablePrimitiveFunctions {
    @AppFunction
    fun pendingIntentInput(appFunctionContext: AppFunctionContext, pendingIntent: PendingIntent) {}

    @AppFunction
    fun pendingIntentOutput(appFunctionContext: AppFunctionContext): PendingIntent {
        val intent = Intent()
        return PendingIntent.getActivity(
            appFunctionContext.context,
            1,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
    }
}
