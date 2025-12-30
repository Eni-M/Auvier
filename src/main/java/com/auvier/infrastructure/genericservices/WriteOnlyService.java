package com.auvier.infrastructure.genericservices;

public interface WriteOnlyService<D, Did> extends AddService<D>, ModifyService<D, Did>, RemoveService<Did> {
}
