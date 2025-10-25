const queryParams = new URLSearchParams(window.location.search);
const STREAM_BASE = (queryParams.get('signal')?.trim() || `${window.location.origin}/api/remoteview`).replace(/\/$/, '');
const SESSION_TTL_MS = 120_000;

const els = {
  sessionId: document.getElementById('sessionId'),
  authToken: document.getElementById('authToken'),
  expiresIn: document.getElementById('expiresIn'),
  status: document.getElementById('status'),
  hint: document.getElementById('hintText'),
  connectBtn: document.getElementById('connectBtn'),
  disconnectBtn: document.getElementById('disconnectBtn'),
  stream: document.getElementById('remoteStream'),
  videoOverlay: document.getElementById('videoOverlay'),
};

const state = {
  sessionId: null,
  authToken: null,
  expiresAt: null,
  countdownTimer: null,
  streamBase: STREAM_BASE,
  currentStreamUrl: null,
  connected: false,
  disconnecting: false,
};

function parseInitialState() {
  state.sessionId = queryParams.get('session')?.trim() || null;
  state.authToken = queryParams.get('auth')?.trim() || null;

  const expiresParam = queryParams.get('expires');
  if (expiresParam) {
    const parsed = Number(expiresParam);
    state.expiresAt = Number.isFinite(parsed) && parsed > Date.now()
      ? parsed
      : Date.now() + SESSION_TTL_MS;
  } else {
    state.expiresAt = Date.now() + SESSION_TTL_MS;
  }

  updateUiState();
}

function updateUiState() {
  els.sessionId.textContent = state.sessionId || '—';
  els.authToken.textContent = state.authToken ? maskToken(state.authToken) : '—';

  const ready = Boolean(state.sessionId && state.authToken);
  els.connectBtn.disabled = !ready || state.expiresAt <= Date.now();
  els.disconnectBtn.disabled = !state.connected;
  els.hint.textContent = ready
    ? 'Ready to connect. Make sure you can reach the host over LAN or VPN.'
    : 'Paste a valid Remote View link (with session + auth) to begin.';

  updateCountdown();
}

function maskToken(token) {
  if (!token || token.length <= 6) {
    return token || '';
  }
  return `${token.slice(0, 3)}•••${token.slice(-3)}`;
}

function updateCountdown() {
  if (state.countdownTimer) {
    clearInterval(state.countdownTimer);
  }

  const tick = () => {
    const remaining = Math.max(0, state.expiresAt - Date.now());
    const seconds = Math.ceil(remaining / 1000);
    els.expiresIn.textContent = `${seconds}s`;

    if (remaining <= 0) {
      els.connectBtn.disabled = true;
      if (!state.connected) {
        setStatus('Link expired – request a fresh /remoteview link.');
      }
      clearInterval(state.countdownTimer);
    }
  };

  tick();
  state.countdownTimer = setInterval(tick, 1000);
}

function startStream() {
  if (!state.sessionId || !state.authToken) {
    return;
  }

  state.disconnecting = false;
  state.connected = false;

  const streamUrl = `${state.streamBase}/stream?session=${encodeURIComponent(state.sessionId)}&auth=${encodeURIComponent(state.authToken)}`;
  state.currentStreamUrl = streamUrl;

  els.videoOverlay.style.display = 'flex';
  els.videoOverlay.textContent = 'Connecting…';
  setStatus('Connecting to stream…');

  els.connectBtn.disabled = true;
  els.disconnectBtn.disabled = false;

  els.stream.src = streamUrl;
}

function stopStream() {
  state.disconnecting = true;
  state.connected = false;
  state.currentStreamUrl = null;

  els.stream.removeAttribute('src');
  els.stream.src = '';
  els.videoOverlay.style.display = 'flex';
  els.videoOverlay.textContent = 'Waiting for stream…';

  els.disconnectBtn.disabled = true;
}

function setStatus(text) {
  els.status.textContent = text;
}

els.stream.addEventListener('load', () => {
  if (!state.currentStreamUrl) {
    return;
  }

  state.connected = true;
  els.connectBtn.disabled = true;
  els.disconnectBtn.disabled = false;
  els.videoOverlay.style.display = 'none';
  setStatus('Streaming');
});

els.stream.addEventListener('error', () => {
  if (state.disconnecting) {
    return;
  }

  state.connected = false;
  state.currentStreamUrl = null;
  els.connectBtn.disabled = state.expiresAt <= Date.now();
  els.disconnectBtn.disabled = true;
  els.videoOverlay.style.display = 'flex';
  els.videoOverlay.textContent = 'Failed – check connectivity';
  setStatus('Failed to connect – ensure the host is reachable.');
});

els.connectBtn.addEventListener('click', () => {
  if (!state.sessionId || !state.authToken) {
    return;
  }
  startStream();
});

els.disconnectBtn.addEventListener('click', () => {
  stopStream();
  setStatus('Disconnected');
  els.disconnectBtn.disabled = true;
  els.connectBtn.disabled = state.expiresAt <= Date.now();
});

window.addEventListener('beforeunload', () => {
  if (state.connected) {
    stopStream();
  }
});

parseInitialState();
setStatus(state.sessionId && state.authToken ? 'Ready' : 'Awaiting parameters…');
