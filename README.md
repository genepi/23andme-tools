# 23andme Tools
23andMe files come in a txt file format. To process these files further (e.g. for Imputation with https://imputationserver.sph.umich.edu), VCFs are needed. This project has been inspired by https://github.com/arrogantrobot/23andme2vcf and provides a simple Java implementation.

Currently two tools are available:

* site-generator: Extracts all sites from the human genome (build 37) fasta file (human_g1k_v37.fasta). It writes the same file format as used in https://github.com/arrogantrobot/23andme2vcf. 
* vcf-generator: Generates valid VCF files out of the 23andMe raw data.


### site-generator

```bash
git clone https://github.com/seppinho/23andme-tools.git
cd 23andme-tools
mvn install
java -jar target/23andme-tools-0.1.jar site-generator --ref <human_g1k_v37.fasta> --in <23andme.txt> --out <target-list.txt>

```
The human_g1k_v37.fasta file is not included in this repository but can be downloaded from here: ftp://ftp.1000genomes.ebi.ac.uk/vol1/ftp/technical/reference/
```bash
wget ftp://ftp.1000genomes.ebi.ac.uk/vol1/ftp/technical/reference/human_g1k_v37.fasta.gz
wget ftp://ftp.1000genomes.ebi.ac.uk/vol1/ftp/technical/reference/human_g1k_v37.fasta.fai
gunzip human_g1k_v37.fasta.gz
```
