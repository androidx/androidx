package com.sdk

import android.content.Context
import androidx.privacysandbox.ui.core.SandboxedUiAdapter
import androidx.privacysandbox.ui.core.SandboxedUiAdapter.SessionClient
import java.util.concurrent.Executor

public class MySecondInterfaceClientProxy(
    public val remote: IMySecondInterface,
    public val sandboxedUiAdapter: SandboxedUiAdapter,
) : MySecondInterface {
    public override fun doStuff(): Unit {
        remote.doStuff()
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
