package foo;

import android.arch.lifecycle.GenericLifecycleObserver;
import android.arch.lifecycle.LifecycleOwner;

import java.lang.Object;
import java.lang.Override;

public class InheritanceOk2Base_LifecycleAdapter implements GenericLifecycleObserver {
    final InheritanceOk2Base mReceiver;

    InheritanceOk2Base_LifecycleAdapter(InheritanceOk2Base receiver) {
        this.mReceiver = receiver;
    }

    @Override
    public void onStateChanged(LifecycleOwner owner, int event) {
        if ((event & 8192) != 0) {
            mReceiver.onStop(owner, event);
        }
    }

    public Object getReceiver() {
        return mReceiver;
    }
}
