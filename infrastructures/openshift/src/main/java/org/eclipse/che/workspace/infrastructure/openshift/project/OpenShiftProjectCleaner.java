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
import org.eclipse.che.workspace.infrastructure.openshift.OpenShiftPvcStrategy;

/**
 * Clean the workspace related OpenShift resources after {@code WorkspaceRemovedEvent}.
 *
 * <p>Note that depending on a configuration different types of cleaners may be chosen. In case of
 * configuration when new OpenShift project created for each workspace, the whole project will be
 * removed, after workspace removal.
 *
 * @author Sergii Leshchenko
 * @author Anton Korneta
 */
@Singleton
public class OpenShiftProjectCleaner {

  private final boolean pvcEnabled;
  private final String projectName;
  private final String pvcName;
  private final OpenShiftPvcStrategy pvcStrategy;
  private final OpenShiftClientFactory clientFactory;

  @Inject
  public OpenShiftProjectCleaner(
      @Named("che.infra.openshift.pvc.enabled") boolean pvcEnabled,
      @Nullable @Named("che.infra.openshift.project") String projectName,
      @Named("che.infra.openshift.pvc.name") String pvcName,
      OpenShiftPvcStrategy pvcStrategy,
      OpenShiftClientFactory clientFactory) {
    this.pvcEnabled = pvcEnabled;
    this.projectName = projectName;
    this.pvcName = pvcName;
    this.pvcStrategy = pvcStrategy;
    this.clientFactory = clientFactory;
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
      case UNIQUE:
        eventService.subscribe(new DeleteOpenShiftProjectPvcOnWorkspaceRemove(projectName));
        break;
      case COMMON:
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
