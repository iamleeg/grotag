package net.sf.grotag.guide;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

import net.sf.grotag.common.AmigaTools;
import net.sf.grotag.common.Tools;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

/**
 * Factory to create a DOM for DocBook XML.
 * 
 * @author Thomas Aglassinger
 */
public class DocBookDomFactory extends AbstractDomFactory {

    private Logger log;
    private Tools tools;
    private Map<String, String> agNodeToDbNodeMap;

    public DocBookDomFactory(GuidePile newPile) throws ParserConfigurationException {
        super(newPile);

        log = Logger.getLogger(DocBookDomFactory.class.getName());
        tools = Tools.getInstance();

        // Map the Amigaguide node names to DocBook id's that conform to the
        // NCName definition.
        agNodeToDbNodeMap = new HashMap<String, String>();
        int nodeCounter = 1;
        for (Guide guide : newPile.getGuides()) {
            for (NodeInfo nodeInfo : guide.getNodeInfos()) {
                String agNodeName = nodeKey(guide, nodeInfo);
                String dbNodeName = "n" + nodeCounter;

                log.log(Level.INFO, "add mapped node {0} from {1}", new Object[] { dbNodeName, agNodeName });

                assert !agNodeToDbNodeMap.containsKey(agNodeName) : "duplicate agNode: " + tools.sourced(agNodeName);
                assert !agNodeToDbNodeMap.containsValue(dbNodeName) : "duplicate dbNode: " + tools.sourced(dbNodeName);

                agNodeToDbNodeMap.put(agNodeName, dbNodeName);
                nodeCounter += 1;
            }
        }
    }

    /**
     * Create node that represents the whole <code>guide</code>.
     * 
     * @see #appendNodeContent(Element, Guide, NodeInfo)
     */
    protected Element createGuideNode(Guide guide) throws IOException {
        assert guide != null;

        Element result = getDom().createElement("chapter");
        String chapterTitle = guide.getDatabaseInfo().getName();

        log.info("create chapter " + tools.sourced(chapterTitle));
        // Create chapter title.
        Element title = getDom().createElement("title");
        Text titleText = getDom().createTextNode(chapterTitle);
        title.appendChild(titleText);
        result.appendChild(title);

        for (NodeInfo nodeInfo : guide.getNodeInfos()) {
            Element section = createNodeBody(guide, nodeInfo);
            result.appendChild(section);
            appendNodeContent(section, guide, nodeInfo);
        }
        return result;
    }

    @Override
    protected Node createAmigaguideNode() {
        Element result = getDom().createElement("productname");
        result.setAttribute("class", "registered");
        result.appendChild(getDom().createTextNode("Amigaguide"));
        return result;
    }

    @Override
    protected Node createLinkToGuideNode(Guide sourceGuide, File guideFile, String targetNode, String linkLabel) {
        Node result;
        Element resultElement = null;
        Guide guide = getPile().getGuide(guideFile);

        if (guide != null) {
            String mappedNode = agNodeToDbNodeMap.get(nodeKey(guide, targetNode));
            if (mappedNode != null) {
                resultElement = getDom().createElement("link");
                resultElement.setAttribute("linkend", mappedNode);
                resultElement.appendChild(getDom().createTextNode(linkLabel));
            }
        }

        if (resultElement != null) {
            result = resultElement;
        } else {
            result = getDom().createTextNode(linkLabel);
            String fileText = tools.sourced(guideFile);
            String nodeText = tools.sourced(targetNode);
            log.warning("skipped link to unknown node " + nodeText + " in " + fileText);
        }

        return result;
    }

    @Override
    protected Node createOtherFileLinkNode(Guide sourceGuide, File linkedFile, String linkLabel) {
        // TODO: Copy linked file to same
        // folder as target document.
        URL linkedUrl = createUrl("file", "localhost", linkedFile);
        Element result = getDom().createElement("ulink");
        result.setAttribute("url", linkedUrl.toExternalForm());
        result.appendChild(getDom().createTextNode(linkLabel));
        return result;
    }

    @Override
    protected Node createEmbeddedFile(File embeddedFile) {
        Element result = createParagraph(Wrap.NONE, false);
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(embeddedFile),
                    AmigaTools.ENCODING));
            try {
                String textLine = reader.readLine();
                while (textLine != null) {
                    result.appendChild(getDom().createTextNode(textLine + "\n"));
                    textLine = reader.readLine();
                }
            } finally {
                reader.close();
            }
        } catch (UnsupportedEncodingException error) {
            throw new IllegalStateException("Amiga encoding not supported", error);
        } catch (IOException error) {
            result = getDom().createElement("caution");
            Element cautionTitle = getDom().createElement("title");
            Element cautionContent = getDom().createElement("para");
            cautionTitle.appendChild(getDom().createTextNode("Missing embedded file"));
            cautionContent.appendChild(getDom().createTextNode(
                    "@embed for " + tools.sourced(embeddedFile) + " failed: " + error.getMessage()));
            result.appendChild(cautionTitle);
            result.appendChild(cautionContent);
        }

        return result;
    }

    @Override
    protected Node createLinkToNonGuideNode(Guide sourceGuide, File linkedFile, String linkLabel) {
        URL linkedUrl = createUrl("file", "localhost", linkedFile);
        Element elementToAppend = getDom().createElement("ulink");
        elementToAppend.setAttribute("url", linkedUrl.toExternalForm());
        elementToAppend.appendChild(getDom().createTextNode(linkLabel));
        return elementToAppend;
    }

    /**
     * Create a book that contains a chapter for each Amigaguide document and a
     * section for each node.
     */
    public Document createBook() throws DOMException, IOException {
        log.info("create book");
        Element bookElement = getDom().createElement("book");
        getDom().appendChild(bookElement);
        if (getPile().getGuides().size() > 0) {
            Node metaInfoNode = createMetaInfoNode(getPile().getGuides().get(0));
            if (metaInfoNode != null) {
                bookElement.appendChild(metaInfoNode);
            }
        }
        // TODO #3: Use streams.
        for (Guide guide : getPile().getGuides()) {
            bookElement.appendChild(createGuideNode(guide));
        }

        return getDom();
    }

    /**
     * Create node that holds all the meta information about the document, such
     * as <code>@author</code>, <code>@(c)</code>, <code>@keywords</code> and so on.
     */
    private Node createMetaInfoNode(Guide guide) {
        Element result;
        if (guide == getPile().getGuides().get(0)) {
            DatabaseInfo dbInfo = guide.getDatabaseInfo();
            result = getDom().createElement("bookinfo");

            // Add document title.
            String title = dbInfo.getName();
            assert title != null;
            Element titleNode = getDom().createElement("title");
            titleNode.appendChild(getDom().createTextNode(title));
            result.appendChild(titleNode);

            // Add optional author(s).
            String authorText = dbInfo.getAuthor();
            String[] authors = tools.separated(authorText);
            if (authors != null) {
                for (String author : authors) {
                    author = tools.cutOffAt(author, '(');
                    author = tools.cutOffAt(author, ',');
                    author = author.replace('\t', ' ');
                    author = author.trim();
                    if (author.length() > 0) {
                        Element authorElement = getDom().createElement("author");
                        String[] nameParts = author.split(" ");
                        // TODO #3: Check if streams are useful here.
                        for (int partIndex = 0; partIndex < nameParts.length; partIndex += 1) {
                            String namePart = nameParts[partIndex];
                            String namePartElementName;
                            if ((nameParts.length > 1) && (partIndex == 0)) {
                                namePartElementName = "firstname";
                            } else if (partIndex == (nameParts.length - 1)) {
                                namePartElementName = "surname";
                            } else {
                                namePartElementName = "othername";
                            }
                            Element nameElement = getDom().createElement(namePartElementName);
                            nameElement.appendChild(getDom().createTextNode(namePart));
                            authorElement.appendChild(nameElement);
                        }
                        result.appendChild(authorElement);
                    }
                }
            }

            // Add (optional) version information.
            String versionText = dbInfo.getVersion();
            if (versionText != null) {
                Element versionElement = getDom().createElement("releaseinfo");
                versionElement.appendChild(getDom().createTextNode("$VER: " + versionText));
                result.appendChild(versionElement);
            }

            // Add (optional) copyright information.
            String copyrightYear = dbInfo.getCopyrightYear();
            if (copyrightYear != null) {
                String copyrightHolder = dbInfo.getCopyrightHolder();
                assert copyrightHolder != null;

                Element copyrightElement = getDom().createElement("copyright");
                Element yearElement = getDom().createElement("year");
                Element holderElement = getDom().createElement("holder");
                yearElement.appendChild(getDom().createTextNode(copyrightYear));
                holderElement.appendChild(getDom().createTextNode(copyrightHolder));
                copyrightElement.appendChild(yearElement);
                copyrightElement.appendChild(holderElement);
                result.appendChild(copyrightElement);
            }

        } else {
            result = null;
        }
        return result;
    }

    @Override
    protected Element createParagraph(Wrap wrap, boolean isProportional) {
        Element result;
        String tagName;

        if (wrap == Wrap.NONE) {
            tagName = "literallayout";
        } else {
            tagName = "para";
            assert wrap != Wrap.DEFAULT;
        }
        result = getDom().createElement(tagName);
        if (!isProportional && (wrap == Wrap.NONE)) {
            result.setAttribute("class", "monospaced");
        }

        return result;
    }

    private String getIdFor(Guide guideContainingNode, NodeInfo nodeInfo) {
        return agNodeToDbNodeMap.get(nodeKey(guideContainingNode, nodeInfo));
    }

    @Override
    protected Element createNodeBody(Guide guide, NodeInfo nodeInfo) {
        Element result = getDom().createElement("section");
        String sectionId = getIdFor(guide, nodeInfo);
        String sectionTitle = nodeInfo.getTitle();

        log.log(Level.INFO, "create section with id={0} from node {1}: {2}", new Object[] { tools.sourced(sectionId),
                tools.sourced(nodeInfo.getName()), tools.sourced(sectionTitle) });
        result.setAttribute("id", sectionId);

        return result;
    }

    @Override
    protected Element createNodeHeading(String heading) {
        assert heading != null;
        Element result = getDom().createElement("title");
        result.appendChild(getDom().createTextNode(heading));
        return result;
    }
}
