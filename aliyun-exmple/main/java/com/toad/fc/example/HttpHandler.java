package com.toad.fc.example;

import com.aliyun.fc.runtime.Context;
import com.aliyun.fc.runtime.HttpRequestHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class HttpHandler implements HttpRequestHandler {
    @Override
    public void handleRequest(HttpServletRequest request,
                              HttpServletResponse response,
                              Context context) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        response.getWriter().write("{\"message\":\"hello java fc\"}");
    }
}