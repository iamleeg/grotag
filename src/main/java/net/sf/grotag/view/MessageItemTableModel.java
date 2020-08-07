package net.sf.grotag.view;

import java.util.LinkedList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import net.sf.grotag.parse.MessageItem;
import net.sf.grotag.parse.MessagePool;

/**
 * TableModel to show all <code>MessageItem</code>s in the
 * <code>MessagePool</code>.
 * 
 * @see MessagePool
 * @author Thomas Aglassinger
 */
public class MessageItemTableModel extends AbstractTableModel {
    private static final String[] TITLES = new String[] { "File", "Location", "Description" };

    private MessageItem[] items;
    private Object updateLock;

    public MessageItemTableModel() {
        super();
        items = new MessageItem[] {};
        updateLock = new Object();
    }

    /**
     * Update the table model from the current messages in
     * <code>MessagePool</code>.
     * 
     * @see MessagePool
     */
    public void update() {
        MessagePool pool = MessagePool.getInstance();
        List<MessageItem> newValues = new LinkedList<MessageItem>();

        synchronized (updateLock) {
            for (MessageItem item : pool.getItems()) {
                do {
                    newValues.add(item);
                    item = item.getSeeAlso();
                } while (item != null);
            }
            items = newValues.toArray(new MessageItem[] {});
        }

        fireTableDataChanged();
    }

    public int getColumnCount() {
        return TITLES.length;
    }

    public int getRowCount() {
        synchronized (updateLock) {
            return items.length;
        }
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        String result;

        synchronized (updateLock) {
            MessageItem item = items[rowIndex];
            if (columnIndex == 0) {
                result = item.getFile().getShortName();
            } else if (columnIndex == 1) {
                result = "" + item.getLine() + ":" + item.getColumn();
            } else if (columnIndex == 2) {
                result = item.getText();
            } else {
                result = "columnIndex=" + columnIndex;
                assert false : result;
            }
        }

        return result;
    }

    @Override
    public String getColumnName(int column) {
        return TITLES[column];
    }
}
