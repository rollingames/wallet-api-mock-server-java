package com.rollingames.sofa.mock.repository;

import com.rollingames.sofa.mock.entity.ApiToken;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApiTokenRepository extends CrudRepository<ApiToken, Long> {
}
