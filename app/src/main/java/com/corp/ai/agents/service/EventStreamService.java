package com.corp.ai.agents.service;

import com.corp.ai.agents.model.AgentThoughtEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class EventStreamService {

    private static final Logger log = LoggerFactory.getLogger(EventStreamService.class);
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter registerClient() {
        SseEmitter emitter = new SseEmitter(180_000L); // 3 minutos de timeout
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> {
            emitters.remove(emitter);
            emitter.complete();
        });
        emitter.onError((ex) -> {
            emitters.remove(emitter);
            emitter.complete();
        });

        // Evento de boas-vindas
        try {
            emitter.send(SseEmitter.event()
                    .name("CONNECTED")
                    .data(new AgentThoughtEvent("init", "SYSTEM", "CONNECTED", "Conexão SSE estabelecida com sucesso.", null)));
        } catch (IOException e) {
            emitters.remove(emitter);
        }

        return emitter;
    }

    public void emitThought(AgentThoughtEvent event) {
        log.info("[SSE Emit] [{}] {}: {}", event.getAgentName(), event.getEventType(), event.getContent());
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("AGENT_THOUGHT")
                        .data(event));
            } catch (Exception e) {
                emitters.remove(emitter);
            }
        }
    }
}
