package com.faizan.cricket.sim;

import com.faizan.cricket.domain.EventType;
import com.faizan.cricket.domain.InvalidEventException;
import com.faizan.cricket.dto.*;
import com.faizan.cricket.service.MatchService;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Plays a full match ball by ball so the project can be demoed and load tested
 * without any paid live-data API. Enable with --spring.profiles.active=simulate
 */
@Component
@Profile("simulate")
public class MatchSimulator implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(MatchSimulator.class);

    private final MatchService service;
    private final long delayMs;
    private final int overs;

    public MatchSimulator(MatchService service,
                          @Value("${simulator.delay-ms:800}") long delayMs,
                          @Value("${simulator.overs:5}") int overs) {
        this.service = service;
        this.delayMs = delayMs;
        this.overs = overs;
    }

    @Override
    public void run(String... args) {
        MatchState match = service.create(new CreateMatchRequest("India", "Australia", overs));
        service.start(match.id());
        log.info("Simulating match {}. Open http://localhost:8080/?match={}", match.id(), match.id());

        Thread t = new Thread(() -> play(match.id()), "match-simulator");
        t.setDaemon(true);
        t.start();
    }

    private void play(Long id) {
        String[] batters = {"Rohit", "Gill", "Kohli", "Pant", "Hardik", "Jadeja", "Axar", "Bumrah", "Shami", "Siraj", "Kuldeep"};
        int striker = 0;
        int innings = 1;
        try {
            while (true) {
                Ball b = nextBall();
                MatchState s = service.submit(id, new BallEventRequest(
                        UUID.randomUUID().toString(), b.type, b.runs, batters[Math.min(striker, 10)], "Starc")).state();
                if (b.type == EventType.WICKET) striker++;
                if (s.innings() != innings) {
                    innings = s.innings();
                    striker = 0;
                    log.info("Innings 1 over: {} {}/{}. {} need {} to win.",
                            s.firstInnings().team(), s.firstInnings().runs(), s.firstInnings().wickets(),
                            s.battingTeam(), s.target());
                } else {
                    log.info("Inn {} {} runs={} -> {}/{} ({})", s.innings(), b.type, b.runs, s.runs(), s.wickets(), s.overs());
                }
                if (s.status().name().equals("COMPLETED")) {
                    log.info("RESULT: {}", s.result());
                    return;
                }
                Thread.sleep(delayMs);
            }
        } catch (InvalidEventException e) {
            log.info("Simulation stopped: {}", e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private record Ball(EventType type, int runs) {}

    private static Ball nextBall() {
        int r = ThreadLocalRandom.current().nextInt(100);
        if (r < 30) return new Ball(EventType.RUNS, 0);
        if (r < 58) return new Ball(EventType.RUNS, 1);
        if (r < 68) return new Ball(EventType.RUNS, 2);
        if (r < 70) return new Ball(EventType.RUNS, 3);
        if (r < 82) return new Ball(EventType.RUNS, 4);
        if (r < 87) return new Ball(EventType.RUNS, 6);
        if (r < 92) return new Ball(EventType.WICKET, 0);
        if (r < 96) return new Ball(EventType.WIDE, 0);
        if (r < 98) return new Ball(EventType.NO_BALL, 0);
        if (r < 99) return new Ball(EventType.BYE, 1);
        return new Ball(EventType.LEG_BYE, 1);
    }
}
