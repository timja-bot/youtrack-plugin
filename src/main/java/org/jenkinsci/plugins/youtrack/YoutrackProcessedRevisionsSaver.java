package org.jenkinsci.plugins.youtrack;

import jenkins.model.Jenkins;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is a class to persist the commit ids processed by the YouTrack plugin for Jenkins
 */
public class YoutrackProcessedRevisionsSaver {
    private Logger LOGGER = Logger.getLogger(YoutrackProcessedRevisionsSaver.class.getName());

    public Set<String> processedIds;
    private File file;

    public YoutrackProcessedRevisionsSaver() {
        Jenkins instance = Jenkins.getInstance();
        if (instance != null) {
            file = new File(instance.getRootDir(), "youtrack-processed");
            load();
        }

    }

    private void load() {
        processedIds = new HashSet<String>();
        if (!file.exists()) {
            try {
                boolean newFile = file.createNewFile();
                if (!newFile) {
                    LOGGER.log(Level.SEVERE, "Could not create youtrack processed file");
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Could not create youtrack processed file", e);
            }
        } else {
            try(FileInputStream fileInputStream = new FileInputStream(file)) {
                try(BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream, StandardCharsets.UTF_8))) {
                    String l;
                    while ((l = bufferedReader.readLine()) != null) {
                        processedIds.add(l);
                    }
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Could not load youtrack processed file", e);
            }
        }

    }

    public synchronized boolean isProcessed(String revisionId) {
        return processedIds.contains(revisionId);
    }

    public synchronized void addProcessed(String revisionId) {
        processedIds.add(revisionId);
        try(PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.UTF_8))) {
            printWriter.append(revisionId).append("\n");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Could not write to youtrack processed file", e);
        }
    }
}
