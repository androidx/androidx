package com.sdk

public interface MyInterface {
    public suspend fun add(x: Int, y: Int): Int

    public fun doSomething(firstInterface: MyInterface, secondInterface: MySecondInterface): Unit

    public fun doSomethingWithNullableInterface(maybeInterface: MySecondInterface?): Unit
}
