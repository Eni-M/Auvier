package com.auvier.infrastructure.genericservices;

public interface FindOneService<D, Did> {
    D findOne(Did id);
}
