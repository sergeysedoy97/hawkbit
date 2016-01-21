/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.artifact.repository;

import java.io.InputStream;

import org.eclipse.hawkbit.artifact.repository.model.DbArtifact;

import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSFile;

/**
 * A wrapper object for the {@link DbArtifact} object which returns the
 * {@link InputStream} directly from {@link GridFSDBFile#getInputStream()} which
 * retrieves when calling {@link #getFileInputStream()} always a new
 * {@link InputStream} and not the same.
 *
 *
 *
 */
public class GridFsArtifact extends DbArtifact {

    private final GridFSFile dbFile;

    /**
     * @param dbFile
     */
    public GridFsArtifact(final GridFSFile dbFile) {
        this.dbFile = dbFile;
    }

    @Override
    public InputStream getFileInputStream() {
        if (dbFile instanceof GridFSDBFile) {
            return ((GridFSDBFile) dbFile).getInputStream();
        }
        return null;
    }
}
