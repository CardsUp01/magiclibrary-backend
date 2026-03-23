package com.magiclibrary.repositories.interfaces;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.magiclibrary.entities.User;

public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByEmailUser(String emailUser);

    @Query("""
            SELECT u
            FROM User u
            JOIN FETCH u.role
            WHERE u.idUser = :idUser
           """)
    Optional<User> findByIdUserWithRole(@Param("idUser") Integer idUser);

    @Query("""
            SELECT u
            FROM User u
            JOIN FETCH u.role
            WHERE u.emailUser = :emailUser
           """)
    Optional<User> findByEmailUserWithRole(@Param("emailUser") String emailUser);

    boolean existsByEmailUser(String emailUser);

    @Query("""
            SELECT DISTINCT u
            FROM User u
            JOIN FETCH u.role r
            WHERE (
                    :search IS NULL
                    OR :search = ''
                    OR LOWER(u.firstNameUser) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(u.lastNameUser) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(u.emailUser) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(r.labelRole) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR STR(u.idUser) LIKE CONCAT('%', :search, '%')
                  )
              AND (
                    :role IS NULL
                    OR :role = ''
                    OR UPPER(r.labelRole) = UPPER(:role)
                  )
              AND (
                    :status IS NULL
                    OR u.activeUser = :status
                  )
            ORDER BY u.lastNameUser ASC, u.firstNameUser ASC, u.idUser ASC
           """)
    List<User> findAllWithFilters(
            @Param("search") String search,
            @Param("role") String role,
            @Param("status") Boolean status
    );

    @Query(
            value = """
                    SELECT u
                    FROM User u
                    JOIN FETCH u.role r
                    WHERE (
                            :search IS NULL
                            OR :search = ''
                            OR LOWER(u.firstNameUser) LIKE LOWER(CONCAT('%', :search, '%'))
                            OR LOWER(u.lastNameUser) LIKE LOWER(CONCAT('%', :search, '%'))
                            OR LOWER(u.emailUser) LIKE LOWER(CONCAT('%', :search, '%'))
                            OR LOWER(r.labelRole) LIKE LOWER(CONCAT('%', :search, '%'))
                            OR STR(u.idUser) LIKE CONCAT('%', :search, '%')
                          )
                      AND (
                            :role IS NULL
                            OR :role = ''
                            OR UPPER(r.labelRole) = UPPER(:role)
                          )
                      AND (
                            :status IS NULL
                            OR u.activeUser = :status
                          )
                    """,
            countQuery = """
                    SELECT COUNT(u)
                    FROM User u
                    JOIN u.role r
                    WHERE (
                            :search IS NULL
                            OR :search = ''
                            OR LOWER(u.firstNameUser) LIKE LOWER(CONCAT('%', :search, '%'))
                            OR LOWER(u.lastNameUser) LIKE LOWER(CONCAT('%', :search, '%'))
                            OR LOWER(u.emailUser) LIKE LOWER(CONCAT('%', :search, '%'))
                            OR LOWER(r.labelRole) LIKE LOWER(CONCAT('%', :search, '%'))
                            OR STR(u.idUser) LIKE CONCAT('%', :search, '%')
                          )
                      AND (
                            :role IS NULL
                            OR :role = ''
                            OR UPPER(r.labelRole) = UPPER(:role)
                          )
                      AND (
                            :status IS NULL
                            OR u.activeUser = :status
                          )
                    """
    )
    Page<User> findAllWithFiltersPaged(
            @Param("search") String search,
            @Param("role") String role,
            @Param("status") Boolean status,
            Pageable pageable
    );
}