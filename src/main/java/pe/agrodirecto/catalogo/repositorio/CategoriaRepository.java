package pe.agrodirecto.catalogo.repositorio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.agrodirecto.catalogo.dominio.Categoria;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findAllByOrderByNombre();
}
