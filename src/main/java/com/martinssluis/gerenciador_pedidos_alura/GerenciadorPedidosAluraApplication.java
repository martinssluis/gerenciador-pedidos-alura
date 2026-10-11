package com.martinssluis.gerenciador_pedidos_alura;

import com.martinssluis.gerenciador_pedidos_alura.principal.Principal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GerenciadorPedidosAluraApplication implements CommandLineRunner {

	public static void main(String[] args) {
		SpringApplication.run(GerenciadorPedidosAluraApplication.class, args);
	}

	@Autowired
	private Principal principal;

	@Override
	public void run(String... args) throws Exception{
		principal.salvarNoBanco();
	}

}
