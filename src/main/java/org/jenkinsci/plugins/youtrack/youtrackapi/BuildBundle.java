package org.jenkinsci.plugins.youtrack.youtrackapi;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents a build bundle.
 */
@RequiredArgsConstructor
public class BuildBundle {
    @Getter private final String name;

    public static class Handler extends DefaultHandler {
        @Getter
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
    }
}
