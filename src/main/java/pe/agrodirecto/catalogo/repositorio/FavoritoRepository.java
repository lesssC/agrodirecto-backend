package pe.agrodirecto.catalogo.repositorio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.agrodirecto.catalogo.dominio.Favorito;

public interface FavoritoRepository extends JpaRepository<Favorito, Favorito.Clave> {

    List<Favorito> findByIdUsuarioId(Long usuarioId);
}
