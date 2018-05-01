package io.ghostwriter.rt.snaperr.tracker;

import io.ghostwriter.rt.snaperr.api.TrackedScope;

import java.util.Iterator;

public interface ReferenceTracker {

    <T> void track(String variableName, T variableReference);

    void pushScope(Object source, String methodName);

    void popScope();

    boolean isEmpty();

    TrackedScope currentScope();

    Iterator<TrackedScope> scopes();

}
