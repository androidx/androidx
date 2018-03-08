//ComputableLiveData interface for tests
package androidx.lifecycle;
import androidx.lifecycle.LiveData;
public abstract class ComputableLiveData<T> {
    public ComputableLiveData(){}
    abstract protected T compute();
    public LiveData<T> getLiveData() {return null;}
    public void invalidate() {}
}
