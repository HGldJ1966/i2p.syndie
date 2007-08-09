package syndie.gui.desktop;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import syndie.data.MessageInfo;
import syndie.data.Timer;
import syndie.db.DBClient;
import syndie.db.UI;
import syndie.gui.BanControl;
import syndie.gui.BookmarkControl;
import syndie.gui.FireSelectionListener;
import syndie.gui.MessageViewBody;
import syndie.gui.NavigationControl;
import syndie.gui.Theme;
import syndie.gui.ThemeRegistry;
import syndie.gui.Themeable;
import syndie.gui.Translatable;
import syndie.gui.TranslationRegistry;
import syndie.gui.URIHelper;

public class StandaloneMessageViewer implements Translatable, Themeable {
    private DBClient _client;
    private UI _ui;
    private Shell _parent;
    private MessageInfo _msg;
    private int _startPage;
    private MessageViewBody _view;
    private Button _close;
    private NavigationControl _nav;
    private BookmarkControl _bookmarkCtl;
    private BanControl _banCtl;
    private ThemeRegistry _themeRegistry;
    private TranslationRegistry _translationRegistry;
    
    private Shell _shell;
    
    public StandaloneMessageViewer(DBClient client, UI ui, Shell parent, MessageInfo msg, int startPage, NavigationControl nav, ThemeRegistry themes, TranslationRegistry trans, BookmarkControl bookmarkCtl, BanControl banCtl) {
        _client = client;
        _ui = ui;
        _parent = parent;
        _msg = msg;
        _startPage = startPage;
        _nav = nav;
        _themeRegistry = themes;
        _translationRegistry = trans;
        _bookmarkCtl = bookmarkCtl;
        _banCtl = banCtl;
        initComponents();
    }
    
    private void initComponents() {
        if (_msg == null) return;
        
        _shell = new Shell(_parent, SWT.SHELL_TRIM);
        GridLayout gl = new GridLayout(1, true);
        gl.horizontalSpacing = 0;
        gl.verticalSpacing = 0;
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        _shell.setLayout(gl);
        _shell.addShellListener(new ShellListener() {
            public void shellActivated(ShellEvent shellEvent) {}
            public void shellClosed(ShellEvent shellEvent) { 
                dispose();
            }
            public void shellDeactivated(ShellEvent shellEvent) {}
            public void shellDeiconified(ShellEvent shellEvent) {}
            public void shellIconified(ShellEvent shellEvent) {}
        });
        
        Composite body = new Composite(_shell, SWT.NONE);
        body.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
        body.setLayout(new FillLayout());
        _view = new MessageViewBody(_client, _ui, _themeRegistry, _translationRegistry, _nav, URIHelper.instance(), _bookmarkCtl, _banCtl, body);
        _view.hideThreadTab();
        Timer timer = new Timer("standalone viewer", _ui);
        _view.viewMessage(_msg, _startPage, timer);
        
        _close = new Button(_shell, SWT.PUSH);
        _close.addSelectionListener(new FireSelectionListener() { public void fire() { dispose(); } });
        _close.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
        
        _shell.setText(_msg.getSubject());
        
        Rectangle disp = _shell.getDisplay().getBounds();
        int width = (int)(disp.width*.8);
        int height = (int)(disp.height*.8);
        int x = (disp.width-width)/2;
        int y = (disp.height-height)/2;
        _shell.setSize(width, height);
        _shell.setLocation(x, y);
        
        _themeRegistry.register(this);
        _translationRegistry.register(this);
        
        //_shell.pack();
        _shell.open();
    }
    
    private void dispose() {
        _themeRegistry.unregister(this);
        _translationRegistry.unregister(this);
        _shell.dispose();
        _view.dispose();
    }

    public void applyTheme(Theme theme) { _close.setFont(theme.BUTTON_FONT); _shell.layout(true, true); }
    public void translate(TranslationRegistry registry) { _close.setText(registry.getText(T_CLOSE, "Close")); }
    private static final String T_CLOSE = "syndie.gui.desktop.standalonemessageviewer.close";
}