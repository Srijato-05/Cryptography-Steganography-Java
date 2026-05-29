# ==================================================================================
# RUNTIME ENVIRONMENT SETUP (Production JRE for Minimal Footprint)
# ==================================================================================
FROM eclipse-temurin:17-jre-focal

# Prevent interactive prompts during package installation
ENV DEBIAN_FRONTEND=noninteractive
ENV HOME=/home/appuser

# System telemetry & X11 environment flags
ENV DISPLAY=:99
ENV RESOLUTION=1350x950x24

# JVM Performance & Anti-Aliasing Tuning for Headless VNC Environments:
# - Force OpenGL hardware acceleration inside Xvfb
# - Force system-wide subpixel anti-aliased font rendering
ENV JAVA_OPTS="-Dsun.java2d.opengl=true -Dawt.useSystemAAFontSettings=on -Dswing.aatext=true"

# Install virtual frame buffer (Xvfb), VNC server, Openbox window manager, noVNC web client, curl for healthchecks, and procps
RUN apt-get update && apt-get install -y --no-install-recommends \
    xvfb \
    x11vnc \
    novnc \
    websockify \
    openbox \
    curl \
    procps \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Inject noVNC auto-connect & scale redirect script directly into the webroot:
# This bypasses the default noVNC login/connect buttons and auto-fits the Swing GUI to the browser window.
RUN echo '<!DOCTYPE html>\n\
<html>\n\
<head>\n\
    <meta http-equiv="refresh" content="0; url=vnc.html?path=websockify&autoconnect=true&resize=scale&reconnect=true">\n\
</head>\n\
<body>\n\
</body>\n\
</html>' > /usr/share/novnc/index.html

# Create a non-root system user with UID 1000 (compliant with Hugging Face security sandboxing)
RUN useradd -m -u 1000 appuser && \
    mkdir -p /app && \
    chown -R appuser:appuser /app

# Switch context to the sandboxed user
USER appuser
RUN mkdir -p /home/appuser/.config/openbox

# Write the Openbox configuration file to strip window borders/decorations and auto-maximize Java Swing panels
RUN echo '<?xml version="1.0" encoding="UTF-8"?>\n\
<openbox_config xmlns="http://openbox.org/3.0/rc">\n\
  <applications>\n\
    <application class="*">\n\
      <maximized>true</maximized>\n\
      <decor>no</decor>\n\
    </application>\n\
  </applications>\n\
</openbox_config>' > /home/appuser/.config/openbox/rc.xml

# Set up working directory
WORKDIR /app

# Copy the application binary and the entrypoint startup script
COPY --chown=appuser:appuser Project-Hermes.jar /app/Project-Hermes.jar
COPY --chown=appuser:appuser entrypoint.sh /app/entrypoint.sh

# Ensure the startup script is executable
RUN chmod +x /app/entrypoint.sh

# Expose port 7860 (Hugging Face default app port)
EXPOSE 7860

# Add a healthcheck verification loop to ensure websockify proxy is responsive on port 7860
HEALTHCHECK --interval=15s --timeout=5s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:7860/ || exit 1

# Start the application services using the entrypoint script
CMD ["/app/entrypoint.sh"]
