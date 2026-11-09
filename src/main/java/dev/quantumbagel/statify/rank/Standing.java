package dev.quantumbagel.statify.rank;

/** One row of a leaderboard. Tied entries share a place, and the next place skips ahead (1, 2, 2, 4). */
public record Standing<K>(int place, boolean tied, K key, double value) {
}
