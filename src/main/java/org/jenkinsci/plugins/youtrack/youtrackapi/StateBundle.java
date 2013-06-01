package org.jenkinsci.plugins.youtrack.youtrackapi;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents a state bundle.
 */
public class StateBundle {
    /**
     * Name of the bundle.
     */
    private String name;
    /**
     * Url of the bundle.
     */
    private String url;

    /**
     * The states for this state bundle.
     */
    private List<State> states;

    public StateBundle(String name, String url) {
        this.name = name;
        this.url = url;
        this.states = new ArrayList<State>();
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public List<State> getStates() {
        return states;
    }

    public static class StateBundleListHandler extends DefaultHandler {
        private ArrayList<StateBundle> bundles;

        @Override
        public void startDocument() throws SAXException {
            super.startDocument();
            this.bundles = new ArrayList<StateBundle>();
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            super.startElement(uri, localName, qName, attributes);
            if (qName.equals("stateBundle")) {
                String bundleName = attributes.getValue("name");
                String bundleUrl = attributes.getValue("url");
                StateBundle stateBundle = new StateBundle(bundleName, bundleUrl);
                bundles.add(stateBundle);
            }
        }

        public List<StateBundle> getStateBundles() {
            return bundles;
        }
    }

    public static class StateBundleHandler extends DefaultHandler {
        private StateBundle stateBundle;
        private StringBuilder stringBuilder = new StringBuilder();
        boolean inStateValue;
        private State currentState;

        public StateBundleHandler(StateBundle stateBundle) {
            this.stateBundle = stateBundle;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            stringBuilder.setLength(0);
            super.startElement(uri, localName, qName, attributes);
            if (qName.equals("state")) {
                inStateValue = true;
                State state = new State();
                currentState = state;
                state.setDescription(attributes.getValue("description"));
                state.setResolved(Boolean.valueOf(attributes.getValue("isResolved")));
            }

        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            super.characters(ch, start, length);
            if (inStateValue) {
                stringBuilder.append(ch, start, length);
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            super.endElement(uri, localName, qName);
            if (qName.equals("state")) {
                currentState.setValue(stringBuilder.toString());
                stateBundle.states.add(currentState);
                currentState = null;
            }
        }
    }
}
