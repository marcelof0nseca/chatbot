package com.wxn.chatbot.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wxn.chatbot.dominio.Atendente;

public interface AtendenteRepository extends JpaRepository<Atendente, Long> {
}
