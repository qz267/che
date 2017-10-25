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

import static org.eclipse.che.workspace.infrastructure.openshift.OpenShiftPvcStrategy.COMMON;
import static org.eclipse.che.workspace.infrastructure.openshift.OpenShiftPvcStrategy.UNIQUE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fabric8.openshift.client.OpenShiftClient;
import org.eclipse.che.api.core.model.workspace.Workspace;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.workspace.infrastructure.openshift.OpenShiftClientFactory;
import org.eclipse.che.workspace.infrastructure.openshift.project.OpenShiftProjectCleaner.DeleteOpenShiftProjectOnWorkspaceRemove;
import org.eclipse.che.workspace.infrastructure.openshift.project.OpenShiftProjectCleaner.DeleteOpenShiftProjectPvcOnWorkspaceRemove;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/** Tests {@link OpenShiftProjectCleaner}. */
@Listeners(MockitoTestNGListener.class)
public class OpenShiftProjectCleanerTest {

  private static final String WORKSPACE_ID = "workspace123";
  private static final String PROJECT_NAME = "che";
  private static final String PVC_NAME = "che-workspace-data";

  @Mock private Workspace workspace;
  @Mock private OpenShiftClientFactory clientFactory;
  @Mock private OpenShiftClient client;
  @Mock private EventService eventService;

  private OpenShiftProjectCleaner openShiftProjectCleaner;

  @BeforeMethod
  public void setUp() {
    openShiftProjectCleaner =
        spy(new OpenShiftProjectCleaner(true, PROJECT_NAME, PVC_NAME, COMMON, clientFactory));
    when(clientFactory.create()).thenReturn(client);
    when(workspace.getId()).thenReturn(WORKSPACE_ID);
  }

  @Test
  public void testDoNotSubscribesCleanerWhenPvcDisabled() {
    openShiftProjectCleaner =
        spy(new OpenShiftProjectCleaner(false, PROJECT_NAME, PVC_NAME, UNIQUE, clientFactory));

    openShiftProjectCleaner.subscribe(eventService);

    verify(eventService, never()).subscribe(any());
  }

  @Test
  public void testSubscribesDeleteOpenShiftProjectOnWorkspaceRemove() throws Exception {
    openShiftProjectCleaner =
        spy(new OpenShiftProjectCleaner(true, null, PVC_NAME, COMMON, clientFactory));

    openShiftProjectCleaner.subscribe(eventService);

    verify(eventService, times(1)).subscribe(any(DeleteOpenShiftProjectOnWorkspaceRemove.class));
  }

  @Test
  public void testSubscribesDeleteOpenShiftProjectPvcOnWorkspaceRemove() throws Exception {
    openShiftProjectCleaner =
        spy(new OpenShiftProjectCleaner(true, PROJECT_NAME, PVC_NAME, UNIQUE, clientFactory));

    openShiftProjectCleaner.subscribe(eventService);

    verify(eventService, times(1)).subscribe(any(DeleteOpenShiftProjectPvcOnWorkspaceRemove.class));
  }

  @Test
  public void testDoNotSubscribesCleanerForOnePerProjectStrategy() throws Exception {
    openShiftProjectCleaner.subscribe(eventService);

    verify(eventService, never()).subscribe(any());
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void throwsIllegalArgumentExceptionWhenUnsupportedStrategySelected() throws Exception {
    openShiftProjectCleaner =
        spy(new OpenShiftProjectCleaner(true, null, null, null, clientFactory));
    doThrow(IllegalArgumentException.class).when(openShiftProjectCleaner).subscribe(any());

    openShiftProjectCleaner.subscribe(eventService);
  }
}
