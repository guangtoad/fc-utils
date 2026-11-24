#/bin/bash
curl --noproxy '*' -i -X \
  POST http://localhost:8080/fc/invoke/HelloHandler \
  -H "Content-Type: application/json" \
  -d '{"bucket":"my-bucket", "key":"test.txt"}'