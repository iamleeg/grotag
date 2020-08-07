package net.sf.grotag.parse;

/**
 * Item representing a new line of text. This is particular important for
 * paragraphs in <code>@smartwrap</code> or <code>@worddrap</code>.
 * @author Thomas Aglassinger
 * 
 */
public class NewLineItem extends AbstractItem {
    public NewLineItem(AbstractSource newSource, int newLine, int newColumn) {
        super(newSource, newLine, newColumn);
    }

    @Override
    protected String toStringSuffix() {
        return "<newline>";
    }

    @Override
    public String toPrettyAmigaguide() {
        return "\n";
    }
}
