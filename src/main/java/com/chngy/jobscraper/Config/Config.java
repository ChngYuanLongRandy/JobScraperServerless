package com.chngy.jobscraper.Config;

import com.chngy.jobscraper.App;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

// This componentScan is needed since its not running on spring boot, just spring context
@Configuration
@ComponentScan(basePackageClasses = App.class)
public class Config {

}
