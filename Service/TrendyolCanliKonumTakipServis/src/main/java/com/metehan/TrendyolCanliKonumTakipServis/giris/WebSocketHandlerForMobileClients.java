package com.metehan.TrendyolCanliKonumTakipServis.giris;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

public class WebSocketHandlerForMobileClients extends TextWebSocketHandler {

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            String clientMessage = message.getPayload();
            System.out.println("Mobile Client - Received message: " + clientMessage);
            session.sendMessage(new TextMessage("Echo - Mobile Client Server received: " + clientMessage));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
