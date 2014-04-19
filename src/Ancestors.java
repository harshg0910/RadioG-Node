import java.util.Vector;
import java.util.logging.Level;

import rice.p2p.commonapi.NodeHandle;

public class Ancestors {
	private static Vector<NodeHandle> ancestorList;

	public void addAncetor(NodeHandle node) {
		if (ancestorList != null) {
			ancestorList.add(node);
		}
		else{
			Radio.logger.log(Level.SEVERE,"Ancestors not initialized");
		}
	}

	public void removeAncestor(NodeHandle node) {
		if (ancestorList!=null && ancestorList.contains(node)) {
			ancestorList.remove(node);
		}
	}

	public Vector<NodeHandle> getAncestorsList() {
		return ancestorList;
	}

	@SuppressWarnings("unchecked")
	public void initAncestors(Vector<NodeHandle> ancestors) {
		ancestorList = (Vector<NodeHandle>) ancestors.clone();
	}
	public  boolean isAncestor(NodeHandle handle){
		if(ancestorList!=null){
			return ancestorList.contains(handle);
		}
		return true;
	}
	public void printAncestors() {
		System.out.println(".....Printing Ancestor List....");
		for(NodeHandle node : ancestorList){
			System.out.println(node);
		}
	}
}
