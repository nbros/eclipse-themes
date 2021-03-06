package net.jeeeyul.eclipse.themes.ui.store;

import java.io.ByteArrayInputStream;
import java.util.Properties;

import net.jeeeyul.eclipse.themes.JThemesCore;
import net.jeeeyul.eclipse.themes.SharedImages;
import net.jeeeyul.eclipse.themes.ui.hotswap.RewriteCustomTheme;
import net.jeeeyul.eclipse.themes.ui.internal.Debug;
import net.jeeeyul.eclipse.themes.ui.preference.JTPConstants;
import net.jeeeyul.eclipse.themes.ui.preference.JThemePreferenceStore;
import net.jeeeyul.eclipse.themes.ui.preference.internal.PropertiesUtil;
import net.jeeeyul.eclipse.themes.ui.preference.preset.IJTPreset;
import net.jeeeyul.swtend.SWTExtensions;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.css.swt.theme.IThemeEngine;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.browser.CloseWindowListener;
import org.eclipse.swt.browser.OpenWindowListener;
import org.eclipse.swt.browser.WindowEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;

/**
 * Store client
 * 
 * @author Jeeeyul
 */
@SuppressWarnings("restriction")
public class StoreClient extends EditorPart {
	/**
	 * Store Client Editor ID.
	 */
	public static final String EDITOR_ID = StoreClient.class.getCanonicalName();
	private Browser browser;
	private Text urlField;
	private ToolItem backItem;
	private ToolItem forwardItem;
	private ToolItem homeItem;

	@Override
	public void doSave(IProgressMonitor monitor) {

	}

	@Override
	public void doSaveAs() {

	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	private String getBootstrapURL() {
		if (Debug.useLocalStore()) {
			return "http://localhost:3000/";
		} else {
			return "http://themes.jeeeyul.net";
		}
	}

	@Override
	public void createPartControl(Composite parent) {
		try {
			Composite composite = new Composite(parent, SWT.NORMAL);
			GridLayout layout = new GridLayout(3, false);
			composite.setLayout(layout);
			layout.marginWidth = layout.marginHeight = 0;
			layout.verticalSpacing = 0;

			ToolBar toolBar = new ToolBar(composite, SWT.FLAT);
			backItem = new ToolItem(toolBar, SWT.PUSH);
			backItem.setImage(SharedImages.getImage(SharedImages.BACKWARD_NAV));
			backItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event event) {
					browser.back();
				}
			});

			forwardItem = new ToolItem(toolBar, SWT.PUSH);
			forwardItem.setImage(SharedImages.getImage(SharedImages.FORWARD_NAV));
			forwardItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event event) {
					browser.forward();
				}
			});
			new ToolItem(toolBar, SWT.SEPARATOR);

			homeItem = new ToolItem(toolBar, SWT.PUSH);
			homeItem.setImage(SharedImages.getImage(SharedImages.HOME_NAV));
			homeItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event event) {
					browser.setUrl(getBootstrapURL());
				}
			});

			urlField = new Text(composite, SWT.BORDER | SWT.READ_ONLY);
			urlField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			ToolBar rightToolBar = new ToolBar(composite, SWT.FLAT);
			ToolItem openInExternalBrowser = new ToolItem(rightToolBar, SWT.PUSH);
			openInExternalBrowser.setImage(SharedImages.getImage(SharedImages.EXTERNAL_BROWSER));
			openInExternalBrowser.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event event) {
					if (urlField.getText().length() > 0) {
						Program.launch(urlField.getText().trim());
					}
				}
			});
			browser = new Browser(composite, SWT.NONE);
			GridData browserData = new GridData(GridData.FILL_BOTH);
			browserData.horizontalSpan = 3;
			browser.setLayoutData(browserData);

			browser.addOpenWindowListener(new OpenWindowListener() {
				@Override
				public void open(WindowEvent event) {
					handleOpen(event);
				}
			});

			installFunctions();

			browser.setUrl(getBootstrapURL());
		} catch (SWTError e) {
			createBrowserHelpPage(parent);
		}

	}

	private void createBrowserHelpPage(Composite parent) {
		Link link = new Link(parent, SWT.NORMAL);
		link.setText("Could not create browser, Please read <a href=\"https://github.com/jeeeyul/eclipse-themes/wiki/Linux-Theme-Store-Problem\">Linux User Guide</a>");
		link.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				Program.launch(event.text);
			}
		});
	}

	private void installFunctions() {
		new BrowserFunction(browser, "__install") {
			public Object function(Object[] arguments) {
				if (arguments.length == 1 && arguments[0] instanceof String) {
					String soruce = (String) arguments[0];
					installEPF(soruce);
				}
				return null;
			}
		};

		new BrowserFunction(browser, "__getCurrentEPF") {
			public Object function(Object[] args) {
				return new EPFGenerator().generate();
			}
		};

		new BrowserFunction(browser, "__updateURL") {
			public Object function(Object[] args) {
				if (args.length == 1 && args[0] instanceof String) {
					urlField.setText((String) args[0]);
				}
				return null;
			}
		};

		new BrowserFunction(browser, "__openURL") {
			public Object function(Object[] args) {
				if (args.length == 1 && args[0] instanceof String) {
					String url = (String) args[0];
					Program.launch(url);
				}
				return null;
			}
		};

	}

	private void installEPF(String epfString) {
		try {
			Properties properties = new Properties();
			properties.load(new ByteArrayInputStream(epfString.getBytes("ISO-8859-1")));

			IJTPreset defaultPreset = JThemesCore.getDefault().getPresetManager().getDefaultPreset();
			properties = PropertiesUtil.merge(defaultPreset.getProperties(), properties);

			JThemePreferenceStore store = JThemesCore.getDefault().getPreferenceStore();

			for (Object keyObj : properties.keySet()) {
				String key = (String) keyObj;
				String value = properties.getProperty(key);
				if (key.equals(JTPConstants.Layout.TAB_HEIGHT)) {
					int intValue = Integer.parseInt(value);
					store.setValue(key, Math.max(intValue, SWTExtensions.INSTANCE.getMinimumToolBarHeight()));
				} else {
					store.setValue(key, value);
				}
			}
			new RewriteCustomTheme(true).rewrite();
			MApplication application = (MApplication) PlatformUI.getWorkbench().getService(MApplication.class);
			IEclipseContext context = application.getContext();
			IThemeEngine engine = context.get(IThemeEngine.class);
			if (engine.getActiveTheme() == null || !engine.getActiveTheme().getId().equals(JThemesCore.CUSTOM_THEME_ID)) {
				engine.setTheme(JThemesCore.CUSTOM_THEME_ID, true);
			}

			store.setValue(JTPConstants.Memento.LAST_CHOOSED_PRESET, "");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void handleOpen(WindowEvent event) {
		final Shell shell = new Shell(browser.getShell(), SWT.SHELL_TRIM);
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		shell.setLayout(layout);
		Browser browser = new Browser(shell, SWT.NORMAL);

		event.browser = browser;
		browser.addCloseWindowListener(new CloseWindowListener() {
			@Override
			public void close(WindowEvent event) {
				shell.dispose();
			}
		});

		GridData data = new GridData(GridData.FILL_BOTH);
		Point size = event.size != null ? event.size : new Point(640, 480);

		data.widthHint = size.x;
		data.heightHint = size.y;

		browser.setLayoutData(data);
		shell.pack();

		shell.open();
	}

	@Override
	public void setFocus() {
		if (SWTExtensions.INSTANCE.isAlive(browser))
			browser.setFocus();
	}

}
