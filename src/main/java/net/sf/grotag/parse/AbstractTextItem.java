package net.sf.grotag.parse;

public abstract class AbstractTextItem extends AbstractItem {
    private String text;

    protected AbstractTextItem(AbstractSource newSource, int newLine, int newColumn) {
        super(newSource, newLine, newColumn);
    }

    protected final void setText(String newText) {
        text = newText;
    }

    public String getText() {
        return text;
    }
}
