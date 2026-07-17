package org.osbo.bots.jms.queue.pojos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Button {
    private String text;
    private String callbackData;
    private String url;

    public Button(String text, String callbackData) {
        this.text = text;
        this.callbackData = callbackData;
    }
}
