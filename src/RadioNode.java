import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import rice.environment.Environment;
import rice.p2p.commonapi.NodeHandle;
import rice.pastry.NodeIdFactory;
import rice.pastry.PastryNode;
import rice.pastry.PastryNodeFactory;
import rice.pastry.leafset.LeafSet;
import rice.pastry.socket.SocketPastryNodeFactory;
import rice.pastry.standard.RandomNodeIdFactory;

public class RadioNode {

	public static PastryNode node;
	private static RadioNode radioNode = null;
	RadioApp app;
	Environment env;
	LeafSet leafSet;
	public static boolean isBootStrapeNode = false;
	private static rice.p2p.commonapi.NodeHandle nodeHandle;

	public static RadioNode getRadioNode() {
		if (radioNode != null) {
			return radioNode;
		}
		return null;
	}

	public RadioNode(int bindport, InetSocketAddress bootaddress,
			Environment env, boolean isBoostrapNode) throws Exception {
		RadioNode.isBootStrapeNode = isBoostrapNode;

		radioNode = this;

		this.env = env;
		// Generate the NodeIds Randomly
		NodeIdFactory nidFactory = new RandomNodeIdFactory(env);

		InetAddress localhost = InetAddress.getByName(Radio.getMyIP());
		if (localhost.isLoopbackAddress()) {
			Socket s = new Socket("202.141.80.14", 80);
			localhost = s.getLocalAddress();
			System.out.println("****"
					+ s.getLocalAddress().getHostAddress());
			s.close();
		}
		System.out.println("**********"+localhost);
		PastryNodeFactory factory = new SocketPastryNodeFactory(nidFactory,
				localhost, bindport, env);

		// construct a node
		node = factory.newNode();

		nodeHandle = node.getLocalNodeHandle();
		// construct a new MyApp
		node.boot(bootaddress);

		// the node may require sending several messages to fully boot into the
		// ring
		synchronized (node) {
			while (!node.isReady() && !node.joinFailed()) {
				// delay so we don't busy-wait
				node.wait(500);
				// abort if can't join
				if (node.joinFailed()) {
					throw new IOException(
							"Could not join the FreePastry ring.  Reason:"
									+ node.joinFailedReason());
				}
			}
		}

		System.out.println("Finished creating new node " + node);
		updateLeafSet();
		System.out.println("Creating app");
		bindport = getBindPort(nodeHandle);
		Radio.settxtBindPort(String.valueOf(bindport));
		app = new RadioApp(node, bindport + 1, bindport);
		app.startLivenessCheck();
		if (isBoostrapNode) {
			if (Radio.getAudioFilepath() != "Filepath") {
				app.setStream(Radio.getAudioFilepath());
				Radio.setError("Bootstrapping port " + bindport);
			} else
				Radio.setError("Please choose an audio file");
		}

		app.sendStreamRequest();
	}

	// get bind port from the node handle
	// if pastry fails to bind with the input port it takes next available port
	// and we were unable to find that port using API so this dirty method
	// extracts port from node handle. you are more than welcome to change it
	private int getBindPort(NodeHandle handle) {
		String str = handle.toString();
		System.out.println("handle" + str);
		String tokens[] = str.split(":");
		String port = tokens[2].substring(0, tokens[2].length() - 1);
		return Integer.parseInt(port);
	}

	public void updateLeafSet() {
		leafSet = node.getLeafSet();
	}

	public static rice.p2p.commonapi.NodeHandle getLocalNodeHandle() {
		return nodeHandle;
	}
	
}
