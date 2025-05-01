package org.osbo.bots.model.entity;

import jakarta.annotation.sql.DataSourceDefinition;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "messages")
public class Message {
    @Id
    private String id;
    private String messageid;
    private String texto;
    private String userid;
    private String expiracion;
    private String estado;
    private String media;

}
