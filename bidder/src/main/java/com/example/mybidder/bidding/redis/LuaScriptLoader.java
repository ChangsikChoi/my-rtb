package com.example.mybidder.bidding.redis;

import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

public class LuaScriptLoader {
    public static String loadScript(String scriptName) {
        try {
            ClassPathResource resource = new ClassPathResource("lua/" + scriptName);
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load Lua Script: " + scriptName, e);
        }
    }
}
