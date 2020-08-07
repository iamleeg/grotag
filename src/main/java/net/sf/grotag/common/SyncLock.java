package net.sf.grotag.common;

/**
 * Lock for synchronized blocks. This is mostly similar but make the intention
 * more clear than using a plain Object instance.
 * 
 * @author Thomas Aglassinger
 */
public class SyncLock {
    private String scopeDescription;

    public SyncLock(String newScopeDescription) {
        assert newScopeDescription != null;
        scopeDescription = newScopeDescription;
    }

    public String getScopeDescription() {
        return scopeDescription;
    }

    @Override
    public String toString() {
        return "lock to synchronize on " + getScopeDescription();
    }
}
