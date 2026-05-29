#!/bin/bash
set -eo pipefail

# ==================================================================================
# SYSTEM DIAGNOSTIC CHECKS
# ==================================================================================
echo "======================================================================"
echo "          PROJECT HERMES SYSTEM DIAGNOSTICS & INITIALIZATION          "
echo "======================================================================"
echo "Timestamp: $(date -u)"
echo "User: $(whoami) (UID: $(id -u))"
echo "Work Directory: $(pwd)"
echo "Java Version: $(java -version 2>&1 | head -n 1)"

# Check if JAR file exists
JAR_PATH="/app/Project-Hermes.jar"
if [ ! -f "$JAR_PATH" ]; then
    echo "[CRITICAL ERROR] Executable JAR not found at $JAR_PATH." >&2
    exit 1
fi
echo "[SUCCESS] Executable JAR verified: $(du -sh $JAR_PATH)"

# ==================================================================================
# VIRTUAL DISPLAY SERVER (XVFB) SETUP
# ==================================================================================
export DISPLAY=:99
export RESOLUTION="1350x950x24"

# Clean up lock files if they exist from previous runs
rm -f /tmp/.X99-lock /tmp/.X11-unix/X99

echo "Launching Xvfb on display $DISPLAY with resolution $RESOLUTION..."
Xvfb $DISPLAY -screen 0 $RESOLUTION -ac +extension RANDR &
XVFB_PID=$!

# Failsafe: Wait and verify Xvfb started successfully
for i in {1..10}; do
    if [ -S "/tmp/.X11-unix/X99" ]; then
        echo "[SUCCESS] Xvfb virtual frame buffer initialized (PID: $XVFB_PID)."
        break
    fi
    if [ $i -eq 10 ]; then
        echo "[CRITICAL ERROR] Xvfb failed to start within timeout." >&2
        exit 1
    fi
    sleep 0.5
done

# ==================================================================================
# WINDOW MANAGER (OPENBOX) INITIALIZATION
# ==================================================================================
echo "Launching Openbox window manager..."
openbox-session &
OB_PID=$!

# Verify Openbox started
sleep 1
if kill -0 $OB_PID 2>/dev/null; then
    echo "[SUCCESS] Openbox window manager active (PID: $OB_PID)."
else
    echo "[WARNING] Openbox window manager failed to start. Window positioning may be basic."
fi

# ==================================================================================
# X11VNC SERVER INITIALIZATION
# ==================================================================================
echo "Launching x11vnc server on port 5900..."
# Run x11vnc: shared session, forever, no password, bind to localhost
x11vnc -forever -shared -nopw -display $DISPLAY -listen localhost -bg -o /tmp/x11vnc.log
sleep 1

# Check VNC listening state
if ss -tuln | grep -q ":5900 "; then
    echo "[SUCCESS] VNC server listening on port 5900."
else
    echo "[CRITICAL ERROR] VNC server failed to bind to port 5900. Log output:" >&2
    cat /tmp/x11vnc.log >&2
    exit 1
fi

# ==================================================================================
# WEBSOCKIFY PROXY SETUP (noVNC Bridge)
# ==================================================================================
echo "Launching websockify proxy (VNC-to-WebSocket) on port 7860..."
# Route public HF port 7860 to local VNC port 5900, serving noVNC web client files
websockify --web=/usr/share/novnc/ 7860 localhost:5900 &
WEBSOCKIFY_PID=$!

# Failsafe: Verify websockify binds to port 7860
for i in {1..10}; do
    if ss -tuln | grep -q ":7860 "; then
        echo "[SUCCESS] websockify proxy successfully active on port 7860 (PID: $WEBSOCKIFY_PID)."
        break
    fi
    if [ $i -eq 10 ]; then
        echo "[CRITICAL ERROR] websockify proxy failed to bind to port 7860." >&2
        exit 1
    fi
    sleep 0.5
done

# ==================================================================================
# EXECUTE APPLICATION
# ==================================================================================
echo "======================================================================"
echo "               LAUNCHING PROJECT HERMES APPLICATION                   "
echo "======================================================================"

# Run the Java application with optimized JVM options
java $JAVA_OPTS -jar "$JAR_PATH" 2>&1 | tee /tmp/app_execution.log
