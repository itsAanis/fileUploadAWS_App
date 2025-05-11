package com.fileupload.customer;

public record CustomerUpdateRequest(
        String name,
        String email,
        Integer age
) {
}
