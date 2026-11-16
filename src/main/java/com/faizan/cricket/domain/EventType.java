package com.faizan.cricket.domain;

public enum EventType {
    RUNS,      // runs off the bat, legal delivery (0-6)
    WICKET,    // wicket on a legal delivery, optional runs completed (0-3)
    WIDE,      // wide: 1 penalty + 0-4 extra runs, NOT a legal delivery
    NO_BALL,   // no-ball: 1 penalty + 0-6 runs off the bat, NOT a legal delivery
    BYE,       // byes (1-4), legal delivery
    LEG_BYE    // leg byes (1-4), legal delivery
}
