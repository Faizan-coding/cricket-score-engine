package com.faizan.cricket.cache;

import com.faizan.cricket.dto.MatchState;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Default cache so the project runs with zero setup. Use the "redis" profile for Redis. */
@Component
@Profile("!redis")
public class InMemoryMatchStateCache implements MatchStateCache {

    private final ConcurrentHashMap<Long, MatchState> map = new ConcurrentHashMap<>();

    @Override
    public Optional<MatchState> get(Long matchId) {
        return Optional.ofNullable(map.get(matchId));
    }

    @Override
    public void put(MatchState state) {
        // Atomic per key: never let an older state overwrite a newer one.
        map.merge(state.id(), state, (cur, next) -> next.eventCount() >= cur.eventCount() ? next : cur);
    }

    @Override
    public void evict(Long matchId) {
        map.remove(matchId);
    }
}
