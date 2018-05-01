package io.ghostwriter.rt.snaperr;

/**
 * A test value class that is mutable
 *
 */
class MutableValueClass {

    private String stringValue;

    private int primitiveValue;

    public MutableValueClass(String stringValue, int primitiveValue) {
        this.stringValue = stringValue;
        this.primitiveValue = primitiveValue;
    }

    public String getStringValue() {
        return stringValue;
    }

    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    public int getPrimitiveValue() {
        return primitiveValue;
    }

    public void setPrimitiveValue(int primitiveValue) {
        this.primitiveValue = primitiveValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MutableValueClass that = (MutableValueClass) o;

        if (primitiveValue != that.primitiveValue) return false;
        return !(stringValue != null ? !stringValue.equals(that.stringValue) : that.stringValue != null);

    }

    @Override
    public int hashCode() {
        int result = stringValue != null ? stringValue.hashCode() : 0;
        result = 31 * result + primitiveValue;
        return result;
    }

    @Override
    public String toString() {
        return "MutableValueClass{" +
                "stringValue='" + stringValue + '\'' +
                ", primitiveValue=" + primitiveValue +
                '}';
    }
}

