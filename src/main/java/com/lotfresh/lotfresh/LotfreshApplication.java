package com.lotfresh.lotfresh;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableBatchProcessing
public class LotfreshApplication {

	public static void main(String[] args) {
		SpringApplication.run(LotfreshApplication.class, args);
	}

}
