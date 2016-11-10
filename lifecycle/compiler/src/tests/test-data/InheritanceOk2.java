package foo;

import static com.android.support.lifecycle.Lifecycle.ON_STOP;

import com.android.support.lifecycle.LifecycleProvider;
import com.android.support.lifecycle.OnLifecycleEvent;

class InheritanceOk2Base {
    @OnLifecycleEvent(ON_STOP)
    public void onStop(LifecycleProvider provider, int lastEvent) {
    }
}

class InheritanceOk2Derived extends InheritanceOk2Base {
    @OnLifecycleEvent(ON_STOP)
    public void onStop2(LifecycleProvider provider, int lastEvent) {
    }
}