package com.faizan.cricket.web;

import com.faizan.cricket.dto.*;
import com.faizan.cricket.service.LiveUpdateService;
import com.faizan.cricket.service.MatchService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final MatchService service;
    private final LiveUpdateService live;

    public MatchController(MatchService service, LiveUpdateService live) {
        this.service = service;
        this.live = live;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MatchState create(@Valid @RequestBody CreateMatchRequest request) {
        return service.create(request);
    }

    @PostMapping("/{id}/start")
    public MatchState start(@PathVariable Long id) {
        return service.start(id);
    }

    /** 201 for a new event, 200 when the same eventId was already processed. */
    @PostMapping("/{id}/events")
    public ResponseEntity<SubmitResult> submit(@PathVariable Long id, @Valid @RequestBody BallEventRequest request) {
        SubmitResult result = service.submit(id, request);
        return ResponseEntity.status(result.duplicate() ? HttpStatus.OK : HttpStatus.CREATED).body(result);
    }

    @GetMapping("/{id}")
    public MatchState get(@PathVariable Long id) {
        return service.getState(id);
    }

    @GetMapping("/{id}/events")
    public List<BallEventView> events(@PathVariable Long id) {
        return service.getEvents(id);
    }

    @GetMapping("/{id}/verify")
    public VerifyResult verify(@PathVariable Long id) {
        return service.verify(id);
    }

    @GetMapping(value = "/{id}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable Long id) {
        service.getState(id); // 404 early if the match does not exist
        return live.subscribe(id, () -> service.getState(id));
    }
}
