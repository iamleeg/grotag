package net.sf.grotag.parse;

import net.sf.grotag.common.HashCodeTools;

/**
 * A message generated during parsing.
 * 
 * @author Thomas Aglassinger
 */
public class MessageItem implements Comparable<MessageItem> {
    private AbstractSource source;
    private int line;
    private int column;
    private String text;
    private MessageItem seeAlso;

    public MessageItem(AbstractSource newSource, int newLine, int newColumn, String newText) {
        assert newSource != null;
        assert newLine >= 0;
        assert newColumn >= 0;
        assert newText != null;
        assert newText.length() > 0;

        source = newSource;
        line = newLine;
        column = newColumn;
        text = newText;
    }

    public MessageItem(AbstractItem baseItem, String newText) {
        this(baseItem.getFile(), baseItem.getLine(), baseItem.getColumn(), newText);
    }

    public MessageItem(AbstractSource newSource, String newText) {
        this(newSource, 0, 0, newText);
    }

    // FIXME: Rename to getSource().
    public AbstractSource getFile() {
        return source;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public String getText() {
        return text;
    }

    public MessageItem getSeeAlso() {
        return seeAlso;
    }

    public void setSeeAlso(MessageItem newSeeAlso) {
        seeAlso = newSeeAlso;
    }

    @Override
    public String toString() {
        String result = getFile().getShortName() + "[" + getLine() + ":" + getColumn() + "]: " + getText();
        if (getSeeAlso() != null) {
            result += "; see also: " + getSeeAlso().toString();
        }
        return result;
    }

    public int compareTo(MessageItem other) {
        int result;
        if (other == null) {
            result = -1;
        } else {
            result = getFile().getFullName().compareTo(other.getFile().getFullName());
            if (result == 0) {
                result = getLine() - other.getLine();
                if (result == 0) {
                    result = getColumn() - other.getColumn();
                }
            }
        }
        return result;
    }

    @Override
    public boolean equals(Object other) {
        boolean result = (other instanceof MessageItem);
        if (result) {
            result = (compareTo((MessageItem) other) == 0);
        }
        return result;
    }

    @Override
    public int hashCode() {
        int result = HashCodeTools.SEED;
        HashCodeTools hashCodeTools = HashCodeTools.getInstance();
        result = hashCodeTools.hash(result, getFile().getFullName());
        result = hashCodeTools.hash(result, getLine());
        result = hashCodeTools.hash(result, getColumn());
        return result;
    }
}
