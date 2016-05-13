package genepi.objects;

public class Chip {

	private String rsid;
	private String chromosome;
	private int pos;
	private String ref;

	public Chip(String line) {
		parse(line);
	}

	public String getRsid() {
		return rsid;
	}

	public void setRsid(String rsid) {
		this.rsid = rsid;
	}

	public String getChromosome() {
		return chromosome;
	}

	public void setChromosome(String chromosome) {
		this.chromosome = chromosome;
	}

	public int getPos() {
		return pos;
	}

	public void setPos(int pos) {
		this.pos = pos;
	}

	public String getRef() {
		return ref;
	}

	public void setRef(String ref) {
		this.ref = ref;
	}

	private void parse(String line) {

		String[] splits = line.split("\t");
		this.chromosome = splits[0];
		this.pos = Integer.valueOf(splits[1]);
		this.rsid = splits[2];
		this.ref = splits[3];

	}
}
