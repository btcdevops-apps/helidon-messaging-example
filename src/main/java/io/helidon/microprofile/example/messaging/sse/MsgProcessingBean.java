/*
 * Copyright (c)  2020 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.helidon.microprofile.example.messaging.sse;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseBroadcaster;
import javax.ws.rs.sse.SseEventSink;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.eclipse.microprofile.reactive.streams.operators.ProcessorBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.reactivestreams.Publisher;

@ApplicationScoped
public class MsgProcessingBean {
    private final EmittingPublisher<String> emitter = EmittingPublisher.create();
    private SseBroadcaster sseBroadcaster;

    @Outgoing("multiplyVariants")
    public Publisher<String> preparePublisher() {
        // Create new publisher for emitting to by this::process
        return ReactiveStreams
                .fromPublisher(emitter)
                .buildRs();
    }

    @Incoming("multiplyVariants")
    @Outgoing("wrapSseEvent")
    public ProcessorBuilder<String, String> multiply() {
        // Multiply to 3 variants of same message
        return ReactiveStreams.<String>builder()
                .flatMap(o ->
                        ReactiveStreams.of(
                                // upper case variant
                                o.toUpperCase(),
                                // repeat twice variant
                                o.repeat(2),
                                // reverse chars 'tnairav'
                                new StringBuilder(o).reverse().toString())
                );
    }

    @Incoming("wrapSseEvent")
    @Outgoing("broadcast")
    public OutboundSseEvent wrapSseEvent(String msg) {
        // Map every message to sse event
        return new OutboundEvent.Builder().data(msg).build();
    }

    @Incoming("broadcast")
    public void broadcast(OutboundSseEvent sseEvent) {
        // Broadcast to all sse sinks
        this.sseBroadcaster.broadcast(sseEvent);
    }

    public void addSink(final SseEventSink eventSink, final Sse sse) {
        if (this.sseBroadcaster == null) {
            this.sseBroadcaster = sse.newBroadcaster();
        }
        this.sseBroadcaster.register(eventSink);
    }

    public void process(final String msg) {
        emitter.emit(msg);
    }
}
