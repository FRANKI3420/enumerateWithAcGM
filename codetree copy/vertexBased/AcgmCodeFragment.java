package codetree.vertexBased;

import java.util.*;

import codetree.common.VertexLabel;
import codetree.core.*;

class AcgmCodeFragment
        implements CodeFragment, ObjectFragment {
    final byte vLabel;
    final byte[] eLabels;
    final boolean isConnected;
    final int edges;

    AcgmCodeFragment(byte vLabel, int length) {
        this.vLabel = vLabel;
        eLabels = new byte[length];
        isConnected =true;
        edges = 0;
    }

    AcgmCodeFragment(byte vLabel, byte[] eLabels) {
        this.vLabel = vLabel;
        this.eLabels = eLabels.clone();
        isConnected = false;
        edges = 0;
    }
    // AcgmCodeFragment(byte vLabel, byte[] eLabels,boolean pastIsConnected) {
    //     this.vLabel = vLabel;
    //     this.eLabels = eLabels.clone();
    //     if(!pastIsConnected){
    //         this.isConnected = false;
    //     }else{
    //         this.isConnected = judgeIsConnected(eLabels);
    //     }
    // }
    AcgmCodeFragment(byte vLabel, byte[] eLabels,boolean isConnected,int edges) {
        this.vLabel = vLabel;
        this.eLabels = eLabels.clone();
        this.isConnected = isConnected;
        this.edges = edges;
    }

    //eLabelsの配列が全て0であれば、非連結である
    private boolean judgeIsConnected(byte[] eLabels) {
        for(byte e : eLabels){
            if(e==1){ 
                return true;
            }
        }
        return false;
    }

    int isMoreCanonicalThan(AcgmCodeFragment other) {
        final int res = vLabel - other.vLabel;
        return res != 0 ? res : Arrays.compare(eLabels, other.eLabels);
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
    public int getEdges(){
        return this.edges;
    }
}
