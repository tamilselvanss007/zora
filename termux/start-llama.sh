#!/data/data/com.termux/files/usr/bin/bash
# Starts the local model server if it is not already running. Safe to run any number of times: it does nothing when
# the server answers or a supervisor is already starting it. Called by the Sora app (through Termux) and at boot.
SORA_DIR="${SORA_DIR:-$HOME/.sora}"
# shellcheck disable=SC1091
[ -f "$SORA_DIR/llama.conf" ] && . "$SORA_DIR/llama.conf"
: "${HOST:=127.0.0.1}" "${PORT:=8080}"
SESSION=sora-ai

# 1. Already answering? Done.
if curl -sf --max-time 1 "http://$HOST:$PORT/health" >/dev/null 2>&1; then exit 0; fi

# 2. A supervisor is already running (the model is loading, or it is restarting after a crash)? Leave it alone.
if tmux has-session -t "$SESSION" 2>/dev/null && pgrep -f "llama-supervisor.sh" >/dev/null 2>&1; then exit 0; fi

# 3. Leftover session without a supervisor: clear it, then start fresh.
tmux kill-session -t "$SESSION" 2>/dev/null
command -v termux-wake-lock >/dev/null 2>&1 && termux-wake-lock
tmux new-session -d -s "$SESSION" "bash '$SORA_DIR/llama-supervisor.sh'"
