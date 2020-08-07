package net.sf.grotag.parse;

import net.sf.grotag.common.Tools;

/**
 * An item representing white space.
 * 
 * @author Thomas Aglassinger
 */
public class SpaceItem extends AbstractItem {
    private String space;

    public SpaceItem(AbstractSource newSource, int newLine, int newColumn, String newSpace) {
        super(newSource, newLine, newColumn);

        // Assert that every characters actually is white space.
        // Ensuring this is the responsibility of LineTokenizer.
        assert newSpace != null;
        for (int i = 0; i < newSpace.length(); i += 1) {
            char ch = newSpace.charAt(i);
            assert Character.isWhitespace(ch) : "character at position " + i
                    + " must be whitespace but is: "
                    + Tools.getInstance().sourced(ch);
        }
        space = newSpace;
    }

    public String getSpace() {
        return space;
    }

    @Override
    protected String toStringSuffix() {
        return "<space>" + Tools.getInstance().sourced(getSpace());
    }

    @Override
    public String toPrettyAmigaguide() {
        return getSpace();
    }
}
