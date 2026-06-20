package com.vendra.repo;

import com.vendra.domain.Category;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, String> {
  List<Category> findByIsActiveTrueOrderBySortOrderAsc();

  Optional<Category> findBySlug(String slug);
}
