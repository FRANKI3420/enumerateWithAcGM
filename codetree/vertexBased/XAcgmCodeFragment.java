package codetree.vertexBased;

import java.util.*;

import codetree.core.*;

class XAcgmCodeFragment
        implements CodeFragment {
    final byte vLabel;
    final int degree1, degree2;
    final byte[] eLabels;

    XAcgmCodeFragment(byte vLabel, int length) {
        this.vLabel = vLabel;
        this.degree1 = 0;
        this.degree2 = 0;
        eLabels = new byte[length];
    }

    XAcgmCodeFragment(byte vLabel, int degree1, int degree2, byte[] eLabels) {
        this.vLabel = vLabel;
        this.degree1 = degree1;
        this.degree2 = degree2;
        this.eLabels = eLabels.clone();
    }

    int isMoreCanonicalThan(XAcgmCodeFragment other) {
        int res;

        if ((res = vLabel - other.vLabel) == 0) {
            if ((res = degree1 - other.degree1) == 0) {
                if ((res = degree2 - other.degree2) == 0) {
                    res = Arrays.compare(eLabels, other.eLabels);
                }
            }
        }

        return res;
    }

    @Override
    public boolean equals(Object other0) {
        XAcgmCodeFragment other = (XAcgmCodeFragment) other0;
        return vLabel == other.vLabel
                && degree1 == other.degree1
                && degree2 == other.degree2
                && Arrays.equals(eLabels, other.eLabels);
    }

    @Override
    public boolean contains(CodeFragment other0) {
        XAcgmCodeFragment other = (XAcgmCodeFragment) other0;

        final int len = eLabels.length;
        if (len != other.eLabels.length) {
            throw new IllegalArgumentException("Compareing incompatible fragments.");
        }

        if (vLabel != other.vLabel || degree1 < other.degree1 || degree2 < other.degree2) {
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
    public byte getVlabel() {
        return this.vLabel;
    }

    @Override
    public byte[] getelabel() {
        return this.eLabels;
    }
}
