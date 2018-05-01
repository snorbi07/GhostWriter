package io.ghostwriter.rt.snaperr;

import io.ghostwriter.Tracer;

public class DelegatingTracer implements Tracer {

    private Tracer otherTracer;

    public void entering(Object source, String method, Object... params) {
        otherTracer.entering(source, method, params);
    }

    public void exiting(Object source, String method) {
        otherTracer.exiting(source, method);
    }

    public void valueChange(Object source, String method, String variable, Object value) {
        otherTracer.valueChange(source, method, variable, value);
    }

    public <T> void returning(Object source, String method, T returnValue) {
        otherTracer.returning(source, method, returnValue);
    }

    public void onError(Object source, String method, Throwable error) {
        otherTracer.onError(source, method, error);
    }

    public void timeout(Object source, String method, long timeoutThreshold, long timeout) {
        otherTracer.timeout(source, method, timeoutThreshold, timeout);
    }

    public void setOtherTracer(Tracer tracer) {
        this.otherTracer = tracer;
    }

}
