package net.sf.grotag.parse;

import net.sf.grotag.common.Tools;

/**
 * Item that holds a string, which originally was embedded in quotes though the
 * internal representation does remove these quotes.
 * 
 * @author Thomas Aglassinger
 */
public class StringItem extends AbstractTextItem {

    public StringItem(AbstractSource newSource, int newLine, int newColumn, String newString) {
        super(newSource, newLine, newColumn);

        // Assert that the text actually is a string embedded between quotes.
        // Ensuring this is the responsibility of LineTokenizer.
        assert newString != null;
        assert newString.length() >= 2;
        assert newString.charAt(0) == '"';
        assert newString.charAt(newString.length() - 1) == '"';

        setText(newString.substring(1, newString.length() - 1));
    }

    @Override
    protected String toStringSuffix() {
        return "<string>" + Tools.getInstance().sourced(getText());
    }

    @Override
    public String toPrettyAmigaguide() {
        return "\"" + getText() + "\"";
    }
}
