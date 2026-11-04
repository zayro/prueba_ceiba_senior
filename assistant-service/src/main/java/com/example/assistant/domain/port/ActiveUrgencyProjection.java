package com.example.assistant.domain.port;

import com.example.assistant.domain.model.TriageEvent;
import java.util.Map;
import org.reactivestreams.Publisher;

public interface ActiveUrgencyProjection {
	Publisher<Long> apply(TriageEvent event);

	Publisher<Map<String, Long>> activeCounts();
}
