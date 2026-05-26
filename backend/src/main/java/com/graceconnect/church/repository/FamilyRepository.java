package com.graceconnect.church.repository;

import com.graceconnect.church.model.Family;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FamilyRepository extends JpaRepository<Family, Long> {
    
    @Query("SELECT DISTINCT f FROM Family f LEFT JOIN f.members m WHERE " +
           "LOWER(f.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(f.address) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(f.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(m.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(m.lastName) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Family> searchFamilies(@Param("query") String query, Pageable pageable);
}
