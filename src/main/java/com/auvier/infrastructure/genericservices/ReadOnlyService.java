package com.auvier.infrastructure.genericservices;

public interface ReadOnlyService<D, Did> extends FindOneService<D, Did>, FindAll<D> {
}
