package net.sf.grotag.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;
import javax.xml.parsers.ParserConfigurationException;

import net.sf.grotag.Grotag;
import net.sf.grotag.common.AmigaPathList;
import net.sf.grotag.common.SwingWorker;
import net.sf.grotag.common.SyncLock;
import net.sf.grotag.common.Tools;
import net.sf.grotag.guide.DomWriter;
import net.sf.grotag.guide.ExportTools;
import net.sf.grotag.guide.Guide;
import net.sf.grotag.guide.GuidePile;
import net.sf.grotag.guide.HtmlDomFactory;
import net.sf.grotag.guide.NodeInfo;
import net.sf.grotag.guide.Relation;
import net.sf.grotag.guide.DomWriter.Dtd;

import org.xml.sax.SAXException;

/**
 * JFrame to browse an Amigaguide document converted to HTML.
 * 
 * @author Thomas Aglassinger
 */
public class GrotagFrame extends JFrame implements HyperlinkListener {
    /**
     * JToolBar for Grotag allowing quick access to the most important
     * functions, in particular navigation within the document.
     * 
     * @author Thomas Aglassinger
     */
    private class GrotagToolBar extends JToolBar {
        public GrotagToolBar() {
            super();
            JButton contentsButton = createRelationButton("Contents", Relation.contents);
            JButton indexButton = createRelationButton("Index", Relation.index);
            JButton helpButton = createRelationButton("Help", Relation.help);
            JButton nextButton = createRelationButton("Next", Relation.next);
            JButton previousButton = createRelationButton("Previous", Relation.previous);
            relationButtons.add(contentsButton);
            relationButtons.add(indexButton);
            relationButtons.add(helpButton);
            relationButtons.add(nextButton);
            relationButtons.add(previousButton);

            backButton = createToolbarButton("back", new BackAction());
            forwardButton = createToolbarButton("forward", new ForwardAction());
            homeButton = createToolbarButton("home", new HomeAction());

            add(backButton);
            add(forwardButton);
            addSeparator();
            add(previousButton);
            add(nextButton);
            addSeparator();
            add(contentsButton);
            add(homeButton);
            add(indexButton);
            add(helpButton);
            setFloatable(false);
        }

        /**
         * List of actions as assigned to the buttons in this tool bar.
         */
        public List<Action> getActionList() {
            List<Action> result = new LinkedList<Action>();

            for (Component component : getComponents()) {
                if (component instanceof JButton) {
                    JButton button = (JButton) component;
                    Action action = button.getAction();
                    if (action != null) {
                        result.add(action);
                    }
                }
            }
            return result;
        }

        private JButton createToolbarButton(String iconName, Action action) {
            JButton result;
            String imageName = iconName + ".png";
            Image image = tools.readImageRessource(imageName);
            result = new JButton(action);
            result.setIcon(new ImageIcon(image));
            result.setToolTipText(result.getText());
            result.setText(null);
            return result;
        }

        private JButton createRelationButton(String label, Relation relation) {
            JButton result = createToolbarButton(relation.toString().toLowerCase(), new RelationAction(label, relation));
            return result;
        }
    }

    /**
     * Menu bar to interact with the Grotag viewer.
     * 
     * @author Thomas Aglassinger
     */
    private class GrotagMenuBar extends JMenuBar {
        private int commandMask;
        private JMenuItem exportItem;

        public GrotagMenuBar() {
            super();
            if (tools.isMacOsX()) {
                commandMask = ActionEvent.META_MASK;
            } else {
                commandMask = ActionEvent.CTRL_MASK;
            }
            add(createFileMenu());
            add(createEditMenu());
            add(createGoMenu());
            add(createHelpMenu());
        }

        /**
         * List of actions as assigned to the items in the menu bar.
         */
        public List<Action> getActionList() {
            List<Action> result = new LinkedList<Action>();

            for (int menuIndex = 0; menuIndex < getMenuCount(); menuIndex += 1) {
                JMenu menu = getMenu(menuIndex);
                for (int itemIndex = 0; itemIndex < menu.getItemCount(); itemIndex += 1) {
                    JMenuItem item = menu.getItem(itemIndex);
                    Action action = item.getAction();
                    if (action != null) {
                        result.add(action);
                    }
                }
            }
            return result;
        }

        public void setGuiState(GuiState state) {
            assert state != null;
            for (int menuIndex = 0; menuIndex < getMenuCount(); menuIndex += 1) {
                JMenu menu = getMenu(menuIndex);
                boolean anyEnabled = false;
                for (int itemIndex = 0; itemIndex < menu.getItemCount(); itemIndex += 1) {
                    JMenuItem item = menu.getItem(itemIndex);
                    Action action = item.getAction();
                    if (action != null) {
                        boolean actionIsEnabled;
                        if (action instanceof AbstractGuiAction) {
                            AbstractGuiAction guiAction = (AbstractGuiAction) action;
                            actionIsEnabled = guiAction.isEnabledFor(state);
                        } else {
                            // Enable non-GuiAction such as Edit > Copy during
                            // browsing only.
                            actionIsEnabled = (state == GuiState.BROWSING);
                        }
                        if (actionIsEnabled) {
                            anyEnabled = true;
                        }
                        item.setEnabled(actionIsEnabled);
                    }
                }
                menu.setEnabled(anyEnabled);
            }
        }

        private void setAccelerator(JMenuItem item, int code) {
            item.setAccelerator(KeyStroke.getKeyStroke(code, commandMask));
        }

        private final JMenu createFileMenu() {
            JMenu result = new JMenu("File");
            JMenuItem openItem = new JMenuItem(new OpenAction());
            exportItem = new JMenuItem(new ExportAction());
            openItem.setMnemonic(KeyEvent.VK_O);
            setAccelerator(openItem, KeyEvent.VK_O);
            exportItem.setEnabled(false);
            exportItem.setMnemonic(KeyEvent.VK_E);
            setAccelerator(exportItem, KeyEvent.VK_E);
            result.add(openItem);
            result.add(exportItem);
            JMenuItem exitItem = new JMenuItem(new ExitAction());
            exitItem.setMnemonic(KeyEvent.VK_X);
            setAccelerator(exitItem, KeyEvent.VK_X);
            result.add(exitItem);
            result.setMnemonic(KeyEvent.VK_F);
            return result;
        }

        private final JMenu createEditMenu() {
            JMenu result = new JMenu("Edit");
            Action copyAction = getActionByName(DefaultEditorKit.copyAction);
            Action selectAllAction = getActionByName(DefaultEditorKit.selectAllAction);
            JMenuItem copyItem = new JMenuItem(copyAction);
            JMenuItem selectAllItem = new JMenuItem(selectAllAction);
            copyItem.setAction(copyAction);
            copyItem.setText("Copy");
            copyItem.setMnemonic(KeyEvent.VK_C);
            setAccelerator(copyItem, KeyEvent.VK_C);
            selectAllItem.setText("Select all");
            selectAllItem.setMnemonic(KeyEvent.VK_A);
            setAccelerator(selectAllItem, KeyEvent.VK_A);
            result.add(copyItem);
            result.add(selectAllItem);
            result.setMnemonic(KeyEvent.VK_E);
            return result;
        }

        private final JMenu createGoMenu() {
            JMenu result = new JMenu("Go");
            JMenuItem nextItem = new JMenuItem(new RelationAction("Next", Relation.next));
            nextItem.setMnemonic(KeyEvent.VK_N);
            setAccelerator(nextItem, KeyEvent.VK_RIGHT);
            JMenuItem previousItem = new JMenuItem(new RelationAction("Previous", Relation.previous));
            previousItem.setMnemonic(KeyEvent.VK_P);
            setAccelerator(previousItem, KeyEvent.VK_LEFT);
            JMenuItem tocItem = new JMenuItem(new RelationAction("Contents", Relation.contents));
            tocItem.setMnemonic(KeyEvent.VK_C);
            setAccelerator(tocItem, KeyEvent.VK_T);
            JMenuItem indexItem = new JMenuItem(new RelationAction("Index", Relation.index));
            indexItem.setMnemonic(KeyEvent.VK_I);
            setAccelerator(indexItem, KeyEvent.VK_N);
            result.add(nextItem);
            result.add(previousItem);
            result.add(tocItem);
            result.add(indexItem);
            result.setMnemonic(KeyEvent.VK_G);
            return result;
        }

        private final JMenu createHelpMenu() {
            JMenu result = new JMenu("Help");
            JMenuItem aboutItem = new JMenuItem("About");
            aboutItem.setAction(new AboutAction());
            result.add(aboutItem);
            return result;
        }
    }

    /**
     * Action to close the window and exit.
     * 
     * @author Thomas Aglassinger
     */
    private class ExitAction extends AbstractGuiAction {
        public ExitAction() {
            super("Exit");
        }

        public void actionPerformed(ActionEvent e) {
            dispose();
            System.exit(0);
        }

        @Override
        public boolean isEnabledFor(GuiState state) {
            return true;
        }
    }

    /**
     * Action to open a new guide using a dialog.
     * 
     * @author Thomas Aglassinger
     */
    private class ExportAction extends AbstractGuiAction {
        public ExportAction() {
            super("Export...");
        }

        public void actionPerformed(ActionEvent event) {
            try {
                setGuiState(GuiState.EXPORTING);
                String lastExportFolderPath = settings.get(SETTING_LAST_EXPORT_FOLDER, null);
                if (lastExportFolderPath != null) {
                    exportChooser.setSelectedFile(new File(lastExportFolderPath));
                }

                int userAction = exportChooser.showDialog(getGrotagFrame(), "Export");

                if (userAction == JFileChooser.APPROVE_OPTION) {
                    ExportTools exportTools = ExportTools.getInstance();
                    File folderToExportTo = exportChooser.getSelectedFile();
                    settings.put(SETTING_LAST_EXPORT_FOLDER, folderToExportTo.getAbsolutePath());
                    File inputFile = new File(settings.get(SETTING_LAST_GUIDE_FILE_OPENED, null));
                    Dtd formatToExport = ((ExportAccessory) exportChooser.getAccessory()).getFormat();

                    if (formatToExport == Dtd.DOCBOOK) {
                        File outputFile = exportTools.targetFileFor(inputFile, folderToExportTo, "xml");
                        exportTools.exportAsDocBookXml(pile, outputFile);
                    } else if ((formatToExport == Dtd.HTML) || (formatToExport == Dtd.XHTML)) {
                        exportTools.exportAsHtml(pile, folderToExportTo, formatToExport);
                    } else {
                        assert false : "format=" + formatToExport;
                    }
                }
            } catch (Exception error) {
                showError("cannot export document", error);
            } finally {
                setGuiState(GuiState.BROWSING);
            }
        }

        @Override
        public boolean isEnabledFor(GuiState state) {
            return state == GuiState.BROWSING;
        }
    }

    /**
     * Action to open a new guide using a dialog.
     * 
     * @author Thomas Aglassinger
     */
    private class OpenAction extends AbstractGuiAction {
        public OpenAction() {
            super("Open...");
        }

        public void actionPerformed(ActionEvent event) {
            try {
                String lastGuideFileOpenedPath = settings.get(SETTING_LAST_GUIDE_FILE_OPENED, null);
                if (lastGuideFileOpenedPath != null) {
                    openChooser.setSelectedFile(new File(lastGuideFileOpenedPath));
                }

                int userAction = openChooser.showOpenDialog(getGrotagFrame());
                if (userAction == JFileChooser.APPROVE_OPTION) {
                    File guideFileToOpen = openChooser.getSelectedFile();
                    read(guideFileToOpen);
                }
            } catch (Exception error) {
                showError("cannot open file", error);
            }
        }

        @Override
        public boolean isEnabledFor(GuiState state) {
            return (state == GuiState.BROWSING) || (state == GuiState.EMPTY);
        }
    }

    /**
     * Worker to read Amigaguide document in the background while updating the
     * progress bar and status.
     * 
     * @author Thomas Aglassinger
     */
    private class ReadWorker extends SwingWorker {
        private File guideFile;
        private AmigaPathList amigaPaths;

        public ReadWorker(File newGuideFile, AmigaPathList newAmigaPaths) {
            assert newGuideFile != null;
            assert newAmigaPaths != null;
            guideFile = newGuideFile;
            amigaPaths = newAmigaPaths;
        }

        @Override
        public Object construct() {
            try {
                doRead(guideFile, amigaPaths);
            } catch (IOException error) {
                showError("cannot read " + tools.sourced(guideFile), error);
            } catch (Exception error) {
                showError("cannot process " + tools.sourced(guideFile), error);
            }
            return null;
        }
    }

    /**
     * Action to process the "back" command.
     * 
     * @author Thomas Aglassinger
     */
    public class BackAction extends AbstractGuiAction {
        private BackAction() {
            super("Back");
        }

        public void actionPerformed(ActionEvent event) {
            try {
                synchronized (pageLock) {
                    log.info("action: back");
                    logHistory();
                    assert pageHistoryIndex > 0;
                    pageHistoryIndex -= 1;
                    URI uriToGoTo = pageHistory.get(pageHistoryIndex);
                    setPageWithoutHistory(uriToGoTo);
                    setBackForwardAndHomeButtonEnabled();
                    logHistory();
                }
            } catch (Throwable error) {
                showError("cannot go back", error);
            }
        }

        @Override
        public boolean isEnabledFor(GuiState state) {
            return state == GuiState.BROWSING;
        }
    }

    /**
     * Action to process the "back" command.
     * 
     * @author Thomas Aglassinger
     */
    public class ForwardAction extends AbstractGuiAction {
        private ForwardAction() {
            super("Forward");
        }

        public void actionPerformed(ActionEvent event) {
            try {
                synchronized (pageLock) {
                    log.info("action: forward");
                    logHistory();
                    assert (pageHistoryIndex != NO_INDEX) && (pageHistoryIndex < pageHistory.size() - 1);
                    pageHistoryIndex += 1;
                    URI uriToGoTo = pageHistory.get(pageHistoryIndex);
                    setPageWithoutHistory(uriToGoTo);
                    setBackForwardAndHomeButtonEnabled();
                    logHistory();
                }
            } catch (Throwable error) {
                showError("cannot go forward", error);
            }
        }

        @Override
        public boolean isEnabledFor(GuiState state) {
            return state == GuiState.BROWSING;
        }
    }

    /**
     * Action to process the "home" command.
     * 
     * @author Thomas Aglassinger
     */
    public class HomeAction extends AbstractGuiAction {
        private HomeAction() {
            super("Home");
        }

        public void actionPerformed(ActionEvent event) {
            try {
                synchronized (pageLock) {
                    log.info("action: home");
                    setPage(homeUri);
                }
            } catch (Throwable error) {
                showError("cannot go to home page", error);
            }
        }

        @Override
        public boolean isEnabledFor(GuiState state) {
            return state == GuiState.BROWSING;
        }
    }

    /**
     * Action to change URL according to a relation.
     * 
     * @author Thomas Aglassinger
     */
    public class RelationAction extends AbstractGuiAction {
        private Relation relation;

        public RelationAction(String name, Relation newRelation) {
            super(name);
            assert name != null;
            assert newRelation != null;
            relation = newRelation;
        }

        public void actionPerformed(ActionEvent event) {
            try {
                URI pageToGo = relationMap.get(relation);
                setPage(pageToGo);
            } catch (Exception error) {
                showError("cannot go to " + relation + " page", error);
            }
        }

        public Relation getRelation() {
            return relation;
        }

        @Override
        public boolean isEnabledFor(GuiState state) {
            return state == GuiState.BROWSING;
        }
    }

    private static final String DEFAULT_TITLE = "Grotag";
    private static final int NO_INDEX = -1;
    private static final String SETTING_LAST_GUIDE_FILE_OPENED = "lastGuideFileOpened";
    private static final String SETTING_LAST_EXPORT_FOLDER = "lastExportFolder";

    private JLabel statusLabel;
    private JTextPane htmlPane;
    private JScrollPane htmlScrollPane;
    private JPanel statusPane;
    private GrotagToolBar toolBar;
    private JSplitPane splitPane;
    private JScrollPane messagePane;
    private Logger log;
    private Tools tools;
    private int pageHistoryIndex;
    private List<URI> pageHistory;
    private JProgressBar progressBar;
    private File tempFolder;
    private GuidePile pile;
    private Map<Relation, URI> relationMap;
    private List<JButton> relationButtons;
    private Map<URI, NodeInfo> urlToNodeMap;
    private Map<Object, Action> editorKitActionMap;
    private JFileChooser openChooser;
    private JFileChooser exportChooser;
    private MessageItemTableModel messageModel;
    private URI homeUri;
    private SyncLock pageLock;
    private JButton backButton;
    private JTable messageTable;
    private JButton forwardButton;
    private JButton homeButton;
    private Preferences settings;

    public GrotagFrame() {
        super(DEFAULT_TITLE);

        log = Logger.getLogger(GrotagFrame.class.getName());
        tools = Tools.getInstance();
        settings = Preferences.userNodeForPackage(Grotag.class);

        pageHistoryIndex = NO_INDEX;
        pageHistory = new ArrayList<URI>();
        relationMap = new TreeMap<Relation, URI>();
        relationButtons = new LinkedList<JButton>();
        pageLock = new SyncLock("page or file operations");
        openChooser = new JFileChooser();
        openChooser.addChoosableFileFilter(new GuideFileFilter());
        openChooser.setAcceptAllFileFilterUsed(false);
        exportChooser = new JFileChooser() {
            @Override
            public void approveSelection() {
                if (!getSelectedFile().isFile()) {
                    super.approveSelection();
                }
            }
        };
        exportChooser.setDialogTitle("Export");
        exportChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        exportChooser.setAccessory(new ExportAccessory());
        setLayout(new BorderLayout());
        toolBar = new GrotagToolBar();
        setUpHtmlPane();
        setUpEditorActionTable(htmlPane);
        setUpMessagePane();
        setUpSplitPane();
        setUpStatusPane();
        clearStatus();
        add(toolBar, BorderLayout.PAGE_START);
        add(splitPane, BorderLayout.CENTER);
        add(statusPane, BorderLayout.PAGE_END);
        setJMenuBar(new GrotagMenuBar());
        pack();
        progressBar.setVisible(false);
        tools.setGrotagIcon(this);
        setGuiState(GuiState.EMPTY);
    }

    private void setUpMessagePane() {
        messageModel = new MessageItemTableModel();
        messageTable = new JTable(messageModel);
        messageTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        tools.initColumnWidths(messageTable);
        messagePane = new JScrollPane(messageTable);
    }

    private void setUpSplitPane() {
        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, htmlScrollPane, messagePane);
        splitPane.setOneTouchExpandable(true);
        splitPane.setResizeWeight(0.90);
    }

    /**
     * Build a map to find editor actions by name.
     */
    private void setUpEditorActionTable(JTextComponent textComponent) {
        editorKitActionMap = new HashMap<Object, Action>();
        Action[] actionsArray = textComponent.getActions();
        for (int i = 0; i < actionsArray.length; i++) {
            Action a = actionsArray[i];
            editorKitActionMap.put(a.getValue(Action.NAME), a);
        }
    }

    private Action getActionByName(String name) {
        return editorKitActionMap.get(name);
    }

    public void clearStatus() {
        setStatus(" ");
    }

    public void setStatus(String text) {
        statusLabel.setText(text);
    }

    public void setGuiStateFor(List<Action> actions, GuiState state) {
        assert actions != null;
        assert state != null;
        for (Action action : actions) {
            if (action != null) {
                boolean actionIsEnabled;
                if (action instanceof AbstractGuiAction) {
                    AbstractGuiAction guiAction = (AbstractGuiAction) action;
                    actionIsEnabled = guiAction.isEnabledFor(state);
                } else {
                    // Enable non-GuiAction such as Edit > Copy during
                    // browsing only.
                    actionIsEnabled = (state == GuiState.BROWSING);
                }
                action.setEnabled(actionIsEnabled);
            }
        }
    }

    private void setGuiState(GuiState state) {
        assert state != null;
        // TODO: Remove: ((GrotagMenuBar) getJMenuBar()).setGuiState(state);
        List<Action> actions;
        actions = toolBar.getActionList();
        setGuiStateFor(actions, state);
        actions = ((GrotagMenuBar) getJMenuBar()).getActionList();
        setGuiStateFor(actions, state);
        ((GrotagMenuBar) getJMenuBar()).setGuiState(state);
    }

    private File createTempFolder() throws IOException {
        File result = File.createTempFile("grotag-view-", null);
        tools.delete(result);
        tools.mkdirs(result);
        return result;
    }

    private void showError(String message, Throwable details) {
        assert message != null;
        assert details != null;
        tools.showError(this, message, details);
    }

    /**
     * Read the GuidePile to browse starting with <code>guideFile</code>. In
     * order to resolve Amiga paths, look for <code>grotag.xml</code> in the
     * current folder, otherwise in the in the folder <code>guideFile</code> is
     * located in.
     */
    public void read(File guideFile) throws SAXException, ParserConfigurationException, IOException {
        assert guideFile != null;

        AmigaPathList amigaPaths = new AmigaPathList();
        File baseFolder = guideFile.getParentFile();
        File[] possibleGrotaxXmlFolders = new File[] { baseFolder, new File(System.getProperty("user.dir")) };
        int folderIndex = 0;
        boolean grotagXmlFound = false;

        while (!grotagXmlFound && (folderIndex < possibleGrotaxXmlFolders.length)) {
            File grotagXml = new File(possibleGrotaxXmlFolders[folderIndex], "grotag.xml");
            try {
                amigaPaths.read(grotagXml);
                grotagXmlFound = true;
            } catch (FileNotFoundException errorToIgnore) {
                // Just move on and try the next file.
                folderIndex += 1;
            }
        }
        read(guideFile, amigaPaths);
    }

    /**
     * Read the GuidePile to browse starting with <code>guideFile</code>. In
     * order to resolve Amiga paths, use <code>newAmigaPaths</code>.
     */
    public void read(File guideFile, AmigaPathList newAmigaPaths) {
        assert guideFile != null;
        assert newAmigaPaths != null;
        ReadWorker worker = new ReadWorker(guideFile, newAmigaPaths);
        worker.start();
    }

    private void doRead(File guideFile, AmigaPathList newAmigaPaths) throws IOException {
        assert guideFile != null;
        assert newAmigaPaths != null;

        File newTempFolder = createTempFolder();
        GuidePile newPile = null;

        synchronized (pageLock) {
            progressBar.setValue(0);
            progressBar.setIndeterminate(true);
            progressBar.setVisible(true);
            setGuiState(GuiState.OPENING);
            try {
                setStatus("Reading " + guideFile);
                newPile = GuidePile.createGuidePile(guideFile, newAmigaPaths);
                HtmlDomFactory factory = new HtmlDomFactory(newPile, newTempFolder);

                factory.copyStyleFile();
                factory.setAddDublinCore(false);
                factory.setAddNavigationBar(false);
                factory.setCopyNonGuides(false);

                // Compute number of nodes in pile to show progress.
                // TODO: Use number of items as base for progress.
                int nodeCount = 0;
                for (Guide guide : newPile.getGuides()) {
                    nodeCount += guide.getNodeInfos().size();
                }

                progressBar.setMinimum(0);
                progressBar.setMaximum(nodeCount);
                progressBar.setIndeterminate(false);

                urlToNodeMap = new HashMap<URI, NodeInfo>();
                int nodesWritten = 0;
                for (Guide guide : newPile.getGuides()) {
                    for (NodeInfo nodeInfo : guide.getNodeInfos()) {
                        setStatus("Reading " + guide.getDatabaseInfo().getName() + "/" + nodeInfo.getName());
                        File targetFile = factory.getTargetFileFor(guide, nodeInfo);
                        URI targetUrl = targetFile.toURI();
                        org.w3c.dom.Document htmlDocument = factory.createNodeDocument(guide, nodeInfo);
                        DomWriter htmlWriter = new DomWriter(DomWriter.Dtd.HTML);
                        htmlWriter.write(htmlDocument, targetFile);
                        urlToNodeMap.put(targetUrl, nodeInfo);
                        nodesWritten += 1;
                        progressBar.setValue(nodesWritten);
                    }
                }
                settings.put(SETTING_LAST_GUIDE_FILE_OPENED, guideFile.getAbsolutePath());
            } catch (Throwable error) {
                showError("cannot read " + tools.sourced(guideFile), error);
            } finally {
                clearStatus();
                progressBar.setVisible(false);
                if (newPile != null) {
                    // Start showing new guide.
                    if (tempFolder != null) {
                        tools.attemptToDeleteAll(tempFolder);
                    }
                    tempFolder = newTempFolder;
                    pile = newPile;
                    homeUri = pile.getFirstHtmlFile(tempFolder).toURI();
                    pageHistory.clear();
                    setPage(homeUri);
                } else {
                    // Error while preparing new guide; keep the old one.
                    tools.attemptToDeleteAll(newTempFolder);
                }
                // FIXME: Check if this call causes an exception in the Swing
                // thread in case there are no messages.
                messageModel.update();
                tools.initColumnWidths(messageTable);

                if (pile == null) {
                    setGuiState(GuiState.EMPTY);
                } else {
                    setGuiState(GuiState.BROWSING);
                }
            }
        }
    }

    public void setPage(File pageFile) throws IOException {
        assert pageFile != null;
        setPage(pageFile.toURI());
    }

    /**
     * The URI of the page currently viewing.
     */
    private URI getPage() {
        URI result;
        synchronized (pageLock) {
            assert pageHistory.size() > 0;
            assert pageHistoryIndex >= 0;
            result = pageHistory.get(pageHistoryIndex);
        }
        return result;
    }

    private void setBackForwardAndHomeButtonEnabled() {
        backButton.setEnabled(pageHistoryIndex > 0);
        forwardButton.setEnabled((pageHistoryIndex != NO_INDEX) && (pageHistoryIndex < pageHistory.size() - 1));
        boolean enableHomeAction = (pageHistory.size() > 0);
        if (enableHomeAction) {
            enableHomeAction = !homeUri.equals(getPage());
        }
        homeButton.setEnabled(enableHomeAction);
    }

    public void setPage(URI pageUri) throws IOException {
        synchronized (pageLock) {
            setPageWithoutHistory(pageUri);
            while ((pageHistoryIndex != NO_INDEX) && (pageHistory.size() > (pageHistoryIndex + 1))) {
                pageHistory.remove(pageHistory.size() - 1);
            }
            pageHistory.add(pageUri);
            pageHistoryIndex = pageHistory.size() - 1;
            logHistory();
            setBackForwardAndHomeButtonEnabled();
        }
    }

    private void logHistory() {
        int historyIndex = 0;
        log.info("history pointer at " + pageHistoryIndex);
        for (URI uri : pageHistory) {
            String line;
            if (historyIndex == pageHistoryIndex) {
                line = "->";
            } else {
                line = "  ";
            }
            line += " " + uri;
            log.info(line);
            historyIndex += 1;
        }
    }

    private void setPageWithoutHistory(URI pageUri) throws IOException {
        assert pageUri != null;
        synchronized (pageLock) {
            log.info("set page to: " + tools.sourced(pageUri.toString()));
            htmlPane.setPage(pageUri.toURL());

            HtmlInfo htmlInfo = new HtmlInfo(pageUri);
            String title = htmlInfo.getTitle();

            if (title != null) {
                setTitle(title + " - Grotag");
            } else {
                setTitle(DEFAULT_TITLE);
            }

            relationMap = htmlInfo.getRelationMap();
            for (JButton button : relationButtons) {
                Relation buttonRelation = ((RelationAction) button.getAction()).getRelation();
                boolean relationEnabled = relationMap.containsKey(buttonRelation);
                button.setEnabled(relationEnabled);
            }
        }
    }

    private final void setUpHtmlPane() {
        htmlPane = new JTextPane();
        htmlPane.addHyperlinkListener(this);
        htmlPane.setPreferredSize(new Dimension(640, 512));
        htmlPane.setEditable(false);
        htmlScrollPane = new JScrollPane(htmlPane);
    }

    private final void setUpStatusPane() {
        statusPane = new JPanel();
        statusPane.setLayout(new BoxLayout(statusPane, BoxLayout.LINE_AXIS));
        statusLabel = new JLabel("Initialising");
        int rigidSize = statusLabel.getPreferredSize().height;
        progressBar = new JProgressBar();
        progressBar.setPreferredSize(new Dimension(100, progressBar.getPreferredSize().height));
        progressBar.setMaximumSize(progressBar.getPreferredSize());

        statusPane.add(statusLabel);
        statusPane.add(Box.createHorizontalGlue());
        statusPane.add(progressBar);
        statusPane.add(Box.createRigidArea(new Dimension(rigidSize, 0)));
    }

    /**
     * Handle to this frame for inner classes which cannot refer to it using
     * <code>this</code>.
     */
    private JFrame getGrotagFrame() {
        return this;
    }

    public void hyperlinkUpdate(HyperlinkEvent linkEvent) {
        try {
            if (linkEvent.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                URI uriToOpen = linkEvent.getURL().toURI();
                try {
                    BufferedImage possibleImage = null;
                    try {
                        possibleImage = ImageIO.read(uriToOpen.toURL());
                    } catch (IIOException error) {
                        log.fine("assume url is not an image: " + uriToOpen);
                    }
                    if (possibleImage == null) {
                        setPage(uriToOpen);
                    } else {
                        ImageFrame imageFrame = new ImageFrame(possibleImage);
                        imageFrame.setTitle(tools.getName(uriToOpen));
                        imageFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                        imageFrame.pack();
                        imageFrame.setVisible(true);
                    }
                } catch (IOException error) {
                    showError("cannot open URL: " + tools.sourced(uriToOpen.toString()), error);
                }
            } else if (linkEvent.getEventType() == HyperlinkEvent.EventType.ENTERED) {
                setStatus("Go to " + linkEvent.getURL().toExternalForm());
            } else if (linkEvent.getEventType() == HyperlinkEvent.EventType.EXITED) {
                clearStatus();
            } else {
                log.fine("ignored hyperlink event: " + linkEvent.getEventType());
            }
        } catch (Throwable error) {
            showError("cannot process hyperlink event", error);
        }
    }

    @Override
    public void dispose() {
        synchronized (pageLock) {
            if (htmlPane != null) {
                htmlPane.removeHyperlinkListener(this);
            }
            pile = null;
            try {
                settings.flush();
            } catch (BackingStoreException error) {
                tools.showError(this, "cannot store settings", error);
            }
            if (tempFolder != null) {
                tools.attemptToDeleteAll(tempFolder);
                tempFolder = null;
            }
            super.dispose();
        }
    }
}
