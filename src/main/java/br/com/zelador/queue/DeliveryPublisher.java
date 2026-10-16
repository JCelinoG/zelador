package br.com.zelador.queue;

import br.com.zelador.domain.Delivery;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DeliveryPublisher {

    public static final String STREAM_KEY = "zelador:deliveries";

    private final StringRedisTemplate redis;

    public DeliveryPublisher(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void publicar(Delivery delivery) {
        Map<String, String> body = Map.of(
                "deliveryId", delivery.getId().toString(),
                "webhookId", delivery.getWebhookId().toString(),
                "tenantId", delivery.getTenantId().toString()
        );

        MapRecord<String, String, String> record =
                StreamRecords.mapBacked(body).withStreamKey(STREAM_KEY);

        redis.opsForStream().add(record);
    }
}