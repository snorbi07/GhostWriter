package io.ghostwriter.rt.snaperr.api;

import io.ghostwriter.rt.snaperr.tracker.ReferenceTracker;

import java.util.Iterator;
import java.util.Objects;

final public class ErrorTrigger {

    private final ReferenceTracker referenceTracker;

    private final Throwable throwable;

    public ErrorTrigger(ReferenceTracker referenceTracker, Throwable throwable) {
        this.referenceTracker = Objects.requireNonNull(referenceTracker);
        this.throwable = Objects.requireNonNull(throwable);
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public TrackedScope currentScope() {
        return referenceTracker.currentScope();
    }

    public Iterator<TrackedScope> scopes() {
        return referenceTracker.scopes();
    }
}
