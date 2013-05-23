package com.hoccer.talk.filecache.control;

import better.jsonrpc.server.JsonRpcServer;
import better.jsonrpc.websocket.JsonRpcWsConnection;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoccer.talk.filecache.rpc.ICacheControl;
import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketServlet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;

@WebServlet(urlPatterns = "/control")
public class ControlServlet extends WebSocketServlet {

    ObjectMapper mJsonMapper;

    JsonRpcServer mRpcServer;

    @Override
    public void init() throws ServletException {
        super.init();

        mJsonMapper = new ObjectMapper();

        mRpcServer = new JsonRpcServer(ICacheControl.class);
    }

    @Override
    public WebSocket doWebSocketConnect(HttpServletRequest request, String protocol) {
        if(protocol.equals("com.hoccer.talk.filecache.control.v1")) {
            JsonRpcWsConnection connection = new JsonRpcWsConnection(mJsonMapper);
            ControlConnection handler = new ControlConnection(this, connection);
            connection.bindServer(mRpcServer, handler);
            return connection;
        }
        return null;
    }
}