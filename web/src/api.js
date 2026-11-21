export const SESSION_KEY = "musicloud.web.session";
export const API_BASE_URL = (
  import.meta.env.VITE_API_BASE_URL || "http://localhost:8080"
).replace(/\/+$/, "");

export class ApiError extends Error {
  constructor(message, status, details) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.details = details;
  }
}

function getStoredToken() {
  try {
    const stored = sessionStorage.getItem(SESSION_KEY);
    if (!stored) return null;
    const session = JSON.parse(stored);
    return session.expiresAt > Date.now() ? session.token : null;
  } catch {
    return null;
  }
}

function formatDetails(details) {
  if (!details || typeof details !== "object") return "";
  return Object.entries(details)
    .map(([field, message]) => `${field}: ${message}`)
    .join(" ");
}

export async function api(path, { method = "GET", body, auth = true, signal } = {}) {
  const token = auth ? getStoredToken() : null;
  const headers = { Accept: "application/json" };

  if (body !== undefined) headers["Content-Type"] = "application/json";
  if (token) headers.Authorization = `Bearer ${token}`;

  let response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      signal,
    });
  } catch (error) {
    if (error.name === "AbortError") throw error;
    throw new ApiError(
      "Could not reach MusicLoud. Check your connection and try again.",
      0,
    );
  }

  const text = await response.text();
  let payload = null;
  if (text) {
    try {
      payload = JSON.parse(text);
    } catch {
      payload = { message: text };
    }
  }

  if (!response.ok) {
    if (response.status === 401 && token) {
      sessionStorage.removeItem(SESSION_KEY);
      window.dispatchEvent(new Event("musicloud:session-expired"));
    }
    const fallback =
      response.status === 403
        ? "Your account does not have permission to do that."
        : `MusicLoud request failed (${response.status}).`;
    const message =
      payload?.error ||
      payload?.message ||
      formatDetails(payload?.details) ||
      fallback;
    throw new ApiError(
      `${message}${formatDetails(payload?.details) && payload?.error ? ` ${formatDetails(payload.details)}` : ""}`,
      response.status,
      payload?.details,
    );
  }

  return payload;
}

export function saveSession(authResponse) {
  if (!authResponse?.accessToken) {
    throw new ApiError("The server did not return an access token.", 0);
  }
  const session = {
    token: authResponse.accessToken,
    expiresAt: Date.now() + (Number(authResponse.expiresIn) || 86_400_000),
    user: {
      id: authResponse.userId,
      username: authResponse.username,
      fullName: authResponse.fullName,
      email: authResponse.email,
      role: authResponse.role,
    },
  };
  sessionStorage.setItem(SESSION_KEY, JSON.stringify(session));
  return session;
}

export function readSession() {
  try {
    const raw = sessionStorage.getItem(SESSION_KEY);
    if (!raw) return null;
    const session = JSON.parse(raw);
    if (!session.token || !session.expiresAt || session.expiresAt <= Date.now()) {
      sessionStorage.removeItem(SESSION_KEY);
      return null;
    }
    return session;
  } catch {
    sessionStorage.removeItem(SESSION_KEY);
    return null;
  }
}
