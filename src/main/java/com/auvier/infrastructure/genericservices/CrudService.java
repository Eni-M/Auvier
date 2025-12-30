package com.auvier.infrastructure.genericservices;

public interface CrudService<D, Did> extends ReadOnlyService<D, Did>, WriteOnlyService<D, Did> {
}
