import java.awt.EventQueue;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.SwingConstants;

import java.awt.event.ActionEvent;

import rice.environment.Environment;
import rice.p2p.commonapi.NodeHandle;

import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.util.Vector;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JRadioButton;

import java.awt.Color;

import javax.swing.UIManager;

import logging.LoggingExample;
import logging.MyFormatter;
import logging.MyHandler;

public class Radio {

	private JFrame frmRadiog;
	private JTextField txtBootstrapIp;
	private JTextField txtBootstrapPort;
	private static JTextField txtBindPort;
	private static JTextArea textAreaClients;
	private static JLabel lblGettingStream;
	private static JLabel lblSystemProperty;
	private static String OS = "";
	private static String OsArch = "";
	private static String JREArch = "";
	private static JLabel lblError = new JLabel("Message");
	private static final JTextField lblAudioPath = new JTextField(
			"C:\\Users\\harsh\\Desktop\\abc.mp3");
	private static JTextField textMyIP;
	private Handler fileHandler;
	public static Logger logger = Logger.getLogger(LoggingExample.class.getName());

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Radio window = new Radio();
					window.frmRadiog.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 * 
	 * @throws IOException
	 */
	public Radio() {
		initialize();
		getSystemProperties();
	}

	/**
	 * Initialize the contents of the frame.
	 * 
	 * @throws IOException
	 */
	private void initialize() {
		System.out.println("Initializing GUI...");
		frmRadiog = new JFrame();
		frmRadiog.setTitle("RadioG");
		frmRadiog.setBounds(100, 100, 585, 403);
		frmRadiog.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmRadiog.getContentPane().setLayout(null);

		txtBootstrapPort = new JTextField();
		txtBootstrapIp = new JTextField();
		txtBootstrapIp.setBounds(144, 86, 196, 20);
		txtBootstrapIp.setHorizontalAlignment(SwingConstants.LEFT);
		frmRadiog.getContentPane().add(txtBootstrapIp);
		txtBootstrapIp.setColumns(10);

		final JRadioButton rdbtnBootstrapNode = new JRadioButton(
				"Bootstrap Node");

		
		
		// Finding bootstrap node from url

		try {
			String[] boot = getUrlSource(
					"http://172.16.27.65/files/RadioGBoot.txt").split(":");
			// System.out.println(boot[0]);
			// System.out.println(boot[1]);
			txtBootstrapIp.setText(boot[0]);
			txtBootstrapPort.setText(boot[1]);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			logger.warning("Cannot fetch bootstap server");
			lblError.setText("Cannot fetch bootstap server.\nPlease enter manually");
		}

		final JButton connect = new JButton("Start");
		connect.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent arg0) {
				
				//Logging config
				try {
					LogManager.getLogManager().readConfiguration(
							new FileInputStream("mylogging.properties"));
				} catch (SecurityException | IOException e1) {
					e1.printStackTrace();
				}
				logger.setLevel(Level.FINE);
				// adding custom handler
				logger.addHandler(new MyHandler());
				try {
					// FileHandler file name with max size and number of log files limit
					fileHandler = new FileHandler("logger"+txtBindPort.getText()+".html");
					fileHandler.setFormatter(new MyFormatter());
					// fileHandler.setFilter(new MyFilter());
					logger.addHandler(fileHandler);
				} catch (SecurityException | IOException e) {
					e.printStackTrace();
				}
				
				// Loads pastry settings
				System.out.println("Starting the system...");
				Environment env = new Environment("freepastry");

				try {

					boolean isBoostrapNode = false;
					// the port to begin creating nodes on
					int PORT = 5009;

					// input address of boostrap node or empty of new ring is
					// required
					// input initial stream source
					InetSocketAddress address;

					int bindPort = Integer.parseInt(txtBindPort.getText());
					final String input = txtBootstrapIp.getText();

					if (!rdbtnBootstrapNode.isSelected()) {
						// System.out.println("Type bootstrap port : ");
						logger.log(Level.FINE,"Started as client node");
						PORT = Integer.parseInt(txtBootstrapPort.getText());
						logger.config("Bootstrap IP "+input);
						logger.config("Bootstrap port "+PORT);
						address = new InetSocketAddress(InetAddress
								.getByName(input), PORT);
					} else {
						logger.log(Level.FINE,"Started as bootstrapped node");
						InetAddress localhost = InetAddress.getByName(Radio.getMyIP());
						
						if (localhost.isLoopbackAddress()) {
							Socket s = new Socket("202.141.80.14", 80);
							localhost = s.getLocalAddress();
							System.out.println("****"
									+ s.getLocalAddress().getHostAddress());
							s.close();
						}
						logger.config("Bootstrap IP "+localhost);
						logger.config("Bootstrap port "+bindPort);
						address = new InetSocketAddress(localhost
								.getHostAddress(), bindPort);
						isBoostrapNode = true;
						
					}

					new RadioNode(bindPort, address, env, isBoostrapNode);
					connect.setEnabled(false);

				} catch (Exception e) {
					logger.log(Level.SEVERE,e.getMessage());
					System.out.println(e.getMessage());
				}

				System.out.println(txtBootstrapPort.getText());
				System.out.println(txtBootstrapIp.getText());
			}
		});

		connect.setBounds(11, 173, 89, 23);
		frmRadiog.getContentPane().add(connect);
		txtBootstrapPort.setBounds(144, 117, 86, 20);
		frmRadiog.getContentPane().add(txtBootstrapPort);
		txtBootstrapPort.setColumns(10);

		txtBindPort = new JTextField();
		txtBindPort.setText("5787");
		txtBindPort.setBounds(145, 148, 86, 20);
		frmRadiog.getContentPane().add(txtBindPort);
		txtBindPort.setColumns(10);

		JLabel lblBootstrapIp = new JLabel("Bootstrap IP");
		lblBootstrapIp.setBounds(11, 86, 71, 14);
		frmRadiog.getContentPane().add(lblBootstrapIp);

		JLabel lblBootstrapPort = new JLabel("Bootstrap port");
		lblBootstrapPort.setBounds(11, 117, 71, 14);
		frmRadiog.getContentPane().add(lblBootstrapPort);

		JLabel label = new JLabel("Binding Port");
		label.setBounds(11, 148, 89, 14);
		frmRadiog.getContentPane().add(label);

		final JButton btnPause = new JButton("Pause");
		btnPause.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (btnPause.getText() == "Pause")
					btnPause.setText("Play");
				else
					btnPause.setText("Pause");
				Player.Pause();
			}
		});
		btnPause.setBounds(121, 173, 89, 23);
		frmRadiog.getContentPane().add(btnPause);

		final JButton btnMute = new JButton("Mute");
		btnMute.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (btnMute.getText() == "Mute") {
					if (Player.Mute()) {
						btnMute.setText("Unmute");
					}
				} else {
					if (Player.UnMute()) {
						btnMute.setText("Mute");
					}
				}
			}
		});
		btnMute.setBounds(230, 173, 98, 23);
		frmRadiog.getContentPane().add(btnMute);

		textAreaClients = new JTextArea();
		textAreaClients.setEditable(false);
		textAreaClients.setBounds(144, 210, 317, 48);
		frmRadiog.getContentPane().add(textAreaClients);

		JLabel lblCurrentListener = new JLabel("Streaming to");
		lblCurrentListener.setBounds(11, 207, 104, 14);
		frmRadiog.getContentPane().add(lblCurrentListener);

		rdbtnBootstrapNode.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (!rdbtnBootstrapNode.isSelected()) {
					txtBootstrapIp.setEditable(true);
					txtBootstrapPort.setEditable(true);
				} else {
					txtBootstrapIp.setEditable(false);
					txtBootstrapPort.setEditable(false);
				}
			}
		});
		rdbtnBootstrapNode.setBounds(11, 44, 104, 23);
		frmRadiog.getContentPane().add(rdbtnBootstrapNode);

		lblSystemProperty = new JLabel("");
		lblSystemProperty.setBounds(30, 11, 445, 26);
		frmRadiog.getContentPane().add(lblSystemProperty);

		JLabel lblGettingSteamFrom = new JLabel("Getting steam from");
		lblGettingSteamFrom.setBounds(11, 281, 104, 14);
		frmRadiog.getContentPane().add(lblGettingSteamFrom);

		lblGettingStream = new JLabel("None");
		lblGettingStream.setBackground(UIManager
				.getColor("EditorPane.selectionBackground"));
		lblGettingStream.setBounds(144, 278, 252, 26);
		frmRadiog.getContentPane().add(lblGettingStream);
		lblError.setForeground(Color.RED);

		lblError.setBackground(Color.RED);
		lblError.setBounds(11, 306, 432, 47);
		frmRadiog.getContentPane().add(lblError);

		lblAudioPath.setBounds(243, 48, 218, 20);
		frmRadiog.getContentPane().add(lblAudioPath);

		JButton btnNewButton = new JButton("Chose Audio File");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				JFileChooser folder = new JFileChooser();
				folder.setFileSelectionMode(JFileChooser.FILES_ONLY);
				if (folder.showOpenDialog(frmRadiog) == JFileChooser.APPROVE_OPTION) {
					lblAudioPath.setText(folder.getSelectedFile()
							.getAbsolutePath());
				}
			}
		});
		btnNewButton.setBounds(121, 44, 112, 23);
		frmRadiog.getContentPane().add(btnNewButton);
		
		textMyIP = new JTextField();
		textMyIP.setBounds(338, 148, 123, 20);
		frmRadiog.getContentPane().add(textMyIP);
		textMyIP.setColumns(10);
		
		JLabel lblYourIpAddress = new JLabel("Your IP Address");
		lblYourIpAddress.setBounds(241, 151, 99, 14);
		frmRadiog.getContentPane().add(lblYourIpAddress);

	}

	public static void refrestClientList(Vector<NodeHandle> clients) {
		textAreaClients.setText(clients.toString().replace(',', '\n'));
	}

	public static void setGetStreamLabel(String source) {
		lblGettingStream.setText(source);
	}

	public static void setError(String source) {
		lblError.setText(source);
	}

	public static void settxtBindPort(String source) {
		txtBindPort.setText(source);
	}
	
	public static String getMyIP(){
		return textMyIP.getText();
	}
	
	public static String getAudioFilepath() {
		return lblAudioPath.getText();
	}

	/**
	 * get system properties
	 */
	private void getSystemProperties() {
		OS = CheckSystem.getOS();
		OsArch = CheckSystem.getOsArch();
		JREArch = CheckSystem.getJREArch();

		lblSystemProperty.setText(OS + " " + OsArch + "; JRE " + JREArch);
	}

	// Get the webpage
	private static String getUrlSource(String url) throws IOException {
		URL page = new URL(url);
		URLConnection yc = page.openConnection();
		BufferedReader in = new BufferedReader(new InputStreamReader(
				yc.getInputStream(), "UTF-8"));
		String inputLine;
		StringBuilder a = new StringBuilder();
		while ((inputLine = in.readLine()) != null)
			a.append(inputLine);
		in.close();

		return a.toString();
	}
}
