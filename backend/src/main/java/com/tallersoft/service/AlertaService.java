package com.tallersoft.service;

import com.tallersoft.exception.EntityNotFoundException;
import com.tallersoft.model.AlertaLeida;
import com.tallersoft.model.Usuario;
import com.tallersoft.repository.AlertaLeidaRepository;
import com.tallersoft.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertaService {

    private final AlertaLeidaRepository alertaLeidaRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public void marcarLeida(Long usuarioId, String alertaKey) {
        if (alertaLeidaRepository.existsByUsuarioIdAndAlertaKey(usuarioId, alertaKey)) {
            return;
        }
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado con id: " + usuarioId));

        AlertaLeida al = AlertaLeida.builder()
                .usuario(usuario)
                .alertaKey(alertaKey)
                .build();
        alertaLeidaRepository.save(al);
        log.debug("Alerta '{}' marcada como leída para usuario {}", alertaKey, usuarioId);
    }

    @Transactional(readOnly = true)
    public List<String> obtenerAlertasLeidas(Long usuarioId) {
        return alertaLeidaRepository.findAlertaKeysByUsuarioId(usuarioId);
    }
}
