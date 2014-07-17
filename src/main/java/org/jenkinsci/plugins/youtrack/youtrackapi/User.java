package org.jenkinsci.plugins.youtrack.youtrackapi;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * This object represents a user.
 */
@NoArgsConstructor
public class User {
    /**
     * The username/login of the user.
     */
    @Getter @Setter private String username;

    /**
     * True if the user object is logged in.
     */
    @Getter @Setter private boolean loggedIn;

    /**
     * The set of cookies if this user has a session.
     */
    @Getter private transient List<String> cookies = new ArrayList<String>();

    /**
     * Handler for parsing user query if will find the first user
     * in the result.
     */
    public static class UserRefHandler extends DefaultHandler {
        /**
         * Gets the first user found. Should first be called when parsing if finished.
         */
        @Getter private User user;

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            super.startElement(uri, localName, qName, attributes);
            if (user == null && qName.equals("user")) {
                user = new User();
                user.username = attributes.getValue("login");
            }
        }
    }
}
