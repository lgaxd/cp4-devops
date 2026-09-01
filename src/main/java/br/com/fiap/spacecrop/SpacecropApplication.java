package br.com.fiap.spacecrop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class SpacecropApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpacecropApplication.class, args);
	}

}
