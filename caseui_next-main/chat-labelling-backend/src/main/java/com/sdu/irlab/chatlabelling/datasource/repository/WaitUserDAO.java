package com.sdu.irlab.chatlabelling.datasource.repository;

import com.sdu.irlab.chatlabelling.datasource.domain.WaitUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.persistence.Cacheable;

import java.util.List;

@Repository
public interface WaitUserDAO  extends JpaRepository<WaitUser,Long> {
    public WaitUser findByName(String username);
    public List<WaitUser> findByRole(String role);
}
