package com.wxn.chatbot.repositorio;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wxn.chatbot.dominio.Cliente;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByTelefone(String telefone);
}
