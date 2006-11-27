package syndie.gui;

import java.util.List;
import net.i2p.data.Hash;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import syndie.Constants;
import syndie.data.ChannelInfo;
import syndie.data.MessageInfo;
import syndie.data.SyndieURI;
import syndie.db.CommandImpl;
import syndie.db.DBClient;
import syndie.db.UI;

/**
 *
 */
public class BrowseForum implements MessageTree.MessageTreeListener, Translatable {
    private DBClient _client;
    private Composite _parent;
    private SashForm _root;
    private Composite _top;
    private Composite _meta;
    private ImageCanvas _metaAvatar;
    private Link _metaName;
    private Menu _metaNameMenu;
    private MenuItem _metaNameMenuBookmark;
    private MenuItem _metaNameMenuMarkRead;
    private MenuItem _metaNameMenuDeleteRead;
    private MenuItem _metaNameMenuCopyURI;
    private MenuItem _metaNameMenuDeleteAll;
    private MenuItem _metaNameMenuBan;
    private Label _metaIconManageable;
    private Label _metaIconPostable;
    private Label _metaIconReferences;
    private Label _metaIconAdmins;
    private MessageTree _tree;
    private MessageTree.MessageTreeListener _listener;
    private MessagePreview _preview;
    private Hash _scope;
    private UI _ui;
    private BrowserControl _browser;
    
    public BrowseForum(Composite parent, BrowserControl browser, MessageTree.MessageTreeListener lsnr) {
        _browser = browser;
        _client = browser.getClient();
        _parent = parent;
        _listener = lsnr;
        _ui = browser.getUI();
        _ui.debugMessage("initializing browse");
        initComponents();
        _ui.debugMessage("browse initialized");
    }
    
    public Control getControl() { return _root; }
    
    private void initComponents() {
        _root = new SashForm(_parent, SWT.VERTICAL);
        _root.SASH_WIDTH = 3;
        _root.setBackground(ColorUtil.getColor("gray", null));
        
        _top = new Composite(_root, SWT.NONE);
        _top.setLayout(new GridLayout(1, true));
        _meta = new Composite(_top, SWT.NONE);
        _meta.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
        _meta.setLayout(new GridLayout(6, false));

        _metaAvatar = new ImageCanvas(_meta, false);
        _metaAvatar.forceSize(20, 20);
        _metaAvatar.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, false, false));
        _metaName = new Link(_meta, SWT.NONE);
        _metaName.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, true, false));
        _metaName.setText("");
        _metaIconManageable = new Label(_meta, SWT.NONE);
        _metaIconPostable = new Label(_meta, SWT.NONE);
        _metaIconReferences = new Label(_meta, SWT.NONE);
        _metaIconAdmins = new Label(_meta, SWT.NONE);
        _metaIconManageable.setLayoutData(new GridData(20, 20));
        _metaIconPostable.setLayoutData(new GridData(20, 20));
        _metaIconReferences.setLayoutData(new GridData(20, 20));
        _metaIconAdmins.setLayoutData(new GridData(20, 20));
        //_metaIconManageable.setEnabled(false);
        //_metaIconPostable.setEnabled(false);
        //_metaIconReferences.setEnabled(false);
        //_metaIconAdmins.setEnabled(false);
        _metaIconManageable.setImage(ImageUtil.ICON_BROWSE_MANAGEABLE);
        _metaIconPostable.setImage(ImageUtil.ICON_BROWSE_POSTABLE);
        _metaIconReferences.setImage(ImageUtil.ICON_BROWSE_REFS);
        _metaIconAdmins.setImage(ImageUtil.ICON_BROWSE_ADMINS);
        
        _metaNameMenu = new Menu(_metaName);
        _metaNameMenuBookmark = new MenuItem(_metaNameMenu, SWT.PUSH);
        _metaNameMenuMarkRead = new MenuItem(_metaNameMenu, SWT.PUSH);
        _metaNameMenuDeleteRead = new MenuItem(_metaNameMenu, SWT.PUSH);
        _metaNameMenuCopyURI = new MenuItem(_metaNameMenu, SWT.PUSH);
        _metaNameMenuDeleteAll = new MenuItem(_metaNameMenu, SWT.PUSH);
        _metaNameMenuBan = new MenuItem(_metaNameMenu, SWT.PUSH);
        
        _metaNameMenuBookmark.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent selectionEvent) { _browser.bookmark(SyndieURI.createScope(_scope)); }
            public void widgetSelected(SelectionEvent selectionEvent) { _browser.bookmark(SyndieURI.createScope(_scope)); }
        });
        
        _metaName.setMenu(_metaNameMenu);
        _metaName.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent selectionEvent) { _metaNameMenu.setVisible(true); }
            public void widgetSelected(SelectionEvent selectionEvent) { _metaNameMenu.setVisible(true); }
        });
        
        _metaIconManageable.addMouseListener(new MouseListener() {
            public void mouseDoubleClick(MouseEvent mouseEvent) {}
            public void mouseDown(MouseEvent mouseEvent) { manage(); }
            public void mouseUp(MouseEvent mouseEvent) {}
        });
        _metaIconManageable.addTraverseListener(new TraverseListener() {
            public void keyTraversed(TraverseEvent evt) {
                if (evt.detail == SWT.TRAVERSE_RETURN) manage();
            }
        });
        
        _metaIconPostable.addMouseListener(new MouseListener() {
            public void mouseDoubleClick(MouseEvent mouseEvent) {}
            public void mouseDown(MouseEvent mouseEvent) { post(); }
            public void mouseUp(MouseEvent mouseEvent) {}
        });
        _metaIconPostable.addTraverseListener(new TraverseListener() {
            public void keyTraversed(TraverseEvent evt) {
                if (evt.detail == SWT.TRAVERSE_RETURN) post();
            }
        });
        
        _browser.getUI().debugMessage("browseForum.initialize: creating tree");
        _tree = new MessageTree(_browser, _top, this);
        _tree.getControl().setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
        _browser.getUI().debugMessage("browseForum.initialize: creating preview");
        _preview = new MessagePreview(_browser, _root);
        _browser.getUI().debugMessage("browseForum.initialize: preview created");
        _root.setWeights(new int[] { 80, 20 });
        _root.setMaximizedControl(_top);
        
        _browser.getTranslationRegistry().register(this);
    }

    public void dispose() {
        _browser.getTranslationRegistry().unregister(this);
        _preview.dispose();
        _tree.dispose();
    }
    
    private void updateMetadata(SyndieURI uri) {
        // if the target channel has changed, update the metadata fields
        Hash scope = null;
        if (uri.isChannel())
            scope = uri.getScope();
        else if (uri.isSearch())
            scope = uri.getHash("scope");
        
        if ( ( (scope == null) && (_scope == null) ) || ( (scope != null) && (scope.equals(_scope)) ) )
            return; // same as before
        
        ChannelInfo info = null;
        if (scope != null) {
            long chanId = _client.getChannelId(scope);
            info = _client.getChannel(chanId);
            
            if ( (chanId >= 0) && (uri.getMessageId() != null) ) {
                MessageInfo msg = _client.getMessage(chanId, uri.getMessageId());
                if (msg != null) {
                    scope = msg.getTargetChannel();
                    info = _client.getChannel(msg.getTargetChannelId());
                }
            }
        }
        
        if (info != null) {
            String name = info.getName();
            if (name == null) name = scope.toBase64().substring(0,6);
            StringBuffer buf = new StringBuffer();
            buf.append("<a>");
            buf.append(CommandImpl.strip(name, "\n\r\t<>", ' '));
            buf.append("</a>: ");
            String desc = info.getDescription();
            if (desc == null) desc = scope.toBase64();
            buf.append(desc);
            _metaName.setText(buf.toString());
            boolean manage = (_client.getNymKeys(scope, Constants.KEY_FUNCTION_MANAGE).size() > 0);
            _metaIconManageable.setVisible(manage);
            boolean post = manage || info.getAllowPublicPosts() || (_client.getNymKeys(scope, Constants.KEY_FUNCTION_POST).size() > 0);
            _metaIconPostable.setVisible(post);
            List refs = info.getReferences();
            boolean inclRefs = (refs != null) && (refs.size() > 0);
            _metaIconReferences.setVisible(inclRefs);
            
            boolean admins = (info.getAuthorizedManagers().size() > 0) || (info.getAuthorizedPosters().size() > 0);
            _metaIconAdmins.setVisible(admins);
        } else {
            _metaName.setText(_browser.getTranslationRegistry().getText(T_META_NAME_MULTIPLE, "multiple forums selected"));
            _metaIconManageable.setVisible(false);
            _metaIconPostable.setVisible(false);
            _metaIconReferences.setVisible(false);
            _metaIconAdmins.setVisible(false);
        }
        _scope = scope;
        _meta.layout(true, true);
    }

    private static final String T_META_NAME_MULTIPLE = "syndie.gui.browseforum.meta.name.multiple";
    
    public void setFilter(SyndieURI filter) { 
        _ui.debugMessage("setting filter...");
        _tree.setFilter(filter);
        _ui.debugMessage("applying filter...");
        _tree.applyFilter();
        _ui.debugMessage("filter applied");
    }
    
    public void messageSelected(MessageTree tree, SyndieURI uri, boolean toView) {
        //if (toView)
        //    _shell.setVisible(false);
        _ui.debugMessage("message selected: " + uri);
        preview(uri);
        if (_listener != null)
            _listener.messageSelected(tree, uri, toView);
    }

    public void filterApplied(MessageTree tree, SyndieURI searchURI) {
        // update the metadata line if the scope has changed
        updateMetadata(searchURI);
        if (_listener != null)
            _listener.filterApplied(tree, searchURI);
    }
    
    void preview(SyndieURI uri) { 
        _tree.select(uri);
        _root.setMaximizedControl(null);
        // dont update the metadata, since a message may be selected that isn't strictly
        // in this forum (eg its in another forum, but uses something in the filtered messages
        // as a parent).  when viewing a forum, the forum metadata at the top stays the same
        // (unless you edit the filter to view another forum)
        //_ui.debugMessage("updating metadata in preview...");
        //updateMetadata(uri);
        _ui.debugMessage("previewing...");
        _preview.preview(uri);
        _ui.debugMessage("preview complete");
    }
    
    private void post() {
        if (_browser != null) {
            _browser.getUI().debugMessage("posting...");
            _browser.view(_browser.createPostURI(_scope, null));
        }
    }
    
    private void manage() {
        if (_browser != null) {
            _browser.view(_browser.createManageURI(_scope));
        }
    }

    private static final String T_MANAGEABLE_TOOLTIP = "syndie.gui.browseforum.manageable";
    private static final String T_POSTABLE_TOOLTIP = "syndie.gui.browseforum.postable";
    private static final String T_REFS_TOOLTIP = "syndie.gui.browseforum.refs";
    private static final String T_ADMINS_TOOLTIP = "syndie.gui.browseforum.admins";

    private static final String T_BOOKMARK = "syndie.gui.browseforum.bookmark";
    private static final String T_MARKALLREAD = "syndie.gui.browseforum.markallread";
    private static final String T_DELETEREAD = "syndie.gui.browseforum.deleteread";
    private static final String T_COPYURI = "syndie.gui.browseforum.copyuri";
    private static final String T_DELETEALL = "syndie.gui.browseforum.deleteall";
    private static final String T_BAN = "syndie.gui.browseforum.ban";
    
    public void translate(TranslationRegistry registry) {
        _metaIconManageable.setToolTipText(registry.getText(T_MANAGEABLE_TOOLTIP, "You can manage this forum"));
        _metaIconPostable.setToolTipText(registry.getText(T_POSTABLE_TOOLTIP, "You can post in this forum"));
        _metaIconReferences.setToolTipText(registry.getText(T_REFS_TOOLTIP, "This forum has published references"));
        _metaIconAdmins.setToolTipText(registry.getText(T_ADMINS_TOOLTIP, "This forum has specific admins"));

        _metaNameMenuBookmark.setText(registry.getText(T_BOOKMARK, "Bookmark this forum"));
        _metaNameMenuMarkRead.setText(registry.getText(T_MARKALLREAD, "Mark all messages read"));
        _metaNameMenuDeleteRead.setText(registry.getText(T_DELETEREAD, "Delete read messages"));
        _metaNameMenuCopyURI.setText(registry.getText(T_COPYURI, "Copy forum URI"));
        _metaNameMenuDeleteAll.setText(registry.getText(T_DELETEALL, "Delete all messages"));
        _metaNameMenuBan.setText(registry.getText(T_BAN, "Ban this forum"));
    }
}
