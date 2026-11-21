package gui;

import cloud.ApiClient;
import cloud.ApiClient.ApiResponse;
import cloud.CloudAuthService;
import cloud.CloudPlaylistService;
import cloud.CloudSongService;
import cloud.CloudStatusBanner;
import cloud.CloudUserService;
import code.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.JTextComponent;

/** Modern Java Swing interface for the existing MusicLoud project (purple theme). */
public class MusicLoudRedesign extends JFrame {

    // ------------------------------------------------------------------ palette
    private static final Color BG = new Color(13, 10, 22);
    private static final Color SIDEBAR = new Color(18, 14, 30);
    private static final Color TOPBAR = new Color(15, 12, 26);
    private static final Color CARD = new Color(28, 22, 44);
    private static final Color CARD_HOVER = new Color(40, 31, 62);
    private static final Color LINE = new Color(42, 33, 64);
    private static final Color PURPLE = new Color(167, 119, 255);
    private static final Color PURPLE_DARK = new Color(105, 64, 174);
    private static final Color VIOLET = new Color(124, 58, 237);
    private static final Color FUCHSIA = new Color(192, 74, 220);
    private static final Color BORDER = new Color(65, 49, 91);
    private static final Color TEXT = new Color(245, 245, 250);
    private static final Color MUTED = new Color(163, 166, 180);
    private static final Color LAVENDER = new Color(226, 204, 255);
    private static final String FONT = pickFont();

    private static final Color[][] COVERS = {
        {new Color(124, 58, 237), new Color(236, 72, 153)},
        {new Color(99, 102, 241), new Color(168, 85, 247)},
        {new Color(192, 38, 211), new Color(251, 146, 60)},
        {new Color(79, 70, 229), new Color(56, 189, 248)},
        {new Color(147, 51, 234), new Color(244, 114, 182)},
        {new Color(91, 33, 182), new Color(167, 139, 250)}
    };

    // ------------------------------------------------------------------ state
    private final SongsFileHandling songs = new SongsFileHandling();
    private final UserFileHandling users = new UserFileHandling();
    private final CardLayout pages = new CardLayout() {
        @Override
        public Dimension preferredLayoutSize(Container parent) {
            for (Component c : parent.getComponents()) {
                if (c.isVisible()) {
                    Insets in = parent.getInsets();
                    Dimension d = c.getPreferredSize();
                    return new Dimension(d.width + in.left + in.right, d.height + in.top + in.bottom);
                }
            }
            return super.preferredLayoutSize(parent);
        }
    };
    private final JPanel pageHost = new PageHost(pages);
    private final Map<String, Btn> nav = new LinkedHashMap<>();
    private boolean admin;
    private String displayName = "Listener";
    /** Song list for the open session. Cloud catalog when the API is up, otherwise the local file. */
    private ArrayList<SongsData> librarySongs;
    /** Admin account list for this session. Null means "read the local user file". */
    private ArrayList<User> accountList;
    private JTable songTable;
    private DefaultTableModel songModel;
    private JScrollPane pageScroll;
    private Input searchInput;
    private Runnable searchAction;

    // now-playing bar
    private SongsData nowPlaying;
    private File lastFile;
    private boolean playing;
    private Cover playerCover;
    private JLabel playerTitle, playerSub, playerStatus;
    private PlayDot playerButton;
    private Equalizer equalizer;

    public MusicLoudRedesign() {
        super("MusicLoud | Your music space");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1050, 700));
        setSize(1280, 820);
        setLocationRelativeTo(null);
        songs.createSongFile();
        users.createUserFile();
        new Thread(ApiClient::checkHealth, "cloud-health-check").start();
        showAuth();
    }

    private ArrayList<SongsData> currentSongs() {
        return librarySongs != null ? librarySongs : songs.getSongs();
    }

    private ArrayList<User> currentUsers() {
        return accountList != null ? accountList : users.getUsers();
    }

    // ================================================================== AUTH
    private void showAuth() {
        ApiClient.clearSession();
        admin = false;
        librarySongs = null;
        accountList = null;
        nowPlaying = null; lastFile = null; playing = false;
        RoundPanel form = new RoundPanel(CARD, 28);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(new EmptyBorder(40, 42, 40, 42));
        form.setPreferredSize(new Dimension(420, 500));
        Input username = new Input("Enter your username or email", Kind.USER);
        PassInput password = new PassInput("Enter your password");
        Btn signIn = new Btn("Sign in  →", Style.PRIMARY, null);
        Btn create = new Btn("Create an account", Style.SECONDARY, null);
        put(form, label("Welcome back", 28, TEXT, true));
        put(form, gap(8));
        put(form, label("Sign in to continue listening", 14, MUTED, false));
        put(form, gap(34));
        put(form, labeled("USERNAME / EMAIL", username));
        put(form, gap(18));
        put(form, labeled("PASSWORD", password));
        put(form, gap(28));
        put(form, wide(signIn));
        put(form, gap(12));
        put(form, wide(create));
        form.add(Box.createVerticalGlue());
        signIn.addActionListener(e -> {
            String u = username.getText().trim(), p = new String(password.getPassword());
            if (u.isEmpty() || p.isEmpty()) { message("Enter your username or email and password.", JOptionPane.WARNING_MESSAGE); return; }
            signIn.setEnabled(false);
            signIn.setText("Signing in…");
            CloudAuthService.loginAsync(u, p, (ok, err) -> SwingUtilities.invokeLater(() -> {
                if (!ok) {
                    signIn.setEnabled(true);
                    signIn.setText("Sign in  →");
                    message(err != null ? err : "Login details are incorrect. Please try again.", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                signIn.setText("Opening library…");
                applySession();
                enterApp();
            }));
        });
        create.addActionListener(e -> showRegister());
        setContentPane(authShell(form)); revalidate(); repaint();
    }

    private void showRegister() {
        RoundPanel form = new RoundPanel(CARD, 28);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(new EmptyBorder(30, 40, 30, 40));
        form.setPreferredSize(new Dimension(470, 600));
        Input first = new Input("First name", null), last = new Input("Last name", null);
        Input email = new Input("you@example.com", Kind.MAIL), user = new Input("Choose a username", Kind.USER);
        PassInput pass = new PassInput("Password"), confirm = new PassInput("Repeat password");
        put(form, label("Create your account", 28, TEXT, true));
        put(form, gap(8));
        put(form, label("Join MusicLoud and find your next favourite.", 13, MUTED, false));
        put(form, gap(24));
        put(form, pair(labeled("FIRST NAME", first), labeled("LAST NAME", last)));
        put(form, gap(14));
        put(form, labeled("EMAIL", email));
        put(form, gap(14));
        put(form, labeled("USERNAME", user));
        put(form, gap(14));
        put(form, pair(labeled("PASSWORD", pass), labeled("CONFIRM PASSWORD", confirm)));
        put(form, gap(24));
        Btn register = new Btn("Create account", Style.PRIMARY, null), back = new Btn("Back to sign in", Style.SECONDARY, null);
        put(form, wide(register));
        put(form, gap(10));
        put(form, wide(back));
        form.add(Box.createVerticalGlue());
        register.addActionListener(e -> {
            String f=first.getText().trim(), l=last.getText().trim(), em=email.getText().trim(), un=user.getText().trim(), pw=new String(pass.getPassword());
            if(f.isEmpty()||l.isEmpty()||em.isEmpty()||un.isEmpty()||pw.isEmpty()){message("Please complete every field.",JOptionPane.WARNING_MESSAGE);return;}
            if(!em.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")){message("Enter a valid email address.",JOptionPane.WARNING_MESSAGE);return;}
            if(pw.length()<4||!pw.equals(new String(confirm.getPassword()))){message("Passwords must match and contain at least 4 characters.",JOptionPane.WARNING_MESSAGE);return;}
            register.setEnabled(false);
            register.setText("Creating account…");
            CloudAuthService.registerAsync(f, l, em, un, pw, (ok, err) -> SwingUtilities.invokeLater(() -> {
                register.setEnabled(true);
                register.setText("Create account");
                if (!ok) { message(err != null ? err : "Registration failed. Please try again.", JOptionPane.WARNING_MESSAGE); return; }
                message(ApiClient.isCloudAvailable()
                        ? "Your account has been created on MusicLoud and saved for offline sign-in."
                        : "The server is offline. Your account was saved on this computer.", JOptionPane.INFORMATION_MESSAGE);
                showAuth();
            }));
        });
        back.addActionListener(e -> showAuth());
        setContentPane(authShell(form)); revalidate(); repaint();
    }

    /** Split screen used by sign-in and register: artwork on the left, form on the right. */
    private JPanel authShell(JComponent form) {
        JPanel root = new JPanel(new GridLayout(1, 2));
        root.setBackground(BG);
        Hero hero = new Hero(new Color(96, 48, 170), new Color(52, 29, 94), new Color(18, 13, 34), 0, 1);
        hero.setLayout(new BoxLayout(hero, BoxLayout.Y_AXIS));
        hero.setBorder(new EmptyBorder(56, 56, 56, 56));
        JPanel brand = new JPanel(); brand.setOpaque(false); brand.setLayout(new BoxLayout(brand, BoxLayout.X_AXIS));
        brand.add(new Disc(null, Kind.NOTE, 44)); brand.add(Box.createHorizontalStrut(14));
        brand.add(label("MUSICLOUD", 24, LAVENDER, true));
        put(hero, cap(brand));
        put(hero, gap(60));
        put(hero, label("Your sound.\nYour space.", 48, TEXT, true));
        put(hero, gap(18));
        put(hero, label("A little more music, a little less noise.", 16, new Color(211, 202, 230), false));
        hero.add(Box.createVerticalGlue());
        JPanel formWrap = new JPanel(new GridBagLayout());
        formWrap.setBackground(BG);
        formWrap.add(form);
        root.add(hero); root.add(formWrap);
        return root;
    }

    // ================================================================== DASHBOARD
    private void showDashboard() { showDashboard("home"); }

    private void showDashboard(String openPage) {
        JPanel shell = new JPanel(new BorderLayout()); shell.setBackground(BG);

        // ---- sidebar
        JPanel side = new JPanel(); side.setBackground(SIDEBAR); side.setPreferredSize(new Dimension(250, 0));
        side.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, LINE), new EmptyBorder(26, 16, 20, 16)));
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        JPanel logo = new JPanel(); logo.setOpaque(false); logo.setLayout(new BoxLayout(logo, BoxLayout.X_AXIS));
        logo.add(new Disc(null, Kind.NOTE, 40)); logo.add(Box.createHorizontalStrut(12));
        JPanel logoText = new JPanel(); logoText.setOpaque(false); logoText.setLayout(new BoxLayout(logoText, BoxLayout.Y_AXIS));
        logoText.add(label("MusicLoud", 22, LAVENDER, true)); logoText.add(label("LISTEN BEYOND THE ORDINARY", 8, MUTED, true));
        logo.add(logoText);
        put(side, cap(logo)); put(side, gap(26));
        RoundPanel chip = new RoundPanel(new Color(255, 255, 255, 14), 22);
        chip.setLayout(new BoxLayout(chip, BoxLayout.X_AXIS)); chip.setBorder(new EmptyBorder(10, 12, 10, 12));
        chip.add(new Disc(displayName.substring(0, 1).toUpperCase(), null, 38)); chip.add(Box.createHorizontalStrut(11));
        JPanel who = new JPanel(); who.setOpaque(false); who.setLayout(new BoxLayout(who, BoxLayout.Y_AXIS));
        who.add(label(displayName, 13, TEXT, true)); who.add(label(admin ? "Admin account" : "Listener account", 11, MUTED, false));
        chip.add(who);
        chip.setMaximumSize(new Dimension(Integer.MAX_VALUE, 62));
        put(side, chip); put(side, gap(30));
        put(side, label("MENU", 10, MUTED, true)); put(side, gap(12));
        nav.clear();
        addNav(side, "Overview", Kind.HOME, "home");
        addNav(side, "Music library", Kind.LIBRARY, "library");
        addNav(side, "Search music", Kind.SEARCH, "search");
        if (!admin) { addNav(side, "Recommendations", Kind.STAR, "recommend"); addNav(side, "Playlists", Kind.LIST, "playlists"); }
        if (admin) addNav(side, "Registered users", Kind.USER, "users");
        side.add(Box.createVerticalGlue());
        Btn logout = new Btn("Log out", Style.GHOST, Kind.LOGOUT);
        logout.setHorizontalAlignment(SwingConstants.LEFT);
        logout.addActionListener(e -> showAuth());
        put(side, wide(logout));

        // ---- pages
        pageHost.removeAll();
        pageHost.add(homePage(), "home"); pageHost.add(libraryPage(), "library"); pageHost.add(searchPage(), "search");
        pageHost.add(admin ? usersPage() : recommendPage(), admin ? "users" : "recommend");
        if (!admin) pageHost.add(playlistsPage(), "playlists");

        // ---- top bar
        JPanel workspace = new JPanel(new BorderLayout()); workspace.setBackground(BG);
        JPanel topbar = new JPanel(new BorderLayout(20, 0)); topbar.setBackground(TOPBAR);
        topbar.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, LINE), new EmptyBorder(14, 30, 14, 30)));
        Input top = new Input("Search songs, artists, genres…", Kind.SEARCH);
        top.addActionListener(e -> { searchInput.setText(top.getText().trim()); searchAction.run(); navigate("search"); });
        JPanel topLeft = new JPanel(new BorderLayout()); topLeft.setOpaque(false); topLeft.setPreferredSize(new Dimension(440, 46)); topLeft.add(top);
        JPanel topRight = new JPanel(); topRight.setOpaque(false); topRight.setLayout(new BoxLayout(topRight, BoxLayout.X_AXIS));
        RoundPanel tag = new RoundPanel(new Color(167, 119, 255, 36), 30); tag.setBorder(new EmptyBorder(7, 14, 7, 14)); tag.setLayout(new BorderLayout());
        tag.add(label(admin ? "ADMIN WORKSPACE" : "YOUR MUSIC SPACE", 10, new Color(200, 176, 240), true));
        topRight.add(tag); topRight.add(Box.createHorizontalStrut(16));
        topRight.add(new Disc(displayName.substring(0, 1).toUpperCase(), null, 36)); topRight.add(Box.createHorizontalStrut(10));
        topRight.add(label(displayName, 13, TEXT, true));
        JPanel topRightWrap = new JPanel(new GridBagLayout()); topRightWrap.setOpaque(false); topRightWrap.add(topRight);
        topbar.add(topLeft, BorderLayout.WEST); topbar.add(topRightWrap, BorderLayout.EAST);

        pageScroll = new JScrollPane(pageHost);
        pageScroll.setBorder(BorderFactory.createEmptyBorder()); pageScroll.setBackground(BG); pageScroll.getViewport().setBackground(BG);
        pageScroll.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        pageScroll.getVerticalScrollBar().setUnitIncrement(18);
        slimBars(pageScroll);
        JPanel north = new JPanel(new BorderLayout());
        north.setOpaque(false);
        north.add(CloudStatusBanner.createBanner(), BorderLayout.NORTH);
        north.add(topbar, BorderLayout.CENTER);
        workspace.add(north, BorderLayout.NORTH); workspace.add(pageScroll, BorderLayout.CENTER);
        shell.add(side, BorderLayout.WEST); shell.add(workspace, BorderLayout.CENTER);
        if (!admin) shell.add(playerBar(), BorderLayout.SOUTH);
        setContentPane(shell); navigate(openPage); revalidate(); repaint();
    }

    private void addNav(JPanel side, String text, Kind icon, String key) {
        Btn b = new Btn(text, Style.NAV, icon);
        b.addActionListener(e -> navigate(key));
        nav.put(key, b); put(side, b); put(side, gap(6));
    }

    private void navigate(String key) {
        pages.show(pageHost, key);
        pageHost.revalidate(); pageHost.repaint();
        for (Map.Entry<String, Btn> e : nav.entrySet()) e.getValue().setActive(e.getKey().equals(key));
        if (pageScroll != null) pageScroll.getVerticalScrollBar().setValue(0);
    }

    // ================================================================== PAGES
    private JPanel homePage() {
        JPanel main = new JPanel(); main.setOpaque(false); main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
        put(main, label(greeting() + ", " + displayName, 30, TEXT, true)); put(main, gap(6));
        put(main, label(admin ? "Manage your music library from one place." : "Find a track that matches your mood.", 14, MUTED, false)); put(main, gap(22));
        Hero hero = hero(admin ? "MUSICLOUD  /  ADMIN SPACE" : "YOUR DAILY SOUNDTRACK",
                admin ? "Keep the library\nbeautifully organised." : "Press play.\nLet the moment happen.",
                admin ? "Add, edit and tidy every track in one place." : "Browse, search by mood and play your favourites.",
                admin ? "Manage songs  →" : "Explore songs", admin ? null : Kind.PLAY, "library");
        put(main, hero); put(main, gap(28));
        put(main, sectionHeader("Recently added", "See all", () -> navigate("library"))); put(main, gap(14));
        ArrayList<SongsData> all = currentSongs();
        if (all.isEmpty()) put(main, empty("Your music library is waiting for its first song."));
        else {
            JPanel cards = new JPanel(new GridLayout(1, 4, 16, 0)); cards.setOpaque(false);
            int shown = 0;
            for (int i = all.size() - 1; i >= 0 && shown < 4; i--, shown++) cards.add(albumCard(all.get(i)));
            for (; shown < 4; shown++) { JPanel blank = new JPanel(); blank.setOpaque(false); cards.add(blank); }
            put(main, cap(cards));
        }
        put(main, gap(30));
        put(main, label("AT A GLANCE", 12, MUTED, true)); put(main, gap(12));
        JPanel stats = new JPanel(new GridLayout(1, 3, 14, 0)); stats.setOpaque(false);
        stats.add(stat("SONGS", String.valueOf(currentSongs().size()), "In your library", Kind.NOTE));
        stats.add(stat("USERS", String.valueOf(currentUsers().size()), "Registered listeners", Kind.USER));
        stats.add(stat("YOUR ROLE", admin ? "Admin" : "Listener", "MusicLoud account", Kind.STAR));
        put(main, cap(stats));
        // side column: quick picks
        RoundPanel picks = new RoundPanel(CARD, 24); picks.setLayout(new BoxLayout(picks, BoxLayout.Y_AXIS)); picks.setBorder(new EmptyBorder(22, 20, 18, 20));
        put(picks, label("Quick picks", 19, TEXT, true)); put(picks, gap(4));
        put(picks, label("From your library", 12, MUTED, false)); put(picks, gap(14));
        for (int i = 0; i < Math.min(5, all.size()); i++) { put(picks, rankRow(i + 1, all.get(i))); put(picks, gap(4)); }
        if (all.isEmpty()) put(picks, label("Nothing here yet.", 13, MUTED, false));
        JPanel sideWrap = new JPanel(new BorderLayout()); sideWrap.setOpaque(false); sideWrap.setPreferredSize(new Dimension(300, 10)); sideWrap.add(picks, BorderLayout.NORTH);
        JPanel p = new JPanel(new BorderLayout(24, 0)); p.setBackground(BG); p.setBorder(new EmptyBorder(30, 36, 30, 36));
        p.add(main, BorderLayout.CENTER); p.add(sideWrap, BorderLayout.EAST);
        return p;
    }

    private JPanel libraryPage() {
        JPanel p = content();
        JPanel head = new JPanel(new BorderLayout()); head.setOpaque(false);
        JPanel titles = new JPanel(); titles.setOpaque(false); titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.add(label("Music library", 30, TEXT, true)); titles.add(gap(6)); titles.add(label("All the tracks stored in MusicLoud.", 14, MUTED, false));
        head.add(titles, BorderLayout.WEST);
        if (admin) {
            Btn add = new Btn("Add a song", Style.PRIMARY, Kind.PLUS);
            add.addActionListener(e -> songDialog(null));
            JPanel addWrap = new JPanel(new GridBagLayout()); addWrap.setOpaque(false); addWrap.add(add);
            head.add(addWrap, BorderLayout.EAST);
        }
        put(p, cap(head)); put(p, gap(22));
        songModel = new DefaultTableModel(new Object[]{"ID", "SONG", "ARTIST", "GENRE", "YEAR"}, 0) { public boolean isCellEditable(int r, int c) { return false; } };
        songTable = new JTable(songModel); styleTable(songTable); refreshSongs();
        songTable.getColumnModel().getColumn(0).setMaxWidth(70);
        songTable.getColumnModel().getColumn(4).setMaxWidth(90);
        put(p, tableCard(songTable, 420));
        if (admin) {
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); actions.setOpaque(false); actions.setBorder(new EmptyBorder(16, 0, 0, 0));
            Btn edit = new Btn("Edit selected", Style.SECONDARY, null), del = new Btn("Delete selected", Style.SECONDARY, null);
            edit.addActionListener(e -> { int r = songTable.getSelectedRow(); if (r >= 0) songDialog(findSong((int) songModel.getValueAt(r, 0))); else message("Select a song first.", JOptionPane.INFORMATION_MESSAGE); });
            del.addActionListener(e -> deleteSelected());
            actions.add(edit); actions.add(Box.createHorizontalStrut(10)); actions.add(del); put(p, cap(actions));
        }
        return p;
    }

    private JPanel searchPage() {
        JPanel p = content();
        put(p, label("Search music", 30, TEXT, true)); put(p, gap(8));
        put(p, label("Search by song title, artist, genre, year or ID.", 14, MUTED, false)); put(p, gap(22));
        Input q = new Input("Type something to search...", Kind.SEARCH);
        Btn go = new Btn("Search", Style.PRIMARY, null);
        JPanel bar = new JPanel(new BorderLayout(12, 0)); bar.setOpaque(false); bar.add(q, BorderLayout.CENTER); bar.add(go, BorderLayout.EAST);
        put(p, cap(bar)); put(p, gap(22));
        JPanel results = new JPanel(); results.setOpaque(false); results.setLayout(new BoxLayout(results, BoxLayout.Y_AXIS));
        put(p, results);
        Runnable search = () -> {
            results.removeAll(); String term = q.getText().trim().toLowerCase();
            for (SongsData s : currentSongs())
                if (term.isEmpty() || containsText(s.getSongName(), term) || containsText(s.getArtistName(), term) || containsText(s.getGenre(), term) || String.valueOf(s.getYear()).contains(term) || String.valueOf(s.getId()).equals(term)) {
                    put(results, songLine(s, 0)); put(results, gap(8));
                }
            if (results.getComponentCount() == 0) put(results, empty("No matching songs found."));
            results.revalidate(); results.repaint();
        };
        go.addActionListener(e -> search.run()); q.addActionListener(e -> search.run());
        searchInput = q; searchAction = search;
        search.run();
        return p;
    }

    private JPanel recommendPage() {
        JPanel p = content();
        put(p, label("Made for your mood", 30, TEXT, true)); put(p, gap(8));
        put(p, label("Choose a genre to discover matching songs.", 14, MUTED, false)); put(p, gap(22));
        String[] genres = {"All", "Pop", "Rock", "Hip-Hop", "Love", "Classical", "Jazz", "Electronic"};
        JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); chips.setOpaque(false);
        JPanel list = new JPanel(); list.setOpaque(false); list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        String[] picked = {"All"};
        ArrayList<Btn> group = new ArrayList<>();
        Runnable fill = () -> {
            list.removeAll(); String g = picked[0];
            for (SongsData s : currentSongs()) if (g.equals("All") || (s.getGenre() != null && s.getGenre().equalsIgnoreCase(g))) { put(list, songLine(s, 0)); put(list, gap(8)); }
            if (list.getComponentCount() == 0) put(list, empty("No songs found for this genre yet."));
            list.revalidate(); list.repaint();
        };
        for (String g : genres) {
            Btn b = new Btn(g, Style.CHIP, null); b.setActive(g.equals("All"));
            b.addActionListener(e -> { picked[0] = g; for (Btn x : group) x.setActive(x == b); fill.run(); });
            group.add(b); chips.add(b); chips.add(Box.createHorizontalStrut(8));
        }
        put(p, cap(chips)); put(p, gap(22)); put(p, list);
        fill.run();
        return p;
    }

    private JPanel usersPage() {
        JPanel p = content();
        put(p, label("Registered users", 30, TEXT, true)); put(p, gap(8));
        put(p, label("Accounts registered with MusicLoud.", 14, MUTED, false)); put(p, gap(22));
        DefaultTableModel model = new DefaultTableModel(new Object[]{"USERNAME", "NAME", "EMAIL"}, 0) { public boolean isCellEditable(int r, int c) { return false; } };
        for (User u : currentUsers()) model.addRow(new Object[]{u.getUserName(), u.getFullName(), u.getEmail()});
        JTable table = new JTable(model); styleTable(table);
        put(p, tableCard(table, 400));
        Btn remove = new Btn("Remove selected user", Style.SECONDARY, null);
        remove.addActionListener(e -> {
            int r = table.getSelectedRow();
            if (r < 0) { message("Select a user first.", JOptionPane.INFORMATION_MESSAGE); return; }
            String name = (String) model.getValueAt(r, 0);
            if (!confirm("Delete account '" + name + "'?")) return;
            remove.setEnabled(false);
            CloudUserService.deleteAsync(name, (ok, err) -> SwingUtilities.invokeLater(() -> {
                remove.setEnabled(true);
                if (!ok) { message(err != null ? err : "Could not delete that account.", JOptionPane.WARNING_MESSAGE); return; }
                if (accountList != null) accountList.removeIf(u -> u.getUserName().equalsIgnoreCase(name));
                model.removeRow(r);
            }));
        });
        put(p, gap(16)); put(p, remove);
        return p;
    }

    private JPanel playlistsPage() {
        JPanel p = content();
        put(p, label("Your playlists", 30, TEXT, true)); put(p, gap(8));
        put(p, label("Create collections and add songs from the shared MusicLoud library.", 14, MUTED, false)); put(p, gap(20));
        DefaultTableModel model = new DefaultTableModel(new Object[]{"PLAYLIST", "TRACKS", "ID"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable table = new JTable(model);
        styleTable(table);
        table.removeColumn(table.getColumnModel().getColumn(2));
        put(p, tableCard(table, 310));

        JComboBox<SongsData> songChoice = new JComboBox<>(currentSongs().toArray(new SongsData[0]));
        songChoice.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value instanceof SongsData) {
                    SongsData song = (SongsData) value;
                    setText(song.getSongName() + " — " + song.getArtistName());
                }
                return this;
            }
        });
        songChoice.setPreferredSize(new Dimension(300, 38));

        Btn create = new Btn("Create playlist", Style.PRIMARY, Kind.PLUS);
        Btn addSong = new Btn("Add selected song", Style.SECONDARY, null);
        Btn delete = new Btn("Delete selected", Style.SECONDARY, null);
        Btn refresh = new Btn("Refresh", Style.GHOST, null);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actions.setOpaque(false);
        actions.add(create);
        actions.add(songChoice);
        actions.add(addSong);
        actions.add(delete);
        actions.add(refresh);
        put(p, gap(14)); put(p, cap(actions));

        Runnable load = () -> {
            refresh.setEnabled(false);
            CloudPlaylistService.loadPlaylistsAsync((playlists, error) -> SwingUtilities.invokeLater(() -> {
                refresh.setEnabled(true);
                if (error != null) {
                    message(error, JOptionPane.WARNING_MESSAGE);
                    return;
                }
                model.setRowCount(0);
                for (CloudPlaylistService.Playlist playlist : playlists) {
                    model.addRow(new Object[]{playlist.getName(), playlist.getSongCount(), playlist.getId()});
                }
            }));
        };
        create.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(this, "Playlist name:", "Create playlist", JOptionPane.PLAIN_MESSAGE);
            if (name == null) return;
            name = name.trim();
            if (name.isEmpty()) {
                message("Enter a playlist name.", JOptionPane.WARNING_MESSAGE);
                return;
            }
            create.setEnabled(false);
            CloudPlaylistService.createPlaylistAsync(name, (playlist, error) -> SwingUtilities.invokeLater(() -> {
                create.setEnabled(true);
                if (error != null) {
                    message(error, JOptionPane.WARNING_MESSAGE);
                    return;
                }
                load.run();
            }));
        });
        addSong.addActionListener(e -> {
            int row = table.getSelectedRow();
            SongsData song = (SongsData) songChoice.getSelectedItem();
            if (row < 0 || song == null) {
                message("Select a playlist and a song first.", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            Long playlistId = (Long) model.getValueAt(table.convertRowIndexToModel(row), 2);
            addSong.setEnabled(false);
            CloudPlaylistService.addSongToPlaylistAsync(playlistId, (long) song.getId(), (ok, error) ->
                    SwingUtilities.invokeLater(() -> {
                        addSong.setEnabled(true);
                        if (!ok) {
                            message(error != null ? error : "Could not add that song.", JOptionPane.WARNING_MESSAGE);
                            return;
                        }
                        load.run();
                    }));
        });
        delete.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                message("Select a playlist first.", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            int modelRow = table.convertRowIndexToModel(row);
            Long playlistId = (Long) model.getValueAt(modelRow, 2);
            String playlistName = String.valueOf(model.getValueAt(modelRow, 0));
            if (!confirm("Delete playlist '" + playlistName + "'?")) return;
            delete.setEnabled(false);
            CloudPlaylistService.deletePlaylistAsync(playlistId, (ok, error) -> SwingUtilities.invokeLater(() -> {
                delete.setEnabled(true);
                if (!ok) {
                    message(error != null ? error : "Could not delete that playlist.", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                load.run();
            }));
        });
        refresh.addActionListener(e -> load.run());
        load.run();
        return p;
    }

    /** Large gradient banner used on the overview and playlists pages. */
    private Hero hero(String tag, String title, String sub, String buttonText, Kind buttonIcon, String targetPage) {
        Hero hero = new Hero(new Color(106, 52, 190), new Color(184, 66, 205), new Color(244, 114, 140), 28, 0);
        hero.setLayout(new BorderLayout()); hero.setBorder(new EmptyBorder(28, 36, 28, 36));
        hero.setPreferredSize(new Dimension(700, 285)); hero.setMaximumSize(new Dimension(Integer.MAX_VALUE, 285));
        JPanel left = new JPanel(); left.setOpaque(false); left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        RoundPanel pill = new RoundPanel(new Color(255, 255, 255, 40), 30); pill.setLayout(new BorderLayout()); pill.setBorder(new EmptyBorder(6, 14, 6, 14));
        pill.add(label(tag, 10, new Color(255, 240, 255), true)); pill.setMaximumSize(pill.getPreferredSize());
        put(left, pill); put(left, gap(16));
        put(left, label(title, 34, Color.WHITE, true)); put(left, gap(12));
        put(left, label(sub, 13, new Color(246, 232, 255), false)); put(left, gap(20));
        Btn action = new Btn(buttonText, Style.WHITE, buttonIcon);
        action.addActionListener(e -> {
            if (targetPage != null) navigate(targetPage);
        });
        put(left, action);
        hero.add(left, BorderLayout.CENTER);
        return hero;
    }

    // ================================================================== SONG ACTIONS
    private void songDialog(SongsData old) {
        Input title = new Input("Song title", null), artist = new Input("Artist name", null), genre = new Input("Genre", null), year = new Input("Release year", null);
        if (old != null) { title.setText(old.getSongName()); artist.setText(old.getArtistName()); genre.setText(old.getGenre()); year.setText(String.valueOf(old.getYear())); }
        JPanel f = new JPanel(); f.setOpaque(false); f.setLayout(new BoxLayout(f, BoxLayout.Y_AXIS));
        put(f, labeled("SONG TITLE", title)); put(f, gap(12)); put(f, labeled("ARTIST", artist)); put(f, gap(12));
        put(f, pair(labeled("GENRE", genre), labeled("RELEASE YEAR", year)));
        f.setPreferredSize(new Dimension(440, 250));
        if (!showDialog(old == null ? "Add a song" : "Edit song", old == null ? "Add a song" : "Edit song", "Fill in the details below.", f, "Save", "Cancel")) return;
        try {
            if (title.getText().isBlank() || artist.getText().isBlank() || genre.getText().isBlank()) throw new IllegalArgumentException();
            int y = Integer.parseInt(year.getText().trim());
            String titleText = title.getText().trim(), artistText = artist.getText().trim(), genreText = genre.getText().trim();
            if (old == null) {
                CloudSongService.addSongAsync(titleText, artistText, genreText, y, (ok, msg) -> SwingUtilities.invokeLater(() -> {
                    if (!ok) {
                        message(msg != null ? msg : "Could not save this song.", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    afterSongWrite(msg == null, msg == null ? "Song details saved." : msg);
                }));
            } else {
                SongsData edited = new SongsData();
                edited.setId(old.getId());
                edited.setSongName(titleText);
                edited.setArtistName(artistText);
                edited.setGenre(genreText);
                edited.setYear(y);
                edited.setAudioUrl(old.getAudioUrl());
                edited.setCoverUrl(old.getCoverUrl());
                CloudSongService.editSongAsync(edited, (ok, msg) -> SwingUtilities.invokeLater(() -> {
                    if (!ok) {
                        message(msg != null ? msg : "Could not save this song.", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    afterSongWrite(msg == null, msg == null ? "Song details saved." : msg);
                }));
            }
            return;
        } catch (Exception ex) { message("Enter a title, artist, genre and a valid year.", JOptionPane.WARNING_MESSAGE); }
    }

    private void deleteSelected() {
        int r = songTable.getSelectedRow();
        if (r < 0) { message("Select a song first.", JOptionPane.INFORMATION_MESSAGE); return; }
        int id = (int) songModel.getValueAt(r, 0);
        if (confirm("Delete this song from the library?")) {
            CloudSongService.deleteSongAsync(id, (ok, err) -> SwingUtilities.invokeLater(() -> {
                if (!ok) {
                    message(err != null ? err : "Could not delete this song.", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                afterSongWrite(err == null, err);
            }));
        }
    }

    private SongsData findSong(int id) { for (SongsData s : currentSongs()) if (s.getId() == id) return s; return null; }

    private void refreshSongs() {
        if (songModel == null) return;
        songModel.setRowCount(0);
        for (SongsData s : currentSongs()) songModel.addRow(new Object[]{s.getId(), s.getSongName(), s.getArtistName(), s.getGenre(), s.getYear()});
    }

    private void applySession() {
        admin = ApiClient.isAdmin();
        String name = ApiClient.getCurrentFullName();
        if (name == null || name.isBlank()) name = ApiClient.getCurrentUsername();
        if (name == null || name.isBlank()) name = admin ? "Administrator" : "Listener";
        displayName = name;
        PanelLoginAndRegister.loggedInUserFullName = displayName;
        PanelLoginAndRegister.userN = ApiClient.getCurrentUsername();
    }

    /** Pull the cloud catalog, upload this computer's missing songs when an admin is signed in, then open the app. */
    private void enterApp() {
        new Thread(() -> {
            syncLibrary();
            if (admin && ApiClient.getAccessToken() != null) accountList = loadUsersBlocking();
            SwingUtilities.invokeLater(() -> showDashboard("home"));
        }, "library-sync").start();
    }

    private void afterSongWrite(boolean reloadCloud, String notice) {
        new Thread(() -> {
            if (reloadCloud && ApiClient.isCloudAvailable()) {
                ArrayList<SongsData> remote = loadCloudSongs();
                if (remote != null) {
                    librarySongs = remote;
                    if (admin && ApiClient.getAccessToken() != null) songs.saveSongsData(new ArrayList<>(remote));
                } else librarySongs = songs.getSongs();
            } else librarySongs = songs.getSongs();
            SwingUtilities.invokeLater(() -> {
                showDashboard("library");
                if (notice != null) message(notice, JOptionPane.INFORMATION_MESSAGE);
            });
        }, "song-reload").start();
    }

    private void syncLibrary() {
        ArrayList<SongsData> local = songs.getSongs();
        if (!ApiClient.isCloudAvailable()) { librarySongs = local; return; }
        ArrayList<SongsData> remote = loadCloudSongs();
        if (!ApiClient.isCloudAvailable() || remote == null) { librarySongs = local; return; }
        if (!admin || ApiClient.getAccessToken() == null) {
            librarySongs = remote.isEmpty() ? local : remote;
            return;
        }
        for (SongsData s : local) {
            if (!catalogHas(remote, s)) ApiClient.postAuth("/api/songs", songJson(s));
        }
        ArrayList<SongsData> refreshed = loadCloudSongs();
        if (refreshed != null && !refreshed.isEmpty()) {
            remote = refreshed;
            songs.saveSongsData(new ArrayList<>(remote));
        } else if (remote.isEmpty()) {
            remote = local;
        }
        librarySongs = remote;
    }

    private boolean catalogHas(ArrayList<SongsData> list, SongsData song) {
        for (SongsData s : list) {
            if (sameText(s.getSongName(), song.getSongName()) && sameText(s.getArtistName(), song.getArtistName())) return true;
        }
        return false;
    }

    private static boolean sameText(String a, String b) {
        return a != null && b != null && a.equalsIgnoreCase(b);
    }

    private static boolean containsText(String value, String term) {
        return value != null && value.toLowerCase().contains(term);
    }

    private String songJson(SongsData s) {
        return "{"
                + "\"title\":" + ApiClient.jsonString(s.getSongName()) + ","
                + "\"artist\":" + ApiClient.jsonString(s.getArtistName()) + ","
                + "\"genre\":" + ApiClient.jsonString(s.getGenre()) + ","
                + "\"releaseYear\":" + s.getYear()
                + "}";
    }

    private ArrayList<SongsData> loadCloudSongs() {
        ArrayList<SongsData>[] box = new ArrayList[1];
        CountDownLatch latch = new CountDownLatch(1);
        CloudSongService.loadAllAsync((list, err) -> { box[0] = err == null ? list : null; latch.countDown(); });
        try { latch.await(30, TimeUnit.SECONDS); }
        catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
        return box[0];
    }

    private ArrayList<User> loadUsersBlocking() {
        ArrayList<User>[] box = new ArrayList[1];
        CountDownLatch latch = new CountDownLatch(1);
        CloudUserService.loadAllAsync((list, err) -> { box[0] = err == null ? list : null; latch.countDown(); });
        try { latch.await(30, TimeUnit.SECONDS); }
        catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
        return box[0];
    }

    private void playSong(SongsData s) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select the .wav file for: " + s.getSongName());
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".wav")) { message("This project currently supports .wav audio files.", JOptionPane.WARNING_MESSAGE); return; }
            startPlayback(s, file);
        }
    }

    private void startPlayback(SongsData s, File file) {
        nowPlaying = s; lastFile = file; playing = true; refreshPlayer();
        new Thread(() -> {
            new PlayMusic().playMusic(file.getAbsolutePath());
            SwingUtilities.invokeLater(() -> { playing = false; refreshPlayer(); });
        }).start();
    }

    // ================================================================== NOW PLAYING BAR
    private JPanel playerBar() {
        JPanel bar = new JPanel(new BorderLayout(16, 0)); bar.setBackground(TOPBAR);
        bar.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, LINE), new EmptyBorder(12, 24, 12, 24)));
        playerCover = new Cover("MusicLoud", 14, 54, 54);
        playerTitle = label("Nothing playing", 14, TEXT, true);
        playerSub = label("Press Play on any song", 12, MUTED, false);
        JPanel info = new JPanel(); info.setOpaque(false); info.setLayout(new BoxLayout(info, BoxLayout.X_AXIS));
        JPanel texts = new JPanel(); texts.setOpaque(false); texts.setLayout(new BoxLayout(texts, BoxLayout.Y_AXIS));
        texts.add(Box.createVerticalGlue()); texts.add(playerTitle); texts.add(playerSub); texts.add(Box.createVerticalGlue());
        info.add(playerCover); info.add(Box.createHorizontalStrut(14)); info.add(texts);
        JPanel west = new JPanel(new BorderLayout()); west.setOpaque(false); west.setPreferredSize(new Dimension(300, 54)); west.add(info, BorderLayout.CENTER);
        playerButton = new PlayDot(46); equalizer = new Equalizer();
        playerButton.addActionListener(e -> {
            if (lastFile == null) message("Press Play on any song to start listening.", JOptionPane.INFORMATION_MESSAGE);
            else if (!playing) startPlayback(nowPlaying, lastFile);
        });
        JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 4)); center.setOpaque(false); center.add(playerButton); center.add(equalizer);
        playerStatus = label("WAV  •  LOCAL FILES", 10, MUTED, true);
        JPanel east = new JPanel(new GridBagLayout()); east.setOpaque(false); east.setPreferredSize(new Dimension(300, 54)); east.add(playerStatus);
        bar.add(west, BorderLayout.WEST); bar.add(center, BorderLayout.CENTER); bar.add(east, BorderLayout.EAST);
        refreshPlayer();
        return bar;
    }

    private void refreshPlayer() {
        if (playerTitle == null) return;
        if (nowPlaying != null) {
            setText(playerTitle, nowPlaying.getSongName()); setText(playerSub, nowPlaying.getArtistName() + "  •  " + nowPlaying.getGenre());
            playerCover.setSeed(nowPlaying.getSongName());
        }
        setText(playerStatus, playing ? "●  NOW PLAYING  —  PRESS OK TO STOP" : "WAV  •  LOCAL FILES");
        playerStatus.setForeground(playing ? PURPLE : MUTED);
        equalizer.setActive(playing); playerButton.repaint();
    }

    // ================================================================== BUILDING BLOCKS
    private JPanel albumCard(SongsData s) {
        RoundPanel card = new RoundPanel(CARD, 22); card.setLayout(new BorderLayout(0, 12)); card.setBorder(new EmptyBorder(12, 12, 14, 12));
        card.add(new Cover(s.getSongName(), 16, 10, 118), BorderLayout.NORTH);
        JPanel names = new JPanel(); names.setOpaque(false); names.setLayout(new BoxLayout(names, BoxLayout.Y_AXIS));
        names.add(label(s.getSongName(), 14, TEXT, true)); names.add(label(s.getArtistName() + "  •  " + s.getGenre(), 12, MUTED, false));
        JPanel row = new JPanel(new BorderLayout(8, 0)); row.setOpaque(false); row.add(names, BorderLayout.CENTER);
        if (!admin) { PlayDot d = new PlayDot(34); d.addActionListener(e -> playSong(s)); JPanel dw = new JPanel(new GridBagLayout()); dw.setOpaque(false); dw.add(d); row.add(dw, BorderLayout.EAST); }
        card.add(row, BorderLayout.CENTER);
        hoverable(card, CARD_HOVER);
        return card;
    }

    private JPanel rankRow(int rank, SongsData s) {
        RoundPanel row = new RoundPanel(CARD, 16); row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS)); row.setBorder(new EmptyBorder(8, 6, 8, 8));
        JLabel n = label(String.format("%02d", rank), 12, MUTED, true); n.setPreferredSize(new Dimension(30, 16)); n.setMinimumSize(new Dimension(30, 16)); n.setMaximumSize(new Dimension(30, 16));
        n.setHorizontalAlignment(SwingConstants.CENTER);
        row.add(n); row.add(new Cover(s.getSongName(), 10, 40, 40)); row.add(Box.createHorizontalStrut(10));
        JPanel names = new JPanel(); names.setOpaque(false); names.setLayout(new BoxLayout(names, BoxLayout.Y_AXIS));
        names.add(label(s.getSongName(), 13, TEXT, true)); names.add(label(s.getArtistName(), 11, MUTED, false));
        row.add(names); row.add(Box.createHorizontalGlue());
        if (!admin) { PlayDot d = new PlayDot(30); d.addActionListener(e -> playSong(s)); row.add(d); }
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        hoverable(row, CARD_HOVER);
        return row;
    }

    private JPanel songLine(SongsData s, int rank) {
        RoundPanel row = new RoundPanel(CARD, 18); row.setLayout(new BorderLayout(15, 0)); row.setBorder(new EmptyBorder(11, 14, 11, 16));
        row.add(new Cover(s.getSongName(), 12, 48, 48), BorderLayout.WEST);
        JPanel names = new JPanel(); names.setOpaque(false); names.setLayout(new BoxLayout(names, BoxLayout.Y_AXIS));
        names.add(Box.createVerticalGlue()); names.add(label(s.getSongName(), 15, TEXT, true)); names.add(label(s.getArtistName() + "  •  " + s.getGenre() + "  •  " + s.getYear(), 12, MUTED, false)); names.add(Box.createVerticalGlue());
        row.add(names, BorderLayout.CENTER);
        if (!admin) { PlayDot d = new PlayDot(38); d.addActionListener(e -> playSong(s)); JPanel dw = new JPanel(new GridBagLayout()); dw.setOpaque(false); dw.add(d); row.add(dw, BorderLayout.EAST); }
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 76)); row.setPreferredSize(new Dimension(300, 76));
        hoverable(row, CARD_HOVER);
        return row;
    }

    private JPanel stat(String name, String value, String detail, Kind icon) {
        RoundPanel c = new RoundPanel(CARD, 20); c.setLayout(new BorderLayout(14, 0)); c.setBorder(new EmptyBorder(16, 16, 16, 16));
        JPanel wrap = new JPanel(new GridBagLayout()); wrap.setOpaque(false); wrap.add(new Disc(null, icon, 46));
        JPanel t = new JPanel(); t.setOpaque(false); t.setLayout(new BoxLayout(t, BoxLayout.Y_AXIS));
        t.add(label(name, 10, MUTED, true)); t.add(gap(4)); t.add(label(value, 24, TEXT, true)); t.add(gap(2)); t.add(label(detail, 11, MUTED, false));
        c.add(wrap, BorderLayout.WEST); c.add(t, BorderLayout.CENTER);
        return c;
    }

    private JPanel sectionHeader(String title, String actionText, Runnable action) {
        JPanel h = new JPanel(new BorderLayout()); h.setOpaque(false);
        h.add(label(title, 20, TEXT, true), BorderLayout.WEST);
        Btn b = new Btn(actionText, Style.GHOST, null); b.addActionListener(e -> action.run());
        JPanel bw = new JPanel(new GridBagLayout()); bw.setOpaque(false); bw.add(b); h.add(bw, BorderLayout.EAST);
        return cap(h);
    }

    private JPanel tableCard(JTable table, int height) {
        RoundPanel card = new RoundPanel(CARD, 22); card.setLayout(new BorderLayout()); card.setBorder(new EmptyBorder(8, 8, 8, 8));
        JScrollPane sp = new JScrollPane(table); sp.setBorder(BorderFactory.createEmptyBorder()); sp.setOpaque(false); sp.getViewport().setOpaque(false);
        slimBars(sp); card.add(sp);
        card.setPreferredSize(new Dimension(760, height)); card.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        return card;
    }

    private void styleTable(JTable t) {
        t.setOpaque(false); t.setBackground(CARD); t.setForeground(TEXT); t.setGridColor(LINE); t.setRowHeight(48);
        t.setShowVerticalLines(false); t.setShowHorizontalLines(true); t.setIntercellSpacing(new Dimension(0, 1)); t.setFillsViewportHeight(true);
        t.setFont(font(Font.PLAIN, 13)); t.setSelectionBackground(new Color(75, 57, 111)); t.setSelectionForeground(TEXT);
        DefaultTableCellRenderer cell = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable tb, Object v, boolean sel, boolean foc, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(tb, v, false, false, r, c);
                l.setOpaque(sel); l.setBackground(new Color(75, 57, 111));
                l.setForeground(c == 1 ? TEXT : new Color(200, 195, 218)); l.setFont(font(c == 1 ? Font.BOLD : Font.PLAIN, 13));
                l.setBorder(new EmptyBorder(0, 16, 0, 8)); return l;
            }
        };
        t.setDefaultRenderer(Object.class, cell);
        DefaultTableCellRenderer head = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable tb, Object v, boolean sel, boolean foc, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(tb, v, false, false, r, c);
                l.setOpaque(true); l.setBackground(new Color(38, 29, 58)); l.setForeground(MUTED); l.setFont(font(Font.BOLD, 11));
                l.setHorizontalAlignment(SwingConstants.LEFT); l.setBorder(new EmptyBorder(0, 16, 0, 8)); return l;
            }
        };
        t.getTableHeader().setDefaultRenderer(head); t.getTableHeader().setPreferredSize(new Dimension(10, 40));
        t.getTableHeader().setReorderingAllowed(false); t.getTableHeader().setOpaque(false); t.getTableHeader().setBackground(new Color(38, 29, 58));
    }

    private void slimBars(JScrollPane sp) {
        sp.getVerticalScrollBar().setUI(new SlimBar()); sp.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0)); sp.getVerticalScrollBar().setOpaque(false);
        sp.getHorizontalScrollBar().setUI(new SlimBar()); sp.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 10)); sp.getHorizontalScrollBar().setOpaque(false);
        sp.setCorner(JScrollPane.LOWER_RIGHT_CORNER, new JPanel() {{ setOpaque(false); }});
    }

    private void hoverable(RoundPanel p, Color hoverColor) {
        p.hover = hoverColor;
        MouseAdapter ma = new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { p.over = true; p.repaint(); }
            @Override public void mouseExited(MouseEvent e) { Point q = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), p); if (!p.contains(q)) { p.over = false; p.repaint(); } }
        };
        attach(p, ma);
    }

    private void attach(Component c, MouseAdapter ma) { c.addMouseListener(ma); if (c instanceof Container) for (Component k : ((Container) c).getComponents()) attach(k, ma); }

    private String greeting() { int h = java.time.LocalTime.now().getHour(); return h < 12 ? "Good morning" : h < 17 ? "Good afternoon" : "Good evening"; }

    // ---- layout helpers
    private JPanel content() { JPanel p = new JPanel(); p.setBackground(BG); p.setBorder(new EmptyBorder(32, 38, 32, 38)); p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS)); return p; }
    private JPanel empty(String text) { RoundPanel p = new RoundPanel(CARD, 18); p.setBorder(new EmptyBorder(18, 20, 18, 20)); p.setLayout(new BorderLayout()); p.add(label(text, 13, MUTED, false)); p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60)); return p; }
    private static void put(JComponent parent, Component c) { if (c instanceof JComponent) ((JComponent) c).setAlignmentX(Component.LEFT_ALIGNMENT); parent.add(c); }
    private static Component gap(int h) { JComponent s = (JComponent) Box.createVerticalStrut(h); s.setAlignmentX(Component.LEFT_ALIGNMENT); return s; }
    private static <T extends JComponent> T cap(T c) { c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height)); c.setAlignmentX(Component.LEFT_ALIGNMENT); return c; }
    private static JComponent wide(JComponent c) { c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height)); return c; }
    private JPanel labeled(String name, JComponent field) { JPanel p = new JPanel(); p.setOpaque(false); p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS)); put(p, label(name, 11, MUTED, true)); put(p, gap(7)); put(p, field); return p; }
    private JPanel pair(JComponent a, JComponent b) { JPanel p = new JPanel(new GridLayout(1, 2, 14, 0)); p.setOpaque(false); p.add(a); p.add(b); p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90)); return p; }
    private static String esc(String s) { return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"); }
    private static void setText(JLabel l, String t) { l.setText("<html>" + esc(t).replace("\n", "<br>") + "</html>"); }
    private static Font font(int style, int size) { return new Font(FONT, style, size); }
    private static String pickFont() {
        java.util.Set<String> have = new java.util.HashSet<>(java.util.Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        for (String f : new String[]{"Segoe UI", "Inter", "Helvetica Neue"}) if (have.contains(f)) return f;
        return "SansSerif";
    }
    private JLabel label(String t, int size, Color color, boolean bold) {
        JLabel l = new JLabel(); setText(l, t); l.setFont(font(bold ? Font.BOLD : Font.PLAIN, size)); l.setForeground(color); l.setAlignmentX(Component.LEFT_ALIGNMENT); return l;
    }

    // ---- dialogs (dark themed, replace the light system dialogs)
    private void message(String text, int type) {
        String head = type == JOptionPane.WARNING_MESSAGE ? "Heads up" : type == JOptionPane.ERROR_MESSAGE ? "Something went wrong" : "MusicLoud";
        showDialog("MusicLoud", head, text, null, "OK", null);
    }

    private boolean confirm(String text) { return showDialog("Confirm deletion", "Are you sure?", text, null, "Delete", "Cancel"); }

    private boolean showDialog(String windowTitle, String heading, String text, JComponent body, String okText, String cancelText) {
        JDialog d = new JDialog(this, windowTitle, true);
        boolean[] result = {false};
        JPanel root = new JPanel(new BorderLayout(0, 20)); root.setBackground(CARD); root.setBorder(new EmptyBorder(26, 28, 22, 28));
        JPanel top = new JPanel(); top.setOpaque(false); top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        put(top, label(heading, 20, TEXT, true)); put(top, gap(8));
        JLabel msg = new JLabel("<html><body style='width:330px'>" + esc(text) + "</body></html>"); msg.setFont(font(Font.PLAIN, 13)); msg.setForeground(MUTED);
        put(top, msg);
        root.add(top, BorderLayout.NORTH);
        if (body != null) root.add(body, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0)); buttons.setOpaque(false);
        Btn ok = new Btn(okText, Style.PRIMARY, null);
        ok.addActionListener(e -> { result[0] = true; d.dispose(); });
        if (cancelText != null) { Btn cancel = new Btn(cancelText, Style.SECONDARY, null); cancel.addActionListener(e -> d.dispose()); buttons.add(cancel); }
        buttons.add(ok);
        root.add(buttons, BorderLayout.SOUTH);
        d.setContentPane(root); d.getRootPane().setDefaultButton(ok);
        d.getRootPane().registerKeyboardAction(e -> d.dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        d.pack(); d.setResizable(false); d.setLocationRelativeTo(this); d.setVisible(true);
        return result[0];
    }

    // ================================================================== CUSTOM COMPONENTS
    private enum Kind { HOME, LIBRARY, NOTE, SEARCH, STAR, LIST, USER, LOGOUT, PLAY, PAUSE, PLUS, MAIL }
    private enum Style { PRIMARY, SECONDARY, WHITE, GHOST, NAV, CHIP }

    private static Graphics2D g2(Graphics g) {
        Graphics2D x = (Graphics2D) g.create();
        x.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        x.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        x.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return x;
    }

    /** Vector icons drawn with Java2D so they stay crisp and need no image files. */
    private static class Ico implements Icon {
        final Kind k; final int s; final Color c;
        Ico(Kind k, int s, Color c) { this.k = k; this.s = s; this.c = c; }
        public int getIconWidth() { return s; }
        public int getIconHeight() { return s; }
        public void paintIcon(Component comp, Graphics g, int x, int y) {
            Graphics2D g2 = g2(g); g2.translate(x, y); g2.scale(s, s);
            g2.setColor(c); g2.setStroke(new BasicStroke(1.8f / s, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D.Double p = new Path2D.Double();
            switch (k) {
                case HOME:
                    p.moveTo(.1, .5); p.lineTo(.5, .15); p.lineTo(.9, .5);
                    p.moveTo(.2, .42); p.lineTo(.2, .86); p.lineTo(.8, .86); p.lineTo(.8, .42);
                    p.moveTo(.42, .86); p.lineTo(.42, .62); p.lineTo(.58, .62); p.lineTo(.58, .86);
                    g2.draw(p); break;
                case LIBRARY:
                    g2.draw(new RoundRectangle2D.Double(.13, .2, .2, .6, .06, .06)); g2.draw(new RoundRectangle2D.Double(.4, .2, .2, .6, .06, .06));
                    g2.rotate(Math.toRadians(-16), .8, .5); g2.draw(new RoundRectangle2D.Double(.68, .2, .2, .6, .06, .06)); break;
                case NOTE:
                    g2.fill(new Ellipse2D.Double(.14, .58, .3, .24));
                    p.moveTo(.42, .7); p.lineTo(.42, .16); p.curveTo(.5, .3, .8, .3, .8, .5); g2.draw(p); break;
                case SEARCH:
                    g2.draw(new Ellipse2D.Double(.14, .14, .5, .5)); g2.draw(new Line2D.Double(.6, .6, .86, .86)); break;
                case STAR:
                    p.moveTo(.5, .06); p.lineTo(.62, .38); p.lineTo(.94, .5); p.lineTo(.62, .62); p.lineTo(.5, .94); p.lineTo(.38, .62); p.lineTo(.06, .5); p.lineTo(.38, .38); p.closePath(); g2.fill(p); break;
                case LIST:
                    g2.draw(new Line2D.Double(.12, .26, .66, .26)); g2.draw(new Line2D.Double(.12, .5, .66, .5)); g2.draw(new Line2D.Double(.12, .74, .42, .74));
                    p.moveTo(.7, .56); p.lineTo(.7, .86); g2.draw(p); g2.fill(new Ellipse2D.Double(.6, .76, .16, .13)); break;
                case USER:
                    g2.draw(new Ellipse2D.Double(.33, .12, .34, .34)); g2.draw(new Arc2D.Double(.17, .54, .66, .6, 0, 180, Arc2D.OPEN)); break;
                case LOGOUT:
                    p.moveTo(.45, .14); p.lineTo(.16, .14); p.lineTo(.16, .86); p.lineTo(.45, .86);
                    p.moveTo(.36, .5); p.lineTo(.86, .5); p.moveTo(.68, .32); p.lineTo(.86, .5); p.lineTo(.68, .68); g2.draw(p); break;
                case PLAY:
                    p.moveTo(.3, .18); p.lineTo(.3, .82); p.lineTo(.84, .5); p.closePath(); g2.fill(p); break;
                case PAUSE:
                    g2.fill(new RoundRectangle2D.Double(.26, .2, .17, .6, .06, .06)); g2.fill(new RoundRectangle2D.Double(.57, .2, .17, .6, .06, .06)); break;
                case PLUS:
                    g2.draw(new Line2D.Double(.5, .16, .5, .84)); g2.draw(new Line2D.Double(.16, .5, .84, .5)); break;
                case MAIL:
                    g2.draw(new RoundRectangle2D.Double(.1, .22, .8, .56, .1, .1));
                    p.moveTo(.12, .28); p.lineTo(.5, .56); p.lineTo(.88, .28); g2.draw(p); break;
            }
            g2.dispose();
        }
    }

    private static class RoundPanel extends JPanel {
        final Color fill; final int arc; Color hover; boolean over;
        RoundPanel(Color fill, int arc) { this.fill = fill; this.arc = arc; setOpaque(false); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = g2(g); g2.setColor(over && hover != null ? hover : fill);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc); g2.dispose(); super.paintComponent(g);
        }
    }

    /** Gradient banner. mode 0 = banner with a vinyl on the right, mode 1 = full-bleed artwork panel. */
    private static class Hero extends JPanel {
        final Color a, b, c; final int arc, mode;
        Hero(Color a, Color b, Color c, int arc, int mode) { this.a = a; this.b = b; this.c = c; this.arc = arc; this.mode = mode; setOpaque(false); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = g2(g); int w = getWidth(), h = getHeight();
            g2.setPaint(new LinearGradientPaint(0, 0, w, h, new float[]{0f, .55f, 1f}, new Color[]{a, b, c}));
            if (arc > 0) g2.fillRoundRect(0, 0, w, h, arc, arc); else g2.fillRect(0, 0, w, h);
            Shape old = g2.getClip(); if (arc > 0) g2.clip(new RoundRectangle2D.Double(0, 0, w, h, arc, arc));
            g2.setColor(new Color(255, 255, 255, 16)); g2.fillOval(w - 210, -110, 300, 300);
            g2.setColor(new Color(255, 255, 255, 10)); g2.fillOval(w - 120, h - 120, 220, 220);
            g2.setClip(old);
            if (mode == 0 && w > 560) {
                int r = (int) (h * 0.27); int cx = w - r - 30, cy = h / 2 + 4;
                paintVinyl(g2, cx, cy, r);
                new Ico(Kind.NOTE, 26, new Color(255, 255, 255, 150)).paintIcon(this, g2, cx - r - 6, cy - r - 14);
                new Ico(Kind.NOTE, 20, new Color(255, 255, 255, 110)).paintIcon(this, g2, cx + r - 14, cy - r - 22);
                new Ico(Kind.NOTE, 16, new Color(255, 255, 255, 90)).paintIcon(this, g2, cx - r - 4, cy + r - 10);
            } else if (mode == 1) {
                int r = (int) (Math.min(w, h) * 0.34); paintVinyl(g2, (int) (w * 0.66), (int) (h * 0.8), r);
                new Ico(Kind.NOTE, 34, new Color(255, 255, 255, 70)).paintIcon(this, g2, (int) (w * 0.8), (int) (h * 0.52));
                new Ico(Kind.NOTE, 24, new Color(255, 255, 255, 60)).paintIcon(this, g2, (int) (w * 0.9), (int) (h * 0.44));
            }
            g2.dispose(); super.paintComponent(g);
        }
        static void paintVinyl(Graphics2D g, int cx, int cy, int r) {
            g.setPaint(new RadialGradientPaint(cx, cy, r * 1.3f, new float[]{.6f, 1f}, new Color[]{new Color(255, 255, 255, 46), new Color(255, 255, 255, 0)}));
            g.fillOval(cx - (int) (r * 1.3), cy - (int) (r * 1.3), (int) (r * 2.6), (int) (r * 2.6));
            g.setColor(new Color(20, 13, 34)); g.fillOval(cx - r, cy - r, 2 * r, 2 * r);
            g.setStroke(new BasicStroke(1f)); g.setColor(new Color(255, 255, 255, 16));
            for (int i = 1; i <= 6; i++) { int rr = (int) (r * (0.46 + 0.08 * i)); g.drawOval(cx - rr, cy - rr, 2 * rr, 2 * rr); }
            g.setColor(new Color(255, 255, 255, 30)); g.fillArc(cx - r, cy - r, 2 * r, 2 * r, 28, 34); g.fillArc(cx - r, cy - r, 2 * r, 2 * r, 208, 34);
            int lr = (int) (r * 0.36);
            g.setPaint(new GradientPaint(cx - lr, cy - lr, new Color(244, 114, 182), cx + lr, cy + lr, new Color(124, 58, 237))); g.fillOval(cx - lr, cy - lr, 2 * lr, 2 * lr);
            g.setColor(new Color(20, 13, 34)); int hr = Math.max(3, (int) (r * 0.06)); g.fillOval(cx - hr, cy - hr, 2 * hr, 2 * hr);
        }
    }

    /** Circular badge: gradient with an icon or an initial. */
    private static class Disc extends JComponent {
        final String text; final Kind icon; final int size;
        Disc(String text, Kind icon, int size) { this.text = text; this.icon = icon; this.size = size; Dimension d = new Dimension(size, size); setPreferredSize(d); setMinimumSize(d); setMaximumSize(d); setAlignmentY(0.5f); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = g2(g); g2.setPaint(new GradientPaint(0, 0, VIOLET, size, size, new Color(236, 90, 190))); g2.fillOval(0, 0, size, size);
            if (icon != null) { int is = (int) (size * 0.5); new Ico(icon, is, Color.WHITE).paintIcon(this, g2, (size - is) / 2, (size - is) / 2); }
            else if (text != null) { g2.setFont(font(Font.BOLD, (int) (size * 0.42))); FontMetrics fm = g2.getFontMetrics(); g2.setColor(Color.WHITE); g2.drawString(text, (size - fm.stringWidth(text)) / 2, (size + fm.getAscent() - fm.getDescent()) / 2); }
            g2.dispose();
        }
    }

    /** Generated album art: a gradient picked from the song name. */
    private static class Cover extends JComponent {
        String seed; final int arc;
        Cover(String seed, int arc, int w, int h) { this.seed = seed; this.arc = arc; setPreferredSize(new Dimension(w, h)); setMinimumSize(new Dimension(w, h)); setAlignmentY(0.5f); if (w >= 40 && w <= 60) setMaximumSize(new Dimension(w, h)); }
        void setSeed(String s) { seed = s; repaint(); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = g2(g); int w = getWidth(), h = getHeight(); Color[] pal = COVERS[Math.abs(seed.hashCode()) % COVERS.length];
            g2.setPaint(new GradientPaint(0, 0, pal[0], w, h, pal[1])); g2.fillRoundRect(0, 0, w, h, arc, arc);
            g2.clip(new RoundRectangle2D.Double(0, 0, w, h, arc, arc));
            g2.setColor(new Color(255, 255, 255, 30)); g2.fillOval(-w / 4, h / 2, w, w); g2.setColor(new Color(255, 255, 255, 22)); g2.fillOval(w / 2, -h / 3, w, w);
            int is = (int) (Math.min(w, h) * 0.42); new Ico(Kind.NOTE, is, new Color(255, 255, 255, 235)).paintIcon(this, g2, (w - is) / 2, (h - is) / 2);
            g2.dispose();
        }
    }

    private static class PlayDot extends JButton {
        final int size; boolean hover;
        PlayDot(int size) {
            this.size = size; setContentAreaFilled(false); setBorderPainted(false); setFocusPainted(false); setOpaque(false);
            Dimension d = new Dimension(size, size); setPreferredSize(d); setMinimumSize(d); setMaximumSize(d); setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); setAlignmentY(0.5f);
            addMouseListener(new MouseAdapter() { public void mouseEntered(MouseEvent e) { hover = true; repaint(); } public void mouseExited(MouseEvent e) { hover = false; repaint(); } });
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = g2(g); Color a = hover ? new Color(150, 90, 250) : VIOLET, b = hover ? new Color(240, 110, 210) : FUCHSIA;
            g2.setPaint(new GradientPaint(0, 0, a, size, size, b)); g2.fillOval(0, 0, size, size);
            int is = (int) (size * 0.44); new Ico(Kind.PLAY, is, Color.WHITE).paintIcon(this, g2, (size - is) / 2 + 1, (size - is) / 2); g2.dispose();
        }
    }

    private static class Equalizer extends JComponent {
        private boolean active; private double t; private final javax.swing.Timer timer;
        Equalizer() { setPreferredSize(new Dimension(34, 26)); timer = new javax.swing.Timer(90, e -> { t += 0.7; repaint(); }); }
        void setActive(boolean a) { active = a; if (a) timer.start(); else timer.stop(); repaint(); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = g2(g); g2.setColor(active ? PURPLE : new Color(90, 76, 120));
            for (int i = 0; i < 4; i++) { double f = active ? 0.3 + 0.7 * Math.abs(Math.sin(t + i * 1.4)) : new double[]{0.4, 0.75, 0.55, 0.3}[i]; int bh = (int) (getHeight() * f); g2.fillRoundRect(i * 8, getHeight() - bh, 4, bh, 4, 4); }
            g2.dispose();
        }
    }

    /** Button painted entirely by us, so it looks the same on every Look and Feel. */
    private static class Btn extends JButton {
        final Style style; final Kind kind; boolean active, hover;
        Btn(String text, Style style, Kind kind) {
            super(text); this.style = style; this.kind = kind;
            setContentAreaFilled(false); setBorderPainted(false); setFocusPainted(false); setOpaque(false); setFont(font(Font.BOLD, 13));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); setAlignmentX(Component.LEFT_ALIGNMENT);
            int v = style == Style.CHIP ? 8 : 12, hpad = style == Style.NAV ? 16 : 22;
            setBorder(new EmptyBorder(v, hpad, v, hpad));
            if (style == Style.NAV) setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            addMouseListener(new MouseAdapter() { public void mouseEntered(MouseEvent e) { hover = true; repaint(); } public void mouseExited(MouseEvent e) { hover = false; repaint(); } });
        }
        void setActive(boolean a) { active = a; repaint(); }
        private Color fg() {
            if (style == Style.WHITE) return new Color(88, 36, 160);
            if (style == Style.NAV) return active || hover ? TEXT : MUTED;
            if (style == Style.GHOST) return hover ? TEXT : MUTED;
            return TEXT;
        }
        @Override public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont()); Insets in = getInsets(); int iw = kind != null ? 18 + 10 : 0;
            return new Dimension(in.left + iw + fm.stringWidth(getText()) + in.right, Math.max(fm.getHeight(), 18) + in.top + in.bottom);
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = g2(g); int w = getWidth(), h = getHeight(); boolean pressed = getModel().isPressed();
            int arc = style == Style.PRIMARY || style == Style.WHITE || style == Style.CHIP ? h : 16;
            boolean gradient = style == Style.PRIMARY || (style == Style.CHIP && active) || (style == Style.NAV && active);
            if (gradient) {
                Color a = hover || pressed ? new Color(148, 88, 248) : VIOLET, b = hover || pressed ? new Color(222, 100, 225) : FUCHSIA;
                if (style == Style.NAV) { a = new Color(124, 58, 237, 235); b = new Color(168, 85, 247, 190); }
                g2.setPaint(new GradientPaint(0, 0, a, w, 0, b)); g2.fillRoundRect(0, 0, w, h, arc, arc);
            } else if (style == Style.WHITE) { g2.setColor(hover ? new Color(244, 236, 255) : Color.WHITE); g2.fillRoundRect(0, 0, w, h, arc, arc); }
            else if (style == Style.SECONDARY || style == Style.CHIP) {
                g2.setColor(hover ? new Color(60, 47, 80) : new Color(43, 35, 58)); g2.fillRoundRect(0, 0, w, h, arc, arc);
                g2.setColor(new Color(76, 58, 99)); g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);
            } else if (hover) { g2.setColor(new Color(255, 255, 255, 16)); g2.fillRoundRect(0, 0, w, h, arc, arc); }
            FontMetrics fm = g2.getFontMetrics(getFont()); g2.setFont(getFont()); Insets in = getInsets();
            int iw = kind != null ? 18 + 10 : 0, cw = iw + fm.stringWidth(getText());
            int x = style == Style.NAV || getHorizontalAlignment() == SwingConstants.LEFT ? in.left : (w - cw) / 2;
            if (kind != null) { new Ico(kind, 18, fg()).paintIcon(this, g2, x, (h - 18) / 2); x += iw; }
            g2.setColor(fg()); g2.drawString(getText(), x, (h + fm.getAscent() - fm.getDescent()) / 2);
            g2.dispose();
        }
    }

    // text fields with placeholder, rounded border and optional leading icon
    private static void paintField(JTextComponent c, Graphics g, String hint, Kind icon) {
        Graphics2D g2 = g2(g); int w = c.getWidth(), h = c.getHeight();
        g2.setColor(new Color(37, 29, 52)); g2.fillRoundRect(0, 0, w, h, 16, 16);
        g2.setColor(c.hasFocus() ? PURPLE : BORDER); g2.drawRoundRect(0, 0, w - 1, h - 1, 16, 16);
        if (icon != null) new Ico(icon, 18, new Color(150, 138, 178)).paintIcon(c, g2, 15, (h - 18) / 2);
        g2.dispose();
    }

    private static void paintHint(JTextComponent c, Graphics g, String hint) {
        if (c.getDocument().getLength() > 0 || hint == null) return;
        Graphics2D g2 = g2(g); g2.setFont(c.getFont()); g2.setColor(new Color(128, 118, 154)); FontMetrics fm = g2.getFontMetrics(); Insets in = c.getInsets();
        g2.drawString(hint, in.left, (c.getHeight() + fm.getAscent() - fm.getDescent()) / 2); g2.dispose();
    }

    private static void prepField(JTextComponent c, Kind icon) {
        c.setOpaque(false); c.setFont(font(Font.PLAIN, 14)); c.setForeground(TEXT); c.setCaretColor(TEXT); c.setSelectionColor(PURPLE_DARK); c.setSelectedTextColor(TEXT);
        c.setBorder(new EmptyBorder(12, icon != null ? 44 : 16, 12, 16)); c.setPreferredSize(new Dimension(260, 46)); c.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        c.addFocusListener(new FocusAdapter() { public void focusGained(FocusEvent e) { c.repaint(); } public void focusLost(FocusEvent e) { c.repaint(); } });
    }

    private static class Input extends JTextField {
        final String hint; final Kind icon;
        Input(String hint, Kind icon) { this.hint = hint; this.icon = icon; prepField(this, icon); }
        @Override protected void paintComponent(Graphics g) { paintField(this, g, hint, icon); super.paintComponent(g); paintHint(this, g, hint); }
    }

    private static class PassInput extends JPasswordField {
        final String hint;
        PassInput(String hint) { this.hint = hint; prepField(this, null); }
        @Override protected void paintComponent(Graphics g) { paintField(this, g, hint, null); super.paintComponent(g); paintHint(this, g, hint); }
    }

    private static class PageHost extends JPanel implements Scrollable {
        PageHost(LayoutManager lm) { super(lm); setOpaque(false); }
        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 18; }
        public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return 120; }
        public boolean getScrollableTracksViewportWidth() { return true; }
        public boolean getScrollableTracksViewportHeight() { return false; }
    }

    private static class SlimBar extends BasicScrollBarUI {
        private static JButton none() { JButton b = new JButton(); b.setPreferredSize(new Dimension(0, 0)); b.setMinimumSize(new Dimension(0, 0)); b.setMaximumSize(new Dimension(0, 0)); return b; }
        @Override protected JButton createDecreaseButton(int o) { return none(); }
        @Override protected JButton createIncreaseButton(int o) { return none(); }
        @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) { }
        @Override protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            if (r.isEmpty() || !scrollbar.isEnabled()) return;
            Graphics2D g2 = g2(g); g2.setColor(isThumbRollover() ? new Color(120, 96, 168) : new Color(82, 64, 116)); g2.fillRoundRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 8, 8); g2.dispose();
        }
    }

    public static void main(String[] args) { try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) { } SwingUtilities.invokeLater(() -> new MusicLoudRedesign().setVisible(true)); }
}
