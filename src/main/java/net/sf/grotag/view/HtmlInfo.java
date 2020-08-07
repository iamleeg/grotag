package net.sf.grotag.view;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.HTML.Tag;
import javax.swing.text.html.parser.ParserDelegator;

import net.sf.grotag.guide.HtmlDomFactory;
import net.sf.grotag.guide.Relation;

/**
 * Information about an HTML document.
 * 
 * @author Thomas Aglassinger
 */
class HtmlInfo {
    private Map<Relation, URI> relationMap;
    private Map<String, Relation> relToRelationMap;
    private Logger log;
    private URI baseUri;
    private String title;

    /**
     * HTML parser callback to extract &lt;link&gt; relations.
     * 
     * @author Thomas Aglassinger
     */
    private class InfoCallback extends HTMLEditorKit.ParserCallback {
        private boolean insideTitle;

        public InfoCallback() {
            relationMap.clear();
            title = "";
            insideTitle = false;
        }

        @Override
        public void handleSimpleTag(Tag tag, MutableAttributeSet attributes, int pos) {
            assert tag != null;
            assert attributes != null;
            if (tag == Tag.LINK) {
                String rel = (String) attributes.getAttribute(HTML.Attribute.REL);
                if (rel != null) {
                    String href = (String) attributes.getAttribute(HTML.Attribute.HREF);
                    if (href != null) {
                        Relation relation = relToRelationMap.get(rel);
                        if (relation != null) {
                                relationMap.put(relation, baseUri.resolve(href));
                                log.info("added relation: " + relation + "=" + relationMap.get(relation));
                        } else if (!rel.equals(HtmlDomFactory.REL_STYLESHEET)) {
                            log.warning("ignored unknown rel: " + rel);
                        }
                    } else {
                        log.warning("ignored <link> without href at " + pos);
                    }
                } else {
                    log.warning("ignored <link> without rel at " + pos);
                }
            }
        }

        @Override
        public void handleStartTag(Tag tag, MutableAttributeSet a, int pos) {
            if (tag == HTML.Tag.TITLE) {
                insideTitle = true;
            }
        }

        @Override
        public void handleComment(char[] data, int pos) {
            // Do nothing.
        }

        @Override
        public void handleEndOfLineString(String eol) {
            // Do nothing.
        }

        @Override
        public void handleEndTag(Tag tag, int pos) {
            if (tag == HTML.Tag.TITLE) {
                insideTitle = false;
            }
        }

        @Override
        public void handleText(char[] data, int pos) {
            if (insideTitle) {
                title += new String(data);
            }
        }
    }

    HtmlInfo(URI newBaseUrl) throws IOException {
        assert newBaseUrl != null;

        log = Logger.getLogger(HtmlInfo.class.getName());
        relationMap = new TreeMap<Relation, URI>();
        relToRelationMap = new TreeMap<String, Relation>();
        relToRelationMap.put("help", Relation.help);
        relToRelationMap.put("index", Relation.index);
        relToRelationMap.put("next", Relation.next);
        relToRelationMap.put("prev", Relation.previous);
        relToRelationMap.put("toc", Relation.contents);

        baseUri = newBaseUrl;
        InfoCallback callback = new InfoCallback();
        ParserDelegator parser = new ParserDelegator();
        Reader reader = new InputStreamReader(baseUri.toURL().openStream());
        try {
            parser.parse(reader, callback, true);
        } finally {
            reader.close();
        }
    }

    /**
     * The document relations as specified with
     * <code>&lt;link rel="..." href="..."&gt;</code>.
     */
    Map<Relation, URI> getRelationMap() {
        return relationMap;
    }

    /**
     * The document title as specified with <code>&lt;title&gt;</code>.
     */
    String getTitle() {
        return title;
    }
}
