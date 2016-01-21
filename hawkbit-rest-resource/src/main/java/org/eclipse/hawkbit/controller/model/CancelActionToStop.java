/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.controller.model;

import javax.validation.constraints.NotNull;

import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.rest.resource.model.doc.ApiModelProperties;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * The {@link Action} that has to be stopped by the target.
 *
 *
 *
 */
@ApiModel(ApiModelProperties.CANCEL_ACTION)
public class CancelActionToStop {

    @ApiModelProperty(value = ApiModelProperties.ACTION_ID, required = true)
    @NotNull
    private final String stopId;

    /**
     * Parameterized constructor.
     * 
     * @param stopId
     *            ID of the {@link Action} to be stoppedW
     */
    public CancelActionToStop(final String stopId) {
        super();
        this.stopId = stopId;
    }

    /**
     * @return the stopId
     */
    public String getStopId() {
        return stopId;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "CancelAction [stopId=" + stopId + "]";
    }

}
