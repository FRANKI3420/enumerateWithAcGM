package codetree.edgeBased;

import codetree.core.CodeFragment;

class DfsCodeFragment
    implements CodeFragment
{
    byte vLabel;
    byte eLabel;
    int parent;

    DfsCodeFragment()
    {
        vLabel = Byte.MAX_VALUE;
    }

    DfsCodeFragment(byte vLabel, byte eLabel, int parent)
    {
        this.vLabel = vLabel;
        this.eLabel = eLabel;
        this.parent = parent;
    }

    int isMoreCanonicalThan(DfsCodeFragment other)
    {
        int res;

		if (other.vLabel == -1) {
			if (vLabel == -1) {
				if ((res = parent - other.parent) == 0) {
                    res = other.eLabel - eLabel;
                }
			} else {
                return -1;
            }
		} else {
			if (vLabel == -1) {
                return 1;
            } else {
				if ((res = parent - other.parent) == 0) {
					if ((res = other.vLabel - vLabel) == 0) {
                        res = other.eLabel - eLabel;		
                    }
				}
			}
		}

        return res;
    }

    @Override
    public boolean contains(CodeFragment other0)
    {
        return this.equals(other0);
    }

    @Override
    public boolean equals(Object other0)
    {
        DfsCodeFragment other = (DfsCodeFragment)other0;

        return vLabel == other.vLabel && eLabel == other.eLabel && parent == other.parent;
    }

    @Override
    public String toString()
    {
        return "["+vLabel+","+eLabel+","+parent+"]";
    }
}
