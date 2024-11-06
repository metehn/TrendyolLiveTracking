package com.metehan.TrendyolCanliKonumTakipServis.gelisme;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new WebSocketHandlerForWebClients(), "/ws/web").setAllowedOrigins("*");
        registry.addHandler(new WebSocketHandlerForMobileClients(), "/ws/mobile").setAllowedOrigins("*");
    }
}
