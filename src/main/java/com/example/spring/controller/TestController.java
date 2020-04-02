package com.example.spring.controller;

import com.example.spring.annotation.MyController;
import com.example.spring.annotation.MyRequestMapping;
import com.example.spring.annotation.MyRequestParam;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@MyController
public class TestController {

    @MyRequestMapping("/test1")
    public void test1(HttpServletResponse response) {
        try {
            response.getWriter().println("test1 method success!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @MyRequestMapping("/test2")
    public void test2(HttpServletResponse response, @MyRequestParam("param") String param) {
        System.out.println(param);
        try {
            response.getWriter().write("test2 method success! param:" + param);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
