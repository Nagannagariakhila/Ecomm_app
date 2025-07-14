package com.acc.repository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.acc.entity.Address;
@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByProfileId(Long profileId);

}