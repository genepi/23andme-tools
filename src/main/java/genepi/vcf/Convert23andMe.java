package genepi.vcf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.FileUtils;

import genepi.base.Tool;
import genepi.io.text.LineReader;
import genepi.objects.Chip;
import genepi.objects.GenotypeLine;
import genepi.util.Chromosome;
import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.reference.IndexedFastaSequenceFile;
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

public class Convert23andMe extends Tool {

	private static String GRCh37_PATH = "ftp://ftp.1000genomes.ebi.ac.uk/vol1/ftp/technical/reference/human_g1k_v37.fasta";

	public Convert23andMe(String[] args) {
		super(args);
	}

	@Override
	public void init() {

		System.out.println("Generate vcf.gz files from your 23andMe Genotype Data\n\n");

	}

	@Override
	public void createParameters() {

		addParameter("genome", "input 23andme raw data (e.g. my-genome.txt)");
		addParameter("ref", "input reference (e.g. human_g1k_v37.fasta)");
		addOptionalParameter("chromosomes", "optional; specify list of chromosomes to extract (e.g. 1,3,5)", STRING);
		addParameter("out", "output vcf directory");

	}

	@Override
	public int run() {

		try {
			String in = (String) getValue("genome");
			String ref = (String) getValue("ref");
			String chromosomes = (String) getValue("chromosomes");
			String outDirectory = (String) getValue("out");

			VariantContextWriter vcfWriter = null;
			VCFHeader header = null;
			
			//download reference 
			String fastaPath = "human_g1k_v37.fasta";
			if (ref.equals("v37") && !new File(fastaPath).exists()) {
				
				System.out.println("Download Reference GRCh37 from " +  GRCh37_PATH+".gz");
				FileUtils.copyURLToFile(new URL(GRCh37_PATH + ".gz"), new File(fastaPath+".gz"));
				FileUtils.copyURLToFile(new URL(GRCh37_PATH + ".fai"), new File(fastaPath+".fai"));
				gunzip("human_g1k_v37.fasta.gz", fastaPath);
				ref = fastaPath;

			}

			// create final output directory
			new File(outDirectory).mkdirs();
			int counter = 0;
			String prev = "";

			//initial everything
			LineReader genomeReader = new LineReader(in);
			IndexedFastaSequenceFile refFasta = new IndexedFastaSequenceFile(new File(ref));
			
			// loop over 23andMe file
			while (genomeReader.next()) {

				if (genomeReader.get().startsWith("#")) {
					continue;
				}

				// parse 23andMe input line
				GenotypeLine line = new GenotypeLine(genomeReader.get());
				String genotype = line.getGenotype();
				String chromosome = line.getChromosome();
				String g0 = genotype.substring(0, 1);
				String g1 = null;

				// order chromosomes (one time loop over file)
				String[] chromosomeParts = null;
				if (chromosomes != null) {
					chromosomeParts = chromosomes.split(",");
				}
				if (chromosomeParts != null && !Arrays.asList(chromosomeParts).contains(line.getChromosome())) {
					continue;
				}

				// Exclude indels, unsupported & multialleleic alleles
				if (line.getGenotype().contains("I") || genotype.contains("D") || genotype.contains("-")
						|| genotype.length() > 2) {
					continue;
				}

				// prepare file for each chromosome
				if (!chromosome.equals(prev)) {

					if (vcfWriter != null) {
						System.out.println("chr" + prev + " - #sites: " + counter);
						vcfWriter.close();
					}

					header = generateHeader(chromosome);

					VariantContextWriterBuilder builder = new VariantContextWriterBuilder()
							.setOutputFile(outDirectory + File.separator + chromosome + ".vcf.gz")
							.unsetOption(Options.INDEX_ON_THE_FLY);

					vcfWriter = builder.build();

					vcfWriter.writeHeader(header);

					// prepare for next chromosome
					prev = chromosome;
					counter = 0;

				}

				// get reference position from fasta
				String refPos = refFasta.getSubsequenceAt(line.getChromosome(), line.getPos(), line.getPos())
						.getBaseString();

				Allele refAllele = Allele.create(refPos, true);
				Allele altAllele = null;
				boolean heterozygous = false;
				boolean homozygous = false;

				if (genotype.length() == 2) {
					g1 = genotype.substring(1, 2);
				}

				// heterozygous genotype check
				if (g1 != null && !g0.equals(g1)) {
					altAllele = Allele.create(!refPos.equals(g0) ? g0 : g1, false);
					heterozygous = true;
				}
				// homozygous genotype check
				else if (!refPos.equals(g0)) {
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
					vcfWriter.add(createVC(header, chromosome, line.getRsid(), alleles, alleles, line.getPos()));

				} else if (homozygous) {

					counter++;
					final List<Allele> genotypes = new ArrayList<Allele>();
					genotypes.add(altAllele);
					genotypes.add(altAllele);
					vcfWriter.add(createVC(header, chromosome, line.getRsid(), alleles, genotypes, line.getPos()));

				}

			} // while loop

			System.out.println("chr" + prev + " - #sites: " + counter);
			vcfWriter.close();
			refFasta.close();

			return 0;

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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

	public void gunzip(String inFile, String outFile) {

		byte[] buffer = new byte[1024];

		try {

			System.out.println("Uncompress...");
			
			GZIPInputStream gzis = new GZIPInputStream(new FileInputStream(inFile));

			FileOutputStream out = new FileOutputStream(outFile);

			int len;
			while ((len = gzis.read(buffer)) > 0) {
				out.write(buffer, 0, len);
			}

			gzis.close();
			out.close();

			System.out.println("Done");

		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	public static void main(String[] args) {
		new Convert23andMe(args).start();
	}

}
