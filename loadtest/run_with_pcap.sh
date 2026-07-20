#!/usr/bin/env bash
# Usage: sudo ./run_with_pcap.sh <k6_script> <base_url> <auth_token>
# Example: sudo ./run_with_pcap.sh loadtest/k6/wallet_10k_1M.js http://10.0.0.5:8083 my-token

set -euo pipefail
SCRIPT=${1:-loadtest/k6/wallet_10k_1M.js}
BASE_URL=${2:-http://localhost:8083}
AUTH_TOKEN=${3:-dummy-token}
PCAP_FILE="k6_run_$(date +%Y%m%d_%H%M%S).pcap"

echo "Starting tcpdump (capture to ${PCAP_FILE}) - requires sudo"
# Capture only traffic to the target host (BASE_URL host) - extract host
TARGET_HOST=$(echo ${BASE_URL} | sed -E 's#https?://([^/:]+).*#\1#')
# Start tcpdump in background capturing IP packets to/from TARGET_HOST
sudo timeout 300 tcpdump -i any host ${TARGET_HOST} -w "${PCAP_FILE}" &
TCPDUMP_PID=$!

# Run k6 with environment
echo "Running k6 script ${SCRIPT} against ${BASE_URL}"
BASE_URL=${BASE_URL} TEST_TOKEN=${AUTH_TOKEN} k6 run ${SCRIPT}

# After k6 exits, give tcpdump a moment to flush
sleep 2
if ps -p ${TCPDUMP_PID} > /dev/null 2>&1; then
  echo "Stopping tcpdump (pid ${TCPDUMP_PID})"
  sudo kill ${TCPDUMP_PID} || true
fi

# Move PCAP to artifacts directory
mkdir -p loadtest/artifacts
mv ${PCAP_FILE} loadtest/artifacts/
ls -lh loadtest/artifacts/${PCAP_FILE}

echo "PCAP saved to loadtest/artifacts/${PCAP_FILE}"

echo "Done"

