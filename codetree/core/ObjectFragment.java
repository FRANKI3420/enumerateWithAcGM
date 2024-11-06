package codetree.core;

public interface ObjectFragment {
    public abstract boolean contains(CodeFragment other);
    public abstract boolean equals(Object other);

    public abstract byte getVlabel();

    public abstract byte[] getelabel();
    public abstract boolean getIsConnected();
    public abstract boolean getAallElabelSame();
    // public abstract boolean getIsCanonical();
    public abstract boolean getIsMaxLabel();
}
