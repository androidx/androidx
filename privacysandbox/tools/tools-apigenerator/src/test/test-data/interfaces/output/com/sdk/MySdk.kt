package com.sdk

public interface MySdk {
    public suspend fun getInterface(): MyInterface

    public suspend fun maybeGetInterface(): MyInterface?
}
