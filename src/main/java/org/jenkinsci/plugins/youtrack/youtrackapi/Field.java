package org.jenkinsci.plugins.youtrack.youtrackapi;

import lombok.Getter;
import lombok.Setter;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * This represents a field in YouTrack.
 */
public class Field {
    @Getter @Setter private String name;
    @Getter @Setter private String url;
    @Getter private String type;
    @Getter private String defaultBundle;

    public Field(String name, String url) {
        this.name = name;
        this.url = url;
    }

    public static class FieldListHandler extends DefaultHandler {
        @Getter private List<Field> fields = new ArrayList<Field>();

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
    }

    public static class FieldHandler extends DefaultHandler {
        @Getter private Field field;

        public FieldHandler(String name, String url) {
            field = new Field(name, url);
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
            if (qName.equals("projectCustomField")) {
                field.type = attributes.getValue("type");
            }
            if (qName.equals("param") && attributes.getValue("name").equals("bundle")) {
                field.defaultBundle=  attributes.getValue("value");
            }

        }
    }
}
