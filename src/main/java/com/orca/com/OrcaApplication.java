package com.orca.com;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.orca.com.config.UdpProperties;

@SpringBootApplication
@EnableConfigurationProperties({UdpProperties.class})
public class OrcaApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrcaApplication.class, args);
	}

}
