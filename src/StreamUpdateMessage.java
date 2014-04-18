import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.NodeHandle;


public class StreamUpdateMessage implements Message {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public static enum StreamInfo{
		STREAM_FREE,
		STREAM_FULL,
	}
	
	private StreamInfo info;
	private NodeHandle node;
	
	
	
	public StreamInfo getInfo(){
		return info;
	}
	
	public void setInfo(StreamInfo inf){
		this.info = inf;
	}

	@Override
	public int getPriority() {
		// TODO Auto-generated method stub
		return Message.LOW_PRIORITY;
	}

	public NodeHandle getNode() {
		return node;
	}

	public void setNode(NodeHandle node) {
		this.node = node;
	}

}
