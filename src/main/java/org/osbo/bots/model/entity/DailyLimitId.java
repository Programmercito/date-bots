package org.osbo.bots.model.entity;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Composite primary key for {@link DailyLimit}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyLimitId implements Serializable {

    private String chatid;

    private String date;

}
