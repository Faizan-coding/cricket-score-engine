package com.faizan.cricket.service;

import com.faizan.cricket.dto.MatchState;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Pushes score updates to subscribers over Server-Sent Events instead of making clients poll. */
@Service
public class LiveUpdateService {

    private final Map<Long, List<SseEmitter>> subscribers = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long matchId, Supplier<MatchState> snapshot) {
        SseEmitter emitter = new SseEmitter(0L); // no server-side timeout
        List<SseEmitter> list = subscribers.computeIfAbsent(matchId, k -> new CopyOnWriteArrayList<>());
        list.add(emitter);

        Runnable remove = () -> list.remove(emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(e -> remove.run());

        // Register first, then send the snapshot, so no update can fall in the gap.
        // Clients ignore any message whose eventCount is lower than one they already have.
        send(emitter, snapshot.get(), remove);
        return emitter;
    }

    public void publish(MatchState state) {
        List<SseEmitter> list = subscribers.get(state.id());
        if (list == null) return;
        for (SseEmitter emitter : list) {
            send(emitter, state, () -> list.remove(emitter));
        }
    }

    public int subscriberCount(Long matchId) {
        List<SseEmitter> list = subscribers.get(matchId);
        return list == null ? 0 : list.size();
    }

    private void send(SseEmitter emitter, MatchState state, Runnable onFailure) {
        try {
            emitter.send(SseEmitter.event().name("score").id(String.valueOf(state.eventCount())).data(state));
        } catch (IOException | IllegalStateException e) {
            onFailure.run();
            emitter.completeWithError(e);
        }
    }
}
