package codetree.core;

public interface CodeFragment {
    public abstract boolean contains(CodeFragment other);

    public abstract boolean equals(Object other);

    public abstract byte getVlabel();

    public abstract byte[] getelabel();
}
