package genepi.vcf;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import genepi.base.Tool;
import genepi.io.text.LineReader;
import genepi.util.Chromosome;
import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.Genotype;
import htsjdk.variant.variantcontext.GenotypeBuilder;
import htsjdk.variant.variantcontext.GenotypesContext;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.variantcontext.VariantContextBuilder;
import htsjdk.variant.variantcontext.writer.Options;
import htsjdk.variant.variantcontext.writer.VariantContextWriter;
import htsjdk.variant.variantcontext.writer.VariantContextWriterBuilder;
import htsjdk.variant.vcf.VCFConstants;
import htsjdk.variant.vcf.VCFFormatHeaderLine;
import htsjdk.variant.vcf.VCFHeader;
import htsjdk.variant.vcf.VCFHeaderLine;
import htsjdk.variant.vcf.VCFHeaderLineType;
import htsjdk.variant.vcf.VCFHeaderVersion;

public class GenerateVCF extends Tool {

	public GenerateVCF(String[] args) {
		super(args);
	}

	@Override
	public void init() {

		System.out.println("Generate vcf.gz files from your 23andme Raw Data\n\n");

	}

	@Override
	public void createParameters() {

		addParameter("genome", "input 23andme raw data");
		addParameter("chip", "input chip target list");
		addParameter("out", "output vcf directory");

	}

	@Override
	public int run() {

		try {
			String in = (String) getValue("genome");
			String chip = (String) getValue("chip");
			String out = (String) getValue("out");
			
			new File(out).mkdirs();

			LineReader chipReader = new LineReader(chip);
			LineReader dataReader = new LineReader(in);
			VariantContextWriter writer2 = null;
			VCFHeader header = null;
			String prev = "";

			while (dataReader.next()) {

				if (dataReader.get().startsWith("#")) {
					continue;
				}

				chipReader.next();

				String[] chipSplit = chipReader.get().split("\t");
				String[] dataSplit = dataReader.get().split("\t");

				if (!dataSplit[2].equals(chipSplit[1])) {
					System.err.println("Control Chip version! Positions should be identical in both files");
					System.exit(-1);
				}

				String rsid = dataSplit[0];
				String chromosome = dataSplit[1];
				int pos = Integer.valueOf(dataSplit[2]);
				String genotype = dataSplit[3];
				String ref = chipSplit[3];

				if (genotype.contains("I") || genotype.contains("D") || genotype.contains("-")
						|| genotype.length() > 2) {
					continue;
				}

				if (!chromosome.equals(prev)) {

					if (writer2 != null) {
						writer2.close();
					}

					header = generateHeader(chromosome);
					VariantContextWriterBuilder builder = new VariantContextWriterBuilder()
							.setOutputFile(out + "/" + chromosome + ".vcf.gz").unsetOption(Options.INDEX_ON_THE_FLY);
					writer2 = builder.build();
					writer2.writeHeader(header);
					prev = chromosome;

				}

				Allele refAllele = Allele.create(ref, true);
				Allele altAllele = null;
				boolean heterozygous = false;
				boolean homozygous = false;

				String g0 = genotype.substring(0, 1);
				String g1 = null;

				if (genotype.length() == 2) {
					g1 = genotype.substring(1, 2);
				}

				// heterozygous check
				if (g1 != null && !g0.equals(g1)) {
					altAllele = Allele.create(!ref.equals(g0) ? g0 : g1, false);
					heterozygous = true;
				}
				// homozygous check
				else if (!ref.equals(g0)) {
					altAllele = Allele.create(g0, false);
					homozygous = true;
				}
				// do nothing since its identical to the reference.
				else {
				}

				if (heterozygous) {

					List<Allele> alleles = new ArrayList<Allele>();
					alleles.add(refAllele);
					alleles.add(altAllele);

					writer2.add(createVC(header, chromosome, rsid, alleles, alleles, pos));

				} else if (homozygous) {

					final List<Allele> alleles = new ArrayList<Allele>();
					alleles.add(refAllele);
					alleles.add(altAllele);

					final List<Allele> genotypes = new ArrayList<Allele>();
					genotypes.add(altAllele);

					writer2.add(createVC(header, chromosome, rsid, alleles, genotypes, pos));
				}

			} // while loop

			writer2.close();

			return 0;

		} catch (IOException e) {
			// TODO Auto-generated catch block
			return -1;
		}
	}

	private VCFHeader generateHeader(String chromosome) {

		Set<VCFHeaderLine> headerLines = new HashSet<VCFHeaderLine>();
		Set<String> additionalColumns = new HashSet<String>();

		headerLines.add(new VCFHeaderLine(VCFHeaderVersion.VCF4_0.getFormatString(),
				VCFHeaderVersion.VCF4_0.getVersionString()));
		headerLines.add(new VCFFormatHeaderLine(VCFConstants.GENOTYPE_KEY, 1, VCFHeaderLineType.String, "Genotype"));

		additionalColumns.add("23andMe-Sample");

		SAMSequenceDictionary sequenceDict = generateSequenceDictionary(chromosome);

		VCFHeader header = new VCFHeader(headerLines, additionalColumns);
		header.setSequenceDictionary(sequenceDict);

		return header;

	}

	private SAMSequenceDictionary generateSequenceDictionary(String chromosome) {

		SAMSequenceDictionary sequenceDict = new SAMSequenceDictionary();

		SAMSequenceRecord newSequence = new SAMSequenceRecord(chromosome, Chromosome.getChrLength(chromosome));

		sequenceDict.addSequence(newSequence);

		return sequenceDict;

	}

	private VariantContext createVC(VCFHeader header, String chrom, String rsid, List<Allele> alleles,
			List<Allele> genotype, int position) {

		final Map<String, Object> attributes = new HashMap<String, Object>();
		final GenotypesContext genotypes = GenotypesContext.create(header.getGenotypeSamples().size());

		for (final String name : header.getGenotypeSamples()) {
			final Genotype gt = new GenotypeBuilder(name, genotype).phased(false).make();
			genotypes.add(gt);
		}

		return new VariantContextBuilder("23AndMe", chrom, position, position, alleles).genotypes(genotypes)
				.attributes(attributes).id(rsid).make();
	}

	public static void main(String[] args) {
		new GenerateVCF(args).start();
	}

}
