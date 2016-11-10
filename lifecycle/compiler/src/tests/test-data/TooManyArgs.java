package foo;

import static com.android.support.lifecycle.Lifecycle.ON_STOP;

import com.android.support.lifecycle.LifecycleProvider;
import com.android.support.lifecycle.OnLifecycleEvent;

public class TooManyArgs {
    @OnLifecycleEvent(ON_STOP)
    public void onStop(LifecycleProvider provider, int lastEvent, int x) {
    }
}