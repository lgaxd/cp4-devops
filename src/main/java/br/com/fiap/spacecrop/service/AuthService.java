package br.com.fiap.spacecrop.service;

import br.com.fiap.spacecrop.config.JwtUtil;
import br.com.fiap.spacecrop.dto.request.LoginRequestDTO;
import br.com.fiap.spacecrop.dto.request.RegisterRequestDTO;
import br.com.fiap.spacecrop.dto.response.LoginResponseDTO;
import br.com.fiap.spacecrop.entity.Usuario;
import br.com.fiap.spacecrop.exception.BusinessException;
import br.com.fiap.spacecrop.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @Transactional
    public LoginResponseDTO register(RegisterRequestDTO request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email já cadastrado");
        }

        Usuario usuario = Usuario.builder()
            .nome(request.getNome())
            .email(request.getEmail())
            .senhaHash(passwordEncoder.encode(request.getSenha()))
            .build();

        usuario = usuarioRepository.save(usuario);

        String token = jwtUtil.generateToken(
            new org.springframework.security.core.userdetails.User(
                usuario.getEmail(),
                usuario.getSenhaHash(),
                java.util.Collections.emptyList()
            )
        );

        return new LoginResponseDTO(token, "Bearer", usuario.getId(), usuario.getNome(), usuario.getEmail());
    }

    public LoginResponseDTO login(LoginRequestDTO request) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getSenha())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtUtil.generateToken(userDetails);

        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new BusinessException("Usuário não encontrado"));

        return new LoginResponseDTO(token, "Bearer", usuario.getId(), usuario.getNome(), usuario.getEmail());
    }

    public void logout() {
        SecurityContextHolder.clearContext();
    }
}