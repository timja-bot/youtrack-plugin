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
 * This class represents a project.
 */
@NoArgsConstructor
public class Project {
    /**
     * The short name of the project.
     */
    @Getter @Setter private String shortName;

    /**
     * Constructor for a project.
     * @param shortName the short name of the project in YouTrack.
     */
    public Project(String shortName) {
        this.shortName = shortName;
    }

    /**
     * Handler for the project list.
     */
    static class ProjectListHandler extends DefaultHandler {
        /**
         * Returns the projects found in the xml, should first be called when parsing is over.
         */
        @Getter private List<Project> projects;

        @Override
        public void startDocument() throws SAXException {
            super.startDocument();
            this.projects = new ArrayList<Project>();
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            super.startElement(uri, localName, qName, attributes);
            if (qName.equals("project")) {
                Project project = new Project();
                project.setShortName(attributes.getValue("shortName"));
                projects.add(project);
            }
        }
    }
}
