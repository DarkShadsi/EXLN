package exln;

public class ExlnArray {
    private final Object[] elements;

    public ExlnArray(Object[] elements) {
        this.elements = elements;
    }
    public Object[] getElements() {
        return elements;
    }
    public Object get(int index) {
        return elements[index];
    }
    public void set(int index, Object value) {
        elements[index] = value;
    }
    public int size() {
        return elements.length;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < elements.length; i++) {
            sb.append(elements[i]);
            if (i < elements.length - 1) sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }
}