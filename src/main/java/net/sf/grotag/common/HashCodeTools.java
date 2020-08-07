package net.sf.grotag.common;

import java.lang.reflect.Array;
import java.util.logging.Logger;

/**
 * Tools to simplify implementing <code>hashCode</code>. This is based on an
 * article about "<a
 * href="http://www.javapractices.com/topic/TopicAction.do?Id=28">Implementing
 * hashCode</a>" on javapractices.com.
 * 
 * @author Thomas Aglassinger
 */
public final class HashCodeTools {
    private static HashCodeTools instance;
    private Logger log;

    private HashCodeTools() {
        log = Logger.getLogger(HashCodeTools.class.getName());
    }

    public static final synchronized HashCodeTools getInstance() {
        if (instance == null) {
            instance = new HashCodeTools();
        }
        return instance;
    }

    /**
     * An initial value for a <code>hashCode</code>, to which is added
     * contributions from fields. Using a non-zero value decreases collisions of
     * <code>hashCode</code> values.
     */
    public static final int SEED = 23;

    public int hash(int aSeed, boolean aBoolean) {
        log.finest("boolean...");
        return firstTerm(aSeed) + (aBoolean ? 1 : 0);
    }

    public int hash(int aSeed, char aChar) {
        log.finest("char...");
        return firstTerm(aSeed) + aChar;
    }

    public int hash(int aSeed, int aInt) {
        // Note that byte and short are handled by this method, through implicit
        // conversion.
        log.finest("int...");
        return firstTerm(aSeed) + aInt;
    }

    public int hash(int aSeed, long aLong) {
        log.finest("long...");
        return firstTerm(aSeed) + (int) (aLong ^ (aLong >>> 32));
    }

    public int hash(int aSeed, float aFloat) {
        return hash(aSeed, Float.floatToIntBits(aFloat));
    }

    public int hash(int aSeed, double aDouble) {
        return hash(aSeed, Double.doubleToLongBits(aDouble));
    }

    /**
     * <code>aObject</code> is a possibly-null object field, and possibly an
     * array.
     * 
     * If <code>aObject</code> is an array, then each element may be a
     * primitive or a possibly-null object.
     */
    public int hash(int aSeed, Object aObject) {
        int result = aSeed;
        if (aObject == null) {
            result = hash(result, 0);
        } else if (!isArray(aObject)) {
            result = hash(result, aObject.hashCode());
        } else {
            int length = Array.getLength(aObject);
            for (int idx = 0; idx < length; ++idx) {
                Object item = Array.get(aObject, idx);
                // Recursive call.
                result = hash(result, item);
            }
        }
        return result;
    }

    private static final int ODD_PRIME_NUMBER = 37;

    private int firstTerm(int aSeed) {
        return ODD_PRIME_NUMBER * aSeed;
    }

    private boolean isArray(Object aObject) {
        return aObject.getClass().isArray();
    }
}