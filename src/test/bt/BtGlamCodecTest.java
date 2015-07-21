package bt;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import util.LogManager;
import bt.sc.SimilarityMetrics;

public class BtGlamCodecTest {
	static {
		LogManager.initialise("BtGlamCodecTest");
	}
	
	@Test
	public void testLoadAlphabet() throws IOException {
		File alphabetFile = new File("glam2-working/alphabet_glam.txt");
		BtGlamCodec gc = new BtGlamCodec(alphabetFile, "R", "L",
				n -> SimilarityMetrics.exactActionHash(n));
		//System.out.println(Arrays.toString(gc.getAlphabet()));
		assertTrue(gc.getAlphabet().length == 184);
	}
}
