/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import java.util.List;

import org.eclipse.hawkbit.repository.model.LocalArtifact;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link LocalArtifact} repository.
 *
 *
 *
 */
@Transactional(readOnly = true)
public interface LocalArtifactRepository extends BaseEntityRepository<LocalArtifact, Long> {
    /**
     * Searches for a {@link LocalArtifact} based on given gridFsFileName.
     *
     * @param gridFsFileName
     *            to search
     * @return list of {@link LocalArtifact}s.
     */
    List<LocalArtifact> findByGridFsFileName(String gridFsFileName);

    /**
     * Searches for a {@link LocalArtifact} based on given gridFsFileName.
     *
     * @param gridFsFileName
     *            to search
     * @return {@link LocalArtifact} the first in the result list
     */
    LocalArtifact findFirstByGridFsFileName(String gridFsFileName);

    /**
     * Searches for a {@link LocalArtifact} based user provided filename at
     * upload.
     *
     * @param filename
     *            to search
     * @return list of {@link LocalArtifact}.
     */
    List<LocalArtifact> findByFilename(String filename);

    /**
     * Searches for local artifact for a base software module.
     *
     * @param pageReq
     *            Pageable
     * @param swId
     *            software module id
     *
     * @return Page<LocalArtifact>
     */
    Page<LocalArtifact> findBySoftwareModuleId(Pageable pageReq, final Long swId);

    /**
     * Searches for a {@link LocalArtifact} based user provided filename at
     * upload and selected software module id.
     *
     * @param filename
     *            to search
     * @param softwareModuleId
     *            selected software module id
     * @return list of {@link LocalArtifact}.
     */
    List<LocalArtifact> findByFilenameAndSoftwareModuleId(final String filename, final Long softwareModuleId);

}
