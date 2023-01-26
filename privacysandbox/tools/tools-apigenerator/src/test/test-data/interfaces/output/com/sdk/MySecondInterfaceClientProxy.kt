package com.sdk

public class MySecondInterfaceClientProxy(
    public val remote: IMySecondInterface,
) : MySecondInterface {
    public override fun doStuff(): Unit {
        remote.doStuff()
    }
}
