package net.sf.grotag.parse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import net.sf.grotag.common.TestTools;

import org.junit.Before;
import org.junit.Test;

/**
 * Test for ItemReader.
 * 
 * @author Thomas Aglassinger
 */
public class ItemReaderTest {
    private Logger logger;
    private TestTools testTools;

    @Before
    public void setUp() throws Exception {
        testTools = TestTools.getInstance();
        logger = Logger.getLogger(ItemReaderTest.class.getName());
    }

    private StringSource createStringSource(String shortName, String text) {
        assert shortName != null;
        assert text != null;
        return new StringSource(ItemReaderTest.class.getName() + File.separator + shortName, text);
    }

    @Test
    public void testSpace() throws Exception {
        final String SPACE = " \t ";
        AbstractSource guide = createStringSource("testSpace", SPACE + "x");
        ItemReader reader = new ItemReader(guide);
        reader.read();
        List<AbstractItem> items = reader.getItems();
        assertEquals(3, items.size());
        AbstractItem item = items.get(0);
        logger.info(item.toString());
        assertTrue(item instanceof SpaceItem);
        assertEquals(SPACE, ((SpaceItem) item).getSpace());
    }

    @Test
    public void testString() throws Exception {
        StringSource guide = createStringSource("testText", "@title \"hugo\"");
        ItemReader reader = new ItemReader(guide);
        reader.read();
        List<AbstractItem> items = reader.getItems();
        assertEquals(1, items.size());
        AbstractItem item = items.get(0);
        logger.info(item.toString());
        assertTrue(item instanceof CommandItem);
        CommandItem titleItem = (CommandItem) item;
        List<AbstractItem> options = titleItem.getItems();
        assertEquals(2, options.size());
        assertTrue(options.get(0) instanceof SpaceItem);
        assertTrue(options.get(1) instanceof StringItem);
        assertEquals("hugo", ((StringItem) options.get(1)).getText());
    }

    @Test
    public void testText() throws Exception {
        StringSource guide = createStringSource("testText", "a\\\\b\\@");
        ItemReader reader = new ItemReader(guide);
        reader.read();
        List<AbstractItem> items = reader.getItems();
        assertEquals(2, items.size());
        AbstractItem item = items.get(0);
        logger.info(item.toString());
        assertTrue(item instanceof TextItem);
        assertEquals("a\\b@", ((TextItem) item).getText());
    }

    @Test
    public void testDanglingAtSign() throws Exception {
        StringSource guide = createStringSource("testDanglingAtSign", "@");
        ItemReader reader = new ItemReader(guide);
        reader.read();
        List<AbstractItem> items = reader.getItems();
        assertEquals(2, items.size());
        AbstractItem item = items.get(0);
        logger.info(item.toString());
        assertTrue(item instanceof TextItem);
        assertEquals("@", ((TextItem) item).getText());
    }

    @Test
    public void testCommand() throws Exception {
        AbstractSource guide = createStringSource("testDanglingAtSign", "@dAtAbAsE hugo");
        ItemReader reader = new ItemReader(guide);
        reader.read();
        List<AbstractItem> items = reader.getItems();
        assertEquals(1, items.size());
        AbstractItem item = items.get(0);
        logger.info(item.toString());
        assertTrue(item instanceof CommandItem);
        CommandItem commandItem = (CommandItem) item;
        assertEquals("database", commandItem.getCommandName());
        assertEquals("dAtAbAsE", commandItem.getOriginalCommandName());
        assertFalse(commandItem.isInline());
        assertNotNull(commandItem.getItems());
        assertEquals(2, commandItem.getItems().size());
    }

    private CommandItem createCommandItem(String testName, String commandDefinition) throws IOException {
        assert testName != null;
        assert commandDefinition != null;
        CommandItem result;
        AbstractSource guide = createStringSource(testName, commandDefinition);
        ItemReader reader = new ItemReader(guide);
        reader.read();
        List<AbstractItem> items = reader.getItems();
        int itemCount = items.size();
        assert (itemCount >= 1) && (itemCount <= 2) : "itemCount=" + itemCount + ", items=" + items;
        AbstractItem firstItem = items.get(0);
        assert firstItem instanceof CommandItem : "broken command=" + firstItem;
        result = (CommandItem) firstItem;
        logger.fine("created command: " + result);
        return result;
    }

    @Test
    public void testCommandOptions() throws Exception {
        CommandItem command;
        List<AbstractItem> items;

        command = createCommandItem("testNoOptions", "@wordwrap");
        items = command.getItems();
        assertEquals(0, items.size());
        assertNull(command.getOption(0));

        // A command with space between all options.
        command = createCommandItem("testSomeOptions", "@{\"Introduction\" link \"intro\"}");
        items = command.getItems();
        assertEquals(4, items.size());
        assertTrue(items.get(0) instanceof SpaceItem);
        assertTrue(items.get(1) instanceof TextItem);
        assertTrue(items.get(2) instanceof SpaceItem);
        assertTrue(items.get(3) instanceof StringItem);
        assertEquals(2, command.getOptionCount());
        assertEquals("link", command.getOption(0));
        String secondOption = command.getOption(1);
        assertEquals("intro", secondOption);

        // A command without space between a String and a normal text option.
        command = createCommandItem("testSomeOptions", "@{\"Introduction\"link \"intro\" 17}");
        items = command.getItems();
        assertEquals(5, items.size());
        assertTrue(items.get(0) instanceof TextItem);
        assertTrue(items.get(1) instanceof SpaceItem);
        assertTrue(items.get(2) instanceof StringItem);
        assertTrue(items.get(3) instanceof SpaceItem);
        assertTrue(items.get(4) instanceof TextItem);
        assertEquals(3, command.getOptionCount());
        assertEquals("link", command.getOption(0));
        assertEquals("intro", command.getOption(1));
        assertEquals("17", command.getOption(2));
        command.cutOptionsAt(2);
        assertNotNull(command.getOption(1));
        assertNull(command.getOption(2));
    }

    @Test
    public void testLichtTools() throws Exception {
        AbstractSource guide = new FileSource(testTools.getTestInputFile("basics.guide"));
        ItemReader reader = new ItemReader(guide);
        reader.read();
        for (AbstractItem item : reader.getItems()) {
            logger.info(item.toString());
        }
    }
}
