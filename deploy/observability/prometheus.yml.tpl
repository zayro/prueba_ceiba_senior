global:
  scrape_interval: 15s

scrape_configs:
  - job_name: triage
    metrics_path: /actuator/prometheus
    http_headers:
      X-API-Key:
        values: ["__API_KEY__"]
    static_configs:
      - targets: ["triage:8080"]

  - job_name: assistant
    metrics_path: /actuator/prometheus
    http_headers:
      X-API-Key:
        values: ["__API_KEY__"]
    static_configs:
      - targets: ["assistant:8081"]
