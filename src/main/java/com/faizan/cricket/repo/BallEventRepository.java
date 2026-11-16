package com.faizan.cricket.repo;

import com.faizan.cricket.domain.BallEvent;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BallEventRepository extends JpaRepository<BallEvent, Long> {
    Optional<BallEvent> findByMatchIdAndEventId(Long matchId, String eventId);

    List<BallEvent> findByMatchIdOrderBySequenceNoAsc(Long matchId);
}
