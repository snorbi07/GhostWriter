package io.ghostwriter.rt.snaperr.core;

import io.ghostwriter.rt.snaperr.api.*;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

final public class StringSerializer implements TriggerSerializer {

    @Override
    public String serializeTrigger(ErrorTrigger errorTrigger) {
        final int INITIAL_CAPACITY = 256;
        final StringBuilder sb = new StringBuilder(INITIAL_CAPACITY);
        sb.append("Snaperr - state snapshot of error '").append(errorTrigger.getThrowable().toString()).append("': [\n");

        final Iterator<TrackedScope> scopes = errorTrigger.scopes();
        while (scopes.hasNext()) {
            final TrackedScope currentScope = scopes.next();
            serializeScope(currentScope, sb);
            sb.append("\n");
        }

        sb.append("]");

        return sb.toString();
    }

    @Override
    public String serializeTrigger(TimeoutTrigger timeoutTrigger) {
        // FIXME: add implementation for this as well
        throw new UnsupportedOperationException("Implementation missing!");
    }

    private StringBuilder serializeScope(TrackedScope currentScope, StringBuilder sb) {
        final String source = String.valueOf(currentScope.getSource());
        final String methodName = currentScope.getMethodName();

        sb.append("\t").append(source).append(".").append(methodName).append(" {\n");
        final Set<Map.Entry<String, TrackedValue>> entries = currentScope.getReferences().entrySet();
        for (Map.Entry<String, TrackedValue> entry : entries) {
            final String variableName = entry.getKey();
            final String variableValue = String.valueOf(entry.getValue().getValue());
            sb.append("\t\t").append(variableName).append("=").append(variableValue);
            sb.append("\n");
        }
        sb.append("\t}");

        return sb;
    }

}
