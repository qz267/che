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
package org.eclipse.che.wsagent.server;

import static com.google.common.collect.Sets.newConcurrentHashSet;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.eclipse.che.api.workspace.shared.Constants.WS_AGENT_TRACK_CONNECTION_HEARTBEAT;
import static org.eclipse.che.api.workspace.shared.Constants.WS_AGENT_TRACK_CONNECTION_PERIOD;
import static org.eclipse.che.api.workspace.shared.Constants.WS_AGENT_TRACK_CONNECTION_SUBSCRIBE;
import static org.eclipse.che.api.workspace.shared.Constants.WS_AGENT_TRACK_CONNECTION_UNSUBSCRIBE;

import java.util.Set;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.che.api.core.jsonrpc.commons.RequestHandlerConfigurator;
import org.eclipse.che.api.core.jsonrpc.commons.RequestTransmitter;
import org.eclipse.che.commons.schedule.ScheduleRate;

/**
 * Sends requests to interested clients to confirm that connection isn't lost.
 *
 * @author Roman Nikitenko
 */
@Singleton
public class WsAgentWebSocketConnectionKeeper {
  private final Set<String> endpointIds = newConcurrentHashSet();
  private RequestTransmitter transmitter;
  private RequestHandlerConfigurator configurator;

  @Inject
  public WsAgentWebSocketConnectionKeeper(
      RequestTransmitter transmitter, RequestHandlerConfigurator configurator) {
    this.transmitter = transmitter;
    this.configurator = configurator;
  }

  @PostConstruct
  public void initialize() {
    configurator
        .newConfiguration()
        .methodName(WS_AGENT_TRACK_CONNECTION_SUBSCRIBE)
        .noParams()
        .noResult()
        .withConsumer(endpointIds::add);

    configurator
        .newConfiguration()
        .methodName(WS_AGENT_TRACK_CONNECTION_UNSUBSCRIBE)
        .noParams()
        .noResult()
        .withConsumer(endpointIds::remove);
  }

  @ScheduleRate(period = WS_AGENT_TRACK_CONNECTION_PERIOD, unit = MILLISECONDS)
  public void sendRequest() {
    endpointIds.forEach(
        endpointId ->
            transmitter
                .newRequest()
                .endpointId(endpointId)
                .methodName(WS_AGENT_TRACK_CONNECTION_HEARTBEAT)
                .noParams()
                .sendAndSkipResult());
  }
}
