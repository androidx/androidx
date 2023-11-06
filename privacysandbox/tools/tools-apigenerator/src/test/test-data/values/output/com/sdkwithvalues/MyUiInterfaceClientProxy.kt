package com.sdkwithvalues

import android.content.Context
import android.os.Bundle
import android.os.IBinder
import androidx.privacysandbox.ui.client.SandboxedUiAdapterFactory
import androidx.privacysandbox.ui.core.SandboxedUiAdapter
import androidx.privacysandbox.ui.core.SandboxedUiAdapter.SessionClient
import java.util.concurrent.Executor

public class MyUiInterfaceClientProxy(
    public val remote: IMyUiInterface,
    public val coreLibInfo: Bundle,
) : MyUiInterface {
    public val sandboxedUiAdapter: SandboxedUiAdapter =
            SandboxedUiAdapterFactory.createFromCoreLibInfo(coreLibInfo)

    public override fun doUiStuff() {
        remote.doUiStuff()
    }

    public override fun openSession(
        context: Context,
        windowInputToken: IBinder,
        initialWidth: Int,
        initialHeight: Int,
        isZOrderOnTop: Boolean,
        clientExecutor: Executor,
        client: SandboxedUiAdapter.SessionClient,
    ) {
        sandboxedUiAdapter.openSession(context, windowInputToken, initialWidth, initialHeight,
                isZOrderOnTop, clientExecutor, client)
    }
}
