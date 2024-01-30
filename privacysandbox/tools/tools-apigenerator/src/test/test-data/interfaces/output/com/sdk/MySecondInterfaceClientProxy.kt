package com.sdk

import android.content.Context
import android.os.Bundle
import android.os.IBinder
import androidx.privacysandbox.ui.client.SandboxedUiAdapterFactory
import androidx.privacysandbox.ui.core.SandboxedUiAdapter
import androidx.privacysandbox.ui.core.SandboxedUiAdapter.SessionClient
import java.util.concurrent.Executor

public class MySecondInterfaceClientProxy(
    public val remote: IMySecondInterface,
    public val coreLibInfo: Bundle,
) : MySecondInterface {
    public val sandboxedUiAdapter: SandboxedUiAdapter =
            SandboxedUiAdapterFactory.createFromCoreLibInfo(coreLibInfo)

    public override fun doStuff() {
        remote.doStuff()
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
