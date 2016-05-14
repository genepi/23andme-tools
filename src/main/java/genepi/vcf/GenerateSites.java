package genepi.vcf;

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

		System.out.println("Generate Chip Target List from your 23andMe Raw Data\n\n");

	}

	@Override
	public void createParameters() {

		addParameter("ref", "input reference (human_g1k_v37.fasta)");
		addParameter("genome", "input your personal 23andme data (as txt)");
		addParameter("out", "output chip target list");

	}

	@Override
	public int run() {

		try {

			String genome = (String) getValue("genome");
			String ref = (String) getValue("ref");
			String out = (String) getValue("out");
			

			LineReader reader = new LineReader(genome);
			IndexedFastaSequenceFile file = new IndexedFastaSequenceFile(new File(ref));
			LineWriter writer = new LineWriter(out);

			StringBuilder outFile = new StringBuilder();
			
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
