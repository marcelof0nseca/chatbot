package com.nxn.chatbot.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nxn.chatbot.dominio.Atendente;

public interface AtendenteRepository extends JpaRepository<Atendente, Long> {
}
