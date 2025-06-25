package com.example;

public class Main {
    public static final String GREETING = "Hello, World!";

    public static void main(String[] args) {
        Main main = new Main();
        System.out.println(main.getGreeting());
    }

    public String getGreeting() {
        return GREETING;
    }
}
