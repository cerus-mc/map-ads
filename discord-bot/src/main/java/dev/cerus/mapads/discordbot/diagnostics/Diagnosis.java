package dev.cerus.mapads.discordbot.diagnostics;

public record Diagnosis(boolean success, String message, Throwable error) {

}
