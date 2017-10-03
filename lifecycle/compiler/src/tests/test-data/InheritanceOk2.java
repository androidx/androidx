package foo;

import static android.arch.lifecycle.Lifecycle.Event.ON_STOP;

import android.arch.lifecycle.Lifecycle.Event;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.OnLifecycleEvent;

class InheritanceOk2Base implements LifecycleObserver {
    @OnLifecycleEvent(ON_STOP)
    public void onStop(LifecycleOwner provider) {
    }
}

class InheritanceOk2Derived extends InheritanceOk2Base {
    @OnLifecycleEvent(ON_STOP)
    public void onStop2(LifecycleOwner provider) {
    }
}
