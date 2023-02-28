package com.sdk

public interface MySdk {
    public suspend fun getInterface(): MyInterface

    public suspend fun getUiInterface(): MySecondInterface

    public suspend fun maybeGetInterface(): MyInterface?
}
