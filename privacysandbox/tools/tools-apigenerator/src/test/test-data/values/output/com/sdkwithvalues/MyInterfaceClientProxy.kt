package com.sdkwithvalues

public class MyInterfaceClientProxy(
    public val remote: IMyInterface,
) : MyInterface {
    public override fun doStuff() {
        remote.doStuff()
    }
}
