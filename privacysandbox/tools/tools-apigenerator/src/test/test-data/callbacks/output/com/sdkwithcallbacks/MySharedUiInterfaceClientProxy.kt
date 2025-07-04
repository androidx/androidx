package com.sdkwithcallbacks

import android.os.Bundle
import androidx.privacysandbox.ui.client.SharedUiAdapterFactory
import androidx.privacysandbox.ui.core.SharedUiAdapter
import java.util.concurrent.Executor

public class MySharedUiInterfaceClientProxy(
    public val remote: IMySharedUiInterface,
    public val coreLibInfo: Bundle,
) : MySharedUiInterface {
    public val sharedUiAdapter: SharedUiAdapter =
            SharedUiAdapterFactory.createFromCoreLibInfo(coreLibInfo)

    public override fun doUiStuff() {
        remote.doUiStuff()
    }

    public override fun openSession(clientExecutor: Executor, client: SharedUiAdapter.SessionClient) {
        sharedUiAdapter.openSession(clientExecutor, client)
    }
}
