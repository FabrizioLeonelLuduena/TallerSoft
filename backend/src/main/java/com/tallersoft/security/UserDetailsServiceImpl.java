package com.tallersoft.security;

import com.tallersoft.model.Usuario;
import com.tallersoft.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * UserDetailsService implementation for Spring Security
 * Loads user from database by email
 */
@Slf4j
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Usuario no encontrado: {}", email);
                    return new UsernameNotFoundException("Usuario no encontrado: " + email);
                });

        if (!usuario.isActivo()) {
            log.warn("Usuario inactivo: {}", email);
            throw new UsernameNotFoundException("Usuario inactivo: " + email);
        }

        return usuario;
    }
}
