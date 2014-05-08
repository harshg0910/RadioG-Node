import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.logging.Level;
import rice.environment.time.simple.SimpleTimeSource;
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
	public static boolean isBootStrapNode = false;
	public static boolean isSurrogate = false;
	private static rice.p2p.commonapi.NodeHandle nodeHandle;
	public static SimpleTimeSource sts = new SimpleTimeSource();;
	
	
	
	public static RadioNode getRadioNode() {
		if (radioNode != null) {
			return radioNode;
		}
		return null;
	}
	/**
	 * Starts a pastry node and binds it to the application
	 * @param bindport - Application bind port
	 * @param bootaddress - Bootstrap node's address
	 * @param env - refer Pastry Enviornment
	 * @param isBoostrapNode  
	 * @param isSurrogate
	 * @throws Exception
	 */
	public RadioNode(int bindport, InetSocketAddress bootaddress,
			Environment env, boolean isBoostrapNode, boolean isSurrogate) throws Exception {
		RadioNode.isBootStrapNode = isBoostrapNode;

		radioNode = this;
		RadioNode.isSurrogate = isSurrogate;
		this.env = env;
		// Generate the NodeIds Randomly
		NodeIdFactory nidFactory = new RandomNodeIdFactory(env);

		InetAddress localhost = InetAddress.getByName(Radio.getMyIP());
		if (localhost.isLoopbackAddress()) {
			Socket s = new Socket("202.141.80.14", 80);
			localhost = s.getLocalAddress();
			s.close();
		}
		PastryNodeFactory factory = new SocketPastryNodeFactory(nidFactory,
				localhost, bindport, env);

		// construct a node
		node = factory.newNode();
		
		
		
		nodeHandle = node.getLocalNodeHandle();
		node.boot(bootaddress);

		// the node may require sending several messages to fully boot into the
		// ring
		synchronized (node) {
			while (!node.isReady() && !node.joinFailed()) {
				// delay so we don't busy-wait
				node.wait(500);
				// abort if can't join
				if (node.joinFailed()) {
					Radio.logger.log(Level.SEVERE,node.joinFailedReason().toString());
					throw new IOException(
							"Could not join the FreePastry ring.  Reason:"
									+ node.joinFailedReason());
				}
			}
		}
		Radio.logger.log(Level.INFO,"Node joined ring at " +Radio.upTime);
		System.out.println("Finished creating new node " + node);
		bindport = getBindPort(nodeHandle);
		Radio.logger.log(Level.CONFIG,"Node_Handle "+ nodeHandle);
		Radio.settxtBindPort(String.valueOf(bindport));
		app = new RadioApp(node, bindport + 1, bindport);
		Radio.logger.log(Level.INFO, "Livenes Check is up.");
		app.startLivenessCheck();
		if (isBoostrapNode) {
			if (Radio.getAudioFilepath() != "Filepath") {
				app.setStream(Radio.getAudioFilepath());
				Radio.setError("Bootstrapping port " + bindport);
			} else
			{
				Radio.setError("Please choose an audio file");
			}
		}
		if(!isBoostrapNode){
			Radio.logger.log(Level.INFO, "Sendding stream request");
			app.sendStreamRequest();
		}
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

	

	public static rice.p2p.commonapi.NodeHandle getLocalNodeHandle() {
		return nodeHandle;
	}
	
	
}
