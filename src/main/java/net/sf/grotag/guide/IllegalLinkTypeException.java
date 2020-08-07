package net.sf.grotag.guide;

/**
 * Exception to be thrown if an unknown link type is encountered.
 * 
 * @see Link
 * @author Thomas Aglassinger
 */
public class IllegalLinkTypeException extends IllegalArgumentException {
    private String linkType;

    public IllegalLinkTypeException(String newLinkType, IllegalArgumentException cause) {
        super("unknown link type: " + newLinkType, cause);
        assert newLinkType != null;
        assert cause != null;
        linkType = newLinkType;
    }

    public String getLinkType() {
        return linkType;
    }

}
