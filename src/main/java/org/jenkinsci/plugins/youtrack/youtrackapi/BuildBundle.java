package org.jenkinsci.plugins.youtrack.youtrackapi;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents a build bundle.
 */
public class BuildBundle {
    private String name;

    public BuildBundle(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
    public static class Handler extends DefaultHandler {
        private List<BuildBundle> bundles;

        @Override
        public void startDocument() throws SAXException {
            super.startDocument();
            bundles = new ArrayList<BuildBundle>();
        }


        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            super.startElement(uri, localName, qName, attributes);
            if("buildBundle".equals(qName))  {
                String bundleName = attributes.getValue("name");
                bundles.add(new BuildBundle(bundleName));
            }
        }

        public List<BuildBundle> getBundles() {
            return bundles;
        }
    }
}
