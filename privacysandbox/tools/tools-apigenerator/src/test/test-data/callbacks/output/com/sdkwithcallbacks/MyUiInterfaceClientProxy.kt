package com.sdkwithcallbacks

import android.content.Context
import android.os.Bundle
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

    public override fun doUiStuff(): Unit {
        remote.doUiStuff()
    }

    public override fun openSession(
        context: Context,
        initialWidth: Int,
        initialHeight: Int,
        isZOrderOnTop: Boolean,
        clientExecutor: Executor,
        client: SandboxedUiAdapter.SessionClient,
    ): Unit {
        sandboxedUiAdapter.openSession(context, initialWidth, initialHeight, isZOrderOnTop,
                clientExecutor, client)
    }
}
