package org.jenkinsci.plugins.youtrack;

import jenkins.model.Jenkins;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

/**
 * This is a class to persist the commit ids processed by the YouTrack plugin for Jenkins
 */
public class YoutrackProcessedRevisionsSaver {
    private static final Logger LOGGER = Logger.getLogger(YoutrackProcessedRevisionsSaver.class.getName());


    public Set<String> processedIds;
    private final File file;

    public YoutrackProcessedRevisionsSaver() {
        load();
        file = new File(Jenkins.getInstance().getRootDir(), "youtrack-processed");

    }

    private void load() {
        processedIds = new HashSet<String>();
        if (!file.exists()) {
            try {
                boolean newFile = file.createNewFile();
                if (!newFile) {
                    LOGGER.error("Could not create youtrack processed file");
                }
            } catch (IOException e) {
                LOGGER.error("Could not create youtrack processed file", e);
            }
        } else {
            try {
                FileInputStream fileInputStream = new FileInputStream(file);
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));
                String l;
                while ((l = bufferedReader.readLine()) != null) {
                    processedIds.add(l);
                }
                fileInputStream.close();
            } catch (FileNotFoundException e) {
                LOGGER.error("Could not load youtrack processed file", e);
            } catch (IOException e) {
                LOGGER.error("Could not load youtrack processed file", e);
            }
        }

    }

    public synchronized boolean isProcessed(String revisionId) {
        return processedIds.contains(revisionId);
    }

    public synchronized void addProcessed(String revisionId) {
        processedIds.add(revisionId);
        try {
            FileWriter fileWriter = new FileWriter(file, true);
            fileWriter.append(revisionId).append("\n");
            fileWriter.close();
        } catch (IOException e) {
            LOGGER.error("Could not write to youtrack processed file", e);
        }
    }
}
