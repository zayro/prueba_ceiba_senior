package com.example.ceiba.domain.port;

import org.reactivestreams.Publisher;

public interface OutboxPublishingUseCase {
	Publisher<Void> publishPending();
}
