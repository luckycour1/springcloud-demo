package com.example.common.dto;

import java.io.Serializable;
import lombok.Data;

@Data
public class UserDTO implements Serializable {
    private Long id;
    private String username;
    private String email;

}


