#/bin/bash
curl --noproxy '*' -i -X \
  POST http://localhost:8080/fc/load \
  -H "Content-Type: application/json" \
  -d '{}'