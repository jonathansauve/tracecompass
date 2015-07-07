/*******************************************************************************
 * Copyright (c) 2015 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Francis Giraldeau- Initial API and implementation
 *******************************************************************************/
package org.eclipse.tracecompass.analysis.graph.core.base;

/**
 * Runtime exception when computing the critical path.
 *
 * @author Francis Giraldeau <francis.giraldeau@gmail.com>
 */
public class TmfGraphException extends RuntimeException {

    /**
     * Version ID for serialization
     */
    private static final long serialVersionUID = 3119156523726241685L;

    /**
     * Default constructor with no message.
     */
    public TmfGraphException() {
        super();
    }

    /**
     * Constructor with an attached message.
     *
     * @param message
     *            The message attached to this exception
     */
    public TmfGraphException(String message) {
        super(message);
    }

    /**
     * Re-throw an exception into this type.
     *
     * @param e
     *            The previous Throwable we caught
     */
    public TmfGraphException(Throwable e) {
        super(e);
    }

    /**
     * Constructor with an attached message and re-throw an exception into this
     * type.
     *
     * @param message
     *            The message attached to this exception
     * @param exception
     *            The previous Exception caught
     */
    public TmfGraphException(String message, Throwable exception) {
        super(message, exception);
    }


}
