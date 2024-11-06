package com.metehan.TrendyolCanliKonumTakipServis.gelisme;

import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.TextMessage;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SessionManager {

    private static final Map<String, Set<WebSocketSession>> sessions = new HashMap<>();

    public static synchronized void addSession(WebSocketSession session, String type) {
        sessions.computeIfAbsent(type, k -> new HashSet<>()).add(session);
    }

    public static synchronized void removeSession(WebSocketSession session, String type) {
        Set<WebSocketSession> sessionSet = sessions.get(type);
        if (sessionSet != null) {
            sessionSet.remove(session);
            if (sessionSet.isEmpty()) {
                sessions.remove(type);
            }
        }
    }

    public static synchronized void broadcastMessage(String message, String targetType) {
        Set<WebSocketSession> targetSessions = sessions.get(targetType);
        if (targetSessions != null) {
            for (WebSocketSession session : targetSessions) {
                try {
                    session.sendMessage(new TextMessage(message));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

