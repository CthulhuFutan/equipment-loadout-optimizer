package com.dvm.rune_server_backend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
/**
 * @author 25826
 */
@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class})
@MapperScan("com.dvm.rune_server_backend.mapper")
public class RuneServerBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(RuneServerBackendApplication.class, args);
	}

}
