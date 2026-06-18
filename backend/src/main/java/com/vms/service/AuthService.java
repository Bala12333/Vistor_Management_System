package com.vms.service;

import com.vms.entity.User;

public interface AuthService {
    
    String login(String email, String password);
    
    User getCurrentUser();
    
    void logout(String token);
}
