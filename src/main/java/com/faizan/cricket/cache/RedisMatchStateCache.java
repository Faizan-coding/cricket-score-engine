package com.faizan.cricket.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.faizan.cricket.dto.MatchState;
import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("redis")
public class RedisMatchStateCache implements MatchStateCache {

    private static final Logger log = LoggerFactory.getLogger(RedisMatchStateCache.class);
    private static final Duration TTL = Duration.ofHours(1);

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    public RedisMatchStateCache(StringRedisTemplate redis, ObjectMapper mapper) {
        this.redis = redis;
        this.mapper = mapper;
    }

    private static String key(Long id) {
        return "match:state:" + id;
    }

    @Override
    public Optional<MatchState> get(Long matchId) {
        try {
            String json = redis.opsForValue().get(key(matchId));
            return json == null ? Optional.empty() : Optional.of(mapper.readValue(json, MatchState.class));
        } catch (JsonProcessingException | RuntimeException e) {
            log.warn("Cache read failed for match {}, falling back to DB: {}", matchId, e.toString());
            return Optional.empty();
        }
    }

    @Override
    public void put(MatchState state) {
        try {
            Optional<MatchState> current = get(state.id());
            // Known limitation: check-then-set is not atomic. A Lua script would make it atomic.
            if (current.isPresent() && current.get().eventCount() > state.eventCount()) {
                return;
            }
            redis.opsForValue().set(key(state.id()), mapper.writeValueAsString(state), TTL);
        } catch (JsonProcessingException | RuntimeException e) {
            log.warn("Cache write failed for match {}: {}", state.id(), e.toString());
        }
    }

    @Override
    public void evict(Long matchId) {
        try {
            redis.delete(key(matchId));
        } catch (RuntimeException e) {
            log.warn("Cache evict failed for match {}: {}", matchId, e.toString());
        }
    }
}
