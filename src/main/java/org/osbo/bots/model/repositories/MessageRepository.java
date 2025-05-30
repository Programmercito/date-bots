package org.osbo.bots.model.repositories;

import java.util.List;

import org.osbo.bots.model.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends JpaRepository<Message, String> {
    @Query(value = "SELECT COUNT(*) > 0 FROM messages WHERE datetime(expiracion) >= datetime('now','localtime', '-1 hour') and userid=:user and estado in ('publicado','pendiente')", nativeQuery = true)
    public int existsMessageInLastHour(String user);
    @Query(value = "SELECT * FROM messages WHERE datetime(expiracion) < datetime('now','localtime', '-1 hour') and estado='publicado'", nativeQuery = true)
    public List<Message> vencidos();
}