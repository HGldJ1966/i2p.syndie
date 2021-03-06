package syndie.gui;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Properties;
import net.i2p.data.Base64;
import net.i2p.data.Hash;
import net.i2p.data.SessionKey;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Text;
import syndie.Constants;
import syndie.Version;
import syndie.data.BugConfig;
import syndie.data.ChannelInfo;
import syndie.data.ReferenceNode;
import syndie.data.SyndieURI;
import syndie.db.CommandImpl;
import syndie.db.DBClient;
import syndie.db.MessageCreator;
import syndie.db.MessageCreatorDirect;
import syndie.db.MessageCreatorSource;
import syndie.db.UI;
import syndie.html.WebRipRunner;

/**
 *
 */
class BugReport extends BaseComponent implements Themeable, Translatable {
    private Composite _parent;
    private BanControl _dataControl;
    private DataCallback _dataCallback;
    private NavigationControl _navControl;
    private ScrolledComposite _scroll;
    private Composite _root;
    private SyndieURI _uri;
    
    private Label _componentLabel;
    private Button _component;
    private Menu _componentMenu;
    private Label _typeLabel;
    private Combo _type;
    private ArrayList _types;
    private Label _severityLabel;
    private Combo _severity;
    private ArrayList _severities;
    private Label _osLabel;
    private Text _os;
    private Label _jvmLabel;
    private Text _jvm;
    private Label _swtLabel;
    private Text _swt;
    private Label _syndieLabel;
    private Text _syndie;
    private Label _hsqldbLabel;
    private Text _hsqldb;
    private Label _attachmentsLabel;
    private Combo _attachments;
    private Button _attachmentsAdd;
    private Button _attachmentsRemove;
    private Label _summaryLabel;
    private Text _summary;
    private Group _logGroup;
    private Text _log;
    
    private Button _post;
    private Label _targetLabel;
    private Combo _target;
    private Button _asPrivate;
    private Label _signAsLabel;
    private Combo _signAs;
    
    private String _selectedComponentId;
    private String _selectedTypeId;
    private String _selectedSeverityId;
    private ArrayList _attachmentFiles;
    private ArrayList _targetChans;
    private ArrayList _signAsChans;
    
    /** 
     * if the user knows this forum (and its one they can post to), it'll be their default 
     * target (which is good, since this is the standard bug report forum)
     */
    private static final String STANDARD_BUGREPORT_FORUM = "eu61~moznLTNsOizxDjAsJpxBIm1WC1s4b1hWDy8gYQ=";
    
    public BugReport(DBClient client, UI ui, ThemeRegistry themes, TranslationRegistry trans, DataCallback callback, NavigationControl navControl, Composite parent, SyndieURI uri) {
        super(client, ui, themes, trans);
        _dataCallback = callback;
        _navControl = navControl;
        _parent = parent;
        _uri = uri;
        _attachmentFiles = new ArrayList();
        _targetChans = new ArrayList();
        _signAsChans = new ArrayList();
        _severities = new ArrayList();
        _types = new ArrayList();
        initComponents();
    }
    
    private void initComponents() {
        _scroll = new ScrolledComposite(_parent, SWT.H_SCROLL | SWT.V_SCROLL);
        _scroll.setAlwaysShowScrollBars(false);
        _scroll.setExpandHorizontal(true);
        _scroll.setExpandVertical(true);
        _root = new Composite(_scroll, SWT.NONE);
        _root.setLayout(new GridLayout(4, false));
        _scroll.setContent(_root);
        
        _componentLabel = new Label(_root, SWT.NONE);
        _componentLabel.setLayoutData(new GridData(GridData.END, GridData.CENTER, false, false));
        _component = new Button(_root, SWT.DROP_DOWN | SWT.READ_ONLY);
        _component.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 3, 1));
        _componentMenu = new Menu(_component);
        _component.setMenu(_componentMenu);
        _component.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent selectionEvent) { _componentMenu.setVisible(true); }
            public void widgetSelected(SelectionEvent selectionEvent) { _componentMenu.setVisible(true); }
        });
        
        _typeLabel = new Label(_root, SWT.NONE);
        _typeLabel.setLayoutData(new GridData(GridData.END, GridData.CENTER, false, false));
        _type = new Combo(_root, SWT.DROP_DOWN | SWT.READ_ONLY);
        _type.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
        _type.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent selectionEvent) { _selectedTypeId = (String)_types.get(_type.getSelectionIndex()); }
            public void widgetSelected(SelectionEvent selectionEvent) { _selectedTypeId = (String)_types.get(_type.getSelectionIndex()); }
        });
        
        _severityLabel = new Label(_root, SWT.NONE);
        _severityLabel.setLayoutData(new GridData(GridData.END, GridData.CENTER, false, false));
        _severity = new Combo(_root, SWT.DROP_DOWN | SWT.READ_ONLY);
        GridData gd = new GridData(GridData.FILL, GridData.FILL, true, false);
        gd.widthHint = 100;
        _severity.setLayoutData(gd);
        _severity.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent selectionEvent) { _selectedSeverityId = (String)_severities.get(_severity.getSelectionIndex()); }
            public void widgetSelected(SelectionEvent selectionEvent) { _selectedSeverityId = (String)_severities.get(_severity.getSelectionIndex()); }
        });
        
        _signAsLabel = new Label(_root, SWT.NONE);
        _signAsLabel.setLayoutData(new GridData(GridData.END, GridData.CENTER, false, false));
        _signAs = new Combo(_root, SWT.DROP_DOWN | SWT.READ_ONLY);
        gd = new GridData(GridData.FILL, GridData.CENTER, false, false);
        gd.widthHint = 100;
        _signAs.setLayoutData(gd);
        
        _attachmentsLabel = new Label(_root, SWT.NONE);
        _attachmentsLabel.setLayoutData(new GridData(GridData.END, GridData.CENTER, false, false));
        
        Composite attach = new Composite(_root, SWT.NONE);
        attach.setLayout(new GridLayout(3, false));
        attach.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false));
        
        _attachments = new Combo(attach, SWT.DROP_DOWN | SWT.READ_ONLY);
        _attachments.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
        
        _attachmentsAdd = new Button(attach, SWT.PUSH);
        _attachmentsAdd.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, false, false));
        _attachmentsRemove = new Button(attach, SWT.PUSH);
        _attachmentsRemove.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, false, false));
        
        _attachmentsAdd.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent selectionEvent) { addAttachment(); }
            public void widgetSelected(SelectionEvent selectionEvent) { addAttachment(); }
        });
        _attachmentsRemove.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent selectionEvent) { removeAttachment(); }
            public void widgetSelected(SelectionEvent selectionEvent) { removeAttachment(); }
        });
        
        _summaryLabel = new Label(_root, SWT.NONE);
        _summaryLabel.setLayoutData(new GridData(GridData.END, GridData.CENTER, false, false));
        _summary = new Text(_root, SWT.SINGLE | SWT.BORDER);
        _summary.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 3, 1));
        
        _asPrivate = new Button(_root, SWT.CHECK);
        _asPrivate.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, false, false, 4, 1));
        
        _logGroup = new Group(_root, SWT.SHADOW_ETCHED_IN);
        _logGroup.setLayout(new FillLayout());
        _logGroup.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true, 4, 1));
        _log = new Text(_logGroup, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.WRAP);

        Composite revRow = new Composite(_root, SWT.NONE);
        revRow.setLayout(new GridLayout(10, false));
        revRow.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 4, 1));
        
        _syndieLabel = new Label(revRow, SWT.NONE);
        _syndieLabel.setLayoutData(new GridData(GridData.END, GridData.CENTER, false, false));
        _syndie = new Text(revRow, SWT.SINGLE | SWT.BORDER);
        gd = new GridData(GridData.FILL, GridData.FILL, true, false);
        gd.widthHint = 30;
        _syndie.setLayoutData(gd);
        
        _osLabel = new Label(revRow, SWT.NONE);
        _osLabel.setLayoutData(new GridData(GridData.END, GridData.CENTER, false, false));
        _os = new Text(revRow, SWT.SINGLE | SWT.BORDER);
        gd = new GridData(GridData.FILL, GridData.FILL, true, false);
        gd.widthHint = 50;
        _os.setLayoutData(gd);
        
        _jvmLabel = new Label(revRow, SWT.NONE);
        _jvmLabel.setLayoutData(new GridData(GridData.END, GridData.CENTER, false, false));
        _jvm = new Text(revRow, SWT.SINGLE | SWT.BORDER);
        gd = new GridData(GridData.FILL, GridData.FILL, true, false);
        gd.widthHint = 50;
        _jvm.setLayoutData(gd);
        
        _swtLabel = new Label(revRow, SWT.NONE);
        _swtLabel.setLayoutData(new GridData(GridData.END, GridData.CENTER, false, false));
        _swt = new Text(revRow, SWT.SINGLE | SWT.BORDER);
        gd = new GridData(GridData.FILL, GridData.FILL, true, false);
        gd.widthHint = 25;
        _swt.setLayoutData(gd);

        _hsqldbLabel = new Label(revRow, SWT.NONE);
        _hsqldbLabel.setLayoutData(new GridData(GridData.END, GridData.CENTER, false, false));
        _hsqldb = new Text(revRow, SWT.SINGLE | SWT.BORDER);
        gd = new GridData(GridData.FILL, GridData.FILL, true, false);
        gd.widthHint = 25;
        _hsqldb.setLayoutData(gd);

        Composite action = new Composite(_root, SWT.NONE);
        action.setLayout(new GridLayout(3, false));
        action.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false, 4, 1));
        
        _post = new Button(action, SWT.PUSH);
        _post.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
        _post.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent selectionEvent) { postReport(); }
            public void widgetSelected(SelectionEvent selectionEvent) { postReport(); }
        });
        
        _targetLabel = new Label(action, SWT.NONE);
        _targetLabel.setLayoutData(new GridData(GridData.END, GridData.CENTER, false, false));
        
        _target = new Combo(action, SWT.DROP_DOWN | SWT.READ_ONLY);
        gd = new GridData(GridData.FILL, GridData.FILL, false, false);
        gd.widthHint = 250;
        _target.setLayoutData(gd);
        _target.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent selectionEvent) { targetSelected(); }
            public void widgetSelected(SelectionEvent selectionEvent) { targetSelected(); }
        });
        
        _translationRegistry.register(this);
        _themeRegistry.register(this);
        
        loadConfig();
        _root.layout(true, true);
        _scroll.setMinSize(_root.computeSize(SWT.DEFAULT, SWT.DEFAULT));
    }
    
    private void savePrefs(String os, String jvm, String swt, String hsqldb) {
        Properties prefs = _client.getNymPrefs();
        prefs.setProperty("bugreport.os", os);
        prefs.setProperty("bugreport.jvm", jvm);
        prefs.setProperty("bugreport.swt", swt);
        prefs.setProperty("bugreport.hsqldb", hsqldb);
        _client.setNymPrefs(prefs);
    }

    private void loadPrefs() {
        String defOS = System.getProperty("os.name") + " " + System.getProperty("os.version") + " " + System.getProperty("os.arch");
        String defJVM = System.getProperty("java.vm.name") + " " + System.getProperty("java.vm.version");
        String defSWT = SWT.getPlatform() + "-" + SWT.getVersion();
        String defHSQLDB = _client.getHsqldbVersion();
        
        Properties prefs = _client.getNymPrefs();
        _os.setText(prefs.getProperty("bugreport.os", defOS));
        _jvm.setText(prefs.getProperty("bugreport.jvm", defJVM));
        _swt.setText(prefs.getProperty("bugreport.swt", defSWT));
        _hsqldb.setText(prefs.getProperty("bugreport.hsqldb", defHSQLDB));
    }
    
    private void postReport() {
        final String os = _os.getText().trim();
        final String jvm = _jvm.getText().trim();
        final String swt = _swt.getText().trim();
        final String hsqldb = _hsqldb.getText().trim();
        
        savePrefs(os, jvm, swt, hsqldb);
                
        final MessageCreator creator = new MessageCreatorDirect(new MessageCreatorSource() {
            public MessageCreator.ExecutionListener getListener() {
                return new MessageCreator.ExecutionListener() {
                    public void creationComplete(MessageCreator exec, SyndieURI uri, String errors, boolean successful, SessionKey replySessionKey, byte[] replyIV, File msg) {
                        if (successful) {
                            boolean ok = exec.importCreated(_client, _ui, uri, msg, replyIV, replySessionKey, getPassphrase());
                            if (ok) {
                                _navControl.view(uri);
                                _navControl.unview(_uri);
                                _dataCallback.messageImported();
                            } else {
                                MessageBox box = new MessageBox(_root.getShell(), SWT.ICON_ERROR | SWT.OK);
                                box.setMessage(errors);
                                box.setText(getText("Error posting report"));
                                box.open();
                            }
                        } else {
                            MessageBox box = new MessageBox(_root.getShell(), SWT.ICON_ERROR | SWT.OK);
                            box.setMessage(errors);
                            box.setText(getText("Error posting report"));
                            box.open();
                        }
                        exec.cleanup();
                    }
                    
                };
            }
            public DBClient getClient() { return _client; }
            public UI getUI() { return _ui; }
            public Hash getAuthor() {
                int idx = _signAs.getSelectionIndex();
                if (idx >= _signAsChans.size()) {
                    // create a new keypair?
                    return null;
                } else {
                    return (Hash)_signAsChans.get(idx);
                }
            }
            public Hash getSignAs() { return null; }
            public boolean getAuthorHidden() { return false; }
            public String getPageTitle(int page) { return null; }

            public Hash getTarget() {
                int idx = _target.getSelectionIndex();
                if (idx < _targetChans.size())
                    return (Hash)_targetChans.get(idx);
                else
                    return null;
            }

            public int getPageCount() { return 1; }

            public String getPageContent(int page) { 
                StringBuilder rv = new StringBuilder();
                rv.append("OS: " + os + "\n");
                rv.append("JVM: " + jvm + "\n");
                rv.append("SWT: " + swt + "\n");
                rv.append("HSQLDB: " + hsqldb + "\n");
        
                rv.append("\n");
                rv.append(_log.getText());
                return rv.toString();
            }
            public String getPageType(int page) { return "text/plain"; }
            public java.util.List getAttachmentNames() {
                ArrayList rv = new ArrayList();
                for (int i = 0; i < _attachmentFiles.size(); i++)
                    rv.add(((File)_attachmentFiles.get(i)).getName());
                return rv;
            }

            public java.util.List getAttachmentTypes() {
                ArrayList rv = new ArrayList();
                for (int i = 0; i < _attachmentFiles.size(); i++) {
                    String name = ((File)_attachmentFiles.get(i)).getName();
                    String type = WebRipRunner.guessContentType(name);
                    rv.add(type);
                }
                return rv;
            }

            public byte[] getAttachmentData(int attachmentIndex) {
                File f = (File)_attachmentFiles.get(attachmentIndex-1); // 1-indexed
                byte buf[] = new byte[(int)f.length()];
                int off = 0;
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(f);
                    int remaining = buf.length;
                    int read = 0;
                    while ( (remaining > 0) && ((read = fis.read(buf, off, remaining)) != -1) ) {
                        off += read;
                        remaining -= read;
                    }
                    return buf;
                } catch (IOException ioe) {
                    _ui.errorMessage("Error reading attachment", ioe);
                    return null;
                } finally {
                    if (fis != null) try { fis.close(); } catch (IOException ioe) {}
                }
            }

            public String getSubject() { return _summary.getText(); }
            public boolean getPrivacyPBE() { return false; }
            public String getPassphrase() { return null; }
            public String getPassphrasePrompt() { return null; }
            public boolean getPrivacyPublic() { return !_asPrivate.getSelection(); }
            public String getAvatarUnmodifiedFilename() { return null; }
            public byte[] getAvatarModifiedData() { return null; }
            public boolean getPrivacyReply() { return _asPrivate.getSelection(); }
            public String[] getPublicTags() {
                ArrayList tags = new ArrayList();
                tags.add("bugreport");
                tags.add("syndie." + CommandImpl.strip(_syndie.getText()));
                tags.add("severity." + _severities.get(_severity.getSelectionIndex()));
                if (_selectedComponentId != null)
                    tags.add("component." + _selectedComponentId);
                tags.add("type." + _types.get(_type.getSelectionIndex()));
                return (String[])tags.toArray(new String[0]);
            }

            public String[] getPrivateTags() { return new String[0]; }
            public java.util.List getReferenceNodes() { return new ArrayList(); }
            public int getParentCount() { return 0; }
            public SyndieURI getParent(int depth) { return null; }
            public String getExpiration() { return null; }
            public boolean getForceNewThread() { return false; }
            public boolean getRefuseReplies() { return false; }
            public java.util.List getCancelURIs() { return new ArrayList(); }
        });
        creator.execute();
    }
    
    
    private void targetSelected() {
        /*
        SyndieURI target = _browser.getClient().getBugConfig().getTargetScope();
        long targetId = _browser.getClient().getChannelId(target.getScope());
        
        if ( (targetId >= 0) && (_target.getSelectionIndex() == 0) ) {
            _asPrivate.setEnabled(true);
        } else {
            _asPrivate.setEnabled(false);
            _asPrivate.setSelection(false);
        }
         */
    }
    
    private FileDialog _fileDialog;
    private void addAttachment() {
        if (_fileDialog == null) {
            _fileDialog = new FileDialog(_root.getShell(), SWT.OPEN | SWT.MULTI);
            _fileDialog.setText(getText("File to attach"));
        }
        if (_fileDialog.open() == null) return;
        String selected[] = _fileDialog.getFileNames();
        String base = _fileDialog.getFilterPath();
        for (int i = 0; i < selected.length; i++) {
            File cur = null;
            if (base == null)
                cur = new File(selected[i]);
            else
                cur = new File(base, selected[i]);
            if (cur.exists() && cur.isFile() && cur.canRead()) {
                if (!_attachmentFiles.contains(cur)) {
                    _attachmentFiles.add(cur);
                    _attachments.add(cur.getName());
                    _attachments.select(_attachments.getItemCount()-1);
                }
            }
        }
    }
    private void removeAttachment() {
        int idx = _attachments.getSelectionIndex();
        if (idx < 0) return;
        _attachmentFiles.remove(idx);
        _attachments.remove(idx);
        if (_attachments.getItemCount() > 0)
            _attachments.select(_attachments.getItemCount()-1);
    }
    
    private void loadConfig() {
        BugConfig cfg = _client.getBugConfig();
        rebuildComponentMenu(cfg);
        ReferenceNode def = cfg.getComponentDefault();
        if (def != null)
            _component.setText(getText(def.getName(), def.getDescription()));
        
        _severity.setRedraw(false);
        for (int i = 0; i < cfg.getSeverityCount(); i++) {
            String id = cfg.getSeverityId(i);
            String name = cfg.getSeverityName(i);
            _severity.add(getText(id, name));
            _severities.add(id);
        }
        if (cfg.getSeverityDefaultIndex() >= 0) {
            _severity.select(cfg.getSeverityDefaultIndex());
            _selectedSeverityId = (String)_severities.get(cfg.getSeverityDefaultIndex());
        }
        _severity.setRedraw(true);
        
        _type.setRedraw(false);
        for (int i = 0; i < cfg.getTypeCount(); i++) {
            String id = cfg.getTypeId(i);
            String name = cfg.getTypeName(i);
            _type.add(getText(id, name));
            _types.add(id);
        }
        if (cfg.getTypeDefaultIndex() >= 0) {
            _type.select(cfg.getTypeDefaultIndex());
            _selectedTypeId = (String)_types.get(cfg.getTypeDefaultIndex());
        }
        _type.setRedraw(true);
        
        loadPrefs();
        _syndie.setText(Version.VERSION);
        
        SyndieURI target = cfg.getTargetScope();
        if (target != null) {
            long targetId = _client.getChannelId(target.getScope());
            if (targetId >= 0) {
                String name = _client.getChannelName(targetId);
                _target.add(UIUtil.displayName(name, target.getScope()));
            }
        }
        
        DBClient.ChannelCollector chans = _client.getChannels(true, true, true, true);
        for (int i = 0; i < chans.getIdentityChannelCount(); i++) {
            ChannelInfo info = chans.getIdentityChannel(i);
            _targetChans.add(info.getChannelHash());
            _signAsChans.add(info.getChannelHash());
            _target.add(UIUtil.displayName(info.getName(), info.getChannelHash()));
            _signAs.add(UIUtil.displayName(info.getName(), info.getChannelHash()));
        }
        for (int i = 0; i < chans.getManagedChannelCount(); i++) {
            ChannelInfo info = chans.getManagedChannel(i);
            _targetChans.add(info.getChannelHash());
            _target.add(UIUtil.displayName(info.getName(), info.getChannelHash()));
        }
        for (int i = 0; i < chans.getPostChannelCount(); i++) {
            ChannelInfo info = chans.getPostChannel(i);
            _targetChans.add(info.getChannelHash());
            _target.add(UIUtil.displayName(info.getName(), info.getChannelHash()));
        }
        for (int i = 0; i < chans.getPublicPostChannelCount(); i++) {
            ChannelInfo info = chans.getPublicPostChannel(i);
            _targetChans.add(info.getChannelHash());
            _target.add(UIUtil.displayName(info.getName(), info.getChannelHash()));
        }
        
        // todo: enable this (need to modify MessageGen to create authorized unauthenticated posts)
        // _signAs.add(_browser.getTranslationRegistry().getText("Anonymous"));
        
        int idx = _targetChans.indexOf(Hash.create(Base64.decode(STANDARD_BUGREPORT_FORUM)));
        if (idx >= 0)
            _target.select(idx);
        else
            _target.select(0);
        targetSelected();
        
        Properties prefs = _client.getNymPrefs();
        String val = prefs.getProperty("editor.defaultAuthor");
        if (val != null) {
            byte hash[] = Base64.decode(val);
            if ( (hash != null) && (hash.length == Hash.HASH_LENGTH) ) {
                idx = _signAsChans.indexOf(Hash.create(hash));
                if (idx >= 0)
                    _signAs.select(idx);
                else
                    _signAs.select(0);
            } else {
                _signAs.select(0);
            }
        } else {
            _signAs.select(0);
        }
    }
    
    private void rebuildComponentMenu(BugConfig cfg) {
        MenuItem items[] = _componentMenu.getItems();
        for (int i = 0; i < items.length; i++) items[i].dispose();
        
        for (int i = 0; i < cfg.getComponentCount(); i++)
            addComponent(cfg.getComponent(i), _componentMenu);
    }
    private void addComponent(final ReferenceNode node, Menu parent) {
        if (node == null) return;
        MenuItem item = null;
        if (node.getChildCount() > 0) {
            item = new MenuItem(parent, SWT.CASCADE);
            Menu sub = new Menu(item);
            item.setMenu(sub);
            item.setText(getText(node.getName(), node.getDescription()));
            
            MenuItem subcur = new MenuItem(sub, SWT.PUSH);
            subcur.setText(getText(node.getName(), node.getDescription()));
            subcur.addSelectionListener(new SelectionListener() {
                public void widgetDefaultSelected(SelectionEvent selectionEvent) { selectComponent(node); }
                public void widgetSelected(SelectionEvent selectionEvent) { selectComponent(node); }
            });
            new MenuItem(sub, SWT.SEPARATOR);
            for (int i = 0; i < node.getChildCount(); i++)
                addComponent(node.getChild(i), sub);
        } else {
            item = new MenuItem(parent, SWT.PUSH);
            item.setText(getText(node.getName(), node.getDescription()));
            item.addSelectionListener(new SelectionListener() {
                public void widgetDefaultSelected(SelectionEvent selectionEvent) { selectComponent(node); }
                public void widgetSelected(SelectionEvent selectionEvent) { selectComponent(node); }
            });
        }
    }
    
    private void selectComponent(ReferenceNode node) {
        _component.setText(getText(node.getName(), node.getDescription()));
        _selectedComponentId = node.getName();
    }
    
    public void dispose() {
        _translationRegistry.unregister(this);
        _themeRegistry.unregister(this);
    }
    
    public void applyTheme(Theme theme) {
        _componentLabel.setFont(theme.DEFAULT_FONT);
        _component.setFont(theme.DEFAULT_FONT);
        _typeLabel.setFont(theme.DEFAULT_FONT);
        _type.setFont(theme.DEFAULT_FONT);
        _severityLabel.setFont(theme.DEFAULT_FONT);
        _severity.setFont(theme.DEFAULT_FONT);
        _osLabel.setFont(theme.DEFAULT_FONT);
        _os.setFont(theme.DEFAULT_FONT);
        _jvmLabel.setFont(theme.DEFAULT_FONT);
        _jvm.setFont(theme.DEFAULT_FONT);
        _swtLabel.setFont(theme.DEFAULT_FONT);
        _swt.setFont(theme.DEFAULT_FONT);
        _syndieLabel.setFont(theme.DEFAULT_FONT);
        _hsqldbLabel.setFont(theme.DEFAULT_FONT);
        _syndie.setFont(theme.DEFAULT_FONT);
        _summaryLabel.setFont(theme.DEFAULT_FONT);
        _summary.setFont(theme.DEFAULT_FONT);
        _logGroup.setFont(theme.DEFAULT_FONT);
        _log.setFont(theme.DEFAULT_FONT);
        _attachmentsLabel.setFont(theme.DEFAULT_FONT);
        _attachments.setFont(theme.DEFAULT_FONT);
        _attachmentsAdd.setFont(theme.BUTTON_FONT);
        _attachmentsRemove.setFont(theme.BUTTON_FONT);
        _post.setFont(theme.BUTTON_FONT);
        _targetLabel.setFont(theme.DEFAULT_FONT);
        _target.setFont(theme.DEFAULT_FONT);
        _asPrivate.setFont(theme.DEFAULT_FONT);
        _signAsLabel.setFont(theme.DEFAULT_FONT);
        _signAs.setFont(theme.DEFAULT_FONT);
        
        _scroll.setMinSize(_root.computeSize(SWT.DEFAULT, SWT.DEFAULT));
    }
    
    

    public void translate(TranslationRegistry registry) {
        _componentLabel.setText(registry.getText("Component") + ':');
        _typeLabel.setText(registry.getText("Bug type") + ':');
        _severityLabel.setText(registry.getText("Severity") + ':');
        _osLabel.setText(registry.getText("OS") + ':');
        _jvmLabel.setText(registry.getText("JVM") + ':');
        _swtLabel.setText(registry.getText("SWT") + ':');
        _syndieLabel.setText(registry.getText("Syndie") + ':');
        _hsqldbLabel.setText(registry.getText("HSQLDB") + ':');
        _summaryLabel.setText(registry.getText("Issue summary") + ':');
        _logGroup.setText(registry.getText("Details") + ':');
        _attachmentsLabel.setText(registry.getText("Attachments") + ':');
        _attachmentsAdd.setText(registry.getText("Add"));
        _attachmentsRemove.setText(registry.getText("Remove"));
        
        _post.setText(registry.getText("Post bug report"));
        _targetLabel.setText(registry.getText("Post to") + ':');
        _asPrivate.setText(registry.getText("Report includes sensitive data (so only let the admins read it)"));
        _signAsLabel.setText(registry.getText("Sign report as") + ':');
        if (_signAs.getItemCount() > _signAsChans.size()) {
            _signAs.remove(_signAs.getItemCount()-1);
            _signAs.add(registry.getText("Anonymous"));
        }
        
        rebuildComponentMenu(_client.getBugConfig());
    }
}
