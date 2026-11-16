package com.faizan.cricket.cache;

import com.faizan.cricket.dto.MatchState;
import java.util.Optional;

/** Read-through / write-after-commit cache of the current match state. */
public interface MatchStateCache {
    Optional<MatchState> get(Long matchId);

    /** Stores the state unless the cache already holds a newer one (higher eventCount). */
    void put(MatchState state);

    void evict(Long matchId);
}
