package util.graph;

public abstract class FileGraph<A, V> {
    public abstract boolean contains(A key);
    public abstract void add(A key);
    public abstract V get(A key);
    public abstract void setRoot(String root);
    public abstract String getErrors();
}
