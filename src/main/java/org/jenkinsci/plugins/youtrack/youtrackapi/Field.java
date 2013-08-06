package org.jenkinsci.plugins.youtrack.youtrackapi;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * This represents a field in YouTrack.
 */
public class Field {
    private String name;
    private String url;
    private String type;
    private String defaultBundle;

    public Field(String name, String url) {
        this.name = name;
        this.url = url;
    }

    public String getType() {
        return type;
    }

    public String getDefaultBundle() {
        return defaultBundle;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public static class FieldListHandler extends DefaultHandler {
        private List<Field> fields = new ArrayList<Field>();

        @Override
        public void startDocument() throws SAXException {
            super.startDocument();
            fields = new ArrayList<Field>();
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            super.startElement(uri, localName, qName, attributes);
            if (qName.equals("customFieldPrototype")) {
                String fieldName = attributes.getValue("name");
                String fieldUrl = attributes.getValue("url");
                fields.add(new Field(fieldName, fieldUrl));
            }
        }

        public List<Field> getFields() {
            return fields;
        }
    }

    public static class FieldHandler extends DefaultHandler {
       private Field field;

        public FieldHandler(String name, String url) {
            field = new Field(name, url);
        }

        public Field getField() {
            return field;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            super.startElement(uri, localName, qName, attributes);
            if (qName.equals("customFieldPrototype")) {
                field.type = attributes.getValue("type");
            }
            if (qName.equals("defaultParam") && attributes.getValue("name").equals("defaultBundle")) {
                field.defaultBundle=  attributes.getValue("value");
            }
        }
    }
}
