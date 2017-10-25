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
package org.eclipse.che.workspace.infrastructure.openshift.provision.volume;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.eclipse.che.workspace.infrastructure.openshift.OpenShiftPvcStrategy.COMMON;
import static org.eclipse.che.workspace.infrastructure.openshift.OpenShiftPvcStrategy.UNIQUE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMount;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.che.api.core.model.workspace.runtime.RuntimeIdentity;
import org.eclipse.che.api.workspace.server.model.impl.ServerConfigImpl;
import org.eclipse.che.api.workspace.server.spi.InternalEnvironment;
import org.eclipse.che.api.workspace.server.spi.InternalMachineConfig;
import org.eclipse.che.api.workspace.shared.Constants;
import org.eclipse.che.workspace.infrastructure.openshift.environment.OpenShiftEnvironment;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/**
 * Tests {@link PersistentVolumeClaimProvisioner}.
 *
 * @author Anton Korneta
 */
@Listeners(MockitoTestNGListener.class)
public class PersistentVolumeClaimProvisionerTest {

  private static final String PVC_NAME = "claim-che-workspace";
  private static final String PVC_SIZE = "10Gi";
  private static final String PVC_ACCESS_MODE = "ReadWriteOnce";
  private static final String WORKSPACE_ID = "workspace132";
  private static final String WORKSPACE_PROJECT_PATH = "/projects";
  private static final String POD_NAME = "test";
  private static final String CONTAINER_NAME = "app";
  private static final String MACHINE_NAME = POD_NAME + '/' + CONTAINER_NAME;

  @Mock private InternalEnvironment environment;
  @Mock private OpenShiftEnvironment osEnv;
  @Mock private RuntimeIdentity runtimeIdentity;

  private PersistentVolumeClaimProvisioner provisioner;

  @BeforeMethod
  public void setup() {
    when(runtimeIdentity.getWorkspaceId()).thenReturn(WORKSPACE_ID);
    provisioner =
        new PersistentVolumeClaimProvisioner(
            false, PVC_NAME, PVC_SIZE, PVC_ACCESS_MODE, WORKSPACE_PROJECT_PATH, COMMON);
  }

  @Test
  public void doNothingWhenPvcDisabled() throws Exception {
    provisioner.provision(environment, osEnv, runtimeIdentity);

    verify(runtimeIdentity, never()).getWorkspaceId();
    verify(environment, never()).getMachines();
  }

  @Test
  public void provisionOnePvcForAllWorkspaces() throws Exception {
    provisioner =
        new PersistentVolumeClaimProvisioner(
            true, PVC_NAME, PVC_SIZE, PVC_ACCESS_MODE, WORKSPACE_PROJECT_PATH, COMMON);
    final Map<String, PersistentVolumeClaim> pvcs = new HashMap<>();
    final List<Volume> volumes = new ArrayList<>();
    final List<VolumeMount> volumeMounts = new ArrayList<>();
    mockOpenShiftEnvironment(pvcs, volumes, volumeMounts);

    provisioner.provision(environment, osEnv, runtimeIdentity);

    verify(runtimeIdentity, times(1)).getWorkspaceId();
    verify(environment, times(1)).getMachines();
    assertFalse(pvcs.isEmpty());
    assertTrue(pvcs.containsKey(PVC_NAME));
    assertFalse(volumes.isEmpty());
    assertFalse(volumeMounts.isEmpty());
  }

  @Test
  public void provisionOnePvcForOneWorkspace() throws Exception {
    provisioner =
        new PersistentVolumeClaimProvisioner(
            true, PVC_NAME, PVC_SIZE, PVC_ACCESS_MODE, WORKSPACE_PROJECT_PATH, UNIQUE);
    final Map<String, PersistentVolumeClaim> pvcs = new HashMap<>();
    final List<Volume> volumes = new ArrayList<>();
    final List<VolumeMount> volumeMounts = new ArrayList<>();
    mockOpenShiftEnvironment(pvcs, volumes, volumeMounts);

    provisioner.provision(environment, osEnv, runtimeIdentity);

    verify(runtimeIdentity, times(1)).getWorkspaceId();
    verify(environment, times(1)).getMachines();
    assertFalse(pvcs.isEmpty());
    assertTrue(pvcs.containsKey(PVC_NAME + '-' + WORKSPACE_ID));
    assertFalse(volumes.isEmpty());
    assertFalse(volumeMounts.isEmpty());
  }

  private void mockOpenShiftEnvironment(
      Map<String, PersistentVolumeClaim> pvcs,
      List<Volume> volumes,
      List<VolumeMount> volumeMounts) {
    when(osEnv.getPersistentVolumeClaims()).thenReturn(pvcs);
    final InternalMachineConfig devMachine = mock(InternalMachineConfig.class);
    when(environment.getMachines()).thenReturn(ImmutableMap.of(MACHINE_NAME, devMachine));
    when(devMachine.getServers())
        .thenReturn(singletonMap(Constants.SERVER_WS_AGENT_HTTP_REFERENCE, new ServerConfigImpl()));
    final Pod pod = mock(Pod.class);
    final PodSpec podSpec = mock(PodSpec.class);
    final ObjectMeta podMeta = mock(ObjectMeta.class);
    final Container container = mock(Container.class);
    when(pod.getSpec()).thenReturn(podSpec);
    when(pod.getMetadata()).thenReturn(podMeta);
    when(podMeta.getName()).thenReturn(POD_NAME);
    when(podSpec.getContainers()).thenReturn(singletonList(container));
    when(podSpec.getVolumes()).thenReturn(volumes);
    when(container.getName()).thenReturn(CONTAINER_NAME);
    when(container.getVolumeMounts()).thenReturn(volumeMounts);
    when(osEnv.getPods()).thenReturn(ImmutableMap.of(POD_NAME, pod));
  }
}
