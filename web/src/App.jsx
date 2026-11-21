import { useCallback, useEffect, useMemo, useState } from "react";
import {
  ArrowDownRight,
  ArrowRight,
  AudioLines,
  BadgeCheck,
  Disc3,
  Headphones,
  LockKeyhole,
  Mail,
  Music2,
  Search,
  UserRound,
} from "lucide-react";
import { api, readSession, saveSession, SESSION_KEY } from "./api.js";

const genres = ["All", "Pop", "Rock", "Hip-Hop", "Love", "Classical", "Jazz", "Electronic"];

function displayError(error) {
  return error?.message || "Something went wrong. Please try again.";
}

function App() {
  const [session, setSession] = useState(readSession);
  const [authNotice, setAuthNotice] = useState("");

  useEffect(() => {
    const expire = () => {
      setSession(null);
      setAuthNotice("Your session expired. Please sign in again.");
    };
    window.addEventListener("musicloud:session-expired", expire);
    return () => window.removeEventListener("musicloud:session-expired", expire);
  }, []);

  useEffect(() => {
    if (!session) return undefined;
    const timer = window.setTimeout(
      () => {
        sessionStorage.removeItem(SESSION_KEY);
        setSession(null);
        setAuthNotice("Your session expired. Please sign in again.");
      },
      Math.max(0, session.expiresAt - Date.now()),
    );
    return () => window.clearTimeout(timer);
  }, [session]);

  async function signIn(credentials) {
    const result = await api("/api/auth/login", {
      method: "POST",
      auth: false,
      body: credentials,
    });
    setSession(saveSession(result));
  }

  async function registerAccount(details) {
    const result = await api("/api/auth/register", {
      method: "POST",
      auth: false,
      body: details,
    });
    setSession(saveSession(result));
  }

  function signOut() {
    sessionStorage.removeItem(SESSION_KEY);
    setSession(null);
    setAuthNotice("");
  }

  if (!session) {
    return (
      <AuthScreen
        notice={authNotice}
        onSignIn={signIn}
        onRegister={registerAccount}
      />
    );
  }

  return <MusicWorkspace session={session} onSignOut={signOut} />;
}

function Brand({ compact = false }) {
  return (
    <div className={`brand ${compact ? "brand-compact" : ""}`}>
      <span className="brand-mark" aria-hidden="true">
        <Music2 size={21} strokeWidth={2.2} />
      </span>
      <span className="brand-copy">
        <strong>MusicLoud</strong>
        {!compact && <small>LISTEN BEYOND THE ORDINARY</small>}
      </span>
    </div>
  );
}

function AuthScreen({ notice, onSignIn, onRegister }) {
  const [mode, setMode] = useState("login");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [values, setValues] = useState({
    firstName: "",
    lastName: "",
    email: "",
    username: "",
    usernameOrEmail: "",
    password: "",
    confirmPassword: "",
  });

  function update(event) {
    setValues((current) => ({ ...current, [event.target.name]: event.target.value }));
  }

  async function submit(event) {
    event.preventDefault();
    setError("");
    if (mode === "register") {
      if (values.password.length < 4 || values.password.length > 72) {
        setError("Password must be between 4 and 72 characters.");
        return;
      }
      if (values.password !== values.confirmPassword) {
        setError("Your passwords do not match.");
        return;
      }
      if (values.username.trim().length < 3 || values.username.trim().length > 30) {
        setError("Username must be between 3 and 30 characters.");
        return;
      }
    }

    setBusy(true);
    try {
      if (mode === "login") {
        await onSignIn({
          usernameOrEmail: values.usernameOrEmail.trim(),
          password: values.password,
        });
      } else {
        await onRegister({
          firstName: values.firstName.trim(),
          lastName: values.lastName.trim(),
          email: values.email.trim(),
          username: values.username.trim(),
          password: values.password,
        });
      }
    } catch (requestError) {
      setError(displayError(requestError));
      setBusy(false);
    }
  }

  function changeMode(next) {
    setError("");
    setMode(next);
  }

  return (
    <main className="auth-page">
      <section className="auth-art">
        <div className="art-glow art-glow-one" />
        <div className="art-glow art-glow-two" />
        <div className="auth-art-content">
          <Brand />
          <div className="art-copy">
            <span className="eyebrow"><span className="live-dot" /> YOUR PERSONAL MUSIC SPACE</span>
            <h1>Your sound.<br />Your space.</h1>
            <p>A little more music, a little less noise. Find the tracks that feel like you.</p>
            <div className="art-disc">
              <div className="disc-ring disc-ring-one" />
              <div className="disc-ring disc-ring-two" />
              <span className="disc-center"><Music2 size={32} /></span>
              <span className="orbit-note orbit-note-one"><AudioLines size={17} /></span>
              <span className="orbit-note orbit-note-two"><Music2 size={15} /></span>
            </div>
          </div>
          <div className="art-footer"><span>MADE FOR THE MOMENT</span><span>VOL. 01 — MUSICLOUD</span></div>
        </div>
      </section>

      <section className="auth-form-side">
        <div className="auth-form-wrap">
          <div className="mobile-brand"><Brand compact /></div>
          <div className="auth-heading">
            <span className="eyebrow">WELCOME TO MUSICLOUD</span>
            <h2>{mode === "login" ? "Welcome back" : "Create your account"}</h2>
            <p>{mode === "login" ? "Sign in to continue listening." : "Join MusicLoud and find your next favourite."}</p>
          </div>

          {(notice || error) && (
            <div className={`notice ${error ? "notice-error" : ""}`} role="alert">
              {error || notice}
            </div>
          )}

          <form onSubmit={submit} className="auth-form">
            {mode === "register" ? (
              <>
                <div className="field-grid">
                  <Field label="FIRST NAME" icon={UserRound}>
                    <input name="firstName" autoComplete="given-name" maxLength="50" value={values.firstName} onChange={update} required placeholder="First name" />
                  </Field>
                  <Field label="LAST NAME" icon={UserRound}>
                    <input name="lastName" autoComplete="family-name" maxLength="50" value={values.lastName} onChange={update} required placeholder="Last name" />
                  </Field>
                </div>
                <Field label="EMAIL ADDRESS" icon={Mail}>
                  <input name="email" type="email" autoComplete="email" value={values.email} onChange={update} required placeholder="you@example.com" />
                </Field>
                <Field label="USERNAME" icon={UserRound}>
                  <input name="username" autoComplete="username" minLength="3" maxLength="30" value={values.username} onChange={update} required placeholder="Choose a username" />
                </Field>
              </>
            ) : (
              <Field label="USERNAME OR EMAIL" icon={Mail}>
                <input name="usernameOrEmail" autoComplete="username" value={values.usernameOrEmail} onChange={update} required placeholder="Enter your username or email" />
              </Field>
            )}
            <Field label="PASSWORD" icon={LockKeyhole}>
              <input name="password" type="password" autoComplete={mode === "login" ? "current-password" : "new-password"} value={values.password} onChange={update} required minLength="4" maxLength="72" placeholder="Enter your password" />
            </Field>
            {mode === "register" && (
              <Field label="CONFIRM PASSWORD" icon={LockKeyhole}>
                <input name="confirmPassword" type="password" autoComplete="new-password" value={values.confirmPassword} onChange={update} required minLength="4" maxLength="72" placeholder="Repeat your password" />
              </Field>
            )}
            <button className="button button-primary auth-submit" disabled={busy} type="submit">
              {busy ? "One moment…" : mode === "login" ? "Sign in" : "Create account"}
              {!busy && <ArrowRight size={17} />}
            </button>
          </form>

          <div className="auth-switch">
            {mode === "login" ? (
              <>New to MusicLoud? <button type="button" onClick={() => changeMode("register")}>Create an account</button></>
            ) : (
              <>Already have an account? <button type="button" onClick={() => changeMode("login")}>Sign in</button></>
            )}
          </div>
          <p className="auth-footnote"><LockKeyhole size={13} /> Your account is secured by MusicLoud.</p>
        </div>
      </section>
    </main>
  );
}

function Field({ label, icon: Icon, children }) {
  return (
    <label className="field">
      <span className="field-label">{label}</span>
      <span className="field-control">
        {Icon && <Icon size={16} aria-hidden="true" />}
        {children}
      </span>
    </label>
  );
}

const navigation = [
  { id: "home", label: "Overview", icon: "home" },
  { id: "library", label: "Music library", icon: "library" },
  { id: "search", label: "Search music", icon: "search" },
  { id: "playlists", label: "Playlists", icon: "playlist" },
  { id: "profile", label: "My profile", icon: "profile" },
];

const navIcons = {
  home: Disc3,
  library: Music2,
  search: Search,
  playlist: AudioLines,
  profile: UserRound,
};

function MusicWorkspace({ session, onSignOut }) {
  const [page, setPage] = useState("home");
  const [songs, setSongs] = useState([]);
  const [genreSongs, setGenreSongs] = useState([]);
  const [searchResults, setSearchResults] = useState([]);
  const [playlists, setPlaylists] = useState([]);
  const [songLoading, setSongLoading] = useState(true);
  const [playlistLoading, setPlaylistLoading] = useState(true);
  const [pageError, setPageError] = useState("");
  const [genre, setGenre] = useState("All");
  const [query, setQuery] = useState("");
  const [headerSearch, setHeaderSearch] = useState("");
  const [selectedPlaylistId, setSelectedPlaylistId] = useState(null);
  const [songToAdd, setSongToAdd] = useState("");
  const [activeTrack, setActiveTrack] = useState(null);
  const [modal, setModal] = useState(null);
  const [toast, setToast] = useState(null);
  const [saving, setSaving] = useState(false);

  const isAdmin = session.user.role === "ADMIN";
  const userName = session.user.fullName || session.user.username || "Listener";
  const selectedPlaylist = playlists.find((item) => item.id === selectedPlaylistId) || playlists[0] || null;

  const loadSongs = useCallback(async () => {
    setSongLoading(true);
    setPageError("");
    try {
      const result = await api("/api/songs");
      if (!Array.isArray(result)) throw new Error("The server returned an invalid song list.");
      setSongs(result);
      setGenreSongs(result);
    } catch (error) {
      setPageError(displayError(error));
    } finally {
      setSongLoading(false);
    }
  }, []);

  const loadPlaylists = useCallback(async () => {
    setPlaylistLoading(true);
    try {
      const result = await api("/api/playlists");
      if (!Array.isArray(result)) throw new Error("The server returned an invalid playlist list.");
      setPlaylists(result);
      setSelectedPlaylistId((current) =>
        result.some((item) => item.id === current) ? current : result[0]?.id ?? null,
      );
    } catch (error) {
      setPageError(displayError(error));
    } finally {
      setPlaylistLoading(false);
    }
  }, []);

  useEffect(() => {
    loadSongs();
    loadPlaylists();
  }, [loadSongs, loadPlaylists]);

  useEffect(() => {
    if (!toast) return undefined;
    const timer = window.setTimeout(() => setToast(null), 4200);
    return () => window.clearTimeout(timer);
  }, [toast]);

  const librarySongs = genre === "All" ? songs : genreSongs;
  const latestSongs = useMemo(() => songs.slice(0, 4), [songs]);

  async function chooseGenre(nextGenre) {
    setGenre(nextGenre);
    setPageError("");
    if (nextGenre === "All") {
      setGenreSongs(songs);
      return;
    }
    try {
      const result = await api(`/api/songs/filter?genre=${encodeURIComponent(nextGenre)}`);
      setGenreSongs(Array.isArray(result) ? result : []);
    } catch (error) {
      setPageError(displayError(error));
    }
  }

  async function searchSongs(event) {
    event?.preventDefault();
    const term = query.trim();
    setPageError("");
    setSongLoading(true);
    try {
      const result = await api(`/api/songs/search?q=${encodeURIComponent(term)}`);
      setSearchResults(Array.isArray(result) ? result : []);
      setPage("search");
    } catch (error) {
      setPageError(displayError(error));
    } finally {
      setSongLoading(false);
    }
  }

  async function searchFromHeader(event) {
    event.preventDefault();
    setQuery(headerSearch);
    setPageError("");
    setSongLoading(true);
    try {
      const result = await api(`/api/songs/search?q=${encodeURIComponent(headerSearch.trim())}`);
      setSearchResults(Array.isArray(result) ? result : []);
      setPage("search");
    } catch (error) {
      setPageError(displayError(error));
    } finally {
      setSongLoading(false);
    }
  }

  async function saveSong(song) {
    setSaving(true);
    try {
      const isEditing = Boolean(song.id);
      await api(isEditing ? `/api/songs/${song.id}` : "/api/songs", {
        method: isEditing ? "PUT" : "POST",
        body: {
          title: song.title.trim(),
          artist: song.artist.trim(),
          genre: song.genre.trim(),
          releaseYear: Number(song.releaseYear),
          audioUrl: song.audioUrl.trim(),
          coverUrl: song.coverUrl.trim(),
        },
      });
      setModal(null);
      setToast({ type: "success", text: isEditing ? "Song details updated." : "Song added to the library." });
      await loadSongs();
      await loadPlaylists();
    } catch (error) {
      setToast({ type: "error", text: displayError(error) });
    } finally {
      setSaving(false);
    }
  }

  async function deleteSong(song) {
    if (!window.confirm(`Delete “${song.title}” from the library?`)) return;
    try {
      await api(`/api/songs/${song.id}`, { method: "DELETE" });
      setToast({ type: "success", text: "Song deleted." });
      await loadSongs();
      await loadPlaylists();
    } catch (error) {
      setToast({ type: "error", text: displayError(error) });
    }
  }

  async function createPlaylist(name) {
    setSaving(true);
    try {
      const result = await api("/api/playlists", {
        method: "POST",
        body: { name: name.trim() },
      });
      setPlaylists((current) => [...current, result]);
      setSelectedPlaylistId(result.id);
      setModal(null);
      setToast({ type: "success", text: "Your playlist is ready." });
    } catch (error) {
      setToast({ type: "error", text: displayError(error) });
    } finally {
      setSaving(false);
    }
  }

  async function deletePlaylist(playlist) {
    if (!window.confirm(`Delete the playlist “${playlist.name}”?`)) return;
    try {
      await api(`/api/playlists/${playlist.id}`, { method: "DELETE" });
      const remaining = playlists.filter((item) => item.id !== playlist.id);
      setPlaylists(remaining);
      setSelectedPlaylistId(remaining[0]?.id ?? null);
      setToast({ type: "success", text: "Playlist deleted." });
    } catch (error) {
      setToast({ type: "error", text: displayError(error) });
    }
  }

  async function addSongToPlaylist() {
    if (!selectedPlaylist || !songToAdd) return;
    try {
      const updated = await api(
        `/api/playlists/${selectedPlaylist.id}/songs/${songToAdd}`,
        { method: "POST", body: {} },
      );
      setPlaylists((current) =>
        current.map((item) => (item.id === updated.id ? updated : item)),
      );
      setToast({ type: "success", text: "Song added to your playlist." });
    } catch (error) {
      setToast({ type: "error", text: displayError(error) });
    }
  }

  async function removeSongFromPlaylist(playlist, song) {
    try {
      const updated = await api(
        `/api/playlists/${playlist.id}/songs/${song.id}`,
        { method: "DELETE" },
      );
      setPlaylists((current) =>
        current.map((item) => (item.id === updated.id ? updated : item)),
      );
      setToast({ type: "success", text: "Song removed from the playlist." });
    } catch (error) {
      setToast({ type: "error", text: displayError(error) });
    }
  }

  function navigate(nextPage) {
    setPage(nextPage);
    setPageError("");
  }

  return (
    <div className="workspace">
      <aside className="sidebar">
        <Brand />
        <div className="sidebar-user">
          <Avatar name={userName} />
          <span><strong>{userName}</strong><small>{isAdmin ? "Admin account" : "Listener account"}</small></span>
          <span className="online-dot" title="Connected" />
        </div>
        <span className="nav-caption">YOUR SPACE</span>
        <nav className="side-nav" aria-label="Main navigation">
          {navigation.map(({ id, label, icon }) => {
            const Icon = navIcons[icon];
            return (
              <button
                className={`nav-item ${page === id ? "active" : ""}`}
                key={id}
                onClick={() => navigate(id)}
                type="button"
              >
                <Icon size={18} />
                <span>{label}</span>
                {page === id && <span className="nav-indicator" />}
              </button>
            );
          })}
        </nav>
        <div className="sidebar-bottom">
          <div className="sidebar-note">
            <Headphones size={19} />
            <span><strong>Your sound, your rules.</strong><small>Make a little room for music.</small></span>
          </div>
          <button className="sign-out-button" onClick={onSignOut} type="button">
            <ArrowDownRight size={17} /> Sign out
          </button>
        </div>
      </aside>

      <div className="workspace-main">
        <header className="topbar">
          <div className="topbar-title">
            <span className="eyebrow">MUSICLOUD / {isAdmin ? "ADMIN SPACE" : "YOUR MUSIC SPACE"}</span>
            <strong>{pageTitle(page)}</strong>
          </div>
          <form className="header-search" onSubmit={searchFromHeader}>
            <Search size={17} />
            <input
              aria-label="Search songs, artists, and genres"
              onChange={(event) => setHeaderSearch(event.target.value)}
              placeholder="Search songs, artists, genres..."
              value={headerSearch}
            />
            <kbd>↵</kbd>
          </form>
          <button className="topbar-user" onClick={() => navigate("profile")} type="button">
            <Avatar name={userName} small />
            <span>{userName}</span>
          </button>
        </header>

        <main className="page-content">
          {pageError && <div className="notice notice-error page-notice" role="alert">{pageError}</div>}
          {page === "home" && (
            <DashboardPage
              userName={userName}
              isAdmin={isAdmin}
              songs={songs}
              playlists={playlists}
              loading={songLoading}
              onNavigate={navigate}
              onPlay={setActiveTrack}
              onEdit={(song) => setModal({ type: "song", song })}
            />
          )}
          {page === "library" && (
            <LibraryPage
              songs={librarySongs}
              allSongs={songs}
              genre={genre}
              loading={songLoading}
              isAdmin={isAdmin}
              onGenre={chooseGenre}
              onRefresh={loadSongs}
              onAdd={() => setModal({ type: "song" })}
              onEdit={(song) => setModal({ type: "song", song })}
              onDelete={deleteSong}
              onPlay={setActiveTrack}
            />
          )}
          {page === "search" && (
            <SearchPage
              query={query}
              onQuery={setQuery}
              onSearch={searchSongs}
              results={searchResults}
              loading={songLoading}
              onPlay={setActiveTrack}
            />
          )}
          {page === "playlists" && (
            <PlaylistsPage
              playlists={playlists}
              songs={songs}
              selected={selectedPlaylist}
              selectedId={selectedPlaylist?.id}
              loading={playlistLoading}
              songToAdd={songToAdd}
              onSelect={setSelectedPlaylistId}
              onSongToAdd={setSongToAdd}
              onCreate={() => setModal({ type: "playlist" })}
              onDelete={deletePlaylist}
              onAddSong={addSongToPlaylist}
              onRemoveSong={removeSongFromPlaylist}
              onPlay={setActiveTrack}
            />
          )}
          {page === "profile" && <ProfilePage user={session.user} />}
        </main>

        <footer className="workspace-footer">
          <span><span className="online-dot" /> Connected to MusicLoud</span>
          <span>YOUR SOUND. YOUR SPACE.</span>
        </footer>
      </div>

      {activeTrack && (
        <PlayerBar song={activeTrack} onClose={() => setActiveTrack(null)} />
      )}
      {modal?.type === "song" && (
        <SongModal
          song={modal.song}
          saving={saving}
          onClose={() => setModal(null)}
          onSave={saveSong}
        />
      )}
      {modal?.type === "playlist" && (
        <PlaylistModal
          saving={saving}
          onClose={() => setModal(null)}
          onSave={createPlaylist}
        />
      )}
      {toast && (
        <div className={`toast ${toast.type === "error" ? "toast-error" : ""}`} role="status">
          <span className="toast-mark">{toast.type === "error" ? "!" : "✓"}</span>
          {toast.text}
          <button aria-label="Dismiss notification" onClick={() => setToast(null)} type="button">×</button>
        </div>
      )}
    </div>
  );
}

function pageTitle(page) {
  return ({
    home: "Overview",
    library: "Music library",
    search: "Search music",
    playlists: "Your playlists",
    profile: "Your profile",
  })[page] || "MusicLoud";
}

function Avatar({ name, small = false }) {
  return <span className={`avatar ${small ? "avatar-small" : ""}`}>{name?.trim()?.[0]?.toUpperCase() || "M"}</span>;
}

function DashboardPage({ userName, isAdmin, songs, playlists, loading, onNavigate, onPlay, onEdit }) {
  const firstName = userName.split(" ")[0] || "Listener";
  return (
    <div className="dashboard-page">
      <div className="welcome-line">
        <div><h1>Good to see you, {firstName}<span className="wave"> ✦</span></h1>
          <p>{isAdmin ? "Your music library, ready for a little care." : "Find a track that matches your mood."}</p>
        </div>
        <span className="date-chip"><span className="live-dot" /> YOUR MUSIC SPACE</span>
      </div>
      <section className="hero-banner">
        <div className="hero-orb hero-orb-one" />
        <div className="hero-orb hero-orb-two" />
        <div className="hero-text">
          <span className="hero-pill"><AudioLines size={14} /> {isAdmin ? "MUSICLOUD / ADMIN SPACE" : "YOUR DAILY SOUNDTRACK"}</span>
          <h2>{isAdmin ? <>Keep the library<br />beautifully organised.</> : <>Press play.<br />Let the moment happen.</>}</h2>
          <p>{isAdmin ? "Add, edit and tidy every track in one place." : "Browse, search by mood and play your favourites."}</p>
          <button className="button button-light" onClick={() => onNavigate("library")} type="button">
            Explore songs <ArrowRight size={16} />
          </button>
        </div>
        <div className="hero-art" aria-hidden="true">
          <div className="hero-disc"><div /><span><Music2 size={30} /></span></div>
          <span className="floating-note note-a"><AudioLines size={20} /></span>
          <span className="floating-note note-b"><Music2 size={17} /></span>
          <span className="floating-star">✦</span>
        </div>
        <div className="hero-index">01 <span>/</span> 04</div>
      </section>

      <section className="stats-grid">
        <StatCard label="IN THE LIBRARY" value={loading ? "—" : songs.length} detail="Tracks to explore" icon={Music2} />
        <StatCard label="YOUR COLLECTIONS" value={playlists.length} detail="Playlists made yours" icon={AudioLines} />
        <StatCard label="YOUR ROLE" value={isAdmin ? "Admin" : "Listener"} detail="MusicLoud account" icon={BadgeCheck} />
      </section>

      <section className="section-block">
        <div className="section-heading">
          <div><span className="eyebrow">FRESH FROM THE LIBRARY</span><h2>Recently added</h2></div>
          <button className="text-button" onClick={() => onNavigate("library")} type="button">See all <ArrowRight size={15} /></button>
        </div>
        {loading ? <LoadingState label="Loading your library…" /> : songs.length ? (
          <div className="album-grid">
            {songs.slice(0, 4).map((song, index) => (
              <SongTile key={song.id} song={song} index={index} onPlay={onPlay} onEdit={isAdmin ? onEdit : undefined} />
            ))}
          </div>
        ) : <EmptyState title="Your library is waiting." detail="There are no songs in the shared catalog yet." />}
      </section>

      <section className="section-block quick-picks">
        <div className="section-heading">
          <div><span className="eyebrow">A PLACE TO BEGIN</span><h2>Find your next favourite</h2></div>
        </div>
        <div className="quick-grid">
          <button className="quick-card quick-card-purple" onClick={() => onNavigate("search")} type="button">
            <span className="quick-icon"><Search size={20} /></span><strong>Search the library</strong><span>Look up a title, artist or genre</span><ArrowRight size={17} />
          </button>
          <button className="quick-card quick-card-lilac" onClick={() => onNavigate("playlists")} type="button">
            <span className="quick-icon"><AudioLines size={20} /></span><strong>Make it a collection</strong><span>Build playlists for every mood</span><ArrowRight size={17} />
          </button>
          <button className="quick-card quick-card-rose" onClick={() => onNavigate("profile")} type="button">
            <span className="quick-icon"><UserRound size={20} /></span><strong>Your music profile</strong><span>See your MusicLoud account</span><ArrowRight size={17} />
          </button>
        </div>
      </section>
    </div>
  );
}

function StatCard({ label, value, detail, icon: Icon }) {
  return (
    <div className="stat-card">
      <div><span className="stat-label">{label}</span><strong className="stat-value">{value}</strong><span className="stat-detail">{detail}</span></div>
      <span className="stat-icon"><Icon size={19} /></span>
    </div>
  );
}

function LibraryPage({ songs, allSongs, genre, loading, isAdmin, onGenre, onRefresh, onAdd, onEdit, onDelete, onPlay }) {
  return (
    <div className="inner-page">
      <PageIntro eyebrow="THE MUSIC COLLECTION" title="Music library" detail="All the tracks stored in MusicLoud." action={
        isAdmin && <button className="button button-primary" onClick={onAdd} type="button"><span>＋</span> Add a song</button>
      } />
      <div className="library-toolbar">
        <div className="filter-chips" aria-label="Filter by genre">
          {genres.map((item) => (
            <button className={`filter-chip ${genre === item ? "selected" : ""}`} key={item} onClick={() => onGenre(item)} type="button">{item}</button>
          ))}
        </div>
        <button className="quiet-button" onClick={onRefresh} type="button"><span className="refresh-glyph">↻</span> Refresh</button>
      </div>
      <div className="library-count"><span>{songs.length} {songs.length === 1 ? "track" : "tracks"}</span><span>Sorted by recently added</span></div>
      {loading ? <LoadingState label="Finding your music…" /> : songs.length ? (
        <div className="song-list">
          {songs.map((song, index) => (
            <SongRow key={song.id} song={song} index={index} isAdmin={isAdmin} onPlay={onPlay} onEdit={onEdit} onDelete={onDelete} />
          ))}
        </div>
      ) : (
        <EmptyState
          title={allSongs.length ? "No tracks in this genre yet." : "Your library is waiting."}
          detail={allSongs.length ? "Choose another genre to keep listening." : isAdmin ? "Add the first track to your shared catalog." : "Ask your MusicLoud admin to add the first track."}
        />
      )}
    </div>
  );
}

function SearchPage({ query, onQuery, onSearch, results, loading, onPlay }) {
  return (
    <div className="inner-page">
      <PageIntro eyebrow="FIND YOUR NEXT FAVOURITE" title="Search music" detail="Look up a song, artist, genre, or year." />
      <form className="large-search" onSubmit={onSearch}>
        <Search size={19} />
        <input aria-label="Search the music library" onChange={(event) => onQuery(event.target.value)} placeholder="What are you in the mood for?" value={query} />
        <button className="button button-primary" type="submit">Search <ArrowRight size={16} /></button>
      </form>
      <div className="section-heading search-result-heading">
        <div><span className="eyebrow">YOUR DISCOVERY</span><h2>{query.trim() ? `Results for “${query.trim()}”` : "All songs"}</h2></div>
        {!loading && <span className="result-count">{results.length} found</span>}
      </div>
      {loading ? <LoadingState label="Searching the library…" /> : results.length ? (
        <div className="song-list">
          {results.map((song, index) => <SongRow key={song.id} song={song} index={index} onPlay={onPlay} />)}
        </div>
      ) : <EmptyState title="No matching songs." detail="Try another title, artist, or genre." />}
    </div>
  );
}

function PlaylistsPage({ playlists, songs, selected, selectedId, loading, songToAdd, onSelect, onSongToAdd, onCreate, onDelete, onAddSong, onRemoveSong, onPlay }) {
  return (
    <div className="inner-page">
      <PageIntro eyebrow="YOUR PERSONAL COLLECTIONS" title="Your playlists" detail="Make a collection, add tracks, and keep the music close." action={
        <button className="button button-primary" onClick={onCreate} type="button"><span>＋</span> New playlist</button>
      } />
      {loading ? <LoadingState label="Loading your playlists…" /> : (
        <div className="playlist-layout">
          <section className="playlist-column">
            <div className="subsection-title"><span>YOUR COLLECTIONS</span><span className="count-pill">{playlists.length}</span></div>
            {playlists.length ? playlists.map((playlist, index) => (
              <button
                className={`playlist-card ${selectedId === playlist.id ? "playlist-selected" : ""}`}
                key={playlist.id}
                onClick={() => onSelect(playlist.id)}
                type="button"
              >
                <span className={`playlist-art playlist-art-${index % 4}`}><AudioLines size={23} /></span>
                <span className="playlist-card-copy"><strong>{playlist.name}</strong><small>{playlist.songs?.length || 0} tracks</small></span>
                <ArrowRight size={17} />
              </button>
            )) : <EmptyState compact title="Your first playlist starts here." detail="Create a collection and add tracks from the library." />}
          </section>
          <section className="playlist-detail">
            {selected ? (
              <>
                <div className="playlist-detail-top">
                  <span className="playlist-art playlist-art-hero"><AudioLines size={30} /></span>
                  <div className="playlist-detail-title"><span className="eyebrow">MUSICLOUD PLAYLIST</span><h2>{selected.name}</h2><p>{selected.songs?.length || 0} tracks · Curated by you</p></div>
                  <button className="icon-button danger-hover" aria-label={`Delete ${selected.name}`} onClick={() => onDelete(selected)} title="Delete playlist" type="button">×</button>
                </div>
                <div className="add-track-bar">
                  <select aria-label="Choose a song to add" onChange={(event) => onSongToAdd(event.target.value)} value={songToAdd}>
                    <option value="">Choose a song to add…</option>
                    {songs.map((song) => <option key={song.id} value={song.id}>{song.title} — {song.artist}</option>)}
                  </select>
                  <button className="button button-secondary" disabled={!songToAdd} onClick={onAddSong} type="button">Add track <ArrowRight size={15} /></button>
                </div>
                <div className="playlist-track-list">
                  {selected.songs?.length ? selected.songs.map((song, index) => (
                    <SongRow
                      key={song.id}
                      song={song}
                      index={index}
                      onPlay={onPlay}
                      trailing={<button className="icon-button danger-hover" aria-label={`Remove ${song.title}`} onClick={() => onRemoveSong(selected, song)} title="Remove from playlist" type="button">×</button>}
                    />
                  )) : <EmptyState compact title="No tracks in this playlist yet." detail="Choose a song above to add it." />}
                </div>
              </>
            ) : (
              <div className="playlist-empty-detail"><span className="playlist-art playlist-art-hero"><AudioLines size={30} /></span><h2>Make room for a mood.</h2><p>Create a playlist and add your favourite tracks.</p><button className="button button-primary" onClick={onCreate} type="button">Create a playlist <ArrowRight size={16} /></button></div>
            )}
          </section>
        </div>
      )}
    </div>
  );
}

function ProfilePage({ user }) {
  const name = user.fullName || user.username || "MusicLoud listener";
  const role = user.role === "ADMIN" ? "Administrator" : "Listener";
  return (
    <div className="inner-page">
      <PageIntro eyebrow="YOUR MUSICLOUD ACCOUNT" title="Your profile" detail="The details attached to your MusicLoud account." />
      <section className="profile-card">
        <div className="profile-cover"><div className="profile-spark profile-spark-a">✦</div><div className="profile-spark profile-spark-b">♪</div><div className="profile-cover-disc"><Music2 size={38} /></div></div>
        <div className="profile-content">
          <Avatar name={name} />
          <span className="role-pill"><BadgeCheck size={14} /> {role}</span>
          <h2>{name}</h2>
          <p className="profile-handle">@{user.username || "listener"}</p>
          <div className="profile-info-grid">
            <div><span>EMAIL</span><strong>{user.email || "Not provided"}</strong></div>
            <div><span>USERNAME</span><strong>{user.username || "Not provided"}</strong></div>
            <div><span>ACCOUNT TYPE</span><strong>{role}</strong></div>
            <div><span>ACCOUNT ID</span><strong className="profile-id">{user.id || "—"}</strong></div>
          </div>
          <p className="profile-footnote"><LockKeyhole size={14} /> Profile editing is not available in the current MusicLoud API.</p>
        </div>
      </section>
    </div>
  );
}

function PageIntro({ eyebrow, title, detail, action }) {
  return (
    <div className="page-intro">
      <div><span className="eyebrow">{eyebrow}</span><h1>{title}</h1><p>{detail}</p></div>
      {action && <div className="page-intro-action">{action}</div>}
    </div>
  );
}

function SongTile({ song, index, onPlay, onEdit }) {
  const colors = ["cover-violet", "cover-indigo", "cover-pink", "cover-blue"];
  return (
    <article className="song-tile">
      <Cover song={song} className={colors[index % colors.length]} />
      <div className="song-tile-meta"><span className="song-genre">{song.genre || "Music"}</span><span>{song.releaseYear || "—"}</span></div>
      <strong title={song.title}>{song.title}</strong>
      <div className="song-tile-bottom"><span>{song.artist}</span><div>
        {song.audioUrl && <button className="mini-play" onClick={() => onPlay(song)} title="Play song" type="button"><ArrowRight size={14} /></button>}
        {onEdit && <button className="mini-edit" onClick={() => onEdit(song)} title="Edit song" type="button">Edit</button>}
      </div></div>
    </article>
  );
}

function SongRow({ song, index, isAdmin = false, onPlay, onEdit, onDelete, trailing }) {
  const colors = ["cover-violet", "cover-indigo", "cover-pink", "cover-blue"];
  return (
    <article className="song-row">
      <Cover song={song} className={colors[index % colors.length]} small />
      <div className="song-row-main"><strong title={song.title}>{song.title}</strong><span>{song.artist}</span></div>
      <span className="song-row-genre">{song.genre || "—"}</span>
      <span className="song-row-year">{song.releaseYear || "—"}</span>
      <div className="song-row-actions">
        {song.audioUrl && <button className="icon-button play-hover" onClick={() => onPlay(song)} title={`Play ${song.title}`} type="button"><ArrowRight size={16} /></button>}
        {isAdmin && onEdit && <button className="icon-button" onClick={() => onEdit(song)} title="Edit song" type="button"><span className="edit-glyph">✎</span></button>}
        {isAdmin && onDelete && <button className="icon-button danger-hover" onClick={() => onDelete(song)} title="Delete song" type="button">×</button>}
        {trailing}
      </div>
    </article>
  );
}

function Cover({ song, className = "", small = false }) {
  const [broken, setBroken] = useState(false);
  return (
    <span className={`cover-art ${small ? "cover-small" : ""} ${className}`}>
      {song.coverUrl && !broken ? (
        <img alt="" onError={() => setBroken(true)} src={song.coverUrl} />
      ) : (
        <><span className="cover-glow" /><Music2 size={small ? 20 : 29} /></>
      )}
    </span>
  );
}

function EmptyState({ title, detail, compact = false }) {
  return (
    <div className={`empty-state ${compact ? "empty-compact" : ""}`}>
      <span className="empty-icon"><Music2 size={21} /></span>
      <strong>{title}</strong>
      <p>{detail}</p>
    </div>
  );
}

function LoadingState({ label }) {
  return <div className="loading-state"><span className="spinner" /><span>{label}</span></div>;
}

function PlayerBar({ song, onClose }) {
  return (
    <div className="player-bar">
      <div className="player-track">
        <Cover song={song} className="cover-violet" small />
        <div><strong>{song.title}</strong><span>{song.artist}</span></div>
      </div>
      {song.audioUrl ? (
        <audio controls autoPlay key={song.id} src={song.audioUrl}>
          Your browser does not support audio playback.
        </audio>
      ) : (
        <span className="player-unavailable">No audio URL is available for this track.</span>
      )}
      <button className="icon-button" onClick={onClose} aria-label="Close player" type="button">×</button>
    </div>
  );
}

function SongModal({ song, saving, onClose, onSave }) {
  const [values, setValues] = useState({
    title: song?.title || "",
    artist: song?.artist || "",
    genre: song?.genre || "",
    releaseYear: song?.releaseYear || new Date().getFullYear(),
    audioUrl: song?.audioUrl || "",
    coverUrl: song?.coverUrl || "",
  });
  const [error, setError] = useState("");

  function update(event) {
    setValues((current) => ({ ...current, [event.target.name]: event.target.value }));
  }

  function submit(event) {
    event.preventDefault();
    const year = Number(values.releaseYear);
    if (!values.title.trim() || !values.artist.trim() || !values.genre.trim()) {
      setError("Title, artist, and genre are required.");
      return;
    }
    if (!Number.isInteger(year) || year < 1000 || year > 9999) {
      setError("Enter a valid release year.");
      return;
    }
    onSave({ ...values, id: song?.id, releaseYear: year });
  }

  return (
    <Modal title={song ? "Edit song" : "Add a song"} subtitle="Update the shared MusicLoud catalog." onClose={onClose}>
      <form className="modal-form" onSubmit={submit}>
        <Field label="SONG TITLE"><input autoFocus maxLength="160" name="title" onChange={update} required value={values.title} /></Field>
        <Field label="ARTIST"><input maxLength="160" name="artist" onChange={update} required value={values.artist} /></Field>
        <div className="field-grid">
          <Field label="GENRE"><input maxLength="80" name="genre" onChange={update} required value={values.genre} /></Field>
          <Field label="RELEASE YEAR"><input max="9999" min="1000" name="releaseYear" onChange={update} required type="number" value={values.releaseYear} /></Field>
        </div>
        <Field label="AUDIO URL (OPTIONAL)"><input name="audioUrl" onChange={update} placeholder="https://…" type="url" value={values.audioUrl} /></Field>
        <Field label="COVER IMAGE URL (OPTIONAL)"><input name="coverUrl" onChange={update} placeholder="https://…" type="url" value={values.coverUrl} /></Field>
        {error && <div className="notice notice-error" role="alert">{error}</div>}
        <div className="modal-actions">
          <button className="button button-secondary" onClick={onClose} type="button">Cancel</button>
          <button className="button button-primary" disabled={saving} type="submit">{saving ? "Saving…" : "Save song"} <ArrowRight size={15} /></button>
        </div>
      </form>
    </Modal>
  );
}

function PlaylistModal({ saving, onClose, onSave }) {
  const [name, setName] = useState("");
  const [error, setError] = useState("");

  function submit(event) {
    event.preventDefault();
    if (!name.trim()) {
      setError("Enter a name for your playlist.");
      return;
    }
    if (name.trim().length > 100) {
      setError("Playlist names can be up to 100 characters.");
      return;
    }
    onSave(name);
  }

  return (
    <Modal title="Create a playlist" subtitle="Give this collection a name that feels like you." onClose={onClose}>
      <form className="modal-form" onSubmit={submit}>
        <Field label="PLAYLIST NAME" icon={AudioLines}>
          <input autoFocus maxLength="100" onChange={(event) => setName(event.target.value)} placeholder="Late night favourites" value={name} />
        </Field>
        {error && <div className="notice notice-error" role="alert">{error}</div>}
        <div className="modal-actions">
          <button className="button button-secondary" onClick={onClose} type="button">Cancel</button>
          <button className="button button-primary" disabled={saving} type="submit">{saving ? "Creating…" : "Create playlist"} <ArrowRight size={15} /></button>
        </div>
      </form>
    </Modal>
  );
}

function Modal({ title, subtitle, onClose, children }) {
  useEffect(() => {
    function onKeyDown(event) {
      if (event.key === "Escape") onClose();
    }
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [onClose]);

  return (
    <div className="modal-backdrop" onMouseDown={(event) => event.target === event.currentTarget && onClose()}>
      <section aria-labelledby="modal-heading" aria-modal="true" className="modal-card" role="dialog">
        <div className="modal-heading"><div><span className="eyebrow">MUSICLOUD</span><h2 id="modal-heading">{title}</h2><p>{subtitle}</p></div><button className="icon-button" onClick={onClose} aria-label="Close dialog" type="button">×</button></div>
        {children}
      </section>
    </div>
  );
}

export default App;
