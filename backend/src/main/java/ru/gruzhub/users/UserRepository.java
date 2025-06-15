package ru.gruzhub.users;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.gruzhub.users.enums.UserRole;
import ru.gruzhub.users.models.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailAndRole(String email, UserRole role);

    Optional<User> findByEmail(String email);

    Optional<User> findByPhoneAndRole(String phone, UserRole role);

    @Query("SELECT u FROM User u WHERE u.role = :role AND u.address" + ".region.id = :regionId")
    List<User> findMastersInRegion(UserRole role, Long regionId);

    List<User> findByIdIn(List<Long> ids);

    List<User> findByRole(UserRole role);

    @Query("SELECT u FROM User u WHERE u.role IN :roles ORDER BY u.id DESC")
    List<User> findUsersByRoles(List<UserRole> roles);

    @Query("SELECT u FROM User u WHERE u.address.region.id IN :regionsIds ORDER BY u.id DESC")
    List<User> findUsersByRegions(List<Long> regionsIds);

    @Query("SELECT u FROM User u WHERE u.role IN :roles AND u.address.region.id IN :regionsIds " +
           "ORDER BY u.id DESC")
    List<User> findUsersByRolesAndRegions(List<UserRole> roles, List<Long> regionsIds);

    @Query("SELECT u FROM User u ORDER BY u.id DESC")
    List<User> findAllUsers();
}
