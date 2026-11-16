package com.faizan.cricket.repo;

import com.faizan.cricket.domain.Match;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchRepository extends JpaRepository<Match, Long> {
}
