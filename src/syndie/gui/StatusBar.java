package syndie.gui;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import net.i2p.data.DataHelper;
import net.i2p.data.Hash;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import syndie.Constants;
import syndie.Version;
import syndie.data.ChannelInfo;
import syndie.data.NymReferenceNode;
import syndie.data.ReferenceNode;
import syndie.data.SyndieURI;
import syndie.data.Timer;
import syndie.db.DBClient;
import syndie.db.JobRunner;
import syndie.db.SyncArchive;
import syndie.db.SyncManager;
import syndie.db.ThreadAccumulatorJWZ;
import syndie.db.UI;

/**
 *
 */
public class StatusBar extends BaseComponent implements Translatable, Themeable, DBClient.WatchEventListener {
    private BookmarkControl _bookmarkControl;
    private NavigationControl _navControl;
    private URIControl _uriControl;
    private DataCallback _dataCallback;
    private Browser _browser;
    private Composite _parent;
    private Composite _root;
    private Button _bookmark;
    private Label _onlineState;
    private Label _nextSyncLabel;
    private Label _nextSyncDate;
    private Button _newForum;
    private Menu _newForumMenu;
    private Button _unread;
    private Menu _unreadMenu;
    private Button _pbe;
    private Menu _pbeMenu;
    private Button _priv;
    private Menu _privMenu;
    private Button _postpone;
    private Menu _postponeMenu;
    private Label _version;
    private boolean _enableRefresh;
    private boolean _syncNow;
    
    public StatusBar(DBClient client, UI ui, ThemeRegistry themes, TranslationRegistry trans, BookmarkControl bookmarkControl, NavigationControl navControl, URIControl uriControl, Browser browser, DataCallback callback, Composite parent, Timer timer) {
        super(client, ui, themes, trans);
        _bookmarkControl = bookmarkControl;
        _navControl = navControl;
        _uriControl = uriControl;
        _browser = browser;
        _dataCallback = callback;
        _parent = parent;
        _enableRefresh = true;
        _syncNow = false;
        initComponents(timer);
    }
    
    public Control getControl() { return _root; }
    
    private void initComponents(Timer timer) {
        _root = new Composite(_parent, SWT.NONE);
        GridLayout gl = new GridLayout(10, false);
        _root.setLayout(gl);
        
        _bookmark = new Button(_root, SWT.PUSH);
        _bookmark.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false));
        _bookmark.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent selectionEvent) {
                _bookmarkControl.bookmarkCurrentTab();
            }
            public void widgetSelected(SelectionEvent selectionEvent) {
                _bookmarkControl.bookmarkCurrentTab();
            }
        });
        
        _onlineState = new Label(_root, SWT.SHADOW_OUT | SWT.BORDER);
        _onlineState.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false));
        
        _nextSyncLabel = new Label(_root, SWT.NONE);
        _nextSyncLabel.setLayoutData(new GridData(GridData.END, GridData.CENTER, false, false));
        _nextSyncDate = new Label(_root, SWT.NONE);
        _nextSyncDate.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, false, false));
        
        _nextSyncLabel.addMouseListener(new MouseListener() {
            public void mouseDoubleClick(MouseEvent mouseEvent) {}
            public void mouseDown(MouseEvent mouseEvent) {}
            public void mouseUp(MouseEvent mouseEvent) {
                _navControl.view(_uriControl.createSyndicationConfigURI());
            }
        });
        _nextSyncDate.addMouseListener(new MouseListener() {
            public void mouseDoubleClick(MouseEvent mouseEvent) {}
            public void mouseDown(MouseEvent mouseEvent) {}
            public void mouseUp(MouseEvent mouseEvent) {
                _navControl.view(_uriControl.createSyndicationConfigURI());
            }
        });
        
        _newForum = new Button(_root, SWT.PUSH);
        _newForum.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false));
        
        _newForumMenu = new Menu(_newForum);
        _newForum.setMenu(_newForumMenu);
        _newForum.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent selectionEvent) { _newForumMenu.setVisible(true); }
            public void widgetSelected(SelectionEvent selectionEvent) { _newForumMenu.setVisible(true); }
        });
        
        _unread = new Button(_root, SWT.PUSH);
        _unread.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false));
        
        _unreadMenu = new Menu(_unread);
        _unread.setMenu(_unreadMenu);
        _unread.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent selectionEvent) { _unreadMenu.setVisible(true); }
            public void widgetSelected(SelectionEvent selectionEvent) { _unreadMenu.setVisible(true); }
        });
        
        _pbe = new Button(_root, SWT.PUSH);
        _pbe.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false));
        
        _pbeMenu = new Menu(_pbe);
        _pbe.setMenu(_pbeMenu);
        _pbe.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent selectionEvent) { _pbeMenu.setVisible(true); }
            public void widgetSelected(SelectionEvent selectionEvent) { _pbeMenu.setVisible(true); }
        });
        
        _priv = new Button(_root, SWT.PUSH);
        _priv.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false));
        
        _privMenu = new Menu(_priv);
        _priv.setMenu(_privMenu);
        _priv.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent selectionEvent) { _privMenu.setVisible(true); }
            public void widgetSelected(SelectionEvent selectionEvent) { _privMenu.setVisible(true); }
        });
        
        _postpone = new Button(_root, SWT.PUSH);
        _postpone.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false));
        
        _postponeMenu = new Menu(_postpone);
        _postpone.setMenu(_postponeMenu);
        _postpone.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent selectionEvent) { _postponeMenu.setVisible(true); }
            public void widgetSelected(SelectionEvent selectionEvent) { _postponeMenu.setVisible(true); }
        });
        
        _version = new Label(_root, SWT.NONE);
        _version.setText("Syndie " + Version.VERSION);
        _version.setLayoutData(new GridData(GridData.END, GridData.CENTER, true, false));

        _onlineState.addMouseListener(new MouseListener() {
            public void mouseDoubleClick(MouseEvent mouseEvent) {}
            public void mouseDown(MouseEvent mouseEvent) { toggleOnline(); }
            public void mouseUp(MouseEvent mouseEvent) {}
        });
        
        timer.addEvent("status bar: gui constructed");
        
        SyncManager mgr = SyncManager.getInstance(_client, _ui);
        mgr.addListener(new SyncManager.SyncListener() {
            public void archiveAdded(SyncArchive archive) {
                registerListen(archive);
            }
            public void archiveRemoved(SyncArchive archive) {}
            public void archiveLoaded(SyncArchive archive) {
                registerListen(archive);
            }
            public void onlineStatusUpdated(final boolean nowOnline) {
                Display.getDefault().asyncExec(new Runnable() {
                    public void run() {
                        displayOnlineState(nowOnline);
                    }
                });
            }
        });
        timer.addEvent("status bar: sync manager registered");
        for (int i = 0; i < mgr.getArchiveCount(); i++)
            registerListen(mgr.getArchive(i));
    
        timer.addEvent("status bar: sync archive registered");
        initDisplay(); // refreshes the display assuming no interesting attributes
        // but update the display in a second w/ any real attributes
        _browser.runAfterStartup(new Runnable() { public void run() { doRefreshDisplay(true); } });
        //_root.getDisplay().timerExec(500, new Runnable() {
        //    public void run() { doRefreshDisplay(true); }
        //});
        doRefreshDisplay(true); // only the online state (no queries)
        timer.addEvent("status bar: doRefreshDisplay");
        
        _client.addWatchEventListener(this);
        _translationRegistry.register(this);
        _themeRegistry.register(this);
        
        timer.addEvent("status bar: themed");
        initDnD();
        timer.addEvent("status bar: dnd initialized");
        
        delayedRefresh();
        timer.addEvent("status bar: delayed refresh");
    }
    
    // periodically update the online state section, since that includes a timer
    // for when the next sync is.  everything else is updated through event callbacks
    private void delayedRefresh() {
        _root.getDisplay().timerExec(30*1000, new Runnable() {
            public void run() {
                doRefreshDisplay(true);
                delayedRefresh();
            }
        });
    }
    
    private void registerListen(SyncArchive archive) {
        archive.addListener(new SyncArchive.SyncArchiveListener() {
            public void incomingUpdated(SyncArchive.IncomingAction action) {
                // we have callbacks now to know when new messages are imported, so ignore
                // this
                
                //if (action.isComplete() || (action.getPBEPrompt() != null))
                //    refreshDisplay(false); // new import may require updating the counts
            }
            public void incomingUpdated(List actions) {}
            public void outgoingUpdated(SyncArchive.OutgoingAction action) {}
            public void archiveUpdated(SyncArchive archive) {
                refreshDisplay(true); // only update the next sync time
            }
        });
    }
    
    private void toggleOnline() {
        SyncManager mgr = SyncManager.getInstance(_client, _ui);
        mgr.setIsOnline(!mgr.isOnline());
    }
    
    private static final String T_ONLINE = "syndie.gui.statusbar.online";
    private static final String T_OFFLINE = "syndie.gui.statusbar.offline";

    public void refreshDisplay() { refreshDisplay(false); }
    public void refreshDisplay(final boolean onlineStateOnly) {
        if (!_enableRefresh) return;
        _root.getDisplay().asyncExec(new Runnable() { public void run() { doRefreshDisplay(onlineStateOnly); } });
    }
    public void setEnableRefresh(boolean enable) {
        _enableRefresh = enable;
        if (enable) refreshDisplay();
    }
    private void doRefreshDisplay() { doRefreshDisplay(false); }
    private void doRefreshDisplay(boolean onlineStateOnly) {
        SyncManager mgr = SyncManager.getInstance(_client, _ui);
        displayOnlineState(mgr.isOnline());
        
        if (onlineStateOnly) return;
        if (_syncNow) return; // don't update the status details during a refresh
       
        if (!_enableRefresh) return;
        int newForums = refreshNewForums();
        calcUnread();
        int pbe = refreshPBE();
        String priv = refreshPrivateMessages();
        int postpone = refreshPostponed();
        
        if (newForums > 0) {
            _newForum.setText(_translationRegistry.getText(T_NEWFORUM, "New forums: ") + newForums);
            ((GridData)_newForum.getLayoutData()).exclude = false;
            _newForum.setVisible(true);
        } else {
            ((GridData)_newForum.getLayoutData()).exclude = true;
            _newForum.setVisible(false);
        }
        
        if (pbe > 0) {
            _pbe.setText(_translationRegistry.getText(T_PBE, "Pass. req: ") + pbe);
            ((GridData)_pbe.getLayoutData()).exclude = false;
            _pbe.setVisible(true);
        } else {
            ((GridData)_pbe.getLayoutData()).exclude = true;
            _pbe.setVisible(false);
        }
        
        if (priv != null) {
            _priv.setText(_translationRegistry.getText(T_PRIV, "Private msgs: ") + priv);
            ((GridData)_priv.getLayoutData()).exclude = false;
            _priv.setVisible(true);
        } else {
            ((GridData)_priv.getLayoutData()).exclude = true;
            _priv.setVisible(false);
        }
        
        if (postpone > 0) {
            _postpone.setText(_translationRegistry.getText(T_POSTPONE, "Drafts: ") + postpone);
            ((GridData)_postpone.getLayoutData()).exclude = false;
            _postpone.setVisible(true);
        } else {
            ((GridData)_postpone.getLayoutData()).exclude = true;
            _postpone.setVisible(false);
        }
        
        // updated later
        boolean unreadExcluded = ((GridData) _unread.getLayoutData()).exclude;
            
        int cells = 1;
        if (newForums == 0) cells++;
        //if (unread == null) cells++;
        if (unreadExcluded) cells++;
        if (pbe == 0) cells++;
        if (priv == null) cells++;
        if (postpone == 0) cells++;
        
        ((GridData)_version.getLayoutData()).horizontalSpan = cells;
        
        _root.layout(true);
    }
    
    private void initDisplay() {
        ((GridData)_newForum.getLayoutData()).exclude = true;
        _newForum.setVisible(false);
        
        ((GridData)_pbe.getLayoutData()).exclude = true;
        _pbe.setVisible(false);
        
        ((GridData)_priv.getLayoutData()).exclude = true;
        _priv.setVisible(false);
        
        ((GridData)_postpone.getLayoutData()).exclude = true;
        _postpone.setVisible(false);
        
        ((GridData)_unread.getLayoutData()).exclude = true;
        _unread.setVisible(false);
            
        int cells = 6;
        
        ((GridData)_version.getLayoutData()).horizontalSpan = cells;
        
        _root.layout(true);
    }
    
    private boolean _unreadCalcInProgress = false;
    private void calcUnread() {
        synchronized (StatusBar.this) {
            if (_unreadCalcInProgress) {
                //_browser.getUI().debugMessage("skipping calcUnread");
                return;
            }
            _unreadCalcInProgress = true;
        }
        _ui.debugMessage("calcUnread begin");
        final SyndieURI uri = _uriControl.createHighlightWatchedURI(_client, true, true, MessageTree.shouldUseImportDate(_client));
        JobRunner.instance().enqueue(new Runnable() {
            public void run() {
                ThreadAccumulatorJWZ acc = new ThreadAccumulatorJWZ(_client, _ui);
                acc.setFilter(uri);
                acc.gatherThreads();
                final Set forums = new HashSet();
                int threads = 0;
                for (int i = 0; i < acc.getThreadCount(); i++) {
                    ReferenceNode node = acc.getRootThread(i);
                    if (calcUnread(node, forums))
                        threads++;
                }
                final int unreadThreads = threads;
                final Map sortedForums = sortForums(forums);
                _ui.debugMessage("calcUnread end: " + forums.size() + " / " + threads);
                
                Display.getDefault().asyncExec(new Runnable() {
                    public void run() {
                        renderUnread(sortedForums, unreadThreads);
                        synchronized (StatusBar.this) {
                            _unreadCalcInProgress = false;
                        }
                    }
                });
            }
        });
    }
    
    /** set set of watched forums has changed */
    public void watchesUpdated() { calcUnread(); }
    
    private void renderUnread(Map sortedForums, int threads) {
        MenuItem items[] = _unreadMenu.getItems();
        for (int i = 0; i < items.length; i++)
            items[i].dispose();
        
        if (threads > 0) {
            MenuItem item = new MenuItem(_unreadMenu, SWT.PUSH);
            item.setText(_translationRegistry.getText(T_UNREAD_BOOKMARKED, "View unread in bookmarked forums"));
            item.addSelectionListener(new SelectionListener() {
                public void widgetDefaultSelected(SelectionEvent selectionEvent) {
                    _navControl.view(_uriControl.createHighlightWatchedURI(_client, true, true, MessageTree.shouldUseImportDate(_client)));
                }
                public void widgetSelected(SelectionEvent selectionEvent) {
                    _navControl.view(_uriControl.createHighlightWatchedURI(_client, true, true, MessageTree.shouldUseImportDate(_client)));
                }
            });
            
            item = new MenuItem(_unreadMenu, SWT.PUSH);
            item.setText(_translationRegistry.getText(T_UNREAD_ALL, "View unread in all forums"));
            item.addSelectionListener(new SelectionListener() {
                public void widgetDefaultSelected(SelectionEvent selectionEvent) {
                    _navControl.view(SyndieURI.createBookmarked(new ArrayList(), true, true, MessageTree.shouldUseImportDate(_client)));
                }
                public void widgetSelected(SelectionEvent selectionEvent) {
                    _navControl.view(SyndieURI.createBookmarked(new ArrayList(), true, true, MessageTree.shouldUseImportDate(_client)));
                }
            });
            

            
            new MenuItem(_unreadMenu, SWT.SEPARATOR);
            
            for (Iterator iter = sortedForums.entrySet().iterator(); iter.hasNext(); ) {
                Map.Entry entry = (Map.Entry)iter.next();
                String name = (String)entry.getKey();
                final Hash forum = (Hash)entry.getValue();
                int msgs = _client.countUnreadMessages(forum);
                StringBuilder buf = new StringBuilder();
                buf.append(name)
                   .append(" (").append(msgs).append(')');
                item = new MenuItem(_unreadMenu, SWT.PUSH);
                item.setText(buf.toString());
                item.setImage(ImageUtil.ICON_MSG_TYPE_META);
                item.addSelectionListener(new SelectionListener() {
                    public void widgetDefaultSelected(SelectionEvent selectionEvent) {
                        _navControl.view(SyndieURI.createSearch(forum, true, true, MessageTree.shouldUseImportDate(_client)));
                    }
                    public void widgetSelected(SelectionEvent selectionEvent) {
                        _navControl.view(SyndieURI.createSearch(forum, true, true, MessageTree.shouldUseImportDate(_client)));
                    }
                });
            }
        }
        
        GridData gd = (GridData)_unread.getLayoutData();
        boolean wasExcluded = gd.exclude;
        if (threads > 0) {
            _unread.setText(_translationRegistry.getText(T_UNREAD, "Unread: ") + sortedForums.size() + "/" + threads);
            gd.exclude = false;
            _unread.setVisible(true);
            if (wasExcluded)
                ((GridData)_version.getLayoutData()).horizontalSpan = ((GridData)_version.getLayoutData()).horizontalSpan - 1;
        } else {
            gd.exclude = true;
            _unread.setVisible(false);
            if (!wasExcluded)
                ((GridData)_version.getLayoutData()).horizontalSpan = ((GridData)_version.getLayoutData()).horizontalSpan + 1;
        }
        
        _root.layout(true);
    }
    
    private static final String T_UNREAD_ALL = "syndie.gui.statusbar.unread.all";
    private static final String T_UNREAD_BOOKMARKED = "syndie.gui.statusbar.unread.bookmarked";

    /**
     *  TODO the keys must include the hash prefix or they aren't unique;
     *  find a better way
     */
    private Map sortForums(Set forums) {
        Map rv = new TreeMap();
        for (Iterator iter = forums.iterator(); iter.hasNext(); ) {
            Hash forum = (Hash)iter.next();
            String name = _client.getChannelName(forum);
            if (name == null) name = "";
            name = name + " [" + forum.toBase64().substring(0,6) + "]";
            rv.put(name, forum);
        }
        return rv;
    }

    private boolean calcUnread(ReferenceNode node, Set forums) {
        boolean unread = false;
        if (node != null) {
            SyndieURI nodeURI = node.getURI();
            if (nodeURI != null) {
                if ( (node.getParent() == null) || (node.getParent().getURI() == null) )
                    unread = true;
                long msgId = _client.getMessageId(nodeURI.getScope(), nodeURI.getMessageId());
                long scopeId = _client.getMessageTarget(msgId);
                Hash scope = _client.getChannelHash(scopeId);
                if (scope != null)
                    forums.add(scope);
            }
            for (int i = 0; i < node.getChildCount(); i++)
                unread = calcUnread(node.getChild(i), forums) || unread;
        }
        return unread;
    }
    
    private String refreshPrivateMessages() {
        MenuItem items[] = _privMenu.getItems();
        for (int i = 0; i < items.length; i++)
            items[i].dispose();
        final List unreadMsgIds = _client.getPrivateMsgIds(false);
        for (int i = 0; i < unreadMsgIds.size(); i++) {
            long msgId = ((Long)unreadMsgIds.get(i)).longValue();
            long authorId = _client.getMessageAuthor(msgId);
            String author = _client.getChannelName(authorId);
            if (author == null) author = "";
            long when = _client.getMessageImportDate(msgId);
            String subject = _client.getMessageSubject(msgId);
            if (subject == null) subject = "";
            String str = Constants.getDate(when) + ": " + author + " - " + subject;
            MenuItem item = new MenuItem(_privMenu, SWT.PUSH);
            item.setText(str);
            item.setImage(ImageUtil.ICON_MSG_TYPE_PRIVATE);
            final SyndieURI uri = _client.getMessageURI(msgId);
            item.addSelectionListener(new SelectionListener() {
                public void widgetDefaultSelected(SelectionEvent selectionEvent) { _navControl.view(uri); }
                public void widgetSelected(SelectionEvent selectionEvent) { _navControl.view(uri); }
            });
        }
        
        if (unreadMsgIds.size() > 0) {
            new MenuItem(_privMenu, SWT.SEPARATOR);
            MenuItem item = new MenuItem(_privMenu, SWT.PUSH);
            item.setText(_translationRegistry.getText(T_PRIV_MARKALLREAD, "Mark all as read"));
            item.addSelectionListener(new SelectionListener() {
                public void widgetDefaultSelected(SelectionEvent selectionEvent) { markAllRead(); }
                public void widgetSelected(SelectionEvent selectionEvent) { markAllRead(); }
                private void markAllRead() {
                    for (int i = 0; i < unreadMsgIds.size(); i++) {
                        Long msgId = (Long)unreadMsgIds.get(i);
                        _client.markMessageRead(msgId.longValue());
                    }
                    _dataCallback.readStatusUpdated();
                }
            });
        }
        
        /*
        final List readMsgIds = _browser.getPrivateMsgIds(true);
        if (readMsgIds.size() > 0) {
            MenuItem sub = new MenuItem(_privMenu, SWT.CASCADE);
            sub.setText(_browser.getTranslationRegistry().getText(T_PRIV_READ, "Already read: ") + readMsgIds.size());
            Menu subm = new Menu(sub);
            sub.setMenu(subm);
            for (int i = 0; i < readMsgIds.size(); i++) {
                long msgId = ((Long)readMsgIds.get(i)).longValue();
                long authorId = _browser.getClient().getMessageAuthor(msgId);
                String author = _browser.getClient().getChannelName(authorId);
                if (author == null) author = "";
                long when = _browser.getClient().getMessageImportDate(msgId);
                String subject = _browser.getClient().getMessageSubject(msgId);
                if (subject == null) subject = "";
                String str = Constants.getDate(when) + ": " + author + " - " + subject;
                MenuItem item = new MenuItem(subm, SWT.PUSH);
                item.setText(str);
                item.setImage(ImageUtil.ICON_MSG_TYPE_PRIVATE);
                final SyndieURI uri = _browser.getClient().getMessageURI(msgId);
                item.addSelectionListener(new SelectionListener() {
                    public void widgetDefaultSelected(SelectionEvent selectionEvent) { _browser.view(uri); }
                    public void widgetSelected(SelectionEvent selectionEvent) { _browser.view(uri); }
                });
            }
        }
         */
        
        int unread = unreadMsgIds.size();
        //int read = readMsgIds.size();
        if (unread == 0) // && (read == 0) )
            return null;
        else
            return unread + ""; //+ "/" + read;
    }

    private static final String T_PRIV_MARKALLREAD = "syndie.gui.statusbar.priv.markallread";
    private static final String T_PRIV_READ = "syndie.gui.statusbar.priv.read";

    private int refreshPBE() {
        MenuItem items[] = _pbeMenu.getItems();
        for (int i = 0; i < items.length; i++)
            items[i].dispose();
        
        List uris = _client.getPBERequired(true, true);
        for (int i = 0; i < uris.size(); i++) {
            final SyndieURI uri = (SyndieURI)uris.get(i);
            StringBuilder buf = new StringBuilder();
            String name = _client.getChannelName(uri.getScope());
            if (name != null) {
                buf.append(name);
            } else {
                buf.append('[').append(uri.getScope().toBase64().substring(0, 6)).append(']');
            }
            if (uri.getMessageId() == null) {
                if (uri.getScope() == null)
                    continue;
                MenuItem item = new MenuItem(_pbeMenu, SWT.PUSH);
                item.setText(buf.toString());
                item.setImage(ImageUtil.ICON_MSG_TYPE_META);
                item.addSelectionListener(new SelectionListener() {
                    public void widgetDefaultSelected(SelectionEvent selectionEvent) { _navControl.view(uri); }
                    public void widgetSelected(SelectionEvent selectionEvent) { _navControl.view(uri); }
                });
            } else {
                MenuItem item = new MenuItem(_pbeMenu, SWT.PUSH);
                item.setText(buf.toString());
                item.setImage(ImageUtil.ICON_MSG_TYPE_NORMAL);
                item.addSelectionListener(new SelectionListener() {
                    public void widgetDefaultSelected(SelectionEvent selectionEvent) { _navControl.view(uri); }
                    public void widgetSelected(SelectionEvent selectionEvent) { _navControl.view(uri); }
                });
            }
        }
        return uris.size();
    }

    private int refreshPostponed() {
        MenuItem items[] = _postponeMenu.getItems();
        for (int i = 0; i < items.length; i++)
            items[i].dispose();

        Map msgs = _client.getResumeable();
        for (Iterator iter = msgs.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry entry = (Map.Entry)iter.next();
            final Long postponeId = (Long)entry.getKey();
            final Integer rev = (Integer)entry.getValue();
            
            String str = _translationRegistry.getText(T_POSTPONE_PREFIX, "Postponed on: ") + Constants.getDateTime(postponeId.longValue());
            MenuItem item = new MenuItem(_postponeMenu, SWT.PUSH);
            item.setText(str);
            item.addSelectionListener(new SelectionListener() {
                public void widgetDefaultSelected(SelectionEvent selectionEvent) {
                    _navControl.resumePost(postponeId.longValue(), rev.intValue());
                }
                public void widgetSelected(SelectionEvent selectionEvent) {
                    _navControl.resumePost(postponeId.longValue(), rev.intValue());
                }
            });
        }
        
        return msgs.size();
    }
    private static final String T_POSTPONE_PREFIX = "syndie.gui.statusbar.postponeprefix";
    
    private int refreshNewForums() {
        MenuItem items[] = _newForumMenu.getItems();
        for (int i = 0; i < items.length; i++)
            items[i].dispose();

        int active = 0;
        
        List channelIds = _client.getNewChannelIds();
        for (int i = 0; i < channelIds.size(); i++) {
            Long channelId = (Long)channelIds.get(i);
            //ChannelInfo info = _browser.getClient().getChannel(channelId.longValue());
            Hash channelHash = _client.getChannelHash(channelId.longValue());
            if (channelHash == null) {
                _ui.debugMessage("refreshing new forums, channelId " + channelId + " is not known?");
                continue;
            }
            int msgs = _client.countUnreadMessages(channelHash);
            if (msgs <= 0)
                continue; // only list new forums with content
            active++;
            
            MenuItem item = new MenuItem(_newForumMenu, SWT.PUSH);
            
            StringBuilder buf = new StringBuilder();
            String name = _client.getChannelName(channelId.longValue()); //info.getName();
            if (name != null) {
                buf.append(name);
            } else {
                buf.append('[').append(channelHash.toBase64().substring(0, 6)).append(']');
            }
            buf.append(" (").append(msgs).append(')');
            item.setText(buf.toString());
            item.setImage(ImageUtil.ICON_MSG_TYPE_META);
            final Hash scope = channelHash;
            item.addSelectionListener(new SelectionListener() {
                public void widgetDefaultSelected(SelectionEvent selectionEvent) {
                    _navControl.view(SyndieURI.createScope(scope));
                }
                public void widgetSelected(SelectionEvent selectionEvent) {
                    _navControl.view(SyndieURI.createScope(scope));
                }
            });
        }
        
        return active;
    }
    
    private void displayOnlineState(boolean online) {
        if (online) {
            _onlineState.setImage(ImageUtil.ICON_ONLINE);
            _onlineState.setToolTipText(_translationRegistry.getText(T_ONLINE, "Online: syndication is enabled"));
        } else {
            _onlineState.setImage(ImageUtil.ICON_OFFLINE);
            _onlineState.setToolTipText(_translationRegistry.getText(T_OFFLINE, "Offline: syndication is deferred"));
        }
        updateNextSync();
    }
    
    private void updateNextSync() {
        SyncManager mgr = SyncManager.getInstance(_client, _ui);
        final boolean isOnline = mgr.isOnline();
        final long nextSync = mgr.getNextSyncDate();
        Display.getDefault().asyncExec(new Runnable() {
            public void run() { setNextSync(nextSync, isOnline); }
        });
    }
    
    private static final String T_NEXT_SYNC_OFFLINE = "syndie.gui.statusbar.nextsync.offline";
    private static final String T_NEXT_SYNC_NOW = "syndie.gui.statusbar.nextsync.now";
    private static final String T_NEXT_SYNC_NONE = "syndie.gui.statusbar.nextsync.none";
    public void setNextSync(long when, boolean online) {
        if (!online) {
            _nextSyncDate.setText(_translationRegistry.getText(T_NEXT_SYNC_OFFLINE, "Deferred..."));
            _syncNow = false;
        } else if (when <= 0) {
            _nextSyncDate.setText(_translationRegistry.getText(T_NEXT_SYNC_NONE, "None scheduled"));
            _syncNow = false;
        } else if (when-System.currentTimeMillis() <= 0) {
            _nextSyncDate.setText(_translationRegistry.getText(T_NEXT_SYNC_NOW, "Now"));
            _syncNow = true;
        } else {
            long delay = when-System.currentTimeMillis();
            if (delay < 60*1000)
                _nextSyncDate.setText("<" + 1 + "m");
            else if (delay < 120*60*1000)
                _nextSyncDate.setText(delay/(60*1000) + "m");
            else if (delay < 48*60*60*1000)
                _nextSyncDate.setText(delay/(60*60*1000) + "h");
            else
                _nextSyncDate.setText(delay/(24*60*60*1000) + "d");
            //_nextSyncDate.setText(DataHelper.formatDuration()); // 3h, 39m, 2d, etc
            _syncNow = false;
        }
        _root.layout(true);
    }
    
    private void initDnD() {
        int ops = DND.DROP_COPY | DND.DROP_LINK;
        Transfer transfer[] = new Transfer[] { TextTransfer.getInstance() };
        DropTarget target = new DropTarget(_bookmark, ops);
        target.setTransfer(transfer);
        target.addDropListener(new DropTargetListener() {
            public void dragEnter(DropTargetEvent evt) {
                // we can take the element
                evt.detail = evt.operations | DND.DROP_COPY;
            }
            public void dragLeave(DropTargetEvent evt) {}
            public void dragOperationChanged(DropTargetEvent evt) {}
            public void dragOver(DropTargetEvent evt) {}
            public void drop(DropTargetEvent evt) {
                if (evt.data == null) {
                    evt.detail = DND.DROP_NONE;
                    return;
                } else {
                    BookmarkDnD bookmark = new BookmarkDnD();
                    bookmark.fromString(evt.data.toString());
                    if (bookmark.uri != null) { // parsed fine
                        NymReferenceNode parent = getParent(_ui, _translationRegistry, _bookmarkControl, bookmark);
                        long parentGroupId = -1;
                        if (parent != null)
                            parentGroupId = parent.getGroupId();
                        NymReferenceNode node = new NymReferenceNode(bookmark.name, bookmark.uri, bookmark.desc, -1, -1, parentGroupId, 0, false, false, false);
                        _bookmarkControl.bookmark(node, true);
                        _ui.debugMessage("bookmark the target w/ parentGroupId=" + parentGroupId + " resulting in " + node.getGroupId());
                    } else { // wasn't in bookmark syntax, try as a uri
                        String str = evt.data.toString();
                        try {
                            SyndieURI uri = new SyndieURI(str);
                            _bookmarkControl.bookmark(uri);
                        } catch (URISyntaxException use) {
                            _ui.debugMessage("invalid uri: " + str, use);
                        }
                    }
                }
            }
            public void dropAccept(DropTargetEvent evt) {}
        });
    }
    
    /**
     * select the bookmark folder to stash the bookmark in, creating a new one if
     * necessary.  the folder is named "$date messages"
     */
    static NymReferenceNode getParent(UI ui, TranslationRegistry trans, BookmarkControl bookmarkControl, BookmarkDnD bookmark) {
        if (bookmark.uri.getMessageId() == null) return null;
        
        String wantedName = Constants.getDate(System.currentTimeMillis()) + " " + trans.getText(T_BOOKMARK_SUFFIX_MSGS, "messages");
        
        List bookmarks = bookmarkControl.getBookmarks();
        for (int i = 0; i < bookmarks.size(); i++) {
            NymReferenceNode node = (NymReferenceNode)bookmarks.get(i);
            if (wantedName.equals(node.getName()))
                return node;
        }
        // does not exist.  create it
        NymReferenceNode node = new NymReferenceNode(wantedName, null, "", -1, -1, -1, 0, false, false, false);
        bookmarkControl.bookmark(node, false);
        ui.debugMessage("created parent for the bookmark: " + wantedName + " as groupId=" + node.getGroupId());
        return node;
    }
    
    private static final String T_BOOKMARK_SUFFIX_MSGS = "syndie.gui.statusbar.bookmarksuffixmsgs";
    
    private static final String T_NEXT_SYNC = "syndie.gui.statusbar.nextsync";
    private static final String T_NEWFORUM = "syndie.gui.statusbar.newforum";
    private static final String T_UNREAD = "syndie.gui.statusbar.newmsg";
    private static final String T_PBE = "syndie.gui.statusbar.pbe";
    private static final String T_PRIV = "syndie.gui.statusbar.priv";
    private static final String T_POSTPONE = "syndie.gui.statusbar.postpone";
    private static final String T_BOOKMARK = "syndie.gui.statusbar.bookmark";
    public void translate(TranslationRegistry registry) {
        _nextSyncLabel.setText(registry.getText(T_NEXT_SYNC, "Next sync:"));
        _bookmark.setText(registry.getText(T_BOOKMARK, "Bookmark!"));
        _root.layout(true);
    }
    
    public void applyTheme(Theme theme) {
        _bookmark.setFont(theme.FINEPRINT_FONT);
        _nextSyncDate.setFont(theme.FINEPRINT_FONT);
        _nextSyncLabel.setFont(theme.FINEPRINT_FONT);
        _onlineState.setFont(theme.FINEPRINT_FONT);
        _version.setFont(theme.FINEPRINT_FONT);
        
        _newForum.setFont(theme.FINEPRINT_FONT);
        _unread.setFont(theme.FINEPRINT_FONT);
        _pbe.setFont(theme.FINEPRINT_FONT);
        _priv.setFont(theme.FINEPRINT_FONT);
        _postpone.setFont(theme.FINEPRINT_FONT);        
    }
}
