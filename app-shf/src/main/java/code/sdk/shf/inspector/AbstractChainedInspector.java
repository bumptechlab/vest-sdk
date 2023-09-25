package code.sdk.shf.inspector;

public abstract class AbstractChainedInspector {

    private AbstractChainedInspector mNext;

    public static AbstractChainedInspector makeChain(AbstractChainedInspector... inspectors) {
        if (inspectors.length == 0) {
            return null;
        }
        for (int i = 0; i < inspectors.length - 1; i++) {
            inspectors[i].mNext = inspectors[i + 1];
        }
        return inspectors[0];
    }

    public boolean verify() {
        if (!inspect()) {
            return false;
        }
        if (mNext != null) {
            return mNext.verify();
        }
        return true;
    }

    protected abstract boolean inspect();
}

