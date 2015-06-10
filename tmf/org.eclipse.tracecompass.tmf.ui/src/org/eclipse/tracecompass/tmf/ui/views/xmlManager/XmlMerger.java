package org.eclipse.tracecompass.tmf.ui.views.xmlManager;

import java.io.File;

/**
 * This class offers methods to allows the merge of
 * XML files.
 *
 * @author jonathansauve
 *
 */
public class XmlMerger {

    private static int currentProgress = 0;

    /** Make this class non-instantiable */
    private XmlMerger() {}

    /**
     * @param file1 The first file
     * @param file2 The second file
     */
    public static void mergeXmlFiles(File file1, File file2) {


    }

    /**
     * This method returns the current progress of the merge,
     * in percentage. The value is between 0 and 100. The value -1
     * is return if the merge failed.
     *
     * @return The current progress of the merge
     */
    public static int getCurrentProgress() {
        return currentProgress;
    }
}
