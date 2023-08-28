package code.sdk.shf.inspector;

public class AbstractChainedInspector implements IChainedInspector {
    public static final String TAG = AbstractChainedInspector.class.getSimpleName();

    private AbstractChainedInspector mNext;

    public static AbstractChainedInspector makeChain(AbstractChainedInspector head,
                                                     AbstractChainedInspector...rest) {
        AbstractChainedInspector p = head;
        for (AbstractChainedInspector next : rest) {
            p.mNext = next;
            p = next;
        }
        return head;
    }

    @Override
    public boolean inspect() {
        return false;
    }

    @Override
    public boolean inspectNext() {
        if (mNext == null) {
            return true;
        }
        return mNext.inspect();
    }
}
