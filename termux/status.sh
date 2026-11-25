#!/data/data/com.termux/files/usr/bin/bash
# Shows whether the local model server for Sora is healthy, and the end of its log.
SORA_DIR="${SORA_DIR:-$HOME/.sora}"
# shellcheck disable=SC1091
[ -f "$SORA_DIR/llama.conf" ] && . "$SORA_DIR/llama.conf"
: "${HOST:=127.0.0.1}" "${PORT:=8080}"
echo "health : $(curl -s --max-time 2 "http://$HOST:$PORT/health" || echo 'not answering')"
echo "tmux   : $(tmux ls 2>&1 | head -3)"
echo "procs  : $(pgrep -af 'llama-server|llama-supervisor' | cut -c1-120)"
echo "--- last log lines ---"
tail -n 15 "$SORA_DIR/llama.log" 2>/dev/null || echo "(no log yet)"
