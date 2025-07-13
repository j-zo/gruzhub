package ru.gruzhub.document.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.gruzhub.document.model.Document;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    Document removeById(Long id);
}
