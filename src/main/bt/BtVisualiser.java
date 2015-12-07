package bt;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;

import bt.sc.ActionBtNode;
import bt.sc.NodeWithActions;
import util.Util;
import util.Util.Pair;

public class BtVisualiser {
	private static final Logger LOGGER = Logger.getLogger(BtVisualiser.class.getName());
	
	/** Properties file to load */
	private static final String PROPERTIES_FILENAME = "btMakerConfig.properties";
	
	private static int MAX_NODES = 5000;
	
	private final String graphvisDotExe;
	/** File extension of visualiser output */
	private final String graphvisExt;
	private final String graphvisTreefileExt;
	
	
	public static void main(String[] args) throws IOException {
		BtVisualiser bv = new BtVisualiser();
		
		File curDir = new File(".");
		List<String> existingOutNoExt = Arrays.stream(curDir.listFiles())
				.map(f -> f.getName())
				.filter(s -> s.endsWith(bv.graphvisExt))
				.map(s -> s.substring(0, s.length() - bv.graphvisExt.length()))
				.collect(Collectors.toList());
				
		File[] toProcess = curDir.listFiles(f -> f.getName().endsWith(bv.graphvisTreefileExt));
		List<Pair<String, Process>> runningProcesses =
				Collections.synchronizedList(new ArrayList<>());
		// Easy mode parallelisation over all trees
		Arrays.stream(toProcess).parallel().forEach(treeFile -> {
			try {
				String n = treeFile.getName();
				String treeFileNoExt = n.substring(0, n.length() - bv.graphvisTreefileExt.length());
				// Don't regenerate files that already exist
				if (!existingOutNoExt.contains(treeFileNoExt)) {
					File outFile = new File(treeFileNoExt + bv.graphvisExt);
					Process graphGen = bv.generateGraph(treeFile, MAX_NODES, outFile, true);
					runningProcesses.add(new Pair<>(treeFileNoExt, graphGen));
				} else {
					LOGGER.info("Skipping already-processed tree " + n);
				}
			} catch (IOException e) {
				LOGGER.log(Level.SEVERE, "Exception processing trees", e);
			}
		});
		while (!runningProcesses.isEmpty()) {
			int count = 0;
			for (Iterator<Pair<String, Process>> it = runningProcesses.iterator(); it.hasNext(); ) {
				Pair<String, Process> p = it.next();
				if (!p.second.isAlive()) {
					LOGGER.info("Done generating graph to " + p.first);
					it.remove();
				}
			}
			if (++count % 10 == 0) {
				LOGGER.info("Still waiting on " + runningProcesses.size() + " process(es)");
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// Ignore
			}
		}
		LOGGER.info("All done");
	}
	
	public BtVisualiser() throws IOException {
		// Load properties
		Properties p = Util.loadProperties(PROPERTIES_FILENAME);
		graphvisDotExe = Util.getPropertyNotNull(p, "graphvis_dot_exe");
		graphvisExt = Util.getPropertyNotNull(p, "graphvis_ext");
		graphvisTreefileExt = Util.getPropertyNotNull(p, "graphvis_treefile_ext");
	}
	
	public Process generateGraph(File treeFile, int maxNumNodes, File outFile, boolean blocking)
			throws IOException {
		LOGGER.info("Processing " + treeFile.getName());
		BehaviourTree tree = BtXmlCodec.load(treeFile);
		LOGGER.info("Loaded. Generating Graph");
		
		String ext = FilenameUtils.getExtension(outFile.getName());
		if (ext == null || ext.isEmpty()) {
			throw new IOException("Outfile must have a file extension");
		}
		File dotFile = File.createTempFile(outFile.getName(), ".dot");
		dotFile.deleteOnExit();
		
		generateDotFile(tree, maxNumNodes, dotFile);
		
		LOGGER.info("Dot file generated. Converting to " + graphvisExt);
		Process p = new ProcessBuilder(graphvisDotExe, dotFile.getAbsolutePath(),
				"-T" + ext, "-o" + outFile.getAbsolutePath())
				.inheritIO()
				.start();
		if (blocking) {
			try {
				p.waitFor();
				LOGGER.info("Done generating graph to " + outFile.getName());
			} catch (InterruptedException e) {
				LOGGER.info("Interrupted");
			}
		}
		return p;
	}
	
	/**
	 * Traverse the tree in level order and output (as a graphvis .dot file) the nodes until
	 * maxNumNodes have been output
	 * @throws IOException  
	 * @throws FileNotFoundException 
	 */
	public void generateDotFile(BehaviourTree tree, int maxNumNodes, File outFile)
			throws IOException {
		LOGGER.info("Generating dot file " + outFile.getAbsolutePath());
		ArrayDeque<BtNode> open = new ArrayDeque<>();
		Map<BtNode, Integer> closed = new LinkedHashMap<>();
		open.add(tree.getRoot());
		
		int idCounter = 0;
		while (!open.isEmpty() && closed.size() < maxNumNodes) {
			BtNode current = open.removeFirst();
			if (!closed.containsKey(current)) {
				closed.put(current, ++idCounter);
				int childCount = 0;
				for (BtNode child : current.getChildren()) {
					// Always add high-freq children
					int childFreq = 0;
					if (current instanceof BtSelNode) {
						childFreq = ((BtSelNode) current).getChildCounts().get(child);
					} else if (current instanceof NodeWithActions) {
						childFreq = ((NodeWithActions) current).getActions().size();
					}
					// Be selective about how many children to show so it's viewable
					if (childCount < 5 || child.getChildren().size() > 0 && childCount < 20
							|| child.hasBeenMerged() || childFreq > 10) {
						open.add(child);
						childCount++;
					}
				}
			}
		}
			
		try (BufferedWriter out = new BufferedWriter(new FileWriter(outFile))) {
			// header
			out.write("digraph G {\n");
			//out.write("concentrate=true\n");
			out.write("ordering=out;\n");
			// root must come first, always ID 1
			out.write("1;\n");
			// write out all nodes
			for (Entry<BtNode, Integer> e : closed.entrySet()) {
				int parentId = e.getValue();
				// eg: 1 [label="A"];
				out.write(String.format("%d [label=\"%s\"];\n",
						parentId, e.getKey().toShortString()));
				// write out connections to other nodes
				int numExcludedNodes = 0;
				List<BtNode> children = e.getKey().getChildren();
				for (int childIdx = 0; childIdx < children.size(); childIdx++) {
					BtNode child = children.get(childIdx);
					int freq = 1;
					int weight = 1; // Not on label
					if (e.getKey() instanceof BtSelNode) {
						freq = ((BtSelNode) e.getKey()).getChildCounts().get(child);
					}
					if (child instanceof ActionBtNode) {
						weight = ((ActionBtNode) child).getActions().size();
					}
					if (closed.containsKey(child) && numExcludedNodes > 0) {
						// nodes have been excluded in between children, write ellipsis first
						writeEllipsisNode(out, parentId, ++idCounter, numExcludedNodes);
						numExcludedNodes = 0;
					}
					if (closed.containsKey(child)) {
						String label = String.format(",label=\"%d\"", freq);
						if (freq == 1)
							label = "";
						out.write(String.format("%d -> %d [weight=%d%s];\n",
								parentId, closed.get(child), freq + weight, label));
					} else {
						numExcludedNodes += freq;
					}
				}
				if (numExcludedNodes > 0) {
					writeEllipsisNode(out, parentId, ++idCounter, numExcludedNodes);
				}
			}
			// footer
			out.write("}\n");
		}
	}
	
	/** write a link to an ellipsis (...) node */
	private void writeEllipsisNode(BufferedWriter out, int parentId, int idCounter,
			int numExcludedNodes) throws IOException {
		out.write(String.format("%d -> %d [weight=%d,label=\"%d\"];\n",
				parentId, idCounter, 1, numExcludedNodes));
		out.write(String.format("%d [label=\"...\"];\n", idCounter));
	}
	
}
