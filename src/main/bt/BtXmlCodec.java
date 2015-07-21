package bt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import scdb.OfflineJNIBWAPI;
import bt.adapt.NameAsIdReferenceMarshallingStrategy;
import bt.adapt.OrderTypeConverter;
import bt.adapt.UnitCommandTypeConverter;
import bt.adapt.UnitTypeConverter;

import com.thoughtworks.xstream.XStream;

/** Encoder / decoder for Behaviour Trees <-> XML */
public class BtXmlCodec {
	private static final Logger LOGGER = Logger.getLogger(BtXmlCodec.class.getName());
	
	/** One MB in B (2^20) */
	private static final int BUF_SIZE = 1024 * 1024;
	
	/** Make an XStream instance with nicer XML representation */
	private static XStream getXStream() {
		XStream xstream = new XStream();
		xstream.setMode(XStream.ID_REFERENCES);
		xstream.setMarshallingStrategy(new NameAsIdReferenceMarshallingStrategy());
		xstream.registerConverter(new UnitTypeConverter());
		xstream.registerConverter(new OrderTypeConverter());
		xstream.registerConverter(new UnitCommandTypeConverter());
		return xstream;
	}
	
	public static void save(BehaviourTree bt, File file) throws IOException {
		LOGGER.info("Saving tree to file " + file.getAbsolutePath());
		XStream xstream = getXStream();
		// in case of exception, don't overwrite file until output complete
		File temp = File.createTempFile("behaviourTree", ".tmp");
		temp.deleteOnExit(); // in case of exception, clean up
		
		if (file.getName().endsWith(".gz")) {
			try (GZIPOutputStream out = new GZIPOutputStream(new FileOutputStream(temp), BUF_SIZE) ) {
				xstream.toXML(bt, out);
			}
		} else {
			try (FileOutputStream out = new FileOutputStream(temp) ) {
				xstream.toXML(bt, out);
			}
		}
		if (file.exists()) {
			file.delete();
		}
		temp.renameTo(file);
		file.setLastModified(new Date().getTime());
		bt.sanityCheck();
	}
	
	public static BehaviourTree load(File file) throws IOException {
		// Ensure BWAPI data loaded
		OfflineJNIBWAPI.loadOfflineJNIBWAPIData();
		XStream xstream = getXStream();
		
		BehaviourTree bt;
		if (file.getName().endsWith(".gz")) {
			try (GZIPInputStream in = new GZIPInputStream(new FileInputStream(file), BUF_SIZE) ) {
				bt = (BehaviourTree) xstream.fromXML(in);
			}
		} else {
			try (FileInputStream in = new FileInputStream(file) ) {
				bt = (BehaviourTree) xstream.fromXML(in);
			}
		}
		bt.sanityCheck();
		return bt;
	}
}
