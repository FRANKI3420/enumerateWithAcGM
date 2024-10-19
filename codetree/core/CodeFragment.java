package codetree.core;

public interface CodeFragment {

    public abstract byte getVlabel();

    public abstract byte[] getelabel();

    public abstract boolean contains(CodeFragment other);

    public abstract boolean bigger(CodeFragment other);

    public abstract boolean equals(Object other);

    public abstract String toString();
    public abstract boolean getIsConnected();
}
