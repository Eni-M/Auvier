package com.auvier.infrastructure.genericservices;

public interface ModifyService<D, Did> {
    D modify(Did id, D model);
}
