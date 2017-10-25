/*
 * Copyright (c) 2012-2017 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.workspace.infrastructure.openshift;

import static java.lang.String.format;

import com.google.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

/**
 * Provides equivalent of {@link OpenShiftPvcStrategy} for configured value.
 *
 * @author Anton Korneta
 */
public class OpenShiftPvcStrategyProvider implements Provider<OpenShiftPvcStrategy> {

  @Inject
  @Named("che.infra.openshift.pvc.strategy")
  private String strategy;

  @Override
  public OpenShiftPvcStrategy get() {
    for (OpenShiftPvcStrategy value : OpenShiftPvcStrategy.values()) {
      if (strategy.equalsIgnoreCase(value.getName())) {
        return value;
      }
    }
    throw new IllegalArgumentException(
        format("Unsupported PVC strategy '%s' configured", strategy));
  }
}
