package net.sf.grotag.parse;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import net.sf.grotag.common.Tools;

/**
 * Reader to dissect an Amigaguide document into a list of
 * <code>AbstractItem</code>s.
 * 
 * @author Thomas Aglassinger
 */
public class ItemReader {
    private LineTokenizer tokenizer;
    private List<AbstractItem> items;
    private int lineNumber;
    private Logger log;
    private Tools tools;
    private AbstractSource source;

    public ItemReader(AbstractSource newSource) {
        assert newSource != null;
        log = Logger.getLogger(ItemReader.class.getName());
        tools = Tools.getInstance();
        source = newSource;
        items = new ArrayList<AbstractItem>();
    }

    /**
     * The source from which the items are read.
     */
    public AbstractSource getSource() {
        return source;
    }

    public void read() throws IOException {
        BufferedReader guideReader = source.createBufferedReader();
        int columnNumber;
        String line;

        try {
            log.info("read items from " + tools.sourced(source.getFullName()));
            lineNumber = 0;
            items = new ArrayList<AbstractItem>();
            do {
                line = guideReader.readLine();
                if (line != null) {
                    tokenizer = new LineTokenizer(source, lineNumber, line);
                    while (tokenizer.hasNext()) {
                        columnNumber = tokenizer.getColumn();
                        tokenizer.advance();
                        if (tokenizer.getType() == LineTokenizer.Type.SPACE) {
                            items.add(new SpaceItem(source, lineNumber, columnNumber, tokenizer.getToken()));
                        } else if (tokenizer.getType() == LineTokenizer.Type.COMMAND) {
                            readCommand();
                        } else {
                            items.add(new TextItem(source, lineNumber, columnNumber, tokenizer.getToken()));
                        }
                    }

                    // Add newline unless the last item is a line command.
                    AbstractItem lastItem = items.get(items.size() - 1);
                    boolean addNewLine = !(lastItem instanceof CommandItem);
                    if (!addNewLine) {
                        CommandItem lastCommand = (CommandItem) lastItem;
                        addNewLine = lastCommand.isInline();
                    }
                    if (addNewLine) {
                        items.add(new NewLineItem(source, lineNumber, tokenizer.getColumn()));
                    }

                    lineNumber += 1;
                }
            } while (line != null);
        } finally {
            guideReader.close();
        }
    }

    private void readCommand() {
        int commandColumnNumber = tokenizer.getColumn();
        List<AbstractItem> commandItems = new ArrayList<AbstractItem>();
        String commandName;

        assert tokenizer.getType() == LineTokenizer.Type.COMMAND;
        assert tokenizer.hasNext();
        tokenizer.advance();
        boolean isInlineCommand = tokenizer.getType() == LineTokenizer.Type.OPEN_BRACE;

        // Skip possible "{".
        if (isInlineCommand) {
            assert tokenizer.hasNext();
            tokenizer.advance();
        }

        assert tokenizer.getType() != LineTokenizer.Type.SPACE : "\"@{\" with white space must have been handled by "
                + LineTokenizer.class;
        commandName = tokenizer.getToken();
        while (tokenizer.hasNext() && !(isInlineCommand && tokenizer.getType() == LineTokenizer.Type.CLOSE_BRACE)) {
            int columnNumber = tokenizer.getColumn();
            tokenizer.advance();
            if (tokenizer.getType() == LineTokenizer.Type.SPACE) {
                commandItems.add(new SpaceItem(source, lineNumber, columnNumber, tokenizer.getToken()));
            } else if (tokenizer.getType() == LineTokenizer.Type.STRING) {
                commandItems.add(new StringItem(source, lineNumber, columnNumber, tokenizer.getToken()));
            } else {
                commandItems.add(new TextItem(source, lineNumber, columnNumber, tokenizer.getToken()));
            }
        }
        items.add(new CommandItem(source, lineNumber, commandColumnNumber, commandName, isInlineCommand, commandItems));
    }

    /**
     * Items in the Amigaguide.
     */
    public List<AbstractItem> getItems() {
        return items;
    }
}
