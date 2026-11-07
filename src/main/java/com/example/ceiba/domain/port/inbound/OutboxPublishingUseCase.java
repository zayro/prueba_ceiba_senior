package com.example.ceiba.domain.port.inbound;

import org.reactivestreams.Publisher;

public interface OutboxPublishingUseCase {
	Publisher<Void> publishPending();
}
