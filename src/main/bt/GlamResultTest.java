package bt;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import util.LogManager;
import bt.sc.SimilarityMetrics;

public class GlamResultTest {
	static {
		LogManager.initialise("GlamResultTest");
	}
	
	@Test
	public void test() throws IOException {
		File glamTxtFile = new File("glam2-working/glam2_out/run1/glam2.txt");
		File alphabetFile = new File("glam2-working/alphabet_glam.txt");
		BtGlamCodec gc = new BtGlamCodec(alphabetFile, "R", "L",
				n -> SimilarityMetrics.exactActionHash(n));
		
		GlamResult gr = new GlamResult(glamTxtFile, gc.getAlphabet(), gc.withoutSpecialChars(gc
				.getAlphabet()).length);
		gr.calcCharProportions(0);
	}
	
}
