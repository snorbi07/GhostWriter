package io.ghostwriter.rt.snaperr.api;


import java.util.Map;

public final class TrackedScope {

    private Object source;

    private String methodName;

    private Map<String, TrackedValue> references;

    public TrackedScope(Object source, String methodName, Map<String, TrackedValue> references) {
        this.source = source;
        this.methodName = methodName;
        this.references = references;
    }

    public Object getSource() {
        return source;
    }

    public void setSource(Object source) {
        this.source = source;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public Map<String, TrackedValue> getReferences() {
        return references;
    }

    public void setReferences(Map<String, TrackedValue> references) {
        this.references = references;
    }
}
