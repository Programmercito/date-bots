package org.osbo.bots.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    private String chatid;
    private String user;
    private String fecha_registro;
    private String estado;
    private String comando;

}
