package genepi.ngs;

import java.io.File;
import java.io.IOException;

import genepi.base.Tool;
import genepi.io.text.LineReader;
import genepi.io.text.LineWriter;
import htsjdk.samtools.reference.IndexedFastaSequenceFile;

public class GenerateSites extends Tool {

	public GenerateSites(String[] args) {
		super(args);
	}

	@Override
	public void init() {
		
		System.out.println("Generate Target List from 23andme Raw Data\n\n");

	}

	@Override
	public void createParameters() {
		
		addParameter("ref", "input human_g1k_v37.fasta");
		addParameter("in", "input 23andme raw data");
		addParameter("out", "output target list");

	}

	@Override
	public int run() {
		
		try {

			String in = (String) getValue("in");
			String out = (String) getValue("out");
			String fasta = (String) getValue("ref");
			
			LineReader reader = new LineReader(in);
			LineWriter writer = new LineWriter(out);
			StringBuilder outFile = new StringBuilder();
			IndexedFastaSequenceFile file = new IndexedFastaSequenceFile(
					new File(fasta));

			while (reader.next()) {

				String line = reader.get();

				if (!line.startsWith("#")) {

					String[] splits = line.split("\t");
					String rs = splits[0];
					String chr = splits[1];
					long pos = Integer.valueOf(splits[2]);
					String genotype = file.getSubsequenceAt(splits[1], pos, pos).getBaseString();

					outFile.append(chr + "\t" + pos + "\t" + rs + "\t" + genotype + "\n");
				}

			}

			writer.write(outFile.toString());
			file.close();
			writer.close();
			return 0;

		} catch (IOException e) {
			// TODO Auto-generated catch block
			return -1;
		}
	}

	public static void main(String[] args) {
		new GenerateSites(args).start();
	}

}
