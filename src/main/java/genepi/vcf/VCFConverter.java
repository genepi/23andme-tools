package genepi.vcf;

import java.io.IOException;
import java.net.MalformedURLException;

import genepi.base.Tool;

public class VCFConverter extends Tool {

	public VCFConverter(String[] args) {
		super(args);
	}

	@Override
	public void init() {

		System.out
				.println("Generate vcf.gz files from your 23andMe Genotype Data\n\n");

	}

	@Override
	public void createParameters() {

		addParameter("in",
				"input 23andMe genotype data (e.g. genome.zip or genome.txt)");
		addParameter("ref",
				"input reference (v37 or /path/to/human_g1k_v37.fasta)");
		addParameter("out", "output vcf directory");
		addOptionalParameter(
				"exclude",
				"optional: specify list of chromosomes to exclude, seperated by comma (e.g. 1,22,X,MT)",
				STRING);
		addOptionalParameter(
				"split",
				"optional: set to false if one VCF file incluging all chromosomes should be created)",
				STRING);

	}

	@Override
	public int run() {

		String in = (String) getValue("in");
		String ref = (String) getValue("ref");
		String exclude = (String) getValue("exclude");
		String out = (String) getValue("out");
		String split = (String) getValue("split");

		VCFBuilder builder = new VCFBuilder(in);
		builder.setReference(ref);
		builder.setOutDirectory(out);
		builder.setExcludeList(exclude);
		if (split != null)
			builder.setSplit(Boolean.valueOf(split));

		try {
			return builder.build();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		}
	}

	public static void main(String[] args) {
		new VCFConverter(args).start();
	}

}
