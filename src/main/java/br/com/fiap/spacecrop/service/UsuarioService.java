package br.com.fiap.spacecrop.service;

import br.com.fiap.spacecrop.dto.request.UsuarioUpdateRequestDTO;
import br.com.fiap.spacecrop.dto.response.UsuarioResponseDTO;
import br.com.fiap.spacecrop.entity.Usuario;
import br.com.fiap.spacecrop.exception.BusinessException;
import br.com.fiap.spacecrop.exception.ResourceNotFoundException;
import br.com.fiap.spacecrop.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public Page<UsuarioResponseDTO> listarTodos(String nome, String email, Pageable pageable) {
        return usuarioRepository.findAllWithFilters(nome, email, pageable)
            .map(this::toResponseDTO);
    }

    @Cacheable(value = "usuarios", key = "#id")
    public UsuarioResponseDTO buscarPorId(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com ID: " + id));
        return toResponseDTO(usuario);
    }

    // MÉTODO ADICIONADO PARA USO NO AlertaService e FazendaService
    public Usuario buscarEntidadePorId(Long id) {
        return usuarioRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com ID: " + id));
    }

    public Usuario buscarEntidadePorEmail(String email) {
        return usuarioRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com email: " + email));
    }

    @Transactional
    @CacheEvict(value = "usuarios", key = "#id")
    public UsuarioResponseDTO atualizar(Long id, UsuarioUpdateRequestDTO request) {
        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com ID: " + id));

        if (request.getNome() != null) {
            usuario.setNome(request.getNome());
        }
        if (request.getEmail() != null) {
            if (usuarioRepository.existsByEmail(request.getEmail()) && 
                !usuario.getEmail().equals(request.getEmail())) {
                throw new BusinessException("Email já está em uso");
            }
            usuario.setEmail(request.getEmail());
        }
        if (request.getSenha() != null) {
            usuario.setSenhaHash(passwordEncoder.encode(request.getSenha()));
        }

        usuario = usuarioRepository.save(usuario);
        return toResponseDTO(usuario);
    }

    @Transactional
    @CacheEvict(value = "usuarios", key = "#id")
    public void deletar(Long id) {
        if (!usuarioRepository.existsById(id)) {
            throw new ResourceNotFoundException("Usuário não encontrado com ID: " + id);
        }
        usuarioRepository.deleteById(id);
    }

    private UsuarioResponseDTO toResponseDTO(Usuario usuario) {
        return UsuarioResponseDTO.builder()
            .id(usuario.getId())
            .nome(usuario.getNome())
            .email(usuario.getEmail())
            .build();
    }
}