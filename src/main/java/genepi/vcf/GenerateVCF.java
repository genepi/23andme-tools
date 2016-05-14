package genepi.vcf;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import genepi.base.Tool;
import genepi.io.text.LineReader;
import genepi.objects.Chip;
import genepi.objects.Genome;
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

		System.out.println("Generate vcf.gz files from your 23andMe Genotype Data\n\n");

	}

	@Override
	public void createParameters() {

		addParameter("genome", "input 23andme raw data");
		addParameter("chip", "input chip target list");
		addParameter("out", "output vcf directory");
		addOptionalParameter("chromosomes", "specify list of chromosomes, e.g. 1,3,5", STRING);

	}

	@Override
	public int run() {

		try {
			String in = (String) getValue("genome");
			String chipName = (String) getValue("chip");
			String outDirectory = (String) getValue("out");
			String chromosomes = (String) getValue("chromosomes");
			
			new File(outDirectory).mkdirs();

			LineReader chipReader = new LineReader(chipName);
			LineReader dataReader = new LineReader(in);
			VariantContextWriter vcfWriter = null;
			VCFHeader header = null;
			int counter = 0;
			String prev = "";

			while (dataReader.next()) {

				if (dataReader.get().startsWith("#")) {
					continue;
				}
				//Process only a given sets of chromosomes
				String[] chromosomeParts = null;
				if(chromosomes != null){
					chromosomeParts = chromosomes.split(",");
				Arrays.sort(chromosomeParts);
				}
				
				//parse 23andMe genome line
				Genome genome = new Genome(dataReader.get());
				
				if (chromosomes!=null && !Arrays.asList(chromosomeParts).contains(genome.getChromosome())) {
					chipReader.next();
					continue;
				}
				
				//parse chip specification
				chipReader.next();
				Chip chip = new Chip(chipReader.get());
				
				if (genome.getPos() != chip.getPos()) {
					System.out.println(genome.getChromosome());
					System.out.println(genome.getPos());
					System.out.println(chip.getPos());
					System.err.println("Control chip version! Positions should be identical in both files");
					System.exit(-1);
				}

				Allele refAllele = Allele.create(chip.getRef(), true);
				Allele altAllele = null;
				boolean heterozygous = false;
				boolean homozygous = false;

				String genotype = genome.getGenotype();
				String chromosome = genome.getChromosome();
				String g0 = genotype.substring(0, 1);
				String g1 = null;

				if (genome.getGenotype().contains("I") || genotype.contains("D") || genotype.contains("-")
						|| genotype.length() > 2) {
					continue;
				}

				if (!chromosome.equals(prev)) {
					
					if (vcfWriter != null) {
						System.out.println("chr" + prev +" - #sites: " + counter);
						vcfWriter.close();
					}

					header = generateHeader(chromosome);

					VariantContextWriterBuilder builder = new VariantContextWriterBuilder()
							.setOutputFile(outDirectory + File.separator + chromosome + ".vcf.gz")
							.unsetOption(Options.INDEX_ON_THE_FLY);

					vcfWriter = builder.build();

					vcfWriter.writeHeader(header);

					prev = chromosome;
					
					counter = 0;

				}

				if (genotype.length() == 2) {
					g1 = genotype.substring(1, 2);
				}

				// heterozygous check
				if (g1 != null && !g0.equals(g1)) {
					altAllele = Allele.create(!chip.getRef().equals(g0) ? g0 : g1, false);
					heterozygous = true;
				}
				// homozygous check
				else if (!chip.getRef().equals(g0)) {
					altAllele = Allele.create(g0, false);
					homozygous = true;
				}
				// do nothing since its identical to the reference.
				else {
				}

				final List<Allele> alleles = new ArrayList<Allele>();
				alleles.add(refAllele);
				alleles.add(altAllele);

				if (heterozygous) {
					
					counter++;
					// set alleles for genotype
					vcfWriter.add(createVC(header, chromosome, genome.getRsid(), alleles, alleles, genome.getPos()));

				} else if (homozygous) {

					counter++;
					final List<Allele> genotypes = new ArrayList<Allele>();
					genotypes.add(altAllele);
					genotypes.add(altAllele);
					vcfWriter.add(createVC(header, chromosome, genome.getRsid(), alleles, genotypes, genome.getPos()));

				}

			} // while loop

			System.out.println("chr" + prev +" - #sites: " + counter);
			vcfWriter.close();

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

		additionalColumns.add("sample-23andMe");

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

		return new VariantContextBuilder("23andMe", chrom, position, position, alleles).genotypes(genotypes)
				.attributes(attributes).id(rsid).make();
	}

	public static void main(String[] args) {
		new GenerateVCF(args).start();
	}

}
