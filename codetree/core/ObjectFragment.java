package codetree.core;

public interface ObjectFragment {
    public abstract boolean equals(Object other);

    public abstract int isMoreCanonicalThan(ObjectFragment other);

    public abstract byte getVlabel();

    public abstract byte[] getelabel();

    public abstract boolean getIsConnected();

    public abstract boolean getIsMaxLabel();

    public abstract boolean getIsAllSameVlabel();
}
