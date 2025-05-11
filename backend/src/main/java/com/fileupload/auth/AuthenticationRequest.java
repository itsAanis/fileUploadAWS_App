package com.fileupload.auth;

public record AuthenticationRequest(
        String username,
        String password
) {
}
