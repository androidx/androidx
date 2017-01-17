//ComputableLiveData interface for tests
package com.android.support.lifecycle;
import com.android.support.lifecycle.LiveData;
public abstract class ComputableLiveData<T> {
    public ComputableLiveData(){}
    abstract protected T compute();
    public LiveData<T> getLiveData() {return null;}
    public void invalidate() {}
}