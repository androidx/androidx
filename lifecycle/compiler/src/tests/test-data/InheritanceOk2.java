package foo;

import static com.android.support.lifecycle.Lifecycle.ON_STOP;

import com.android.support.lifecycle.LifecycleOwner;
import com.android.support.lifecycle.OnLifecycleEvent;

class InheritanceOk2Base {
    @OnLifecycleEvent(ON_STOP)
    public void onStop(LifecycleOwner provider, int lastEvent) {
    }
}

class InheritanceOk2Derived extends InheritanceOk2Base {
    @OnLifecycleEvent(ON_STOP)
    public void onStop2(LifecycleOwner provider, int lastEvent) {
    }
}