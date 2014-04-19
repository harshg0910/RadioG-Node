import java.util.Vector;

import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.NodeHandle;


@SuppressWarnings("serial")
public class AncestorMessage implements Message{
	private Vector<NodeHandle> ancestorList;
	private long delay;
	@SuppressWarnings("unchecked")
	public AncestorMessage(Ancestors ancestors, NodeHandle node, long delay){
		this.ancestorList = (Vector<NodeHandle>) ancestors.getAncestorsList().clone();
		this.ancestorList.add(node);
		this.delay = delay;
	}
	
	public Vector<NodeHandle> getAncestorList(){
		return ancestorList;
	}
	
	public long getDelay(){
		return delay;
	}
	public int getPriority() {
		// TODO Auto-generated method stub
		return 0;
	}

}
