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
package org.eclipse.che.workspace.infrastructure.openshift.project;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fabric8.openshift.client.OpenShiftClient;
import org.eclipse.che.api.core.model.workspace.Workspace;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.workspace.infrastructure.openshift.OpenShiftClientFactory;
import org.eclipse.che.workspace.infrastructure.openshift.project.OpenShiftProjectCleanerSelector.DeleteOpenShiftProjectOnWorkspaceRemove;
import org.eclipse.che.workspace.infrastructure.openshift.project.OpenShiftProjectCleanerSelector.DeleteOpenShiftProjectPvcOnWorkspaceRemove;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/**
 * Tests {@link OpenShiftProjectCleanerSelector}.
 */
@Listeners(MockitoTestNGListener.class)
public class OpenShiftProjectCleanerSelectorTest {

  private static final String WORKSPACE_ID = "workspace123";
  private static final String PROJECT_NAME = "che";
  private static final String PVC_NAME = "che-workspace-data";
  private static final String ONE_PVC_PER_WORKSPACE_STRATEGY = "onePerWorkspace";
  private static final String ONE_PVC_PER_PROJECT_STRATEGY = "onePerProject";

  @Mock
  private Workspace workspace;
  @Mock
  private OpenShiftClientFactory clientFactory;
  @Mock
  private OpenShiftClient openShiftClient;
  @Mock
  private EventService eventService;
  @Mock
  private DeleteOpenShiftProjectOnWorkspaceRemove removeOSProjectOnWsRemoveSubscriber;
  @Mock
  private DeleteOpenShiftProjectPvcOnWorkspaceRemove removeOSProjectPVCOnWsRemoveSubscriber;

  private OpenShiftProjectCleanerSelector openShiftProjectCleanerSelector;

  @BeforeMethod
  public void setUp() {
    openShiftProjectCleanerSelector =
        spy(
            new OpenShiftProjectCleanerSelector(
                true, PROJECT_NAME, ONE_PVC_PER_PROJECT_STRATEGY, PVC_NAME, clientFactory));

    when(workspace.getId()).thenReturn(WORKSPACE_ID);
  }

  @Test
  public void testDoNotSubscribesCleanerWhenPvcDisabled() {
    openShiftProjectCleanerSelector =
        spy(
            new OpenShiftProjectCleanerSelector(
                false, PROJECT_NAME, ONE_PVC_PER_PROJECT_STRATEGY, PVC_NAME, clientFactory));

    openShiftProjectCleanerSelector.subscribe(eventService);

    verify(eventService, never()).subscribe(any());
  }

  @Test
  public void testSubscribesDeleteOpenShiftProjectOnWorkspaceRemove() throws Exception {
    openShiftProjectCleanerSelector =
        spy(
            new OpenShiftProjectCleanerSelector(
                true, null, ONE_PVC_PER_PROJECT_STRATEGY, PVC_NAME, clientFactory));

    openShiftProjectCleanerSelector.subscribe(eventService);

    verify(eventService, times(1)).subscribe(any(DeleteOpenShiftProjectOnWorkspaceRemove.class));
  }

  @Test
  public void testSubscribesDeleteOpenShiftProjectPvcOnWorkspaceRemove() throws Exception {
    openShiftProjectCleanerSelector =
        spy(
            new OpenShiftProjectCleanerSelector(
                true, PROJECT_NAME, ONE_PVC_PER_WORKSPACE_STRATEGY, PVC_NAME, clientFactory));

    openShiftProjectCleanerSelector.subscribe(eventService);

    verify(eventService, times(1)).subscribe(any(DeleteOpenShiftProjectPvcOnWorkspaceRemove.class));
  }

  @Test
  public void testDoNotSubscribesCleanerForOnePerProjectStrategy() throws Exception {
    openShiftProjectCleanerSelector.subscribe(eventService);

    verify(eventService, never()).subscribe(any());
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void throwsIllegalArgumentExceptionWhenUnsupportedStrategySelected() throws Exception {
    openShiftProjectCleanerSelector =
        spy(
            new OpenShiftProjectCleanerSelector(
                true, PROJECT_NAME, "unsupported", PVC_NAME, clientFactory));

    openShiftProjectCleanerSelector.subscribe(eventService);

    verify(eventService, never()).subscribe(any());
  }
}
