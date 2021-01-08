package com.igorgmoraes.sincronizacaoreceita;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SincronizacaoReceitaApplication{

	public static void main(String[] args) {
		String[] newArgs = new String[]{"fileName=" + args[0]};
		SpringApplication.exit(SpringApplication.run(SincronizacaoReceitaApplication.class, newArgs));
	}
}
