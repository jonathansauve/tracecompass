/*******************************************************************************
 * Copyright (c) 2015 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Francis Giraldeau - Initial API and implementation
 *   Geneviève Bastien - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.graph.core.tests.graph;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Various utilities for unit testing: - find traces under directory - make
 * experiments - synchronize traces
 *
 * The aim of this class is to avoid code duplication in the test themselves
 * when working with actual traces.
 *
 * @author Francis Giraldeau <francis.giraldeau@gmail.com>
 */

public class Utils {

    /**
     * @author francis
     *
     */
    public static enum Traceset {
        /**
         * Trace of a remote procedure call
         */
        TRACE_RPC("wk-rpc-100ms-k");
        private final String fName;
        Traceset(String name) {
            fName = name;
        }
        /**
         * @return trace name
         */
        public String getName() {
            return fName;
        }
    }

    /**
     * Find firectories that looks like a CTF trace. It returns the directories
     * containing a metadata file.
     *
     * @author Francis Giraldeau <francis.giraldeau@gmail.com>
     *
     */
    private static class CtfTraceFinder extends SimpleFileVisitor<Path> {

        private static final String GLOB_METADATA = "glob:metadata";
        private final PathMatcher fMatcher;
        private final List<Path> fResults;

        CtfTraceFinder() {
            fMatcher = FileSystems.getDefault().getPathMatcher(GLOB_METADATA);
            fResults = new LinkedList<>();
        }

        @Override
        public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
            Path name = path.getFileName();
            if (name != null && fMatcher.matches(name)) {
                fResults.add(path.getParent());
            }
            return FileVisitResult.CONTINUE;
        }

        /**
         * Return the list of directories containing a metadata file.
         *
         * @return results
         */
        public List<Path> getResults() {
            return fResults;
        }

    }

    /**
     * Compare java.nio.Path based on creation time
     *
     * @author Francis Giraldeau <francis.giraldeau@gmail.com>
     *
     */
    public static class PathCreationTimeComparator implements Comparator<Path> {
        @Override
        public int compare(Path p0, Path p1) {
            try {
                FileTime t0 = Files.readAttributes(p0, BasicFileAttributes.class).creationTime();
                FileTime t1 = Files.readAttributes(p1, BasicFileAttributes.class).creationTime();
                return t0.compareTo(t1);
            } catch (IOException e) {
                /*
                 * DISCUSS: handle exception in a comparator for use outside
                 * unit testing.
                 */
                throw new RuntimeException(e.getMessage());
            }
        }
    }

    /**
     * Utility method to walk directories and find CTF traces
     *
     * @param base
     *            base directory
     * @return list of trace paths
     */
    public static List<Path> findCtfTrace(Path base) {
        CtfTraceFinder finder = new CtfTraceFinder();
        try {
            Files.walkFileTree(base, finder);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }

        return finder.getResults();
    }

    /**
     * Find all trace paths below a given directory
     *
     * @param dir
     *            parent directory
     * @return list of paths
     */
    public static List<Path> getTracePathsByCreationTime(Path dir) {
        List<Path> paths = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path item : stream) {
                BasicFileAttributes attrs = Files.readAttributes(item, BasicFileAttributes.class);
                if (attrs.isDirectory()) {
                    paths.add(item);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }

        return paths;
    }

    /**
     * Make an experiment of CTF traces
     *
     * @param paths
     *            directory of traces
     * @param traceKlass
     *            the trace type
     * @param evKlass
     *            the event type
     * @return experiment
     */
    public static ITmfTrace makeExperimentGeneric(List<Path> paths, Class<? extends ITmfTrace> traceKlass, Class<? extends ITmfEvent> evKlass) {
        try {
            ITmfTrace main = traceKlass.newInstance();
            for (Path p : paths) {
                ITmfTrace child = traceKlass.newInstance();
                child.initTrace(null, p.toString(), evKlass);
                main.addChild(child);
            }
            return main;
        } catch (InstantiationException | IllegalAccessException | TmfTraceException e) {
            throw new RuntimeException(e.getMessage());
        }

    }

}
