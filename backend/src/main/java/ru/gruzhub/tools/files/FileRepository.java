package ru.gruzhub.tools.files;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.gruzhub.tools.files.models.File;

@Repository
public interface FileRepository extends JpaRepository<File, String> {}
