package com.chngy.jobscraper.Runner;

import com.chngy.jobscraper.App;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class Runner {
    public static void main(String[] args){
        App app = new App();
        String result = app.handleRequest(Map.of(), null);
        log.info("Result of run : {}", result);
    }
}
