//ComputableLiveData interface for tests
package androidx.lifecycle;
import androidx.lifecycle.LiveData;
import java.util.concurrent.Executor;
public abstract class ComputableLiveData<T> {
    public ComputableLiveData(){}
    public ComputableLiveData(Executor e){}
    abstract protected T compute();
    public LiveData<T> getLiveData() {return null;}
    public void invalidate() {}
}
