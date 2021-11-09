package foo;

import static androidx.lifecycle.Lifecycle.Event.ON_STOP;

import androidx.lifecycle.Lifecycle.Event;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;

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
