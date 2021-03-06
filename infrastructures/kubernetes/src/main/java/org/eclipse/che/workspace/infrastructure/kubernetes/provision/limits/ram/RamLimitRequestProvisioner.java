/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.workspace.infrastructure.kubernetes.provision.limits.ram;

import static org.eclipse.che.api.core.model.workspace.config.MachineConfig.MEMORY_LIMIT_ATTRIBUTE;
import static org.eclipse.che.api.core.model.workspace.config.MachineConfig.MEMORY_REQUEST_ATTRIBUTE;
import static org.eclipse.che.workspace.infrastructure.kubernetes.Names.machineName;

import io.fabric8.kubernetes.api.model.Container;
import java.util.Map;
import javax.inject.Inject;
import org.eclipse.che.api.core.model.workspace.runtime.RuntimeIdentity;
import org.eclipse.che.api.workspace.server.spi.InfrastructureException;
import org.eclipse.che.api.workspace.server.spi.environment.InternalMachineConfig;
import org.eclipse.che.api.workspace.server.spi.environment.MemoryAttributeProvisioner;
import org.eclipse.che.commons.annotation.Traced;
import org.eclipse.che.commons.tracing.TracingTags;
import org.eclipse.che.workspace.infrastructure.kubernetes.environment.KubernetesEnvironment;
import org.eclipse.che.workspace.infrastructure.kubernetes.environment.KubernetesEnvironment.PodData;
import org.eclipse.che.workspace.infrastructure.kubernetes.provision.ConfigurationProvisioner;
import org.eclipse.che.workspace.infrastructure.kubernetes.util.Containers;

/**
 * Sets or overrides Kubernetes container RAM limit and request if corresponding attributes are
 * present in machine corresponding to the container.
 *
 * @author Anton Korneta
 */
public class RamLimitRequestProvisioner implements ConfigurationProvisioner {

  private final MemoryAttributeProvisioner memoryAttributeProvisioner;

  @Inject
  public RamLimitRequestProvisioner(MemoryAttributeProvisioner memoryAttributeProvisioner) {
    this.memoryAttributeProvisioner = memoryAttributeProvisioner;
  }

  @Override
  @Traced
  public void provision(KubernetesEnvironment k8sEnv, RuntimeIdentity identity)
      throws InfrastructureException {

    TracingTags.WORKSPACE_ID.set(identity::getWorkspaceId);

    final Map<String, InternalMachineConfig> machines = k8sEnv.getMachines();
    for (PodData pod : k8sEnv.getPodsData().values()) {
      for (Container container : pod.getSpec().getContainers()) {
        InternalMachineConfig machineConfig = machines.get(machineName(pod, container));
        memoryAttributeProvisioner.provision(
            machineConfig, Containers.getRamLimit(container), Containers.getRamRequest(container));
        final Map<String, String> attributes = machineConfig.getAttributes();
        String memoryLimitAttribute = attributes.get(MEMORY_LIMIT_ATTRIBUTE);
        if (memoryLimitAttribute != null) {
          Containers.addRamLimit(container, Long.parseLong(memoryLimitAttribute));
        }
        String memoryRequestAttribute = attributes.get(MEMORY_REQUEST_ATTRIBUTE);
        if (memoryRequestAttribute != null) {
          Containers.addRamRequest(container, Long.parseLong(memoryRequestAttribute));
        }
      }
    }
  }
}
