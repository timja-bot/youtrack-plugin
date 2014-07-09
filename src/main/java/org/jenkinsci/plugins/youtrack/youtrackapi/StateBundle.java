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
 * This class represents a state bundle.
 */
@RequiredArgsConstructor
public class StateBundle {
    /**
     * Name of the bundle.
     */
    @Getter private final String name;
    /**
     * Url of the bundle.
     */
    @Getter private final String url;

    /**
     * The states for this state bundle.
     */
    @Getter private final List<State> states = new ArrayList<State>();

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
