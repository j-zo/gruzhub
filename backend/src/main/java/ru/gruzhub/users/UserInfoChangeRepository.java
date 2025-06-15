package ru.gruzhub.users;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.gruzhub.users.models.UserInfoChange;

@Repository
public interface UserInfoChangeRepository extends JpaRepository<UserInfoChange, Long> {
    List<UserInfoChange> findByUserIdOrderByIdDesc(Long userId);
}
