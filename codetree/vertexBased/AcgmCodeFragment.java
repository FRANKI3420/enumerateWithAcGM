package codetree.vertexBased;

import java.util.*;

import codetree.common.VertexLabel;
import codetree.core.*;

class AcgmCodeFragment
        implements CodeFragment, ObjectFragment {
    final byte vLabel;
    final byte[] eLabels;
    final boolean isConnected;
    final boolean isMaxLabel;
    final boolean isAllSameVlabel;

    AcgmCodeFragment(byte vLabel, int length) {
        this.vLabel = vLabel;
        eLabels = new byte[length];
        isConnected = true;
        isMaxLabel = true;
        isAllSameVlabel = true;
    }

    AcgmCodeFragment(byte vLabel, byte[] eLabels) {
        this.vLabel = vLabel;
        this.eLabels = eLabels.clone();
        isConnected = false;
        isMaxLabel = true;
        isAllSameVlabel = true;
    }

    AcgmCodeFragment(byte vLabel, byte[] eLabels, boolean isConnected, boolean isMaxLabel,
            boolean isAllSameVlabel) {
        this.vLabel = vLabel;
        this.eLabels = eLabels.clone();
        this.isConnected = isConnected;
        this.isMaxLabel = isMaxLabel;
        this.isAllSameVlabel = isAllSameVlabel;
    }

    int isMoreCanonicalThan(AcgmCodeFragment other) {
        final int res = vLabel - other.vLabel;
        return res != 0 ? res : Arrays.compare(eLabels, other.eLabels);
    }

    public int isMoreCanonicalThan(ObjectFragment other) {
        final int res = vLabel - other.getVlabel();
        return res != 0 ? res : Arrays.compare(eLabels, other.getelabel());
    }

    @Override
    public int isMoreCanonicalThan(CodeFragment other) {
        AcgmCodeFragment other0 = (AcgmCodeFragment) other;
        final int res = vLabel - other0.vLabel;
        return res != 0 ? res : Arrays.compare(eLabels, other0.eLabels);
    }

    @Override
    public boolean equals(Object other0) {
        AcgmCodeFragment other = (AcgmCodeFragment) other0;
        return vLabel == other.vLabel && Arrays.equals(eLabels, other.eLabels);
    }

    @Override
    public boolean contains(CodeFragment other0) {
        AcgmCodeFragment other = (AcgmCodeFragment) other0;

        final int len = eLabels.length;
        if (len != other.eLabels.length) {
            throw new IllegalArgumentException("Compareing incompatible fragments.");
        }

        if (vLabel != other.vLabel) {
            return false;
        }

        for (int i = 0; i < len; ++i) {
            if (other.eLabels[i] > 0 && eLabels[i] != other.eLabels[i]) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String toString() {
        String s = VertexLabel.id2string(vLabel);
        for (int i = 0; i < eLabels.length; ++i) {
            s += String.valueOf(eLabels[i]);
        }

        return s;
    }

    @Override
    public byte getVlabel() {
        return this.vLabel;
    }

    @Override
    public byte[] getelabel() {
        return this.eLabels;
    }

    @Override
    public boolean getIsConnected() {
        return this.isConnected;
    }

    @Override
    public boolean getIsMaxLabel() {
        return this.isMaxLabel;
    }

    @Override
    public boolean getIsAllSameVlabel() {
        return this.isAllSameVlabel;
    }
}
