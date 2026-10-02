# Run after docker compose up (Redpanda)
$topics = @("capture.ingested", "capture.processed", "capture.ingested.dlq")
foreach ($t in $topics) {
  docker compose exec redpanda rpk topic create $t -X brokers=localhost:9092 2>$null
}
Write-Host "Kafka topics ready."
