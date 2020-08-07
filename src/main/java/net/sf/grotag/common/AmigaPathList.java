package net.sf.grotag.common;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * List of mappings of Amiga paths to local folders.
 * 
 * @author Thomas Aglassinger
 */
public class AmigaPathList {
    /**
     * A mapping of an Amiga path to a local folder.
     * 
     * @author Thomas Aglassinger
     */
    public static class AmigaPathFilePair {
        private String amigaPath;
        private File localFolder;

        private AmigaPathFilePair(String newAmigaPath) {
            assert newAmigaPath != null;
            amigaPath = newAmigaPath;
        }

        private void setLocalFolder(String newLocalFolder) {
            assert newLocalFolder != null;
            localFolder = new File(newLocalFolder);
        }

        public String getAmigaPath() {
            return amigaPath;
        }

        public File getLocalFolder() {
            return localFolder;
        }
    }

    /**
     * SAX parser for grotag Amiga path list.
     * 
     * @author Thomas Aglassinger
     */
    private class AmigaPathXmlParser extends DefaultHandler {
        private Logger logXml = Logger.getLogger(AmigaPathXmlParser.class.getName());
        private List<AmigaPathFilePair> targetList;
        private String text;
        private AmigaPathFilePair currentPair;

        private AmigaPathXmlParser(List<AmigaPathFilePair> newTargetList) {
            assert newTargetList != null;
            targetList = newTargetList;
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            text += new String(ch, start, length);
            super.characters(ch, start, length);
        }

        @Override
        public void endElement(String uri, String localName, String name) throws SAXException {
            String lowerName = name.toLowerCase();
            if (lowerName.equals("amiga")) {
                currentPair = new AmigaPathFilePair(text.toLowerCase());
            } else if (lowerName.equals("local")) {
                if (currentPair != null) {
                    currentPair.setLocalFolder(text);
                    targetList.add(currentPair);
                } else {
                    logXml.warning("ignored <local> without previous <amiga>: " + text);
                }
            }
            super.endElement(uri, localName, name);
        }

        @Override
        public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
            text = "";
            super.startElement(uri, localName, name, attributes);
        }
    }

    private List<AmigaPathFilePair> pathList;

    public AmigaPathList() {
        pathList = new LinkedList<AmigaPathFilePair>();
    }

    public void read(File inFile) throws SAXException, IOException, ParserConfigurationException {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        SAXParser sp = spf.newSAXParser();
        AmigaPathXmlParser parser = new AmigaPathXmlParser(pathList);
        sp.parse(inFile, parser);
    }

    /**
     * Add undefined Amiga path.
     */
    public void addUndefined(String newAmigaPath) {
        assert newAmigaPath != null;
        assert newAmigaPath.contains(":");
        pathList.add(new AmigaPathFilePair(newAmigaPath));

    }

    public List<AmigaPathFilePair> items() {
        return pathList;
    }
}
