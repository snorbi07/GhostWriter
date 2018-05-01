package io.ghostwriter.rt.snaperr.tracker;


import io.ghostwriter.rt.snaperr.api.TrackedScope;
import io.ghostwriter.rt.snaperr.api.TrackedValue;

import java.util.*;

public class StackBasedReferenceTracker implements ReferenceTracker {

    private ThreadLocal<CallStackScope> trackedThreadStack = new ThreadLocal<CallStackScope>() {
        @Override
        protected CallStackScope initialValue() {
            CallStackScope trackedCallStackScope = new CallStackScope();
            return trackedCallStackScope;
        }
    };

    @Override
    public <T> void track(String variableName, T variableReference) {
        final CallStackScope trackedScopes = trackedThreadStack.get();
        final TrackedScope currentScope = trackedScopes.peek();
        final Map<String, TrackedValue> references = currentScope.getReferences();
        TrackedValue savedTrackedValue = references.get(variableName);

        if (savedTrackedValue == null) {
            TrackedValue trackedValue = new TrackedValue(Objects.requireNonNull(variableName), variableReference);
            references.put(variableName, trackedValue);
        } else {
            savedTrackedValue.setValue(variableReference);
        }

    }

    @Override
    public void pushScope(Object source, String methodName) {
        final CallStackScope trackedScopes = trackedThreadStack.get();
        trackedScopes.push(source, methodName, new HashMap<String, TrackedValue>());
    }

    @Override
    public void popScope() {
        final CallStackScope trackedScopes = trackedThreadStack.get();
        trackedScopes.pop();
    }

    @Override
    public TrackedScope currentScope() {
        final CallStackScope trackedScopes = trackedThreadStack.get();
        final TrackedScope currentScope = trackedScopes.peek();
        Map<String, TrackedValue> lockedReferences = Collections.unmodifiableMap(currentScope.getReferences());
        return new TrackedScope(currentScope.getSource(), currentScope.getMethodName(), lockedReferences);
    }

    @Override
    public boolean isEmpty() {
        final CallStackScope trackedScopes = trackedThreadStack.get();
        return trackedScopes.isEmpty();
    }

    /**
     * Will return a reverse order iterator of a copy list
     * therefore remove() operation is unsupported
     */
    @Override
    public Iterator<TrackedScope> scopes() {
        return trackedThreadStack.get().iterator();
    }
}
