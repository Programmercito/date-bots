package org.osbo.bots;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling 
public class DatesBotApplication {


	public static void main(String[] args) {
		SpringApplication.run(DatesBotApplication.class, args);
	}

}
