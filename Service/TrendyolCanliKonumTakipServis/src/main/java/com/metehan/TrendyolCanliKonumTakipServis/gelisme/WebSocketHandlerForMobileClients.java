package com.metehan.TrendyolCanliKonumTakipServis.gelisme;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

public class WebSocketHandlerForMobileClients extends TextWebSocketHandler {

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        SessionManager.addSession(session, "mobile");
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            String clientMessage = message.getPayload();
            System.out.println("Mobile Client - Received message: " + clientMessage);
            SessionManager.broadcastMessage(clientMessage, "web");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        SessionManager.removeSession(session, "mobile");
    }
}

