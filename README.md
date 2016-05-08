# 23andMe Tools - 23andMe data to VCF
23andMe raw data is provided in a tab-delimited text format including 4 columns (#rsid;chromosome;position;genotype). To process these files further (e.g. for Imputation with the [Michigan Imputation Server](https://imputationserver.sph.umich.edu) or for assigning a mitochondrial haplogroup with [HaploGrep](http://haplogrep.uibk.ac.at)), VCF files are needed. Here we provide a simple Java implementation to convert your 23andMe raw data into VCFs. Credits to [this](https://github.com/arrogantrobot/23andme2vcf) project, which provides a working perl implementation.  

This github project includes 2 tools:

* site-generator: Extracts all sites from the human genome (build 37) fasta file (human_g1k_v37.fasta) and generates a chip-specific file. For simplicity, the same file format as described in the perl project above is generated. 
* vcf-generator: Generates valid VCF files out of the 23andMe raw data.

## Get your data
Your personal genome can be downloaded from [here](https://www.23andme.com/you/download). After entering your secure answer, the complete dataset can be downloaded at once (zip file).

## Generate the chip sites
I already generated a site list for the current 23andMe version 4 (v4) genotyping chip (May 2016) including 610544 sites. It basically extracts the information from your personal genome (without the genotype) and adds the reference base from the FWD strand. If your data has been genotyped with this version of the chip, you can skip this step. For older chip versions (v1-v3) I would very much appreciate a pull request to the repository. 

```bash
git clone https://github.com/seppinho/23andme-tools.git
cd 23andme-tools
mvn install
java -jar target/23andme-tools-0.1.jar site-generator --ref <human_g1k_v37.fasta> --in <23andme.txt> --out <23andme-v4-GRCh37-fwd.txt>

```
The human_g1k_v37.fasta file is not included in this repository but can be downloaded from here: ftp://ftp.1000genomes.ebi.ac.uk/vol1/ftp/technical/reference/
```bash
wget ftp://ftp.1000genomes.ebi.ac.uk/vol1/ftp/technical/reference/human_g1k_v37.fasta.gz
wget ftp://ftp.1000genomes.ebi.ac.uk/vol1/ftp/technical/reference/human_g1k_v37.fasta.fai
gunzip human_g1k_v37.fasta.gz
```

## Generate a VCF file
//todo
