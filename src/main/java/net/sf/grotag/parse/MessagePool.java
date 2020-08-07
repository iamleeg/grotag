package net.sf.grotag.parse;

import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Pool containing all messages caused while parsing an Amigaguide document.
 * 
 * @author Thomas Aglassinger
 */
public class MessagePool {
    private static MessagePool instance;

    private SortedSet<MessageItem> items;

    public static final synchronized MessagePool getInstance() {
        if (instance == null) {
            instance = new MessagePool();
        }
        return instance;
    }

    private MessagePool() {
        items = new TreeSet<MessageItem>();
    }

    public SortedSet<MessageItem> getItems() {
        return items;
    }

    public void add(MessageItem newItem) {
        assert newItem != null;
        System.err.println(newItem);
        items.add(newItem);
    }

    public void add(AbstractSource source, int line, int column, String text) {
        // FIXME: Use AbstractSource in MessageItem instead of File.
        MessageItem newMessage = new MessageItem(source, line, column, text);
        add(newMessage);
    }
}
