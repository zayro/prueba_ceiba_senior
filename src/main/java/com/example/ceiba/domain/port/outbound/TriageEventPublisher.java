package com.example.ceiba.domain.port.outbound;

import com.example.ceiba.domain.model.TriageEvent;
import org.reactivestreams.Publisher;

public interface TriageEventPublisher {
	Publisher<Void> publish(TriageEvent event);
}
