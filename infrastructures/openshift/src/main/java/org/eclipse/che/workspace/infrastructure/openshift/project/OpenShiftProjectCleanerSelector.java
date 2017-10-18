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

import static java.lang.String.format;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.openshift.client.OpenShiftClient;
import java.util.List;
import javax.inject.Named;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.core.notification.EventSubscriber;
import org.eclipse.che.api.workspace.shared.event.WorkspaceRemovedEvent;
import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.workspace.infrastructure.openshift.OpenShiftClientFactory;

/**
 * Picks the OpenShift project data cleaner on {@code WorkspaceRemovedEvent} in accordance with
 * configuration.
 *
 * @author Sergii Leshchenko
 * @author Anton Korneta
 */
@Singleton
public class OpenShiftProjectCleanerSelector {

  private final boolean pvcEnabled;
  private final String projectName;
  private final String pvcStrategy;
  private final String pvcName;
  private final OpenShiftClientFactory clientFactory;

  @Inject
  public OpenShiftProjectCleanerSelector(
      @Named("che.infra.openshift.pvc.enabled") boolean pvcEnabled,
      @Nullable @Named("che.infra.openshift.project") String projectName,
      @Named("che.infra.openshift.pvc.strategy") String pvcStrategy,
      @Named("che.infra.openshift.pvc.name") String pvcName,
      OpenShiftClientFactory clientFactory) {
    this.pvcEnabled = pvcEnabled;
    this.clientFactory = clientFactory;
    this.pvcStrategy = pvcStrategy;
    this.projectName = projectName;
    this.pvcName = pvcName;
  }

  @Inject
  public void subscribe(EventService eventService) {
    if (!pvcEnabled) {
      return;
    }
    // removes OpenShift project when no project configured for all che workspaces
    if (projectName == null) {
      eventService.subscribe(new DeleteOpenShiftProjectOnWorkspaceRemove());
      return;
    }
    switch (pvcStrategy) {
      case "onePerWorkspace":
        eventService.subscribe(new DeleteOpenShiftProjectPvcOnWorkspaceRemove(projectName));
        break;
      case "onePerProject":
        // TODO implement https://github.com/eclipse/che/issues/6767
        break;
      default:
        throw new IllegalArgumentException(
            format("Unsupported PVC strategy '%s' configured", pvcStrategy));
    }
  }

  /** Removes OpenShift project on {@code WorkspaceRemovedEvent}. */
  @VisibleForTesting
  class DeleteOpenShiftProjectOnWorkspaceRemove implements EventSubscriber<WorkspaceRemovedEvent> {

    @Override
    public void onEvent(WorkspaceRemovedEvent event) {
      doRemoveProject(event.getWorkspace().getId());
    }

    @VisibleForTesting
    void doRemoveProject(String projectName) {
      try (OpenShiftClient client = clientFactory.create()) {
        client.projects().withName(projectName).delete();
      }
    }
  }

  /** Removes OpenShift Project PVC on {@code WorkspaceRemovedEvent}. */
  @VisibleForTesting
  class DeleteOpenShiftProjectPvcOnWorkspaceRemove
      implements EventSubscriber<WorkspaceRemovedEvent> {

    private final String projectName;

    DeleteOpenShiftProjectPvcOnWorkspaceRemove(String projectName) {
      this.projectName = projectName;
    }

    @Override
    public void onEvent(WorkspaceRemovedEvent event) {
      doRemoveProject(projectName, event.getWorkspace().getId());
    }

    @VisibleForTesting
    void doRemoveProject(String projectName, String workspaceId) {
      try (OpenShiftClient client = clientFactory.create()) {
        final String pvcUniqueName = pvcName + '-' + workspaceId;
        final List<PersistentVolumeClaim> pvcs =
            client.persistentVolumeClaims().inNamespace(projectName).list().getItems();
        for (PersistentVolumeClaim pvc : pvcs) {
          if (pvc.getMetadata().getName().equals(pvcUniqueName)) {
            client.persistentVolumeClaims().inNamespace(projectName).delete(pvc);
            return;
          }
        }
      }
    }
  }
}
